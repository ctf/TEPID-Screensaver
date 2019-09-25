package ca.mcgill.sus.screensaver.datafetch;

public class FetchResult<T> {
    public final T value;
    public final Boolean success;
    
    public FetchResult(T _value, Boolean _success){
        value = _value;
        success = _success;
    }

    public FetchResult(T _value){
        this(_value, true);
    }
}
