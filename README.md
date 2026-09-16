# Modern Boss Healthbar

A RuneLite plugin that replaces the opponent health bar with a themed bar.

## Features

- **Layout** — a long, thin bar in a frame with end pieces. The opponent's name
  is shown above the left end, and the bar fades in when a fight starts.
- **Ornaments** — decorations on both ends of the bar, colored to match the
  theme and resizable. A godsword hilt and blade tip, a staff with a glowing
  orb, a skull and bone, a rolled scroll, or bevelled brackets. Can be turned
  off.
- **Damage number** — the total damage of your recent hits, shown above the
  right end of the bar. It resets a few seconds after your last hit.
- **Damage trail** — damage lowers the fill immediately, and the lost health
  stays visible as a lighter section for a moment before draining away. Heals
  refill the bar gradually.
- **Defeat animation** — when the opponent dies, the empty bar stays with a
  "Defeated" label for a moment, then fades out.
- **Intro animation** — when the bar appears it rises into place, widens from
  its center and sweeps the fill up to the opponent's health. It can also just
  slide in, just expand, or only fade in.
- **Low health effect** — the fill pulses and glows once the opponent's health
  is at or below a set threshold (25% by default).
- **Fonts** — RuneLite's font picker: the RuneScape fonts, any font installed on
  your computer, or a .ttf/.otf file added to the `.runelite/fonts` folder,
  with adjustable size, bold and italic.
- **Themes** — one for each god, in their colors: Zamorak (default), Saradomin,
  Guthix, Armadyl, Bandos, Zaros, Seren, Tumeken, Elidinis, Ralos and Ranul.
  Each theme has its own fill, trail and frame colors, and the fill color
  changes as the opponent's health drops.
- **Custom colors** — choose the Custom theme to pick every color with a color
  picker: the fill at full and low health, damage trail, frame, ornament metal
  and gems, and each piece of text.
- **Optional extras** (off by default) — the combat level next to the name,
  hitpoints text below the bar (percentage, value when the max hitpoints are
  known, or both), and a border flash on big hits.
- **Fit to game view** — in fixed mode or a small window, the bar gets narrower
  so it doesn't take up too much of the game view.
- The bar can be moved like any other RuneLite overlay.

## How it works

The bar follows the NPC you are interacting with, the same way RuneLite's
"Opponent Information" plugin does. Attacking a lower-level NPC, such as one a
boss spawns, doesn't move the bar away from a living boss. Health is read from
the game's health ratio and scale, and converted to hitpoints using RuneLite's
NPC hitpoints data when the max hitpoints are known.

With "Only show for bosses" on, superior slayer monsters you spawn also get the
bar, whatever their combat level. The plugin spots them from the game message
sent when one appears, and picks the NPC that spawned closest to you.

The plugin only shows information the game already shows. It doesn't predict
mechanics or add timers.

By default, the plugin turns off the health bar of RuneLite's "Opponent
Information" plugin while it runs, so two health bars aren't shown at once.
The setting is restored when this plugin is turned off. This can be disabled
in the config.

## The game's own boss health bars

Some bosses show the game's own health bar at the top of the screen. While
you fight one of them, the plugin hides that bar and shows this bar instead,
using the game's exact hitpoints and phase markers. For bosses the game only
shows as a percentage, the hitpoints text also only shows a percentage.

Theatre of Blood has its own boss health bar. While it's up, this bar follows
the room's boss, which is taken to be the nearby attackable NPC with the
highest combat level. It uses the health from the Theatre of Blood bar and
hides that bar.

Turn off "Replace game's boss health bar" to keep the game's bars. This bar is
then hidden for those bosses, so two bars aren't shown at once.

## Configuration

Right-click the bar, or find "Modern Boss Healthbar" in the RuneLite plugin
panel, to configure:

- **Preview** — show the bar with a sample opponent while you aren't fighting,
  to try out settings
- **Appearance** — theme, ornaments and their size, bar width and height, fit to game view, damage trail, phase
  markers, flash on big hits, intro animation, defeat animation, low health effect and
  threshold, heal animation speed
- **Custom colors** — the colors used by the Custom theme
- **Text** — font, whether to show the name, combat level and damage
  number, hitpoints text (none / percentage / hitpoints / both)
- **Behaviour** — only show for bosses and the minimum combat level for that,
  whether to also show it for superior slayer monsters, how long the bar stays after you stop attacking, whether to replace the
  game's boss health bars, and whether to hide the "Opponent Information"
  health bar
