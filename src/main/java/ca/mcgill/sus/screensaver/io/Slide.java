package ca.mcgill.sus.screensaver.io;

import java.awt.image.BufferedImage;

public class Slide {
	public final String name;
	public BufferedImage dark, light;
	public Slide(String name) {
		this.name = name;
	}
}
