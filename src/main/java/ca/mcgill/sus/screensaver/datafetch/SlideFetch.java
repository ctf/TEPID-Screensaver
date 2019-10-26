package ca.mcgill.sus.screensaver.datafetch;

import ca.mcgill.sus.screensaver.Config;
import ca.mcgill.sus.screensaver.io.Slide;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

public class SlideFetch extends DataFetchable<List<Slide>> {

    SlideFetch() {
    }

	@Override
	public FetchResult<List<Slide>> fetch() {
		Map<String, BufferedImage> images = new HashMap<>();
		try {
			for (File f : new File(Config.INSTANCE.getAnnouncement_slide_directory()).listFiles()) {
				try {
					images.put(f.getName(), ImageIO.read(f));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Map<String, Slide> slides = new HashMap<>();
		for (Map.Entry<String, BufferedImage> e : images.entrySet()) {
			if (e.getValue() == null) continue;
			String name = (e.getKey().contains(".") ? e.getKey().substring(0, e.getKey().indexOf(".")): e.getKey()).toLowerCase();
			boolean light = name.endsWith("_light");
			if (light) name = name.substring(0, name.indexOf("_light"));
			if (!slides.containsKey(name)) slides.put(name, new Slide(name));
			if (light) slides.get(name).light = e.getValue();
			else slides.get(name).dark = e.getValue();
		}
		List<Slide> out = new ArrayList<>(slides.values());
		for (Slide s : out) {
			if (s.light == null) s.light = s.dark;
			if (s.dark == null) s.dark = s.light;
		}
		out.sort(Comparator.comparing(s -> s.name));
		value = out;
		return new FetchResult<>(value, true);
	}
}
