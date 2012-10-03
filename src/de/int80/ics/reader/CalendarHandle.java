/* Copyright (C) 2012 Jens Kehne
 *
 * ICSReader is free software; you can redistribute it and/or modify it under 
 * the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 2 of the License, or (at your option) 
 * any later version.
 *
 * ICSReader is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
 * more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with ICSReader; if not, see http://www.gnu.org/licenses.
 */

package de.int80.ics.reader;

import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;

public class CalendarHandle {
	
	private long calID;
	private String accountName;
	private String accountType;
	private Context mContext;
	private static final Uri CALENDAR_URI = Uri.parse("content://com.android.calendar/calendars");
	private static final Uri EVENTS_URI = Uri.parse("content://com.android.calendar/events");
	
	private static final String TAG = "CalendarHandle";

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
				asSyncAdapter(CALENDAR_URI, name, type), 
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
	
	public void insertEvent(Date start, Date end, String title, String desc, String loc, boolean allDay) {
		ContentResolver cr = mContext.getContentResolver();
		ContentValues values = new ContentValues();
		values.put(Events.DTSTART, start.getTime());
		values.put(Events.DTEND, end != null ? end.getTime() : start.getTime());
		values.put(Events.TITLE, title);
		values.put(Events.DESCRIPTION, desc);
		values.put(Events.CALENDAR_ID, calID);
		values.put(Events.EVENT_TIMEZONE, "Europe/Berlin");
		values.put(Events.ALL_DAY, allDay ? 1 : 0);
		cr.insert(asSyncAdapter(EVENTS_URI, accountName, accountType), values);
	}
	
	public void deleteAllEvents() {
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(asSyncAdapter(EVENTS_URI, accountName, accountType), 
				new String[]{Events._ID}, null, null, null);
		if (cursor.getCount() == 0)
			return;
		
		cursor.moveToFirst();
		while (! cursor.isAfterLast()) {
			long eventID = cursor.getLong(cursor.getColumnIndex(Events._ID));
			cr.delete(
					asSyncAdapter(
							ContentUris.withAppendedId(
									EVENTS_URI, 
									eventID), 
							accountName, 
							accountType), 
					null, 
					null);
			
			cursor.moveToNext();
		}
		
		cursor.close();
	}
}
