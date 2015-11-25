package ca.mcgill.sus.screensaver;

import java.awt.Graphics2D;

public interface Drawable {
	void draw(Graphics2D g, int canvasWidth, int canvasHeight);
	Drawable setOnChange(Runnable r);
}
