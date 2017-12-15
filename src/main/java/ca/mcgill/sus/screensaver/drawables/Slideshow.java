package ca.mcgill.sus.screensaver.drawables;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ca.mcgill.sus.screensaver.CubicBezier;
import ca.mcgill.sus.screensaver.DataFetch;
import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.Util;
import ca.mcgill.sus.screensaver.io.Slide;

public class Slideshow implements Drawable {
	
	private int w, h;
	private final int interval, transition;
	private final CubicBezier easeInOut;
	private final List<Slide> slides = DataFetch.getInstance().slides;
	private double progress;
	private Slide slide, nextSlide;
	private final AtomicBoolean dirty = new AtomicBoolean();
	
	
	public Slideshow(int interval, int transition, int height) {
		this.h = height;
		this.interval = interval;
		this.transition = transition;
		easeInOut = CubicBezier.create(0.42, 0, 0.58, 1.0, (1000.0 / 60.0 / transition) / 4.0);
	}

	@Override
	public void draw(Graphics2D g, BufferedImage canvas, int canvasWidth, int canvasHeight) {
		if (slide == null) return;
		w = canvasWidth;
		boolean invert = Util.luminanceAvg(canvas, 0, 0, w, h) < 0.4;
		int nextSlideX = (int) ((1 - progress) * w);
		drawCropped(g, invert ? slide.light : slide.dark, nextSlideX - w, 0, w, h);
		if (nextSlideX != w) {
			drawCropped(g, invert ? nextSlide.light : nextSlide.dark, nextSlideX, 0, w, h);
		}
	}
	
	private static void drawCropped(Graphics2D g, BufferedImage img, int x, int y, int w, int h) {
		int imgW = img.getWidth(), imgH = img.getHeight();
		if ((double) imgW / imgH < (double) w / h) {
			int croppedH = h * imgW / w;
			img = img.getSubimage(0, imgH / 2 - croppedH / 2, imgW, croppedH);
		} else {
			int croppedW = w * imgH / h;
			img = img.getSubimage(imgW / 2 - croppedW / 2, 0, croppedW, imgH);
		}
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.drawImage(img, x, y, w, h, null);
	}

	@Override
	public void step(long notUsed) {
		if (slides.isEmpty()) return;
		long t = System.currentTimeMillis();
		int totalDuration = interval + transition,
		slideIndex = (int) ((double) t / totalDuration) % slides.size();
		double p = 0;
		if (t % totalDuration >= interval) {
			p = easeInOut.calc(((double) (t % totalDuration) - interval) / transition);
		}
		if (p < progress || slide == null) {
			if (nextSlide == null) nextSlide = slides.get(slideIndex);
			slide = nextSlide;
			nextSlide = slides.get((slideIndex + 1) % slides.size());
		}
		if (p != progress || t == 0) this.setDirty(true);
		progress = p;
	}

	@Override
	public boolean isDirty() {
		return this.dirty.get();
	}

	@Override
	public void setDirty(boolean dirty) {
		this.dirty.set(dirty);
	}
}
