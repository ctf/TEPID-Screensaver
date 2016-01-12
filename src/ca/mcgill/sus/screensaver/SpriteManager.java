package ca.mcgill.sus.screensaver;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

public class SpriteManager {
	private static final SpriteManager INSTANCE = new SpriteManager();

	private Map<String, BufferedImage> cache = new ConcurrentHashMap<>();
	private Map<String, AnimatedSprite> animCache = new ConcurrentHashMap<>();

	private SpriteManager() {}

	public static SpriteManager getInstance() {
		return INSTANCE;
	}

	public BufferedImage getSprite(String name) {
		BufferedImage sprite = cache.get(name);
		if (sprite != null) {
			return sprite;
		}
		try {
			InputStream is = SpriteManager.class.getResourceAsStream("sprites/" + name);
			sprite = ImageIO.read(is);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not load sprite: " + name, e);
		}
		if (sprite == null) {
			throw new RuntimeException("Could not load sprite: " + name);
		}
		cache.put(name, sprite);
		return sprite;
	}
	
	public BufferedImage getColoredSprite(String name, int color) {
		BufferedImage sprite = cache.get(name + Integer.toHexString(color));
		if (sprite != null) {
			return sprite;
		}
		sprite = Util.color(getSprite(name), color);
		cache.put(name + Integer.toHexString(color), sprite);
		return sprite;
	}
	
	public AnimatedSprite getAnimatedSprite(String name, int cols, int rows) {
		AnimatedSprite sprite = animCache.get(name);
		if (sprite != null) {
			return sprite;
		}
		try {
			InputStream is = SpriteManager.class.getResourceAsStream("sprites/" + name);
			BufferedImage sheet = ImageIO.read(is);
			int w = sheet.getWidth() / cols, 
			h = sheet.getHeight() / rows;
			BufferedImage[] frames = new BufferedImage[cols * rows];
			for (int i = 0; i < frames.length; i++) {
				int x = i % cols * w, y = i / cols * h;
				frames[i] = Util.convert(sheet.getSubimage(x, y, w, h), BufferedImage.TYPE_INT_ARGB);
			}
			sprite = new AnimatedSprite(frames);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not load animated sprite: " + name, e);
		}
		animCache.put(name, sprite);
		return sprite;
	}
}