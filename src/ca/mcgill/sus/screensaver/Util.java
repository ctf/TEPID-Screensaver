package ca.mcgill.sus.screensaver;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

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
}
