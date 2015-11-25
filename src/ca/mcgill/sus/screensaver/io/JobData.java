package ca.mcgill.sus.screensaver.io;

import java.util.Date;


public class JobData {
	
	private String user = "";
	private int jobId, date;
	
	public String getUser() {
		return user;
	}
	
	public int getJobId() {
		return jobId;
	}
	
	public Date getDate() {
		return new Date(date * 1000L);
	}
	
}
