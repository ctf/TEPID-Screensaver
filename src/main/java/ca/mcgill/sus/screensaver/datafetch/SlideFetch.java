package ca.mcgill.sus.screensaver.datafetch;

import ca.mcgill.sus.screensaver.Util;
import ca.mcgill.sus.screensaver.io.Slide;

import java.util.ArrayList;
import java.util.List;

public class SlideFetch extends DataFetchable<List<Slide>> {

	private List<Slide> slides = new ArrayList<>();

    SlideFetch() {
    }

	@Override
	public FetchResult<List<Slide>> fetch() {
		slides = Util.loadSlides();
		return new FetchResult<>(slides, true);
	}
}
