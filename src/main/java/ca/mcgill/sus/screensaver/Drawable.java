package ca.mcgill.sus.screensaver;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public interface Drawable {
	void step(long timestamp);
	void draw(Graphics2D g, BufferedImage canvas, int canvasWidth, int canvasHeight);
	boolean isDirty();
	void setDirty(boolean dirty);
}
