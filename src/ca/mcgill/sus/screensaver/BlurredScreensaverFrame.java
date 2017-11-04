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
		super(display, false);
		stage = new Stage();
		if (Main.LOGGED_IN) stage.setDrawableOpacity(0);
		this.add(stage);
		if (Main.LOGGED_IN) {
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
					BufferedImage b1 = background, b2 = background;
					int maxFrost = 0x77;
					for (int i = 0; i < maxBlur; i++) {
						if (i % frameCount == 0) {
							b1 = b2;
							try {
								b2 = frames.poll(10, TimeUnit.SECONDS);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
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
						stage.safeRepaint();
					}
				};
			}.start();
		}
	}
}
