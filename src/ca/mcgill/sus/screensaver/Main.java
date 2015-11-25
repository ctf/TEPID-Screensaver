package ca.mcgill.sus.screensaver;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.net.InetAddress;

import javax.swing.JOptionPane;


public class Main {
	public static void main(String[] args) {
		boolean start = false, kiosk = false;
		for (String arg : args) {
			if (arg.equalsIgnoreCase("/s")) {
				start = true;
			} else if (arg.equalsIgnoreCase("/k")) {
				kiosk = start = true;
			} else if (arg.equalsIgnoreCase("/p")) {
				System.exit(0);
			} else if (arg.equalsIgnoreCase("/c")) {
				JOptionPane.showMessageDialog(null, "There are (currently) no options to configure :(", "Nothing to configure", JOptionPane.INFORMATION_MESSAGE);
				System.exit(0);
			}
		}
		if (start) {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] gd = ge.getScreenDevices();
			for (int i = 0; i < gd.length; i++) {
				if (gd[i] == ge.getDefaultScreenDevice()) {
					if (isReachable("taskforce.sus.mcgill.ca", 4000)) {
						new ScreensaverMainDisplay(i, kiosk).setVisible(true);
					} else {
						new ScreensaverError(i).setVisible(true);
					}
				} else {
					new ScreensaverLogo(i).setVisible(true);
				}
			}
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
