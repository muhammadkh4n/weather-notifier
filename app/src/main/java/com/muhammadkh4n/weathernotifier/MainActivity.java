package com.muhammadkh4n.weathernotifier;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    AlarmReceiver alarm = new AlarmReceiver();
    SharedPreferences settings;

    private TextView alarm_number;
    private TextView alarm_status;

    public static final String ALARM_SETTINGS = "AlarmSettings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = getSharedPreferences(ALARM_SETTINGS, 0);

        alarm_number = (TextView) findViewById(R.id.alarm_number);
        alarm_number.setText(settings.getString("alarmNumber", "Set Number"));

        alarm_status = (TextView) findViewById(R.id.alarm_status_on_off);
        alarm_status.setText(settings.getString("alarmStatus", "OFF"));
        if (alarm_status.getText().toString().equals("ON"))
            alarm_status.setTextColor(Color.GREEN);
    }

    public void sendSms(View v) {
        if (!alarm_number.getText().toString().equals("Set Number"))
            startService(new Intent(getApplication(), WeatherNotifierService.class));
        else
            Toast.makeText(this, "Set Number First", Toast.LENGTH_SHORT).show();
    }

    public void setAlarmNumber(View v) {
        EditText number = (EditText) findViewById(R.id.phone_number);
        if (!number.getText().toString().equals("") && number.getText().toString().length() == 11) {
            alarm_number.setText(number.getText());
            SharedPreferences.Editor edit = settings.edit();
            edit.putString("alarmNumber", alarm_number.getText().toString());
            edit.commit();
        } else {
            Toast invalidToast = Toast.makeText(this, "Enter a valid Number", Toast.LENGTH_LONG);
            invalidToast.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // Menu options to set and cancel the alarm.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // When the user clicks START ALARM, set the alarm.
            case R.id.start_alarm:
                if (!alarm_number.getText().toString().equals("")) {
                    alarm.setAlarm(this);
                    alarm_status.setText("ON");
                    alarm_status.setTextColor(Color.GREEN);
                }
                return true;
            // When the user clicks CANCEL ALARM, cancel the alarm.
            case R.id.cancel_alarm:
                alarm.cancelAlarm(this);
                alarm_status.setText("OFF");
                alarm_status.setTextColor(Color.RED);
                return true;
        }
        return false;
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("alarmNumber", alarm_number.getText().toString());
        editor.putString("alarmStatus", alarm_status.getText().toString());

        editor.apply();
    }
}
