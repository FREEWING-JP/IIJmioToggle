package com.unk2072.iijmiotoggle;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class AsyncHttpRequest extends AsyncTask<String, Void, Integer>  {
    private static final String TAG = "AsyncHttpRequest";
    private MyService mService;
    int mMode;
    int mVolume;
    boolean mCouponUse;

    public AsyncHttpRequest(MyService my, int mode) {
        super();
        mService = my;
        mMode = mode;
    }

    @Override
    protected Integer doInBackground(String... values) {
        String hdoServiceCode;
        String token = values[0];
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

            InputStream in = http.getInputStream();
            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[1024];
            int size;
            while ((size = in.read(buf)) >= 0) {
                sb.append(new String(buf, 0, size));
            }
            in.close();

            final String json = new String(sb);
            Log.i(TAG, "get json=" + json);
            JSONObject root = new JSONObject(json);
            JSONObject couponInfo = root.getJSONArray("couponInfo").getJSONObject(0);
            JSONObject hdoInfo = couponInfo.getJSONArray("hdoInfo").getJSONObject(0);
            mVolume = hdoInfo.getJSONArray("coupon").getJSONObject(0).getInt("volume");
            mCouponUse = hdoInfo.getBoolean("couponUse");
            hdoServiceCode = hdoInfo.getString("hdoServiceCode");
            JSONArray couponArray = couponInfo.getJSONArray("coupon");
            for (int i = 0; i < couponArray.length(); i++) {
                mVolume += couponArray.getJSONObject(i).getInt("volume");
            }
            Log.i(TAG, "couponUse=" + mCouponUse + ", volume=" + mVolume);
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        } catch (JSONException e) {
            e.printStackTrace();
            return 1;
        } finally {
            if (http != null) http.disconnect();
        }

        if (mMode != 0) {
            Boolean couponUseNew = mMode != 1;
            if (mCouponUse != couponUseNew) {
                http = null;
                try {
                    URL url = new URL("https://api.iijmio.jp/mobile/d/v1/coupon/");
                    http = (HttpURLConnection) url.openConnection();
                    http.setRequestMethod("PUT");
                    http.setDoOutput(true);
                    http.setRequestProperty("X-IIJmio-Developer", "pZgayGOChl8Lm5ILZKy");
                    http.setRequestProperty("X-IIJmio-Authorization", token);
                    http.setRequestProperty("Content-Type", "application/json");

                    final String json = "{\"couponInfo\": [{\"hdoInfo\": [{\"hdoServiceCode\": \"" + hdoServiceCode + "\", \"couponUse\": " + couponUseNew +" }] }] }";
                    Log.i(TAG, "put json=" + json);
                    OutputStreamWriter out = new OutputStreamWriter(http.getOutputStream());
                    out.write(json);
                    out.close();
                    int status = http.getResponseCode();
                    if (status != 200) {
                        return status;
                    }
                    mCouponUse = couponUseNew;
                } catch (IOException e) {
                    e.printStackTrace();
                    return 1;
                } finally {
                    if (http != null) http.disconnect();
                }
            }
        }
        return 0;
    }

    @Override
    protected void onPostExecute(Integer result) {
        switch (result) {
            case 0:
                mService.doStartService(mVolume, mCouponUse);
                break;
            case 403:
                mService.doRefreshToken(mMode);
                break;
            case 429:
                mService.doRetry(mMode);
                break;
            default:
                mService.doError(result);
                break;
        }
    }
}
