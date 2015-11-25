package ca.mcgill.sus.screensaver;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.mcgill.sus.screensaver.drawables.Error;
import ca.mcgill.sus.screensaver.drawables.Header;

import javax.swing.JPanel;

public class ScreensaverError extends Screensaver {

	private static final long serialVersionUID = 6079399489665204371L;

	public ScreensaverError(int display) {
		super(display, false);
		this.add(new Canvas());
		//close this screen if network is reestablished
//		new Thread("Connection Checker") {
//			@Override
//			public void run() {
//				while (!Main.isReachable("taskforce.sus.mcgill.ca", 4000)) {
//					try {
//						Thread.sleep(10_000);
//					} catch (InterruptedException e) {
//					}
//					System.out.println("Network connection reestablished! Exiting.");
//					System.exit(0);
//				}
//			};
//		}.start();
	}
	
	public static class Canvas extends JPanel {

		private static final long serialVersionUID = -1201562393819046373L;
		private final List<Drawable> drawables = Collections.synchronizedList(new ArrayList<Drawable>());
		
		public Canvas() {
			super(true);
			this.setBackground(Color.WHITE);
			create();
		}
		
		@Override
		public void paint(Graphics graphics) {
			super.paint(graphics);
			Graphics2D g = (Graphics2D) graphics;
			for (Drawable d : drawables) {
				d.draw(g, this.getWidth(), this.getHeight());
			}
		}
		
		public void create() {
			drawables.add(new Error());
			drawables.add(new Header("This computer has", 70, 90, 0x000000, false));
			drawables.add(new Header("No Network Connection", 120, 220, 0x000000));
			drawables.add(new Header("This is usually the result of a disconnected cable.", 50, 900, 0xff0000));
			drawables.add(new Header("Please report that this workstation (" + System.getenv("computerName") + ")", 30, 975, 0xff0000, false));
			drawables.add(new Header("is not functioning to a CTF volunteer in Burnside 1B19.", 30, 1015, 0xff0000, false));
			this.repaint();
		}
	}

}
