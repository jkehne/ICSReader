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
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.DateProperty;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class ICSCalendarSyncAdapterService extends Service {

	private class ICSCalendarSyncAdapterImpl extends
			AbstractThreadedSyncAdapter {

		private Context mContext;
		private static final String TAG = "ICSCalendarSyncAdapterImpl";
		private CalendarBuilder calendarBuilder = new CalendarBuilder();

		public ICSCalendarSyncAdapterImpl(Context context) {
			super(context, true);
			mContext = context;
		}

		@Override
		public void onPerformSync(Account account, Bundle extras,
				String authority, ContentProviderClient provider,
				SyncResult syncResult) {
			AccountManager am = AccountManager.get(ICSCalendarSyncAdapterService.this);
			Log.d(TAG, "performSync: " + account.toString());
			String calendarURL = am.getUserData(account, CALENDAR_URL_KEY);
			Log.d(TAG, "Calendar URL is " + calendarURL);
			final String user = am.getUserData(account, USERNAME_KEY);
			Log.d(TAG, "Username is '" + user + "'");
			final String pass = am.getPassword(account);
			if (! pass.equals(""))
				Log.d(TAG, "Password is set");
			else
				Log.d(TAG, "Password is NOT set");
			long calID = Long.valueOf(am.getUserData(account, CALENDAR_ID_KEY));
			CalendarHandle calHandle = new CalendarHandle(mContext, calID, account.name, account.type);
			
			URL url;
			try {
				url = new URL(calendarURL);
			} catch (MalformedURLException e) {
				Log.e(TAG, "Malformed URL: " + calendarURL);
				return;
			}
			
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(user, pass.toCharArray());
				}
			});

			long lastSync = calHandle.getLastSyncTime();
			InputStream in;
			try {
				if (calendarURL.startsWith("https://")) {
					HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
					connection.setIfModifiedSince(lastSync);
					if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
						Log.d(TAG, "Calendar was not modified since last sync");
						return;
					}
					if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
						Log.e(TAG, "Failed to fetch calendar: Response code " + connection.getResponseCode());
						syncResult.stats.numIoExceptions++;
						return;
					}
					in = new BufferedInputStream(connection.getInputStream());
				} else {
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.setIfModifiedSince(lastSync);
					if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
						Log.i(TAG, "Calendar was not modified since last sync");
						return;
					}
					if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
						Log.e(TAG, "Failed to fetch calendar: Response code " + connection.getResponseCode());
						syncResult.stats.numIoExceptions++;
						return;
					}
					in = new BufferedInputStream(connection.getInputStream());
				}
			} catch(IOException e) {
				Log.e(TAG, "Failed to connect to " + calendarURL, e);
				syncResult.stats.numIoExceptions++;
				return;
			}
			
			Calendar calendar;
			try {
				calendar = calendarBuilder.build(in);
			} catch (IOException e) {
				Log.e(TAG, "Failed to download calendar " + calendarURL, e);
				syncResult.stats.numIoExceptions++;
				return;
			} catch (ParserException e) {
				Log.e(TAG, "Failed to parse ics file from " + calendarURL, e);
				syncResult.stats.numParseExceptions++;
				return;
			}
			
			//first, delete all events from the calendar, then re-insert them
			calHandle.deleteAllEvents();
			
			boolean allDay;
			for (Object entryObject : calendar.getComponents()) {
				Component entry = (Component) entryObject;
				
				//for now, we ignore anything that's not an event
				if (! entry.getName().equals("VEVENT"))
					continue;
				
				//now, get the event properties...
				DateProperty startDate = (DateProperty) entry.getProperty("DTSTART");
				if (startDate == null) {
					Log.e(TAG, "Invalid event (DTSTART is null)");
					syncResult.stats.numSkippedEntries++;
					continue;
				}
				Date start = startDate.getDate();
				Parameter param = startDate.getParameter("VALUE");
				if (param != null)
					allDay = param.getValue().equals(Value.DATE.getValue());
				else
					allDay = false;
				
				DateProperty endDate = (DateProperty) entry.getProperty("DTEND");
				Date end;
				if (endDate == null) {
					Log.e(TAG, "Invalid event (DTEND is null). Assuming all-day event.");
					allDay = true;
					end = null;
				} else
					end = endDate.getDate();
				
				Property titleProp = entry.getProperty("SUMMARY");
				if (titleProp == null) {
					Log.e(TAG, "Invalid event (Event title is null)");
					syncResult.stats.numSkippedEntries++;
					continue;
				}
				String title = titleProp.getValue();
				
				//the remaining properties are optional, 
				//so we need to expect and accept null values
				Property descProp = entry.getProperty("DESCRIPTION");
				String desc;
				if (descProp == null)
					desc = "";
				else
					desc = descProp.getValue();
				
				Property locProp = entry.getProperty("LOCATION");
				String loc;
				if (locProp == null)
					loc = "";
				else
					loc = locProp.getValue();
				
				//... and insert the event into the android calendar
				calHandle.insertEvent(start, end, title, desc, loc, allDay);
			}
			calHandle.updateLastSyncTime();
		}

	}

	private static final String TAG = "ICSCalendarSyncAdapterService";
	private static String CALENDAR_URL_KEY;
	private static String USERNAME_KEY;
	private static String CALENDAR_ID_KEY;
	private static ICSCalendarSyncAdapterImpl sSyncAdapter;

	public ICSCalendarSyncAdapterService() {
		super();
	}

	private ICSCalendarSyncAdapterImpl getSyncAdapter() {
		if (sSyncAdapter == null) {
			sSyncAdapter = new ICSCalendarSyncAdapterImpl(this);
			CALENDAR_URL_KEY = getString(R.string.URL_KEY);
			USERNAME_KEY = getString(R.string.USERNAME_KEY);
			CALENDAR_ID_KEY = getString(R.string.CALENDAR_ID_KEY);
		}
		
		return sSyncAdapter;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return getSyncAdapter().getSyncAdapterBinder();
	}	
}
