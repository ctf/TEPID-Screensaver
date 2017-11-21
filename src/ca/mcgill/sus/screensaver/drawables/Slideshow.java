package ca.mcgill.sus.screensaver.drawables;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Queue;

import ca.mcgill.sus.screensaver.CubicBezier;
import ca.mcgill.sus.screensaver.DataFetch;
import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.Stage;
import ca.mcgill.sus.screensaver.Util;

public class Slideshow implements Drawable {
	
	private int w, h;
	private long startTime;
	private Stage parent;
	private final int interval, transition;
	private final CubicBezier easeInOut;
	private final Queue<BufferedImage> slides = DataFetch.getInstance().slides;
	private Iterator<BufferedImage> sliderator = slides.iterator();
	private double lastProgress;
	private BufferedImage slide, nextSlide;
	
	
	public Slideshow(int interval, int transition, int height) {
		this.h = height;
		this.interval = interval;
		this.transition = transition;
		easeInOut = CubicBezier.create(0.42, 0, 0.58, 1.0, (1000.0 / 60.0 / transition) / 4.0);
		new Thread("Repaint Slideshow") {
			public void run() {
				while (!Thread.interrupted()) {
					if (parent != null && !slides.isEmpty()) parent.safeRepaint();
					Util.sleep(16);
				}
			}
		}.start();
	}

	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		w = canvasWidth;
		if (startTime == 0) startTime = System.nanoTime();
		long t = (System.nanoTime() - startTime) / 1000000;
		int totalDuration = interval + transition;
		double progress = 0;
		if (t % totalDuration >= interval) {
			progress = easeInOut.calc(((double) (t % totalDuration) - interval) / transition);
		}
		if (slides.isEmpty()) return;
		if (progress < lastProgress || slide == null) {
			if (nextSlide == null) nextSlide = getNextSlide();
			slide = nextSlide;
			nextSlide = getNextSlide();
		}
		lastProgress = progress;
		int nextSlideX = (int) ((1 - progress) * w);
		drawCropped(g, slide, nextSlideX - w, 0, w, h);
		if (nextSlideX != w) {
			drawCropped(g, nextSlide, nextSlideX, 0, w, h);
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
	
	private BufferedImage getNextSlide() {
		if (slides.isEmpty()) return null;
		if (!sliderator.hasNext()) sliderator = slides.iterator();
		return sliderator.next();
	}

	@Override
	public void setParent(Stage parent) {
		this.parent = parent;
	}
}
