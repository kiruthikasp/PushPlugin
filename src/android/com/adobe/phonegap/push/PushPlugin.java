package com.adobe.phonegap.push;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import java.io.File;
import android.util.Log;
import android.widget.Toast;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.media.Ringtone;
import android.view.View.OnClickListener;
import android.app.Activity;
import android.media.Ringtone;
import android.widget.TextView;
import android.view.View;
import android.widget.Button;
import android.database.Cursor;
import android.view.Menu;
import java.lang.NullPointerException;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.iid.InstanceID;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import android.net.Uri;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

public class PushPlugin extends CordovaPlugin implements PushConstants {

    public static final String LOG_TAG = "PushPlugin";

    private static CallbackContext pushContext;
    private static CordovaWebView gWebView;
    private static Bundle gCachedExtras = null;
    private static boolean gForeground = false;
    public static final String MY_PREFS_NAME = "MyPrefsFile";

    /**
     * Gets the application context from cordova's main activity.
     * @return the application context
     */
    private Context getApplicationContext() {
        return this.cordova.getActivity().getApplicationContext();
    }

    @Override
    public boolean execute(final String action, final JSONArray data, final CallbackContext callbackContext) {
        Log.v(LOG_TAG, "execute: action=" + action);
        gWebView = this.webView;

        if (INITIALIZE.equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    pushContext = callbackContext;
                    JSONObject jo = null;

                    Log.v(LOG_TAG, "execute: data=" + data.toString());
                    SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(COM_ADOBE_PHONEGAP_PUSH, Context.MODE_PRIVATE);
                    String token = null;
                    String senderID = null;
                    String soundpath = null;

                    try {
                        jo = data.getJSONObject(0).getJSONObject(ANDROID);

                        Log.v(LOG_TAG, "execute: jo=" + jo.toString());

                        senderID = jo.getString(SENDER_ID);

                        Log.v(LOG_TAG, "execute: senderID=" + senderID);

                        String savedSenderID = sharedPref.getString(SENDER_ID, "");
                        String savedRegID = sharedPref.getString(REGISTRATION_ID, "");

                        // first time run get new token
                        if ("".equals(savedRegID)) {
                            token = InstanceID.getInstance(getApplicationContext()).getToken(senderID, GCM);
                        }
                        // new sender ID, re-register
                        else if (!savedSenderID.equals(senderID)) {
                            token = InstanceID.getInstance(getApplicationContext()).getToken(senderID, GCM);
                        }
                        // use the saved one
                        else {
                            token = sharedPref.getString(REGISTRATION_ID, "");
                        }

                        if (!"".equals(token)) {
                            JSONObject json = new JSONObject().put(REGISTRATION_ID, token);

                            Log.v(LOG_TAG, "onRegistered: " + json.toString());

                            JSONArray topics = jo.optJSONArray(TOPICS);
                            subscribeToTopics(topics, token);

                            PushPlugin.sendEvent( json );
                        } else {
                            callbackContext.error("Empty registration ID received from GCM");
                            return;
                        }
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "execute: Got JSON Exception " + e.getMessage());
                        callbackContext.error(e.getMessage());
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "execute: Got JSON Exception " + e.getMessage());
                        callbackContext.error(e.getMessage());
                    }

                    if (jo != null) {
                        SharedPreferences.Editor editor = sharedPref.edit();
                        try {
                            editor.putString(ICON, jo.getString(ICON));
                        } catch (JSONException e) {
                            Log.d(LOG_TAG, "no icon option");
                        }
                        try {
                            editor.putString(ICON_COLOR, jo.getString(ICON_COLOR));
                        } catch (JSONException e) {
                            Log.d(LOG_TAG, "no iconColor option");
                        }
                        editor.putBoolean(SOUND, jo.optBoolean(SOUND, true));
                        editor.putString(SOUNDPATH, soundpath);
                        editor.putBoolean(VIBRATE, jo.optBoolean(VIBRATE, true));
                        editor.putBoolean(CLEAR_NOTIFICATIONS, jo.optBoolean(CLEAR_NOTIFICATIONS, true));
                        editor.putBoolean(FORCE_SHOW, jo.optBoolean(FORCE_SHOW, false));
                        editor.putString(SENDER_ID, senderID);
                        editor.putString(REGISTRATION_ID, token);
                        editor.commit();
                    }

                    if (gCachedExtras != null) {
                        Log.v(LOG_TAG, "sending cached extras");
                        sendExtras(gCachedExtras);
                        gCachedExtras = null;
                    }
                }
            });
        } else if (UNREGISTER.equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(COM_ADOBE_PHONEGAP_PUSH, Context.MODE_PRIVATE);
                        String token = sharedPref.getString(REGISTRATION_ID, "");
                        JSONArray topics = data.optJSONArray(0);
                        if (topics != null && !"".equals(token)) {
                            unsubscribeFromTopics(topics, token);
                        } else {
                            InstanceID.getInstance(getApplicationContext()).deleteInstanceID();
                            Log.v(LOG_TAG, "UNREGISTER");

                            // Remove shared prefs
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.remove(SOUND);
                            editor.remove(SOUNDPATH);
                            editor.remove(VIBRATE);
                            editor.remove(CLEAR_NOTIFICATIONS);
                            editor.remove(FORCE_SHOW);
                            editor.remove(SENDER_ID);
                            editor.remove(REGISTRATION_ID);
                            editor.commit();
                        }

                        callbackContext.success();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "execute: Got JSON Exception " + e.getMessage());
                        callbackContext.error(e.getMessage());
                }
            }
            });
        } else if (FINISH.equals(action)) {
            callbackContext.success();
        } else if(TONE.equals(action)){
            try{
            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE);
            String s = sharedPref.getString("soundpath",null);
            if(s != null){
            Uri ringtoneUri = Uri.parse(s);
            Ringtone ringtone = RingtoneManager.getRingtone(this.cordova.getActivity().getApplicationContext(), ringtoneUri);
            String name = ringtone.getTitle(this.cordova.getActivity().getApplicationContext());
            callbackContext.success(name);
            }
            }catch(UnknownError e){
             callbackContext.error(e.getMessage());
            }
        } else if (HAS_PERMISSION.equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    JSONObject jo = new JSONObject();
                    try {
                        jo.put("isEnabled", PermissionUtils.hasPermission(getApplicationContext(), "OP_POST_NOTIFICATION"));
                        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jo);
                        pluginResult.setKeepCallback(true);
                        callbackContext.sendPluginResult(pluginResult);
                    } catch (UnknownError e) {
                        callbackContext.error(e.getMessage());
                    } catch (JSONException e) {
                        callbackContext.error(e.getMessage());
                    }
                }
            });
        } else if (SELECT.equals(action)){
            // Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            // intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select ringtone for notifications:");
            // intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
            // intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            // intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM);
            // this.startActivityForResult( intent, 999);
        try {  
            //Context context=this.cordova.getActivity().getApplicationContext();
            //or Context context=cordova.getActiivity().getApplicationContext();
            this.cordova.setActivityResultCallback(this); 
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_NOTIFICATION | RingtoneManager.TYPE_RINGTONE);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            this.cordova.getActivity().startActivityForResult(intent, 999);
            callbackContext.success();
            //  Toast.makeText(this.cordova.getActivity().getApplicationContext(),"called",
            //  4000).show();
        } catch (UnknownError e) {
            callbackContext.error(e.getMessage());
        } 
        }else {
            Log.e(LOG_TAG, "Invalid action : " + action);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
            return false;
        }

        return true;
    }
   

 public void onActivityResult( int requestCode,  int resultCode,  Intent intent)
 {
     SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE);
     if ( requestCode == 999)
     {
         try{
          Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
          
          if(uri != null){
              String str = uri.toString();
              Context context=this.cordova.getActivity().getApplicationContext();
              SharedPreferences.Editor editor = sharedPref.edit();
              editor.putString("soundpath",str);
              editor.commit();
          }else{
             Toast.makeText(this.cordova.getActivity().getApplicationContext(), "No Ringtone Path Found!", 2500).show(); 
          }
         }catch(NullPointerException e){
             Toast.makeText(this.cordova.getActivity().getApplicationContext(), "No Ringtone Selected!", 2500).show(); 
         }
     }
  }
    public static void sendEvent(JSONObject _json) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, _json);
        pluginResult.setKeepCallback(true);
        if (pushContext != null) {
            pushContext.sendPluginResult(pluginResult);
        }
    }

    public static void sendError(String message) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, message);
        pluginResult.setKeepCallback(true);
        if (pushContext != null) {
            pushContext.sendPluginResult(pluginResult);
        }
    }

    /*
     * Sends the pushbundle extras to the client application.
     * If the client application isn't currently active, it is cached for later processing.
     */
    public static void sendExtras(Bundle extras) {
        if (extras != null) {
            if (gWebView != null) {
                sendEvent(convertBundleToJson(extras));
            } else {
                Log.v(LOG_TAG, "sendExtras: caching extras to send at a later time.");
                gCachedExtras = extras;
            }
        }
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        gForeground = true;
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        gForeground = false;

        SharedPreferences prefs = getApplicationContext().getSharedPreferences(COM_ADOBE_PHONEGAP_PUSH, Context.MODE_PRIVATE);
        if (prefs.getBoolean(CLEAR_NOTIFICATIONS, true)) {
            final NotificationManager notificationManager = (NotificationManager) cordova.getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        gForeground = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        gForeground = false;
        gWebView = null;
    }

    private void subscribeToTopics(JSONArray topics, String registrationToken) {
        if (topics != null) {
            String topic = null;
            for (int i=0; i<topics.length(); i++) {
                try {
                    topic = topics.optString(i, null);
                    if (topic != null) {
                        Log.d(LOG_TAG, "Subscribing to topic: " + topic);
                        GcmPubSub.getInstance(getApplicationContext()).subscribe(registrationToken, "/topics/" + topic, null);
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Failed to subscribe to topic: " + topic, e);
                }
            }
        }
    }

    private void unsubscribeFromTopics(JSONArray topics, String registrationToken) {
        if (topics != null) {
            String topic = null;
            for (int i=0; i<topics.length(); i++) {
                try {
                    topic = topics.optString(i, null);
                    if (topic != null) {
                        Log.d(LOG_TAG, "Unsubscribing to topic: " + topic);
                        GcmPubSub.getInstance(getApplicationContext()).unsubscribe(registrationToken, "/topics/" + topic);
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Failed to unsubscribe to topic: " + topic, e);
                }
            }
        }
    }

    /*
     * serializes a bundle to JSON.
     */
    private static JSONObject convertBundleToJson(Bundle extras) {
        Log.d(LOG_TAG, "convert extras to json");
        try {
            JSONObject json = new JSONObject();
            JSONObject additionalData = new JSONObject();

            // Add any keys that need to be in top level json to this set
            HashSet<String> jsonKeySet = new HashSet();
            Collections.addAll(jsonKeySet, TITLE,MESSAGE,COUNT,SOUND,SOUNDPATH,IMAGE);

            Iterator<String> it = extras.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                Object value = extras.get(key);

                Log.d(LOG_TAG, "key = " + key);

                if (jsonKeySet.contains(key)) {
                    json.put(key, value);
                }
                else if (key.equals(COLDSTART)) {
                    additionalData.put(key, extras.getBoolean(COLDSTART));
                }
                else if (key.equals(FOREGROUND)) {
                    additionalData.put(key, extras.getBoolean(FOREGROUND));
                }
                else if ( value instanceof String ) {
                    String strValue = (String)value;
                    try {
                        // Try to figure out if the value is another JSON object
                        if (strValue.startsWith("{")) {
                            additionalData.put(key, new JSONObject(strValue));
                        }
                        // Try to figure out if the value is another JSON array
                        else if (strValue.startsWith("[")) {
                            additionalData.put(key, new JSONArray(strValue));
                        }
                        else {
                            additionalData.put(key, value);
                        }
                    } catch (Exception e) {
                        additionalData.put(key, value);
                    }
                }
            } // while

            json.put(ADDITIONAL_DATA, additionalData);
            Log.v(LOG_TAG, "extrasToJSON: " + json.toString());

            return json;
        }
        catch( JSONException e) {
            Log.e(LOG_TAG, "extrasToJSON: JSON exception");
        }
        return null;
    }

    public static boolean isInForeground() {
      return gForeground;
    }

    public static boolean isActive() {
        return gWebView != null;
    }
}
