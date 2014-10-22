package com.unk2072.iijmiotoggle;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class AsyncHttpRequest extends AsyncTask<Integer, Integer, Integer>  {
    private static final String TAG = "AsyncHttpRequest";
    private MyService my_service;

    public AsyncHttpRequest(MyService my) {
        super();
        my_service = my;
    }

    @Override
    protected Integer doInBackground(Integer... values) {
        int mode = values[0];
        int volume;
        boolean couponUse;
        String hdoServiceCode;

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(my_service);
        String token = pref.getString(Const.ACCESS_TOKEN, "");
        Log.i(TAG, "doInBackground token=" + token);

        HttpURLConnection http = null;
        try {
            URL url = new URL("https://api.iijmio.jp/mobile/d/v1/coupon/");
            http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("GET");
            http.setRequestProperty("X-IIJmio-Developer", "pZgayGOChl8Lm5ILZKy");
            http.setRequestProperty("X-IIJmio-Authorization", token);
            http.connect();
            int status = http.getResponseCode();
            if (status != 200) {
                return status;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            in.close();

            final String json = new String(sb);
            Log.i(TAG, "get json=" + json);
            JSONObject root = new JSONObject(json);
            JSONObject couponInfo = root.getJSONArray("couponInfo").getJSONObject(0);
            JSONObject hdoInfo = couponInfo.getJSONArray("hdoInfo").getJSONObject(0);
            volume = hdoInfo.getJSONArray("coupon").getJSONObject(0).getInt("volume");
            couponUse = hdoInfo.getBoolean("couponUse");
            hdoServiceCode = hdoInfo.getString("hdoServiceCode");
            JSONArray couponArray = couponInfo.getJSONArray("coupon");
            for (int i = 0; i < couponArray.length(); i++) {
                volume += couponArray.getJSONObject(i).getInt("volume");
            }
            Log.i(TAG, "couponUse=" + couponUse + ", volume=" + volume);
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        } catch (JSONException e) {
            e.printStackTrace();
            return 1;
        } finally {
            if (http != null) http.disconnect();
        }

        if (mode != 0) {
            Boolean couponUse_new = mode != 1;
            if (couponUse != couponUse_new) {
                http = null;
                try {
                    URL url = new URL("https://api.iijmio.jp/mobile/d/v1/coupon/");
                    http = (HttpURLConnection) url.openConnection();
                    http.setRequestMethod("PUT");
                    http.setDoOutput(true);
                    http.setRequestProperty("X-IIJmio-Developer", "pZgayGOChl8Lm5ILZKy");
                    http.setRequestProperty("X-IIJmio-Authorization", token);
                    http.setRequestProperty("Content-Type", "application/json");

                    final String json = "{\"couponInfo\": [{\"hdoInfo\": [{\"hdoServiceCode\": \"" + hdoServiceCode + "\", \"couponUse\": " + couponUse_new +" }] }] }";
                    Log.i(TAG, "put json=" + json);
                    OutputStreamWriter out = new OutputStreamWriter(http.getOutputStream());
                    out.write(json);
                    out.close();
                    int status = http.getResponseCode();
                    if (status != 200) {
                        return status;
                    }
                    couponUse = couponUse_new;
                } catch (IOException e) {
                    e.printStackTrace();
                    return 1;
                } finally {
                    if (http != null) http.disconnect();
                }
            }
        }

        Intent i = new Intent(my_service, MyService.class);
        i.putExtra(Const.RUN_MODE, couponUse ? Const.MODE_OFF : Const.MODE_ON);
        PendingIntent pi = PendingIntent.getService(my_service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new Notification.Builder(my_service)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(couponUse ? R.drawable.ic_stat_on : R.drawable.ic_stat_off)
                .setContentTitle(my_service.getString(R.string.notify_title, volume))
                .setContentText(my_service.getString(R.string.notify_text))
                .setContentIntent(pi)
                .setOngoing(true)
                .setAutoCancel(false)
                .build();

        NotificationManager nm = (NotificationManager) my_service.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(1, n);

        i = new Intent(my_service, MyService.class);
        i.putExtra(Const.RUN_MODE, Const.MODE_START);
        pi = PendingIntent.getService(my_service, 1, i, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) my_service.getSystemService(Context.ALARM_SERVICE);
        if (couponUse) {
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (120 * 60000), pi);
        } else {
            am.cancel(pi);
        }

        SharedPreferences.Editor edit = pref.edit();
        edit.putBoolean(Const.RUN_FLAG, true);
        edit.apply();
        return 0;
    }
}
