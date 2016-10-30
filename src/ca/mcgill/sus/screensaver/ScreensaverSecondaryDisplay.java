package ca.mcgill.sus.screensaver;

import static ca.mcgill.sus.screensaver.ScreensaverMainDisplay.blur;

import java.awt.AWTException;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import ca.mcgill.sus.screensaver.drawables.Logo;
import ca.mcgill.sus.screensaver.drawables.OfficeHoursMarquee;
import ca.mcgill.sus.screensaver.filters.Filter;
import ca.mcgill.sus.screensaver.filters.HardLight;

public class ScreensaverSecondaryDisplay extends Screensaver {
	private static final long serialVersionUID = 4848839375816808489L;
	private final Canvas canvas;
	private final boolean kiosk;

	public ScreensaverSecondaryDisplay(int display, boolean kiosk) {
		super(display, kiosk);
		this.kiosk = kiosk;
		canvas = new Canvas(kiosk);
		this.add(canvas);
	}

	public BufferedImage screenshot(Rectangle bounds) {
		try {
			return new Robot().createScreenCapture(bounds);
		} catch (AWTException e) {
			throw new RuntimeException("Could not take screenshot", e);
		}
	}

	@Override
	public void setVisible(boolean visible) {
		BufferedImage background;
		Rectangle bounds;
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gd = ge.getScreenDevices();
		if (this.display >= 0 && this.display < gd.length) {
			bounds = gd[this.display].getDefaultConfiguration().getBounds();
		} else {
			throw new RuntimeException("Invalid display index");
		}
		if (Main.LOGGED_IN && !this.kiosk) {
			background = screenshot(bounds);
			super.setVisible(visible);
			canvas.setBackground(background);
		} else {
			try {
				InputStream bgJpg; 
				File localBg = new File(System.getenv("systemdrive") 
						+ "\\CTF Screensaver" + (bounds.getHeight() > bounds.getWidth() ? " Vertical" : "") + ".jpg");
				if (localBg.exists()) {
					bgJpg = new FileInputStream(localBg);
				} else {
					bgJpg = ScreensaverMainDisplay.class.getResourceAsStream("background/bg.jpg");
				}
				background =  Util.convert(ImageIO.read(bgJpg), BufferedImage.TYPE_INT_RGB);				} catch (IOException e) {
					background = null;
					System.err.println("Could not load background image...");
				}
				super.setVisible(visible);
				canvas.setBackground(background);
			}
			canvas.repaint();
			if (Main.LOGGED_IN) {
			final int maxBlur = 32, frameCount = 8;
			final BlockingQueue<BufferedImage> frames = new LinkedBlockingQueue<>();
			final BufferedImage bg = background;
			new Thread("Blurify") {
				public void run() {
					for (int i = 0; i < frameCount; i++) {
						frames.offer(blur((i + 1) * (maxBlur / frameCount), bg));
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
				g.drawImage(b2, 0, 0, null);
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				int alpha = (int) (((double) i / maxBlur) * maxFrost);
				g.setColor(new Color((alpha << 24) | 0xffffff, true));
				g.fillRect(0, 0, this.getWidth(), this.getHeight());
				g.dispose();
				canvas.setBackground(composite);
				canvas.repaint();
			}
		}
		canvas.create();
		canvas.repaint();
	}

	public static class Canvas extends JPanel {
		private static final long serialVersionUID = 2466660164920195989L;

		private final Queue<Drawable> drawables = new ConcurrentLinkedQueue<Drawable>();
		private BufferedImage background;

		public void setBackground(BufferedImage bg) {
			this.background = bg;
		}

		public Canvas(boolean kiosk) {
			super(true);
			this.setBackground(new Color(0x292929));
		}

		Filter overlay = new HardLight();
		@Override
		public void paint(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics;
			if (background != null) {
				g.drawImage(background, getWidth() / 2 - background.getWidth() / 2, getHeight() / 2 - background.getHeight() / 2, null);
				if (!drawables.isEmpty()) {
					BufferedImage fg = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
					g = fg.createGraphics();
					for (Drawable d : drawables) {
						d.draw(g, this.getWidth(), this.getHeight());
					}
					g.dispose();
					g = (Graphics2D) graphics;
					//uncomment this to reenable hard-light filtering on fg
					//					g.drawImage(overlay.filter(fg, background), 0, 0, null);
					g.drawImage(fg, 0, 0, null);
				}
			}
		}

		public void create() {
			final Runnable onChange = new Runnable() {
				@Override
				public void run() {
					if (SwingUtilities.isEventDispatchThread()) {
						Canvas.this.repaint();
					} else {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								Canvas.this.repaint();
							}
						});
					}
				}
			};
			drawables.add(new OfficeHoursMarquee(100, Main.TEXT_COLOR).setOnChange(onChange));
			drawables.add(new Logo(15).setOnChange(onChange));
		}

	}


}
