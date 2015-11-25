package ca.mcgill.sus.screensaver;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

public class Screensaver extends JFrame {
	private static final long serialVersionUID = 4848839375816808489L;
	private final int display;
	
	public Screensaver(int display, final boolean kiosk) {
		super("CTF Screensaver");
		this.display = display;
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setAlwaysOnTop(true);
		this.setUndecorated(true);
		this.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				super.keyReleased(e);
				if (!kiosk || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					Screensaver.this.dispose();
					System.exit(0);
				}
			}
		});
		if (!kiosk) {
			this.hideMouse();
			this.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					super.mouseReleased(e);
					Screensaver.this.dispose();
					System.exit(0);
				}
				
			});
			this.addMouseMotionListener(new MouseAdapter() {
				int lastX = -1, lastY = -1;
				@Override
				public void mouseMoved(MouseEvent e) {
					super.mouseMoved(e);
					if (lastX != -1 && distance(lastX, lastY, e.getX(), e.getY()) > 10) {
						Screensaver.this.dispose();
						System.exit(0);
					}
					lastX = e.getX();
					lastY = e.getY();
					
				}
			});
		}
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			this.createBufferStrategy(3);
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] gd = ge.getScreenDevices();
			if (this.display >= 0 && this.display < gd.length) {
				this.setBounds(gd[this.display].getDefaultConfiguration().getBounds());
			} else {
				throw new RuntimeException("Invalid display index");
			}
			this.setExtendedState(MAXIMIZED_BOTH);
		}
	}

	public static double distance(int x1, int y1, int x2, int y2) {
		return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
	}
	
	public void hideMouse() {
		this.setCursor(this.getToolkit().createCustomCursor(new BufferedImage( 1, 1, BufferedImage.TYPE_INT_ARGB ), new Point(), null ));
	}
	

}
