package de.int80.ics.reader;

import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class AccountLister extends ListActivity {

	private ArrayList<Account> accountList;
	
	public AccountLister() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AccountManager am = AccountManager.get(this);
		Account[] accounts = am.getAccountsByType(getString(R.string.ACCOUNT_TYPE));
		ArrayAdapter<String> accountAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		accountList = new ArrayList<Account>();
		
		for (Account account : accounts) {
			accountAdapter.add(account.name);
			accountList.add(account);
		}
		
		setListAdapter(accountAdapter);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent intent = new Intent();
		intent.setClass(this, AccountPreferencesScreen.class);
		intent.putExtra(getString(R.string.CALENDAR_ACCOUNT_KEY), accountList.get(position));
		startActivity(intent);
		finish();
	}
}
