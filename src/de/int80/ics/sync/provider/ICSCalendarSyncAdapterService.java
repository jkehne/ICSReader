package de.int80.ics.sync.provider;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
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

		public ICSCalendarSyncAdapterImpl(Context context) {
			super(context, true);
			mContext = context;
		}

		@Override
		public void onPerformSync(Account account, Bundle extras,
				String authority, ContentProviderClient provider,
				SyncResult syncResult) {
			Log.i(TAG, "performSync: " + account.toString());
			String calendarURL = AccountManager.get(ICSCalendarSyncAdapterService.this).getUserData(account, CALENDAR_URL_KEY);
			Log.i(TAG, "Calendar URL is " + calendarURL);
			//This is where the magic will happen!
		}

	}

	private static final String TAG = "ICSCalendarSyncAdapterService";
	private static String CALENDAR_URL_KEY;
	private static ICSCalendarSyncAdapterImpl sSyncAdapter;

	public ICSCalendarSyncAdapterService() {
		super();
	}

	private ICSCalendarSyncAdapterImpl getSyncAdapter() {
		if (sSyncAdapter == null) {
			sSyncAdapter = new ICSCalendarSyncAdapterImpl(this);
			CALENDAR_URL_KEY = getString(R.string.URL_KEY);
		}
		
		return sSyncAdapter;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return getSyncAdapter().getSyncAdapterBinder();
	}	
}
