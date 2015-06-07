package com.example.flo.knowyourvoice;


import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import org.jtransforms.fft.DoubleFFT_1D;

import java.io.DataOutputStream;


public class MainActivity extends Activity {

    private static final int SAMPLERATE = 32000;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int FRAMES_PER_BUFFER = 2048; // want to play 2048 (2K) since 2 bytes we use only 1024
    private static final int BYTES_PER_SAMPLE = 16; // 2 bytes in 16bit format
    private static final DataOutputStream output = null;
    private AudioRecord audioRecord = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;


    private int bufferSizeInBytes;

    private short sampleShortBuffer[];


    // audio output of recorded data (only use with headphones!)
    private AudioTrack audioTrack;
    private double sampleDoubleBuffer[];

    // http://wendykierp.github.io/JTransforms/apidocs/
    private DoubleFFT_1D fft;

    int bufferSize = AudioRecord.getMinBufferSize(SAMPLERATE,
            AudioFormat.CHANNEL_IN_MONO, AUDIO_ENCODING);




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        configUI();
        //showVolume();


    }

        private void configUI(){
            final ImageButton recBtn = (ImageButton)findViewById(R.id.Rec);
            final ImageButton stopRecBtn = (ImageButton)findViewById(R.id.Stop);
            final TextView rec = (TextView)findViewById(R.id.recText);
            final TextView pitch = (TextView)findViewById(R.id.pitch);
            final TextView freq = (TextView)findViewById(R.id.freq);


            recBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();


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

                stopRecording();
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

    private void startRecording() {
        fft = new DoubleFFT_1D(FRAMES_PER_BUFFER);
        sampleShortBuffer = new short[FRAMES_PER_BUFFER];
        sampleDoubleBuffer = new double[FRAMES_PER_BUFFER];




        bufferSizeInBytes = FRAMES_PER_BUFFER * BYTES_PER_SAMPLE;
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLERATE, AudioFormat.CHANNEL_IN_MONO,
                AUDIO_ENCODING, bufferSizeInBytes);
        audioRecord.startRecording();
        isRecording = true;


        //output recorded audio data
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO,
                AUDIO_ENCODING, bufferSizeInBytes, AudioTrack.MODE_STREAM);
        audioTrack.play();

        recordingThread = new Thread(new Runnable() {
            public void run() {
                processAudioData();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void convertToDouble(short[] input, double[] output){
        double scale = 1 / 32768.0;
        for(int i = 0; i < input.length; i++){
            output[i] = input[i] * scale;
        }
    }

/* Lautstärkeausgabe: http://developer.samsung.com/technical-doc/view.do?v=T000000086
private void showVolume() {
    final ProgressBar pB = (ProgressBar)findViewById(R.id.volume);
    short[] buffer = new short[8000];
    while (isRecording) {

        double sum = 0;
        int readSize = audioRecord.read(sampleShortBuffer, 0, sampleShortBuffer.length);
        for (int z = 0; z < readSize; z++) {
            try {
                output.writeShort(buffer[z]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            sum += buffer [z] * buffer[z];

        }
        if (readSize > 0) {
            final double ampl = sum / readSize;
            pB.setProgress((int) Math.sqrt(ampl));

        }

    }
}*/






    private void processAudioData() {
        while (isRecording) {
            // gets the voice output from microphone to byte format

            audioRecord.read(sampleShortBuffer, 0, FRAMES_PER_BUFFER);
            // output recorded audio data
            audioTrack.write(sampleShortBuffer, 0, FRAMES_PER_BUFFER);

            convertToDouble(sampleShortBuffer, sampleDoubleBuffer);
            fft.realForward(sampleDoubleBuffer);
        }
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != audioRecord) {
            isRecording = false;
            try {
                recordingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            recordingThread = null;


            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;

            // stop audio output
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }


}
