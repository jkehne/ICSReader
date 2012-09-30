package de.int80.ics.sync.provider;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;

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
			Log.i(TAG, "performSync: " + account.toString());
			String calendarURL = am.getUserData(account, CALENDAR_URL_KEY);
			Log.i(TAG, "Calendar URL is " + calendarURL);
			final String user = am.getUserData(account, USERNAME_KEY);
			Log.i(TAG, "Username is '" + user + "'");
			final String pass = am.getPassword(account);
			if (! pass.equals(""))
				Log.i(TAG, "Password is set");
			else
				Log.i(TAG, "Password is NOT set");
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

			InputStream in;
			try {
				if (calendarURL.startsWith("https://")) {
					HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
					if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
						Log.e(TAG, "Failed to fetch calendar: Response code " + connection.getResponseCode());
						syncResult.stats.numIoExceptions++;
						return;
					}
					in = new BufferedInputStream(connection.getInputStream());
				} else {
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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
			
			for (Object entryObject : calendar.getComponents()) {
				Component entry = (Component) entryObject;
				Log.i(TAG, "Found entry: " + entry.getName());
				for (Object propObject : entry.getProperties()) {
					Property prop = (Property) propObject;
					Log.i(TAG, "Found property: " + prop.getName() + " -> " + prop.getValue());
				}
			}
		}

	}

	private static final String TAG = "ICSCalendarSyncAdapterService";
	private static String CALENDAR_URL_KEY;
	private static String USERNAME_KEY;
	private static ICSCalendarSyncAdapterImpl sSyncAdapter;

	public ICSCalendarSyncAdapterService() {
		super();
	}

	private ICSCalendarSyncAdapterImpl getSyncAdapter() {
		if (sSyncAdapter == null) {
			sSyncAdapter = new ICSCalendarSyncAdapterImpl(this);
			CALENDAR_URL_KEY = getString(R.string.URL_KEY);
			USERNAME_KEY = getString(R.string.USERNAME_KEY);
		}
		
		return sSyncAdapter;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return getSyncAdapter().getSyncAdapterBinder();
	}	
}
