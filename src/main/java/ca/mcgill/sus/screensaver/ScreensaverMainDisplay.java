package ca.mcgill.sus.screensaver;

import ca.mcgill.sus.screensaver.drawables.Clock;
import ca.mcgill.sus.screensaver.drawables.Error;
import ca.mcgill.sus.screensaver.drawables.Header;
import ca.mcgill.sus.screensaver.drawables.InvertingHeader;
import ca.mcgill.sus.screensaver.drawables.JobList;
import ca.mcgill.sus.screensaver.drawables.ProfilePic;
import ca.mcgill.sus.screensaver.drawables.Slideshow;
import ca.mcgill.sus.screensaver.drawables.UserInfoBar;

public class ScreensaverMainDisplay extends BlurredScreensaverFrame {
	private static final long serialVersionUID = 4848839375816808489L;

	public ScreensaverMainDisplay(int display) {
		super(display);
		Runnable checkNetwork = new Runnable() {
			boolean networkUp = true, firstRun = true;
			public void run() {
				if (!DataFetch.getInstance().isLoaded()) return;
				if (DataFetch.getInstance().isNetworkUp() != networkUp || firstRun) {
					firstRun = false;
					networkUp = DataFetch.getInstance().isNetworkUp();
					stage.clear();
					if (networkUp) {
						//if network is up use regular set of widgets
						displayContents();
					} else {
						//if network is down display special error screen
						displayError();
					}
					stage.setDirty(true);
				}
			}
		};
		checkNetwork.run();
		DataFetch.getInstance().addChangeListener(checkNetwork);
	}

	private void displayContents() {
		stage.addDrawable(new JobList(500));
		stage.addDrawable(new Slideshow(10_000, 1000, 420));
		stage.addDrawable(new Clock("hh:mm a", Main.TEXT_COLOR & 0x88000000, 65));
		stage.addDrawable(new InvertingHeader(System.getenv("computerName"), 18, 20, Main.TEXT_COLOR, false).setAlignment(Header.ALIGN_LEFT));
		stage.addDrawable(new UserInfoBar(48, 420));
//						stage.addDrawable(new Marquee(350, Main.TEXT_COLOR));
		if (Main.OFFICE_COMPUTER && Main.LOGGED_IN) stage.addDrawable(new ProfilePic());
	}

	private void displayError() {
		stage.addDrawable(new Error());
		stage.addDrawable(new Header("This computer has", 70, 90, 0x000000, false, "constan.ttf"));
		stage.addDrawable(new Header("No Network Connection", 120, 220, 0x000000, true, "constanb.ttf"));
		stage.addDrawable(new Header("This is usually the result of a disconnected cable.", 50, 900, 0xff0000, true, "constanb.ttf"));
		stage.addDrawable(new Header("Please report that this workstation (" + System.getenv("computerName") + ")", 30, 975, 0xff0000, false, "constan.ttf"));
		stage.addDrawable(new Header("is not functioning to" + Config.INSTANCE.getReport_malfunctioning_to(), 30, 1015, 0xff0000, false, "constan.ttf"));
	}

}
