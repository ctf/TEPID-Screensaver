package ca.mcgill.sus.screensaver.datafetch;

public abstract class DataFetchable<T> {
    public abstract FetchResult<T> fetch ();

    public FetchResult<T> fetchUnexceptionally(){
        try {
            return fetch();
        } catch (Exception e){
            System.out.println(e.getLocalizedMessage());
            return new FetchResult<T>(null, false);
        }
    };
}
