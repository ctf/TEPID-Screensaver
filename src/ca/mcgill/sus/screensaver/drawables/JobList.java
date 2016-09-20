package ca.mcgill.sus.screensaver.drawables;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.jackson.JacksonFeature;

import com.google.gson.Gson;				//TODO: delete once transitioned
import com.google.gson.reflect.TypeToken;

import ca.mcgill.sus.screensaver.AnimatedSprite;
import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;
import ca.mcgill.sus.screensaver.Main;
import ca.mcgill.sus.screensaver.SpriteManager;
import ca.mcgill.sus.screensaver.io.JobData;

public class JobList implements Drawable {
	
	final static WebTarget tepidServer = ClientBuilder.newBuilder().register(JacksonFeature.class).build().target(Main.serverUrl);
	
	private final Map<String, JobData[]> jobData = new ConcurrentSkipListMap<>();
	private ScheduledFuture<?> dataFetchHandle;
	public final int y;
	private Runnable onChange;
	private final AnimatedSprite pusheen = SpriteManager.getInstance().getAnimatedSprite("pusheen_sad.png", 2, 2).setSpeedMs(200);
	private static final Color clrDown = new Color(0xccdc241f, true);
	
	public JobList(int y) {
		startDataFetch();
		this.y = y;
	}

	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		if (!jobData.isEmpty()) {
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int x = 0, tableWidth = canvasWidth / jobData.size();
			for (Entry<String, JobData[]> jobs : jobData.entrySet()) {
				BufferedImage table = renderTable(jobs.getValue().length > 13 ? Arrays.asList(jobs.getValue()).subList(0, 13) : Arrays.asList(jobs.getValue()), tableWidth - 16);
				int space = canvasHeight - y - 10, tableY = y + 10 + space / 2 - table.getHeight() / 2;
				g.setFont(FontManager.getInstance().getFont("nhg-bold.ttf").deriveFont(24f));
				g.setColor(new Color(Main.TEXT_COLOR, true));
				g.drawString(jobs.getKey(), x * tableWidth + tableWidth / 2 - g.getFontMetrics().stringWidth(jobs.getKey()) / 2, tableY - 20);
				g.drawImage(table, 8 + x++ * tableWidth, tableY, null);
			}
		}
	}
	
	public void startDataFetch() {
		//TODO figure out why cert isn't validating
		trustAllCerts();
		
		//UNDER CONSTRUCTION
		// ~dgoldm3
		
		
		
		//END CONSTUCTION
		// ~dgoldm3
		
		final Runnable dataFetch = new Runnable() {
			public void run() {
				System.out.println("Fetching job data");
				Map<String, Boolean> printers;
				try (Reader r = new InputStreamReader(new URL("https://cups.sus.mcgill.ca/bob/status.php").openStream(), "UTF-8")) {
					printers = new Gson().fromJson(r, new TypeToken<Map<String, Boolean>>(){}.getType());
//					printers = new Gson().fromJson("{\"1B16\":true,\"1B17\":false,\"1B18\":true} ", new TypeToken<Map<String, Boolean>>(){}.getType());
				} catch (Exception e) {
					RuntimeException re = new RuntimeException("Could not fetch data", e);
					re.printStackTrace();
					throw re;
				}
				jobData.clear();
				for (Entry<String, Boolean> printer : printers.entrySet()) {
					if (printer.getValue()) {
						try (Reader r = new InputStreamReader(new URL("https://cups.sus.mcgill.ca/functions/last_jobs_json.php?sUser=" + printer.getKey()).openStream(), "UTF-8")) {
							jobData.put(printer.getKey(), new Gson().fromJson(r, JobData[].class));
						} catch (Exception e) {
							new RuntimeException("Could not fetch data", e).printStackTrace();
						}
					} else {
						jobData.put(printer.getKey(), new JobData[0]);
					}
				}
				onChange();
				System.out.println("Done");
			}
		};
		if (dataFetchHandle != null) dataFetchHandle.cancel(false);
		dataFetchHandle = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(dataFetch, 0, 60, TimeUnit.SECONDS);
	}
	
	public BufferedImage renderTable(List<JobData> jobs, int width) {
		int fontPx = 16, padding = 10;
		BufferedImage out;
		if (jobs.isEmpty()) {
			out = new BufferedImage(width, 350, BufferedImage.TYPE_INT_ARGB);
		} else {
			out = new BufferedImage(width, (fontPx + padding * 2) * (jobs.size() + 1) + 1, BufferedImage.TYPE_INT_ARGB);
		}
		Graphics2D g = out.createGraphics();
		if (jobs.isEmpty()) {
			g.setColor(clrDown);
			g.fillRect(0, 0, out.getWidth(), out.getHeight());
		}
		Color oddRows = new Color(0x1A000000 | (0xffffff & Main.TEXT_COLOR), true), lines = new Color(0x4D000000 | (0xffffff & Main.TEXT_COLOR), true);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(FontManager.getInstance().getFont("nhg.ttf").deriveFont((float) fontPx));
		g.setColor(new Color(Main.TEXT_COLOR, true));
		g.setFont(FontManager.getInstance().getFont("nhg-bold.ttf").deriveFont((float) fontPx));
		g.drawString("User", 5, 1 * (fontPx + padding * 2) - padding - 2);
		g.drawString("Date", width / 2, 1 * (fontPx + padding * 2) - padding - 2);
		g.setColor(lines);
		g.setStroke(new BasicStroke(2));
		g.drawLine(0, 1 * (fontPx + padding * 2) - 1, width, 1 * (fontPx + padding * 2) - 1);
		g.setStroke(new BasicStroke(1));
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, h:mm:ss a");
		int i = 2;
		if (!jobs.isEmpty()) {
			for (JobData job : jobs) {
				if (i % 2 == 0) {
					g.setColor(oddRows);
					g.fillRect(0, (i - 1) * (fontPx + padding * 2), width, fontPx + padding * 2);
				}
				g.setColor(new Color(Main.TEXT_COLOR, false));
				g.setFont(FontManager.getInstance().getFont("nhg.ttf").deriveFont((float) fontPx + 4));
				g.drawString(job.getUser(), 5, i * (fontPx + padding * 2) - padding - 2);
				g.setFont(FontManager.getInstance().getFont("nhg-thin.ttf").deriveFont((float) fontPx + 4));
				g.drawString(dateFormat.format(job.getDate()), width / 2, i * (fontPx + padding * 2) - padding - 2);
				g.setColor(lines);
				g.drawLine(0, i * (fontPx + padding * 2), width, i * (fontPx + padding * 2));
				i++;
			}
		} else {
			pusheen.draw(g, width / 2 - pusheen.getWidth() / 2, 100);
		}
		g.dispose();
		return out;
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
	public JobList setOnChange(Runnable r) {
		this.onChange = r;
		this.pusheen.setOnChange(r);
		return this;
	}
	
	private void onChange() {
		if (onChange != null) {
			onChange.run();
		}
	}

}
