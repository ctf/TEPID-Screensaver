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
		if (ColorUtil.hasAlpha(color)) {
			alphaDif = 0xff - ColorUtil.getAlpha(color);
		} else {
			alphaDif = 0;
		}
		color = ColorUtil.getColor(color);
		int[] pix = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		for (int i = 0; i < pix.length; i++) {
			int alpha = 0xff & Math.max(0, ColorUtil.getAlpha(pix[i]) - alphaDif);
			pix[i] = (alpha << 24) | color;
		}
		return image;
	}

	public static BufferedImage blur(int radius, BufferedImage original) {
		if (radius < 1) {
			return (null);
		}

		//init
		int w = original.getWidth();
		int h = original.getHeight();
		BufferedImage output = convert(original, BufferedImage.TYPE_INT_RGB);
		int[] pix = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();
		int wm = w - 1;
		int hm = h - 1;
		int wh = w * h;

		int r[] = new int[wh];
		int g[] = new int[wh];
		int b[] = new int[wh];
		int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
		int vmin[] = new int[Math.max(w, h)];

		int div = radius + radius + 1; // effective diameter
		int divsum = (int) Math.pow ((div + 1) / 2, 2); //effective radius squared
		int dv[] = new int[256 * divsum];
		for (i = 0; i < 256 * divsum; i++) {
			dv[i] = (i / divsum);
		} //generates an array from 0 to 255 with divsum elements of each number


		yw = yi = 0;
		int[][] stack = new int[div][3]; 		// stack of length of effective diameter, with RGB components
		int stackpointer;
		int stackstart;
		int[] sir;
		int rbs;
		int r1 = radius + 1;
		int routsum, goutsum, boutsum;
		int rinsum, ginsum, binsum;

		// for every row
		for (y = 0; y < h; y++) {
			// clear sums
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;


			// from every in the range of -r to +r
			for (i = -radius; i <= radius; i++) {
				// p is the pixel at position i, bounded by the start and end of the row
				p = pix[yi + Math.min(wm, Math.max(i, 0))];

				// set sir to position in stack (forces non-negative)
				sir = stack[i + radius];

				//extract color components to stack
				sir[0] = (p & 0xff0000) >> 16;
				sir[1] = (p & 0x00ff00) >> 8;
				sir[2] = (p & 0x0000ff);

				// complement of distance
				rbs = r1 - Math.abs(i);

				// add color with complement of distance as weight
				rsum += sir[0] * rbs;
				gsum += sir[1] * rbs;
				bsum += sir[2] * rbs;

				// add sir to insum if before center or outsum if after center
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
			// sir will still hold the last pixel's colors

			// for every column in row
			stackpointer = radius;
			for (x = 0; x < w; x++) {
				// assign rsum/divsum*255
				r[yi] = dv[rsum];
				g[yi] = dv[gsum];
				b[yi] = dv[bsum];

				// reduce rsum by the amount leaving
				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;

				// set stackstart to pointer + r + 1
				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];

				// decrease outsum by the next pixel
				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];

				// if it's the first row,
				if (y == 0) {
					// set the vmin element corresponding to the column to the column + radius + 1 capped at the width - 1
					vmin[x] = Math.min(x + radius + 1, wm);
				}
				p = pix[yw + vmin[x]];

				// load this pixel's colors into sir
				sir[0] = (p & 0xff0000) >> 16;
				sir[1] = (p & 0x00ff00) >> 8;
				sir[2] = (p & 0x0000ff);

				// add sir to insum
				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];

				// add insum to sum
				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;

				// increment stackpointer
				stackpointer = (stackpointer + 1) % div;
				sir = stack[(stackpointer) % div];

				// add color to outsum and subtract from insum
				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];
				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];

				// increment yi
				yi++;
			}
			// increment yw by width
			yw += w;
		}

		//for every column
		for (x = 0; x < w; x++) {
			// clear sums
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;

			// sets yp to the offset necessary to move radius rows back
			yp = -radius * w;

			// from every in the range of -r to +r
			for (i = -radius; i <= radius; i++) {
				// sets yi to the farthest pixel vertically from the center
				yi = Math.max(0, yp) + x;

				// loads sir
				sir = stack[i + radius];

				// assigns colors to sir
				sir[0] = r[yi];
				sir[1] = g[yi];
				sir[2] = b[yi];

				// complement of distance
				rbs = r1 - Math.abs(i);

				// add color with complement of distance as weight
				rsum += r[yi] * rbs;
				gsum += g[yi] * rbs;
				bsum += b[yi] * rbs;

				// add sir to insum if before center or outsum if after center
				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}

				// if i is less than the total number of pixels
				if (i < hm) {
					// increment the pointer to pixel by 1 row
					yp += w;
				}
			}

			yi = x;
			stackpointer = radius;
			// for every row
			for (y = 0; y < h; y++) {
				// rebuild pixel array
				// Preserve alpha channel: ( 0xff000000 & pix[yi] )
				pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16)
						| (dv[gsum] << 8) | dv[bsum];

				// subtract outsum from sum
				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;

				// move stack pointer
				stackstart = stackpointer - radius + div;

				// load sir
				sir = stack[stackstart % div];

				// subtract sir from outsum
				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];

				// if first column
				if (x == 0) {
					// set the vmin element corresponding to the column to the column + radius + 1 capped at the width - 1
					vmin[y] = Math.min(y + r1, hm) * w;
				}

				// set the index to the current position in the 1D array
				p = x + vmin[y];

				// set sir from color array
				sir[0] = r[p];
				sir[1] = g[p];
				sir[2] = b[p];

				// add sir to insum
				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];

				// add insum to sum
				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;

				// increment stack pointer
				stackpointer = (stackpointer + 1) % div;

				// load sir
				sir = stack[stackpointer];

				// add sir to outsum
				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];

				// subtract sir from sum
				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];

				// increment yi to next row
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
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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
}
