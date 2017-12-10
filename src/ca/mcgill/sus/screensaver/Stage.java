package ca.mcgill.sus.screensaver;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Stage extends JPanel {
	private static final long serialVersionUID = 2466660164920195989L;

	private final Queue<Drawable> drawables = new ConcurrentLinkedQueue<Drawable>();
	private BufferedImage background;
	private float drawableOpacity = 1f;
	private final int fps;
	private final AtomicBoolean dirty = new AtomicBoolean();

	public Stage(int fps) {
		super(true);
		this.fps = fps;
		this.setBackground(new Color(0x292929));
		new Thread("Draw") {
			public void run() {
				while (!Thread.interrupted()) {
					for (Drawable d : drawables) {
						if (d.isDirty()) dirty.set(true);
					}
					long startTime = System.nanoTime();
					if (dirty.get()) safeRepaint();
					dirty.set(false);
					long t = (System.nanoTime() - startTime) / 1000000;
					Util.sleep(1000 / Stage.this.fps - t);
				}
			}
		}.start();
		new Thread("Compute") {
			public void run() {
				while (!Thread.interrupted()) {
					for (Drawable d : drawables) {
						if (d.isDirty()) dirty.set(true);;
					}
					long startTime = System.nanoTime();
					for (Drawable d : drawables) d.step(System.nanoTime() / 1000000);
					long t = (System.nanoTime() - startTime) / 1000000;
					Util.sleep(1000 / Stage.this.fps - t);
				}
			}
		}.start();
	}

	@Override
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;
		int shortSide = this.getHeight() < this.getWidth() ? this.getHeight() : this.getWidth();
		double scaleFactor = shortSide != 1080 ? shortSide / 1080.0 : 1080;
//		double scaleFactor = 1;
		int scaledWidth = (int) (this.getWidth() / scaleFactor), 
		scaledHeight = (int) (this.getHeight() / scaleFactor);
		if (background != null) {
//			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//			g.scale(scaleFactor, scaleFactor);
			BufferedImage buffer = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
			g = buffer.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.drawImage(background, 0, 0, buffer.getWidth(), buffer.getHeight(), null);
			if (!drawables.isEmpty()) {
				if (drawableOpacity != 1f) {
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, drawableOpacity));
				}
				for (Drawable d : drawables) {
					d.draw(g, buffer, scaledWidth, scaledHeight);
					d.setDirty(false);
				}
				if (drawableOpacity != 1f) {
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				}


			}
			g.dispose();
			g = (Graphics2D) graphics;
			g.drawImage(buffer, 0, 0, null);
		}
	}
	
	public void setBackground(BufferedImage bg) {
		this.background = bg;
	}
	
	private void safeRepaint() {
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

	public void setDirty(boolean dirty) {
		this.dirty.set(dirty);
		
	}

}