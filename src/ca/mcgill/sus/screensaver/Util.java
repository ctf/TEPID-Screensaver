package ca.mcgill.sus.screensaver;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

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
}
