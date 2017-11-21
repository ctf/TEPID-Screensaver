package ca.mcgill.sus.screensaver;

import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

public class Util {
	
	public static BufferedImage addDropShadow() {
		return null;
	}
	
	public static BufferedImage convert(BufferedImage image, int type) {
		BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), type);
		Graphics2D g = out.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return out;
	}
	
	public static BufferedImage color(BufferedImage image, int color) {
		image = convert(image, BufferedImage.TYPE_INT_ARGB);
		int alphaDif;
		if (((color >> 24) & 0xff) > 0) { 
			alphaDif = 0xff - ((color >> 24) & 0xff);
		} else {
			alphaDif = 0;
		}
		color &= 0xffffff;
		int[] pix = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		for (int i = 0; i < pix.length; i++) {
			int alpha = 0xff & Math.max(0, (0xff & (pix[i] >> 24)) - alphaDif);
			pix[i] = (alpha << 24) | color;
		}
		return image;
	}
	public static String newId() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	public static BufferedImage blur(int radius, BufferedImage original) {
		if (radius < 1) {
			return (null);
		}
		int w = original.getWidth();
		int h = original.getHeight();
		BufferedImage output = convert(original, BufferedImage.TYPE_INT_RGB);
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

	public static BufferedImage screenshot(Rectangle bounds) {
		try {
			return new Robot().createScreenCapture(bounds);
		} catch (AWTException e) {
			throw new RuntimeException("Could not take screenshot", e);
		}
	}
	
	public static BufferedImage screenshot(int display) {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gd = ge.getScreenDevices();
		Rectangle bounds;
		if (display >= 0 && display < gd.length) {
			bounds = gd[display].getDefaultConfiguration().getBounds();
		} else {
			throw new RuntimeException("Invalid display index");
		}
		return screenshot(bounds);
	}
	
	public static BufferedImage loadBackground(boolean vertical) {
		BufferedImage background;
		try {
			InputStream bgJpg; 
			File localBg = new File(System.getenv("systemdrive") + "\\CTF Screensaver" + (vertical ? " Vertical" : "") + ".jpg");
			if (localBg.exists()) {
				bgJpg = new FileInputStream(localBg);
			} else {
				bgJpg = BlurredScreensaverFrame.class.getResourceAsStream("background/bg.jpg");
			}
			background =  Util.convert(ImageIO.read(bgJpg), BufferedImage.TYPE_INT_RGB);
		} catch (IOException e) {
			throw new RuntimeException("Could not load background image...", e);
		}
		return background;
	}
	
	public static List<BufferedImage> loadSlides() {
		List<BufferedImage> slides = new ArrayList<>();
		try {
			for (File f : new File("C:\\Screensaver Slides").listFiles()) {
				try {
					slides.add(ImageIO.read(f));
				} catch (Exception e) {}
			}
		} catch (Exception e) {}
		return slides;
	}
	
	/**A function which causes the thread to wait for a time 
	 * @param ms	The time for which the thread should do nothing
	 */
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
