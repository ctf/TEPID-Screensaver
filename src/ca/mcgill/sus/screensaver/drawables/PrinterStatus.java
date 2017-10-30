package ca.mcgill.sus.screensaver.drawables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
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

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.jackson.JacksonFeature;

import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;
import ca.mcgill.sus.screensaver.Main;
import ca.mcgill.sus.screensaver.SpriteManager;
import ca.mcgill.sus.screensaver.Stage;

public class PrinterStatus implements Drawable 
{
	final static WebTarget tepidServer = ClientBuilder
												.newBuilder()
												.register(JacksonFeature.class)
												.build()
												.target(Main.serverUrl); //initialises the server as a targetable thing
	private Map<String, Boolean> status = new ConcurrentHashMap<>();
	public final int y, padding;
	private ScheduledFuture<?> dataFetchHandle;
	private Stage parent;
	
	/**Constructor
	 * @param y			the y position
	 * @param padding	the amount of padding
	 */
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
	
	/**
	 * @return
	 */
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
	
	/** Draws an individual printer
	 * @param name	the name of the printer
	 * @param up	its up status
	 * @return		a BufferedImage containing the printer, name, 
	 */
	private static BufferedImage generatePrinter(String name, boolean up) {
		int vpad = 16, fontSize = 32;
		BufferedImage 	printer = SpriteManager.getInstance().getColoredSprite("printer.png", up ? Main.COLOR_UP : Main.COLOR_DOWN), 	//Creates the image of the printer (coloured depending on its status
						emoji = SpriteManager.getInstance().getSprite(up ? "smile.png" : "frown.png"),									//The emoji to go with it
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
	
	/**Get the up status of the printers, and puts it in the map
	 * 
	 */
	public void startDataFetch() {
		final Runnable dataFetch = new Runnable() 
		{
			public void run() 
			{
				System.out.println("Fetching printer status");
				Map<String, Boolean> newStatus =(tepidServer
					.path("/queues/status")
					.request(MediaType.APPLICATION_JSON)
					.get(new GenericType <Map<String, Boolean>>(){}));	//a newStatus is created so that the status map is never empty
				status.clear();				//clears status map
				status.putAll(newStatus); 	//loads new statuses into the main status map
				if (parent != null) parent.safeRepaint();
			}
		};
		if (dataFetchHandle != null) dataFetchHandle.cancel(false);
		dataFetchHandle = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(dataFetch, 0, 60, TimeUnit.SECONDS);
	}
	
	public void stopDataFetch() {
		if (dataFetchHandle != null) dataFetchHandle.cancel(false);
	}

	@Override
	public void setParent(Stage parent) {
		this.parent = parent;
	}

}
