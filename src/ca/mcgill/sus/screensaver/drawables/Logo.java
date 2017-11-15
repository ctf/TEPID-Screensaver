package ca.mcgill.sus.screensaver.drawables;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.SpriteManager;
import ca.mcgill.sus.screensaver.Stage;

public class Logo implements Drawable {
	
	private int x, y, w = 1024, h = 768;
	private final BufferedImage logo = SpriteManager.getInstance().getSprite("logo_nice.png");
	private Stage parent;
	
	public Logo(int interval) {
		Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
			private final Random random = new Random();
			@Override
			public void run() {
				if (w > 0 && h > 0) {
					x = random.nextInt(w - logo.getWidth());
					y = random.nextInt(h - logo.getHeight() - 200) + 200;
					if (parent != null) parent.safeRepaint();
				}
			}
		}, 0, interval, TimeUnit.SECONDS);
	}

	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		w = canvasWidth;
		h = canvasHeight;
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
		g.drawImage(logo, x, y, null);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
	}

	@Override
	public void setParent(Stage parent) {
		this.parent = parent;
	}
}
