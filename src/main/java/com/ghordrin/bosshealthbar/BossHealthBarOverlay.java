/*
 * Copyright (c) 2026, Ghordrin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ghordrin.bosshealthbar;

import com.google.common.base.Strings;
import static com.ghordrin.bosshealthbar.ColorUtil.brighten;
import static com.ghordrin.bosshealthbar.ColorUtil.darken;
import static com.ghordrin.bosshealthbar.ColorUtil.lerp;
import static com.ghordrin.bosshealthbar.ColorUtil.withAlpha;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ParamID;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.NPCManager;
import net.runelite.client.ui.FontManager;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.Text;

/**
 * Draws the current opponent's health bar: the name and your recent damage above it, the
 * hitpoints text below it, and the damage trail, heal, low health and defeat animations.
 */
class BossHealthBarOverlay extends Overlay
{
	private static final Duration TRAIL_HOLD = Duration.ofMillis(800);
	private static final float TRAIL_CATCH_UP_RATE = 3.5f;
	private static final float TRAIL_MIN_DRAIN_PER_SECOND = 0.15f;
	private static final Duration FLASH_DURATION = Duration.ofMillis(350);
	private static final Duration FADE_IN_DURATION = Duration.ofMillis(300);
	private static final long DAMAGE_NUMBER_FADE_MILLIS = 400;
	private static final float BIG_HIT_FRACTION = 0.08f;
	private static final Duration DEFEAT_HOLD = Duration.ofMillis(1600);
	private static final Duration DEFEAT_FADE = Duration.ofMillis(700);
	private static final String DEFEATED_TEXT = "Defeated";
	private static final Duration LOW_HEALTH_PULSE_PERIOD = Duration.ofMillis(1100);
	private static final float[] NO_PHASE_MARKERS = new float[0];

	// NPCs with this param set to 1 have the game's boss bar show a percentage instead of exact
	// hitpoints. There is no gameval constant for it.
	private static final int PARAM_HP_PERCENTAGE_ONLY = 2289;

	// Varbits holding the hitpoint values the game's boss bar marks with a notch.
	private static final int[] PHASE_MARKER_VARBITS = {
		VarbitID.HPBAR_HUD_LOWER_THRESHOLD,
		VarbitID.HPBAR_HUD_UPPER_THRESHOLD,
		VarbitID.HPBAR_HUD_HP_1,
		VarbitID.HPBAR_HUD_HP_2,
	};

	private static final int HEADER_HEIGHT = 24;
	private static final int HEADER_BASELINE_GAP = 6;
	private static final int FOOTER_HEIGHT = 16;
	private static final int TEXT_INSET = 2;
	private static final int LEVEL_GAP = 12;
	// Room for the damage number is always reserved at this width, so the name doesn't move when it appears.
	private static final String DAMAGE_NUMBER_SIZING = "9999";
	// CAP_WIDTH is the room reserved at each end of the bar for the end plate and diamond, and
	// CAP_RISE is how far the plate reaches above and below the bar.
	private static final int CAP_PLATE_WIDTH = 2;
	private static final int DIAMOND_RADIUS = 3;
	private static final int CAP_WIDTH = CAP_PLATE_WIDTH + DIAMOND_RADIUS * 2 + 2;
	private static final int CAP_RISE = 3;
	private static final int ORNAMENT_OVERLAP = 2;

	private static final Color TRACK_TOP = new Color(6, 5, 5, 225);
	private static final Color TRACK_BOTTOM = new Color(26, 22, 22, 225);
	private static final Color TEXT_COLOR = new Color(232, 226, 212);
	private static final Color SUBTLE_TEXT_COLOR = new Color(200, 192, 176);
	private static final Color HEAL_TINT = new Color(150, 235, 160);

	private final Client client;
	private final BossHealthBarPlugin plugin;
	private final BossHealthBarConfig config;
	private final NPCManager npcManager;
	private final OrnamentRenderer ornamentRenderer = new OrnamentRenderer();

	/**
	 * What the bar shows about its opponent. Read every frame and kept, so the defeat animation
	 * can still draw it after the opponent has despawned.
	 */
	private static final class BarState
	{
		final String name;
		final int combatLevel;
		final Integer maxHealth;
		// Current health as a fraction ratio / scale.
		final int ratio;
		final int scale;
		// Whether ratio is the exact current health, with scale as the max health.
		final boolean exactHealth;
		// Whether the hitpoints text may only show a percentage.
		final boolean percentOnly;
		final float[] phaseMarkers;

		private BarState(String name, int combatLevel, Integer maxHealth, int ratio, int scale,
			boolean exactHealth, boolean percentOnly, float[] phaseMarkers)
		{
			this.name = name;
			this.combatLevel = combatLevel;
			this.maxHealth = maxHealth;
			this.ratio = ratio;
			this.scale = scale;
			this.exactHealth = exactHealth;
			this.percentOnly = percentOnly;
			this.phaseMarkers = phaseMarkers;
		}
	}

	private Actor trackedOpponent;
	private BarState lastState;
	// Health fractions from 0 to 1, or -1 before the first frame for an opponent. displayedFraction
	// is the fill, trailFraction the end of the damage trail, and actualFraction the real health,
	// which the fill grows towards after a heal.
	private float displayedFraction = -1f;
	private float trailFraction = -1f;
	private float actualFraction = -1f;
	private long lastRenderNanos;
	private long fadeStartNanos;
	private long defeatStartNanos;
	private int percentOnlyNpcId = -1;
	private boolean percentOnly;

	private FontStyle cachedFontStyle;
	private int cachedTextSize;
	private Font nameFont;
	private Font levelFont;
	private Font damageFont;
	private Font hpFont;

	@Inject
	private BossHealthBarOverlay(
		Client client,
		BossHealthBarPlugin plugin,
		BossHealthBarConfig config,
		NPCManager npcManager)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.npcManager = npcManager;

		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setResizable(false);
		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Modern Boss Healthbar");
	}

	/**
	 * Clears the animation state, so the next frame starts at the opponent's current health and
	 * fades in, instead of animating from the previous opponent's health.
	 */
	void resetAnimation()
	{
		displayedFraction = -1f;
		trailFraction = -1f;
		actualFraction = -1f;
		fadeStartNanos = 0;
		defeatStartNanos = 0;
		lastState = null;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final Actor opponent = plugin.getLastOpponent();
		final long now = System.nanoTime();

		if (opponent != trackedOpponent)
		{
			if (opponent == null && trackedOpponent != null && lastState != null && defeatStartNanos == 0
				&& config.showDefeatAnimation() && (trackedOpponent.isDead() || lastState.ratio <= 0))
			{
				// The opponent despawned as it died, so keep drawing it to play out the ending.
				defeatStartNanos = now;
			}

			if (opponent != null || defeatStartNanos == 0)
			{
				trackedOpponent = opponent;
				resetAnimation();
			}
		}

		final BarState state;
		if (opponent != null)
		{
			state = readState(opponent);
			if (state == null)
			{
				return null;
			}
			lastState = state;

			if (!opponent.isDead())
			{
				// Covers an opponent that comes back to life, or a defeat that was cut short.
				defeatStartNanos = 0;
			}
			else if (defeatStartNanos == 0 && config.showDefeatAnimation())
			{
				defeatStartNanos = now;
			}
		}
		else if (defeatStartNanos != 0 && lastState != null)
		{
			state = lastState;
		}
		else
		{
			return null;
		}

		final boolean defeated = defeatStartNanos != 0;
		float defeatOpacity = 1f;
		if (defeated)
		{
			final long elapsed = now - defeatStartNanos;
			final long hold = DEFEAT_HOLD.toNanos();
			final long fade = DEFEAT_FADE.toNanos();
			if (elapsed >= hold + fade)
			{
				return null;
			}
			if (elapsed > hold)
			{
				defeatOpacity = 1f - (elapsed - hold) / (float) fade;
			}
		}

		tick(defeated ? 0f : clamp01(state.ratio / (float) state.scale));

		final int width = config.barWidth();
		final int barHeight = config.barHeight();
		updateFonts();
		final float textScale = config.textSize() / 100f;
		final boolean showHeader = config.showBossName() || config.showDamageNumber();
		final int headerHeight = showHeader ? Math.round(HEADER_HEIGHT * textScale) : 0;
		final String hpText = defeated ? null : buildHitpointsText(state);
		final int footerHeight = hpText != null || defeated ? Math.round(FOOTER_HEIGHT * textScale) : 0;
		final HealthBarTheme theme = config.theme();

		final OrnamentRenderer.Ornament ornament = ornamentRenderer.getOrnament(
			config.ornamentStyle(), theme, ornamentScale(barHeight), barHeight / 2f + CAP_RISE + 0.5f);
		// Each piece's anchor sits on the bar's center line, just overlapping the end piece's tip on its side.
		final int leftExtent = ornament != null ? Math.max(0, ornament.left.anchorX - ORNAMENT_OVERLAP) : 0;
		final int rightExtent = ornament != null
			? Math.max(0, ornament.right.image.getWidth() - ornament.right.anchorX - ORNAMENT_OVERLAP) : 0;
		final int barCenterOffset = CAP_RISE + barHeight / 2;
		final int topOffset = ornament != null
			? Math.max(0, Math.max(ornament.left.anchorY, ornament.right.anchorY) - (headerHeight + barCenterOffset)) : 0;
		final int barY = headerHeight + CAP_RISE;

		final Composite originalComposite = graphics.getComposite();
		final float opacity = fadeInOpacity() * defeatOpacity;
		if (opacity < 1f)
		{
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp01(opacity)));
		}

		// Antialiased text with fractional metrics, so resized RuneScape fonts stay evenly spaced too.
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		graphics.translate(leftExtent, topOffset);

		if (showHeader)
		{
			drawHeader(graphics, state.name, state.combatLevel, width,
				headerHeight - Math.round(HEADER_BASELINE_GAP * textScale), theme);
		}

		drawBar(graphics, state.scale, barY, width, barHeight, theme,
			defeated ? NO_PHASE_MARKERS : state.phaseMarkers, lowHealthPulse(defeated));

		if (defeated)
		{
			graphics.setFont(hpFont);
			FontMetrics metrics = graphics.getFontMetrics();
			int textX = (width - metrics.stringWidth(DEFEATED_TEXT)) / 2;
			int baseline = barY + barHeight + CAP_RISE + metrics.getAscent() + 1;
			drawShadowedText(graphics, DEFEATED_TEXT, textX, baseline, brighten(theme.getAccentColor(), 0.45f), 1f);
		}
		else if (hpText != null)
		{
			graphics.setFont(hpFont);
			FontMetrics metrics = graphics.getFontMetrics();
			int textX = width - CAP_WIDTH - TEXT_INSET - metrics.stringWidth(hpText);
			int baseline = barY + barHeight + CAP_RISE + metrics.getAscent() + 1;
			drawShadowedText(graphics, hpText, textX, baseline, SUBTLE_TEXT_COLOR, 1f);
		}

		graphics.translate(-leftExtent, -topOffset);

		int totalHeight = topOffset + barY + barHeight + CAP_RISE + footerHeight;
		if (ornament != null)
		{
			final OrnamentRenderer.Piece left = ornament.left;
			final OrnamentRenderer.Piece right = ornament.right;
			final int centerY = topOffset + headerHeight + barCenterOffset;
			final int leftY = centerY - left.anchorY;
			final int rightY = centerY - right.anchorY;
			graphics.drawImage(left.image, leftExtent + ORNAMENT_OVERLAP - left.anchorX, leftY, null);
			graphics.drawImage(right.image, leftExtent + width - ORNAMENT_OVERLAP - right.anchorX, rightY, null);
			totalHeight = Math.max(totalHeight,
				Math.max(leftY + left.image.getHeight(), rightY + right.image.getHeight()));
		}

		graphics.setComposite(originalComposite);

		return new Dimension(leftExtent + width + rightExtent, totalHeight);
	}

	/**
	 * Reads what the bar should show for the opponent, or returns null when it shouldn't get a bar.
	 * Health comes from the game's boss bar when it tracks the opponent, then the Theatre of Blood
	 * boss bar, then the opponent's overhead health bar.
	 */
	private BarState readState(Actor opponent)
	{
		if (!plugin.shouldShowBarFor(opponent))
		{
			return null;
		}

		final boolean nativeBar = plugin.isNativeBarTracking(opponent);
		final boolean tobBar = plugin.isTobBarTracking(opponent);
		if ((nativeBar || tobBar) && !config.replaceNativeBossBar())
		{
			// The game's own boss bar is showing for this opponent, so don't draw a second one.
			return null;
		}

		Integer maxHealth = null;
		String name = Text.removeTags(opponent.getName());
		if (opponent instanceof NPC)
		{
			NPC npc = (NPC) opponent;
			NPCComposition composition = npc.getTransformedComposition();
			if (composition != null)
			{
				String longName = composition.getStringValue(ParamID.NPC_HP_NAME);
				if (!Strings.isNullOrEmpty(longName))
				{
					name = longName;
				}
			}
			maxHealth = npcManager.getHealth(npc.getId());
		}

		// The game's boss bar knows the exact hitpoints, and bosses using it may not send the
		// usual overhead health updates, so prefer it while it tracks this opponent.
		final int nativeMaxHealth = nativeBar ? client.getVarbitValue(VarbitID.HPBAR_HUD_BASEHP) : 0;
		if (nativeMaxHealth > 0)
		{
			return new BarState(name, opponent.getCombatLevel(), nativeMaxHealth,
				client.getVarbitValue(VarbitID.HPBAR_HUD_HP), nativeMaxHealth, true,
				isNativeBarPercentOnly(), readPhaseMarkers(nativeMaxHealth));
		}

		// The Theatre of Blood boss bar keeps updating while you attack other NPCs in the room, when
		// the boss's overhead health bar can disappear, so prefer it for the room's boss.
		final int tobMax = tobBar ? client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_MAX) : 0;
		if (tobMax > 0)
		{
			final int tobValue = Math.max(0, Math.min(tobMax, client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_VAL)));
			return new BarState(name, opponent.getCombatLevel(), maxHealth, tobValue, tobMax, false, false, NO_PHASE_MARKERS);
		}

		if (opponent.getHealthScale() > 0)
		{
			return new BarState(name, opponent.getCombatLevel(), maxHealth,
				opponent.getHealthRatio(), opponent.getHealthScale(), false, false, NO_PHASE_MARKERS);
		}

		return null;
	}

	/**
	 * Returns the positions of the game's boss bar phase markers as fractions of the bar, placed
	 * the same way the game places them: a marker for hitpoint value v sits at (v - 1) / max health.
	 */
	private float[] readPhaseMarkers(int maxHealth)
	{
		if (!config.showPhaseMarkers())
		{
			return NO_PHASE_MARKERS;
		}

		float[] markers = null;
		int count = 0;
		for (int varbit : PHASE_MARKER_VARBITS)
		{
			final int value = client.getVarbitValue(varbit);
			if (value > 0 && value <= maxHealth + 1)
			{
				if (markers == null)
				{
					markers = new float[PHASE_MARKER_VARBITS.length];
				}
				markers[count++] = clamp01((value - 1) / (float) maxHealth);
			}
		}
		return markers == null ? NO_PHASE_MARKERS : Arrays.copyOf(markers, count);
	}

	/**
	 * Whether the game's boss bar only shows a percentage for the NPC it tracks, in which case this
	 * bar doesn't show exact hitpoints either. Cached per tracked NPC ID.
	 */
	private boolean isNativeBarPercentOnly()
	{
		final int npcId = client.getVarpValue(VarPlayerID.HPBAR_HUD_NPC);
		if (npcId != percentOnlyNpcId)
		{
			final NPCComposition composition = npcId != -1 ? client.getNpcDefinition(npcId) : null;
			percentOnly = composition != null && composition.getIntValue(PARAM_HP_PERCENTAGE_ONLY) == 1;
			percentOnlyNpcId = npcId;
		}
		return percentOnly;
	}

	/**
	 * Advances the animations by the time since the last frame. Damage drops the fill at once, while
	 * the trail holds briefly after a hit and then drains down to it. Heals grow the fill towards
	 * the new health at the "Heal animation speed" rate.
	 *
	 * @param targetFraction the opponent's current health, from 0 to 1
	 */
	private void tick(float targetFraction)
	{
		long now = System.nanoTime();
		float dt = lastRenderNanos == 0 ? 0f : Math.min(0.25f, (now - lastRenderNanos) / 1_000_000_000f);
		lastRenderNanos = now;
		actualFraction = targetFraction;

		if (displayedFraction < 0f)
		{
			displayedFraction = targetFraction;
			trailFraction = targetFraction;
			fadeStartNanos = now;
			return;
		}

		if (targetFraction < displayedFraction)
		{
			displayedFraction = targetFraction;
		}
		else
		{
			float speed = config.animationSpeed();
			displayedFraction += (targetFraction - displayedFraction) * Math.min(1f, dt * speed);
			if (Math.abs(displayedFraction - targetFraction) < 0.001f)
			{
				displayedFraction = targetFraction;
			}
		}

		if (!config.showDamageTrail() || displayedFraction > trailFraction)
		{
			trailFraction = displayedFraction;
			return;
		}

		if (trailFraction > displayedFraction)
		{
			Instant lastHit = plugin.getLastHitTime();
			boolean holding = lastHit != null && Duration.between(lastHit, Instant.now()).compareTo(TRAIL_HOLD) < 0;
			if (!holding)
			{
				// Drain faster the larger the gap, with a minimum speed so the end doesn't crawl.
				float gap = trailFraction - displayedFraction;
				trailFraction -= Math.max(TRAIL_MIN_DRAIN_PER_SECOND, gap * TRAIL_CATCH_UP_RATE) * dt;
				if (trailFraction < displayedFraction)
				{
					trailFraction = displayedFraction;
				}
			}
		}
	}

	/**
	 * The ornament scale for a bar height: 1 up to a height of 8 pixels, then 4% larger per extra pixel.
	 */
	private static float ornamentScale(int barHeight)
	{
		return 1f + Math.max(0, barHeight - 8) * 0.04f;
	}

	/**
	 * Returns the strength of the low health pulse this frame, cycling between 0 and 1 while the
	 * displayed health is at or below the threshold. Returns 0 when the effect is off, the bar is
	 * empty, or the opponent is defeated.
	 */
	private float lowHealthPulse(boolean defeated)
	{
		if (defeated || !config.lowHealthEffect() || displayedFraction <= 0f
			|| displayedFraction > config.lowHealthThreshold() / 100f)
		{
			return 0f;
		}

		final long period = LOW_HEALTH_PULSE_PERIOD.toNanos();
		final double phase = (System.nanoTime() % period) / (double) period * 2 * Math.PI;
		return (float) (0.5 - 0.5 * Math.cos(phase));
	}

	/**
	 * Rebuilds the fonts when the font or text size setting has changed. The RuneScape fonts are
	 * only resized when the text size isn't 100%, since they look best at their original size.
	 */
	private void updateFonts()
	{
		final FontStyle style = config.fontStyle();
		final int textSize = config.textSize();
		if (style == cachedFontStyle && textSize == cachedTextSize)
		{
			return;
		}

		final float scale = textSize / 100f;
		switch (style)
		{
			case RUNESCAPE:
				nameFont = sized(FontManager.getRunescapeBoldFont(), scale);
				levelFont = sized(FontManager.getRunescapeSmallFont(), scale);
				damageFont = sized(FontManager.getRunescapeBoldFont(), scale);
				hpFont = sized(FontManager.getRunescapeSmallFont(), scale);
				break;
			case SANS_SERIF:
				nameFont = font(Font.SANS_SERIF, Font.BOLD, 15f * scale);
				levelFont = font(Font.SANS_SERIF, Font.PLAIN, 11f * scale);
				damageFont = font(Font.SANS_SERIF, Font.BOLD, 15f * scale);
				hpFont = font(Font.SANS_SERIF, Font.PLAIN, 11f * scale);
				break;
			case SERIF:
			default:
				nameFont = font(Font.SERIF, Font.PLAIN, 17f * scale);
				levelFont = font(Font.SERIF, Font.PLAIN, 12f * scale);
				damageFont = font(Font.SERIF, Font.PLAIN, 16f * scale);
				hpFont = font(Font.SERIF, Font.PLAIN, 12f * scale);
				break;
		}

		cachedFontStyle = style;
		cachedTextSize = textSize;
	}

	private static Font font(String family, int style, float size)
	{
		return new Font(family, style, 1).deriveFont(size);
	}

	/**
	 * Scales a font, rounding to a whole point size. Returns the font unchanged at a scale of 1.
	 */
	private static Font sized(Font font, float scale)
	{
		return scale == 1f ? font : font.deriveFont(Math.round(font.getSize2D() * scale) * 1f);
	}

	/**
	 * The bar's opacity from its fade in, going from 0 to 1 over FADE_IN_DURATION.
	 */
	private float fadeInOpacity()
	{
		if (fadeStartNanos == 0)
		{
			return 1f;
		}
		float elapsed = (System.nanoTime() - fadeStartNanos) / (float) FADE_IN_DURATION.toNanos();
		return clamp01(elapsed);
	}

	/**
	 * Draws the row above the bar: the name on the left, followed by the combat level if enabled,
	 * and the damage number on the right. A name too long for the space is cut off with an ellipsis.
	 */
	private void drawHeader(Graphics2D graphics, String name, int combatLevel, int width, int baseline, HealthBarTheme theme)
	{
		final int left = CAP_WIDTH + TEXT_INSET;
		final int right = width - CAP_WIDTH - TEXT_INSET;

		if (config.showBossName())
		{
			String levelText = null;
			int levelWidth = 0;
			if (config.showCombatLevel() && combatLevel > 0)
			{
				levelText = "LV " + combatLevel;
				graphics.setFont(levelFont);
				levelWidth = LEVEL_GAP + graphics.getFontMetrics().stringWidth(levelText);
			}

			int reserved = 0;
			if (config.showDamageNumber())
			{
				graphics.setFont(damageFont);
				reserved = graphics.getFontMetrics().stringWidth(DAMAGE_NUMBER_SIZING) + LEVEL_GAP;
			}

			graphics.setFont(nameFont);
			FontMetrics nameMetrics = graphics.getFontMetrics();
			String nameText = ellipsize(name, nameMetrics, right - left - reserved - levelWidth);
			drawShadowedText(graphics, nameText, left, baseline, TEXT_COLOR, 1f);

			if (levelText != null)
			{
				int levelX = left + nameMetrics.stringWidth(nameText) + LEVEL_GAP;
				graphics.setFont(levelFont);
				drawShadowedText(graphics, levelText, levelX, baseline, brighten(theme.getAccentColor(), 0.35f), 0.9f);
			}
		}

		if (config.showDamageNumber())
		{
			drawDamageNumber(graphics, right, baseline);
		}
	}

	/**
	 * Returns the text, shortened and ending in an ellipsis if needed to fit within maxWidth.
	 */
	private static String ellipsize(String text, FontMetrics metrics, int maxWidth)
	{
		if (metrics.stringWidth(text) <= maxWidth)
		{
			return text;
		}
		for (int end = text.length() - 1; end > 0; end--)
		{
			String candidate = text.substring(0, end).trim() + "…";
			if (metrics.stringWidth(candidate) <= maxWidth)
			{
				return candidate;
			}
		}
		return "…";
	}

	/**
	 * Draws the total of your recent hits, right-aligned to the given x. It fades out over the
	 * last part of the combo window and isn't drawn once the window has passed.
	 */
	private void drawDamageNumber(Graphics2D graphics, int right, int baseline)
	{
		final Instant lastDamage = plugin.getLastDamageDealtTime();
		final int damage = plugin.getComboDamage();
		if (lastDamage == null || damage <= 0)
		{
			return;
		}

		final long elapsed = Duration.between(lastDamage, Instant.now()).toMillis();
		final long window = BossHealthBarPlugin.DAMAGE_COMBO_WINDOW.toMillis();
		if (elapsed >= window)
		{
			return;
		}

		float alpha = 1f;
		if (elapsed > window - DAMAGE_NUMBER_FADE_MILLIS)
		{
			alpha = (window - elapsed) / (float) DAMAGE_NUMBER_FADE_MILLIS;
		}

		String text = String.valueOf(damage);
		graphics.setFont(damageFont);
		FontMetrics metrics = graphics.getFontMetrics();
		drawShadowedText(graphics, text, right - metrics.stringWidth(text), baseline, TEXT_COLOR, alpha);
	}

	/**
	 * Draws text with a two-layer drop shadow to the lower right, so it stays readable over bright
	 * backgrounds.
	 */
	private static void drawShadowedText(Graphics2D graphics, String text, int x, int y, Color color, float alpha)
	{
		alpha = clamp01(alpha);
		graphics.setColor(new Color(0, 0, 0, Math.round(90 * alpha)));
		graphics.drawString(text, x + 2, y + 2);
		graphics.setColor(new Color(0, 0, 0, Math.round(200 * alpha)));
		graphics.drawString(text, x + 1, y + 1);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(255 * alpha)));
		graphics.drawString(text, x, y);
	}

	/**
	 * Draws the bar itself: the track, damage trail, heal preview, fill, frame, phase markers, end
	 * pieces and the big hit flash.
	 *
	 * @param scale the opponent's health scale, used to decide whether the last hit was big
	 * @param y the top of the bar
	 * @param width the full width including the end pieces
	 * @param height the bar height
	 * @param phaseMarkers marker positions as fractions of the bar
	 * @param lowHealthPulse the strength of the low health effect this frame, from 0 (off) to 1
	 */
	private void drawBar(Graphics2D graphics, int scale, int y, int width, int height, HealthBarTheme theme,
		float[] phaseMarkers, float lowHealthPulse)
	{
		final Color accent = theme.getAccentColor();
		final int barX = CAP_WIDTH;
		final int barWidth = width - CAP_WIDTH * 2;

		final int innerX = barX + 1;
		final int innerY = y + 1;
		final int innerWidth = barWidth - 2;
		final int innerHeight = height - 2;

		final Color baseFill = lerp(theme.getLowColor(), theme.getHighColor(), clamp01(displayedFraction));
		final Color fill = lowHealthPulse > 0f ? brighten(baseFill, 0.55f * lowHealthPulse) : baseFill;
		final int fillWidth = Math.round(innerWidth * clamp01(displayedFraction));

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		drawBackdrop(graphics, barX, y, barWidth, height);

		if (lowHealthPulse > 0f && fillWidth > 0)
		{
			// Glow around the fill, drawn as widening translucent rounded rectangles.
			final Color glow = brighten(baseFill, 0.2f);
			for (int i = 4; i >= 1; i--)
			{
				final int spread = i * 3;
				final int arc = height + spread * 2;
				graphics.setColor(withAlpha(glow, Math.round(lowHealthPulse * 130 / i)));
				graphics.fillRoundRect(innerX - spread, y - spread, fillWidth + spread * 2, height + spread * 2, arc, arc);
			}
		}

		// Pixel-aligned rectangles look crisper without antialiasing.
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

		graphics.setPaint(new GradientPaint(0, y, TRACK_TOP, 0, y + height, TRACK_BOTTOM));
		graphics.fillRect(barX, y, barWidth, height);
		graphics.setPaint(null);

		if (config.showDamageTrail() && trailFraction > displayedFraction)
		{
			Color trail = theme.getTrailColor();
			int trailWidth = Math.round(innerWidth * clamp01(trailFraction));
			graphics.setPaint(verticalSheen(innerY, innerHeight, trail, 0.2f, 0.35f));
			graphics.fillRect(innerX, innerY, trailWidth, innerHeight);
			graphics.setPaint(null);
		}

		// After a heal, the new health shows at once as a lighter section that the fill grows into.
		final int healWidth = Math.round(innerWidth * clamp01(actualFraction));
		if (healWidth > fillWidth)
		{
			final Color heal = lerp(theme.getHighColor(), HEAL_TINT, 0.45f);
			graphics.setPaint(verticalSheen(innerY, innerHeight, heal, 0.35f, 0.3f));
			graphics.fillRect(innerX, innerY, healWidth, innerHeight);
			graphics.setPaint(null);
		}

		if (fillWidth > 0)
		{
			graphics.setPaint(verticalSheen(innerY, innerHeight, fill, 0.3f, 0.45f));
			graphics.fillRect(innerX, innerY, fillWidth, innerHeight);
			graphics.setPaint(null);

			// Highlight along the top edge of the fill.
			graphics.setColor(withAlpha(brighten(fill, 0.6f), 90));
			graphics.drawLine(innerX, innerY, innerX + fillWidth - 1, innerY);
		}

		// Shadow along the top edge of the empty part of the track.
		final int filledWidth = Math.max(fillWidth, healWidth);
		if (filledWidth < innerWidth)
		{
			graphics.setColor(new Color(0, 0, 0, 120));
			graphics.drawLine(innerX + filledWidth, innerY, innerX + innerWidth - 1, innerY);
		}

		// While health is low, the frame color pulses towards the fill color.
		final Color frame = lowHealthPulse > 0f ? lerp(accent, brighten(baseFill, 0.3f), 0.85f * lowHealthPulse) : accent;
		drawFrame(graphics, barX, y, barWidth, height, frame);

		// Each phase marker is a light line crossing the bar and extending past the frame, with a
		// dark line beside it so it shows on both the fill and the empty track.
		for (float marker : phaseMarkers)
		{
			final int markerX = innerX + Math.round((innerWidth - 1) * marker);
			graphics.setColor(new Color(0, 0, 0, 170));
			graphics.drawLine(markerX + 1, innerY, markerX + 1, innerY + innerHeight - 1);
			graphics.setColor(withAlpha(brighten(accent, 0.55f), 235));
			graphics.drawLine(markerX, y - 2, markerX, y + height + 1);
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		drawFinials(graphics, barX, y, barWidth, height, accent);
		drawUnderline(graphics, barX, y + height + 1, barWidth, accent);

		if (config.flashOnBigHits())
		{
			Instant lastHit = plugin.getLastHitTime();
			if (lastHit != null && plugin.getLastHitAmount() >= scale * BIG_HIT_FRACTION)
			{
				Duration since = Duration.between(lastHit, Instant.now());
				if (since.compareTo(FLASH_DURATION) < 0)
				{
					float alpha = 1f - (since.toMillis() / (float) FLASH_DURATION.toMillis());
					graphics.setStroke(new BasicStroke(1f));
					graphics.setColor(new Color(1f, 0.95f, 0.85f, clamp01(alpha) * 0.8f));
					graphics.drawRect(barX, y, barWidth - 1, height - 1);
				}
			}
		}
	}

	/**
	 * Draws a 1 pixel frame in the given color, with a dark outline around it and a lighter top edge.
	 */
	private static void drawFrame(Graphics2D graphics, int x, int y, int width, int height, Color accent)
	{
		graphics.setStroke(new BasicStroke(1f));
		graphics.setColor(new Color(6, 5, 5));
		graphics.drawRect(x - 1, y - 1, width + 1, height + 1);

		graphics.setColor(accent);
		graphics.drawRect(x, y, width - 1, height - 1);

		graphics.setColor(withAlpha(brighten(accent, 0.35f), 120));
		graphics.drawLine(x + 1, y, x + width - 2, y);
	}

	/**
	 * Draws a soft dark shadow behind the bar, made of widening translucent rounded rectangles.
	 */
	private static void drawBackdrop(Graphics2D graphics, int barX, int y, int barWidth, int height)
	{
		for (int i = 3; i >= 1; i--)
		{
			graphics.setColor(new Color(0, 0, 0, 34));
			graphics.fillRoundRect(barX - i * 2, y - i - 1, barWidth + i * 4, height + i * 2 + 2, height + i * 2, height + i * 2);
		}
	}

	/**
	 * A vertical gradient from a lighter to a darker shade of the base color, used for the fill,
	 * trail and heal sections.
	 *
	 * @param lift how far towards white the top is, from 0 to 1
	 * @param shade how far towards black the bottom is, from 0 to 1
	 */
	private static LinearGradientPaint verticalSheen(int y, int height, Color base, float lift, float shade)
	{
		return new LinearGradientPaint(
			0, y, 0, y + Math.max(1, height),
			new float[]{0f, 0.45f, 1f},
			new Color[]{brighten(base, lift), base, darken(base, shade)});
	}

	/**
	 * Draws the end pieces on both sides: a thin plate reaching just above and below the bar, and a
	 * small diamond outside it.
	 */
	private static void drawFinials(Graphics2D graphics, int barX, int y, int barWidth, int height, Color accent)
	{
		final int top = y - CAP_RISE;
		final int bottom = y + height + CAP_RISE;
		final int midY = y + height / 2;
		final int rightEdge = barX + barWidth;
		final int diamond = DIAMOND_RADIUS;

		graphics.setPaint(new GradientPaint(0, top, brighten(accent, 0.35f), 0, bottom, darken(accent, 0.35f)));
		graphics.fillRect(barX - CAP_PLATE_WIDTH, top, CAP_PLATE_WIDTH, bottom - top);
		graphics.fillRect(rightEdge, top, CAP_PLATE_WIDTH, bottom - top);

		final int leftCx = barX - CAP_PLATE_WIDTH - diamond - 1;
		final int rightCx = rightEdge + CAP_PLATE_WIDTH + diamond + 1;
		final int[] ys = {midY - diamond, midY, midY + diamond, midY};
		final int[] leftXs = {leftCx, leftCx + diamond, leftCx, leftCx - diamond};
		final int[] rightXs = {rightCx, rightCx + diamond, rightCx, rightCx - diamond};
		graphics.fillPolygon(leftXs, ys, 4);
		graphics.fillPolygon(rightXs, ys, 4);
		graphics.setPaint(null);

		graphics.setStroke(new BasicStroke(1f));
		graphics.setColor(new Color(6, 5, 5, 170));
		graphics.drawPolygon(leftXs, ys, 4);
		graphics.drawPolygon(rightXs, ys, 4);

		// A highlight pixel near the top of each diamond.
		graphics.setColor(withAlpha(brighten(accent, 0.7f), 200));
		graphics.fillRect(leftCx, midY - diamond + 1, 1, 1);
		graphics.fillRect(rightCx, midY - diamond + 1, 1, 1);
	}

	/**
	 * Draws a 1 pixel line below the bar that fades out towards both ends.
	 */
	private static void drawUnderline(Graphics2D graphics, int barX, int y, int barWidth, Color accent)
	{
		graphics.setPaint(new LinearGradientPaint(
			barX, 0, barX + barWidth, 0,
			new float[]{0f, 0.5f, 1f},
			new Color[]{withAlpha(accent, 0), withAlpha(brighten(accent, 0.2f), 80), withAlpha(accent, 0)}));
		graphics.fillRect(barX, y + 1, barWidth, 1);
		graphics.setPaint(null);
	}

	/**
	 * Builds the text below the bar for the "Hitpoints text" setting, or returns null when it's off.
	 * Falls back to a percentage when the max health is unknown or the boss is percentage only.
	 */
	private String buildHitpointsText(BarState state)
	{
		HitpointsTextMode mode = config.hitpointsTextMode();
		if (mode == HitpointsTextMode.NONE)
		{
			return null;
		}

		// Like the game's bar, only show 0% and 100% when the opponent is really dead or full.
		int percent = (int) Math.round(100.0 * state.ratio / state.scale);
		if (state.ratio > 0 && state.ratio < state.scale)
		{
			percent = Math.max(1, Math.min(99, percent));
		}
		String percentText = percent + "%";

		if (mode == HitpointsTextMode.PERCENTAGE || state.maxHealth == null || state.percentOnly)
		{
			return percentText;
		}

		int currentHealth = state.exactHealth ? state.ratio : estimateHealth(state.ratio, state.scale, state.maxHealth);
		String hpText = currentHealth + " / " + state.maxHealth;

		if (mode == HitpointsTextMode.HITPOINTS)
		{
			return hpText;
		}

		return hpText + "   " + percentText;
	}

	/**
	 * Estimates the opponent's current health from its health ratio and known max health. The game
	 * sends healthRatio = 1 + (healthScale - 1) * health / maxHealth, rounded down, for health above
	 * 0. This returns the middle of the range of health values that give the observed ratio.
	 */
	private static int estimateHealth(int ratio, int healthScale, int maxHealth)
	{
		if (ratio <= 0)
		{
			return 0;
		}

		int minHealth = 1;
		int maxHealthForRatio;
		if (healthScale > 1)
		{
			if (ratio > 1)
			{
				minHealth = (maxHealth * (ratio - 1) + healthScale - 2) / (healthScale - 1);
			}
			maxHealthForRatio = (maxHealth * ratio - 1) / (healthScale - 1);
			if (maxHealthForRatio > maxHealth)
			{
				maxHealthForRatio = maxHealth;
			}
		}
		else
		{
			maxHealthForRatio = maxHealth;
		}

		return (minHealth + maxHealthForRatio + 1) / 2;
	}

	private static float clamp01(float value)
	{
		return Math.max(0f, Math.min(1f, value));
	}
}
