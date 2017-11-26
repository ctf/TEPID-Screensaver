package ca.mcgill.sus.screensaver.drawables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.SpriteManager;

/**Used to draw the error sprite if something has gone wrong with the computer
 * 
 */
public class Error implements Drawable {

	private final BufferedImage error = SpriteManager.getInstance().getSprite("error.png");
	private boolean dirty = true;
	
	public Error() {
	}

	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, canvasWidth, canvasHeight);
		int x = canvasWidth / 2, y = canvasHeight / 2;
		g.drawImage(error, x - error.getWidth() / 2, y - error.getHeight() / 2, null);
	}

	@Override
	public void step(long timestamp) {
		
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void setDirty(boolean dirty) {
		this.dirty  = dirty;
	}
	

}
