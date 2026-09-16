package com.ghordrin.bosshealthbar;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Font;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.FontType;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Modern Boss Healthbar",
	description = "Replaces the opponent health bar with a themed bar that shows a damage trail and your recent damage",
	tags = {"boss", "health", "healthbar", "hitpoints", "overlay", "pvm", "combat"}
)
public class BossHealthBarPlugin extends Plugin
{
	/**
	 * Your own hits add up into one damage number while each lands within this long of the last.
	 */
	static final Duration DAMAGE_COMBO_WINDOW = Duration.ofMillis(2500);
	// Values of TOB_CLIENT_WAVEPROGRESS_TYPE: the bar is hidden, or it shows a boss's health.
	// Other values show room progress instead.
	private static final int TOB_PROGRESS_NONE = 0;
	private static final int TOB_PROGRESS_BOSS_HEALTH = 1;
	// How far from the player, in tiles, to look for the boss of the current Theatre of Blood room.
	private static final int TOB_BOSS_SEARCH_DISTANCE = 32;
	// The game message sent when a superior slayer monster spawns for you. It arrives wrapped in
	// color markers, such as "@mes_hl_red@A superior foe has appeared...</col>".
	private static final String SUPERIOR_SPAWN_MESSAGE = "A superior foe has appeared";
	// How many ticks apart the superior's spawn and its message may be to still be matched.
	private static final int SUPERIOR_MATCH_TICKS = 2;
	// How far from the player, in tiles, a newly spawned NPC can be to count as your superior.
	private static final int SUPERIOR_SEARCH_DISTANCE = 15;
	private static final String VANILLA_OVERLAY_GROUP = "opponentinfo";
	private static final String VANILLA_OVERLAY_KEY = "showOpponentHealthOverlay";
	// Kept in this plugin's config group rather than in memory, so the original value can still be
	// restored after the client closes while the plugin is on.
	private static final String SAVED_VANILLA_OVERLAY_KEY = "savedOpponentHealthOverlay";
	private static final String VANILLA_OVERLAY_HIDDEN_KEY = "opponentHealthOverlayHidden";
	// Themes that have been removed, and the closest current theme each saved choice moves to.
	private static final Map<String, HealthBarTheme> REMOVED_THEMES = ImmutableMap.<String, HealthBarTheme>builder()
		.put("ASHEN_CRIMSON", HealthBarTheme.ZAMORAK)
		.put("EMBERFALL", HealthBarTheme.RALOS)
		.put("GILDED_BLOOD", HealthBarTheme.ZAMORAK)
		.put("FROSTBOUND", HealthBarTheme.SARADOMIN)
		.put("ABYSSAL", HealthBarTheme.ZAROS)
		.put("OBSIDIAN", HealthBarTheme.ARMADYL)
		.put("VERDANT", HealthBarTheme.GUTHIX)
		.put("VENOM", HealthBarTheme.BANDOS)
		.put("SUNFORGED", HealthBarTheme.TUMEKEN)
		.put("DUSKROSE", HealthBarTheme.ZAROS)
		.build();
	// The removed font choice and text size percentage, replaced by the font setting.
	private static final String OLD_FONT_STYLE_KEY = "fontStyle";
	private static final String OLD_TEXT_SIZE_KEY = "textSize";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private BossHealthBarConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BossHealthBarOverlay overlay;

	@Getter(AccessLevel.PACKAGE)
	private Actor lastOpponent;

	// When the last hit landed on the opponent, from System.currentTimeMillis(), or 0 if none.
	@Getter(AccessLevel.PACKAGE)
	private long lastHitMillis;

	@Getter(AccessLevel.PACKAGE)
	private int lastHitAmount;

	@Getter(AccessLevel.PACKAGE)
	private int comboDamage;

	// When your last hit landed, from System.currentTimeMillis(), or 0 if none.
	@Getter(AccessLevel.PACKAGE)
	private long lastDamageDealtMillis;

	// When you stopped interacting with the opponent, from System.currentTimeMillis(), or 0 if you haven't.
	private long lastInteractionLostMillis;
	private boolean nativeBarHidden;
	private NPC nativeBarNpc;
	private int nativeBarSearchedId = -1;
	private int replacedNativeBarNpcId = -1;
	private boolean tobBarHidden;
	private NPC tobBoss;
	private boolean tobBossSearchNeeded = true;
	// NPCs that spawned in the last few ticks, with the tick they spawned on.
	private final Map<NPC, Integer> recentSpawnTicks = new HashMap<>();
	// Superior slayer monsters you spawned that are still loaded.
	private final Set<NPC> superiors = new HashSet<>();
	private int superiorMessageTick = -1;

	@Provides
	BossHealthBarConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BossHealthBarConfig.class);
	}

	@Override
	protected void startUp()
	{
		migrateRemovedTheme();
		migrateOldFontSettings();
		// The overlay keeps its state while the plugin is off, and misses any config changes made then.
		overlay.reset();
		overlayManager.add(overlay);
		applyVanillaOverlayOverride();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		restoreVanillaOverlay();
		// The client thread reads and changes this state, so clear it there.
		clientThread.invoke(() ->
		{
			restoreNativeBar();
			restoreTobBar();
			resetState();
		});
	}

	/**
	 * Moves a saved theme that no longer exists to the closest current theme, so the setting isn't
	 * silently reset to the default.
	 */
	private void migrateRemovedTheme()
	{
		final String saved = configManager.getConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.THEME_KEY);
		final HealthBarTheme replacement = saved != null ? REMOVED_THEMES.get(saved) : null;
		if (replacement != null)
		{
			configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.THEME_KEY, replacement);
		}
	}

	/**
	 * Turns the removed font choice and text size settings into the font setting, with the family,
	 * size and weight the old choice was drawn with, then removes them.
	 */
	private void migrateOldFontSettings()
	{
		final String style = configManager.getConfiguration(BossHealthBarConfig.GROUP, OLD_FONT_STYLE_KEY);
		final String textSize = configManager.getConfiguration(BossHealthBarConfig.GROUP, OLD_TEXT_SIZE_KEY);
		if (style == null && textSize == null)
		{
			return;
		}

		if (configManager.getConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.FONT_KEY) == null)
		{
			float scale = 1f;
			if (textSize != null)
			{
				try
				{
					scale = Integer.parseInt(textSize) / 100f;
				}
				catch (NumberFormatException e)
				{
					log.debug("Ignoring invalid saved text size {}", textSize);
				}
			}

			final FontType font;
			if ("RUNESCAPE".equals(style))
			{
				font = FontType.BOLD.withSize(Math.round(16 * scale));
			}
			else if ("SANS_SERIF".equals(style))
			{
				font = new FontType().withFamily(Font.SANS_SERIF).withBold(true).withSize(Math.round(15 * scale));
			}
			else
			{
				font = BossHealthBarConfig.DEFAULT_FONT.withSize(Math.round(17 * scale));
			}
			configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.FONT_KEY, font);
		}

		configManager.unsetConfiguration(BossHealthBarConfig.GROUP, OLD_FONT_STYLE_KEY);
		configManager.unsetConfiguration(BossHealthBarConfig.GROUP, OLD_TEXT_SIZE_KEY);
	}

	/**
	 * Fills in the custom colors with the colors of the theme selected before switching to Custom,
	 * so they can be tweaked from there.
	 *
	 * @param previousValue the saved name of the previous theme, or null when it was the default
	 */
	private void copyThemeToCustomColors(String previousValue)
	{
		HealthBarTheme previous = BossHealthBarConfig.DEFAULT_THEME;
		if (previousValue != null)
		{
			previous = null;
			for (HealthBarTheme theme : HealthBarTheme.values())
			{
				if (theme.name().equals(previousValue))
				{
					previous = theme;
					break;
				}
			}
		}

		final ThemeColors colors = previous != null ? previous.getColors() : null;
		if (colors == null)
		{
			return;
		}

		setCustomColor("customFillHighColor", colors.getFillHigh());
		setCustomColor("customFillLowColor", colors.getFillLow());
		setCustomColor("customTrailColor", colors.getTrail());
		setCustomColor("customFrameColor", colors.getFrame());
		setCustomColor("customOrnamentColor", colors.getOrnament());
		setCustomColor("customGemColor", colors.getGem());
		setCustomColor("customTextColor", colors.getText());
		setCustomColor("customLevelTextColor", colors.getLevelText());
		setCustomColor("customHitpointsTextColor", colors.getHitpointsText());
		setCustomColor("customDefeatedTextColor", colors.getDefeatedText());
	}

	private void setCustomColor(String key, Color color)
	{
		configManager.setConfiguration(BossHealthBarConfig.GROUP, key, color);
	}

	/**
	 * Clears everything known about the opponent and nearby NPCs when logging out or hopping worlds,
	 * since those NPCs are gone.
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			resetState();
		}
	}

	private void resetState()
	{
		// The game's bars are unloaded on the login screen, and restored before this on shutdown.
		nativeBarHidden = false;
		tobBarHidden = false;
		lastOpponent = null;
		nativeBarNpc = null;
		nativeBarSearchedId = -1;
		replacedNativeBarNpcId = -1;
		tobBoss = null;
		tobBossSearchNeeded = true;
		recentSpawnTicks.clear();
		superiors.clear();
		superiorMessageTick = -1;
		lastHitMillis = 0;
		lastInteractionLostMillis = 0;
		resetComboDamage();
		// The overlay keeps drawing the opponent through the defeat animation, so it has to forget it
		// too, or the bar can carry over to the next world.
		overlay.reset();
	}

	/**
	 * Makes the overlay read its colors again when any setting of this plugin changes, so a new
	 * theme or custom color shows on the next frame, and hides or restores the "Opponent
	 * Information" health overlay when that setting is toggled.
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!BossHealthBarConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		overlay.invalidateColors();

		if (BossHealthBarConfig.THEME_KEY.equals(event.getKey())
			&& HealthBarTheme.CUSTOM.name().equals(event.getNewValue()))
		{
			copyThemeToCustomColors(event.getOldValue());
		}

		if (BossHealthBarConfig.HIDE_VANILLA_OVERLAY_KEY.equals(event.getKey()))
		{
			if (config.hideVanillaOverlay())
			{
				applyVanillaOverlayOverride();
			}
			else
			{
				restoreVanillaOverlay();
			}
		}
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		if (event.getSource() != client.getLocalPlayer())
		{
			return;
		}

		Actor opponent = event.getTarget();

		if (opponent == null)
		{
			lastInteractionLostMillis = System.currentTimeMillis();
			log.debug("Interaction lost with {}, will clear after {}s if not resumed", lastOpponent, config.hideDelay());
			return;
		}

		lastInteractionLostMillis = 0;

		if (opponent == lastOpponent)
		{
			return;
		}

		if (lastOpponent != null && !lastOpponent.isDead() && priority(opponent) < priority(lastOpponent))
		{
			// Attacking the lower-level NPCs a boss spawns shouldn't take the bar away from the boss.
			log.debug("Keeping {} over lower priority target {}", lastOpponent.getName(), opponent.getName());
			return;
		}

		setOpponent(opponent);
	}

	private void setOpponent(Actor opponent)
	{
		overlay.resetAnimation();
		resetComboDamage();
		lastOpponent = opponent;
		log.debug("New opponent: {} (combat level {}, meets boss threshold: {})",
			opponent.getName(), opponent.getCombatLevel(), opponent.getCombatLevel() >= config.minimumCombatLevel());
	}

	/**
	 * Ranks an actor for keeping the bar: 2 if a game boss bar is showing it, 1 if it passes the
	 * "Only show for bosses" filter, 0 otherwise, and -1 for null. A new target only replaces a
	 * living opponent with a rank at least as high.
	 */
	private int priority(Actor actor)
	{
		if (actor == null)
		{
			return -1;
		}
		if (isNativeBarNpc(actor) || actor == findTobBoss())
		{
			return 2;
		}
		return shouldShowBarFor(actor) ? 1 : 0;
	}

	/**
	 * Returns the NPC whose health the Theatre of Blood boss bar shows, or null when that bar isn't
	 * up or shows room progress. The bar doesn't say which NPC it belongs to, so this picks the
	 * nearby attackable NPC with the highest combat level, and the largest one on a tie. The result
	 * is cached until an NPC that could replace it spawns or changes, or the boss despawns.
	 */
	private NPC findTobBoss()
	{
		if (client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_TYPE) != TOB_PROGRESS_BOSS_HEALTH
			|| client.getWidget(InterfaceID.TobHud.PROGRESS_CONTAINER) == null)
		{
			tobBoss = null;
			tobBossSearchNeeded = true;
			return null;
		}

		if (!tobBossSearchNeeded)
		{
			return tobBoss;
		}

		tobBossSearchNeeded = false;
		tobBoss = null;
		final Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc.isDead() || !isAttackable(npc)
				|| npc.getWorldLocation().distanceTo(player.getWorldLocation()) > TOB_BOSS_SEARCH_DISTANCE)
			{
				continue;
			}

			if (tobBoss == null
				|| npc.getCombatLevel() > tobBoss.getCombatLevel()
				|| (npc.getCombatLevel() == tobBoss.getCombatLevel() && size(npc) > size(tobBoss)))
			{
				tobBoss = npc;
			}
		}
		return tobBoss;
	}

	/**
	 * Whether the NPC's current form has an "Attack" menu option.
	 */
	private static boolean isAttackable(NPC npc)
	{
		final NPCComposition composition = npc.getTransformedComposition();
		if (composition == null)
		{
			return false;
		}
		for (String action : composition.getActions())
		{
			if ("Attack".equals(action))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * The NPC's size in tiles, or 0 when its current form is unknown.
	 */
	private static int size(NPC npc)
	{
		final NPCComposition composition = npc.getTransformedComposition();
		return composition != null ? composition.getSize() : 0;
	}

	/**
	 * Whether a game boss bar is showing this opponent. Only checks the tracked NPC ID and the
	 * cached result of {@link #findTobBoss()}, so it doesn't search the NPC list.
	 */
	private boolean isGameBarBoss(Actor opponent)
	{
		return opponent != null && (isNativeBarNpc(opponent) || opponent == tobBoss);
	}

	/**
	 * The NPC ID the game's boss bar tracks, or -1 when it tracks none or is turned off in the game
	 * settings.
	 */
	private int nativeBarNpcId()
	{
		if (client.getVarbitValue(VarbitID.HPBAR_HUD_BOSS_DISABLED) != 0)
		{
			return -1;
		}
		return client.getVarpValue(VarPlayerID.HPBAR_HUD_NPC);
	}

	/**
	 * Whether the actor is an NPC with the ID the game's boss bar tracks, which means that bar is
	 * enabled in the game settings and showing it. Several loaded NPCs can share that ID.
	 */
	boolean isNativeBarNpc(Actor actor)
	{
		if (!(actor instanceof NPC))
		{
			return false;
		}
		final int trackedId = nativeBarNpcId();
		return trackedId != -1 && compositionId((NPC) actor) == trackedId;
	}

	/**
	 * Returns the loaded NPC that the game's boss bar is tracking, or null if there is none. When
	 * several NPCs share the tracked ID, the current opponent or the NPC you are attacking is
	 * preferred. Otherwise the result is cached, and the NPC list is only searched again when the
	 * tracked NPC ID changes or an NPC spawns.
	 */
	private NPC findNativeBarNpc()
	{
		final int trackedId = nativeBarNpcId();
		if (trackedId == -1)
		{
			nativeBarNpc = null;
			// The same ID may be tracked again later, so don't let the search below be skipped for it.
			nativeBarSearchedId = -1;
			return null;
		}

		if (isNativeBarNpc(lastOpponent))
		{
			nativeBarNpc = (NPC) lastOpponent;
			return nativeBarNpc;
		}

		final Player player = client.getLocalPlayer();
		final Actor target = player != null ? player.getInteracting() : null;
		if (isNativeBarNpc(target))
		{
			nativeBarNpc = (NPC) target;
			return nativeBarNpc;
		}

		if (nativeBarNpc != null && compositionId(nativeBarNpc) == trackedId)
		{
			return nativeBarNpc;
		}

		if (nativeBarNpc == null && trackedId == nativeBarSearchedId)
		{
			return null;
		}

		nativeBarNpc = null;
		nativeBarSearchedId = trackedId;
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (compositionId(npc) == trackedId)
			{
				nativeBarNpc = npc;
				break;
			}
		}
		return nativeBarNpc;
	}

	/**
	 * The ID of the NPC's base composition, which is what the game's boss bar tracks, or -1 if unknown.
	 */
	private static int compositionId(NPC npc)
	{
		final NPCComposition composition = npc.getComposition();
		return composition != null ? composition.getId() : -1;
	}

	/**
	 * Records hits on the current opponent: every hit for the big hit flash and the damage trail,
	 * and your own hits for the damage number.
	 */
	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getActor() != lastOpponent)
		{
			return;
		}

		final Hitsplat hitsplat = event.getHitsplat();
		if (hitsplat.getAmount() <= 0 || hitsplat.getHitsplatType() == HitsplatID.HEAL)
		{
			// Misses and heals don't remove health, so they shouldn't flash the bar or hold the trail.
			return;
		}

		final long now = System.currentTimeMillis();
		lastHitMillis = now;
		lastHitAmount = hitsplat.getAmount();

		if (hitsplat.isMine())
		{
			if (lastDamageDealtMillis == 0 || now - lastDamageDealtMillis > DAMAGE_COMBO_WINDOW.toMillis())
			{
				comboDamage = 0;
			}
			comboDamage += hitsplat.getAmount();
			lastDamageDealtMillis = now;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		// The boss may have just appeared or reappeared, so search for it again.
		nativeBarSearchedId = -1;
		if (mayBeTobBoss(event.getNpc()))
		{
			tobBossSearchNeeded = true;
		}
		recentSpawnTicks.put(event.getNpc(), client.getTickCount());
	}

	/**
	 * Notes when the game says a superior slayer monster has spawned for you. The NPC itself is
	 * picked out on the game tick, by {@link #markSuperior()}.
	 */
	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if ((event.getType() == ChatMessageType.GAMEMESSAGE || event.getType() == ChatMessageType.SPAM)
			&& event.getMessage().contains(SUPERIOR_SPAWN_MESSAGE))
		{
			superiorMessageTick = client.getTickCount();
			log.debug("Superior spawn message on tick {}", superiorMessageTick);
		}
	}

	/**
	 * After a superior spawn message, marks the attackable NPC that spawned closest to the player
	 * within a few ticks of the message as a superior. The message and the spawn can arrive in either
	 * order, so this keeps trying for {@link #SUPERIOR_MATCH_TICKS} ticks.
	 */
	private void markSuperior()
	{
		final int tick = client.getTickCount();
		recentSpawnTicks.values().removeIf(spawnTick -> tick - spawnTick > SUPERIOR_MATCH_TICKS);

		if (superiorMessageTick == -1)
		{
			return;
		}

		final Player player = client.getLocalPlayer();
		NPC nearest = null;
		int nearestDistance = Integer.MAX_VALUE;
		if (player != null)
		{
			for (NPC npc : recentSpawnTicks.keySet())
			{
				final int distance = npc.getWorldLocation().distanceTo(player.getWorldLocation());
				if (!npc.isDead() && !superiors.contains(npc) && isAttackable(npc)
					&& distance <= SUPERIOR_SEARCH_DISTANCE && distance < nearestDistance)
				{
					nearest = npc;
					nearestDistance = distance;
				}
			}
		}

		if (nearest != null)
		{
			superiors.add(nearest);
			superiorMessageTick = -1;
			log.debug("Marked {} as a superior", nearest.getName());
		}
		else if (tick - superiorMessageTick >= SUPERIOR_MATCH_TICKS)
		{
			superiorMessageTick = -1;
			log.debug("No spawned NPC found for the superior spawn message");
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		// Some bosses change form between phases, which can change their combat level.
		if (event.getNpc() == tobBoss || mayBeTobBoss(event.getNpc()))
		{
			tobBossSearchNeeded = true;
		}
	}

	/**
	 * Whether a spawned or changed NPC could be picked over the current Theatre of Blood boss, so
	 * the many smaller NPCs that spawn in some rooms don't each cause a search.
	 */
	private boolean mayBeTobBoss(NPC npc)
	{
		return tobBoss == null || npc.getCombatLevel() >= tobBoss.getCombatLevel();
	}

	/**
	 * Drops cached references to the despawned NPC, and clears the opponent if it was this NPC, so
	 * the bar doesn't stay on an NPC that no longer exists. A boss that comes back as a new NPC is
	 * picked up again by the next search.
	 */
	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (event.getNpc() == nativeBarNpc)
		{
			nativeBarNpc = null;
		}
		if (event.getNpc() == tobBoss)
		{
			// Only losing the boss itself can change which NPC is picked.
			tobBoss = null;
			tobBossSearchNeeded = true;
		}
		recentSpawnTicks.remove(event.getNpc());
		superiors.remove(event.getNpc());

		if (event.getNpc() != lastOpponent)
		{
			return;
		}

		log.debug("Opponent {} despawned, clearing", lastOpponent);
		lastOpponent = null;
		lastInteractionLostMillis = 0;
		resetComboDamage();
	}

	private void resetComboDamage()
	{
		comboDamage = 0;
		lastDamageDealtMillis = 0;
	}

	/**
	 * Clears the opponent once you have stopped interacting for longer than the "Hide after"
	 * setting, unless a game boss bar is still showing it.
	 */
	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (tobBoss == null)
		{
			// The room's boss may have only come within range as the player walked in.
			tobBossSearchNeeded = true;
		}

		markSuperior();

		final Player player = client.getLocalPlayer();
		if (lastOpponent != null
			&& player != null
			&& lastOpponent != findNativeBarNpc()
			&& lastOpponent != findTobBoss()
			&& lastInteractionLostMillis != 0
			&& player.getInteracting() == null
			&& System.currentTimeMillis() - lastInteractionLostMillis > config.hideDelay() * 1000L)
		{
			log.debug("Opponent {} timed out after {}s with no interaction, clearing", lastOpponent, config.hideDelay());
			lastOpponent = null;
		}
	}

	/**
	 * While a game boss bar is up, switches this bar to the NPC it shows, then hides or restores
	 * the game's bars. This runs before every frame, because the game's scripts can unhide its
	 * bars whenever they update, and it keeps the game's bar from showing for even one frame.
	 */
	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		final NPC nativeBarBoss = findNativeBarNpc();
		final NPC gameBarBoss = nativeBarBoss != null ? nativeBarBoss : findTobBoss();
		if (gameBarBoss != null && !gameBarBoss.isDead() && gameBarBoss != lastOpponent)
		{
			setOpponent(gameBarBoss);
			lastInteractionLostMillis = 0;
		}

		updateNativeBar();
		updateTobBar();
	}

	/**
	 * Hides the game's boss bar while "Replace game's boss health bar" is on and that bar tracks
	 * the current opponent. Otherwise, shows it again if this plugin hid it.
	 */
	private void updateNativeBar()
	{
		final Widget nativeBar = client.getWidget(InterfaceID.HpbarHud.UNIVERSE);
		if (nativeBar == null)
		{
			nativeBarHidden = false;
			return;
		}

		boolean replace = false;
		if (config.replaceNativeBossBar())
		{
			if (shouldShowBarFor(lastOpponent) && isNativeBarNpc(lastOpponent))
			{
				replace = true;
				replacedNativeBarNpcId = nativeBarNpcId();
			}
			else
			{
				// After the boss dies and despawns, the game's bar can stay up on it while this bar
				// plays its defeat animation, so keep it hidden until it tracks a different NPC.
				replace = replacedNativeBarNpcId != -1 && nativeBarNpcId() == replacedNativeBarNpcId;
			}
		}

		if (replace)
		{
			if (!nativeBar.isSelfHidden())
			{
				nativeBar.setHidden(true);
				nativeBarHidden = true;
			}
		}
		else
		{
			replacedNativeBarNpcId = -1;
			restoreNativeBar();
		}
	}

	/**
	 * Hides the Theatre of Blood boss bar while "Replace game's boss health bar" is on and that bar
	 * shows the current opponent. Otherwise, shows it again if this plugin hid it.
	 */
	private void updateTobBar()
	{
		final Widget tobBar = client.getWidget(InterfaceID.TobHud.PROGRESS_CONTAINER);
		if (tobBar == null)
		{
			tobBarHidden = false;
			return;
		}

		if (config.replaceNativeBossBar() && isTobBarTracking(lastOpponent) && shouldShowBarFor(lastOpponent))
		{
			if (!tobBar.isSelfHidden())
			{
				tobBar.setHidden(true);
				tobBarHidden = true;
			}
		}
		else
		{
			restoreTobBar();
		}
	}

	/**
	 * Whether the Theatre of Blood boss bar is showing this opponent's health.
	 */
	boolean isTobBarTracking(Actor opponent)
	{
		return opponent != null && opponent == findTobBoss();
	}

	/**
	 * Shows the game's boss bar again, if this plugin hid it and the game still tracks an NPC with
	 * it. That way a bar the game no longer wants shown doesn't reappear with outdated health.
	 */
	private void restoreNativeBar()
	{
		if (!nativeBarHidden || nativeBarNpcId() == -1)
		{
			return;
		}

		final Widget nativeBar = client.getWidget(InterfaceID.HpbarHud.UNIVERSE);
		if (nativeBar != null)
		{
			nativeBar.setHidden(false);
		}
		nativeBarHidden = false;
	}

	/**
	 * Shows the Theatre of Blood boss bar again, if this plugin hid it and the game still wants it
	 * shown. That way a bar the game has just hidden doesn't reappear with outdated health.
	 */
	private void restoreTobBar()
	{
		if (!tobBarHidden)
		{
			return;
		}

		final Widget tobBar = client.getWidget(InterfaceID.TobHud.PROGRESS_CONTAINER);
		if (tobBar != null && client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_TYPE) != TOB_PROGRESS_NONE)
		{
			tobBar.setHidden(false);
		}
		tobBarHidden = false;
	}

	/**
	 * Whether the opponent should get a bar: it must have a name and, when "Only show for bosses"
	 * is on, either meet the minimum combat level, be shown by a game boss bar, or be a superior
	 * slayer monster you spawned while "Show for superior slayer monsters" is on.
	 */
	boolean shouldShowBarFor(Actor opponent)
	{
		return opponent != null
			&& opponent.getName() != null
			&& (!config.bossOnly()
				|| opponent.getCombatLevel() >= config.minimumCombatLevel()
				|| isGameBarBoss(opponent)
				|| (config.showSuperiors() && superiors.contains(opponent)));
	}

	/**
	 * Turns off the health overlay of RuneLite's "Opponent Information" plugin when the "Hide
	 * vanilla opponent overlay" setting is on, saving its previous value for
	 * {@link #restoreVanillaOverlay()}. If the value was already saved, such as when the client
	 * closed while this plugin was on, the saved value is kept instead of being overwritten.
	 */
	private void applyVanillaOverlayOverride()
	{
		if (!config.hideVanillaOverlay())
		{
			return;
		}

		if (!isVanillaOverlayHidden())
		{
			final String current = configManager.getConfiguration(VANILLA_OVERLAY_GROUP, VANILLA_OVERLAY_KEY);
			if (current == null)
			{
				configManager.unsetConfiguration(BossHealthBarConfig.GROUP, SAVED_VANILLA_OVERLAY_KEY);
			}
			else
			{
				configManager.setConfiguration(BossHealthBarConfig.GROUP, SAVED_VANILLA_OVERLAY_KEY, current);
			}
			configManager.setConfiguration(BossHealthBarConfig.GROUP, VANILLA_OVERLAY_HIDDEN_KEY, true);
		}

		configManager.setConfiguration(VANILLA_OVERLAY_GROUP, VANILLA_OVERLAY_KEY, false);
	}

	/**
	 * Puts back the "Opponent Information" overlay setting saved by {@link #applyVanillaOverlayOverride()}.
	 */
	private void restoreVanillaOverlay()
	{
		if (!isVanillaOverlayHidden())
		{
			return;
		}

		final String saved = configManager.getConfiguration(BossHealthBarConfig.GROUP, SAVED_VANILLA_OVERLAY_KEY);
		if (saved == null)
		{
			configManager.unsetConfiguration(VANILLA_OVERLAY_GROUP, VANILLA_OVERLAY_KEY);
		}
		else
		{
			configManager.setConfiguration(VANILLA_OVERLAY_GROUP, VANILLA_OVERLAY_KEY, saved);
		}

		configManager.unsetConfiguration(BossHealthBarConfig.GROUP, SAVED_VANILLA_OVERLAY_KEY);
		configManager.unsetConfiguration(BossHealthBarConfig.GROUP, VANILLA_OVERLAY_HIDDEN_KEY);
	}

	/**
	 * Whether this plugin has turned off the "Opponent Information" health overlay and not yet put
	 * it back.
	 */
	private boolean isVanillaOverlayHidden()
	{
		return Boolean.parseBoolean(configManager.getConfiguration(BossHealthBarConfig.GROUP, VANILLA_OVERLAY_HIDDEN_KEY));
	}
}
