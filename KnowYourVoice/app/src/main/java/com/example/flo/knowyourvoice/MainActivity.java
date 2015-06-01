package com.example.flo.knowyourvoice;


import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;






public class MainActivity extends ActionBarActivity {




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        configButtons();



    }

        private void configButtons(){
        final ImageButton recBtn = (ImageButton)findViewById(R.id.Rec);
        final ImageButton stopRecBtn = (ImageButton)findViewById(R.id.Stop);
        final TextView rec = (TextView)findViewById(R.id.recText);
        final TextView pitch = (TextView)findViewById(R.id.pitch);
        final TextView freq = (TextView)findViewById(R.id.freq);
        final ProgressBar pB = (ProgressBar)findViewById(R.id.volume);






        recBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                //change Rec.-Button to Stop-Button
                recBtn.setVisibility(View.INVISIBLE);
                stopRecBtn.setVisibility(View.VISIBLE);
                //change Text
                rec.setText("Stop Recording!");
                pitch.setText("Pitch:");
                freq.setText("Hz");
        }

        });
        stopRecBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //change Stop.-Button to Rec-Button
                stopRecBtn.setVisibility(View.INVISIBLE);
                recBtn.setVisibility(View.VISIBLE);
                //change Text
                rec.setText("Start Recording!");
                pitch.setText("");
                freq.setText("");

            }

        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
