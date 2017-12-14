package ca.mcgill.sus.screensaver.filters;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public abstract class Filter {

	public BufferedImage filter(BufferedImage top, BufferedImage bottom) {
		BufferedImage output = new BufferedImage(top.getWidth(), top.getHeight(), BufferedImage.TYPE_INT_ARGB);
		int[] pxTop = ((DataBufferInt) top.getRaster().getDataBuffer()).getData(),
		pxBottom = ((DataBufferInt) bottom.getRaster().getDataBuffer()).getData(),
		pxOutput = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();
		for (int i = 0; i < pxOutput.length; i++) {
			pxOutput[i] = filter(pxTop[i], pxBottom[i]);
		}
		return output;
	}
	
	protected abstract int filter(int a, int b);

}
