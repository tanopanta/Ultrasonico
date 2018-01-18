package com.example.tattata.ultrasonico;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int SAMPLING_RATE = 44100;
    static final double SAMPLING_PERIOD = 1.0 / SAMPLING_RATE;
    //static final int FFT_POINT = 8192;
    static final int FC = 30;
    //static final double BASELINE = Math.pow(2, 15) * FFT_POINT * 2;//測定可能な最大振幅？ 16bit*FFT*2
    private static final int WAVE_AMP = 8000;

    static final int GET_PEAKS = 3;

    int bufSize;
    boolean isRecording = false;
    AudioRecord audioRecord;
    AudioTrack audioTrack;
    Thread thread;
    DoubleFFT_1D fft;
    short buf[];
    double amp[];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bufSize = AudioRecord.getMinBufferSize(SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if(SAMPLING_RATE / 10 < bufSize) {
            Log.v("adasaadada", "bufsizeがおかしくないですか？");
        } else {
            bufSize = SAMPLING_RATE / 10;
        }
        fft = new DoubleFFT_1D(bufSize);

        int trackBuffSize = AudioTrack.getMinBufferSize(
                SAMPLING_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT)
                * 2;
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLING_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                trackBuffSize,
                AudioTrack.MODE_STREAM);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                short[] buf = new short[SAMPLING_RATE/10];
                mixWave(buf, 880, WAVE_AMP);
                audioTrack.write(buf, 0, SAMPLING_RATE/10);
            }
        });

    }
    public void mixWave(short[] buf, int freq, int amp) {
        for(int i = 0; i < buf.length; i++) {
            buf[i] += amp * Math.sin(2 * Math.PI * freq * i * SAMPLING_PERIOD);
        }
    }
    /*
    public double amplitudeToDecibel(double data) {
        return  20 * Math.log10(data / BASELINE);
    }
    */
    public int indexToFrequency(int index) {
        return (int)((SAMPLING_RATE / (double) bufSize) * index);
    }
    public int frequencyToIndex(int freq) {
        return (int)(freq / (SAMPLING_RATE / (double) bufSize));
    }

    public void peak(double[] data, int number) {
        int[] index = new int[number];

        for(int i = 1; i < data.length-1; i++) {
            if(data[i-1] < data[i] && data[i] > data[i+1]) {//波の頂点
                for(int j = 0; j < number; j++) {
                    if(data[i] > data[index[j]]) {
                        index[j] = i;
                        break;
                    }
                }
            }
        }
        for(Integer i: index) {
            if(frequencyToIndex(880)-2 < i && i < frequencyToIndex(880)+2) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "らーーーーーーーーーーーーー", Toast.LENGTH_SHORT)
                                .show();
                    }
                });
            }
            Log.d("aaaaaadasd", "" + indexToFrequency(i));
        }
        Log.d("aaaaaadasd", "-------------");
    }
    @Override
    protected void onResume() {
        super.onResume();
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize);
        audioRecord.startRecording();
        isRecording = true;

        audioTrack.play();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                buf = new short[bufSize];
                long nowTime = System.currentTimeMillis();
                long tmpTime = nowTime;
                while (isRecording) {
                    nowTime = System.currentTimeMillis();
                    Log.v("timeime", String.valueOf(nowTime - tmpTime));
                    tmpTime = nowTime;

                    audioRecord.read(buf, 0, buf.length);


                    double d[] = new double[bufSize];
                    for(int i = 0; i < bufSize; i++) {
                        d[i] = (double)buf[i];
                    }
                    fft.realForward(d);

                    //実部と虚部が交互にある。そこから振幅を求める
                    amp = new double[d.length/2];
                    for(int i = 0; i < d.length; i+=2) {
                        amp[i / 2] = Math.abs(d[i]) + Math.abs(d[i + 1]);
                    }
                    for(int i = 0; i < frequencyToIndex(FC); i++) {
                        amp[i] = 1;
                    }

                    peak(amp, GET_PEAKS);
                }
                // 録音停止
                audioRecord.stop();
                audioRecord.release();
            }
        });
        thread.start();
    }
    @Override
    protected void onPause() {
        super.onPause();
        isRecording = false;
        if(audioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
            audioTrack.stop();
            audioTrack.flush();
        }
    }
}
