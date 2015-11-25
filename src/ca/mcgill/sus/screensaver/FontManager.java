package ca.mcgill.sus.screensaver;

import java.awt.Font;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FontManager {
	private static final FontManager INSTANCE = new FontManager();

	private Map<String, Font> cache = new ConcurrentHashMap<>();

	private FontManager() {}

	public static FontManager getInstance() {
		return INSTANCE;
	}

	public Font getFont(String name) {
		Font font = cache.get(name);
		if (font != null) {
			return font;
		}
		try {
			InputStream is = FontManager.class.getResourceAsStream("fonts/" + name);
			font = Font.createFont(Font.TRUETYPE_FONT, is);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not load font: " + name, e);
		}
		if (font == null) {
			throw new RuntimeException("Could not load font: " + name);
		}
		cache.put(name, font);
		return font;
	}
}