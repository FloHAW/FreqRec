/*
HAW Hamburg
Media Systems
SoSe 15
Mobile Systeme
Projekt: FreqRec

von Florian Langhorst & Maximilian Schön

FreqRec dient zur Aufnahme&Analyse von Audiofrequenzen

Vorgehensweise:

1. Start der App
2. Drücken des Aufnahmebuttons
3. Aufnahme des Tones für ca. 3 Sekunden
4. Drücken des Stoppbuttons
5. Gemessene Frequenz erscheint

Quellen: Progressbar: http://developer.samsung.com/technical-doc/view.do?v=T000000086
         FFT:http://wendykierp.github.io/JTransforms/apidocs/

 */
package com.example.flo.freqrec;
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
    private int maxValue = 0;
    private int maxIndex = 0;
    private int frequency = 0;

    private Chronometer chrono;
    private long time = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buffer = new short[bufferSize];

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize); // Recording via microphone

        final TextView rec = (TextView) findViewById(R.id.recText); // Start/Stop Text
        final TextView freq = (TextView) findViewById(R.id.freq);   // Result in Hz
        final ProgressBar pB = (ProgressBar) findViewById(R.id.volume); // Voice Volume
        chrono =(Chronometer)findViewById(R.id.chrono); // Stopwatch, which shows the time while recording

        final ImageButton statusBtn = (ImageButton) findViewById(R.id.status); // Button to start and stop recording
        statusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chrono.setBase(SystemClock.elapsedRealtime()+time); // Reset of the chronometer
                chrono.start(); //Stopwatch starts


                if (!isRecording) {

                    fft = new DoubleFFT_1D(FRAMES_PER_BUFFER);
                    sampleShortBuffer = new short[FRAMES_PER_BUFFER];
                    sampleDoubleBuffer = new double[FRAMES_PER_BUFFER];
                    freq.setText(""); // Reset frequency-text before new measuring starts
                    statusBtn.setImageResource(R.drawable.stoprec); // Change to Stop-Button
                    isRecording = true;
                    audioRecord.startRecording();// Microphone starts recording

                    recordingThread = new Thread(new Runnable() { //


                        @Override
                        public void run() {

                            try {
                                audioRecord.read(sampleShortBuffer, 0, FRAMES_PER_BUFFER); // Gets microphone output to byte format
                                convertToDouble(sampleShortBuffer, sampleDoubleBuffer); // Calling the function convertToDouble (see below)
                                fft.realForward(sampleDoubleBuffer); //

                                while (isRecording) {

                                    // Reads in from the recorded buffer and calculates the amplitude to set the progress to the progressbar
                                    double sum = 0;
                                    int readSize = audioRecord.read(buffer, 0, buffer.length);
                                    for (int i = 0; i < readSize; i++) {
                                        sum += buffer[i] * buffer[i];
                                    }
                                    if (readSize > 0) {
                                        final double amplitude = sum / readSize;
                                        pB.setProgress((int) Math.sqrt(amplitude));
                                    }

                                    //Calculation to get the magnitude of the spectrum
                                    for (int i = 0; i < (FRAMES_PER_BUFFER / 2) - 1; i++) {
                                        double real = (2 * sampleDoubleBuffer[i]);
                                        double imag = (2 * sampleDoubleBuffer[i] + 1);
                                        magnitude[i] = Math.sqrt(real * real + imag * imag);
                                    }
                                    // Calculation to find the largest peak tin the spectrum
                                    for (int i = 0; i < (FRAMES_PER_BUFFER / 2) - 1; i++) {
                                        if (magnitude[i] > maxValue) {
                                            maxValue = (int) magnitude[i];
                                            maxIndex = i;

                                        }
                                    }
                                }

                            }finally {
                                frequency = (SAMPLE_RATE * maxIndex) / 2048; // converts the largest peak to get the frequency
                                maxValue=0; // Reset maxValue
                                pB.setProgress(0); // Reset the ProgressBar


                            }

                        };

                    });
                    recordingThread.start();
                    rec.setText("Stop");  //Change info-text
                    rec.setTextColor(Color.parseColor("#FFFF002A"));// Change text-color


                } else { // isRecording = false

                    statusBtn.setImageResource(R.drawable.ic_record_audio); //Changes to Record Button
                    chrono.stop(); // Chronometer stops
                    if (null != audioRecord) {
                        isRecording = false;
                        try {
                            recordingThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        recordingThread = null;
                        audioRecord.stop(); // Stop recording

                        rec.setText("Start");//Change Text
                        rec.setTextColor(Color.parseColor("#ff00ff01")); // Change text-color
                        freq.setText(frequency+" "+"Hz"); // Output of the recorded result

                    }
                }
            }

        });
    }

    //Converting the recorded shortbuffer to double values
    private void convertToDouble(short[] input, double[] output){
        double scale = 1 / 32768.0;
        for(int i = 0; i < input.length; i++){
            output[i] = input[i] * scale;
        }
    }
    //Finishing the activity
    @Override
    public void onDestroy() {
        audioRecord.release();

        super.onDestroy();
    }

}





















