package ca.mcgill.sus.screensaver.drawables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;

public class NowPlaying implements Drawable {
	
	private ScheduledFuture<?> dataFetchHandle;
	public final int y;
	private int alphaEntry = 0;
	private String currentSong = "";
	private Runnable onChange;
	private final int color, maxAlpha;
	
	private final String host = "grunt.sus.mcgill.ca", password = "taskforce";
	private final int port = 6600;
	private Socket socket;
	private InputStream socketIn;
	private PrintStream socketOut;
	
	public NowPlaying(int y, int color) {
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
		final String title = "NOW PLAYING";
		int titleWidth = g.getFontMetrics().stringWidth(title);
		g.drawString(title, canvasWidth / 2 - titleWidth / 2, y);
		g.setFont(FontManager.getInstance().getFont("nhg-thin.ttf").deriveFont(24f));
		g.setColor(new Color((alphaEntry << 24) | color, true));
		synchronized(currentSong) {
			g.drawString(currentSong, canvasWidth / 2 - g.getFontMetrics().stringWidth(currentSong) / 2, y + 40);
		}
	}
	
	public void startDataFetch() {
		final Runnable dataFetch = new Runnable() {
			public void run() {
				System.out.println("Fetching now playing");
				if (socket == null || !socket.isConnected()) {
					try {
			        socket = new Socket(host, port);
			        socketIn = socket.getInputStream();
			        socketOut = new PrintStream(socket.getOutputStream());
			        if (!readSocket().startsWith("OK MPD")) {
			        	throw new RuntimeException("Invalid connection response from MPD");
			        }
			        if (password != null) {
			        	socketOut.println("password " + password);
				        if (!readSocket().startsWith("OK")) {
				        	throw new RuntimeException("Incorrect MPD password");
				        }
			        }
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
				socketOut.println("currentsong");
				String title = "", artist = "", file = "";
				try {
					String ack = readSocket();
					for (String l : ack.split("\n")) {
						if (l.startsWith("Title: ")) {
							title = l.substring(l.indexOf(' ')).trim();
						} else if (l.startsWith("Artist: ")) {
							artist = l.substring(l.indexOf(' ')).trim();
						} else if (l.startsWith("file: ")) {
							file = l.substring(l.indexOf(' ')).trim();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				String display;
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
				System.out.println("Done");
			}
		};
		if (dataFetchHandle != null) dataFetchHandle.cancel(false);
		dataFetchHandle = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(dataFetch, 0, 10, TimeUnit.SECONDS);
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
					NowPlaying.sleep(fadeInMs / maxAlpha);
				}
				synchronized(NowPlaying.this.currentSong) {
					NowPlaying.this.currentSong = song;
				}
				while (alphaEntry < maxAlpha) {
					alphaEntry++;
					NowPlaying.this.onChange();
					NowPlaying.sleep(fadeOutMs / maxAlpha);
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
	
	public String readSocket() throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[16384];
		while ((nRead = socketIn.read(data, 0, data.length)) != -1) {
		  buffer.write(data, 0, nRead);
		  int lastEndL;
		  for (lastEndL = nRead - 1; lastEndL > 0 && (data[lastEndL] != '\n' || lastEndL == nRead - 1); lastEndL--) {
		  }
		  String lastLine = new String(data, lastEndL, nRead - lastEndL, "UTF-8").trim();	  
		  if (lastLine.startsWith("OK") || lastLine.startsWith("ACK")) {
			  break;
		  }
		}
		buffer.flush();
		
		return new String(buffer.toByteArray(), "UTF-8");
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
