package ca.mcgill.sus.screensaver.datafetch;

import ca.mcgill.science.tepid.api.ITepidScreensaver;
import ca.mcgill.science.tepid.models.data.Destination;
import ca.mcgill.science.tepid.models.data.MarqueeData;
import ca.mcgill.science.tepid.models.data.NameUser;
import ca.mcgill.science.tepid.models.data.PrintJob;
import ca.mcgill.sus.screensaver.Config;
import ca.mcgill.sus.screensaver.ConfigKt;
import ca.mcgill.sus.screensaver.Main;
import ca.mcgill.sus.screensaver.io.Slide;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.ClientBuilder;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataFetch extends Thread {

	private static final int interval = 15, longFetch = 5 * 60, longFetchEvery = longFetch/interval;

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

	private final Queue<Runnable> listeners = new ConcurrentLinkedQueue<>();

	private final AtomicBoolean success = new AtomicBoolean(true);
	private final AtomicBoolean networkUp = new AtomicBoolean(true);
	private final AtomicBoolean loaded = new AtomicBoolean();

	// DataFetchables
	private UserFetch userFetch = new WindowsUserFetch(interval, api);
	private ProfilePictureFetch profilePictureFetch = new ProfilePictureFetch(
			interval,
			ClientBuilder.newClient().target("https://www.gravatar.com/avatar/"),
			ClientBuilder.newBuilder().register(JacksonFeature.class).build().target("https://www.googleapis.com/customsearch/v1?" + Config.INSTANCE.getGOOGLE_CUSTOM_SEARCH_KEY() + "&searchType=image"),
			null
	);
	private SlideFetch slideFetch = new SlideFetch();
	private EventsFetch eventsFetch = new EventsFetch(
			interval,
			ClientBuilder.newBuilder().register(JacksonFeature.class).build().target("https://calendar.google.com/calendar/ical"),
			Config.INSTANCE.getICS_CALENDAR_ADDRESS()
	);
	private ITepidFetch<Map<String, Destination>> fetchDestinations = new ITepidFetch<>(interval, api::getDestinations);
	private ITepidFetch<Map<String, Boolean>> fetchQueueStatus = new ITepidFetch<>(interval, api::getQueueStatus);
	private ITepidFetch<List<MarqueeData>> fetchMarquee = new ITepidFetch<>(interval, api::getMarquee);
	private JobsFetch jobsFetch = new JobsFetch(interval, api);

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
			success.set(true);
			long startTime = System.currentTimeMillis();

			boolean refetchLongFetch = iterations++ % longFetchEvery == 0 || !success.get(),
					isOfficeComputer = Main.OFFICE_COMPUTER;

			// TEPID
			fetchMandatorily(fetchAndPutAll(fetchDestinations, destinations));
			fetchMandatorily(fetchAndPutAll(fetchQueueStatus, printerStatus));
			jobsFetch.setQueueStatuses(printerStatus);
			fetchMandatorily(fetchAndPutAll(jobsFetch, jobData));

			// User
			fetchMandatorily(fetchAndAddSingle(userFetch, nameUser));
			profilePictureFetch.setUser(nameUser.peek());

			// Optional
			if (refetchLongFetch) {
				fetchOptionally(fetchAndAddAll(slideFetch, slides));
				fetchOptionally(fetchAndAddAll(fetchMarquee, marqueeData));
				if (isOfficeComputer) fetchOptionally(fetchAndAddAll(eventsFetch, upcomingEvents));
			}
			if (isOfficeComputer) fetchOptionally(fetchAndAddSingle(profilePictureFetch, profilePic));

			this.loaded.set(true);
			networkUp.set(success.get());
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

	private interface ReplacerFunc<V> {
		void replace(V val);
	}

	private <T> boolean fetchAndReplace(DataFetchable<T> fetcher, ReplacerFunc<T> replacer) {
		FetchResult<T> result = fetcher.fetchUnexceptionally();
		if (!result.success) {
			return false;
		}
		replacer.replace(result.value);
		return true;
	}

	private <K, V, T extends Map<K, V>> boolean fetchAndPutAll(DataFetchable<T> fetcher, T destination) {
		return fetchAndReplace(fetcher, v -> {
			destination.clear();
			destination.putAll(v);
		});
	}

	private <E, T1 extends Collection<E>, T2 extends Collection<E>> boolean fetchAndAddAll(DataFetchable<T1> fetcher, T2 destination) {
		return fetchAndReplace(fetcher, v -> {
			destination.clear();
			destination.addAll(v);
		});
	}

	private <E, T extends Collection<E>> boolean fetchAndAddSingle(DataFetchable<E> fetcher, Collection<E> destination) {
		return fetchAndReplace(fetcher, v -> {
			destination.clear();
			destination.add(v);
		});
	}

	private void fetchOptionally(boolean result) {
	}

	private void fetchMandatorily(boolean result) {
		success.set(success.get() || result);
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
