package com.android.intentfuzzer.auto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.android.intentfuzzer.fuzz.BasicFuzzIntent;

public class AutoActivitySender implements AutoSender<Intent> {
	
	private Activity mActivityContext;
	
	private Handler mMainHandler;
	
	private static final int DELAY_ACTIIVTY_FINISH = 1000;
	
	public AutoActivitySender(Context context, Handler mainHandler) {
		this.mActivityContext = (Activity) context;
		this.mMainHandler = mainHandler;
	}
	

	@Override
	public void send(final Intent fuzzIntent) {
		mMainHandler.post(new Runnable() {
			@Override
			public void run() {
				try {
					mActivityContext.startActivityForResult(fuzzIntent, Constants.REQUEST_CODE_ACTIIVTY, null);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		mMainHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				try {
					mActivityContext.finishActivity(Constants.REQUEST_CODE_ACTIIVTY);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}, DELAY_ACTIIVTY_FINISH);
		
	}

}