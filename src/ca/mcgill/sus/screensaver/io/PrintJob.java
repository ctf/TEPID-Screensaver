package ca.mcgill.sus.screensaver.io;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrintJob {
	public final String type = "job";
	private String name, queueName, originalHost, userIdentification, destination, error, file;
	private int colorPages, pages;
	public final Date started = new Date();
	private Date processed, printed, failed, received;
	private long eta;
	public String getName() {
		return name;
	}
	public String truncateName(int length) {
		if (name.length() > length) {
			return name.substring(0, length - 3) + "...";
		} else {
			return name;
		}
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getQueueName() {
		return queueName;
	}
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
	public String getOriginalHost() {
		return originalHost;
	}
	public void setOriginalHost(String originalHost) {
		this.originalHost = originalHost;
	}
	public String getUserIdentification() {
		return userIdentification;
	}
	public void setUserIdentification(String userIdentification) {
		this.userIdentification = userIdentification;
	}
	public int getPages() {
		return pages;
	}
	public void setPages(int pages) {
		this.pages = pages;
	}
	public int getColorPages() {
		return colorPages;
	}
	public void setColorPages(int colorPages) {
		this.colorPages = colorPages;
	}
	public Date getProcessed() {
		return processed;
	}
	public void setProcessed(Date processed) {
		this.processed = processed;
	}
	public Date getPrinted() {
		return printed;
	}
	public void setPrinted(Date printed) {
		this.printed = printed;
	}
	public long getEta() {
		return eta;
	}
	public void setEta(long eta) {
		this.eta = eta;
	}
	public Date getFailed() {
		return failed;
	}
	public void setFailed(Date failed) {
		this.failed = failed;
	}
	public void setFailed(Date failed, String error) {
		this.failed = failed;
		this.error = error;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	public String getFile() {
		return file;
	}
	public void setFile(String file) {
		this.file = file;
	}
	public Date getReceived() {
		return received;
	}
	public void setReceived(Date received) {
		this.received = received;
	}
	public String getDestination() {
		return destination;
	}
	public void setDestination(String destination) {
		this.destination = destination;
	}

	private String _id, _rev;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();
	@JsonProperty("_id")
	public String getId() {
		return _id;
	}
	@JsonProperty("_id")
	public void setId(String _id) {
		this._id = _id;
	}
	@JsonProperty("_rev")
	public String getRev() {
		return _rev;
	}
	@JsonProperty("_rev")
	public void setRev(String _rev) {
		this._rev = _rev;
	}

	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}
	
	@JsonAnySetter
	public void setAdditionalProperty(String name, Object value) {
		this.additionalProperties.put(name, value);
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PrintJob [name=").append(name).append(", queueName=")
				.append(queueName).append(", originalHost=")
				.append(originalHost).append(", userIdentification=")
				.append(userIdentification).append("]");
		return builder.toString();
	}
	
}
