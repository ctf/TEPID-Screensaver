package ca.mcgill.sus.screensaver;

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
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import ca.mcgill.sus.screensaver.drawables.Clock;
import ca.mcgill.sus.screensaver.drawables.Header;
import ca.mcgill.sus.screensaver.drawables.JobList;
import ca.mcgill.sus.screensaver.drawables.Marquee;
import ca.mcgill.sus.screensaver.drawables.NowPlaying;
import ca.mcgill.sus.screensaver.drawables.PrinterStatus;
import ca.mcgill.sus.screensaver.drawables.ProfilePic;
import ca.mcgill.sus.screensaver.drawables.UserInfoBar;
import ca.mcgill.sus.screensaver.filters.Filter;
import ca.mcgill.sus.screensaver.filters.HardLight;

public class ScreensaverMainDisplay extends Screensaver {
	private static final long serialVersionUID = 4848839375816808489L;
	private final Canvas canvas;
	private final boolean kiosk;

	public ScreensaverMainDisplay(int display, boolean kiosk) {
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
		if (Main.LOGGED_IN && !this.kiosk) {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] gd = ge.getScreenDevices();
			Rectangle bounds;
			if (this.display >= 0 && this.display < gd.length) {
				bounds = gd[this.display].getDefaultConfiguration().getBounds();
			} else {
				throw new RuntimeException("Invalid display index");
			}
			background = screenshot(bounds);
			super.setVisible(visible);
			canvas.setBackground(background);
		} else {
			try {
				background =  Util.convert(ImageIO.read(ScreensaverMainDisplay.class.getResourceAsStream("background/bg.jpg")), BufferedImage.TYPE_INT_RGB);
			} catch (IOException e) {
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
				g.fillRect(0, 0, this.getWidth(), this.getHeight()); //hihihihihihihihihihh merrrrrrrrrr meeeeeeemememeeeemememememee 
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
		private final boolean kiosk;
		private BufferedImage background;

		public void setBackground(BufferedImage bg) {
			this.background = bg;
		}

		public Canvas(boolean kiosk) {
			super(true);
			this.kiosk = kiosk;
			this.setBackground(new Color(0x292929));
		}

		Filter overlay = new HardLight();
		@Override
		public void paint(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics;
			if (background != null) {
				g.drawImage(background, 0, 0, null);
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
			//			drawables.add(new Header("McGill Science Computer Taskforce", 70, 100, textColor));
			drawables.add(new PrinterStatus(60, 50).setOnChange(onChange));
			drawables.add(new JobList(550).setOnChange(onChange));
			drawables.add(new Clock("h:mm a", Main.TEXT_COLOR).setOnChange(onChange));
			//			drawables.add(new Header("PRINTER STATUS", 32, 525, textColor));
			if (kiosk) {
				drawables.add(new NowPlaying(450, Main.TEXT_COLOR).setOnChange(onChange));
				drawables.add(new Marquee(350, Main.TEXT_COLOR).setOnChange(onChange));
			} else {
				drawables.add(new Header(System.getenv("computerName"), 16, 22, Main.TEXT_COLOR, false).setAlignment(Header.ALIGN_RIGHT));
				UserInfoBar userInfo = new UserInfoBar(48, 450);
				drawables.add(userInfo);
				drawables.add(new Marquee(350, Main.TEXT_COLOR).setOnChange(onChange));
				if (userInfo.officeComputer && Main.LOGGED_IN) {
					drawables.add(new ProfilePic().setOnChange(onChange));
				}
			}
		}

	}

	public static BufferedImage blur(int radius, BufferedImage original) {
		if (radius < 1) {
			return (null);
		}
		int w = original.getWidth();
		int h = original.getHeight();
		BufferedImage output = Util.convert(original, BufferedImage.TYPE_INT_RGB);
		int[] pix = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();
		int wm = w - 1;
		int hm = h - 1;
		int wh = w * h;
		int div = radius + radius + 1;
		int r[] = new int[wh];
		int g[] = new int[wh];
		int b[] = new int[wh];
		int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
		int vmin[] = new int[Math.max(w, h)];
		int divsum = (div + 1) >> 1;
		divsum *= divsum;
		int dv[] = new int[256 * divsum];
		for (i = 0; i < 256 * divsum; i++) {
			dv[i] = (i / divsum);
		}
		yw = yi = 0;
		int[][] stack = new int[div][3];
		int stackpointer;
		int stackstart;
		int[] sir;
		int rbs;
		int r1 = radius + 1;
		int routsum, goutsum, boutsum;
		int rinsum, ginsum, binsum;
		for (y = 0; y < h; y++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			for (i = -radius; i <= radius; i++) {
				p = pix[yi + Math.min(wm, Math.max(i, 0))];
				sir = stack[i + radius];
				sir[0] = (p & 0xff0000) >> 16;
			sir[1] = (p & 0x00ff00) >> 8;
		sir[2] = (p & 0x0000ff);
		rbs = r1 - Math.abs(i);
		rsum += sir[0] * rbs;
		gsum += sir[1] * rbs;
		bsum += sir[2] * rbs;
		if (i > 0) {
			rinsum += sir[0];
			ginsum += sir[1];
			binsum += sir[2];
		} else {
			routsum += sir[0];
			goutsum += sir[1];
			boutsum += sir[2];
		}
			}
			stackpointer = radius;
			for (x = 0; x < w; x++) {
				r[yi] = dv[rsum];
				g[yi] = dv[gsum];
				b[yi] = dv[bsum];
				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;
				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];
				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];
				if (y == 0) {
					vmin[x] = Math.min(x + radius + 1, wm);
				}
				p = pix[yw + vmin[x]];
				sir[0] = (p & 0xff0000) >> 16;
			sir[1] = (p & 0x00ff00) >> 8;
			sir[2] = (p & 0x0000ff);
			rinsum += sir[0];
			ginsum += sir[1];
			binsum += sir[2];
			rsum += rinsum;
			gsum += ginsum;
			bsum += binsum;
			stackpointer = (stackpointer + 1) % div;
			sir = stack[(stackpointer) % div];
			routsum += sir[0];
			goutsum += sir[1];
			boutsum += sir[2];
			rinsum -= sir[0];
			ginsum -= sir[1];
			binsum -= sir[2];
			yi++;
			}
			yw += w;
		}
		for (x = 0; x < w; x++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			yp = -radius * w;
			for (i = -radius; i <= radius; i++) {
				yi = Math.max(0, yp) + x;
				sir = stack[i + radius];
				sir[0] = r[yi];
				sir[1] = g[yi];
				sir[2] = b[yi];
				rbs = r1 - Math.abs(i);
				rsum += r[yi] * rbs;
				gsum += g[yi] * rbs;
				bsum += b[yi] * rbs;
				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}
				if (i < hm) {
					yp += w;
				}
			}
			yi = x;
			stackpointer = radius;
			for (y = 0; y < h; y++) {
				// Preserve alpha channel: ( 0xff000000 & pix[yi] )
				pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16)
						| (dv[gsum] << 8) | dv[bsum];
				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;
				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];
				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];
				if (x == 0) {
					vmin[y] = Math.min(y + r1, hm) * w;
				}
				p = x + vmin[y];
				sir[0] = r[p];
				sir[1] = g[p];
				sir[2] = b[p];
				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];
				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;
				stackpointer = (stackpointer + 1) % div;
				sir = stack[stackpointer];
				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];
				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];
				yi += w;
			}
		}
		return output;
	}
}
