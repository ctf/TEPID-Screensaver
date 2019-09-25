package ca.mcgill.sus.screensaver;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.io.TimezoneAssignment;
import biweekly.io.TimezoneInfo;
import biweekly.property.DateStart;
import biweekly.util.com.google.ical.compat.javautil.DateIterator;
import ca.mcgill.science.tepid.api.ITepidScreensaver;
import ca.mcgill.science.tepid.models.data.Destination;
import ca.mcgill.science.tepid.models.data.MarqueeData;
import ca.mcgill.science.tepid.models.data.NameUser;
import ca.mcgill.science.tepid.models.data.PrintJob;
import ca.mcgill.sus.screensaver.io.Slide;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

	private final WebTarget tepidServer = ClientBuilder.newBuilder().register(JacksonFeature.class).build().target(Main.serverUrl+"screensaver/");
	private final ITepidScreensaver api = ConfigKt.getApi();
	private final WebTarget icalServer = ClientBuilder.newBuilder().register(JacksonFeature.class).build().target("https://calendar.google.com/calendar/ical");
	private final WebTarget gravatarApi = ClientBuilder.newClient().target("https://www.gravatar.com/avatar/");
	private final WebTarget gImageApi = ClientBuilder.newBuilder().register(JacksonFeature.class).build().target("https://www.googleapis.com/customsearch/v1?" + Config.INSTANCE.getGOOGLE_CUSTOM_SEARCH_KEY() + "&searchType=image");

	private final String icsPath = Config.INSTANCE.getICS_CALENDAR_ADDRESS();
	private final Queue<Runnable> listeners = new ConcurrentLinkedQueue<>();

	private final AtomicBoolean networkUp = new AtomicBoolean(true);
	private final AtomicBoolean loaded = new AtomicBoolean();

	//models
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

			try {
				updateMarqueeData();
			}catch(Exception e){
				e.printStackTrace();
			}

			try {
				updateDestinations();
				processPrintQueues();
			}catch(Exception e){
				e.printStackTrace();
				fail=true;
			}

			try {
				updateUserInfo();
			}catch(Exception e){
				e.printStackTrace();
				fail=true;
			}

			try {
				loadSlideImages(pullSlides);
				if (pullPropic) {
					pullProfilePicture(nameUser.peek());
				}
				if (pullEvents) {
					processEvents();
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

	private void pullProfilePicture(NameUser user) {
		//pull profile picture for office
		 BufferedImage gravatar = pullGravatar(user);
		if (gravatar != null) {
			setProfilePic(gravatar);
			return;
		}
		BufferedImage googleThumbnail = pullWebImage(user);
		if (googleThumbnail != null) {
			setProfilePic(googleThumbnail);
			return;
		}
		throw new RuntimeException();
	}

	@Nullable
	private BufferedImage pullGravatar(NameUser user) {
		//look for gravatar; d=404 means don't return a default image, 404 instead; s=128 is the size
		Future<byte[]> futureGravatar = null;
		if (user != null) {
			String email;
			if (user.getEmail() != null) {
				email = user.getEmail();
			}
			else if (user.getLongUser() != null) {
				email = user.getLongUser();
			}
			else {
				return (null); // return null if there's no point searching
			}
			futureGravatar = gravatarApi.path(Util.md5Hex(email)).queryParam("d", "404").queryParam("s", "110").request(MediaType.APPLICATION_OCTET_STREAM).async().get(byte[].class);
		}

		BufferedImage gravatar = null;
		if (futureGravatar != null) try {
			gravatar = Util.readImage(futureGravatar.get(interval, TimeUnit.SECONDS));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return gravatar;
	}

	@Nullable
	private BufferedImage pullWebImage(NameUser user) {
		//search google for "full name" + mcgill
		String name = user == null ? System.getenv("username") : (user.getRealName() == null ? user.getDisplayName() : user.getRealName());
		BufferedImage googleThumbnail = null;
		Future<ObjectNode> futureImageResult = gImageApi.queryParam("q", "\"" + name + "\" " + Config.INSTANCE.getGravatar_search_terms()).request(MediaType.APPLICATION_JSON).async().get(ObjectNode.class);
		try {
			ObjectNode imageSearchResult = futureImageResult.get(interval, TimeUnit.SECONDS);
			boolean hasResults = !("\"0\"".equals(String.valueOf(imageSearchResult.get("searchInformation").get("totalResults"))));
			if (hasResults) {
				String thumbnailUrl = imageSearchResult.get("items").get(0).get("image").get("thumbnailLink").asText();
				googleThumbnail = Util.readImage(ClientBuilder.newClient().target(thumbnailUrl).request().get(byte[].class));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return googleThumbnail;
	}

	private void setProfilePic(BufferedImage pic) {
		profilePic.clear();
		profilePic.add(Util.circleCrop(pic));
	}

	private void loadSlideImages(boolean pullSlides) {
		//load slide images
		if (slides.isEmpty() || pullSlides) {
			List<Slide> newSlides = Util.loadSlides();
			slides.clear();
			slides.addAll(newSlides);
		}
	}

	private void processPrintQueues() throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {
		//process and update printer queues
		Map<String, Boolean> newStatus = updatePrinterStatus();

		Map<String, Future<List<PrintJob>>> futureJobs = new HashMap<>();
		Map<String, List<PrintJob>> newJobs = new HashMap<>();
		//iterates over each queue and gets a list of jobs sent to them
		for (Entry<String, Boolean> q : newStatus.entrySet()) {
			if (printerStatus.get(q.getKey())) {
				futureJobs.put(q.getKey(), tepidServer
								.path("queues").path(q.getKey())  	//path to specific queue
								.queryParam("limit", 10)
								.queryParam("from", System.currentTimeMillis() - (60 * 60 * 1000)) //only get jobs from the last hour
								.request(MediaType.APPLICATION_JSON).async()
								.get(new GenericType<List<PrintJob>>(){}));

			} else {
				newJobs.put(q.getKey(), new ArrayList<PrintJob>(0));
			}
		}
		for (Entry<String, Future<List<PrintJob>>> e : futureJobs.entrySet()) {
			newJobs.put(e.getKey(), e.getValue().get(interval, TimeUnit.SECONDS));
		}
		jobData.clear();
		jobData.putAll(newJobs);
	}

	private NameUser updateUserInfo() {
		//update user info

		String command = "powershell.exe \"Import-Module ActiveDirectory; $attributes = 'displayName', 'samAccountName', 'mail', 'name', 'givenName', 'surname';" +
				"Get-AdUser " + System.getenv("username") + " -Properties $attributes | select $attributes\"";

		Map<String, String> nameInformation = new HashMap<>();
		try {
			Process PsGetAdUser = Runtime.getRuntime().exec(command);
			BufferedReader stdout = new BufferedReader(new InputStreamReader(PsGetAdUser.getInputStream()));

			String rawLine;
			while ((rawLine = stdout.readLine()) != null) {
				String[] line = rawLine.split(":");
				if (line.length == 2){
					nameInformation.put(line[0].trim(), line[1].trim());
				}
			}
			stdout.close();
		} catch (Exception e) {
			System.err.println("Could not fetch user info using powershell");
		}

		NameUser user = new NameUser();
		user.setDisplayName(nameInformation.get("displayName"));
		user.setGivenName(nameInformation.get("givenName"));
		user.setLastName(nameInformation.get("surname"));
		user.setShortUser(nameInformation.get("samAccountName"));
		user.setEmail(nameInformation.get("mail"));

		//todo: handle 404 error
		Future<String> futureNick = Main.LOGGED_IN ? tepidServer.path("user").path(System.getenv("username")).request(MediaType.APPLICATION_JSON).async().get(String.class) : null;

		if (futureNick != null) try {
			String newNick = futureNick.get(interval, TimeUnit.SECONDS);
			user.setNick(newNick);
		} catch (javax.ws.rs.NotFoundException e404){
		    // means that there is no nick for that user
        } catch (Exception e) {
		    if (e.getCause() instanceof javax.ws.rs.NotFoundException){
                // means that there is no nick for that user
            } else{
                System.err.println("Could not fetch user nick: \n" + e);
            }
		}

		user.setSalutation(user.getNick() != null ? user.getNick() : user.getDisplayName());
		if (user.getNick() != null) {
			user.setSalutation(user.getNick());
		} else if (user.getDisplayName() != null) {
			user.setSalutation(user.getDisplayName());
		} else if (user.getShortUser() != null) {
			user.setSalutation(user.getShortUser());
		} else {
			user.setSalutation(System.getenv("username"));
		}

		nameUser.clear();
		nameUser.add(user);

		return user;
	}

	private void updateDestinations() throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {
		//update destinations
		Future<Map<String, Destination>> futureDestinations = tepidServer.path("/destinations").request(MediaType.APPLICATION_JSON).async().get(new GenericType<Map<String, Destination>>(){});
		Map<String, Destination> newDestinations = futureDestinations.get(interval, TimeUnit.SECONDS);
		destinations.clear();
		destinations.putAll(newDestinations);
	}

	@NotNull
	private Map<String, Boolean> updatePrinterStatus() throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {
		//update printer status
		Future<Map<String, Boolean>> futureStatus = tepidServer.path("/queues/status").request(MediaType.APPLICATION_JSON).async().get(new GenericType<Map<String, Boolean>>(){});
		Map<String, Boolean> newStatus = futureStatus.get(interval, TimeUnit.SECONDS);
		printerStatus.clear();
		printerStatus.putAll(newStatus);
		return newStatus;
	}

	private void updateMarqueeData() throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {
		//update marquee data
		Future<MarqueeData[]> futureMarquee = tepidServer.path("marquee").request(MediaType.APPLICATION_JSON).async().get(MarqueeData[].class);
		List<MarqueeData> newMarquee = Arrays.asList(futureMarquee.get(interval, TimeUnit.SECONDS));
		marqueeData.clear();
		marqueeData.addAll(newMarquee);
	}

	private void processEvents()
			throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {

		Future<String> futureEvents = icalServer.path(icsPath).request(MediaType.TEXT_PLAIN).async().get(String.class);

		//process upcoming events (if this is an office computer)
		ICalendar ical = Biweekly.parse(futureEvents.get(interval, TimeUnit.SECONDS)).first();

		Calendar c = Calendar.getInstance();
		Date eventsStart = c.getTime();
		c.add(Calendar.MONTH, 2);
		Date eventsEnd = c.getTime();
		//filter events (remove past events, only include soonest instance of recurring event, make sure it's current semester)
		TimezoneInfo tzInfo = ical.getTimezoneInfo();
		List<Pair<Date, VEvent>> events = Semester.filterEvents(ical.getEvents(), eventsStart, eventsEnd, tzInfo);

		//format into human-friendly strings
		upcomingEvents.clear();
		for (Pair<Date, VEvent> event : events) {
			Date d = event.getValue0();
			VEvent e = event.getValue1();

			Calendar timeOfEvent = GregorianCalendar.getInstance();
			timeOfEvent.setTime(d);
			boolean isOnTheHour = timeOfEvent.get(Calendar.MINUTE) == 0;
			timeOfEvent.set(Calendar.HOUR_OF_DAY, 0);
			timeOfEvent.set(Calendar.MINUTE, 0);
			timeOfEvent.set(Calendar.SECOND, 0);
			timeOfEvent.set(Calendar.MILLISECOND, 0);

			Calendar oneWeek = GregorianCalendar.getInstance();
			oneWeek.add(Calendar.DATE, 6);

			Calendar today = GregorianCalendar.getInstance();
			today.set(Calendar.HOUR_OF_DAY, 0);
			today.set(Calendar.MINUTE, 0);
			today.set(Calendar.SECOND, 0);
			today.set(Calendar.MILLISECOND, 0);

			boolean isSoon = timeOfEvent.before(oneWeek),
			isToday = timeOfEvent.equals(today);
			String dateFormat = (isToday ? "" : (isSoon ? "E": "MMM d")) + (isOnTheHour ? " @ h a" : " @ h:mm a");
			upcomingEvents.add((isToday ? "Today" : "") + new SimpleDateFormat(dateFormat).format(d) + " - " + e.getSummary().getValue());
		}
		System.out.println("Fetched events");
	}

	private static TimeZone getTimezone(TimezoneInfo tzInfo, VEvent e) {
		DateStart dateStart = e.getDateStart();
		TimeZone timezone;
		if (tzInfo.isFloating(dateStart)){
		  timezone = TimeZone.getDefault();
		} else {
		  TimezoneAssignment dateStartTimezone = tzInfo.getTimezone(dateStart);
		  timezone = (dateStartTimezone == null) ? TimeZone.getTimeZone("UTC") : dateStartTimezone.getTimeZone();
		}
		return timezone;
	}

	private enum Semester {
		FALL, WINTER, SPRING;
	    public Semester next() {
	        return values()[(this.ordinal() + 1) % values().length];
	    }
		private static Semester getSemester(Date d) {
			Calendar c = GregorianCalendar.getInstance();
			c.setTime(d);
			int month = c.get(Calendar.MONTH);
			if (month > Calendar.AUGUST) return Semester.FALL;
			if (month > Calendar.APRIL) return Semester.SPRING;
			return Semester.WINTER;
		}
		private static List<Pair<Date, VEvent>> filterEvents(List<VEvent> rawEvents, Date start, Date end, TimezoneInfo tzInfo) {
			List<Pair<Date, VEvent>> events = new ArrayList<>();
			for (VEvent e : rawEvents) {
				Date soonest = null;
				for (DateIterator iter = e.getDateIterator(getTimezone(tzInfo, e)); iter.hasNext();) {
					Date d = iter.next();
					if (d.before(start)) continue;
					if (soonest == null || d.before(soonest)) soonest = d;
					if (d.after(end)) break;
				}
				if (soonest != null) events.add(new Pair<>(soonest, e));
			}
			events.sort(Comparator.comparing(Pair::getValue0));
			return events;
		}
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
