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
        final int mode = intent.getIntExtra(Const.RUN_MODE, Const.MODE_START);
        Log.i(TAG, "onHandleIntent mode=" + mode);

        if (mode < Const.MODE_REFRESH) {
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
        } else if (mode == Const.MODE_REFRESH) {
            doRefreshAlarm();
        } else {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(1);
            cancelAlarm(true);
            cancelAlarm(false);

            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor edit = pref.edit();
            edit.putBoolean(Const.RUN_FLAG, false);
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
        int alarm_flag = pref.getInt(Const.ALARM_FLAG, 0);
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

    private boolean setAlarm(final boolean flag) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int hour, minute;
        if (flag) {
            hour = pref.getInt(Const.OFF_HOUR, 8);
            minute = pref.getInt(Const.OFF_MINUTE, 0);
        } else {
            hour = pref.getInt(Const.ON_HOUR, 17);
            minute = pref.getInt(Const.ON_MINUTE, 0);
        }

        Intent i = new Intent(this, MyService.class);
        i.putExtra(Const.RUN_MODE, flag ? Const.MODE_OFF : Const.MODE_ON);
        PendingIntent pi = PendingIntent.getService(this, flag ? 2 : 3, i, PendingIntent.FLAG_UPDATE_CURRENT);

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

    private boolean cancelAlarm(final boolean flag) {
        Intent i = new Intent(this, MyService.class);
        PendingIntent pi = PendingIntent.getService(this, flag ? 2 : 3, i, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(pi);
        return true;
    }

    private boolean doRefreshToken(final int mode) {
        Log.i(TAG, "doRefreshToken");
        Toast.makeText(this, R.string.toast_1, Toast.LENGTH_LONG).show();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString(Const.ACCESS_TOKEN, "");
        edit.putBoolean(Const.REFRESH_FLAG, true);
        edit.apply();
        Uri uri = Uri.parse("https://api.iijmio.jp/mobile/d/v1/authorization/?response_type=token&client_id=pZgayGOChl8Lm5ILZKy&state=" + mode + "&redirect_uri=com.unk2072.iijmiotoggle%3A%2F%2Fcallback");
        startActivity(new Intent(Intent.ACTION_VIEW, uri).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
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
                i.putExtra(Const.RUN_MODE, mode);
                startService(i);
            }
        }, 60000);
        return true;
    }
}
