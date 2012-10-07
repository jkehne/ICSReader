package de.int80.ics.reader;

import java.util.List;

import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.PeriodicSync;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class AccountPreferencesScreen extends Activity {

	private int calID;
	private int color;
	PeriodicSync sync;
	CalendarHandle calHandle;
	Account account;
	private static final String TAG = "AccountPreferencesScreen";
	private String CALENDAR_AUTHORITY;
	
	private static int intervalToIndex(long interval) {
		int index = 0;
		switch((int) interval) {
		case 0:
			index = 0;
			break;
		case 60:
			index = 1;
			break;
		case 120:
			index = 2;
			break;
		case 300:
			index = 3;
			break;
		case 600:
			index = 4;
			break;
		case 900:
			index = 5;
			break;
		case 1800:
			index = 6;
			break;
		case 2700:
			index = 7;
			break;
		case 3600:
			index = 8;
			break;
		}
		return index;
	}
	
	private static long indexToInterval(int index) {
		int interval = 0;
		switch(index) {
		case 0:
			interval = 0;
			break;
		case 1:
			interval = 60;
			break;
		case 2:
			interval = 120;
			break;
		case 3:
			interval = 300;
			break;
		case 4:
			interval = 600;
			break;
		case 5:
			interval = 900;
			break;
		case 6:
			interval = 1800;
			break;
		case 7:
			interval = 2700;
			break;
		case 8:
			interval = 3600;
			break;
		}
		return interval;
	}
	
	private void setAutomaticSync(Account account, int intervalIndex, Bundle extras) {
		long interval = indexToInterval(intervalIndex);

		List<PeriodicSync> syncsList = ContentResolver.getPeriodicSyncs(account, CALENDAR_AUTHORITY);
		for (PeriodicSync sync : syncsList)
			ContentResolver.removePeriodicSync(sync.account, sync.authority, sync.extras);
		
		if (interval > 0) {
			ContentResolver.addPeriodicSync(account, CALENDAR_AUTHORITY, extras, interval);
			ContentResolver.setSyncAutomatically(account, CALENDAR_AUTHORITY, true);
		} else
			ContentResolver.setSyncAutomatically(account, CALENDAR_AUTHORITY, false);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preferencesscreen);
		CALENDAR_AUTHORITY = getString(R.string.CALENDAR_AUTHORITY);
		
		account = getIntent().getExtras().getParcelable(getString(R.string.CALENDAR_ACCOUNT_KEY));
		AccountManager am = AccountManager.get(this);
		
		TextView calendarName = (TextView) findViewById(R.id.calendarNameView);
		calendarName.setText(account.name);
		
		calID = Integer.parseInt(am.getUserData(account, getString(R.string.CALENDAR_ID_KEY)));
		calHandle = new CalendarHandle(this, calID, account.name, getString(R.string.ACCOUNT_TYPE));
		
		EditText calendarUrl = (EditText) findViewById(R.id.calendarURLField);
		calendarUrl.setText(am.getUserData(account, getString(R.string.URL_KEY)));
		
		EditText username = (EditText) findViewById(R.id.userNameField);
		username.setText(am.getUserData(account, getString(R.string.USERNAME_KEY)));
		
		EditText password = (EditText) findViewById(R.id.passwordField);
		password.setText(am.getPassword(account));
		
		Spinner syncInterval = (Spinner) findViewById(R.id.syncIntervalField);
		List<PeriodicSync> syncsList = ContentResolver.getPeriodicSyncs(account, CALENDAR_AUTHORITY);
		sync = syncsList.size() > 0 ? syncsList.get(0) : new PeriodicSync(account, CALENDAR_AUTHORITY, new Bundle(), 0);
		syncInterval.setSelection(intervalToIndex(sync.period));
		
		color = calHandle.getColor();
		Button colorSelector = (Button) findViewById(R.id.colorPickerButton);
		colorSelector.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// initialColor is the initially-selected color to be shown 
			    // in the rectangle on the left of the arrow.
			    // for example, 0xff000000 is black, 0xff0000ff is blue. 
			    // Please be aware of the initial 0xff which is the alpha.
			    AmbilWarnaDialog dialog = new AmbilWarnaDialog(AccountPreferencesScreen.this, color,
			            new OnAmbilWarnaListener() {
			        @Override
			        public void onOk(AmbilWarnaDialog dialog, int color) {
			                // color is the color selected by the user.
			        	AccountPreferencesScreen.this.color = color;
			        }
			                
			        @Override
			        public void onCancel(AmbilWarnaDialog dialog) {
			                // cancel was selected by the user, ignore
			        }
			    });

			    dialog.show();		
			}
		});
		
		Button okButton = (Button) findViewById(R.id.okButton);
		okButton.setOnClickListener(new OnClickListener() {
			
			
			@Override
			public void onClick(View v) {
				EditText username = (EditText) findViewById(R.id.userNameField);
				EditText password = (EditText) findViewById(R.id.passwordField);
				EditText url = (EditText) findViewById(R.id.calendarURLField);
				Spinner syncInterval = (Spinner) findViewById(R.id.syncIntervalField);

				if (! CalendarHandle.checkCredentials(
						AccountPreferencesScreen.this, 
						url.getText().toString(), 
						username.getText().toString(), 
						password.getText().toString()))
					return;
				
				AccountManager am = AccountManager.get(AccountPreferencesScreen.this);
				
				am.setUserData(account, getString(R.string.USERNAME_KEY), username.getText().toString());
				am.setUserData(account, getString(R.string.URL_KEY), url.getText().toString());
				am.setPassword(account, password.getText().toString());
				calHandle.setColor(color);
				setAutomaticSync(account, syncInterval.getSelectedItemPosition(), sync.extras);
								
				finish();
			}
		});
		
		Button cancelButton = (Button) findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});

	}
}
