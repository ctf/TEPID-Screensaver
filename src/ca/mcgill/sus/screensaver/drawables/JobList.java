package ca.mcgill.sus.screensaver.drawables;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.jackson.JacksonFeature;

import ca.mcgill.sus.screensaver.AnimatedSprite;
import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;
import ca.mcgill.sus.screensaver.Main;
import ca.mcgill.sus.screensaver.SpriteManager;

public class JobList implements Drawable {
	
	final static WebTarget tepidServer = ClientBuilder.newBuilder().register(JacksonFeature.class).build().target(Main.serverUrl); //initialises the server as a targetable thing

	private final Map<PrintQueue, List<PrintJob>> jobData = new TreeMap<PrintQueue, List<PrintJob>>(new Comparator<PrintQueue>() //TODO: rewrite this as Map<String, List<PrintJob>>
	{
		@Override
		public int compare(PrintQueue arg0, PrintQueue arg1) 
		{
			return arg0.name.compareTo(arg1.name);
		}
		
	});		//creates a list of jobs, sorted by queues 
	
	private ScheduledFuture<?> dataFetchHandle;
	public final int y;
	private Runnable onChange;
	private final AnimatedSprite pusheenSad = SpriteManager.getInstance().getAnimatedSprite("pusheen_sad.png", 2, 2).setSpeedMs(200), 
								 pusheenPopcorn = SpriteManager.getInstance().getAnimatedSprite("pusheen_popcorn.png", 2, 2).setSpeedMs(200) ;
	private static final Color clrDown = new Color(0xccdc241f, true);
	private static final Color clrEmpty = new Color (0xcc50c954, true);
	
	private static final Map<String, Boolean> statuses =(tepidServer
			.path("/queues/status")
			.request(MediaType.APPLICATION_JSON)
			.get(new GenericType <Map<String, Boolean>>(){}));	//a map of statuses 
	
	public JobList(int y) {
		startDataFetch();
		this.y = y;
	}

	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		if (!jobData.isEmpty()) {
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int x = 0, tableWidth = canvasWidth / jobData.size();
			for (Entry<PrintQueue, List<PrintJob>> jobs : jobData.entrySet()) {
				BufferedImage table = renderTable(jobs.getValue(), tableWidth - 16, statuses.get(jobs.getKey().name));
				int space = canvasHeight - y - 10, tableY = y + 10 + space / 2 - table.getHeight() / 2;
				g.setFont(FontManager.getInstance().getFont("nhg-bold.ttf").deriveFont(24f));
				g.setColor(new Color(Main.TEXT_COLOR, true));
				g.drawString(jobs.getKey().name, x * tableWidth + tableWidth / 2 - g.getFontMetrics().stringWidth(jobs.getKey().name) / 2, tableY - 20);
				g.drawImage(table, 8 + x++ * tableWidth, tableY, null);
			}
		}
	}
	
	public void startDataFetch() {
		//TODO figure out why cert isn't validating
		trustAllCerts();

		final Runnable dataFetch = new Runnable() 
		{
			public void run()
			{
				System.out.println("Fetching job data");
				PrintQueue[] printers =tepidServer.path("queues").request(MediaType.APPLICATION_JSON).get(PrintQueue[].class);	//gets a list of queues
				jobData.clear();
				for (PrintQueue q : printers)
				{
					System.out.println(q.name);
					jobData.put(q, tepidServer
									.path("queues").path(q.name)  	//path to specific queue
									.queryParam("limit", 13)
									.request(MediaType.APPLICATION_JSON)
									.get(new GenericType <List<PrintJob>>(){}));
				}
				onChange();
				System.out.println("Done");
			}
			
		};
		if (dataFetchHandle != null) dataFetchHandle.cancel(false);
		dataFetchHandle = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(dataFetch, 0, 60, TimeUnit.SECONDS);
	}
	
	public BufferedImage renderTable(List<PrintJob> list, int width, boolean status) {
		int fontPx = 16, padding = 10;
		BufferedImage out;
		if (list.isEmpty()) {
			out = new BufferedImage(width, 350, BufferedImage.TYPE_INT_ARGB);
		} else {
			out = new BufferedImage(width, (fontPx + padding * 2) * (list.size() + 1) + 1, BufferedImage.TYPE_INT_ARGB);
		}
		Graphics2D g = out.createGraphics();
		
		if (list.isEmpty()) {
			if (status == false)
			{
				g.setColor(clrDown);
				g.fillRect(0, 0, out.getWidth(), out.getHeight());
			}
			else
			{
				g.setColor(clrEmpty);
				g.fillRect(0, 0, out.getWidth(), out.getHeight());
			}
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
		if (!list.isEmpty()) {
			for (PrintJob job : list) {
				if (i % 2 == 0) {
					g.setColor(oddRows);
					g.fillRect(0, (i - 1) * (fontPx + padding * 2), width, fontPx + padding * 2);
				}
				g.setColor(new Color(Main.TEXT_COLOR, false));
				g.setFont(FontManager.getInstance().getFont("nhg.ttf").deriveFont((float) fontPx + 4));
				g.drawString(job.getUserIdentification(), 5, i * (fontPx + padding * 2) - padding - 2);
				g.setFont(FontManager.getInstance().getFont("nhg-thin.ttf").deriveFont((float) fontPx + 4));
				if(job.getPrinted() != null) {g.drawString(dateFormat.format(job.getPrinted()), width / 2, i * (fontPx + padding * 2) - padding - 2);}
				g.setColor(lines);
				g.drawLine(0, i * (fontPx + padding * 2), width, i * (fontPx + padding * 2));
				i++;
			}
		} 
		else 
		{
			if (status == false)
			{
				pusheenSad.draw(g, width / 2 - pusheenSad.getWidth() / 2, 100);
			}
			else
			{
				pusheenPopcorn.draw(g, width / 2 - pusheenPopcorn.getWidth() / 2, 100);
			}
			
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
		this.pusheenSad.setOnChange(r);
		this.pusheenPopcorn.setOnChange(r);
		return this;
	}
	
	private void onChange() {
		if (onChange != null) {
			onChange.run();
		}
	}

}
