package ca.mcgill.sus.screensaver.drawables;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import ca.mcgill.sus.screensaver.datafetch.DataFetch;
import ca.mcgill.sus.screensaver.Drawable;

public class ProfilePic implements Drawable {	
	public ProfilePic() {
	}
	
	@Override
	public void step(long timestamp) {
	}

	@Override
	public void draw(Graphics2D g, BufferedImage canvas, int canvasWidth, int canvasHeight) {
		if (!DataFetch.getInstance().profilePic.isEmpty()) g.drawImage(DataFetch.getInstance().profilePic.peek(), 5, 30, null);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public void setDirty(boolean dirty) {		
	}

}