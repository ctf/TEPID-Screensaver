package ca.mcgill.sus.screensaver.drawables;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;
import ca.mcgill.sus.screensaver.io.MarqueeData;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

public class Marquee implements Drawable {
	
	private final Queue<MarqueeData> marqueeData = new ConcurrentLinkedQueue<>();
	private ScheduledFuture<?> dataFetchHandle, marqueeHandle;
	public final int y;
	private int alphaTitle = 0, alphaEntry = 0;
	private String currentTitle = "", currentEntry = "";
	private Runnable onChange;
	private final int color, maxAlpha;
	
	public Marquee(int y, int color) {
		startDataFetch();
		this.y = y;
		this.color = 0xffffff & color;
		if (((color >> 24) & 0xff) > 0) { 
			maxAlpha = (color >> 24) & 0xff;
		} else {
			maxAlpha = 0xff;
		}
	}

	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(FontManager.getInstance().getFont("constanb.ttf").deriveFont(32f));
		g.setColor(new Color((alphaTitle << 24) | color, true));
		synchronized(currentTitle) {
			g.drawString(currentTitle, canvasWidth / 2 - g.getFontMetrics().stringWidth(currentTitle) / 2, y);
		}
		g.setFont(new Font("Times New Roman", Font.PLAIN, 20));
		g.setColor(new Color((alphaEntry << 24) | color, true));
		synchronized(currentEntry) {
			g.drawString(currentEntry, canvasWidth / 2 - g.getFontMetrics().stringWidth(currentEntry) / 2, y + 40);
		}
	}
	
	public void startDataFetch() {
		//TODO figure out why cert isn't validating
		trustAllCerts();
		final Runnable dataFetch = new Runnable() {
			public void run() {
				System.out.println("Fetching marquee data");
				try (Reader r = new InputStreamReader(new URL("https://cups.sus.mcgill.ca/functions/newsFeedApi.php").openStream(), "UTF-8")) {
					marqueeData.clear();
					marqueeData.addAll(Arrays.asList(new Gson().fromJson(new JsonParser().parse(r).getAsJsonObject().get("ctf"), MarqueeData[].class)));
					if (marqueeHandle == null) {
						startMarquee();
					}
				} catch (Exception e) {
					new RuntimeException("Could not fetch data", e).printStackTrace();;
				}
				System.out.println("Done");
			}
		};
		if (dataFetchHandle != null) dataFetchHandle.cancel(false);
		dataFetchHandle = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(dataFetch, 0, 60, TimeUnit.SECONDS);
	}
	
	public void stopDataFetch() {
		if (dataFetchHandle != null) dataFetchHandle.cancel(false);
	}
	
	public void changeEntry(final String entry) {
		new Thread("Change Entry") {
			@Override
			public void run() {
				final int fadeInMs = 1400, fadeOutMs = 800;
				while (alphaEntry > 0) {
					alphaEntry--;
					Marquee.this.onChange();
					Marquee.sleep(fadeInMs / maxAlpha);
				}
				synchronized(Marquee.this.currentEntry) {
					Marquee.this.currentEntry = entry;
				}
				while (alphaEntry < maxAlpha) {
					alphaEntry++;
					Marquee.this.onChange();
					Marquee.sleep(fadeOutMs / maxAlpha);
				}
			}
		}.start();
	}
	
	public void changeTitle(final String title) {
		new Thread("Change Entry") {
			@Override
			public void run() {
				final int fadeInMs = 1400, fadeOutMs = 800;
				while (alphaTitle > 0) {
					alphaTitle--;
					Marquee.this.onChange();
					Marquee.sleep(fadeInMs / maxAlpha);
				}
				synchronized(Marquee.this.currentTitle) {
					Marquee.this.currentTitle = title;
				}
				while (alphaTitle < maxAlpha) {
					alphaTitle++;
					Marquee.this.onChange();
					Marquee.sleep(fadeOutMs / maxAlpha);
				}
			}
		}.start();
	}
	
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void startMarquee() {
		//TODO figure out why cert isn't validating
		trustAllCerts();
		final Runnable marquee = new Runnable() {
			Iterator<MarqueeData> iterMarquee = marqueeData.iterator();
			Iterator<String> iterEntry;
			MarqueeData md;
			public void run() {
				try {
					if (!iterMarquee.hasNext()) {
						iterMarquee = marqueeData.iterator();
					}
					if (iterEntry == null || !iterEntry.hasNext()) {
						md = iterMarquee.next();
						changeTitle(md.title);
						iterEntry = md.entry.iterator();
					}
					changeEntry(iterEntry.next());
					Marquee.this.onChange();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		if (marqueeHandle != null) marqueeHandle.cancel(false);
		marqueeHandle = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(marquee, 0, 5, TimeUnit.SECONDS);
	}
	
	public void stopMarquee() {
		if (marqueeHandle != null) marqueeHandle.cancel(false);
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
	public Marquee setOnChange(Runnable r) {
		this.onChange = r;
		return this;
	}
	
	private void onChange() {
		if (onChange != null) {
			onChange.run();
		}
	}

}
