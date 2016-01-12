package ca.mcgill.sus.screensaver.drawables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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

import com.google.gson.Gson;

import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;
import ca.mcgill.sus.screensaver.io.OhName;

public class OfficeHoursMarquee implements Drawable {
	
	private final Queue<String> names = new ConcurrentLinkedQueue<>(Arrays.asList(new String[]{""}));
	private ScheduledFuture<?> dataFetchHandle, marqueeHandle;
	public final int y;
	private int alphaEntry = 0;
	private String currentEntry = "";
	private final String title = "CURRENT VOLUNTEERS ON DUTY";
	private Runnable onChange;
	private final int color, maxAlpha;
	
	public OfficeHoursMarquee(int y, int color) {
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
		g.setColor(new Color((maxAlpha << 24) | color, true));
		g.drawString(title, canvasWidth / 2 - g.getFontMetrics().stringWidth(title) / 2, y);
		g.setFont(FontManager.getInstance().getFont("nhg-thin.ttf").deriveFont(24f));
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
				System.out.println("Fetching OH data");
				try (Reader r = new InputStreamReader(new URL("https://cups.sus.mcgill.ca/functions/office_hours_names_json.php").openStream(), "UTF-8")) {
					names.clear();
					OhName[][][] oh = new Gson().fromJson(r, OhName[][][].class);
					Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
					calendar.setTime(new Date());
					int hour = calendar.get(Calendar.HOUR_OF_DAY),
					min = calendar.get(Calendar.MINUTE),
					day = calendar.get(Calendar.DAY_OF_WEEK) - 2,
					slot = (hour - 9) * 2 + (min > 30 ? 1 : 0);
					if (day < oh.length && slot < oh[day].length) {
						for (OhName name : oh[day][slot]) {
							int until;
							for (until = slot; until < oh[day].length; until++) {
								if (!Arrays.asList(oh[day][until]).contains(name)) {
									break;
								}
							}
							names.add(name.first_name + " " + name.last_name + " (until " + (9 + until / 2) + (until % 2 == 0 ? ":00" : ":30") + ")");
						}
					} else {
						names.add("Nobody!");
					}
					if (marqueeHandle == null) {
						startMarquee();
					}
				} catch (Exception e) {
					new RuntimeException("Could not fetch data", e).printStackTrace();
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
					OfficeHoursMarquee.this.onChange();
					OfficeHoursMarquee.sleep(fadeInMs / maxAlpha);
				}
				synchronized(OfficeHoursMarquee.this.currentEntry) {
					OfficeHoursMarquee.this.currentEntry = entry;
				}
				while (alphaEntry < maxAlpha) {
					alphaEntry++;
					OfficeHoursMarquee.this.onChange();
					OfficeHoursMarquee.sleep(fadeOutMs / maxAlpha);
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
			Iterator<String> iterNames = names.iterator();
			public void run() {
				try {
					if (!iterNames.hasNext()) {
						iterNames = names.iterator();
					}
					if (iterNames.hasNext()) changeEntry(iterNames.next());
					OfficeHoursMarquee.this.onChange();
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
	public OfficeHoursMarquee setOnChange(Runnable r) {
		this.onChange = r;
		return this;
	}
	
	private void onChange() {
		if (onChange != null) {
			onChange.run();
		}
	}

}
