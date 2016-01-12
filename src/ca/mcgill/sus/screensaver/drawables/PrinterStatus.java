package ca.mcgill.sus.screensaver.drawables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;
import ca.mcgill.sus.screensaver.Main;
import ca.mcgill.sus.screensaver.SpriteManager;

public class PrinterStatus implements Drawable {
	private Map<String, Boolean> status = new ConcurrentHashMap<>();
	public final int y, padding;
	private Runnable onChange;
	private ScheduledFuture<?> dataFetchHandle;
	
	public PrinterStatus(int y, int padding) {
		this.y = y;
		this.padding = padding;
		startDataFetch();
	}
	
	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		BufferedImage printers = generatePrinters();
		if (printers != null) g.drawImage(printers, canvasWidth / 2 - printers.getWidth() / 2, y, null);
	}
	
	private BufferedImage generatePrinters() {
		BufferedImage out = null;
		int i = 0;
		List<Entry<String, Boolean>> status = new ArrayList<>(this.status.entrySet());
		Collections.sort(status, new Comparator<Entry<String, Boolean>>() {
			@Override
			public int compare(Entry<String, Boolean> e1, Entry<String, Boolean> e2) {
				return e1.getKey().compareTo(e2.getKey());
			}

		});
		for (Entry<String, Boolean> s : status) {
			BufferedImage printer = generatePrinter(s.getKey(), s.getValue());
			if (out == null) {
				out = new BufferedImage((printer.getWidth() + padding * 2) * status.size(), printer.getHeight(), BufferedImage.TYPE_INT_ARGB);
			}
			Graphics2D g = out.createGraphics();
			g.drawImage(printer, i * (printer.getWidth() + padding * 2) + padding, 0, null);
			g.dispose();
			i++;
		}
		return out;
	}
	
	private static BufferedImage generatePrinter(String name, boolean up) {
		int vpad = 16, fontSize = 32;
		BufferedImage printer = SpriteManager.getInstance().getColoredSprite("printer.png", up ? Main.COLOR_UP : Main.COLOR_DOWN),
		emoji = SpriteManager.getInstance().getSprite(up ? "smile.png" : "frown.png"),
		out = new BufferedImage(printer.getWidth(), printer.getHeight() + vpad + fontSize + vpad + emoji.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(printer, 0, 0, null);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(FontManager.getInstance().getFont("constanb.ttf").deriveFont((float) fontSize));
		g.setColor(new Color(up ? Main.COLOR_UP : Main.COLOR_DOWN, true));
		g.drawString(name, out.getWidth() / 2 - g.getFontMetrics().stringWidth(name) / 2, printer.getHeight() + vpad + fontSize);
		g.drawImage(emoji, out.getWidth() / 2 - emoji.getWidth() / 2, printer.getHeight() + vpad + fontSize + vpad, null);
		g.dispose();
		return out;
	}
	
	public void startDataFetch() {
		//TODO figure out why cert isn't validating
		trustAllCerts();
		final Runnable dataFetch = new Runnable() {
			public void run() {
				try (Reader r = new InputStreamReader(new URL("https://cups.sus.mcgill.ca/bob/status.php").openStream(), "UTF-8")) {
					Map<String, Boolean> newStatus = new Gson().fromJson(r, new TypeToken<Map<String, Boolean>>(){}.getType());
//					newStatus = new Gson().fromJson("{\"1B16\":true,\"1B17\":false,\"1B18\":true} ", new TypeToken<Map<String, Boolean>>(){}.getType());
					status.clear();
					status.putAll(newStatus);
					PrinterStatus.this.onChange();
				} catch (Exception e) {
					new RuntimeException("Could not fetch data", e).printStackTrace();
				}
			}
		};
		if (dataFetchHandle != null) dataFetchHandle.cancel(false);
		dataFetchHandle = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(dataFetch, 0, 60, TimeUnit.SECONDS);
	}
	
	public void stopDataFetch() {
		if (dataFetchHandle != null) dataFetchHandle.cancel(false);
	}
	
	public static void trustAllCerts() {
		TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
		    public X509Certificate[] getAcceptedIssuers(){return null;}
		    public void checkClientTrusted(X509Certificate[] certs, String authType){}
		    public void checkServerTrusted(X509Certificate[] certs, String authType){}
		}};
		try {
		    SSLContext sc = SSLContext.getInstance("TLS");
		    sc.init(null, trustAllCerts, new SecureRandom());
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
		    ;
		}
	}

	@Override
	public PrinterStatus setOnChange(Runnable r) {
		this.onChange = r;
		return this;
	}
	
	private void onChange() {
		if (onChange != null) {
			onChange.run();
		}
	}

}
