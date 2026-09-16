package com.ghordrin.bosshealthbar;

import static com.ghordrin.bosshealthbar.ColorUtil.brighten;
import static com.ghordrin.bosshealthbar.ColorUtil.darken;
import static com.ghordrin.bosshealthbar.ColorUtil.lerp;
import static com.ghordrin.bosshealthbar.ColorUtil.withAlpha;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Builds the ornaments drawn at both ends of the bar. Weapon styles have a distinct left and right
 * piece, such as the godsword's hilt and blade tip, while the decorative styles draw the same piece
 * on both ends, mirrored. Pieces are drawn from vector shapes into images, which are cached until
 * the style, colors, scale or bar height changes.
 */
class OrnamentRenderer
{
	/**
	 * One ornament image. The anchor is the pixel that the overlay places on the bar's vertical
	 * center line, at the outer tip of the end piece on that side.
	 */
	static final class Piece
	{
		final BufferedImage image;
		final int anchorX;
		final int anchorY;

		private Piece(BufferedImage image, int anchorX, int anchorY)
		{
			this.image = image;
			this.anchorX = anchorX;
			this.anchorY = anchorY;
		}
	}

	/**
	 * The pieces for the left and right ends of the bar.
	 */
	static final class Ornament
	{
		final Piece left;
		final Piece right;

		private Ornament(Piece left, Piece right)
		{
			this.left = left;
			this.right = right;
		}
	}

	private static final Color OUTLINE = new Color(8, 6, 6, 215);
	private static final Color SHADOW = new Color(0, 0, 0, 110);
	private static final Color LEATHER = new Color(74, 50, 36);
	private static final Color STEEL = new Color(214, 216, 222);
	private static final Color WOOD = new Color(92, 64, 40);
	private static final Color BONE = new Color(228, 220, 198);
	private static final Color PARCHMENT = new Color(226, 208, 164);
	private static final Color HOLLOW = new Color(18, 12, 12, 230);

	private OrnamentStyle cachedStyle;
	private ThemeColors cachedColors;
	private float cachedScale;
	private float cachedBarHalfExtent;
	private Ornament cached;

	/**
	 * Returns the ornament for the given style, or null when the style is NONE.
	 *
	 * @param scale the size multiplier for the pieces
	 * @param barHalfExtent half the height of the bar including its end plates, in pixels
	 */
	Ornament getOrnament(OrnamentStyle style, ThemeColors colors, float scale, float barHalfExtent)
	{
		if (style == null || style == OrnamentStyle.NONE)
		{
			return null;
		}

		if (cached == null || style != cachedStyle || !colors.equals(cachedColors) || scale != cachedScale
			|| barHalfExtent != cachedBarHalfExtent)
		{
			cached = build(style, colors, scale, barHalfExtent / scale);
			cachedStyle = style;
			cachedColors = colors;
			cachedScale = scale;
			cachedBarHalfExtent = barHalfExtent;
		}
		return cached;
	}

	/**
	 * Builds the pieces for a style.
	 *
	 * @param half half the height of the bar including its end plates, in unscaled units, so that a
	 *            piece sized against it keeps matching the bar at any scale
	 */
	private static Ornament build(OrnamentStyle style, ThemeColors colors, float scale, float half)
	{
		switch (style)
		{
			case STAFF:
				return buildStaff(colors, scale);
			case BONE:
				return buildBone(colors, scale, half);
			case SCROLL:
				return buildScroll(colors, scale, half);
			case BRACKET:
				return buildBracket(colors, scale, half);
			case GODSWORD:
			default:
				return buildGodsword(colors, scale, half);
		}
	}

	private static Ornament buildGodsword(ThemeColors colors, float scale, float half)
	{
		// The blade tip covers the right end piece, so its height has to match the bar's.
		final float length = Math.max(12f, half * 2.6f);
		final Piece hilt = rasterise(-29f, -22f, 7f, 22f, scale, g -> drawGodswordHilt(g, colors));
		final Piece tip = rasterise(-9f, -half - 1.5f, length + 1.5f, half + 1.5f, scale,
			g -> drawBladeTip(g, colors, half, length));
		return new Ornament(hilt, tip);
	}

	private static Ornament buildStaff(ThemeColors colors, float scale)
	{
		final Piece butt = rasterise(-17f, -4f, 4f, 4f, scale, g -> drawStaffButt(g, colors));
		final Piece head = rasterise(-4f, -11f, 20f, 11f, scale, g -> drawStaffHead(g, colors));
		return new Ornament(butt, head);
	}

	private static Ornament buildBone(ThemeColors colors, float scale, float half)
	{
		final float length = Math.max(12f, half * 2.6f);
		final Piece skull = rasterise(-16f, -9f, 1f, 9f, scale, g -> drawSkull(g, colors));
		final Piece spike = rasterise(-6f, -5f, length + 2f, 5f, scale, g -> drawBoneSpike(g, colors, length));
		return new Ornament(skull, spike);
	}

	private static Ornament buildScroll(ThemeColors colors, float scale, float half)
	{
		// The roll reaches a little past the bar's end plates on both sides.
		final float reach = half + 3f;
		final Piece right = rasterise(-7f, -reach - 4f, 9f, reach + 4f, scale, g -> drawScroll(g, colors, reach));
		return new Ornament(mirror(right), right);
	}

	private static Ornament buildBracket(ThemeColors colors, float scale, float half)
	{
		final float reach = half + 2f;
		final Piece right = rasterise(-5f, -reach - 1.5f, 8f, reach + 1.5f, scale, g -> drawBracket(g, colors, reach));
		return new Ornament(mirror(right), right);
	}

	/**
	 * Returns the piece flipped left to right, for the styles that draw the same shape on both ends.
	 */
	private static Piece mirror(Piece piece)
	{
		final BufferedImage source = piece.image;
		final int width = source.getWidth();
		final int height = source.getHeight();
		final BufferedImage flipped = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = flipped.createGraphics();
		g.drawImage(source, width, 0, 0, height, 0, 0, width, height, null);
		g.dispose();
		return new Piece(flipped, width - piece.anchorX, piece.anchorY);
	}

	/**
	 * Creates an image large enough for the given bounds, in unscaled units around the anchor, and
	 * runs the painter with the graphics translated to the anchor and scaled.
	 */
	private static Piece rasterise(float minX, float minY, float maxX, float maxY, float scale, Consumer<Graphics2D> painter)
	{
		final int pad = 2;
		final int anchorX = pad + (int) Math.ceil(-minX * scale);
		final int anchorY = pad + (int) Math.ceil(-minY * scale);
		final int width = anchorX + (int) Math.ceil(maxX * scale) + pad;
		final int height = anchorY + (int) Math.ceil(maxY * scale) + pad;

		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.translate(anchorX, anchorY);
		g.scale(scale, scale);
		painter.accept(g);
		g.dispose();
		return new Piece(image, anchorX, anchorY);
	}

	/**
	 * Draws the godsword hilt: a guard over the bar's left end piece, a wrapped grip and a diamond
	 * pommel, with gems in the gem color.
	 */
	private static void drawGodswordHilt(Graphics2D g, ThemeColors colors)
	{
		final Color metalColor = colors.getOrnament();
		final Color gemColor = colors.getGem();
		// The guard's center x, over the end piece's diamond.
		final float gx = 1f;

		final Path2D guard = new Path2D.Float();
		guard.moveTo(gx + 3f, -4f);
		guard.quadTo(gx + 2f, -12f, gx + 4f, -17f);
		guard.lineTo(gx, -20f);
		guard.quadTo(gx - 4f, -12f, gx - 3f, -4f);
		guard.lineTo(gx - 3f, 4f);
		guard.quadTo(gx - 4f, 12f, gx, 20f);
		guard.lineTo(gx + 4f, 17f);
		guard.quadTo(gx + 2f, 12f, gx + 3f, 4f);
		guard.closePath();

		final Shape grip = new RoundRectangle2D.Float(-20f, -2.5f, 18.5f, 5f, 2f, 2f);

		final Path2D pommel = new Path2D.Float();
		pommel.moveTo(-23f, -5.5f);
		pommel.lineTo(-19f, 0f);
		pommel.lineTo(-23f, 5.5f);
		pommel.lineTo(-27f, 0f);
		pommel.closePath();

		shadow(g, grip, pommel, guard);

		g.setPaint(new GradientPaint(0, -2.5f, brighten(LEATHER, 0.2f), 0, 2.5f, darken(LEATHER, 0.4f)));
		g.fill(grip);
		// Diagonal wrap lines, clipped to the grip.
		final Shape clip = g.getClip();
		g.clip(grip);
		g.setStroke(new BasicStroke(0.7f));
		g.setColor(withAlpha(Color.BLACK, 120));
		for (float x = -22f; x < 0f; x += 3f)
		{
			g.draw(new Line2D.Float(x, -3f, x + 2.5f, 3f));
		}
		g.setClip(clip);
		outline(g, grip);

		metal(g, pommel, metalColor, -5.5f, 5.5f);
		metal(g, guard, metalColor, -20f, 20f);

		gem(g, gx, 0f, 2.4f, gemColor);
		gem(g, -23f, 0f, 1.5f, gemColor);
	}

	/**
	 * Draws the blade tip over the bar's right end piece.
	 *
	 * @param half half the blade's height
	 * @param length how far the tip extends past the anchor
	 */
	private static void drawBladeTip(Graphics2D g, ThemeColors colors, float half, float length)
	{
		final Color steel = lerp(colors.getOrnament(), STEEL, 0.55f);

		final Path2D tip = new Path2D.Float();
		tip.moveTo(-8f, -half);
		tip.lineTo(0f, -half);
		tip.quadTo(length * 0.6f, -half * 0.75f, length, 0f);
		tip.quadTo(length * 0.6f, half * 0.75f, 0f, half);
		tip.lineTo(-8f, half);
		tip.closePath();

		shadow(g, tip);
		metal(g, tip, steel, -half, half);

		// A dark groove along the middle and a highlight along the top edge.
		g.setStroke(new BasicStroke(0.8f));
		g.setColor(withAlpha(darken(steel, 0.5f), 120));
		g.draw(new Line2D.Float(-7f, 0f, length * 0.55f, 0f));

		final Path2D edge = new Path2D.Float();
		edge.moveTo(-7f, -half + 0.9f);
		edge.lineTo(0f, -half + 0.9f);
		edge.quadTo(length * 0.6f, -half * 0.75f + 0.9f, length - 1.5f, -0.3f);
		g.setColor(withAlpha(brighten(steel, 0.7f), 140));
		g.draw(edge);
	}

	/**
	 * Draws the butt of the staff over the bar's left end piece: a length of shaft, a metal ferrule
	 * and a spike.
	 */
	private static void drawStaffButt(Graphics2D g, ThemeColors colors)
	{
		final Color metalColor = colors.getOrnament();

		final Shape shaft = new RoundRectangle2D.Float(-9f, -2.2f, 12f, 4.4f, 1.5f, 1.5f);
		final Shape ferrule = new RoundRectangle2D.Float(-11.5f, -2.8f, 4f, 5.6f, 1.5f, 1.5f);

		final Path2D spike = new Path2D.Float();
		spike.moveTo(-11f, -2.2f);
		spike.lineTo(-16f, 0f);
		spike.lineTo(-11f, 2.2f);
		spike.closePath();

		shadow(g, shaft, ferrule, spike);

		wood(g, shaft);
		metal(g, spike, metalColor, -2.2f, 2.2f);
		metal(g, ferrule, metalColor, -2.8f, 2.8f);
	}

	/**
	 * Draws the head of the staff over the bar's right end piece: a clawed mount holding a glowing
	 * orb in the gem color.
	 */
	private static void drawStaffHead(Graphics2D g, ThemeColors colors)
	{
		final Color orbColor = colors.getGem();
		final float cx = 10f;
		final float r = 4.6f;

		final Shape shaft = new RoundRectangle2D.Float(-3f, -2.2f, 9f, 4.4f, 1.5f, 1.5f);

		// A claw that reaches around the orb from behind, open towards the right.
		final Path2D claw = new Path2D.Float();
		claw.moveTo(4f, -2.6f);
		claw.quadTo(cx - 1f, -r - 2.5f, cx + 2.5f, -r - 0.5f);
		claw.quadTo(cx - 2.5f, -r + 1f, 6.5f, 0f);
		claw.quadTo(cx - 2.5f, r - 1f, cx + 2.5f, r + 0.5f);
		claw.quadTo(cx - 1f, r + 2.5f, 4f, 2.6f);
		claw.closePath();

		shadow(g, shaft, claw);

		wood(g, shaft);
		metal(g, claw, colors.getOrnament(), -r - 2.5f, r + 2.5f);

		// The orb's glow, drawn as widening translucent circles behind it.
		for (int i = 3; i >= 1; i--)
		{
			final float spread = i * 1.6f;
			g.setColor(withAlpha(orbColor, 40 / i));
			g.fill(new Ellipse2D.Float(cx - r - spread, -r - spread, (r + spread) * 2, (r + spread) * 2));
		}
		gem(g, cx, 0f, r, orbColor);
	}

	/**
	 * Draws a skull over the bar's left end piece, with a spark of the gem color in each socket.
	 */
	private static void drawSkull(Graphics2D g, ThemeColors colors)
	{
		final Color bone = lerp(BONE, colors.getOrnament(), 0.3f);
		final float cx = -8f;

		final Shape cranium = new Ellipse2D.Float(cx - 6.5f, -7f, 13f, 11f);

		final Path2D jaw = new Path2D.Float();
		jaw.moveTo(cx - 4f, 2f);
		jaw.lineTo(cx - 3.5f, 7f);
		jaw.lineTo(cx + 3.5f, 7f);
		jaw.lineTo(cx + 4f, 2f);
		jaw.closePath();

		shadow(g, cranium, jaw);

		g.setPaint(new GradientPaint(0, -7f, brighten(bone, 0.3f), 0, 7f, darken(bone, 0.45f)));
		g.fill(jaw);
		g.fill(cranium);
		outline(g, jaw);
		outline(g, cranium);

		g.setColor(HOLLOW);
		g.fill(new Ellipse2D.Float(cx - 4.6f, -3.6f, 3.6f, 3.4f));
		g.fill(new Ellipse2D.Float(cx + 1f, -3.6f, 3.6f, 3.4f));
		g.fill(new Ellipse2D.Float(cx - 1f, 0.2f, 2f, 2.2f));

		g.setColor(withAlpha(brighten(colors.getGem(), 0.4f), 210));
		g.fill(new Ellipse2D.Float(cx - 3.6f, -2.6f, 1.4f, 1.4f));
		g.fill(new Ellipse2D.Float(cx + 2f, -2.6f, 1.4f, 1.4f));

		g.setStroke(new BasicStroke(0.7f));
		g.setColor(withAlpha(darken(bone, 0.6f), 160));
		for (float x = cx - 2f; x <= cx + 2f; x += 2f)
		{
			g.draw(new Line2D.Float(x, 2.5f, x, 6.5f));
		}
	}

	/**
	 * Draws a bone over the bar's right end piece, tapering from a knobbed base to a point.
	 *
	 * @param length how far the bone extends past the anchor
	 */
	private static void drawBoneSpike(Graphics2D g, ThemeColors colors, float length)
	{
		final Color bone = lerp(BONE, colors.getOrnament(), 0.3f);

		final Path2D shaft = new Path2D.Float();
		shaft.moveTo(-2f, -1.9f);
		shaft.quadTo(length * 0.6f, -1.6f, length, 0f);
		shaft.quadTo(length * 0.6f, 1.6f, -2f, 1.9f);
		shaft.closePath();

		final Shape upperKnob = new Ellipse2D.Float(-4.5f, -4.2f, 5f, 4.4f);
		final Shape lowerKnob = new Ellipse2D.Float(-4.5f, -0.2f, 5f, 4.4f);

		shadow(g, shaft, upperKnob, lowerKnob);

		g.setPaint(new GradientPaint(0, -4.2f, brighten(bone, 0.3f), 0, 4.2f, darken(bone, 0.45f)));
		g.fill(shaft);
		g.fill(upperKnob);
		g.fill(lowerKnob);
		outline(g, shaft);
		outline(g, upperKnob);
		outline(g, lowerKnob);
	}

	/**
	 * Draws a rolled scroll over the bar's end piece: the sheet carrying on over the bar, the roll
	 * itself with a wax seal in the gem color, and a metal knob above and below it.
	 *
	 * @param reach how far the roll reaches above and below the bar's center line
	 */
	private static void drawScroll(Graphics2D g, ThemeColors colors, float reach)
	{
		final Color metalColor = colors.getOrnament();
		final Color paper = lerp(PARCHMENT, metalColor, 0.15f);
		final float sheetReach = reach - 2.5f;

		final Shape sheet = new RoundRectangle2D.Float(-6.5f, -sheetReach, 10f, sheetReach * 2, 1.5f, 1.5f);
		final Shape roll = new RoundRectangle2D.Float(1.5f, -reach, 5.5f, reach * 2, 4.5f, 4.5f);

		shadow(g, sheet, roll);

		g.setPaint(new GradientPaint(0, -sheetReach, brighten(paper, 0.25f), 0, sheetReach, darken(paper, 0.35f)));
		g.fill(sheet);
		outline(g, sheet);

		// Ruled lines across the sheet.
		g.setStroke(new BasicStroke(0.6f));
		g.setColor(withAlpha(darken(paper, 0.6f), 90));
		for (float y = -sheetReach + 2.5f; y < sheetReach - 1.5f; y += 3f)
		{
			g.draw(new Line2D.Float(-5f, y, 0.5f, y));
		}

		// The roll is lit from its left, so it reads as a cylinder rather than a flat strip.
		g.setPaint(new LinearGradientPaint(
			1.5f, 0f, 7f, 0f,
			new float[]{0f, 0.4f, 1f},
			new Color[]{darken(paper, 0.45f), brighten(paper, 0.3f), darken(paper, 0.3f)}));
		g.fill(roll);
		outline(g, roll);

		knob(g, 4.25f, -reach, metalColor);
		knob(g, 4.25f, reach, metalColor);
		gem(g, 4.25f, 0f, 2f, colors.getGem());
	}

	/**
	 * Draws a bevelled bracket over the bar's end piece, with a rivet in the gem color.
	 *
	 * @param reach how far the bracket reaches above and below the bar's center line
	 */
	private static void drawBracket(Graphics2D g, ThemeColors colors, float reach)
	{
		final Color metalColor = colors.getOrnament();

		final Path2D plate = new Path2D.Float();
		plate.moveTo(-4f, -reach);
		plate.lineTo(2f, -reach);
		plate.lineTo(4.5f, -reach + 2.5f);
		plate.lineTo(4.5f, -2.5f);
		plate.lineTo(7f, 0f);
		plate.lineTo(4.5f, 2.5f);
		plate.lineTo(4.5f, reach - 2.5f);
		plate.lineTo(2f, reach);
		plate.lineTo(-4f, reach);
		plate.closePath();

		shadow(g, plate);
		metal(g, plate, metalColor, -reach, reach);

		g.setStroke(new BasicStroke(0.8f));
		g.setColor(withAlpha(brighten(metalColor, 0.7f), 130));
		g.draw(new Line2D.Float(-2.5f, -reach + 1.2f, -2.5f, reach - 1.2f));

		gem(g, 1f, 0f, 1.7f, colors.getGem());
	}

	/**
	 * Draws a small metal diamond, used for the caps at the ends of the scroll's roll.
	 */
	private static void knob(Graphics2D g, float cx, float cy, Color color)
	{
		final Path2D diamond = new Path2D.Float();
		diamond.moveTo(cx, cy - 2.2f);
		diamond.lineTo(cx + 2.2f, cy);
		diamond.lineTo(cx, cy + 2.2f);
		diamond.lineTo(cx - 2.2f, cy);
		diamond.closePath();
		metal(g, diamond, color, cy - 2.2f, cy + 2.2f);
	}

	/**
	 * Fills a shape with a vertical wood gradient and outlines it, for the staff's shaft.
	 */
	private static void wood(Graphics2D g, Shape shape)
	{
		g.setPaint(new GradientPaint(0, -2.2f, brighten(WOOD, 0.35f), 0, 2.2f, darken(WOOD, 0.4f)));
		g.fill(shape);
		outline(g, shape);
	}

	/**
	 * Fills the shapes in a translucent black, offset down and to the right, as a drop shadow.
	 */
	private static void shadow(Graphics2D g, Shape... shapes)
	{
		final AffineTransform base = g.getTransform();
		g.translate(0.8, 1.2);
		g.setColor(SHADOW);
		for (Shape shape : shapes)
		{
			g.fill(shape);
		}
		g.setTransform(base);
	}

	/**
	 * Fills a shape with a vertical gradient from a lighter to a darker shade of the color between
	 * the given y values, then outlines it.
	 */
	private static void metal(Graphics2D g, Shape shape, Color color, float top, float bottom)
	{
		g.setPaint(new GradientPaint(
			0, top, brighten(color, 0.5f),
			0, bottom, darken(color, 0.5f)));
		g.fill(shape);
		outline(g, shape);
	}

	private static void outline(Graphics2D g, Shape shape)
	{
		g.setStroke(new BasicStroke(1f));
		g.setColor(OUTLINE);
		g.draw(shape);
	}

	/**
	 * Draws a round gem with a gradient, an outline and a small highlight.
	 */
	private static void gem(Graphics2D g, float cx, float cy, float r, Color color)
	{
		final Shape stone = new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2);
		g.setPaint(new GradientPaint(
			cx, cy - r, brighten(color, 0.45f),
			cx, cy + r, darken(color, 0.45f)));
		g.fill(stone);
		g.setStroke(new BasicStroke(0.8f));
		g.setColor(OUTLINE);
		g.draw(stone);
		g.setColor(withAlpha(Color.WHITE, 200));
		g.fill(new Ellipse2D.Float(cx - r * 0.5f, cy - r * 0.65f, r * 0.5f, r * 0.45f));
	}
}
