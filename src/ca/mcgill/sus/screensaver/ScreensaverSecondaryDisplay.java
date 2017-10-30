package ca.mcgill.sus.screensaver;

import ca.mcgill.sus.screensaver.drawables.Logo;
import ca.mcgill.sus.screensaver.drawables.UpcomingEvents;

public class ScreensaverSecondaryDisplay extends BlurredScreensaverFrame {
	private static final long serialVersionUID = 4848839375816808489L;

	public ScreensaverSecondaryDisplay(int display) {
		super(display);
		this.stage.addDrawable(new UpcomingEvents(100, Main.TEXT_COLOR));
		this.stage.addDrawable(new Logo(15));
	}

}
