package pl.gembit.cordova.wakeupplugin;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class WakeupReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "WakeupReceiver";

    @SuppressLint({"SimpleDateFormat", "NewApi"})
    @Override
    public void onReceive(Context context, Intent intent) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Log.d(LOG_TAG, "wakeuptimer expired at " + sdf.format(new Date().getTime()));

        launchApp(context);
        WakeupPlugin.fireEvent("alarm", getExtra(intent));
    }

    private JSONArray getExtra(Intent intent)
    {
        Bundle bundle = intent.getExtras();
        JSONArray options = null;

//        Intent i = new Intent(context, c);
        if (bundle != null && bundle.get("extra") != null)
        {
            try {
                options = new JSONArray(bundle.get("extra").toString());
            } catch (JSONException exception) {
            }
        }

        return options;
    }

    private void launchApp(Context context)
    {
        String pkgName  = context.getPackageName();

        Intent intent = context
                .getPackageManager()
                .getLaunchIntentForPackage(pkgName);

        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        context.startActivity(intent);
    }
}
