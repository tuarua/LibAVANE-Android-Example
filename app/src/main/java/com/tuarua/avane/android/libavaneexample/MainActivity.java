package com.tuarua.avane.android.libavaneexample;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.lylc.widget.circularprogressbar.CircularProgressBar;
import com.tuarua.avane.android.LibAVANE;
import com.tuarua.avane.android.Progress;
import com.tuarua.avane.android.constants.LogLevel;
import com.tuarua.avane.android.events.Event;
import com.tuarua.avane.android.events.IEventHandler;
import com.tuarua.avane.android.gets.AvailableFormat;
import com.tuarua.avane.android.gets.BitStreamFilter;
import com.tuarua.avane.android.gets.Codec;
import com.tuarua.avane.android.gets.Color;
import com.tuarua.avane.android.gets.Decoder;
import com.tuarua.avane.android.gets.Device;
import com.tuarua.avane.android.gets.Encoder;
import com.tuarua.avane.android.gets.Filter;
import com.tuarua.avane.android.gets.HardwareAcceleration;
import com.tuarua.avane.android.gets.Layouts;
import com.tuarua.avane.android.gets.PixelFormat;
import com.tuarua.avane.android.gets.Protocols;
import com.tuarua.avane.android.gets.SampleFormat;
import com.tuarua.avane.android.libavaneexample.utils.TextUtils;
import com.tuarua.avane.android.libavaneexample.utils.TimeUtils;
import com.tuarua.avane.android.probe.Probe;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private LibAVANE libAVANE;
    private String appDirectory;

    private CircularProgressBar progressCircle;
    private Double duration;
    private Button btn;
    private TextView tv3;
    private DecimalFormat percentFormat1D;
    private DecimalFormat percentFormat2D;

    private boolean isWorking = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        percentFormat1D = new DecimalFormat("0.0");
        percentFormat1D.setRoundingMode(RoundingMode.UP);

        percentFormat2D = new DecimalFormat("0.00");
        percentFormat2D.setRoundingMode(RoundingMode.UP);

        libAVANE = LibAVANE.getInstance();
        libAVANE.setLogLevel(LogLevel.INFO);

        TextView tv = (TextView) findViewById(R.id.textView);
        tv.setText(libAVANE.getVersion());

        TextView tv2 = (TextView) findViewById(R.id.textView2);
        tv2.setText("http://download.blender.org/durian/trailer/sintel_trailer-1080p.mp4");

        tv3 = (TextView) findViewById(R.id.textView3);

        Log.i("build config",libAVANE.getBuildConfiguration());

        PackageManager m = getPackageManager();
        appDirectory = getPackageName();
        PackageInfo p = null;
        try {
            p = m.getPackageInfo(appDirectory, 0);
            appDirectory = p.applicationInfo.dataDir; // /data/user/0/com.tuarua.avane.android.libavaneexample
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        progressCircle = (CircularProgressBar) findViewById(R.id.circularprogressbar);
        progressCircle.setTitle("0%");
        progressCircle.setSubTitle("");
        progressCircle.setMax(360);
        progressCircle.setProgress(0);

        btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(isWorking){
                            libAVANE.cancelEncode();
                        }else{
                            isWorking = true;
                            btn.setEnabled(false);
                            triggerProbe();
                        }
                    }
                });


        libAVANE.eventDispatcher.addEventListener(Event.TRACE, new IEventHandler(){
            @Override
            public void callback(Event event) {
                String msg = (String) event.getParams();
                Log.i("MA trace",msg);
            }
        });
        libAVANE.eventDispatcher.addEventListener(Event.INFO, new IEventHandler(){
            @Override
            public void callback(Event event) {
                String msg = (String) event.getParams();
                Log.i("MA info",msg);
            }
        });
        libAVANE.eventDispatcher.addEventListener(Event.ON_ENCODE_START, new IEventHandler(){
            @Override
            public void callback(Event event) {
                String msg = (String) event.getParams();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.progressCircle.setVisibility(View.VISIBLE);
                        MainActivity.this.btn.setText("Cancel");
                        MainActivity.this.btn.setEnabled(true);
                    }
                });

                Log.i("MA","encode start");
            }
        });
        libAVANE.eventDispatcher.addEventListener(Event.ON_ENCODE_FINISH, new IEventHandler(){
            @Override
            public void callback(Event event) {
                String msg = (String) event.getParams();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.progressCircle.setProgress(360);
                        MainActivity.this.progressCircle.setTitle("100%");
                        MainActivity.this.btn.setText("Encode");
                        MainActivity.this.btn.setEnabled(true);
                    }
                });
                isWorking = false;
                Log.i("MA","encode finish");
            }
        });

        libAVANE.eventDispatcher.addEventListener(Event.ON_ENCODE_PROGRESS, new IEventHandler() {
            @Override
            public void callback(Event event) {
                final Progress progress = (Progress) event.getParams();
                final Double percent = (progress.secs + (progress.us/100))/duration;
                final int degrees = (int) Math.round(percent * 360);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.progressCircle.setProgress(degrees);
                        MainActivity.this.tv3.setText("");
                        MainActivity.this.tv3.append(String.format("\ntime: %s",
                                TimeUtils.secsToTimeCode(Double.valueOf((progress.secs + progress.us/100)))  + " / " + TimeUtils.secsToTimeCode(MainActivity.this.duration)   ));
                        MainActivity.this.tv3.append(String.format("\nspeed: %s", String.valueOf(percentFormat2D.format(progress.speed)) + "x"));
                        MainActivity.this.tv3.append(String.format("\nfps: %s", String.valueOf(percentFormat2D.format(progress.fps))));
                        MainActivity.this.tv3.append(String.format("\nbitrate: %s", String.valueOf(percentFormat2D.format(progress.bitrate)) +" Kbps" ));
                        MainActivity.this.tv3.append(String.format("\nframe: %s", String.valueOf(progress.frame)));
                        MainActivity.this.tv3.append(String.format("\nsize: %s", TextUtils.bytesToString(progress.size*1024)));
                        MainActivity.this.progressCircle.setTitle(percentFormat1D.format(percent * 100) + "%");
                    }
                });
            }
        });

        libAVANE.eventDispatcher.addEventListener(Event.ON_PROBE_INFO, new IEventHandler() {
            @Override
            public void callback(Event event) {
                Log.i("MA","ON_PROBE_INFO");
            }
        });

        libAVANE.eventDispatcher.addEventListener(Event.ON_PROBE_INFO_AVAILABLE, new IEventHandler() {
            @Override
            public void callback(Event event) {
                Log.i("MA","ON_PROBE_INFO_AVAILABLE");
                Probe probe = libAVANE.getProbeInfo();

                duration = probe.format.duration;
                Log.i("MA","PROBE DONE");
                Log.i("MA","CALLING encode");
                doEncode();

            }
        });

        libAVANE.eventDispatcher.addEventListener(Event.NO_PROBE_INFO, new IEventHandler() {
            @Override
            public void callback(Event event) {
                Log.i("MA","NO PROBE INFO");
            }
        });


    }

    private void triggerProbe(){
        libAVANE.triggerProbeInfo("http://download.blender.org/durian/trailer/sintel_trailer-1080p.mp4");
    }


    private void doEncode(){

        String[] params = libAVANE.cliParse("-i " +
                "http://download.blender.org/durian/trailer/sintel_trailer-1080p.mp4 " +
                "-c:v libx264 -crf 22 -c:a copy -preset ultrafast -y "
                + appDirectory + "/files/avane-encode-classic.mp4",true);

        libAVANE.encode(params);
    }
    private void getInfo(){
        /*
        ArrayList<Color> clrs = libAVANE.getColors();
        Log.i("num colors",String.valueOf(clrs.size()));
        */

        /*
        ArrayList<PixelFormat> fltrs = libAVANE.getPixelFormats();
        Log.i("num flters",String.valueOf(fltrs.size()));
        */

        //Layouts layouts = libAVANE.getLayouts();
        //Protocols protocols = libAVANE.getProtocols();
        //Log.i("num inputs",String.valueOf(protocols.inputs.size()));
        //Log.i("num outputs",String.valueOf(protocols.outputs.size()));

        /*
        ArrayList<BitStreamFilter> bitStreamFilters = libAVANE.getBitStreamFilters();
        Log.i("num bsfs",String.valueOf(bitStreamFilters.size()));
        */

        /*
        ArrayList<Codec> codecs = libAVANE.getCodecs();
        Log.i("num codecs",String.valueOf(codecs.size()));
        */

        /*
        ArrayList<Decoder> decoders = libAVANE.getDecoders();
        Log.i("num decoders",String.valueOf(decoders.size()));
        */

        /*
        ArrayList<Encoder> encoders = libAVANE.getEncoders();
        Log.i("num encoders",String.valueOf(encoders.size()));
        */
        /*
        ArrayList<HardwareAcceleration> hwAcc = libAVANE.getHardwareAccelerations();
        Log.i("num hw accels",String.valueOf(hwAcc.size()));
        */
        /*
        ArrayList<Device> devices = libAVANE.getDevices();
        Log.i("num devices",String.valueOf(devices.size()));
        */

        /*
        ArrayList<AvailableFormat> formats = libAVANE.getAvailableFormats();
        for (AvailableFormat format : formats) {
            Log.i("format: ",format.nameLong);
        }
        Log.i("num formats",String.valueOf(formats.size()));
        */



        ArrayList<SampleFormat> formats = libAVANE.getSampleFormats();
        for (SampleFormat format : formats) {
            Log.i("format: ",format.name);
        }

        Log.i("num sample formats",String.valueOf(formats.size()));
    }

}
