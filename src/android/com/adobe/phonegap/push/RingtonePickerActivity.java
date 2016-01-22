package com.adobe.phonegap.push;

public class Ringtones extends Activity implements OnClickListener{

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
setContentView(R.layout.main);

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



Now we need to finish this method off by closing it with startActivity(intent);, but this will be different then other Intents because we want to get the data from this Intent so we can use it elsewhere. In this case, we want to get the data and set the default ringtone. So now lets close this onClick method and start the Activity:


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
