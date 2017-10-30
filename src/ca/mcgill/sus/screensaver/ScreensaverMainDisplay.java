package ca.mcgill.sus.screensaver;

import ca.mcgill.sus.screensaver.drawables.Clock;
import ca.mcgill.sus.screensaver.drawables.Header;
import ca.mcgill.sus.screensaver.drawables.JobList;
import ca.mcgill.sus.screensaver.drawables.Marquee;
import ca.mcgill.sus.screensaver.drawables.NowPlaying;
import ca.mcgill.sus.screensaver.drawables.PrinterStatus;
import ca.mcgill.sus.screensaver.drawables.UserInfoBar;

public class ScreensaverMainDisplay extends BlurredScreensaverFrame {
	private static final long serialVersionUID = 4848839375816808489L;

	public ScreensaverMainDisplay(int display, boolean kiosk) {
		super(display, kiosk);
		this.stage.addDrawable(new PrinterStatus(60, 50));
		this.stage.addDrawable(new JobList(550));
		this.stage.addDrawable(new Clock("hh:mm a", Main.TEXT_COLOR));
		if (kiosk) {
			this.stage.addDrawable(new NowPlaying(450, Main.TEXT_COLOR));
			this.stage.addDrawable(new Marquee(350, Main.TEXT_COLOR));
		} else {
			this.stage.addDrawable(new Header(System.getenv("computerName"), 16, 22, Main.TEXT_COLOR, false).setAlignment(Header.ALIGN_RIGHT));
			UserInfoBar userInfo = new UserInfoBar(48, 450);
			this.stage.addDrawable(userInfo);
			this.stage.addDrawable(new Marquee(350, Main.TEXT_COLOR));
		}
	}

	
}
