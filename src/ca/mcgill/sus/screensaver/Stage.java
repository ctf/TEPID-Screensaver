package ca.mcgill.sus.screensaver;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Stage extends JPanel {
	private static final long serialVersionUID = 2466660164920195989L;

	private final Queue<Drawable> drawables = new ConcurrentLinkedQueue<Drawable>();
	private BufferedImage background;
	private float drawableOpacity = 1f;

	public Stage() {
		super(true);
		this.setBackground(new Color(0x292929));
	}

	@Override
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;
		if (background != null) {
			g.drawImage(background, getWidth() / 2 - background.getWidth() / 2, getHeight() / 2 - background.getHeight() / 2, null);
			if (!drawables.isEmpty()) {
				BufferedImage fg = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
				g = fg.createGraphics();

				for (Drawable d : drawables) {
					d.draw(g, this.getWidth(), this.getHeight());
				}
				g.dispose();
				g = (Graphics2D) graphics;
				if (drawableOpacity != 1f) {
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, drawableOpacity));
				}
				g.drawImage(fg, 0, 0, null);
				if (drawableOpacity != 1f) {
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				}
			}
		}
	}
	
	public void setBackground(BufferedImage bg) {
		this.background = bg;
	}
	
	public void safeRepaint() {
		if (SwingUtilities.isEventDispatchThread()) {
			Stage.this.repaint();
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					Stage.this.repaint();
				}
			});
		}
	}
	
	public void addDrawable(Drawable d) {
		d.setParent(this);
		this.drawables.add(d);
	}

	public float getDrawableOpacity() {
		return drawableOpacity;
	}

	public void setDrawableOpacity(float drawableOpacity) {
		this.drawableOpacity = drawableOpacity;
	}

	public void clear() {
		this.drawables.clear();
	}

}