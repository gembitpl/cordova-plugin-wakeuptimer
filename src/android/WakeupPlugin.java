package pl.gembit.cordova.wakeupplugin;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.view.Window;

public class WakeupPlugin extends CordovaPlugin {

    protected static final String LOG_TAG = "WakeupPlugin";

    // Reference to the web view for static access
    private static CordovaWebView webView = null;

    // Indicates if the device is ready (to receive events)
    private static Boolean deviceready = false;

    // To inform the user about the state of the app in callbacks
    protected static Boolean isInBackground = true;

    // Queues all events before deviceready
    private static ArrayList<String> eventQueue = new ArrayList<String>();

    protected static final int ID_DAYLIST_OFFSET = 10010;
    protected static final int ID_ONETIME_OFFSET = 10000;
    protected static final int ID_SNOOZE_OFFSET = 10001;

    public static Map<String, Integer> daysOfWeek = new HashMap<String, Integer>() {
        private static final long serialVersionUID = 1L;

        {
            put("sunday", 0);
            put("monday", 1);
            put("tuesday", 2);
            put("wednesday", 3);
            put("thursday", 4);
            put("friday", 5);
            put("saturday", 6);
        }
    };

    public static CallbackContext connectionCallbackContext;

    @Override
    public void initialize (CordovaInterface cordova, CordovaWebView webView) {
        Log.d(LOG_TAG, "initialize");
        WakeupPlugin.webView = webView;
    }

//    @Override
//    public void onReset() {
//        // app startup
//        Log.d(LOG_TAG, "Wakeup Plugin onReset");
//        Bundle extras = cordova.getActivity().getIntent().getExtras();
//        if (extras != null && !extras.getBoolean("wakeup", false)) {
//            setAlarmsFromPrefs(cordova.getActivity().getApplicationContext());
//        }
//        super.onReset();
//    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        Log.d(LOG_TAG, "onResume");

        isInBackground = false;
        deviceready();

        final Window win = cordova.getActivity().getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        if (!cordova.getActivity().getIntent().getBooleanExtra("screen_off", false))
        {
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        }
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        isInBackground = true;
        Log.d(LOG_TAG, "pause");
    }

    @Override
    public void onDestroy() {
        deviceready = false;
        isInBackground = true;
    }

//    public void onNewIntent(Intent intent) {
//        String playStream="";
//
//        Log.d(LOG_TAG, "onNewIntent: " + intent);
//        playStream = intent.getStringExtra("playStream");
//
//        if(playStream != null){
//            if(playStream.equals("true")){
//
//                Log.d(LOG_TAG,"Starting stream from Wakeupplugin");
//                WakeupPlugin.fireEvent("please start the stream :)");
//            }
//        }
//    }

    @Override
    public boolean execute (final String action, final JSONArray args,
                            final CallbackContext command) throws JSONException {

//        Notification.setDefaultTriggerReceiver(TriggerReceiver.class);

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                if (action.equals("wakeup")) {
                    wakeup(args);
                    command.success();
                }
                else if (action.equals("snooze")) {
                    snooze(args);
                    command.success();
                }
                else if (action.equals("alarm")) {
                    alarm(new JSONArray());
                    command.success();
                }
            }
        });

        return true;
    }

    private void alarm(JSONArray args)
    {
//        JSONObject options = args.getJSONObject(0);

//        JSONArray alarms;
//        if (options.has("alarms") == true)
//        {
//            alarms = options.getJSONArray("alarms");
//        }
//        else
//        {
//            alarms = new JSONArray(); // default to empty array
//        }

//        if (extrasBundle != null && extrasBundle.getString("type") != null && extrasBundle.getString("type").equals("daylist")) {
//            // repeat in one week
//            Date next = new Date(new Date().getTime() + (7 * 24 * 60 * 60 * 1000));
//            Log.d(LOG_TAG, "resetting alarm at " + sdf.format(next));
//
//            Intent reschedule = new Intent(context, WakeupReceiver.class);
//            if (extras != null) {
//                reschedule.putExtra("extra", intent.getExtras().get("extra").toString());
//            }
//            reschedule.putExtra("day", WakeupPlugin.daysOfWeek.get(intent.getExtras().get("day")));
//
//            PendingIntent sender = PendingIntent.getBroadcast(context, 19999 + WakeupPlugin.daysOfWeek.get(intent.getExtras().get("day")), intent, PendingIntent.FLAG_UPDATE_CURRENT);
//            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//            if (Build.VERSION.SDK_INT >= 19) {
//                alarmManager.setExact(AlarmManager.RTC_WAKEUP, next.getTime(), sender);
//            } else {
//                alarmManager.set(AlarmManager.RTC_WAKEUP, next.getTime(), sender);
//            }
//        }

        fireEvent("alarm", args);
    }

    private void wakeup(JSONArray args)
    {
        JSONArray alarms = new JSONArray();

        try {
            JSONObject options = args.getJSONObject(0);
            if (options.has("alarms") == true)
            {
                alarms = options.getJSONArray("alarms");
            }
        } catch (JSONException exception) {}

        saveToPrefs(cordova.getActivity().getApplicationContext(), alarms);
        try {
            setAlarms(cordova.getActivity().getApplicationContext(), alarms, true);
        } catch (JSONException exception) {}


        fireEvent("wakeup", args);
    }

    private void snooze(JSONArray args)
    {
        try {
            JSONObject options = args.getJSONObject(0);

            if (options.has("alarms") == true)
            {
                Log.d(LOG_TAG, "scheduling snooze...");
                JSONArray alarms = options.getJSONArray("alarms");
                setAlarms(cordova.getActivity().getApplicationContext(), alarms, false);
            }
        } catch (JSONException exception) {}

        fireEvent("snooze", args);
    }

    public static void setAlarmsFromPrefs(Context context) {
        try {
            SharedPreferences prefs;
            prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String a = prefs.getString("alarms", "[]");
            Log.d(LOG_TAG, "setting alarms:\n" + a);
            JSONArray alarms = new JSONArray(a);
            WakeupPlugin.setAlarms(context, alarms, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint({"SimpleDateFormat", "NewApi"})
    protected static void setAlarms(Context context, JSONArray alarms, boolean cancelAlarms) throws JSONException {

        if (cancelAlarms) {
            cancelAlarms(context);
        }

        for (int i = 0; i < alarms.length(); i++) {
            JSONObject alarm = alarms.getJSONObject(i);

            String type = "onetime";
            if (alarm.has("type")) {
                type = alarm.getString("type");
            }

            if (!alarm.has("time")) {
                throw new JSONException("alarm missing time: " + alarm.toString());
            }

            JSONObject time = alarm.getJSONObject("time");

            if (type.equals("onetime")) {
                Calendar alarmDate = getOneTimeAlarmDate(time);
                Intent intent = new Intent(context, WakeupReceiver.class);
                if (alarm.has("extra")) {
                    intent.putExtra("extra", alarm.getJSONObject("extra").toString());
                    intent.putExtra("type", type);
                }

                setNotification(context, type, alarmDate, intent, ID_ONETIME_OFFSET);

            } else if (type.equals("daylist")) {
                JSONArray days = alarm.getJSONArray("days");

                for (int j = 0; j < days.length(); j++) {
                    Calendar alarmDate = getAlarmDate(time, daysOfWeek.get(days.getString(j)));
                    Intent intent = new Intent(context, WakeupReceiver.class);
                    if (alarm.has("extra")) {
                        intent.putExtra("extra", alarm.getJSONObject("extra").toString());
                        intent.putExtra("type", type);
                        intent.putExtra("time", time.toString());
                        intent.putExtra("day", days.getString(j));
                    }

                    setNotification(context, type, alarmDate, intent, ID_DAYLIST_OFFSET + daysOfWeek.get(days.getString(j)));
                }
            } else if (type.equals("snooze")) {
                cancelSnooze(context);
                Calendar alarmDate = getTimeFromNow(time);
                Intent intent = new Intent(context, WakeupReceiver.class);
                if (alarm.has("extra")) {
                    intent.putExtra("extra", alarm.getJSONObject("extra").toString());
                    intent.putExtra("type", type);
                }
                setNotification(context, type, alarmDate, intent, ID_SNOOZE_OFFSET);
            }
        }
    }


    protected static void setNotification(Context context, String type, Calendar alarmDate, Intent intent, int id) throws JSONException {
        if (alarmDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Log.d(LOG_TAG, "setting alarm at " + sdf.format(alarmDate.getTime()) + "; id " + id);

            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent sender = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmDate.getTimeInMillis(), sender);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, alarmDate.getTimeInMillis(), sender);
            }

//            if (WakeupPlugin.connectionCallbackContext != null) {
//                JSONObject o = new JSONObject();
//                o.put("type", "set");
//                o.put("alarm_type", type);
//                o.put("alarm_date", alarmDate.getTimeInMillis());
//
//                Log.d(LOG_TAG, "alarm time in millis: " + alarmDate.getTimeInMillis());
//
//                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, o);
//                pluginResult.setKeepCallback(true);
//                WakeupPlugin.connectionCallbackContext.sendPluginResult(pluginResult);
//            }
        }
    }

    protected static void cancelAlarms(Context context) {
        Log.d(LOG_TAG, "canceling alarms");
        Intent intent = new Intent(context, WakeupReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, ID_ONETIME_OFFSET, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Log.d(LOG_TAG, "cancelling alarm id " + ID_ONETIME_OFFSET);
        alarmManager.cancel(sender);

        cancelSnooze(context);

        for (int i = 0; i < 7; i++) {
            intent = new Intent(context, WakeupReceiver.class);
            Log.d(LOG_TAG, "cancelling alarm id " + (ID_DAYLIST_OFFSET + i));
            sender = PendingIntent.getBroadcast(context, ID_DAYLIST_OFFSET + i, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.cancel(sender);
        }
    }

    protected static void cancelSnooze(Context context) {
        Log.d(LOG_TAG, "canceling snooze");
        Intent intent = new Intent(context, WakeupReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, ID_SNOOZE_OFFSET, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Log.d(LOG_TAG, "cancelling alarm id " + ID_SNOOZE_OFFSET);
        alarmManager.cancel(sender);
    }

    protected static Calendar getOneTimeAlarmDate(JSONObject time) throws JSONException {
        TimeZone defaultz = TimeZone.getDefault();
        Calendar calendar = new GregorianCalendar(defaultz);
        Calendar now = new GregorianCalendar(defaultz);
        now.setTime(new Date());
        calendar.setTime(new Date());

        int hour = (time.has("hour")) ? time.getInt("hour") : -1;
        int minute = (time.has("minute")) ? time.getInt("minute") : 0;

        if (hour >= 0) {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (calendar.before(now)) {
                calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
            }
        } else {
            calendar = null;
        }

        return calendar;
    }

    protected static Calendar getAlarmDate(JSONObject time, int dayOfWeek) throws JSONException {
        TimeZone defaultz = TimeZone.getDefault();
        Calendar calendar = new GregorianCalendar(defaultz);
        Calendar now = new GregorianCalendar(defaultz);
        now.setTime(new Date());
        calendar.setTime(new Date());

        int hour = (time.has("hour")) ? time.getInt("hour") : -1;
        int minute = (time.has("minute")) ? time.getInt("minute") : 0;

        if (hour >= 0) {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // 1-7 = Sunday-Saturday
            currentDayOfWeek--; // make zero-based

            // add number of days until 'dayOfWeek' occurs
            int daysUntilAlarm = 0;
            if (currentDayOfWeek > dayOfWeek) {
                // currentDayOfWeek=thursday (4); alarm=monday (1) -- add 4 days
                daysUntilAlarm = (6 - currentDayOfWeek) + dayOfWeek + 1; // (days until the end of week) + dayOfWeek + 1
            } else if (currentDayOfWeek < dayOfWeek) {
                // example: currentDayOfWeek=monday (1); dayOfWeek=thursday (4) -- add three days
                daysUntilAlarm = dayOfWeek - currentDayOfWeek;
            } else {
                if (now.after(calendar.getTime())) {
                    daysUntilAlarm = 7;
                } else {
                    daysUntilAlarm = 0;
                }
            }

            calendar.set(Calendar.DATE, now.get(Calendar.DATE) + daysUntilAlarm);
        } else {
            calendar = null;
        }

        return calendar;
    }

    protected static Calendar getTimeFromNow(JSONObject time) throws JSONException {
        TimeZone defaultz = TimeZone.getDefault();
        Calendar calendar = new GregorianCalendar(defaultz);
        calendar.setTime(new Date());

        int seconds = (time.has("seconds")) ? time.getInt("seconds") : -1;

        if (seconds >= 0) {
            calendar.add(Calendar.SECOND, seconds);
        } else {
            calendar = null;
        }

        return calendar;
    }

    protected static void saveToPrefs(Context context, JSONArray alarms) {
        SharedPreferences prefs;
        SharedPreferences.Editor editor;

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = prefs.edit();
        editor.putString("alarms", alarms.toString());
        editor.commit();

    }

    /**
     * Call all pending callbacks after the deviceready event has been fired.
     */
    private static synchronized void deviceready () {
        isInBackground = false;
        deviceready = true;

        for (String js : eventQueue) {
            sendJavascript(js);
        }

        eventQueue.clear();
    }

    /**
     * Fire given event on JS side. Does inform all event listeners.
     *
     * @param event
     *      The event name
     */
    private void fireEvent(String event) {
        fireEvent(event, null);
    }

    /**
     * Fire given event on JS side. Does inform all event listeners.
     *
     * @param event
     *      The event name
     * @param data
     *      Optional local notification to pass the id and properties.
     */
    static void fireEvent (String event, JSONArray data) {
        String state = getApplicationState();
        String params = "\"" + state + "\"";

        if (data != null) {
            params = data.toString() + "," + params;
        }

        String js = "wakeuptimer.fireEvent(" +
                "\"" + event + "\"," + params + ")";

        sendJavascript(js);
    }

    /**
     * Use this instead of deprecated sendJavascript
     *
     * @param js
     *       JS code snippet as string
     */
    private static synchronized void sendJavascript(final String js) {

        if (!deviceready)
        {
            Log.d(LOG_TAG, "add to queue: "+js);
            eventQueue.add(js);
            return;
        }
        Log.d(LOG_TAG, "send: "+js);
        Runnable jsLoader = new Runnable() {
            public void run() {
                WakeupPlugin.webView.loadUrl("javascript:" + js);
            }
        };
        try {
            Method post = WakeupPlugin.webView.getClass().getMethod("post",Runnable.class);
            post.invoke(WakeupPlugin.webView, jsLoader);
        } catch(Exception e) {
            ((Activity)(WakeupPlugin.webView.getContext())).runOnUiThread(jsLoader);
        }
    }

    static String getApplicationState () {
        return isInBackground ? "background" : "foreground";

    }

}
