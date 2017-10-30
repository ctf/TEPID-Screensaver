package ca.mcgill.sus.screensaver;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

public class BlurredScreensaverFrame extends ScreensaverFrame {
	private static final long serialVersionUID = 4848839375816808489L;
	protected final Stage stage;
	private final boolean kiosk;

	public BlurredScreensaverFrame(int display, boolean kiosk) {
		super(display, kiosk);
		this.kiosk = kiosk;
		stage = new Stage();
		if (Main.LOGGED_IN) stage.setDrawableOpacity(0);
		this.add(stage);
	}

	@Override
	public void setVisible(boolean visible) {
		BufferedImage background;
		if (Main.LOGGED_IN && !this.kiosk) {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] gd = ge.getScreenDevices();
			Rectangle bounds;
			if (this.display >= 0 && this.display < gd.length) {
				bounds = gd[this.display].getDefaultConfiguration().getBounds();
			} else {
				throw new RuntimeException("Invalid display index");
			}
			background = Util.screenshot(bounds);
			super.setVisible(visible);
			stage.setBackground(background);
		} else {
			try {
				InputStream bgJpg; 
				File localBg = new File(System.getenv("systemdrive") + "\\CTF Screensaver.jpg");
				if (localBg.exists()) {
					bgJpg = new FileInputStream(localBg);
				} else {
					bgJpg = BlurredScreensaverFrame.class.getResourceAsStream("background/bg.jpg");
				}
				background =  Util.convert(ImageIO.read(bgJpg), BufferedImage.TYPE_INT_RGB);
			} catch (IOException e) {
				background = null;
				System.err.println("Could not load background image...");
			}
			super.setVisible(visible);
			stage.setBackground(background);
		}
		stage.repaint();
		if (Main.LOGGED_IN) {
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
				g.fillRect(0, 0, this.getWidth(), this.getHeight()); 
				g.dispose();
				stage.setBackground(composite);
				stage.repaint();
			}
		}
		stage.repaint();
	}
}
