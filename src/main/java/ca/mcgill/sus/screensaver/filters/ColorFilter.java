package ca.mcgill.sus.screensaver.filters;

public abstract class ColorFilter extends Filter {
	
	@Override
	public int filter(int a, int b) {
		int r1 = 0xff & (a >> 16), g1 = 0xff & (a >> 8), b1 = 0xff & a;
		int r2 = 0xff & (b >> 16), g2 = 0xff & (b >> 8), b2 = 0xff & b;
		return 0xff000000 | ((filterChannel(r1, r2) << 16) | (filterChannel(g1, g2) << 8) | filterChannel(b1, b2));
	}
	
	protected abstract int filterChannel(int a, int b);
}
