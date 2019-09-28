package ca.mcgill.sus.screensaver.datafetch;

import ca.mcgill.science.tepid.api.ITepidScreensaver;
import ca.mcgill.science.tepid.models.data.NameUser;
import ca.mcgill.sus.screensaver.ConfigKt;

import java.util.concurrent.TimeUnit;

public abstract class UserFetch extends DataFetchable<NameUser> {

    private NameUser user;

    private boolean hasNick = true;

    private final long timeOutInterval;
    private final ITepidScreensaver api;

    public UserFetch(long _timeOutInterval, ITepidScreensaver _api) {
        timeOutInterval = _timeOutInterval;
        api = _api;
    }

    @Override
    public FetchResult<NameUser> fetch() {
        FetchResult<NameUser> userResult = getCurrentUser();
        if (!userResult.success) {
            return new FetchResult<>(null, false);
        }
        NameUser user = userResult.value;
        if (hasNick) {
            FetchResult<String> nickResult = getNick(user.getShortUser());
            if (nickResult.success) {
                user.setNick(nickResult.value);
            }
        }
        System.out.println(user);
        computeSalutation(user);

        this.user = user;
        return new FetchResult<>(this.user);
    }

    protected abstract FetchResult<NameUser> getCurrentUser();

    private FetchResult<String> getNick(String shortUser) {
        try {
            String newNick = ConfigKt.asCompletableFuture(api.getUserNick(shortUser)).get(timeOutInterval, TimeUnit.SECONDS);
            return new FetchResult<>(newNick);
        } catch (javax.ws.rs.NotFoundException e404) {
            hasNick = false;
            return new FetchResult<>(null, true);
        } catch (Exception e) {
            return new FetchResult<>(null, false);
        }
    }

    private void computeSalutation(NameUser user) {
        if (user.getNick() != null) {
            user.setSalutation(user.getNick());
        } else if (user.getDisplayName() != null) {
            user.setSalutation(user.getDisplayName());
        } else if (user.getShortUser() != null) {
            user.setSalutation(user.getShortUser());
        }
    }
}
