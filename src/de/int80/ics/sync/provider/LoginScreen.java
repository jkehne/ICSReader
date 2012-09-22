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

public class LoginScreen extends AccountAuthenticatorActivity {

	public LoginScreen() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.loginscreen);
		
		Button okButton = (Button) findViewById(R.id.okButton);
		okButton.setOnClickListener(new OnClickListener() {
			
			
			@Override
			public void onClick(View v) {
				EditText username = (EditText) findViewById(R.id.userNameField);
				EditText password = (EditText) findViewById(R.id.passwordField);
				EditText url = (EditText) findViewById(R.id.calendarURLField);

				Account account = new Account(username.getText().toString(), getString(R.string.ACCOUNT_TYPE));
				AccountManager am = AccountManager.get(LoginScreen.this);
				
				Bundle extras = new Bundle();
				extras.putString(getString(R.string.URL_KEY), url.getText().toString());
				
				am.addAccountExplicitly(account, password.getText().toString(), extras);

				ContentResolver.addPeriodicSync(account, "com.android.calendar", extras, 1800);
				
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
