package ca.mcgill.sus.screensaver;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BlurredScreensaverFrame extends ScreensaverFrame {
	private static final long serialVersionUID = 4848839375816808489L;
	protected final Stage stage;
	private BufferedImage background;
	private boolean alreadyVisible = false;

	public BlurredScreensaverFrame(int display) {
		super(display, Main.flags.contains("/w"));
		stage = new Stage(30);
		this.add(stage);
		if (Main.LOGGED_IN) {
			stage.setDrawableOpacity(0);
			background = Util.screenshot(display);
		} else {
			Rectangle bounds;
			GraphicsDevice[] gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
			if (this.display >= 0 && this.display < gd.length) {
				bounds = gd[this.display].getDefaultConfiguration().getBounds();
			} else {
				throw new RuntimeException("Invalid display index");
			}
			background = Util.loadBackground(bounds.getHeight() > bounds.getWidth());
		}
		stage.setBackground(background);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		stage.repaint();
		if (visible && Main.LOGGED_IN && !alreadyVisible) {
			alreadyVisible = true;
			final int maxBlur = 32, frameCount = 8;
			final double ms = 3000;
			final CubicBezier easeInOut = CubicBezier.create(0.42, 0, 0.58, 1.0, (1000.0 / 60.0 / ms) / 4.0);
			final BlockingQueue<BufferedImage> frames = new LinkedBlockingQueue<>();
			final BufferedImage bg = background;
			new Thread("Blurify") {
				public void run() {
					for (int i = 0; i < frameCount; i++) {
						frames.offer(Util.blur((i + 1) * (maxBlur / frameCount), bg));
					}
				};
			}.start();
			new Thread("Fadeify") {
				public void run() {
					try {
						while (!DataFetch.getInstance().isLoaded() || frames.isEmpty()) Thread.sleep(400);
					} catch (InterruptedException e) {
					}
					BufferedImage b1 = background, b2 = background;
					int maxFrost = 0x77;
					long start = System.nanoTime();
					for (int i = 0, f = 0, lastF = -1; i < maxBlur;) {
						try {
							long t = (System.nanoTime() - start) / 1000000;
							double progress = Math.min(1, easeInOut.calc((double) t / ms));
							i = (int) (progress * maxBlur);
							f = i / frameCount;
							if (f > lastF) {
								b1 = b2;
								try {
									for (int b = 0; b < f - lastF; b++) b2 = frames.poll((long) (ms - t), TimeUnit.MILLISECONDS);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								if (b2 == null) b2 = b1;
							}
							lastF = f;
							BufferedImage composite = new BufferedImage(b1.getWidth(), b2.getHeight(), BufferedImage.TYPE_INT_RGB);
							Graphics2D g = composite.createGraphics();
							g.drawImage(b1, 0, 0, null);
							g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (i % frameCount) / frameCount));
							stage.setDrawableOpacity(i < maxBlur * 0.2 ? 0 : 1.25f * i / maxBlur - 0.25f);
							g.drawImage(b2, 0, 0, null);
							g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
							int alpha = (int) (((double) i / maxBlur) * maxFrost);
							g.setColor(new Color((alpha << 24) | 0xffffff, true));
							g.fillRect(0, 0, getWidth(), getHeight()); 
							g.dispose();
							stage.setBackground(composite);
							stage.setDirty(true);
						} catch (RuntimeException e) {
							e.printStackTrace();
						}
					}
				};
			}.start();
		}
	}
}
