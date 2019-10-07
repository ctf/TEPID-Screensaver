package ca.mcgill.sus.screensaver.datafetch;

public abstract class DataFetchable<T> {

    protected T value;

    public abstract FetchResult<T> fetch();

    public FetchResult<T> fetchUnexceptionally() {
        try {
            return fetch();
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            return new FetchResult<T>(null, false);
        }
    }

    public FetchResult<T> cachedOrFetch() {
        if (value != null){
            return new FetchResult<>(value);
        } else {
            return fetch();
        }
    }

    public FetchResult<T> cachedOrFetchUnexceptionally() {
        try {
            return cachedOrFetch();
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            return new FetchResult<T>(null, false);
        }
    }
}
