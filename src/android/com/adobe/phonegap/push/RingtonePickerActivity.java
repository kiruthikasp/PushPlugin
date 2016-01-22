package com.adobe.phonegap.push;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;

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

public class RingtonePickerActivity extends Activity implements OnClickListener{

Ringtone rt;
RingtoneManager mRingtoneManager;
TextView text;
Button button1;
Cursor mcursor;
Intent Mringtone;
String title;

/** Called when the activity is first created. */
@Override
public void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);
setContentView(R.layout.picker);

//the following appends the cursor with the cursor that is used when the ringtone picker pops up
mRingtoneManager = new RingtoneManager(this);
mcursor = mRingtoneManager.getCursor();
title = mRingtoneManager.EXTRA_RINGTONE_TITLE;

Cursor myCursor = myRingtoneManager.getCursor();


text = (TextView)findViewById(R.id.textadd);
button1 = (Button)findViewById(R.id.button01);
button1.setOnClickListener(this);


@Override
public void onClick(View arg0) {
// TODO Auto-generated method stub
//Starts the intent or Activity of the ringtone manager, opens popup box
Mringtone = new Intent(mRingtoneManager.ACTION_RINGTONE_PICKER);

//specifies what type of tone we want, in this case "ringtone", can be notification if you want
Mringtone.putExtra(mRingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);

//gives the title of the RingtoneManager picker title
Mringtone.putExtra(mRingtoneManager.EXTRA_RINGTONE_TITLE, "This is the title Of Your Picker!");

//returns true shows the rest of the songs on the device in the default location
Mringtone.getBooleanExtra(mRingtoneManager.EXTRA_RINGTONE_INCLUDE_DRM, true);

String uri = null;
//chooses and keeps the selected item as a uri
if ( uri != null ) {
Mringtone.putExtra(mRingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse( uri ));
} else {
Mringtone.putExtra(mRingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri)null);
}


startActivityForResult(Mringtone, 0);

}

protected void onActivityResult(int requestCode, int resultCode, Intent Mringtone) {
switch (resultCode) {
/*
*
*/
case RESULT_OK:
//sents the ringtone that is picked in the Ringtone Picker Dialog
Uri uri = Mringtone.getParcelableExtra(mRingtoneManager.EXTRA_RINGTONE_PICKED_URI);

//send the output of the selected to a string
String test = uri.toString();

//the program creates a "line break" when using the "\n" inside a string value
text.setText("\n " + test + "\n " + title);

//prints out the result in the console window
Log.i("Sample", "uri " + uri);

//this passed the ringtone selected from the user to a new method
play(uri);

//inserts another line break for more data, this times adds the cursor count on the selected item
text.append("\n " + mcursor.getCount());

//set default ringtone
try
{
RingtoneManager.setActualDefaultRingtoneUri(this, resultCode, uri);
}
catch (Exception localException)
{

}
break;


}

}


play(uri);


//this method captures the ringtone from the selection and plays it in the main activity
private void play(Uri uri) {
// TODO Auto-generated method stub
if (uri != null) {

//in order to play the ringtone, you need to create a new Ringtone with RingtoneManager and pass it to a variable
rt = mRingtoneManager.getRingtone(this, uri);
rt.play();

}
}
