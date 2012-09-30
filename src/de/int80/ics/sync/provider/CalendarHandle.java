package de.int80.ics.sync.provider;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;

public class CalendarHandle {
	
	private long calID;

	private static Uri asSyncAdapter(Uri uri, String account, String accountType) {
	    return uri.buildUpon()
	        .appendQueryParameter(android.provider.CalendarContract.CALLER_IS_SYNCADAPTER,"true")
	        .appendQueryParameter(Calendars.ACCOUNT_NAME, account)
	        .appendQueryParameter(Calendars.ACCOUNT_TYPE, accountType).build();
	 }
	
	public CalendarHandle(Context context, int calID) {
		this.calID = calID;
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
	}

	public long getCalID() {
		return calID;
	}
}
