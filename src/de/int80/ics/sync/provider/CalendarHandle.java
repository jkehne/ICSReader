package de.int80.ics.sync.provider;

import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;

public class CalendarHandle {
	
	private long calID;
	private String accountName;
	private String accountType;
	private Context mContext;

	private static Uri asSyncAdapter(Uri uri, String accountName, String accountType) {
	    return uri.buildUpon()
	        .appendQueryParameter(android.provider.CalendarContract.CALLER_IS_SYNCADAPTER,"true")
	        .appendQueryParameter(Calendars.ACCOUNT_NAME, accountName)
	        .appendQueryParameter(Calendars.ACCOUNT_TYPE, accountType).build();
	 }
	
	public CalendarHandle(Context context, long calID, String name, String type) {
		this.calID = calID;
		this.accountName = name;
		this.accountType = type;
		this.mContext = context;
	}

	public CalendarHandle(Context context, String name, String type) {
		ContentValues values = new ContentValues();
		values.put(Calendars.ACCOUNT_NAME, name);
		values.put(Calendars.ACCOUNT_TYPE, type);
		values.put(Calendars.CALENDAR_DISPLAY_NAME, name);
		Uri ret = context.getContentResolver().insert(
				asSyncAdapter(Calendars.CONTENT_URI, name, type), 
				values
				);
		calID = Long.parseLong(ret.getLastPathSegment());
		accountName = name;
		accountType = type;
		mContext = context;
	}

	public long getCalID() {
		return calID;
	}
	
	public void insertEvent(Date start, Date end, String title, String desc, String loc) {
		ContentResolver cr = mContext.getContentResolver();
		ContentValues values = new ContentValues();
		values.put(Events.DTSTART, start.getTime());
		values.put(Events.DTEND, end.getTime());
		values.put(Events.TITLE, title);
		values.put(Events.DESCRIPTION, desc);
		values.put(Events.CALENDAR_ID, calID);
		values.put(Events.EVENT_TIMEZONE, "Europe/Berlin");
		Uri uri = cr.insert(asSyncAdapter(Events.CONTENT_URI, accountName, accountType), values);
	}
}
