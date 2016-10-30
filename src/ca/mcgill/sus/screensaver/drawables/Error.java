package ca.mcgill.sus.screensaver.drawables;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.SpriteManager;

/**Used to draw the error sprite if something has gone wrong with the computer
 * 
 */
public class Error implements Drawable {

	private final BufferedImage error = SpriteManager.getInstance().getSprite("error.png");
	
	public Error() {
	}

	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		int x = canvasWidth / 2, y = canvasHeight / 2;
		g.drawImage(error, x - error.getWidth() / 2, y - error.getHeight() / 2, null);
	}

	@Override
	public Error setOnChange(Runnable r) {
		return this;
	}
	

}
