package com.example.tattata.ultrasonico;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import org.jtransforms.fft.DoubleFFT_1D;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int SAMPLING_RATE = 44100;
    static final int FFT_POINT = 4096;
    static final double BASELINE = Math.pow(2, 15) * FFT_POINT * 2;//測定可能な最大振幅？ 16bit*FFT*2
    static final int DISPLAY_INTERVAL = 1;
    int bufSize;
    boolean isRecording = false;
    AudioRecord audioRecord;
    Thread thread;
    LineChart chart;
    DoubleFFT_1D fft;
    short buf[];
    double amp[];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bufSize = AudioRecord.getMinBufferSize(SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);//ENCODING_PCM_FLOATにしようか
        if(bufSize < FFT_POINT) {
            bufSize = FFT_POINT;
        }

        chart = findViewById(R.id.chart);
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMaxValue(100);
        leftAxis.setAxisMinValue(-200);
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        fft = new DoubleFFT_1D(FFT_POINT);

    }
    public void display(double[] data) {
        List<LineDataSet> dataSets = new ArrayList<>();
        ArrayList<String> xValues = new ArrayList<>();
        ArrayList<Entry> value = new ArrayList<>();
        for(int i = 0; i < data.length/2; i+=DISPLAY_INTERVAL) {
            xValues.add(String.valueOf(indexToFrequency(i)));
            double result = amplitudeToDecibel(data[i]);
            value.add(new Entry((float)result, i / DISPLAY_INTERVAL));
        }
        LineDataSet valueDataSet = new LineDataSet(value, "sample");
        dataSets.add(valueDataSet);
        chart.setData(new LineData(xValues, dataSets));
        chart.invalidate();
    }
    public double amplitudeToDecibel(double data) {
        return  20 * Math.log10(data / BASELINE);
    }
    public int indexToFrequency(int index) {
        return (int)((SAMPLING_RATE / (double) FFT_POINT) * index);
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

                    double d[] = new double[FFT_POINT];
                    for(int i = 0; i < FFT_POINT; i++) {
                        d[i] = (double)buf[i];
                    }
                    fft.realForward(d);

                    //実部と虚部が交互にある。そこから振幅を求める
                    amp = new double[FFT_POINT/2];
                    for(int i = 0; i < FFT_POINT; i+=2) {
                        amp[i / 2] = Math.abs(d[i]) + Math.abs(d[i + 1]);
                    }

                    runOnUiThread(new Runnable() {
                        public void run() {
                            display(amp);
                        }
                    });
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
    }
}
