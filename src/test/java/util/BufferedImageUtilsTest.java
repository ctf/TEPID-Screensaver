package util;

import ca.mcgill.sus.screensaver.Util;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

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

@RunWith(JUnitParamsRunner.class)
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

	@Test
	public void testCanTakeScreenshotBounds(){
		BufferedImage screenshot = null;
		try {
			screenshot = Util.screenshot(new Rectangle(40,50));
		} catch (Exception e){
			fail("Threw exception while taking screenshot");
		}
		if (screenshot == null){
			fail("Screenshot is null");
		}
	}

	@Test
	public void testCanTakeScreenshotDisplay(){
		BufferedImage screenshot = null;
		try {
			screenshot = Util.screenshot(0);
		} catch (Exception e){
			fail("Threw exception while taking screenshot");
		}
		if (screenshot == null){
			fail("Screenshot is null");
		}
	}

	@Ignore
	@Test
	public void testLoadBackground(){
		fail("NI: testing this will be much easier with the path as a parameter");
	}

	@Ignore
	@Test
	public void testLoadSlides(){
		fail("NI: testing this will be much easier with the path as a parameter");
	}

	@Test
	@Parameters({"1432778632, 0.45618222479862386", 	//0x55667788
			"2003195204, 0.34809321867696424"		//0x77665544
	})
	public void luminanceTest(int argb, double expected){
		assertThat(Util.luminance(argb), is(expected));
	}

	@Test
	public void luminanceAvgTest(){
		BufferedImage input = loadImage(testAssetPath + "test_image.png");
		input = Util.convert(input, BufferedImage.TYPE_INT_ARGB);
		double actual = Util.luminanceAvg(input, 20,30,50,40);
		double expected = 0.4327436850953641;
		assertEquals(expected, actual, 0.1);
	}

	@Test
	public void circleCropTest(){
		BufferedImage input = loadImage(testAssetPath + "test_image.png");
		BufferedImage actual = Util.circleCrop(input);
		BufferedImage expected = loadImage(testAssetPath + "util/goal_circleCrop.png");
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