package ca.mcgill.sus.screensaver;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
	
	public ScreensaverMainDisplay(int display, boolean kiosk) {
		super(display, kiosk);
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
		super.setVisible(visible);
		if (!System.getenv("username").equals("SYSTEM")) {
			final BufferedImage screenshot = screenshot(getBounds());
			canvas.setBackground(screenshot);
			canvas.repaint();
			new Thread("Blurify") {
				public void run() {
					final int maxBlur = 14;
					BufferedImage[] frames = new BufferedImage[maxBlur];
					for (int b = 0; b < maxBlur; b++) {
						frames[b] = blur(b, screenshot);
					}
					for (int b = 1; b < maxBlur; b++) {
						canvas.setBackground(frames[b]);
						canvas.repaint();
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					canvas.create();
					canvas.repaint();
				};
			}.start();
		} else {
			BufferedImage background;
			try {
				background =  Util.convert(ImageIO.read(ScreensaverMainDisplay.class.getResourceAsStream("background/bg.jpg")), BufferedImage.TYPE_INT_RGB);
			} catch (IOException e) {
				background = null;
				System.err.println("Could not load background image...");
			}
			canvas.setBackground(background);
			canvas.create();
			canvas.repaint();
		}
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
			BufferedImage fg = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = fg.createGraphics();
//			g.setColor(this.getBackground());
//			g.setColor(new Color(0x7f7f7f));
//			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			for (Drawable d : drawables) {
				d.draw(g, this.getWidth(), this.getHeight());
			}
			g.dispose();
			g = (Graphics2D) graphics;
			if (background != null) {
				//uncomment this to reenable hard-light filtering on fg
//				g.drawImage(overlay.filter(fg, background), 0, 0, null);
				g.drawImage(background, 0, 0, null);
				g.setColor(new Color(0x77ffffff, true));
				g.fillRect(0, 0, fg.getWidth(), fg.getHeight());
				g.drawImage(fg, 0, 0, null);
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
			int textColor = 0xbb000000;
//			drawables.add(new Header("McGill Science Computer Taskforce", 70, 100, textColor));
//			drawables.add(new PrinterStatus(100, 50).setOnChange(onChange));
			drawables.add(new JobList(550).setOnChange(onChange));
			drawables.add(new Clock("h:mm a", textColor).setOnChange(onChange));
			drawables.add(new Header("PRINTER STATUS", 32, 525, textColor));
			if (kiosk) {
				drawables.add(new NowPlaying(470).setOnChange(onChange));
				drawables.add(new Marquee(380, textColor).setOnChange(onChange));
			} else {
				drawables.add(new Header(System.getenv("computerName"), 12, 22, textColor).setAlignment(Header.ALIGN_RIGHT));
				//determining info about the current user could take time, so it has its own thread
				UserInfoBar userInfo = new UserInfoBar(48, 450);
				drawables.add(userInfo);
				drawables.add(new Marquee(350, textColor).setOnChange(onChange));
				if (userInfo.officeComputer && userInfo.loggedIn) {
					drawables.add(new ProfilePic().setOnChange(onChange));
				}
			}
		}
		
	}
	
	public BufferedImage blur(int radius, BufferedImage original) {
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
