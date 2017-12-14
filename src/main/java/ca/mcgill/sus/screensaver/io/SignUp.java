package ca.mcgill.sus.screensaver.io;

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
public class SignUp {

	@JsonProperty("_id")
	private String id;
	@JsonProperty("_rev")
	private String rev;
	@JsonProperty("type")
	private String type;
	@JsonProperty("name")
	private String name;
	@JsonProperty("givenName")
	private String givenName;
	@JsonProperty("nickname")
	private String nickname;
	@JsonProperty("slots")
	private Map<String, String[]> slots;
	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	/**
	 * 
	 * @return The id
	 */
	@JsonProperty("_id")
	public String getId() {
		return id;
	}

	/**
	 * 
	 * @param id
	 *            The _id
	 */
	@JsonProperty("_id")
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * 
	 * @return The rev
	 */
	@JsonProperty("_rev")
	public String getRev() {
		return rev;
	}

	/**
	 * 
	 * @param rev
	 *            The _rev
	 */
	@JsonProperty("_rev")
	public void setRev(String rev) {
		this.rev = rev;
	}

	/**
	 * 
	 * @return The type
	 */
	@JsonProperty("type")
	public String getType() {
		return type;
	}

	/**
	 * 
	 * @param type
	 *            The type
	 */
	@JsonProperty("type")
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * 
	 * @return The name
	 */
	@JsonProperty("name")
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @param name
	 *            The name
	 */
	@JsonProperty("name")
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * 
	 * @return The givenName
	 */
	@JsonProperty("givenName")
	public String getGivenName() {
		return givenName;
	}

	/**
	 * 
	 * @param givenName
	 *            The givenName
	 */
	@JsonProperty("givenName")
	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	/**
	 * 
	 * @return The nickname
	 */
	@JsonProperty("nickname")
	public String getNickname() {
		return nickname;
	}

	/**
	 * 
	 * @param nickname
	 *            The nickname
	 */
	@JsonProperty("nickname")
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	/**
	 * 
	 * @return The slots
	 */
	@JsonProperty("slots")
	public Map<String, String[]> getSlots() {
		return slots;
	}

	/**
	 * 
	 * @param slots
	 *            The slots
	 */
	@JsonProperty("slots")
	public void setSlots(Map<String, String[]> slots) {
		this.slots = slots;
	}

	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}

	@JsonAnySetter
	public void setAdditionalProperty(String name, Object value) {
		this.additionalProperties.put(name, value);
	}
}
