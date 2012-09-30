package de.int80.ics.sync.provider;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class LoginScreen extends AccountAuthenticatorActivity {

	public LoginScreen() {
		// TODO Auto-generated constructor stub
	}
	
	private void setAutomaticSync(Account account, int intervalIndex, Bundle extras) {
		int interval = 0;
		switch(intervalIndex) {
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
		if (interval > 0)
			ContentResolver.addPeriodicSync(account, "com.android.calendar", extras, interval);
	}

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.loginscreen);
		
		Button okButton = (Button) findViewById(R.id.okButton);
		okButton.setOnClickListener(new OnClickListener() {
			
			
			@Override
			public void onClick(View v) {
				EditText calendarName = (EditText) findViewById(R.id.calendarNameField);
				EditText username = (EditText) findViewById(R.id.userNameField);
				EditText password = (EditText) findViewById(R.id.passwordField);
				EditText url = (EditText) findViewById(R.id.calendarURLField);
				Spinner syncInterval = (Spinner) findViewById(R.id.syncIntervalField);

				Account account = new Account(calendarName.getText().toString(), getString(R.string.ACCOUNT_TYPE));
				AccountManager am = AccountManager.get(LoginScreen.this);
				
				CalendarHandle handle = new CalendarHandle(
						LoginScreen.this, 
						calendarName.getText().toString(), 
						getString(R.string.ACCOUNT_TYPE));
				
				Bundle extras = new Bundle();
				extras.putString(getString(R.string.USERNAME_KEY), username.getText().toString());
				extras.putString(getString(R.string.URL_KEY), url.getText().toString());
				extras.putLong(getString(R.string.CALENDAR_ID_KEY), handle.getCalID());
				
				am.addAccountExplicitly(account, password.getText().toString(), extras);

				setAutomaticSync(account, syncInterval.getSelectedItemPosition(), extras);
				
				Bundle result = new Bundle();
				result.putString(AccountManager.KEY_ACCOUNT_NAME, username.getText().toString());
				result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.ACCOUNT_TYPE));
				setAccountAuthenticatorResult(result);
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
