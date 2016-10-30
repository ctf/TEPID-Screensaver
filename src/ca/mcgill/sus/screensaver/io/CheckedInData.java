
package ca.mcgill.sus.screensaver.io;

/**
 * just an IO class for the OfficeHoursMarquee
 * 
 */
public class CheckedInData 
{
	public String text;				//the message text to display on the marquee
	public String shortUserName;	//the short username; could be used for an ID for other processing
	public boolean checkedIn;		//if the person is checked in. Used to colour their name in the marquee
}
