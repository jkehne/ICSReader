package de.int80.ics.sync.provider;

import android.accounts.Account;
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

	private static class ICSCalendarSyncAdapterImpl extends
			AbstractThreadedSyncAdapter {

		private Context mContext;

		public ICSCalendarSyncAdapterImpl(Context context) {
			super(context, true);
			mContext = context;
		}

		@Override
		public void onPerformSync(Account account, Bundle extras,
				String authority, ContentProviderClient provider,
				SyncResult syncResult) {
			try {
				ICSCalendarSyncAdapterService.performSync(mContext, account, extras, authority, provider, syncResult);
			} catch (OperationCanceledException e) {
				//NOP
			}
		}

	}

	private static final String TAG = "ICSCalendarSyncAdapterService";
	private static ContentResolver mContentResolver;
	private static ICSCalendarSyncAdapterImpl sSyncAdapter;

	public ICSCalendarSyncAdapterService() {
		super();
	}

	private ICSCalendarSyncAdapterImpl getSyncAdapter() {
		if (sSyncAdapter == null)
			sSyncAdapter = new ICSCalendarSyncAdapterImpl(this);
		return sSyncAdapter;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return getSyncAdapter().getSyncAdapterBinder();
	}
	
	private static void performSync(Context context, Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult)
			throws OperationCanceledException {
		mContentResolver = context.getContentResolver();
		Log.i(TAG, "performSync: " + account.toString());
		//This is where the magic will happen!
	}
}
