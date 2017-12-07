package ca.mcgill.sus.screensaver;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

public class ScreensaverFrame extends JFrame {
	private static final long serialVersionUID = 4848839375816808489L;
	protected final int display;
	public final boolean window;
	
	public ScreensaverFrame(int display, final boolean window) {
		super("CTF Screensaver");
		this.display = display;
		this.window = window;
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		if (!window) this.setAlwaysOnTop(true);
		if (!window) this.setUndecorated(true);
		this.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				super.keyReleased(e);
				if (!window || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					ScreensaverFrame.this.dispose();
					System.exit(0);
				}
			}
		});
		if (!window) {
			this.hideMouse();
			this.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					super.mouseReleased(e);
					ScreensaverFrame.this.dispose();
					System.exit(0);
				}
				
			});
			this.addMouseMotionListener(new MouseAdapter() {
				int lastX = -1, lastY = -1;
				@Override
				public void mouseMoved(MouseEvent e) {
					super.mouseMoved(e);
					if (lastX != -1 && distance(lastX, lastY, e.getX(), e.getY()) > 10) {
						ScreensaverFrame.this.dispose();
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
				Rectangle bounds = gd[this.display].getDefaultConfiguration().getBounds();
				if (window) {
					bounds.width = 1920;
					bounds.height = 1080;
				}
//				this.setBounds(bounds);
//				System.out.println(this.getBounds());
				this.setResizable(true);
				if (!window) this.setExtendedState(MAXIMIZED_BOTH);
				if (!window && !System.getProperty("os.name").startsWith("Windows") && gd[this.display].isFullScreenSupported()) {
					gd[this.display].setFullScreenWindow(this);
				}
			} else {
				throw new RuntimeException("Invalid display index");
			}
			
		}
	}

	public static double distance(int x1, int y1, int x2, int y2) {
		return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
	}
	
	public void hideMouse() {
		this.setCursor(this.getToolkit().createCustomCursor(new BufferedImage( 1, 1, BufferedImage.TYPE_INT_ARGB ), new Point(), null ));
	}
	

}
