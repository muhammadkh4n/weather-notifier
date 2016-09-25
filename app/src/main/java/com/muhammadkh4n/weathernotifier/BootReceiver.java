package com.muhammadkh4n.weathernotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmReceiver alarm = new AlarmReceiver();
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
            alarm.setAlarm(context);
    }
}
