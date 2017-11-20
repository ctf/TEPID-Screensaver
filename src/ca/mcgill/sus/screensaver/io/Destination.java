package ca.mcgill.sus.screensaver.io;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Destination {
	@JsonProperty("type")
	public final String type = "destination";
	@JsonProperty("_id")
	public String _id;
	@JsonProperty("_rev")
	public String _rev;
	public String name;
}
