package com.example.tattata.ultrasonico;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.rtp.AudioCodec;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int SAMPLING_RATE = 44100;
    int bufSize;
    boolean isRecording = false;
    AudioRecord audioRecord;
    Thread thread;
    LineChart chart;
    short buf[];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bufSize = AudioRecord.getMinBufferSize(SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        chart = findViewById(R.id.chart);


    }
    public void display(short[] data) {
        List<LineDataSet> dataSets = new ArrayList<>();
        ArrayList<String> xValues = new ArrayList<>();
        ArrayList<Entry> value = new ArrayList<>();
        for(int i = 0; i < data.length; i++) {
            xValues.add(String.valueOf(i));
            value.add(new Entry(data[i], i));
        }
        LineDataSet valueDataSet = new LineDataSet(value, "sample");
        dataSets.add(valueDataSet);
        chart.setData(new LineData(xValues, dataSets));
        chart.invalidate();
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

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                buf = new short[bufSize];
                while (isRecording) {
                    audioRecord.read(buf, 0, buf.length);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            display(buf);
                        }
                    });
                }
                // 録音停止
                audioRecord.stop();
                audioRecord.release();
            }
        });
        //スレッドのスタート
        thread.start();
    }
    @Override
    protected void onPause() {
        super.onPause();
        isRecording = false;
    }
}
