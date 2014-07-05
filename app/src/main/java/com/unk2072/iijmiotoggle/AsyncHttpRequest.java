package com.unk2072.iijmiotoggle;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class AsyncHttpRequest extends AsyncTask<Integer, Integer, Integer>  {
    private static final String TAG = "AsyncHttpRequest";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String RUN_FLAG = "run_flag";
    private static final String RUN_MODE = "run_mode";
    private MyService my_service;

    public AsyncHttpRequest(MyService my) {
        super();
        my_service = my;
    }

    @Override
    protected Integer doInBackground(Integer... values) {
        int mode = values[0];
        int volume = 0;
        boolean couponUse = false;
        String hdoServiceCode = "";

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(my_service);
        String token = pref.getString(ACCESS_TOKEN, "");
        Log.i(TAG, "doInBackground token=" + token);

        try {
            HttpGet method = new HttpGet("https://api.iijmio.jp/mobile/d/v1/coupon/");
            method.setHeader("X-IIJmio-Developer", "pZgayGOChl8Lm5ILZKy");
            method.setHeader("X-IIJmio-Authorization", token);
            DefaultHttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(method);
            if (response == null) {
                return 1;
            }
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                final String json = EntityUtils.toString(response.getEntity());
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
            } else {
                return status;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (mode != 0) {
            Boolean couponUse_new = mode != 1;
            if (couponUse != couponUse_new) {
                try {
                    HttpPut method = new HttpPut("https://api.iijmio.jp/mobile/d/v1/coupon/");
                    method.setHeader("X-IIJmio-Developer", "pZgayGOChl8Lm5ILZKy");
                    method.setHeader("X-IIJmio-Authorization", token);
                    method.setHeader("Content-Type", "application/json");
                    final String json = "{\"couponInfo\": [{\"hdoInfo\": [{\"hdoServiceCode\": \"" + hdoServiceCode + "\", \"couponUse\": " + couponUse_new +" }] }] }";
                    method.setEntity(new StringEntity(json));
                    Log.i(TAG, "put json=" + json);
                    DefaultHttpClient client = new DefaultHttpClient();
                    HttpResponse response = client.execute(method);
                    if (response == null) {
                        return 1;
                    }
                    int status = response.getStatusLine().getStatusCode();
                    if (status == 200) {
                        couponUse = couponUse_new;
                    } else {
                        return status;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Intent i = new Intent(my_service, MyService.class);
        i.putExtra(RUN_MODE, couponUse ? 1 : 2);
        PendingIntent pi = PendingIntent.getService(my_service, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new NotificationCompat.Builder(my_service)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(couponUse ? R.drawable.ic_stat_on : R.drawable.ic_stat_off)
                .setContentTitle(my_service.getString(R.string.notify_title, volume))
                .setContentText(my_service.getString(R.string.notify_text))
                .setContentIntent(pi)
                .setOngoing(true)
                .setAutoCancel(false)
                .build();

        NotificationManager nm = (NotificationManager)my_service.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(1, n);

        SharedPreferences.Editor edit = pref.edit();
        edit.putBoolean(RUN_FLAG, true);
        edit.commit();
        return 0;
    }
}
