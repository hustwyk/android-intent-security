package com.android.intentfuzzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.android.intentfuzzer.auto.AutoTestManager;
import com.android.intentfuzzer.util.SerializableTest;
import com.android.intentfuzzer.util.Utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

public class FuzzerActivity extends Activity {

	private static final int DELAY_BATCH_SEND = 2000;
	
	private static final int MSG_SEND = 1;
	
	private ArrayList<String> cmpTypes = new ArrayList<String>();
	private String currentType = null;
	private Spinner typeSpinner = null;
	private ListView cmpListView = null;
	private Button fuzzAllNullBtn = null;
	private Button fuzzAllSeBtn = null;

	private ArrayAdapter<String> cmpAdapter = null;

	private ArrayList<String> cmpNames = new ArrayList<String>();
	private ArrayList<ComponentName> components = new ArrayList<ComponentName>();
	private PackageInfo pkgInfo = null;

	private static Map<Integer, String> ipcTypesToNames = new TreeMap<Integer, String>();
	private static Map<String, Integer> ipcNamesToTypes = new HashMap<String, Integer>();

	private Handler mMainHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_SEND:
				int curIndex = (Integer) msg.obj;
				Intent intent = new Intent();
				ComponentName componentName = components.get(curIndex);
				intent.setComponent(componentName);
				
				if (sendIntentByType(intent, currentType)) {
					Toast.makeText(FuzzerActivity.this,
							"Sent Null " + intent, Toast.LENGTH_LONG)
							.show();
				} else {
					Toast.makeText(FuzzerActivity.this,
							R.string.send_faild, Toast.LENGTH_LONG).show();
				}
				
				curIndex++;
				if (curIndex >= components.size()) {
					Log.d("FuzzerActivity", "send finish");
				} else {
					Message obtainMessage = mMainHandler.obtainMessage(MSG_SEND, curIndex);
					mMainHandler.sendMessageDelayed(obtainMessage, DELAY_BATCH_SEND);
				}
				
				break;

			default:
				break;
			}
		};
	};

	static {
		ipcTypesToNames.put(Utils.ACTIVITIES, "Activities");
		ipcTypesToNames.put(Utils.RECEIVERS, "Receivers");
		ipcTypesToNames.put(Utils.SERVICES, "Services");

		for (Entry<Integer, String> entry : ipcTypesToNames.entrySet()) {
			ipcNamesToTypes.put(entry.getValue(), entry.getKey());
		}
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fuzzer);

		for (String name : ipcTypesToNames.values())
			cmpTypes.add(name);
		currentType = cmpTypes.get(0);

		initView();
		initTypeSpinner();

		// pkgInfo = getPkgInfo();
		pkgInfo = ((MyApp) getApplication()).packageInfo;
		if (pkgInfo == null) {
			Toast.makeText(this, R.string.pkginfo_null, Toast.LENGTH_LONG)
					.show();
			return;
		}
	}

	private PackageInfo getPkgInfo() {
		PackageInfo pkgInfo = null;

		Intent intent = getIntent();
		if (intent.hasExtra(Utils.PKGINFO_KEY)) {
			pkgInfo = intent.getParcelableExtra(Utils.PKGINFO_KEY);
		}

		return pkgInfo;
	}

	private void initView() {
		typeSpinner = (Spinner) findViewById(R.id.type_select);
		cmpListView = (ListView) findViewById(R.id.cmp_listview);
		fuzzAllNullBtn = (Button) findViewById(R.id.fuzz_all_null);
		fuzzAllSeBtn = (Button) findViewById(R.id.fuzz_all_se);

		cmpListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ComponentName toSend = null;
				Intent intent = new Intent();
				String className = cmpAdapter.getItem(position).toString();
				for (ComponentName cmpName : components) {
					if (cmpName.getClassName().equals(className)) {
						toSend = cmpName;
						break;
					}
				}

				intent.setComponent(toSend);

				if (sendIntentByType(intent, currentType)) {
					Toast.makeText(FuzzerActivity.this, "Sent Null " + intent,
							Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(FuzzerActivity.this,
							"Send " + intent + " Failed!", Toast.LENGTH_LONG)
							.show();
				}
			}

		});

		cmpListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				ComponentName toSend = null;
				Intent intent = new Intent();
				String className = cmpAdapter.getItem(position).toString();
				for (ComponentName cmpName : components) {
					if (cmpName.getClassName().equals(className)) {
						toSend = cmpName;
						break;
					}
				}

				intent.setComponent(toSend);
				intent.putExtra("test", new SerializableTest());

				if (sendIntentByType(intent, currentType)) {
					Toast.makeText(FuzzerActivity.this,
							"Sent Serializeable " + intent, Toast.LENGTH_LONG)
							.show();
				} else {
					Toast.makeText(FuzzerActivity.this,
							"Send " + intent + " Failed!", Toast.LENGTH_LONG)
							.show();
				}
				return true;
			}

		});

		fuzzAllNullBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				for (ComponentName cmpName : components) {
				}
				if (components == null || components.isEmpty()) {
					return;
				}
				
				Message obtainMessage = mMainHandler.obtainMessage(MSG_SEND, 0);
				mMainHandler.sendMessage(obtainMessage);
			}
		});

		fuzzAllSeBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				for (ComponentName cmpName : components) {
					Intent intent = new Intent();
					intent.setComponent(cmpName);
					intent.putExtra("test", new SerializableTest());
					if (sendIntentByType(intent, currentType)) {
						Toast.makeText(FuzzerActivity.this,
								"Sent Serializeable " + intent,
								Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(FuzzerActivity.this,
								R.string.send_faild, Toast.LENGTH_LONG).show();
					}
				}
			}

		});
	}

	private void initTypeSpinner() {
		ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, cmpTypes);
		typeAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		typeSpinner.setAdapter(typeAdapter);

		typeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				updateType();
				updateComponents(currentType);
			}

			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	private void updateType() {
		Object selector = typeSpinner.getSelectedItem();
		if (selector != null) {
			currentType = typeSpinner.getSelectedItem().toString();
		}
	}

	private void updateComponents(String currentType) {
		fuzzAllNullBtn.setVisibility(View.INVISIBLE);
		fuzzAllSeBtn.setVisibility(View.INVISIBLE);
		components = getComponents(currentType);
		cmpNames.clear();
		if (!components.isEmpty()) {
			for (ComponentName cmpName : components) {
				if (!cmpNames.contains(cmpName.getClassName())) {
					cmpNames.add(cmpName.getClassName());
				}
			}

			fuzzAllNullBtn.setVisibility(View.VISIBLE);
			fuzzAllSeBtn.setVisibility(View.VISIBLE);

		} else {
			Toast.makeText(this, R.string.no_compt, Toast.LENGTH_LONG).show();
		}
		setListView();
	}

	private ArrayList<ComponentName> getComponents(String currentType) {
		PackageItemInfo items[] = null;
		ArrayList<ComponentName> cmpsFound = new ArrayList<ComponentName>();
		switch (ipcNamesToTypes.get(currentType)) {
		case Utils.ACTIVITIES:
			items = pkgInfo.activities;
			break;
		case Utils.RECEIVERS:
			items = pkgInfo.receivers;
			break;
		case Utils.SERVICES:
			items = pkgInfo.services;
			break;
		default:
			items = pkgInfo.activities;
			break;
		}

		if (items != null) {
			for (PackageItemInfo pkgItemInfo : items) {
				cmpsFound.add(new ComponentName(pkgInfo.packageName,
						pkgItemInfo.name));
			}
		}

		return cmpsFound;
	}

	private void setListView() {
		cmpAdapter = new ArrayAdapter<String>(this, R.layout.component,
				cmpNames);
		cmpListView.setAdapter(cmpAdapter);
	}

	private boolean sendIntentByType(Intent intent, String type) {
		try {
			switch (ipcNamesToTypes.get(type)) {
			case Utils.ACTIVITIES:
				AutoTestManager.getInstance().send(AutoTestManager.SEND_TYPE_ACTIVITY, intent);
				return true;
			case Utils.RECEIVERS:
				sendBroadcast(intent);
				return true;
			case Utils.SERVICES:
				startService(intent);
				return true;
			default:
				return true;
			}
		} catch (Exception e) {
			// e.printStackTrace();
			return false;
		}

	}

}