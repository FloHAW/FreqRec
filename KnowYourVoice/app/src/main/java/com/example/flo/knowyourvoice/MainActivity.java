package com.example.flo.knowyourvoice;


import android.app.Activity;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.jtransforms.fft.DoubleFFT_1D;


public class MainActivity extends Activity {

    private AudioRecord audioRecord;
    private DoubleFFT_1D fft;
    private Thread recordingThread;
    private boolean isRecording = false;



    private final int SAMPLE_RATE = 44100;
    private final int FRAMES_PER_BUFFER = 1024;
    private int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT);
    private double sampleDoubleBuffer[];
    private short[] buffer, sampleShortBuffer;
    private double magnitude[] = new double [FRAMES_PER_BUFFER/2];
    private int maxVal = 0;
    private int maxIndex = -1;
    private int frequency = 0;
    private String voice;

    private Chronometer chrono;
    private long time = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buffer = new short[bufferSize];

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);


        final TextView rec = (TextView) findViewById(R.id.recText); // Infotext below the Image-Button
        final TextView freq = (TextView) findViewById(R.id.freq);   // Infotext to show the result of the record
        final ProgressBar pB = (ProgressBar) findViewById(R.id.volume);
        chrono =(Chronometer)findViewById(R.id.chrono); // Stopwatch, which shows the time while recording

        final ImageButton statusBtn = (ImageButton) findViewById(R.id.status); // Button to start and stop recording
        statusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chrono.setBase(SystemClock.elapsedRealtime()+time); // Reset of the chronometer
                chrono.start(); //Chronometer starts

                if (!isRecording) {

                    fft = new DoubleFFT_1D(FRAMES_PER_BUFFER);
                    sampleShortBuffer = new short[FRAMES_PER_BUFFER];
                    sampleDoubleBuffer = new double[FRAMES_PER_BUFFER];
                    maxVal = 0;
                    freq.setText("");
                    statusBtn.setImageResource(R.drawable.stoprec);
                    isRecording = true;
                    audioRecord.startRecording();

                    recordingThread = new Thread(new Runnable() {



                        @Override
                        public void run() {

                            try {
                                audioRecord.read(sampleShortBuffer, 0, FRAMES_PER_BUFFER); // Gets the voice output from microphone to byte format
                                convertToDouble(sampleShortBuffer, sampleDoubleBuffer);
                                fft.realForward(sampleDoubleBuffer);
                                while (isRecording) {
                                    double sum = 0;
                                    int readSize = audioRecord.read(buffer, 0, buffer.length);
                                    for (int i = 0; i < readSize; i++) {
                                        sum += buffer[i] * buffer[i];
                                    }
                                    if (readSize > 0) {
                                        final double amplitude = sum / readSize;
                                        pB.setProgress((int) Math.sqrt(amplitude));
                                    }
                                    for (int i = 0; i < (FRAMES_PER_BUFFER / 2) - 1; i++) {
                                        double real = (2 * sampleDoubleBuffer[i]);
                                        double imag = (2 * sampleDoubleBuffer[i] + 1);
                                        magnitude[i] = Math.sqrt(real * real + imag * imag);
                                    }

                                    for (int i = 0; i < (FRAMES_PER_BUFFER / 2) - 1; i++) {
                                        if (magnitude[i] > maxVal) {
                                            maxVal = (int) magnitude[i];
                                            maxIndex = i;
                                        }
                                    }
                                }

                            }finally {

                                frequency = (SAMPLE_RATE * maxIndex) / 2048;
                                /*if( frequency >=0 && frequency <= 400){
                                    voice="Bass";
                                }if(frequency > 400 && frequency <=800){
                                    voice="Bariton";
                                }if(frequency >800){
                                    voice="Tenor";
                                }*/
                                pB.setProgress(0);

                            }

                        };

                    });
                    recordingThread.start();
                    rec.setText("Push to Stop");  //change info-text
                    rec.setTextColor(Color.parseColor("#FFFF002A"));// change text-color

                } else {

                    statusBtn.setImageResource(R.drawable.ic_record_audio);
                    chrono.stop(); // Chronometer stops
                    if (null != audioRecord) {
                        isRecording = false;
                        try {
                            recordingThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        recordingThread = null;
                        audioRecord.stop();

                        rec.setText("Push to Start");//Change Text
                        rec.setTextColor(Color.parseColor("#ff00ff01")); // Change text-color
                        freq.setText(frequency+"Hz"); // Output of the recording result
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

    @Override
    public void onDestroy() {
        audioRecord.release();
        super.onDestroy();
    }

}





















