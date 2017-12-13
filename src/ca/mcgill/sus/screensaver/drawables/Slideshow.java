package ca.mcgill.sus.screensaver.drawables;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import ca.mcgill.sus.screensaver.CubicBezier;
import ca.mcgill.sus.screensaver.DataFetch;
import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.Util;
import ca.mcgill.sus.screensaver.io.Slide;

public class Slideshow implements Drawable {
	
	private int w, h;
	private long startTime;
	private final int interval, transition;
	private final CubicBezier easeInOut;
	private final Queue<Slide> slides = DataFetch.getInstance().slides;
	private Iterator<Slide> sliderator = slides.iterator();
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
	
	private Slide getNextSlide() {
		if (slides.isEmpty()) return null;
		if (!sliderator.hasNext()) sliderator = slides.iterator();
		return sliderator.next();
	}

	@Override
	public void step(long timestamp) {
		//forcibly sync the starting timestamp to the nearest hour
		if (startTime == 0) {
			long sync = System.currentTimeMillis() - (System.currentTimeMillis() / 1000 / 60 / 60) * 1000 * 60 * 60;
			startTime = timestamp - sync; 
		}
		long t = timestamp - startTime;
		int totalDuration = interval + transition;
		double p = 0;
		if (t % totalDuration >= interval) {
			p = easeInOut.calc(((double) (t % totalDuration) - interval) / transition);
		}
		if (slides.isEmpty()) return;
		if (p < progress || slide == null) {
			if (nextSlide == null) nextSlide = getNextSlide();
			slide = nextSlide;
			nextSlide = getNextSlide();
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
