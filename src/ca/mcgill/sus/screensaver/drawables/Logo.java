package ca.mcgill.sus.screensaver.drawables;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.SpriteManager;

public class Logo implements Drawable {
	
	private int x, y, w = 1024, h = 768;
	private Runnable onChange;
	private final BufferedImage logo = SpriteManager.getInstance().getSprite("logo_nice.png");
	
	public Logo(int interval) {
//		final int fps = 60, sleepMs = 1000 / fps;
//		new Thread(new Runnable() {
//			final double speed = 5;
//			Random random = new Random();
//			double dir = random.nextDouble() * Math.PI, //random angle
//			hdist, vdist; //cumulative hspeed/vspeed
//			@Override
//			public void run() {
//				System.out.println(dir);
//				while (!Thread.interrupted()) {
//					long startTime = System.currentTimeMillis();
//					if (y + logo.getHeight() > h) {
//						dir = random.nextDouble() * (2 * Math.PI);
////						continue;
////						dir = Math.PI + dir;
//						y -= 100;
//					}
//					hdist += speed * Math.cos(dir);
//					vdist += speed * Math.sin(dir);
//					if (hdist > 1) {
//						x += (int) hdist;
//						hdist %= 1;
////						hdist = 0;
//					}
//					if (vdist > 1) {
//						y += (int) vdist;
//						vdist %= 1;
////						vdist = 0;
//					}
//					onChange();
//					try {
//						Thread.sleep(sleepMs - (System.currentTimeMillis() - startTime));
//					} catch (InterruptedException e) {
//						break;
//					}
//				}
//			}
//		}, "Logo Update").start();
		Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
			private final Random random = new Random();
			@Override
			public void run() {
				if (w > 0 && h > 0) {
					x = random.nextInt(w - logo.getWidth());
					y = random.nextInt(h - logo.getHeight() - 200) + 200;
					onChange();
				}
			}
		}, 0, interval, TimeUnit.SECONDS);
	}

	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		w = canvasWidth;
		h = canvasHeight;
//		g.drawImage(logo, canvasWidth/2 - logo.getWidth() / 2, canvasHeight/2 - logo.getHeight() / 2, null);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
		g.drawImage(logo, x, y, null);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
	}

	@Override
	public Logo setOnChange(Runnable r) {
		this.onChange = r;
		return this;
	}
	
	private void onChange() {
		if (onChange != null) {
			onChange.run();
		}
	}

}
