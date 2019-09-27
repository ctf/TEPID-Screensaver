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
    private Map<String, Boolean> queueStatuses;


    private Map<String, ITepidFetch<List<PrintJob>>> queueJobFetchers;

    public void setQueueStatuses(Map<String, Boolean> queueStatuses) {
        if (this.queueStatuses == queueStatuses) {
            return;
        }
        this.queueStatuses = queueStatuses;
        queueJobFetchers = new ArrayList<>(queueStatuses.keySet()).stream()
                .collect(Collectors.toMap(
                        queueName -> queueName,
                        queueName -> new ITepidFetch<>(
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
                            if (queueStatuses.get(entry.getKey())) {
                                FetchResult<List<PrintJob>> result = entry.getValue();
                                if (result.success) {
                                    return result.value;
                                }
                            }
                            return new ArrayList<>(0);
                        }
                ));

        return new FetchResult<>(printJobs, success);
    }
}
