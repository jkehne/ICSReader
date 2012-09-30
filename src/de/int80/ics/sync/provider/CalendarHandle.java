package de.int80.ics.sync.provider;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.CalendarContract.Calendars;

public class CalendarHandle {
	
	private long calID;

	public CalendarHandle(Context context, int calID) {
		this.calID = calID;
	}

	public CalendarHandle(Context context, String name, String type) {
		ContentValues values = new ContentValues();
		values.put(Calendars.ACCOUNT_NAME, name);
		values.put(Calendars.ACCOUNT_TYPE, type);
		values.put(Calendars.CALENDAR_DISPLAY_NAME, name);
		Uri ret = context.getContentResolver().insert(Calendars.CONTENT_URI, values);
		calID = Long.parseLong(ret.getLastPathSegment());
	}

	public long getCalID() {
		return calID;
	}
}
