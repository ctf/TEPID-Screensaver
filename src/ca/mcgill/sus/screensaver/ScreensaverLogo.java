package ca.mcgill.sus.screensaver;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;

import ca.mcgill.sus.screensaver.drawables.Logo;

public class ScreensaverLogo extends ScreensaverFrame {

	private static final long serialVersionUID = 8717935860219395871L;

	public ScreensaverLogo(int display) {
		super(display, false);
		this.add(new Canvas());
	}

	public static class Canvas extends JPanel {

		private static final long serialVersionUID = -1201562393819046373L;
		private final List<Drawable> drawables = Collections.synchronizedList(new ArrayList<Drawable>());
		
		public Canvas() {
			super(true);
//			this.setBackground(new Color(0x292929));
			this.setBackground(Color.BLACK);
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
			Runnable onChange = new Runnable() {
				@Override
				public void run() {
					Canvas.this.repaint();
				}
			};
			drawables.add(new Logo(15).setOnChange(onChange));
		}
	}
}
