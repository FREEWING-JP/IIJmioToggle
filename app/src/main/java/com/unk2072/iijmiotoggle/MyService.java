package com.unk2072.iijmiotoggle;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;
import java.util.TimeZone;

public class MyService extends IntentService {
    private static final String TAG = "MyService";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String RUN_FLAG = "run_flag";
    private static final String ALARM_FLAG = "alarm_flag";
    private static final String OFF_HOUR = "off_hour";
    private static final String OFF_MINUTE = "off_minute";
    private static final String ON_HOUR = "on_hour";
    private static final String ON_MINUTE = "on_minute";
    private static final String RUN_MODE = "run_mode";
    private boolean retry_flag = false;

    public MyService(){
        super(TAG);
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final int mode = intent.getIntExtra(RUN_MODE, 0);
        Log.i(TAG, "onStartCommand mode=" + mode);

        if (mode < 100) {
            doRefreshAlarm();
            new AsyncHttpRequest(this) {
                @Override
                protected void onPostExecute(Integer result) {
                    switch (result) {
                        case 0:
                            retry_flag = false;
                            break;
                        case 403:
                            doRefreshToken(mode);
                            break;
                        case 429:
                            doRetry(mode);
                            break;
                        default:
                            Log.d(TAG, "onPostExecute result=" + result);
                            break;
                    }
                }
            }.execute(mode);
        } else if (mode == 100) {
            doRefreshAlarm();
        } else {
            NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(1);
            cancelAlarm(true);
            cancelAlarm(false);

            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor edit = pref.edit();
            edit.putBoolean(RUN_FLAG, false);
            edit.apply();
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    private boolean doRefreshAlarm() {
        Log.i(TAG, "doRefreshAlarm");
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int alarm_flag = pref.getInt(ALARM_FLAG, 0);
        switch (alarm_flag) {
            case 0:
                cancelAlarm(true);
                cancelAlarm(false);
                break;
            case 1:
                setAlarm(true);
                setAlarm(false);
                break;
            case 2:
                setAlarm(true);
                cancelAlarm(false);
                break;
            case 3:
                cancelAlarm(true);
                setAlarm(false);
                break;
        }
        return true;
    }

    private boolean setAlarm(boolean flag) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int hour, minute;
        if (flag) {
            hour = pref.getInt(OFF_HOUR, 8);
            minute = pref.getInt(OFF_MINUTE, 0);
        } else {
            hour = pref.getInt(ON_HOUR, 17);
            minute = pref.getInt(ON_MINUTE, 0);
        }

        Intent i = new Intent(this, MyService.class);
        i.putExtra(RUN_MODE, flag ? 1 : 2);
        PendingIntent pi = PendingIntent.getService(this, flag ? 1 : 2, i, PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.setTimeZone(TimeZone.getDefault());
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);

        if (System.currentTimeMillis() > cal.getTimeInMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
        return true;
    }

    private boolean cancelAlarm(boolean flag) {
        Intent i = new Intent(this, MyService.class);
        PendingIntent pi = PendingIntent.getService(this, flag ? 1 : 2, i, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(pi);
        return true;
    }

    private boolean doRefreshToken(final int mode) {
        Log.i(TAG, "doRefreshToken");
        Toast.makeText(this, R.string.toast_1, Toast.LENGTH_LONG).show();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString(ACCESS_TOKEN, "");
        edit.apply();
        Uri uri = Uri.parse("https://api.iijmio.jp/mobile/d/v1/authorization/?response_type=token&client_id=pZgayGOChl8Lm5ILZKy&state=" + mode + "&redirect_uri=com.unk2072.iijmiotoggle%3A%2F%2Fcallback");
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
        return true;
    }

    private boolean doRetry(final int mode) {
        Log.i(TAG, "doRetry");
        if (retry_flag || mode == 0) return false;
        Toast.makeText(this, R.string.toast_2, Toast.LENGTH_LONG).show();
        retry_flag = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(MyService.this, MyService.class);
                i.putExtra(RUN_MODE, mode);
                startService(i);
            }
        }, 60000);
        return true;
    }
}
