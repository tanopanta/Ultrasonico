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
    static final double SAMPLING_PERIOD = 1.0 / SAMPLING_RATE;
    static final int FFT_POINT = 8192;
    static final int FC = 30;
    static final double BASELINE = Math.pow(2, 15) * FFT_POINT * 2;//測定可能な最大振幅？ 16bit*FFT*2
    private static final int WAVE_AMP = 8000;

    static final int DISPLAY_INTERVAL = 1;
    static final int GET_PEAKS = 3;

    int bufSize;
    boolean isRecording = false;
    AudioRecord audioRecord;
    AudioTrack audioTrack;
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

        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLING_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize,
                AudioTrack.MODE_STREAM);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                short[] buf = new short[SAMPLING_RATE];
                mixWave(buf, 880, WAVE_AMP);
                audioTrack.write(buf, 0, SAMPLING_RATE, AudioTrack.WRITE_NON_BLOCKING);
            }
        });

    }
    public void mixWave(short[] buf, int freq, int amp) {
        for(int i = 0; i < buf.length; i++) {
            buf[i] += amp * Math.sin(2 * Math.PI * freq * i * SAMPLING_PERIOD);
        }
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
    public int frequencyToIndex(int freq) {
        return (int)(freq / (SAMPLING_RATE / (double) FFT_POINT));
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
                    for(int i = 0; i < frequencyToIndex(FC); i++) {
                        amp[i] = 1;
                    }

                    peak(amp, GET_PEAKS);
                    /*
                    runOnUiThread(new Runnable() {
                        public void run() {
                            display(amp);
                        }
                    });
                    */
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
