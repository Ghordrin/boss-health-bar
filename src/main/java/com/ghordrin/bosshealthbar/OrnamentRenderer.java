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

import static com.ghordrin.bosshealthbar.ColorUtil.brighten;
import static com.ghordrin.bosshealthbar.ColorUtil.darken;
import static com.ghordrin.bosshealthbar.ColorUtil.lerp;
import static com.ghordrin.bosshealthbar.ColorUtil.withAlpha;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
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
 * Builds the ornaments drawn at both ends of the bar. The godsword style draws a hilt on the left
 * and a blade tip on the right. Pieces are drawn from vector shapes into images, which are cached
 * until the style, theme, scale or bar height changes.
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

	private OrnamentStyle cachedStyle;
	private HealthBarTheme cachedTheme;
	private float cachedScale;
	private float cachedBarHalfExtent;
	private Ornament cached;

	/**
	 * Returns the ornament for the given style, or null when the style is NONE.
	 *
	 * @param scale the size multiplier for the pieces
	 * @param barHalfExtent half the height of the bar including its end plates, in pixels
	 */
	Ornament getOrnament(OrnamentStyle style, HealthBarTheme theme, float scale, float barHalfExtent)
	{
		if (style == null || style == OrnamentStyle.NONE)
		{
			return null;
		}

		if (cached == null || style != cachedStyle || theme != cachedTheme || scale != cachedScale
			|| barHalfExtent != cachedBarHalfExtent)
		{
			cached = buildGodsword(theme, scale, barHalfExtent);
			cachedStyle = style;
			cachedTheme = theme;
			cachedScale = scale;
			cachedBarHalfExtent = barHalfExtent;
		}
		return cached;
	}

	private static Ornament buildGodsword(HealthBarTheme theme, float scale, float barHalfExtent)
	{
		// The blade tip covers the right end piece, so its height has to match the bar's.
		final float half = barHalfExtent / scale;
		final float length = Math.max(12f, half * 2.6f);
		final Piece hilt = rasterise(-29f, -22f, 7f, 22f, scale, g -> drawGodswordHilt(g, theme));
		final Piece tip = rasterise(-9f, -half - 1.5f, length + 1.5f, half + 1.5f, scale,
			g -> drawBladeTip(g, theme, half, length));
		return new Ornament(hilt, tip);
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
	 * pommel, with gems in the theme's fill color.
	 */
	private static void drawGodswordHilt(Graphics2D g, HealthBarTheme theme)
	{
		final Color accent = theme.getAccentColor();
		final Color gemColor = theme.getHighColor();
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

		metal(g, pommel, accent, -5.5f, 5.5f);
		metal(g, guard, accent, -20f, 20f);

		gem(g, gx, 0f, 2.4f, gemColor);
		gem(g, -23f, 0f, 1.5f, gemColor);
	}

	/**
	 * Draws the blade tip over the bar's right end piece.
	 *
	 * @param half half the blade's height
	 * @param length how far the tip extends past the anchor
	 */
	private static void drawBladeTip(Graphics2D g, HealthBarTheme theme, float half, float length)
	{
		final Color steel = lerp(theme.getAccentColor(), STEEL, 0.55f);

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
