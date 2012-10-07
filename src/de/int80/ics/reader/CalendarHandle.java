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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

import net.fortuna.ical4j.data.CalendarBuilder;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;

public class CalendarHandle {
	
	private long calID;
	private String accountName;
	private String accountType;
	private Context mContext;

	private final Uri CALENDAR_URI;
	private final Uri EVENTS_URI;
	
	//calendar DB field names
	private static final String ACCOUNT_NAME = "account_name";
	private static final String ACCOUNT_TYPE = "account_type";
	private static final String CALENDAR_ID = "calendar_id";
	private static final String EVENT_ID = "_id";
	private static final String SYNC_TIMESTAMP = "cal_sync1";
	private static final String CALENDAR_COLOR = "calendar_color";
	private static final String DTSTART = "dtstart";
	private static final String DTEND = "dtend";
	private static final String TITLE = "title";
	private static final String DESCRIPTION = "description";
	private static final String TIMEZONE = "eventTimezone";
	private static final String ALLDAY = "allDay";
	private static final String DISPLAYNAME = "calendar_displayName";
	private static final String IS_SYNCADAPTER = "caller_is_syncadapter";
	
	private static final String TAG = "CalendarHandle";
	
	public static class CredentialsChecker extends AsyncTask<String, Integer, String> {

		private Context mContext;
		private static final String TAG = "CredentialsChecker";
		
		public CredentialsChecker(Context context) {
			mContext = context;
		}
		
		@Override
		protected String doInBackground(String... params) {
			URL url = null;
			String errMsg = null;
			final String calendarUrl = params[0];
			final String user = params[1];
			final String password = params[2];
			InputStream in = null;

			try {
				url = new URL(calendarUrl);
			} catch (MalformedURLException e) {
				errMsg = mContext.getString(R.string.INVALID_URL_ERROR);
			}

			if (errMsg == null) {

				String userpass = null;
				if (user != null && user.length() > 0)
					userpass = user;
				if (password != null && password.length() > 0)
					userpass += ":" + password;

				try {
					int responseCode;
					URLConnection connection;
					if (calendarUrl.startsWith("https://")) {
						connection = (HttpsURLConnection) url.openConnection();
						
						connection.setRequestProperty("Authorization", "Basic " +
						        Base64.encodeToString(userpass.getBytes(), Base64.NO_WRAP));

						responseCode = ((HttpsURLConnection)connection).getResponseCode();
					} else {
						connection = (HttpURLConnection) url.openConnection();

						connection.setRequestProperty("Authorization", "Basic " +
						        Base64.encodeToString(userpass.getBytes(), Base64.NO_WRAP));
						
						responseCode = ((HttpURLConnection)connection).getResponseCode();
					}
					switch (responseCode) {
					case HttpURLConnection.HTTP_OK:
						//this is what we want
						break;
					case HttpURLConnection.HTTP_NOT_FOUND:
						errMsg = mContext.getString(R.string.HTTP_NOT_FOUND_ERROR);
						break;
					case HttpURLConnection.HTTP_UNAUTHORIZED:
						errMsg = mContext.getString(R.string.HTTP_UNAUTHORIZED_ERROR);
						break;
					default:
						errMsg = mContext.getString(R.string.HTTP_UNKNOWN_ERROR, 
								responseCode);
						break;
					}
						
					if (errMsg == null)
						in = new BufferedInputStream(connection.getInputStream());
					
					if (errMsg == null) {
						try {
							new CalendarBuilder().build(in);
						} catch (Exception e) {
							errMsg = mContext.getString(R.string.INVALID_ICS_FILE_ERROR);
						}
					}
					
				} catch (SSLHandshakeException e) {
					errMsg = mContext.getString(R.string.INVALID_CERT_ERROR);
				} catch(ConnectException e) {
					errMsg = mContext.getString(R.string.CONNECTION_REFUSED_ERROR);
				} catch(Exception e) {
					errMsg = mContext.getString(R.string.UNEXPECTED_ERROR, e.toString());
				}
			}
			return errMsg;
		}
	}
	
	public static boolean checkCredentials(Context context, String calendarUrl, String user, String password) {
		CredentialsChecker checker = new CredentialsChecker(context);
		checker.execute(calendarUrl, user, password);
		String errMsg = null;
		boolean retry = true;
		
		while (retry) {
			try {
				retry = false;
				errMsg = checker.get(30, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				retry = true;
			} catch (ExecutionException e) {
				errMsg = context.getString(R.string.UNEXPECTED_ERROR, e.toString());
			} catch (TimeoutException e) {
				errMsg = context.getString(R.string.TIMEOUT_ERROR);
				checker.cancel(true);
			}
		}
		
		if (errMsg != null) {
			AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
			alertBuilder.setTitle(context.getString(R.string.ERROR));
			alertBuilder.setMessage(errMsg);
			alertBuilder.setNeutralButton(context.getString(R.string.OKBUTTON_LABEL), 
					new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               // User clicked OK button
		           }
		       });
			alertBuilder.create().show();
		}
		return errMsg == null;

	}

	private static Uri asSyncAdapter(Uri uri, String accountName, String accountType) {
	    return uri.buildUpon()
	        .appendQueryParameter(IS_SYNCADAPTER,"true")
	        .appendQueryParameter(ACCOUNT_NAME, accountName)
	        .appendQueryParameter(ACCOUNT_TYPE, accountType).build();
	 }
	
	public CalendarHandle(Context context, long calID, String name, String type) {
		CALENDAR_URI = Uri.parse(context.getString(R.string.CALENDAR_URI));
		EVENTS_URI = Uri.parse(context.getString(R.string.EVENTS_URI));

		this.calID = calID;
		this.accountName = name;
		this.accountType = type;
		this.mContext = context;
	}

	public CalendarHandle(Context context, String name, String type, int color) {
		CALENDAR_URI = Uri.parse(context.getString(R.string.CALENDAR_URI));
		EVENTS_URI = Uri.parse(context.getString(R.string.EVENTS_URI));

		ContentValues values = new ContentValues();
		values.put(ACCOUNT_NAME, name);
		values.put(ACCOUNT_TYPE, type);
		values.put(DISPLAYNAME, name);
		values.put(CALENDAR_COLOR, color);
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
	
	public int getColor() {
		Cursor cursor = mContext.getContentResolver().query(
				asSyncAdapter(CALENDAR_URI, accountName, accountType), 
				new String[]{CALENDAR_COLOR}, 
				null, 
				null, 
				null);
		if (cursor.getCount() == 0) {
			cursor.close();
			return 0xff000000;
		}
		cursor.moveToFirst();
		int color = cursor.getInt(cursor.getColumnIndex(CALENDAR_COLOR));
		cursor.close();
		return color;
	}
	
	public void setColor(int color) {
		ContentValues values = new ContentValues();
		values.put(CALENDAR_COLOR, String.valueOf(color));

		mContext.getContentResolver().update(
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
	
	public void updateLastSyncTime(long timestamp) {
		ContentResolver cr = mContext.getContentResolver();
		ContentValues values = new ContentValues();

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
		values.put(DTSTART, start.getTime());
		values.put(DTEND, end != null ? end.getTime() : start.getTime());
		values.put(TITLE, title);
		values.put(DESCRIPTION, desc);
		values.put(CALENDAR_ID, calID);
		values.put(TIMEZONE, "Europe/Berlin");
		values.put(ALLDAY, allDay ? 1 : 0);
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
