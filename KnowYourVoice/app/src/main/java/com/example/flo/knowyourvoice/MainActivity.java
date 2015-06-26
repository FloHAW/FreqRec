package com.example.flo.knowyourvoice;


import android.app.Activity;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.text.format.Time;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.jtransforms.fft.DoubleFFT_1D;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class MainActivity extends Activity {

    private static final int SAMPLERATE = 8000;
    private File mRecording;
    private AudioRecord audioRecord, freqRecord;
    private boolean isRecording = false;
    private short[] buffer, sampleShortBuffer;
    private Chronometer chrono;
    long time = 0;

    private DoubleFFT_1D fft;
    private static final int FRAMES_PER_BUFFER = 1024;
    private static final int BYTES_PER_SAMPLE = 2;
    private double sampleDoubleBuffer[];
    private Thread recordingThread = null;

    public int bufferSize = AudioRecord.getMinBufferSize(SAMPLERATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT);




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buffer = new short[bufferSize];
        int bufferSizeInBytes = FRAMES_PER_BUFFER * BYTES_PER_SAMPLE;
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLERATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        freqRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLERATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);


        final ImageButton statusBtn = (ImageButton) findViewById(R.id.status);

        final TextView rec = (TextView) findViewById(R.id.recText);

        final TextView freq = (TextView) findViewById(R.id.freq);



        chrono =(Chronometer)findViewById(R.id.chrono);

        statusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chrono.setBase(SystemClock.elapsedRealtime()+time);
                chrono.start();
                if (!isRecording) {

                    fft = new DoubleFFT_1D(FRAMES_PER_BUFFER);
                    sampleShortBuffer = new short[FRAMES_PER_BUFFER];
                    sampleDoubleBuffer = new double[FRAMES_PER_BUFFER];


                    statusBtn.setImageResource(R.drawable.stoprec);
                    isRecording = true;
                    audioRecord.startRecording();
                    mRecording = getFile("raw");
                    startBufferedWrite(mRecording);

                    recordingThread = new Thread(new Runnable() {
                        public void run() {
                            processAudioData();
                        }
                    }, "AudioRecorder Thread");
                    recordingThread.start();
                    freq.setText(""+"Hz");


                    //change Text
                    rec.setText("Push to Stop");
                    rec.setTextColor(Color.parseColor("#FFFF002A"));


                } else {
                    statusBtn.setImageResource(R.drawable.ic_record_audio);
                    chrono.stop();
                    if (null != audioRecord) {
                        isRecording = false;
                        try {
                            recordingThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        recordingThread = null;
                        audioRecord.stop();
                        //change Text
                        rec.setText("Push to Start");
                        rec.setTextColor(Color.parseColor("#ff00ff01"));

                    }
                }
            }

        });
    }

    private void convertToDouble(short[] input, double[] output){
        double scale = 1 / 32768.0;
        for(int i = 0; i < input.length; i++){
            output[i] = input[i] * scale;
        }
    }

    private void processAudioData() {

        while (isRecording) {
            // gets the voice output from microphone to byte format
            freqRecord.read(sampleShortBuffer, 0, FRAMES_PER_BUFFER);

            convertToDouble(sampleShortBuffer, sampleDoubleBuffer);
            fft.realForward(sampleDoubleBuffer);


        }
    }

    @Override
    public void onDestroy() {
        audioRecord.release();
        super.onDestroy();
    }

    private void startBufferedWrite(final File file) {
        buffer = new short[bufferSize];
        final ProgressBar pB = (ProgressBar) findViewById(R.id.volume);

        new Thread(new Runnable() {
            @Override
            public void run() {
                DataOutputStream output = null;
                try {
                    output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
                    while (isRecording) {
                        double sum = 0;
                        int readSize = audioRecord.read(buffer, 0, buffer.length);
                        for (int i = 0; i < readSize; i++) {
                            output.writeShort(buffer[i]);
                            sum += buffer[i] * buffer[i];
                        }
                        if (readSize > 0) {
                            final double amplitude = sum / readSize;
                            pB.setProgress((int) Math.sqrt(amplitude));

                        }
                    }
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    pB.setProgress(0);
                    if (output != null) {
                        try {
                            output.flush();
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                        } finally {
                            try {
                                output.close();
                            } catch (IOException e) {
                                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    }
                }

            }
        }).start();

    }
   private File getFile(final String suffix) {
        Time time = new Time();
        time.setToNow();
        return new File(Environment.getExternalStorageDirectory(), time.format("%Y%m%d%H%M%S") + "." + suffix);
    }


}





















