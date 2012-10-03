/* Copyright (C) 2012 Jens Kehne
 *
 * ICSReader is free software; you can redistribute it and/or modify it under 
 * the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 3 of the License, or (at your option) 
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
import android.util.Log;

public class CalendarHandle {
	
	private long calID;
	private String accountName;
	private String accountType;
	private Context mContext;
	private static final Uri CALENDAR_URI = Uri.parse("content://com.android.calendar/calendars");
	private static final Uri EVENTS_URI = Uri.parse("content://com.android.calendar/events");
	private static final String ACCOUNT_NAME = "account_name";
	private static final String ACCOUNT_TYPE = "account_type";
	private static final String CALENDAR_ID = "calendar_id";
	private static final String EVENT_ID = "_id";
	private static final String SYNC_TIMESTAMP = "cal_sync1";
	
	private static final String TAG = "CalendarHandle";

	private static Uri asSyncAdapter(Uri uri, String accountName, String accountType) {
	    return uri.buildUpon()
	        .appendQueryParameter("caller_is_syncadapter","true")
	        .appendQueryParameter(ACCOUNT_NAME, accountName)
	        .appendQueryParameter(ACCOUNT_TYPE, accountType).build();
	 }
	
	public CalendarHandle(Context context, long calID, String name, String type) {
		this.calID = calID;
		this.accountName = name;
		this.accountType = type;
		this.mContext = context;
	}

	public CalendarHandle(Context context, String name, String type, int color) {
		ContentValues values = new ContentValues();
		values.put(ACCOUNT_NAME, name);
		values.put(ACCOUNT_TYPE, type);
		values.put("calendar_displayName", name);
		values.put("calendar_color", color);
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
	
	public long getLastSyncTime() {
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(asSyncAdapter(CALENDAR_URI, accountName, accountType), 
				new String[]{SYNC_TIMESTAMP}, null, null, null);
		if (cursor.getCount() == 0) {
			cursor.close();
			return 0;
		}
		cursor.moveToFirst();
		String timestamp = cursor.getString(cursor.getColumnIndex(SYNC_TIMESTAMP));
		cursor.close();
		if (timestamp == null)
			return 0;
		return Long.parseLong(timestamp);
	}
	
	public void updateLastSyncTime() {
		ContentResolver cr = mContext.getContentResolver();
		ContentValues values = new ContentValues();
		//set last sync to current time - 30 sec. Otherwise, if an event
		//is inserted right after downloading, the next sync might not
		//catch it.
		long timestamp = System.currentTimeMillis() - 30000;
		values.put(SYNC_TIMESTAMP, String.valueOf(timestamp));
		cr.update(
				asSyncAdapter(
						ContentUris.withAppendedId(
								CALENDAR_URI, 
								calID), 
						accountName, 
						accountType), 
				values,
				null,
				null);
	}
	
	public void insertEvent(Date start, Date end, String title, String desc, String loc, boolean allDay) {
		ContentResolver cr = mContext.getContentResolver();
		ContentValues values = new ContentValues();
		values.put("dtstart", start.getTime());
		values.put("dtend", end != null ? end.getTime() : start.getTime());
		values.put("title", title);
		values.put("description", desc);
		values.put(CALENDAR_ID, calID);
		values.put("eventTimezone", "Europe/Berlin");
		values.put("allDay", allDay ? 1 : 0);
		cr.insert(asSyncAdapter(EVENTS_URI, accountName, accountType), values);
	}
	
	public void deleteAllEvents() {
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(asSyncAdapter(EVENTS_URI, accountName, accountType), 
				new String[]{EVENT_ID}, null, null, null);
		if (cursor.getCount() == 0)
			return;
		
		cursor.moveToFirst();
		while (! cursor.isAfterLast()) {
			long eventID = cursor.getLong(cursor.getColumnIndex(EVENT_ID));
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
