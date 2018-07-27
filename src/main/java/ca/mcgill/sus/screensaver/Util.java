package ca.mcgill.sus.screensaver;

import java.awt.AWTException;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import ca.mcgill.sus.screensaver.io.Slide;

public class Util {
	
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
			File localBg = new File(Config.INSTANCE.getBackground_picture_directory() + "CTF Screensaver" + (vertical ? " Vertical" : "") + ".jpg");
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
	
	public static List<Slide> loadSlides() {
		Map<String, BufferedImage> images = new HashMap<>();
		try {
			for (File f : new File(Config.INSTANCE.getAnnouncement_slide_directory()).listFiles()) {
				try {
					images.put(f.getName(), ImageIO.read(f));
				} catch (Exception e) {}
			}
		} catch (Exception e) {}
		Map<String, Slide> slides = new HashMap<>();
		for (Entry<String, BufferedImage> e : images.entrySet()) {
			if (e.getValue() == null) continue;
			String name = (e.getKey().contains(".") ? e.getKey().substring(0, e.getKey().indexOf(".")): e.getKey()).toLowerCase();
			boolean light = name.endsWith("_light");
			if (light) name = name.substring(0, name.indexOf("_light"));
			if (!slides.containsKey(name)) slides.put(name, new Slide(name));
			if (light) slides.get(name).light = e.getValue();
			else slides.get(name).dark = e.getValue();
		}
		List<Slide> out = new ArrayList<>(slides.values());
		for (Slide s : out) {
			if (s.light == null) s.light = s.dark;
			if (s.dark == null) s.dark = s.light;
		}
		out.sort((s1, s2) -> s1.name.compareTo(s2.name));
		return out;
	}
	
	/**A function which causes the thread to wait for a time 
	 * @param ms	The time for which the thread should do nothing
	 */
	public static void sleep(long ms) {
		if (ms < 0) return;
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static Rectangle getRealStringBounds(GlyphVector v) {
		Rectangle2D rect = v.getVisualBounds();
		return new Rectangle((int) rect.getX(), (int) rect.getY(), (int) rect.getWidth(), (int) rect.getHeight());
	}

	public static GlyphVector getStringVector(Graphics2D g, String text) {
		Font font = g.getFont();
		FontMetrics fontMetrics = g.getFontMetrics();
		return font.createGlyphVector(fontMetrics.getFontRenderContext(), text);
	}

	public static <T> T[] concat(T[] a1, T[] a2) {
		T[] out = Arrays.copyOf(a1, a1.length + a2.length);
		System.arraycopy(a2, 0, out, a1.length, a2.length);
		return out;
	}
	
	public static double luminance(int argb) {
		double r = (double) ((argb >> 16) & 0xff) / 0xff,
		g = (double) ((argb >> 8) & 0xff) / 0xff,
		b = (double) (argb & 0xff) / 0xff;
		return Math.sqrt(0.299 * r * r + 0.587 * g * g + 0.114 * b * b);
	}
	
	public static double luminanceAvg(BufferedImage img, int x, int y, int w, int h) {
		if (img.getType() != BufferedImage.TYPE_INT_ARGB && img.getType() != BufferedImage.TYPE_INT_RGB) throw new RuntimeException("Non-int image types not supported");
		x = Math.max(0, Math.min(x, img.getWidth()));
		y = Math.max(0, Math.min(y, img.getHeight()));
		w = Math.max(0, Math.min(w, img.getWidth() - x));
		h = Math.max(0, Math.min(h, img.getHeight() - y));
		int[] pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
		double lum = 0;
		for (int i = y; i < y + h; i++) {
			for (int j = x; j < x + w; j++) {
				lum += luminance(pixels[i * img.getWidth() + j]);
			}
		}
		return lum / (w * h);
	}

	public static BufferedImage circleCrop(BufferedImage image) {
	    int w = Math.max(image.getWidth(), image.getHeight()), h = w;
	    BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g = out.createGraphics();
	    g.setComposite(AlphaComposite.Src);
	    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	    g.setColor(Color.BLACK);
	    g.fillOval(0, 0, w, h);
	    g.setComposite(AlphaComposite.SrcAtop);
	    g.drawImage(image, w / 2 - image.getWidth() / 2, h / 2 - image.getHeight() / 2, null);
	    g.dispose();
	    return out;
	}
	
	public static String hex(byte[] array) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; ++i) {
			sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100), 1, 3);
		}
		return sb.toString();
	}

	public static String md5Hex(String message) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			return hex(md.digest(message.getBytes("CP1252")));
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
		}
		return null;
	}
	
	public static BufferedImage readImage(byte[] bytes) throws IOException {
		ByteArrayInputStream baio = new ByteArrayInputStream(bytes);
		BufferedImage image = ImageIO.read(baio);
		baio.close();
		return image;
	}

//	public static Calendar resetCalendar ()
	
}
