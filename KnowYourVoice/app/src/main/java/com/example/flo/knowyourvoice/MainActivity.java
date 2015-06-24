package com.example.flo.knowyourvoice;


import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Time;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class MainActivity extends Activity {

    private static final int SAMPLERATE = 16000;
    private File mRecording;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private short[] buffer ;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initRecorder();

        final ImageButton statusBtn = (ImageButton)findViewById(R.id.status);

        final TextView rec = (TextView)findViewById(R.id.recText);

        final TextView freq = (TextView)findViewById(R.id.freq);

        final TextView pitch = (TextView)findViewById(R.id.pitch);

        statusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    statusBtn.setImageResource(R.drawable.stoprec);
                    isRecording = true;
                    audioRecord.startRecording();
                    mRecording = getFile("raw");
                    startBufferedWrite(mRecording);

                    //change Text
                    rec.setText("Stop Recording!");
                    pitch.setText("Pitch:");
                    freq.setText("Hz");

                }else{
                    statusBtn.setImageResource(R.drawable.ic_record_audio);

                    isRecording = false;
                    audioRecord.stop();
                    File waveFile = getFile("wav");
                    try {
                        rawToWave(mRecording, waveFile);
                    } catch (IOException e) {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    Toast.makeText(MainActivity.this, "Gespeichert als " + waveFile.getName(),
                            Toast.LENGTH_SHORT).show();

                    //change Text
                    rec.setText("Start Recording!");
                    pitch.setText("");
                    freq.setText("");

                }
            }

        });


    }

    @Override
    public void onDestroy() {
        audioRecord.release();
        super.onDestroy();
    }

    private void initRecorder() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLERATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        buffer = new short[bufferSize];
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLERATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);
    }

    private void startBufferedWrite(final File file) {

       final ProgressBar pB = (ProgressBar)findViewById(R.id.volume);


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


    private void rawToWave(final File rawFile, final File waveFile) throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, SAMPLERATE); // sample rate
            writeInt(output, SAMPLERATE * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }
            output.write(bytes.array());
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private File getFile(final String suffix) {
        Time time = new Time();
        time.setToNow();
        return new File(Environment.getExternalStorageDirectory(), time.format("%Y%m%d%H%M%S") + "." + suffix);
    }

    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }
}














