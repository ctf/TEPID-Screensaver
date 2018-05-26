package ca.mcgill.sus.screensaver;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;


public class Main {
	
	public static final boolean LOGGED_IN = !System.getenv("username").equals("SYSTEM");
	public final static boolean OFFICE_COMPUTER = System.getenv("computerName").matches(***REMOVED***);
//	public static final boolean LOGGED_IN = false;
	public static final int COLOR_DOWN = LOGGED_IN ? 0xbbdc241f : 0xaaf11700, COLOR_UP = LOGGED_IN ? 0xcc50c954 : 0xaaaad400, 
	TEXT_COLOR = LOGGED_IN ? 0xbb000000 : 0xddffffff;
	public static final Set<String> flags = new HashSet<>();
	
	public final static String serverUrl = Config.INSTANCE.getSERVER_URL();
	
	public static void main(String[] args) {
		for (String arg : args) flags.add(arg.toLowerCase());
		if (flags.contains("/c")) JOptionPane.showMessageDialog(null, "There are (currently) no options to configure :(", "Nothing to configure", JOptionPane.INFORMATION_MESSAGE);
		else if (flags.contains("/s") || flags.contains("/w")) {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] gd = ge.getScreenDevices();
			final List<ScreensaverFrame> screensavers = new ArrayList<>(gd.length);
			final List<Thread> screensaverThreads = new ArrayList<>(gd.length);
			for (int i = 0; i < gd.length; i++) {
				final ScreensaverFrame screensaver;
				if (gd[i] == ge.getDefaultScreenDevice()) {
					screensaver = new ScreensaverMainDisplay(i);
				} else {
					screensaver = new ScreensaverSecondaryDisplay(i);
				}
				screensavers.add(screensaver);
				Thread displayThread = new Thread("Screensaver Display " + i) {
					@Override
					public void run() {
						screensaver.setVisible(true);
					}
				};
				screensaverThreads.add(displayThread);
				displayThread.setDaemon(true);
				displayThread.start();
			}
			
		} else {
			System.out.println("Neither /S nor /W flag was passed. Not starting. ");
		}
	}
	
}
