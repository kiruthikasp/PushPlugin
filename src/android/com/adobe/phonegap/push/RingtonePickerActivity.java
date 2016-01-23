package com.adobe.phonegap.push;

import android.media.RingtoneManager;
import android.view.View.OnClickListener;
import android.app.Activity;
import android.media.Ringtone;
import android.widget.TextView;
import android.view.View;
import android.widget.Button;
import android.database.Cursor;
import android.view.Menu;

public class RingtonePickerActivity extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picker);
    }
}
