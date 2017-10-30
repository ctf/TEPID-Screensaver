package ca.mcgill.sus.screensaver;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class AnimatedSprite {
	
	private final BufferedImage[] frames;
	private int frame, speedMs = 100;
	private Stage parent;
	
	public AnimatedSprite(BufferedImage[] frames) {
		this.frames = frames;
		new Thread("Sprite Animation") {
			@Override
			public void run() {
				for (;;) {
					frame = (frame + 1) % AnimatedSprite.this.frames.length;
					onChange();
					try {
						Thread.sleep(speedMs);
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		}.start();
	}
	
	public void draw(Graphics2D g, int x, int y) {
		g.drawImage(frames[frame], x, y, null);
	}
	
	public void setParent(Stage parent) {
		this.parent = parent;
	}
	
	private void onChange() {
		if (parent != null) {
			if (parent != null) parent.safeRepaint();
		}
	}

	public int getSpeedMs() {
		return speedMs;
	}

	public AnimatedSprite setSpeedMs(int speedMs) {
		this.speedMs = speedMs;
		return this;
	}
	
	public int getWidth() {
		return frames[0].getWidth();
	}
	
	
	public int getHeight() {
		return frames[0].getHeight();
	}
}
