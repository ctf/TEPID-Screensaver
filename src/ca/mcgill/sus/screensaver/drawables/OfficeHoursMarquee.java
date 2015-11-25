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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

public class OfficeHoursMarquee implements Drawable {
	
	private final Queue<String> names = new ConcurrentLinkedQueue<>(Arrays.asList(new String[]{""}));
	private ScheduledFuture<?> dataFetchHandle, marqueeHandle;
	public final int y;
	private int alphaEntry = 0;
	private String currentEntry = "";
	private final String title = "CURRENT VOLUNTEERS ON DUTY";
	private Runnable onChange;
	
	public OfficeHoursMarquee(int y) {
		startDataFetch();
		this.y = y;
	}

	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(FontManager.getInstance().getFont("constanb.ttf").deriveFont(32f));
		g.setColor(Color.WHITE);
		g.drawString(title, canvasWidth / 2 - g.getFontMetrics().stringWidth(title) / 2, y);
		g.setFont(new Font("Times New Roman", Font.PLAIN, 26));
		g.setColor(new Color((alphaEntry << 24) | 0xffffff, true));
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
				try (Reader r = new InputStreamReader(new URL("https://cups.sus.mcgill.ca/functions/office_hours_json.php").openStream(), "UTF-8")) {
					names.clear();
					String[][] oh = new Gson().fromJson(r, String[][].class);
					Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
					calendar.setTime(new Date());
					int hour = calendar.get(Calendar.HOUR_OF_DAY),
					min = calendar.get(Calendar.MINUTE),
					day = calendar.get(Calendar.DAY_OF_WEEK) - 2,
					slot = (hour - 9) * 2 + (min > 30 ? 1 : 0);
					if (day < oh.length && slot < oh[day].length) {
						names.addAll(Arrays.asList(oh[day][slot].split(",")));
					} else {
						names.add("Nobody!");
					}
					new Thread("Office Hours Real Names"){
						@Override
						public void run() {
							Iterator<String> iterNames = names.iterator();
							List<String> fullNames = new ArrayList<>();
							while (iterNames.hasNext()) {
								String name = iterNames.next();
								if (name.equals("Nobody!")) fullNames.add("Nobody!");
								Map<String, String> userInfo = UserInfoBar.dsGet(UserInfoBar.dsQuery(name),"-fn", "-ln", "-display");
								fullNames.add(userInfo.get("display"));
							}
							names.clear();
							names.addAll(fullNames);
						}
					}.start();
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
					OfficeHoursMarquee.sleep(fadeInMs / 0xff);
				}
				synchronized(OfficeHoursMarquee.this.currentEntry) {
					OfficeHoursMarquee.this.currentEntry = entry;
				}
				while (alphaEntry < 0xff) {
					alphaEntry++;
					OfficeHoursMarquee.this.onChange();
					OfficeHoursMarquee.sleep(fadeOutMs / 0xff);
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
