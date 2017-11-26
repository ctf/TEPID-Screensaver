package ca.mcgill.sus.screensaver;

import java.awt.Graphics2D;

public interface Drawable {
	void step(long timestamp);
	void draw(Graphics2D g, int canvasWidth, int canvasHeight);
	boolean isDirty();
	void setDirty(boolean dirty);
}
