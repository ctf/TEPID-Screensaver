package ca.mcgill.sus.screensaver.datafetch;

import ca.mcgill.sus.screensaver.Util;
import ca.mcgill.sus.screensaver.io.Slide;

import java.util.List;

public class SlideFetch extends DataFetchable<List<Slide>> {

    SlideFetch() {
    }

	@Override
	public FetchResult<List<Slide>> fetch() {
		value = Util.loadSlides();
		return new FetchResult<>(value, true);
	}
}
