package com.unk2072.iijmiotoggle;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
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
    private static final String ACCESS_TOKEN = "access_token";
    private static final String RUN_FLAG = "run_flag";
    private static final String ALARM_FLAG = "alarm_flag";
    private static final String OFF_HOUR = "off_hour";
    private static final String OFF_MINUTE = "off_minute";
    private static final String ON_HOUR = "on_hour";
    private static final String ON_MINUTE = "on_minute";
    private static final String RUN_MODE = "run_mode";
    private String[] mListText = new String[4];
    private ArrayAdapter<String> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        initListView();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String token = pref.getString(ACCESS_TOKEN, "");

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
        edit.putString(ACCESS_TOKEN, token);
        edit.commit();

        boolean run_flag = pref.getBoolean(RUN_FLAG, false);
        if (run_flag) {
            String state = Uri.parse(data).getQueryParameter("state");
            Log.i(TAG, "onNewIntent state=" + state);
            int mode = Integer.valueOf(state);
            Intent i = new Intent(this, MyService.class);
            i.putExtra(RUN_MODE, mode);
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
        boolean run_flag = pref.getBoolean(RUN_FLAG, false);
        int alarm_flag = pref.getInt(ALARM_FLAG, 0);
        int off_hour = pref.getInt(OFF_HOUR, 8);
        int off_minute = pref.getInt(OFF_MINUTE, 0);
        int on_hour = pref.getInt(ON_HOUR, 17);
        int on_minute = pref.getInt(ON_MINUTE, 0);

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
        boolean run_flag = pref.getBoolean(RUN_FLAG, false);

        if (run_flag) {
            Intent i = new Intent(this, MyService.class);
            i.putExtra(RUN_MODE, 101);
            startService(i);
            mListText[0] = getString(R.string.list1_0);
        } else {
            Intent i = new Intent(this, MyService.class);
            i.putExtra(RUN_MODE, 0);
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
            edit.putInt(ALARM_FLAG, i);
            edit.commit();
            my.mAdapter.notifyDataSetChanged();
            my.refreshSetting();
        }
    }

    public static class SettingDialog2 extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            return new TimePickerDialog(getActivity(), this, pref.getInt(OFF_HOUR, 8), pref.getInt(OFF_MINUTE, 0), DateFormat.is24HourFormat(getActivity()));
        }

        @Override
        public void onTimeSet(TimePicker timePicker, int hour, int minute) {
            MyActivity my = (MyActivity)getActivity();
            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(my);
            SharedPreferences.Editor edit = pref.edit();
            my.mListText[2] = getString(R.string.list3_0, hour, minute);
            edit.putInt(OFF_HOUR, hour);
            edit.putInt(OFF_MINUTE, minute);
            edit.commit();
            my.mAdapter.notifyDataSetChanged();
            my.refreshSetting();
        }
    }

    public static class SettingDialog3 extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            return new TimePickerDialog(getActivity(), this, pref.getInt(ON_HOUR, 17), pref.getInt(ON_MINUTE, 0), DateFormat.is24HourFormat(getActivity()));
        }

        @Override
        public void onTimeSet(TimePicker timePicker, int hour, int minute) {
            MyActivity my = (MyActivity)getActivity();
            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(my);
            SharedPreferences.Editor edit = pref.edit();
            my.mListText[3] = getString(R.string.list4_0, hour, minute);
            edit.putInt(ON_HOUR, hour);
            edit.putInt(ON_MINUTE, minute);
            edit.commit();
            my.mAdapter.notifyDataSetChanged();
            my.refreshSetting();
        }
    }

    private boolean refreshSetting() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean run_flag = pref.getBoolean(RUN_FLAG, false);

        if (run_flag) {
            Intent i = new Intent(this, MyService.class);
            i.putExtra(RUN_MODE, 100);
            startService(i);
        }
        return true;
    }
}
