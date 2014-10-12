package com.unk2072.iijmiotoggle;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

public class MyActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {
    private static final String TAG = "MyActivity";
    private String[] mListText = new String[4];
    private ArrayAdapter<String> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        initListView();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String token = pref.getString(Const.ACCESS_TOKEN, "");

        if (token.equals("")) {
            Uri uri = Uri.parse("https://api.iijmio.jp/mobile/d/v1/authorization/?response_type=token&client_id=pZgayGOChl8Lm5ILZKy&state=0&redirect_uri=com.unk2072.iijmiotoggle%3A%2F%2Fcallback");
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (action == null || !action.equals(Intent.ACTION_VIEW)) {
            Log.e(TAG, "onNewIntent getAction");
            super.onNewIntent(intent);
            return;
        }

        String data = intent.getDataString();
        if (data == null) {
            Log.e(TAG, "getDataString");
            super.onNewIntent(intent);
            return;
        }
        Log.i(TAG, "onNewIntent data=" + data);

        data = data.replace("#", "?");
        String token = Uri.parse(data).getQueryParameter("access_token");
        if (token == null) {
            Log.e(TAG, "onNewIntent getQueryParameter");
            super.onNewIntent(intent);
            return;
        }
        Log.i(TAG, "onNewIntent token=" + token);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString(Const.ACCESS_TOKEN, token);
        edit.apply();

        boolean refresh_flag = pref.getBoolean(Const.REFRESH_FLAG, false);
        if (refresh_flag) {
            edit.putBoolean(Const.REFRESH_FLAG, false);
            edit.apply();
            String state = Uri.parse(data).getQueryParameter("state");
            Log.i(TAG, "onNewIntent state=" + state);
            int mode = Integer.valueOf(state);
            Intent i = new Intent(this, MyService.class);
            i.putExtra(Const.RUN_MODE, mode);
            startService(i);
        }
        super.onNewIntent(intent);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return false;
    }

    private boolean initListView() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean run_flag = pref.getBoolean(Const.RUN_FLAG, false);
        int alarm_flag = pref.getInt(Const.ALARM_FLAG, 0);
        int off_hour = pref.getInt(Const.OFF_HOUR, 8);
        int off_minute = pref.getInt(Const.OFF_MINUTE, 0);
        int on_hour = pref.getInt(Const.ON_HOUR, 17);
        int on_minute = pref.getInt(Const.ON_MINUTE, 0);

        mListText[0] = run_flag ? getString(R.string.list1_1) : getString(R.string.list1_0);
        mListText[1] = getString(R.string.list2_0, getResources().getStringArray(R.array.select_array)[alarm_flag]);
        mListText[2] = getString(R.string.list3_0, off_hour, off_minute);
        mListText[3] = getString(R.string.list4_0, on_hour, on_minute);

        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mListText);
        ListView listView = (ListView) findViewById(R.id.listView);
        TextView textView = new TextView(this);
        textView.setText(R.string.list0_0);
        listView.addHeaderView(textView, null, false);
        listView.setAdapter(mAdapter);

        listView.setOnItemClickListener(this);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        final String DIALOG = "dialog";
        switch (i) {
            case 0:
                break;
            case 1:
                doToggleService();
                break;
            case 2:
                new SettingDialog1().show(getSupportFragmentManager(), DIALOG);
                break;
            case 3:
                new SettingDialog2().show(getSupportFragmentManager(), DIALOG);
                break;
            case 4:
                new SettingDialog3().show(getSupportFragmentManager(), DIALOG);
                break;
        }
    }

    private boolean doToggleService() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean run_flag = pref.getBoolean(Const.RUN_FLAG, false);

        if (run_flag) {
            Intent i = new Intent(this, MyService.class);
            i.putExtra(Const.RUN_MODE, Const.MODE_STOP);
            startService(i);
            mListText[0] = getString(R.string.list1_0);
        } else {
            Intent i = new Intent(this, MyService.class);
            i.putExtra(Const.RUN_MODE, Const.MODE_START);
            startService(i);
            mListText[0] = getString(R.string.list1_1);
        }
        mAdapter.notifyDataSetChanged();
        return true;
    }

    public static class SettingDialog1 extends DialogFragment implements DialogInterface.OnClickListener {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setItems(R.array.select_array, this);
            return builder.create();
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (3 < i) return;
            MyActivity my = (MyActivity)getActivity();
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(my);
            SharedPreferences.Editor edit = pref.edit();

            my.mListText[1] = getString(R.string.list2_0, getResources().getStringArray(R.array.select_array)[i]);
            edit.putInt(Const.ALARM_FLAG, i);
            edit.apply();
            my.mAdapter.notifyDataSetChanged();
            my.refreshSetting();
        }
    }

    public static class SettingDialog2 extends DialogFragment implements DialogInterface.OnClickListener {
        TimePicker mTimePicker;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.time_title);
            mTimePicker = new TimePicker(getActivity());
            if (savedInstanceState != null){
                mTimePicker.setCurrentHour(savedInstanceState.getInt(Const.OFF_HOUR));
                mTimePicker.setCurrentMinute(savedInstanceState.getInt(Const.OFF_MINUTE));
            } else {
                mTimePicker.setCurrentHour(pref.getInt(Const.OFF_HOUR, 8));
                mTimePicker.setCurrentMinute(pref.getInt(Const.OFF_MINUTE, 0));
            }
            mTimePicker.setIs24HourView(DateFormat.is24HourFormat(getActivity()));
            mTimePicker.setSaveFromParentEnabled(false);
            mTimePicker.setSaveEnabled(true);
            builder.setView(mTimePicker);
            builder.setPositiveButton(R.string.time_done, this);
            return builder.create();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            if (mTimePicker != null){
                outState.putInt(Const.OFF_HOUR, mTimePicker.getCurrentHour());
                outState.putInt(Const.OFF_MINUTE, mTimePicker.getCurrentMinute());
            }
            super.onSaveInstanceState(outState);
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            int hour = mTimePicker.getCurrentHour();
            int minute = mTimePicker.getCurrentMinute();
            MyActivity my = (MyActivity)getActivity();
            SharedPreferences.Editor edit = pref.edit();
            my.mListText[2] = getString(R.string.list3_0, hour, minute);
            edit.putInt(Const.OFF_HOUR, hour);
            edit.putInt(Const.OFF_MINUTE, minute);
            edit.apply();
            my.mAdapter.notifyDataSetChanged();
            my.refreshSetting();
        }
    }

    public static class SettingDialog3 extends DialogFragment implements DialogInterface.OnClickListener {
        TimePicker mTimePicker;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.time_title);
            mTimePicker = new TimePicker(getActivity());
            if (savedInstanceState != null){
                mTimePicker.setCurrentHour(savedInstanceState.getInt(Const.ON_HOUR));
                mTimePicker.setCurrentMinute(savedInstanceState.getInt(Const.ON_MINUTE));
            } else {
                mTimePicker.setCurrentHour(pref.getInt(Const.ON_HOUR, 17));
                mTimePicker.setCurrentMinute(pref.getInt(Const.ON_MINUTE, 0));
            }
            mTimePicker.setIs24HourView(DateFormat.is24HourFormat(getActivity()));
            mTimePicker.setSaveFromParentEnabled(false);
            mTimePicker.setSaveEnabled(true);
            builder.setView(mTimePicker);
            builder.setPositiveButton(R.string.time_done, this);
            return builder.create();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            if (mTimePicker != null){
                outState.putInt(Const.ON_HOUR, mTimePicker.getCurrentHour());
                outState.putInt(Const.ON_MINUTE, mTimePicker.getCurrentMinute());
            }
            super.onSaveInstanceState(outState);
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            int hour = mTimePicker.getCurrentHour();
            int minute = mTimePicker.getCurrentMinute();
            MyActivity my = (MyActivity)getActivity();
            SharedPreferences.Editor edit = pref.edit();
            my.mListText[3] = getString(R.string.list4_0, hour, minute);
            edit.putInt(Const.ON_HOUR, hour);
            edit.putInt(Const.ON_MINUTE, minute);
            edit.apply();
            my.mAdapter.notifyDataSetChanged();
            my.refreshSetting();
        }
    }

    private boolean refreshSetting() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean run_flag = pref.getBoolean(Const.RUN_FLAG, false);

        if (run_flag) {
            Intent i = new Intent(this, MyService.class);
            i.putExtra(Const.RUN_MODE, Const.MODE_REFRESH);
            startService(i);
        }
        return true;
    }
}
