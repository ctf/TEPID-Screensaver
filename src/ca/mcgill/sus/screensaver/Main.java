package ca.mcgill.sus.screensaver;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.JOptionPane;


public class Main {
	
	public static final boolean LOGGED_IN = !System.getenv("username").equals("SYSTEM");
//	public static final boolean LOGGED_IN = false;
	public static final int COLOR_DOWN = LOGGED_IN ? 0xbbdc241f : 0xaaf11700, COLOR_UP = LOGGED_IN ? 0xcc50c954 : 0xaaaad400, TEXT_COLOR = LOGGED_IN ? 0xbb000000 : 0xddffffff;
	
	public final static String serverUrl = ***REMOVED***; 	//real tepid url
//	public final static String serverUrl = "http://localhost:8080/tepid/screensaver";				//debugging url
	
	public static void main(String[] args) {
		boolean start = false, kiosk = false;
		for (String arg : args) {
			if (arg.equalsIgnoreCase("/s")) {
				start = true; //normal operation mode
			} else if (arg.equalsIgnoreCase("/k")) {
				kiosk = start = true; //kiosk operation mode; for display stations
			} else if (arg.equalsIgnoreCase("/p")) {
				System.exit(0);
			} else if (arg.equalsIgnoreCase("/c")) {
				JOptionPane.showMessageDialog(null, "There are (currently) no options to configure :(", "Nothing to configure", JOptionPane.INFORMATION_MESSAGE);
				System.exit(0);		//for configuration options. none are configurable (yet)
			}
		}
		if (start) {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] gd = ge.getScreenDevices();
			final List<Screensaver> screensavers = new ArrayList<>(6);
			final List<Thread> screensaverThreads = new ArrayList<>(6);
			for (int i = 0; i < gd.length; i++) {
				final Screensaver screensaver;
				if (gd[i] == ge.getDefaultScreenDevice()) {
					if (isReachable("taskforce.science.mcgill.ca", 4000)) {
						screensaver = new ScreensaverSecondaryDisplay(i, kiosk);
					} else {
						screensaver = new ScreensaverError(i);
					}
				} else {
					screensaver = new ScreensaverSecondaryDisplay(i, kiosk);
				}
				screensavers.add(screensaver);
				Thread displayThread = new Thread("Screensaver Display " + i) {
					@Override
					public void run() {
						screensaver.setVisible(true);
					}
				};
				screensaverThreads.add(displayThread);
				displayThread.start();
			}
			final boolean KIOSK = kiosk;
			new Thread("Network Monitor") {
				int interval = 10_000;
				@Override
				public void run() {
					for (;;) {
						long before = System.currentTimeMillis();
						boolean reachable = isReachable("taskforce.science.mcgill.ca", 4000);
						for (ListIterator<Screensaver> iter = screensavers.listIterator(); iter.hasNext();) {
							Screensaver screensaver = iter.next();
							if (screensaver instanceof ScreensaverMainDisplay && !reachable) {
								screensaver.dispose();
								screensaver = new ScreensaverError(screensaver.display);
								iter.set(screensaver);
							} else if (screensaver instanceof ScreensaverError && reachable) {
								screensaver.dispose();
								screensaver = new ScreensaverMainDisplay(screensaver.display, KIOSK);
								
							} else {
								continue;
							}
							iter.set(screensaver);
							screensaverThreads.get(screensaver.display).interrupt();
							final Screensaver s = screensaver;
							Thread displayThread = new Thread("Screensaver Display " + s.display) {
								@Override
								public void run() {
									s.setVisible(true);
								}
							};
							screensaverThreads.set(screensaver.display, displayThread);
							displayThread.start();
						}
						try {
							Thread.sleep(interval - (System.currentTimeMillis() - before));
						} catch (InterruptedException e) {
							break;
						}
					}
				}
			}.start();
			
		} else {
			System.out.println("Neither /K nor /S flag was passed. Not starting. ");
		}
	}
	
	public static boolean isReachable(String url, int timeoutMs) {
		try {
			for (InetAddress address : InetAddress.getAllByName(url)) if (address.isReachable(timeoutMs)) return true;
		} catch (Exception e) {
		}
		return false;
	}
}
