package ca.mcgill.sus.screensaver.io;

import java.util.Date;


/**An IO class for jobs.
 * 
 * 
 *
 */
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


