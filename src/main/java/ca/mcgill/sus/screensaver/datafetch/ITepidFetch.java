package ca.mcgill.sus.screensaver.datafetch;

import ca.mcgill.sus.screensaver.ConfigKt;
import retrofit2.Call;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ITepidFetch<T> extends DataFetchable<T> {

    interface ApiCall<T> {
        Call<T> buildCall();
    }

    private final ApiCall<T> fetchFunction;
    private final long timeOutInterval;

    ITepidFetch (long _timeOutInterval, ApiCall<T> _fetchFunction){
        fetchFunction = _fetchFunction;
        timeOutInterval = _timeOutInterval;
    }

    @Override
    public FetchResult<T> fetch() {
        Future<T> futureVal = ConfigKt.asCompletableFuture(	fetchFunction.buildCall());
        try {
            value = futureVal.get(timeOutInterval, TimeUnit.SECONDS);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        return new FetchResult<T>(value);
    }
}
