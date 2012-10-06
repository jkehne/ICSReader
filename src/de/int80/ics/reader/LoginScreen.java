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

import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class LoginScreen extends AccountAuthenticatorActivity {

	private int color = 0;
	
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
		if (interval > 0) {
			ContentResolver.addPeriodicSync(account, "com.android.calendar", extras, interval);
			ContentResolver.setSyncAutomatically(account, "com.android.calendar", true);
		}
	}

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.loginscreen);
		
		findViewById(R.id.calendarNameField).requestFocus(View.FOCUS_DOWN);
		
		Button okButton = (Button) findViewById(R.id.okButton);
		okButton.setOnClickListener(new OnClickListener() {
			
			
			@Override
			public void onClick(View v) {
				EditText calendarName = (EditText) findViewById(R.id.calendarNameField);
				EditText username = (EditText) findViewById(R.id.userNameField);
				EditText password = (EditText) findViewById(R.id.passwordField);
				EditText url = (EditText) findViewById(R.id.calendarURLField);
				Spinner syncInterval = (Spinner) findViewById(R.id.syncIntervalField);

				if (! CalendarHandle.checkCredentials(
						LoginScreen.this, 
						url.getText().toString(), 
						username.getText().toString(), 
						password.getText().toString()))
					return;
				
				Account account = new Account(calendarName.getText().toString(), getString(R.string.ACCOUNT_TYPE));
				AccountManager am = AccountManager.get(LoginScreen.this);
				
				CalendarHandle handle = new CalendarHandle(
						LoginScreen.this, 
						calendarName.getText().toString(), 
						getString(R.string.ACCOUNT_TYPE), 
						color);
				
				Bundle extras = new Bundle();
				extras.putString(getString(R.string.USERNAME_KEY), username.getText().toString());
				extras.putString(getString(R.string.URL_KEY), url.getText().toString());
				extras.putString(getString(R.string.CALENDAR_ID_KEY), String.valueOf(handle.getCalID()));
				
				am.addAccountExplicitly(account, password.getText().toString(), extras);

				ContentResolver.setIsSyncable(account, "com.android.calendar", 1);
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
		
		Button colorSelector = (Button) findViewById(R.id.colorPickerButton);
		colorSelector.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// initialColor is the initially-selected color to be shown 
			    // in the rectangle on the left of the arrow.
			    // for example, 0xff000000 is black, 0xff0000ff is blue. 
			    // Please be aware of the initial 0xff which is the alpha.
			    AmbilWarnaDialog dialog = new AmbilWarnaDialog(LoginScreen.this, 0xff000000,
			            new OnAmbilWarnaListener() {
			        @Override
			        public void onOk(AmbilWarnaDialog dialog, int color) {
			                // color is the color selected by the user.
			        	LoginScreen.this.color = color;
			        }
			                
			        @Override
			        public void onCancel(AmbilWarnaDialog dialog) {
			                // cancel was selected by the user, ignore
			        }
			    });

			    dialog.show();		
			}
		});
	}
}
