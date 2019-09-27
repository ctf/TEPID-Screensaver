package ca.mcgill.sus.screensaver.datafetch;

import ca.mcgill.science.tepid.api.ITepidScreensaver;
import ca.mcgill.science.tepid.models.data.PrintJob;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JobsFetch extends DataFetchable<Map<String, List<PrintJob>>> {

    private Map<String, List<PrintJob>> printJobs;


    private final long timeOutInterval;
    private final ITepidScreensaver api;
    private List<String> queueNames;


    private Map<String, ITepidFetch<List<PrintJob>>> queueJobFetchers;

    public void setQueues(List<String> queueNames) {
        if (this.queueNames == queueNames) {
            return;
        }
        queueJobFetchers = queueNames.stream()
                .collect(Collectors.toMap(
                        queueName -> queueName,
                        queueName -> new ITepidFetch<List<PrintJob>>(
                                timeOutInterval,
                                () -> api.listJobs(queueName, 10, System.currentTimeMillis() - (60 * 60 * 1000)) //only get jobs from the last hour
                        ))
                );
    }

    JobsFetch(long _timeOutInterval, ITepidScreensaver _api) {
        timeOutInterval = _timeOutInterval;
        api = _api;
    }

    @Override
    public FetchResult<Map<String, List<PrintJob>>> fetch() {

        Map<String, FetchResult<List<PrintJob>>> futureJobs = queueJobFetchers.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        (entry) -> entry.getValue().fetch()
                ));

        boolean success = futureJobs.values().stream().map(v -> v.success).reduce(true, (accumulator, result) -> result && accumulator);

        printJobs = futureJobs.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        (entry) -> {
                            FetchResult<List<PrintJob>> result = entry.getValue();
                            if (result.success) {
                                return result.value;
                            }
                            return new ArrayList<>(0);
                        }
                ));

        return new FetchResult<>(printJobs, success);
    }
}
