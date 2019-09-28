package ca.mcgill.sus.screensaver.datafetch;

import ca.mcgill.science.tepid.api.ITepidScreensaver;
import ca.mcgill.science.tepid.models.data.Destination;
import ca.mcgill.science.tepid.models.data.MarqueeData;
import ca.mcgill.science.tepid.models.data.NameUser;
import ca.mcgill.science.tepid.models.data.PrintJob;
import ca.mcgill.sus.screensaver.Config;
import ca.mcgill.sus.screensaver.ConfigKt;
import ca.mcgill.sus.screensaver.Main;
import ca.mcgill.sus.screensaver.Util;
import ca.mcgill.sus.screensaver.io.Slide;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataFetch extends Thread {

	private static final int interval = 15, icalInterval = 5 * 60;

	private DataFetch() {
		this.setDaemon(true);
		this.start();
	}

	private static DataFetch INSTANCE;

	public static synchronized DataFetch getInstance() {
		if (INSTANCE == null) INSTANCE = new DataFetch();
		return INSTANCE;
	}

	private final ITepidScreensaver api = ConfigKt.getApi();
	private final WebTarget icalServer = ClientBuilder.newBuilder().register(JacksonFeature.class).build().target("https://calendar.google.com/calendar/ical");
	private final WebTarget gravatarApi = ClientBuilder.newClient().target("https://www.gravatar.com/avatar/");
	private final WebTarget gImageApi = ClientBuilder.newBuilder().register(JacksonFeature.class).build().target("https://www.googleapis.com/customsearch/v1?" + Config.INSTANCE.getGOOGLE_CUSTOM_SEARCH_KEY() + "&searchType=image");

	private final String icsPath = Config.INSTANCE.getICS_CALENDAR_ADDRESS();
	private final Queue<Runnable> listeners = new ConcurrentLinkedQueue<>();

	private final AtomicBoolean networkUp = new AtomicBoolean(true);
	private final AtomicBoolean loaded = new AtomicBoolean();

	// DataFetchables
	private UserFetch userFetch = new WindowsUserFetch(
			interval,
			api
	);
	private ProfilePictureFetch profilePictureFetch = new ProfilePictureFetch(
			interval,
			gravatarApi,
			gImageApi,
			null
	);
	private SlideFetch slideFetch = new SlideFetch(icalInterval/interval);
	private EventsFetch eventsFetch = new EventsFetch(
			icalInterval,
			icalServer,
			icsPath
	);
	private ITepidFetch<Map<String, Destination>> fetchDestinations = new ITepidFetch<>(
			interval,
			api::getDestinations
	);
	private ITepidFetch<Map<String, Boolean>> fetchQueueStatus = new ITepidFetch<>(
			interval,
			api::getQueueStatus
	);
	private ITepidFetch<List<MarqueeData>> fetchMarquee = new ITepidFetch<>(
			interval,
			api::getMarquee
	);
	private JobsFetch jobsFetch = new JobsFetch(
			interval,
			api
	);

	// models
	public final Map<String, Boolean> printerStatus = new ConcurrentHashMap<>();
	public final Map<String, Destination> destinations = new ConcurrentHashMap<>();
	public final Map<String, List<PrintJob>> jobData = new ConcurrentHashMap<>();
	public final Queue<MarqueeData> marqueeData = new ConcurrentLinkedQueue<>();
	public final Queue<String> upcomingEvents = new ConcurrentLinkedQueue<>();
	public final Queue<NameUser> nameUser = new ConcurrentLinkedQueue<>();
	public final List<Slide> slides = Collections.synchronizedList(new ArrayList<>());
	public final Queue<BufferedImage> profilePic = new ConcurrentLinkedQueue<>();

	@Override
	public void run() {
		int iterations = 0;
		while (!Thread.interrupted()) {
			boolean fail = false;
			long startTime = System.currentTimeMillis();

			boolean pullSlides = iterations++ * interval % icalInterval == 0,
			pullEvents = (Main.OFFICE_COMPUTER && pullSlides) || !networkUp.get(),
			pullPropic = Main.OFFICE_COMPUTER && profilePic.isEmpty();

			FetchResult<List<MarqueeData>> marqueeResult = fetchMarquee.fetchUnexceptionally();
			if (marqueeResult.success){
				marqueeData.clear();
				marqueeData.addAll(marqueeResult.value);
			}

			try {
				FetchResult<Map<String, Destination>> destinationResult = fetchDestinations.fetch();
				if (destinationResult.success) {
					destinations.clear();
					destinations.putAll(destinationResult.value);
				}

				FetchResult<Map<String, Boolean>> queueStatusResult = fetchQueueStatus.fetch();
				if (queueStatusResult.success) {
					jobsFetch.setQueueStatuses(queueStatusResult.value);
					printerStatus.clear();
					printerStatus.putAll(queueStatusResult.value);
				}

				FetchResult<Map<String, List<PrintJob>>> jobsResult = jobsFetch.fetch();
				if (jobsResult.success) {
					jobData.clear();
					jobData.putAll(jobsResult.value);
				}

			}catch(Exception e){
				e.printStackTrace();
				fail=true;
			}

			try {
				FetchResult<NameUser> userResult = userFetch.fetch();
				if (userResult.success) {
					nameUser.clear();
					nameUser.add(userResult.value);
				}

				profilePictureFetch.setUser(nameUser.peek());
			}catch(Exception e){
				e.printStackTrace();
				fail=true;
			}

			try {
				FetchResult<List<Slide>> slideResult = slideFetch.fetch();
				if (slideResult.success) {
					slides.clear();
					slides.addAll(slideResult.value);
				}

				if (pullPropic) {
					FetchResult<BufferedImage> profilePicResult = profilePictureFetch.fetch();
					if (profilePicResult.success){
						profilePic.clear();
						profilePic.add(Util.circleCrop(profilePicResult.value));
					}
				}

				if (pullEvents) {
					FetchResult<Queue<String>> eventsResult = eventsFetch.fetch();
					if (eventsResult.success){
						upcomingEvents.clear();
						upcomingEvents.addAll(eventsResult.value);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			this.loaded.set(true);
			networkUp.set(!fail);
			for (Runnable listener : listeners) {
				listener.run();
			}
			long elapsed = System.currentTimeMillis() - startTime;
			System.out.println("Data fetch complete in " + elapsed + "ms");
			try {
				Thread.sleep(Math.max(0, interval * 1000 - elapsed));
			} catch (InterruptedException e) {
				break;
			}
		}
		System.out.println("Data fetch thread over and out");
	}

	public void addChangeListener(Runnable listener) {
		listeners.add(listener);
	}

	public boolean isNetworkUp() {
		return networkUp.get();
	}

	public boolean isLoaded() {
		return loaded.get();
	}
}
