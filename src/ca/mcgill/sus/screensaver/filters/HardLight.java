package ca.mcgill.sus.screensaver.filters;

public class HardLight extends ColorFilter {

	@Override
	protected int filterChannel(int a, int b) {
		if (a < 128) {
			return 2 * b * a / 255;
		} else {
			return (int) (255 * (1 - 2 * (1 - b / 255.0) * (1 - a / 255.0)));
		}
	}

}
