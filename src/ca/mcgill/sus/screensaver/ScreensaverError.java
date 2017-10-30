package ca.mcgill.sus.screensaver;

import java.awt.Color;

import ca.mcgill.sus.screensaver.drawables.Error;
import ca.mcgill.sus.screensaver.drawables.Header;

public class ScreensaverError extends ScreensaverFrame {

	private static final long serialVersionUID = 6079399489665204371L;

	public ScreensaverError(int display) {
		super(display, false);
		Stage stage = new Stage();
		stage.setBackground(Color.WHITE);
		stage.addDrawable(new Error());
		stage.addDrawable(new Header("This computer has", 70, 90, 0x000000, false));
		stage.addDrawable(new Header("No Network Connection", 120, 220, 0x000000));
		stage.addDrawable(new Header("This is usually the result of a disconnected cable.", 50, 900, 0xff0000));
		stage.addDrawable(new Header("Please report that this workstation (" + System.getenv("computerName") + ")", 30, 975, 0xff0000, false));
		stage.addDrawable(new Header("is not functioning to a CTF volunteer in Burnside 1B19.", 30, 1015, 0xff0000, false));
		stage.safeRepaint();
	}

}
