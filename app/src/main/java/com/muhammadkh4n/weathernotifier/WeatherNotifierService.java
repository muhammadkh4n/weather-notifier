package com.muhammadkh4n.weathernotifier;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class WeatherNotifierService extends IntentService {

    private static final String TAG = "WeatherService";
    public static final String ALARM_SETTINGS = "AlarmSettings";

    private static final String URL = "http://api.wunderground.com/api/9c087837fd1cccdc/forecast/q/zmw:00000.6.41672.json";

    private NotificationManager mNotificationManager;
    public static final int NOTIFICATION_ID = 1;
    NotificationCompat.Builder builder;

    public WeatherNotifierService() {
        super("WeatherNotifierService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String urlString = URL;
        String result = "";

        try {
            result = loadFromNetwork(urlString);
        } catch (IOException e) {
            Log.i(TAG, "Connection Error.");
            sendNotification("Connection error! Send SMS manually");
        }

        if (!result.equals("")) {
            String msg = "";
            try {
                JSONObject reader = new JSONObject(result);
                JSONObject req = reader.getJSONObject("forecast").getJSONObject("simpleforecast");
                JSONArray forecastArray = req.getJSONArray("forecastday");
                String[] weather = new String[2];
                for (int i = 0; i < 2; i++) {
                    JSONObject day = forecastArray.getJSONObject(i);
                    String weekday = day.getJSONObject("date").optString("weekday");
                    String conditions = day.optString("conditions");
                    String high = day.getJSONObject("high").optString("celsius");
                    String low = day.getJSONObject("low").optString("celsius");
                    String humidity = day.optInt("avehumidity") + "%";
                    String rain = day.optInt("pop") + "%";
                    weather[i] = weekday + ": "
                            + conditions
                            + ", Temp: " + high + "/" + low
                            + ", Humidity: " + humidity
                            + ", Rain: " + rain;
                }

                msg = weather[0] + "\n" + weather[1];
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            Log.i(TAG, msg);
            sendSms(msg);
        } else {
            Log.i(TAG, "No Data");
        }

        AlarmReceiver.completeWakefulIntent(intent);

    }

    // Checks Network Connectivity.
    private boolean checkNetwork() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }

    // Send SMS
    private void sendSms(String msg) {
        SharedPreferences settings = getSharedPreferences(ALARM_SETTINGS, 0);
        String phoneNo = settings.getString("alarmNumber", null);

        try {
            SmsManager smsMgr = SmsManager.getDefault();
            ArrayList<String> parts = smsMgr.divideMessage(msg);
            smsMgr.sendMultipartTextMessage(phoneNo, null, parts, null, null);
            mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(NOTIFICATION_ID);
        } catch (Exception e) {
            //Toast.makeText(WeatherNotifierService.this, "SMS Failed", Toast.LENGTH_SHORT).show();
            sendNotification("SMS Sending Failed");
            e.printStackTrace();
        }
    }

    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        // sms action
        Intent ws = new Intent(getApplicationContext(), WeatherNotifierService.class);
        PendingIntent sendsms = PendingIntent.getService(this, 0, ws, 0);
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(0, "Send SMS", sendsms).build();

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Weather Notifier Alert!")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg)
                        .addAction(action);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private String loadFromNetwork(String urlString) throws IOException {
        InputStream stream = null;
        String str = "";

        try {
            stream = downloadUrl(urlString);
            str = readIt(stream);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        return str;
    }

    private InputStream downloadUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setUseCaches(false);
        conn.setAllowUserInteraction(false);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);

        conn.connect();
        InputStream stream = conn.getInputStream();

        return stream;
    }

    private String readIt(InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();

        return builder.toString();
    }
}
