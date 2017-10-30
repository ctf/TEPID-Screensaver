package ca.mcgill.sus.screensaver.drawables;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.jackson.JacksonFeature;

import ca.mcgill.sus.screensaver.AnimatedSprite;
import ca.mcgill.sus.screensaver.DataFetch;
import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;
import ca.mcgill.sus.screensaver.Main;
import ca.mcgill.sus.screensaver.SpriteManager;
import ca.mcgill.sus.screensaver.Stage;
import ca.mcgill.sus.screensaver.io.PrintJob;

public class JobList implements Drawable {
	
	final static WebTarget tepidServer = ClientBuilder.newBuilder().register(JacksonFeature.class).build().target(Main.serverUrl); //initialises the server as a targetable thing

	private final Map<String, List<PrintJob>> jobData = DataFetch.getInstance().jobData;
	
	public final int y;
	private Stage parent;
	private final AnimatedSprite pusheenSad = SpriteManager.getInstance().getAnimatedSprite("pusheen_sad.png", 2, 2).setSpeedMs(200), 
								 pusheenPopcorn = SpriteManager.getInstance().getAnimatedSprite("pusheen_popcorn.png", 2, 2).setSpeedMs(200) ;
	private static final Color clrDown = new Color(0xccdc241f, true);
	private static final Color clrEmpty = new Color (0xcc50c954, true);
	
	/**map of the printer statuses
	 */
	private static Map<String, Boolean> statuses =(tepidServer
			.path("/queues/status")
			.request(MediaType.APPLICATION_JSON)
			.get(new GenericType <Map<String, Boolean>>(){}));	//a map of statuses 
	
	/**Constructor
	 * @param y	The Y position
	 */
	public JobList(int y) {
		this.y = y;
		DataFetch.getInstance().addChangeListener(new Runnable() {
			public void run() {
				if (parent != null) parent.safeRepaint();
			}
		});
	}

	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		if (!jobData.isEmpty()) {
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int x = 0, tableWidth = canvasWidth / jobData.size();
			if (tableWidth <= 0) return;
			for (Entry<String, List<PrintJob>> jobs : jobData.entrySet()) {
				BufferedImage table = renderTable(jobs.getValue(), tableWidth - 16, statuses.get(jobs.getKey()));
				int space = canvasHeight - y - 10, tableY = y + 10 + space / 2 - table.getHeight() / 2;
				g.setFont(FontManager.getInstance().getFont("nhg-bold.ttf").deriveFont(24f));
				g.setColor(new Color(Main.TEXT_COLOR, true));
				g.drawString(jobs.getKey(), x * tableWidth + tableWidth / 2 - g.getFontMetrics().stringWidth(jobs.getKey()) / 2, tableY - 20);
				g.drawImage(table, 8 + x++ * tableWidth, tableY, null);
			}
		}
	}
	
	/**
	 * @param list 		the list of print jobs to display
	 * @param width		the width of the table
	 * @param status	the status of the print queue. will determine whether an empty queue gets the popcorn pusheen or the sad pusheen
	 * @return
	 */
	public BufferedImage renderTable(List<PrintJob> list, int width, boolean status) {
		int fontPx = 16, padding = 10;
		BufferedImage out;
		out = new BufferedImage(width, 425, BufferedImage.TYPE_INT_ARGB);
		
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
		//writes the table headers
		g.setFont(FontManager.getInstance().getFont("nhg.ttf").deriveFont((float) fontPx));
		g.setColor(new Color(Main.TEXT_COLOR, true));
		g.setFont(FontManager.getInstance().getFont("nhg-bold.ttf").deriveFont((float) fontPx));
		g.drawString("User", 5, 1 * (fontPx + padding * 2) - padding - 2);
		g.drawString("Date", width / 2, 1 * (fontPx + padding * 2) - padding - 2);
		//draws the divider
		g.setColor(lines);
		g.setStroke(new BasicStroke(2));
		g.drawLine(0, 1 * (fontPx + padding * 2) - 1, width, 1 * (fontPx + padding * 2) - 1);
		g.setStroke(new BasicStroke(1));
		//draws list of printed jobs
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
				if(job.getPrinted() != null) 
					{g.drawString(dateFormat.format(job.getPrinted()), width / 2, i * (fontPx + padding * 2) - padding - 2);}
				g.setColor(lines);
				g.drawLine(0, i * (fontPx + padding * 2), width, i * (fontPx + padding * 2));
				i++;
			}
		} 
		else //if there are no jobs to print
		{
			if (status == false)	//if the printer is down
			{
				pusheenSad.draw(g, width / 2 - pusheenSad.getWidth() / 2, 100); //draws the sad pusheen
			}
			else	//if nothing has been printed today
			{
				pusheenPopcorn.draw(g, width / 2 - pusheenPopcorn.getWidth() / 2, 100); //draws the popcorn pusheen
			}
			
		}
		g.dispose();
		return out;
	}
	@Override
	public void setParent(Stage r) {
		this.parent = r;
		this.pusheenSad.setParent(r);
		this.pusheenPopcorn.setParent(r);
	}

}
