package ca.mcgill.sus.screensaver.filters;

public class Overlay extends ColorFilter {

	@Override
	protected int filterChannel(int a, int b) {
		if (b < 128) {
			return 2 * a * b / 255;
		} else {
			return (int) (255 * (1 - 2 * (1 - a / 255.0) * (1 - b / 255.0)));
		}
	}

}
