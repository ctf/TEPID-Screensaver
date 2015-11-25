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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NowPlaying implements Drawable {
	
	private ScheduledFuture<?> dataFetchHandle;
	public final int y;
	private int alphaEntry = 0;
	private String currentSong = "";
	private Runnable onChange;
	
	public NowPlaying(int y) {
		startDataFetch();
		this.y = y;
	}

	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(FontManager.getInstance().getFont("constanb.ttf").deriveFont(32f));
		g.setColor(Color.WHITE);
		final String title = "NOW PLAYING";
		int titleWidth = g.getFontMetrics().stringWidth(title);
		g.drawString(title, canvasWidth / 2 - titleWidth / 2, y);
		g.setFont(new Font("Arial", Font.BOLD, 20));
		g.setColor(new Color((alphaEntry << 24) | 0xffffff, true));
		synchronized(currentSong) {
			g.drawString(currentSong, canvasWidth / 2 - g.getFontMetrics().stringWidth(currentSong) / 2, y + 40);
		}
	}
	
	public void startDataFetch() {
		//TODO figure out why cert isn't validating
		trustAllCerts();
		final Runnable dataFetch = new Runnable() {
			public void run() {
				System.out.println("Fetching now playing");
				try (Reader r = new InputStreamReader(new URL("https://grunt.sus.mcgill.ca:8443/songs/current").openStream(), "UTF-8")) {
					JsonObject song = new JsonParser().parse(r).getAsJsonObject();
					String title = song.has("Title") ? song.get("Title").getAsString() : "",
					artist = song.has("Artist") ? song.get("Artist").getAsString() : "",
					file = song.has("file") ? song.get("file").getAsString() : "",
					display;
					if (!title.isEmpty() && !artist.isEmpty()) {
						display = String.format("%s - %s", artist, title);
					} else if (!artist.isEmpty()) {
						display = artist;
					} else if (!title.isEmpty()) {
						display = title;
					} else {
						display = file.substring(file.indexOf('/'));
						display = display.substring(0, display.indexOf('.'));
					}
					if (!currentSong.equals(display)) {
						changeSong(display);
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
	
	public void changeSong(final String song) {
		new Thread("Change Song") {
			@Override
			public void run() {
				final int fadeInMs = 1400, fadeOutMs = 800;
				while (alphaEntry > 0) {
					alphaEntry--;
					NowPlaying.this.onChange();
					NowPlaying.sleep(fadeInMs / 0xff);
				}
				synchronized(NowPlaying.this.currentSong) {
					NowPlaying.this.currentSong = song;
				}
				while (alphaEntry < 0xff) {
					alphaEntry++;
					NowPlaying.this.onChange();
					NowPlaying.sleep(fadeOutMs / 0xff);
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
	public NowPlaying setOnChange(Runnable r) {
		this.onChange = r;
		return this;
	}
	
	private void onChange() {
		if (onChange != null) {
			onChange.run();
		}
	}

}
