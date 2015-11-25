package ca.mcgill.sus.screensaver.drawables;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import javax.imageio.ImageIO;

import ca.mcgill.sus.screensaver.Drawable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ProfilePic implements Drawable {
	private BufferedImage proPic = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	private Runnable onChange;
	
	public ProfilePic() {
		new Thread("Profile Picture Fetch") {
			@Override
			public void run() {
				BufferedImage proPic = getGravatar(128);
				if (proPic == null) {
					proPic = getImageResult();
				}
				synchronized(ProfilePic.this.proPic) {
					ProfilePic.this.proPic = circleCrop(proPic);
				}
				onChange();
			};
		}.start();
	}
	
	public static BufferedImage getGravatar(int s) {
		String apiUrl = "https://www.gravatar.com/avatar/";
		try (InputStream is = new URL(apiUrl + md5Hex(getUserPrincipalName()) + "?d=404&s=" + s).openStream()) {
			return ImageIO.read(is);
		} catch (Exception e) {
			new RuntimeException("Could not fetch data", e).printStackTrace();
		}
		return null;
	}
	
	public static BufferedImage getImageResult() {
		String apiUrl = "https://www.googleapis.com/customsearch/v1?key=AIzaSyB76V9lnO6sXbayHB8jB6rwydExoy2a-WA&cx=004537886587437653861:gvmhywuf8wi&searchType=image&q=";
		try (Reader r = new InputStreamReader(new URL(apiUrl + URLEncoder.encode("\"" + getFullUsername() + "\"" + " mcgill", "UTF-8")).openStream(), "UTF-8")) {
			JsonObject imageSearchResult = new JsonParser().parse(r).getAsJsonObject();
			String thumbnailUrl = imageSearchResult.get("items").getAsJsonArray().get(0).getAsJsonObject().get("image").getAsJsonObject().get("thumbnailLink").getAsString();
			try (InputStream is = new URL(thumbnailUrl).openStream()) {
				return ImageIO.read(is);
			} catch (Exception e) {
				new RuntimeException("Could not fetch data", e).printStackTrace();
			}
		} catch (Exception e) {
			new RuntimeException("Could not fetch data", e).printStackTrace();
		}
		return null;
	}
	
	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		synchronized(ProfilePic.this.proPic) {
			g.drawImage(proPic, 10, 10, null);
		}
	}

	@Override
	public ProfilePic setOnChange(Runnable r) {
		this.onChange = r;
		return this;
	}
	
	private void onChange() {
		if (onChange != null) {
			onChange.run();
		}
	}
	
	public static String getFullUsername() {
		String username = System.getenv("username");
		try (Scanner s = new Scanner(Runtime.getRuntime().exec(new String[]{"net", "user", username, "/domain"}).getInputStream())) {
			s.useDelimiter("\r\n");
			while (s.hasNext()) {
				String line = s.next();
				if (line.startsWith("Full Name")) {
					return (line.split("\\s{2,}")[1]);
				}
			}
		} catch (IOException | ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String hex(byte[] array) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < array.length; ++i) {
			sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(
					1, 3));
		}
		return sb.toString();
	}

	public static String md5Hex(String message) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			return hex(md.digest(message.getBytes("CP1252")));
		} catch (NoSuchAlgorithmException e) {
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
	  
	public static String getUserPrincipalName() {
		try (Scanner s = new Scanner(Runtime.getRuntime().exec(new String[]{"whoami", "/upn"}).getInputStream())) {
			s.useDelimiter("\r\n");
			while (s.hasNext()) {
				String line = s.next().trim();
				if (!line.isEmpty()) {
					return line;
				}
			}
		} catch (IOException | ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static BufferedImage circleCrop(BufferedImage image) {
	    int w = image.getWidth(), h = image.getHeight();
	    BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g = out.createGraphics();
	    g.setComposite(AlphaComposite.Src);
	    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	    g.setColor(Color.WHITE);
	    g.fillOval(0, 0, w, h);
	    g.setComposite(AlphaComposite.SrcAtop);
	    g.drawImage(image, 0, 0, null);
	    g.dispose();
	    return out;
	}

}
