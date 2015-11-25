package ca.mcgill.sus.screensaver.drawables;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import ca.mcgill.sus.screensaver.FontManager;

public class UserInfoBar extends Header {

	private Runnable onChange;
	public final boolean officeComputer = System.getenv("computerName").startsWith("520-S"),
	loggedIn = !System.getenv("username").equals("SYSTEM");
	public String displayName = "";
	private final long startTime = System.currentTimeMillis();
//	private final Color textColor = loggedIn ? new Color(0x707070) : new Color(0xcccccc);
	private final Color textColor = new Color(0x44ffffff, true);

	
	public UserInfoBar(int size, int y) {
		//old green: 0x40bb33
		super(null, size, y, System.getenv("username").equals("SYSTEM") ? 0xdd33691e : 0xdddc241f);
		new Thread("User Info") {
			@Override
			public void run() {
				try {
					Map<String, String> userInfo = dsGet(getFQDN(), "-fn", "-ln", "-display");
					if (userInfo.size() == 0) {
						displayName = System.getenv("username");
					} else {
						if (officeComputer) {
							displayName = userInfo.get("fn");
						} else {
							displayName = String.format("%s. %s", userInfo.get("fn").charAt(0), userInfo.get("ln"));
						}
					}
				} catch (Exception e) {
					displayName = System.getenv("username");
				}
//				displayName = "Jae Yong";
				onChange();
			}
		}.start();
	}
	
	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		//time away from machine rounded to 5 minute intervals
		int timeAway = (int) (((System.currentTimeMillis() - this.startTime) / 1000 / 60 / 5) * 5);
		String text;
		if (loggedIn && timeAway > 120) {
			text = "SEEMS LIKE IT'S BEEN FOREVER " + displayName.toUpperCase() + " HAS BEEN GONE";
		} else if (loggedIn && timeAway > 60) {
				text = displayName.toUpperCase() + " HAS BEEN AWAY FOR OVER AN HOUR";
		} else if (loggedIn && timeAway >= 5) {
				text = String.format(displayName.toUpperCase() + " HAS BEEN AWAY FOR ABOUT %d MINUTES", timeAway);
		} else if (loggedIn) {
			text = "THIS COMPUTER IS IN USE BY " + displayName.toUpperCase();
		} else {
			text = "PRESS CTRL + ALT + DELETE TO LOG IN";
		}
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(FontManager.getInstance().getFont(bold ? "constanb.ttf" : "constan.ttf").deriveFont((float) size));
		int x;
		switch (alignment) {
		case ALIGN_LEFT:
			x = 10;
			break;
		case ALIGN_RIGHT:
			x = canvasWidth - g.getFontMetrics().stringWidth(text) - 10;
			break;
		default:
		case ALIGN_CENTER:
			x = canvasWidth / 2 - g.getFontMetrics().stringWidth(text) / 2;
			break;
		}
		g.setColor(color);
		GlyphVector v = getStringVector(g, text);
		Rectangle sb = getRealStringBounds(v);
		int pad = 14;
		g.fillRect(0, y - sb.height / 2 - pad, canvasWidth, sb.height + pad * 2);
		g.setColor(textColor);
//		g.drawString(text, x, y);
	    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	    g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
		g.fill(v.getOutline(x, y + pad));
	    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
		g.fill(v.getOutline(x, y + pad));
	}
	
	@Override
	public UserInfoBar setOnChange(Runnable r) {
		this.onChange = r;
		return this;
	}
	
	private void onChange() {
		if (onChange != null) {
			onChange.run();
		}
	}
	
	public static Rectangle getRealStringBounds(GlyphVector v) {
		Rectangle2D rect = v.getVisualBounds();
		return new Rectangle((int) rect.getX(), (int) rect.getY(), (int) rect.getWidth(), (int) rect.getHeight());
	}
	
	public static GlyphVector getStringVector(Graphics2D g, String text) {
		Font font = g.getFont();
		FontMetrics fontMetrics = g.getFontMetrics();
		return font.createGlyphVector(fontMetrics.getFontRenderContext(), text);
	}
	
	public static Map<String, String> dsGet(String user, String... args) {
		Map<String, String> out = new HashMap<>();
		try (Scanner s = new Scanner(Runtime.getRuntime().exec(concat(new String[]{"dsget", "user", user}, args)).getInputStream(), "Cp850")) {
			s.useDelimiter("\r\n");
			String headers = s.next(),
			results = s.next();
			String[] indvHeaders = headers.trim().split("\\s+");
			int[] columns = new int[indvHeaders.length];
			for (int i = 0; i < columns.length; i++) {
				columns[i] = headers.indexOf(indvHeaders[i]);
			}
			String[] indvResults = new String[columns.length];
			for (int i = 0; i < indvResults.length; i++) {
				int start = columns[i],
				end = i + 1 >= columns.length ? results.length() : columns[i + 1];
				out.put(indvHeaders[i], results.substring(start, end).trim());
			}
		} catch (IOException | ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		return out;
	}
	
	public static String dsQuery(String user, String... args) {
		try (Scanner s = new Scanner(Runtime.getRuntime().exec(concat(new String[]{"dsquery", "user", "-samid", user}, args)).getInputStream(), "Cp850")) {
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
	
	public static String getFQDN() {
		try (Scanner s = new Scanner(Runtime.getRuntime().exec(new String[]{"whoami", "/fqdn"}).getInputStream())) {
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
	
	public static <T> T[] concat(T[] a1, T[] a2) {
		T[] out = Arrays.copyOf(a1, a1.length + a2.length);
		System.arraycopy(a2, 0, out, a1.length, a2.length);
		return out;
	}

}
