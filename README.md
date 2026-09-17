# Modern Boss Healthbar

RuneLite already shows you an opponent's health. It's just a small bar tucked up
in the corner that's easy to forget about. I wanted something I'd actually look
at during a fight, so I wrote this.

It swaps that out for a wide bar with the opponent's name on it, a frame,
decorations on both ends and a bit of animation.

## The idea

Two things bugged me about the default bar. It's small, and it doesn't really
react to anything — a hit for 70 looks the same as a hit for 3.

So this one tries to make a fight readable at a glance:

- Damage drops the fill straight away, but the health you just took off hangs
  around as a lighter section for a moment before draining. You get to see how
  big the hit was after it's landed.
- Your own recent hits add up into a single number above the right end, which
  resets a couple of seconds after you stop hitting.
- The fill shifts color as health drops, and pulses once the opponent is nearly
  dead.

That's about it. It doesn't know anything the game hasn't already told you —
no timers, no attack prediction, no mechanic warnings. Same information, drawn
bigger.

You can drag it around like any other RuneLite overlay.

## Settings

Right-click the bar, or find "Modern Boss Healthbar" in the plugin panel.

**Preview** draws a fake opponent that loops through losing and regaining
health, so you can mess with settings without going and finding something to
fight. Off by default.

### Appearance

- **Theme** — one per god: Zamorak (the default), Saradomin, Guthix, Armadyl,
  Bandos, Zaros, Seren, Tumeken, Elidinis, Ralos and Ranul. Each one sets the
  fill, trail and frame colors. Picking Custom unlocks the Custom colors section
  below, prefilled with whatever theme you were on, so you're not starting from
  scratch.
- **Ornaments** — the decorations on each end. Godsword, Staff and Skull and
  bone draw a different piece on each side; Scroll and Brackets draw the same
  piece mirrored. Set it to None if you'd rather just have the bar.
- **Ornament size** — 50–200%, relative to whatever size suits your bar height.
  Default 100.
- **Bar width** — 200 to 1400 pixels. Default 600.
- **Fit to game view** — shrinks the bar when it'd eat too much of the screen,
  like in fixed mode or a small window. The height and text stay put so it
  doesn't turn into a smudge. On by default.
- **Bar height** — 4 to 24 pixels, just the bar itself and not the text around
  it. Default 7.
- **Show damage trail** — the lighter section described above. Turn it off and
  the fill just drops.
- **Show phase markers** — when this bar is standing in for the game's own boss
  bar, draw the same phase markers that bar would have. On by default.
- **Flash on big hits** — flashes the border when a hit takes off a big chunk.
  Off by default; it's a bit much at some bosses.
- **Intro animation** — how the bar shows up for a new opponent. Fade is just an
  opacity fade, Slide in rises into place, Expand widens from the middle with
  the fill sweeping up to the opponent's health, and Slide in and expand does
  both. That last one is the default.
- **Defeat animation** — holds the empty bar with a "Defeated" label for a
  moment when the opponent dies, then fades it. On by default.
- **Low health effect** — makes the fill pulse and glow when the opponent is
  low. On by default.
- **Low health threshold** — where "low" starts, 5–50%. Default 25.
- **Heal animation speed** — 1 to 10, how fast the bar refills when something
  heals. Default 6. Damage always drops instantly regardless; only healing is
  animated.

### Custom colors

Ignored unless Theme is set to Custom. There's one for the fill at full health
and another for the fill at zero — set both to the same color if you don't want
it to shift. The rest are the damage trail, the frame (which also covers the end
pieces, underline and phase markers), the ornament metal, the ornament gems, the
name and damage text, the combat level, the hitpoints text and the "Defeated"
label.

The ornament metal color gets mixed with whatever the piece is supposed to be
made of, so a bone or a blade tip won't come out as pure metal.

### Text

- **Font** — RuneLite's font picker, so you get the RuneScape fonts, anything
  installed on your machine, and any `.ttf` or `.otf` you drop in
  `.runelite/fonts`. Size, bold and italic too. The combat level and hitpoints
  text use a smaller version of whatever you pick.
- **Show name** — the opponent's name above the left end. On by default.
- **Show combat level** — next to the name. Off by default.
- **Show damage number** — your recent hits totalled above the right end. On by
  default.
- **Hitpoints text** — below the bar: off, a percentage, the actual value, or
  both. Off by default. The value is worked out from the health ratio the game
  sends plus RuneLite's known max hitpoints, so it's an estimate unless the
  game's boss bar is up and giving exact numbers. Bosses the game only ever
  shows as a percentage stay a percentage here too.

### Behaviour

- **Only show for bosses** — on by default, otherwise you get a giant health bar
  for every cow you hit. An opponent qualifies if it's at or above the minimum
  combat level, or the game's own boss bar is showing it, or it's a superior
  slayer monster and that setting is on.
- **Minimum combat level** — default 150.
- **Show for superior slayer monsters** — superiors get a bar no matter their
  combat level. On by default.
- **Hide after** — how long the bar sticks around once you stop attacking, 1–60
  seconds, default 5. If the game's own boss bar is still showing the opponent,
  the bar stays anyway.
- **Replace game's boss health bar** — see below. On by default.
- **Hide vanilla opponent overlay** — turns off the health bar in RuneLite's
  "Opponent Information" plugin while this one is running, so you're not looking
  at two health bars. On by default, and it puts your setting back when you
  disable this plugin.

## How it picks what to show

It follows whatever you're interacting with, the same way the Opponent
Information plugin does. If you attack something weaker mid-fight — one of the
smaller things a boss throws at you, say — the bar stays on the boss instead of
hopping to it.

Superiors are a special case because the game doesn't flag them in any way the
plugin can read directly. It watches for the chat message, then picks the
attackable NPC that spawned nearest to you around the same tick. It's a guess,
but a reliable one in practice.

## The game's own boss bars

Some bosses come with a health bar at the top of the screen. When you're
fighting one of those, this plugin hides it and uses its numbers instead — which
is nice, because that bar knows exact hitpoints and phase markers rather than
the rounded ratio you normally get.

Theatre of Blood has its own version of that bar. It doesn't say which NPC it
belongs to, so the plugin works it out by taking the nearby attackable NPC with
the highest combat level (and the biggest one, if there's a tie).

If you'd rather keep the game's bars, turn off "Replace game's boss health bar".
This bar then gets out of the way for those bosses so you're not looking at two
at once.

## Building

Standard RuneLite external plugin setup — `./gradlew run` launches a
development client with the plugin loaded. Java 11.
