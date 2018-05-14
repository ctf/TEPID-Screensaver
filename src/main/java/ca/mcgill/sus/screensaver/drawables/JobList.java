package ca.mcgill.sus.screensaver.drawables;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.jackson.JacksonFeature;

import ca.mcgill.sus.screensaver.AnimatedSprite;
import ca.mcgill.sus.screensaver.DataFetch;
import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;
import ca.mcgill.sus.screensaver.Main;
import ca.mcgill.sus.screensaver.SpriteManager;
import ca.mcgill.science.tepid.models.data.PrintJob;

public class JobList implements Drawable {

	final static WebTarget tepidServer = ClientBuilder.newBuilder().register(JacksonFeature.class).build().target(Main.serverUrl); //initialises the server as a targetable thing

	private final Map<String, List<PrintJob>> jobData = DataFetch.getInstance().jobData;
	private final Map<String, Boolean> statuses = DataFetch.getInstance().printerStatus;

	public final int y;
	private final AtomicBoolean dirty = new AtomicBoolean();
	private final AnimatedSprite pusheenSad = SpriteManager.getInstance().getAnimatedSprite("pusheen_sad.png", 2, 2).setSpeedMs(200), 
			pusheenPopcorn = SpriteManager.getInstance().getAnimatedSprite("pusheen_popcorn.png", 2, 2).setSpeedMs(200) ;
	private static final Color clrDown = new Color(Main.COLOR_DOWN, true), clrEmpty = new Color (Main.COLOR_UP, true);

	/**Constructor
	 * @param y	The Y position
	 */
	public JobList(int y) {
		this.y = y;
		DataFetch.getInstance().addChangeListener(new Runnable() {
			public void run() {
				dirty.set(true);
			}
		});
	}

	@Override
	public void draw(Graphics2D g, BufferedImage canvas, int canvasWidth, int canvasHeight) {
		if (!jobData.isEmpty()) {
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int x = 0, tableWidth = canvasWidth / jobData.size();
			if (tableWidth <= 0) return;
			List<Entry<String, List<PrintJob>>> queues = new ArrayList<>(jobData.entrySet());
			Collections.sort(queues, new Comparator<Entry<String, List<PrintJob>>>() {
				@Override
				public int compare(Entry<String, List<PrintJob>> e1, Entry<String, List<PrintJob>> e2) {
					return e1.getKey().compareTo(e2.getKey());
				}
			});
			for (Entry<String, List<PrintJob>> jobs : queues) {
				boolean up = statuses.get(jobs.getKey());
				BufferedImage table = renderTable(jobs.getValue(), tableWidth - 16, up),
						icon = SpriteManager.getInstance().getColoredSprite("printer.png", up ? Main.COLOR_UP : Main.COLOR_DOWN);
				g.setFont(FontManager.getInstance().getFont("constanb.ttf").deriveFont(28f));
				g.setColor(new Color(up ? Main.COLOR_UP : Main.COLOR_DOWN, true));
				int tableX = 8 + x * tableWidth,
						iconWidth = icon.getWidth();
				g.drawString(jobs.getKey(), tableX + tableWidth / 2 - g.getFontMetrics().stringWidth(jobs.getKey()) / 2, y + 155);
				g.drawImage(icon, tableX + tableWidth / 2 - iconWidth / 2, y + 10, iconWidth, iconWidth, null);
				g.drawImage(table, tableX, y + 170, null);
				x++;
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
		out = new BufferedImage(width, 400, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		if (list.isEmpty()) {
			if (!status) g.setColor(clrDown);
			else g.setColor(clrEmpty);
			g.fill(new RoundRectangle2D.Float(0, padding, out.getWidth(), out.getHeight() - padding, 5, 5));
			AnimatedSprite pusheen = status ? pusheenPopcorn : pusheenSad;
			pusheen.draw(g, width / 2 - pusheen.getWidth() / 2, out.getHeight() / 2 - pusheen.getHeight() / 2); 
		} else {
			Color oddRows = new Color(0x1A000000 | (0xffffff & Main.TEXT_COLOR), true), lines = new Color(0x4D000000 | (0xffffff & Main.TEXT_COLOR), true);
			//writes the table headers
			g.setFont(FontManager.getInstance().getFont("nhg.ttf").deriveFont((float) fontPx));
			g.setColor(new Color(Main.TEXT_COLOR, true));
			g.setFont(FontManager.getInstance().getFont("nhg-bold.ttf").deriveFont((float) fontPx));
			g.drawString("User", 5, 1 * (fontPx + padding * 2) - padding - 2);
			g.drawString("Printer", width / 4, 1 * (fontPx + padding * 2) - padding - 2);
			g.drawString("Status", width / 2, 1 * (fontPx + padding * 2) - padding - 2);
			//draws the divider
			g.setColor(lines);
			g.setStroke(new BasicStroke(2));
			g.drawLine(0, 1 * (fontPx + padding * 2) - 1, width, 1 * (fontPx + padding * 2) - 1);
			g.setStroke(new BasicStroke(1));
			//draws list of printed jobs
			SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss a");
			int i = 2;
			for (PrintJob job : list) {
				if (job.getFailed() != null) {
					g.setColor(clrDown);
					g.fillRect(0, (i - 1) * (fontPx + padding * 2), width, fontPx + padding * 2);
				} else if (i % 2 == 0) {
					g.setColor(oddRows);
					g.fillRect(0, (i - 1) * (fontPx + padding * 2), width, fontPx + padding * 2);
				}
				g.setColor(new Color(Main.TEXT_COLOR, false));
				g.setFont(FontManager.getInstance().getFont("nhg.ttf").deriveFont((float) fontPx + 4));
				g.drawString(job.getUserIdentification(), 5, i * (fontPx + padding * 2) - padding - 2);
				g.setFont(FontManager.getInstance().getFont("nhg-thin.ttf").deriveFont((float) fontPx + 4));
				if(job.getDestination() != null) g.drawString(DataFetch.getInstance().destinations.get(job.getDestination()).name, width / 4, i * (fontPx + padding * 2) - padding - 2);
				String jobStatus = "Uploading...";
				if(job.getPrinted() != null) jobStatus = ("Printed  " + dateFormat.format(job.getPrinted()));
				else if(job.getError() != null) jobStatus = (job.getError());
				else if(job.getReceived() != null) jobStatus = ("Received  " + dateFormat.format(job.getReceived()));
				g.drawString(jobStatus, width / 2, i * (fontPx + padding * 2) - padding - 2);
				g.setColor(lines);
				g.drawLine(0, i * (fontPx + padding * 2), width, i * (fontPx + padding * 2));
				i++;
			}
		}
		g.dispose();
		return out;
	}

	@Override
	public void step(long timestamp) {
		pusheenPopcorn.step(timestamp);
		pusheenSad.step(timestamp);
	}

	@Override
	public boolean isDirty() {
		if (pusheenPopcorn.isDirty()) return true;
		if (pusheenSad.isDirty()) return true;
		return this.dirty.get();
	}

	@Override
	public void setDirty(boolean dirty) {
		if (!dirty) pusheenPopcorn.setDirty(false);
		if (!dirty) pusheenSad.setDirty(false);
		this.dirty.set(dirty);
	}

}
