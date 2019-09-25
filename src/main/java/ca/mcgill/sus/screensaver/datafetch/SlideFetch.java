package ca.mcgill.sus.screensaver.datafetch;

import ca.mcgill.sus.screensaver.Util;
import ca.mcgill.sus.screensaver.io.Slide;

import java.util.ArrayList;
import java.util.List;

public class SlideFetch extends DataFetchable<List<Slide>> {

    private List<Slide> slides = new ArrayList<Slide>();

    private int iterations = 0;
    private final int refetchEvery;

    private boolean pullSlides() {
        return iterations++ % refetchEvery == 0;
    }

    SlideFetch(int _refetchEvery) {
        refetchEvery = _refetchEvery;
    }

    @Override
    public FetchResult<List<Slide>> fetch() {
        if (pullSlides() || slides == null || slides.isEmpty()) {
            slides = Util.loadSlides();
            return new FetchResult<>(slides, true);
        }
        // cached, disregard and do not change
        return new FetchResult<>(slides, false);
    }
}
