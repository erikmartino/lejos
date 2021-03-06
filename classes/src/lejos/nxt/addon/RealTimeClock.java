package lejos.nxt.addon;

import lejos.nxt.I2CSensor;
import lejos.nxt.I2CPort;
import lejos.robotics.Clock;

import java.io.IOException;
import java.util.Date;

/**
 * 
 * Mindsensors Real-Time Clock
 * www.mindsensors.com
 * 
 * @author Robert W. Kramer
 *
 */

public class RealTimeClock extends I2CSensor implements Clock {
	private byte[] buf;
	private final byte[] dowSkew = {0,3,3,6,1,4,6,2,5,0,3,5};
    public static final int DEFAULT_RTC_ADDRESS = 0xd0;

	/**
	 * Constructor
     * @param p Port used by RTC
     * @param address I2C address of the RTC
	 */
	public RealTimeClock(I2CPort p, int address) {
		super(p, address, I2CPort.LEGO_MODE, TYPE_LOWSPEED);
		buf = new byte[8];
	}

	/**
	 * Constructor
	 * @param p Port used by RTC
	 */
	public RealTimeClock(I2CPort p) {
		this(p, DEFAULT_RTC_ADDRESS);
	}

	//
	// NB: For general use, the next two functions ought to throw
	//     an IllegalArgumentException or something similar if the
	//     parameter is bogus (e.g., pack(100) or unpack(0xbc).
	//     However, their use in this class should guarantee
	//     parameters that are always in-bounds.
	//

	/**
	 * Converts n (0-99) into binary coded decimal form
	 * @param n Value to be packed
	 * @return Packed (BCD) form
	 */
	private byte pack(int n) {
		return (byte)(((n/10)<<4)+(n%10));
	}

	/**
	 * Converts BCD value to binary 
	 * @param b Byte to be unpacked
	 * @return b in integer form
	 */
	private int unpack(int b) {
		return 10 * (b >> 4) + (b & 0x0f);
	}

	/**
	 * computes day of week from RTC date, resets RTC day of week.
	 * Notes:
	 * 1: setting date fields does not automatically update RTC
	 *    day of week; this function is called by the set functions
	 * 2: algorithm comes from http://klauser.com/new-dayofweek.html
	 * @throws IOException if communication to RTC fails
	 */
	private void computeDayOfWeek() throws IOException {
	  int y=getYear(),m=getMonth(),dow;

	  dow = y - 1900;
	  dow += dow / 4;
	  
	  if (y % 4 == 0 && m < 3)
		  dow--;
	  
	  dow += getDay() + dowSkew[m-1];
	  dow = dow % 7 + 1;
	  setDayOfWeek(dow);
	}

	/**
	 * Get RTC year as int
	 * @return Current year (2000-2099)
	 * @throws IOException if communication to RTC fails 
	 */
	public int getYear() throws IOException {
		if (getData(0x06,buf,1) != 0)
			throw new IOException();
		
		return 2000 + unpack(buf[0]);
	}

	/**
	 * Get RTC month as int
	 * @return Current month (1-12)
	 * @throws IOException if communication to RTC fails
	 */
	public int getMonth() throws IOException {
		if (getData(0x05,buf,1) != 0)
			throw new IOException();
		
		return unpack(buf[0]);
	}

	/**
	 * Get RTC day of month as int
	 * @return Current day (1-31)
	 * @throws IOException if communication to RTC fails
	 */
	public int getDay() throws IOException {
		if (getData(0x04,buf,1) != 0)
			throw new IOException();
		
		return unpack(buf[0]);
	}

	/**
	 * Get RTC day of week as int
	 * @return Current day of week (1-7)
	 * @throws IOException if communication to RTC fails
	 */
	public int getDayOfWeek() throws IOException {
		if (getData(0x03,buf,1) != 0)
			throw new IOException();
		
		return buf[0];
	}

	/**
	 * Get RTC hour as int. Uses current RTC format (12- or 24-hour)
	 * @return Current hour (1-12 or 0-23)
	 * @throws IOException if communication to RTC fails
	 */
	public int getHour() throws IOException {
		if (getData(0x02,buf,1) != 0)
			throw new IOException();
		
		if ((buf[0] & 0x40) == 0)
		  return unpack(buf[0] & 0x3f);
		else
		  return unpack(buf[0] & 0x1f);
	}

	/**
	 * Get RTC hour as int. Uses 0-23 format.
	 * @return Current hour (0-23)
	 * @throws IOException if communication to RTC fails
	 */
	public int getCanonicalHour() throws IOException {
		int h;
		
		if (getData(0x02,buf,1) != 0)
			throw new IOException();
		if ((buf[0] & 0x40) == 0)
			return unpack(buf[0] & 0x3f);
		else {
			//
			// convert hour to int
			//			
			h = unpack(buf[0] & 0x1f);			

			//
			// 12 AM/PM maps to 0 AM/PM, other hours left alone
			//
			h = h % 12;
			
			//
			// add 12 hours for PM
			//
			if ((buf[0] & 0x20) != 0)
				h += 12;
			
			return h;
		}
	}

	/**
	 * Get RTC AM/PM indication as boolean flag. Works in 12- and 24-hour
	 * formats.
	 * @return True = AM; false = PM
	 * @throws IOException if communication to RTC fails
	 */
	public boolean isPM() throws IOException {
		if (getData(0x02,buf,1) != 0)
			throw new IOException();
		if ((buf[0] & 0x40) == 0)
			//
			// 24-hour mode, see if hour >= 12
			//
		    return (unpack(buf[0] & 0x3f) >= 12);
		else
			//
			// 12-hour mode, check AM/PM bit
			//			
			return (buf[0] & 0x20) != 0;
	}

	/**
	 * Get RTC minute as int 
	 * @return Current minute (0-59)
	 * @throws IOException if communication to RTC fails
	 */
	public int getMinute() throws IOException {
		int ret = getData(0x01,buf,1);

		if (ret != 0)
			throw new IOException();
		
		return unpack(buf[0]);
	}

	/**
	 * Get RTC second as int
	 * @return Current second (0-59)
	 * @throws IOException if communication to RTC fails
	 */
	public int getSecond() throws IOException {
		int ret = getData(0x00,buf,1);

		if (ret != 0)
			throw new IOException();

		return unpack(buf[0]);
	}

	/**
	 * Get RTC date and time as a Date object
	 * @return Current date and time
	 * @throws IOException if communication to RTC fails
	 */
	public Date getDate() throws IOException {
		Date d = new Date();
		int h;

		if (getData(0x00,buf,7) != 0)
			throw new IOException();

		d.setSeconds(unpack(buf[0]));
		d.setMinutes(unpack(buf[1]));

		if ((buf[2] & 0x40) == 0)
			h = unpack(buf[2] & 0x3f);
		else {
			h = unpack(buf[2] & 0x1f);
			h = h % 12;

			if ((buf[2] & 0x20) != 0)
				h += 12;
		}

		d.setHours(h);
		d.setDate(unpack(buf[4]));
		d.setMonth(unpack(buf[5]));
		d.setYear(2000+unpack(buf[6]));
		
		return d;
	}

	/**
	 * Retrieve RTC date as String
	 * @param usePadding True = always use two digits for day and month
	 * @param dayFirst True = dd/mm/yyyy, false = mm/dd/yyyy
	 * @return Current date as String
	 * @throws IOException if communication to RTC fails
	 */
	public String getDateString(boolean usePadding,boolean dayFirst) throws IOException {
		int m=getMonth(),d=getDay(),y=getYear();
		String s1,s2;

		if (m < 10 && usePadding)
			s1 = "0" + m;
		else
			s1 = "" + m;

		if (d < 10 && usePadding)
			s2 = "0" + d;
		else
			s2 = "" + d;

		if (dayFirst)
			return s2 + "/" + s1 + "/" + y;
		else
			return s1 + "/" + s2 + "/" + y;
	}

	/**
	 * Retrieve RTC date as String in mm/dd/yyyy format 
	 * @return Current date as String
	 * @throws IOException if communication to RTC fails
	 */
	public String getDateString() throws IOException {
		return getDateString(true,false);
	}

	/**
	 * Get RTC time as String
	 * @return Current time in hh:mm:ss format
	 * @throws IOException if communication to RTC fails
	 */
	public String getTimeString() throws IOException {
		int m=getMinute(),h=getHour(),s=getSecond();
		String s1,s2,s3;

		if (h < 10)
			s1 = "0" + h;
		else
			s1 = "" + h;

		if (m < 10)
			s2 = "0" + m;
		else
			s2 = "" + m;

		if (s < 10)
			s3 = "0" + s;
		else
			s3 = "" + s;
		
		return s1 + ":" + s2 + ":" + s3;
	}

	/**
	 * Get RTC AM/PM indication as String. Works in both 12- and 24-hour
	 * formats.
	 * @return "AM" or "PM"
	 * @throws IOException if communication to RTC fails
	 */
	public String getAMPM() throws IOException {
		if (isPM())
			return "PM";
		else
			return "AM";
	}

	/**
	 * Set RTC hour. Uses canonical input, does not change 12/24 hour mode.
	 * @param h New hour count
	 * @throws IllegalArgumentException if h < 0 or h > 23
	 * @throws IOException if communication to RTC fails
	 */
	public void setHour(int h) throws IllegalArgumentException,IOException {
		if (h < 0 || h > 23)
			throw new IllegalArgumentException();		

		if (getData(0x02,buf,1) != 0)
			throw new IOException();
		if ((buf[0] & 0x40) == 0)
			buf[0] = pack(h);
		else {
			buf[0] = 0x40;
			if (h >= 12)
				buf[0] |= 0x20;
			h = h % 12;
			if (h == 0)
				h = 12;
			buf[0] |= pack(h);
		}

		if (sendData(0x02,buf,1) != 0)
			throw new IOException();
	}

	/**
	 * Set RTC hour using AM/PM format. Does not change RTC 12/24 hour mode.
	 * @param h Current hour (1-12)
	 * @param isPM True = PM; false = AM
	 * @throws IllegalArgumentException if h < 1 or h > 12
	 * @throws IOException if communication to RTC fails
	 */
	public void setHour(int h,boolean isPM) throws IllegalArgumentException,IOException {
		if (getData(0x02,buf,1) != 0)
			throw new IOException();
		
		if (h < 1 || h > 12)
			throw new IllegalArgumentException();
		
		h = h % 12;
		
		if ((buf[0] & 0x40) == 0) {
			if (isPM)
				h += 12;
			buf[0] = pack(h);
		} else {
			buf[0] = 0x40;
			if (isPM)
				buf[0] |= 0x20;
			if (h == 0)
				h = 12;
			buf[0] |= pack(h);
		}

		if (sendData(0x02,buf,1) != 0)
			throw new IOException();
	}

	/**
	 * Set RTC minute
	 * @param m New minute count
	 * @throws IllegalArgumentException if m < 0 or m > 59
	 * @throws IOException if communication to RTC fails
	 */
	public void setMinute(int m) throws IllegalArgumentException,IOException {
		if (m < 0 || m > 59)
			throw new IllegalArgumentException();

		buf[0] = pack(m);

		if (sendData(0x01,buf,1) != 0)
			throw new IOException();
	}

	/**
	 * Set RTC seconds
	 * @param s New second count
	 * @throws IllegalArgumentException if s < 0 or s > 59
	 * @throws IOException if communication to RTC fails
	 */
	public void setSecond(int s) throws IllegalArgumentException,IOException {
		if (s < 0 || s > 59)
			throw new IllegalArgumentException();
		
		buf[0] = pack(s);
		
		if (sendData(0x00,buf,1) != 0)
			throw new IOException();
	}

	/**
	 * Set RTC time. Hours are canonical, does not change 12/24 hour format.
	 * @param h New hour
	 * @param m New minute
	 * @param s New second
	 * @throws IllegalArgumentException if h, m or s are out of bounds
	 * @throws IOException if communication to RTC fails
	 */
	public void setTime(int h,int m,int s) throws IllegalArgumentException,IOException {
		setHour(h);
		setMinute(m);
		setSecond(s);
	}

	/**
	 * Set RTC month. Also sets RTC day of week.
	 * @param m New month
	 * @throws IllegalArgumentException if m < 1 or m > 12
	 * @throws IOException if communication to RTC fails
	 */
	public void setMonth(int m) throws IllegalArgumentException,IOException {
		if (m < 1 || m > 12)
			throw new IllegalArgumentException();
		
		buf[0] = pack(m);
		
		if (sendData(0x05,buf,1) != 0)
			throw new IOException();

		computeDayOfWeek();
	}

	/**
	 * Set RTC day of month. Also sets RTC day of week.
	 * @param d New day of month
	 * @throws IllegalArgumentException if d is an invalid day of current month
	 * @throws IOException if communication to RTC fails
	 */
	public void setDay(int d) throws IllegalArgumentException,IOException {
		int m = getMonth(),y = getYear();

		//
		// rule out definite bad days
		//
		if (d < 1 || d > 31)
			throw new IllegalArgumentException();
		
		//
		// February, April, June, September and November have 30 days
		//		
		if (d == 31 && (m == 2 || m == 4 || m == 6 || m == 9 || m == 11))
			throw new IllegalArgumentException();

		//
		// February does not have 30 days
		//
		if (d == 30 && m == 2)
			throw new IllegalArgumentException();
		//
		// February only has 29 days in a leap year.
		// Note that this will not work for the year 2100, but I hope that
		// this code will be replaced long before then.
		//
		if (d == 29 && m == 2 && y % 4 != 0)
			throw new IllegalArgumentException();
		
		buf[0] = pack(d);
		
		if (sendData(0x04,buf,1) != 0)
			throw new IOException();
		
		computeDayOfWeek();
	}

	/**
	 * Set RTC year. Also sets RTC day of week.
	 * @param y New year to use
	 * @throws IllegalArgumentException if y < 0 or y > 99
	 * @throws IOException if communication to RTC fails
	 */
	public void setYear(int y) throws IllegalArgumentException,IOException {
		if (y < 2000 || y > 2099)
			throw new IllegalArgumentException();
		
		y = y % 2000;
		buf[0] = pack(y);
		
		if (sendData(0x06,buf,1) != 0)
			throw new IOException();
		
		computeDayOfWeek();
	}

	/**
	 * Set RTC day of week. Does not change current date.
	 * @param d New day of week
	 * @throws IllegalArgumentException if d < 1 or d > 7
	 * @throws IOException if communication to RTC fails
	 */

	public void setDayOfWeek(int d) throws IllegalArgumentException,IOException {
		if (d < 1 || d > 7)
			throw new IllegalArgumentException();
		
		buf[0] = (byte)d;

		if (sendData(0x03,buf,1) != 0)
			throw new IOException();
	}	

	/**
	 * Set RTC date to m/d/y. Also sets RTC day of week.
	 * @param m New month
	 * @param d New day of month
	 * @param y New year
	 * @throws IllegalArgumentException if m, d or y is invalid
	 * @throws IOException if communication to RTC fails
	 */
	public void setDate(int m,int d,int y) throws IllegalArgumentException,IOException {
		setYear(y);
		setMonth(m);

		//
		// NB: year and month must be set first; this allows
		//     setDay() to work when setting the date to Feb 29
		//     of a leap year
		//		
		setDay(d);
	}	

	/**
	 * Reset RTC hour mode. Does not change RTC current hour.
	 * @param use24h True = use 24-hour format; false = use 12-hour format.
	 * @throws IOException if communication to RTC fails
	 */
	public void setHourMode(boolean use24h) throws IOException {
		int h = getCanonicalHour();

		if (use24h)
			buf[0] = pack(h);
		else {
			buf[0] = 0x40;
			
			if (h >= 12)
				buf[0] |= 0x20;	
			
			h = h % 12;
			
			if (h == 0)
				h = 12;
			
			buf[0] |= pack(h);
		}

		if (sendData(0x02,buf,1) != 0)
			throw new IOException();
	}

	/**
	 * Retrieves RTC RAM byte; can retrieve clock bytes (0-7)
	 * @param loc Byte to retrieve, 0 <= loc <= 0x3f
	 * @return RTC RAM[loc]
	 * @throws IndexOutOfBoundsException if loc < 0 or loc > 63 (0x3f)
	 * @throws IOException if communication to RTC fails
	 */
	public byte getByte(int loc) throws IndexOutOfBoundsException,IOException {
		//
		// ensure loc is within proper bounds.
		// supposedly the RTC ignores anything above bits 0-5, but this makes
		// sure.
		//
		if ((loc & ~0x3f) != 0)
			throw new IndexOutOfBoundsException();

		//
		// grab the byte
		//
		if (getData(loc,buf,1) != 0)
			throw new IOException();

		return buf[0];
	}

	/**
	 * Sets RTC RAM byte to new value
	 * @param loc Byte to set, 0x08 <= loc <= 0x3f
	 * @param b New value of byte
	 * @throws IndexOutOfBoundsException if loc < 8 or loc > 63 (0x3f)
	 * @throws IOException if communication to RTC fails
	 */
	public void setByte(int loc,byte b) throws IndexOutOfBoundsException,IOException {
		//
		// make sure loc is in range.
		// don't allow setting of bytes 0-7, as those are the clock settings
		//
		if (loc > 0x3f || loc < 8)
			throw new IndexOutOfBoundsException();

		buf[0] = b;

		if (sendData(loc,buf,1) != 0)
			throw new IOException();
	}
}

