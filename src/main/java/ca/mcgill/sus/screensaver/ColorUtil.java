package ca.mcgill.sus.screensaver;

public class ColorUtil {

	public static int getAlpha(int color){
		return ((color >> 24) & 0xff);
	}

	public static boolean hasAlpha (int color){
		return (((color >> 24) & 0xff) > 0);
	}

	public static int getColor(int color){
		return (color & 0xffffff);
	}

}
