package util;

import ca.mcgill.sus.screensaver.Util;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BufferedImageUtilsTest {

	private String testAssetPath = "out/test/classes/";
	private String assetPath = "src/main/resources/ca/mcgill/sus/screensaver/";

	/**
	 * Taken wholesale from https://stackoverflow.com/a/15305092/1947070
	 * Code by jazzbassrob, edited by Nicholas DiPiazza
	 * @param img1
	 * @param img2
	 * @return
	 */

	private boolean bufferedImagesEqual(BufferedImage img1, BufferedImage img2) {
		if (img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight()) {
			for (int x = 0; x < img1.getWidth(); x++) {
				for (int y = 0; y < img1.getHeight(); y++) {
					if (img1.getRGB(x, y) != img2.getRGB(x, y))
						return false;
				}
			}
		} else {
			return false;
		}
		return true;
	}

	private BufferedImage loadImage(String path){
		try {
			File f = new File(path);
			return ImageIO.read(f);
		} catch (IOException e){
			fail("IOexception: test asset missing");
		}
		return null;
	}

	@Test
	public void testColor(){
		BufferedImage input = loadImage(assetPath + "sprites/printer.png");
		BufferedImage actual = Util.color(input, 0x8888ccff);
		BufferedImage expected = loadImage(testAssetPath + "util/goal_color.png");
		assertTrue(bufferedImagesEqual(expected, actual));
	}

	@Test
	public void testBlur(){
		BufferedImage input = loadImage(testAssetPath + "test_image.png");
		BufferedImage actual = Util.blur(5, input);
		BufferedImage expected = loadImage(testAssetPath + "util/goal_blur.png");
		assertTrue(bufferedImagesEqual(expected, actual));
	}
/*	*//**
	 * 	A function for generating target images assuming everything works right
	 *//*
	@Test
	public void makeify(){
		try {
			BufferedImage input = loadImage(testAssetPath + "test_image.png");

			BufferedImage o = Util.circleCrop( input);

			File f = new File("goal_circleCrop.png");
			ImageIO.write(o, "png", f);
		}
		catch(IOException e){
			fail("NI");
		}
		fail("Not a real test");
	}*/
}