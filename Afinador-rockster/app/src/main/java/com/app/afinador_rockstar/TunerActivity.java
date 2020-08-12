package com.app.afinador_rockstar;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TunerActivity extends AppCompatActivity {

    private static final String STATE_NEEDLE_POS = "needle_pos";
    private static final String STATE_PITCH_INDEX = "pitch_index";
    private static final String STATE_LAST_FREQ = "last_freq";
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 443;

    private Tuning mTuning;
    private AudioProcessor mAudioProcessor;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private NeedleView mNeedleView;
    private TuningView mTuningView;
    private TextView mFrequencyView;
    private boolean borderGreen = false;
    private boolean borderRed = false;
    private boolean mProcessing = false;

    private int mPitchIndex;
    private double mLastFreq;

    @Override
    protected void onStart() {
        super.onStart();
        if(Utils.checkPermission(this)) {
            startAudioProcessing();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mProcessing) {
            mAudioProcessor.stop();
            mProcessing = false;
        }
    }

    private void requestPermissions() {
        if (!Utils.checkPermission(this)) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {

                DialogUtils.showPermissionDialog(this, getString(R.string.permission_record_audio), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(TunerActivity.this,
                                new String[]{Manifest.permission.RECORD_AUDIO},
                                PERMISSION_REQUEST_RECORD_AUDIO);
                    }
                });

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        PERMISSION_REQUEST_RECORD_AUDIO);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startAudioProcessing();
                }
                break;
            }
        }
    }

    private void startAudioProcessing() {
        if (mProcessing)
            return;

        mAudioProcessor = new AudioProcessor();
        mAudioProcessor.init();
        mAudioProcessor.setPitchDetectionListener(new AudioProcessor.PitchDetectionListener() {
            @Override
            public void onPitchDetected(final float freq, final boolean standBy) {

                final int index = mTuning.closestPitchIndex(freq);
                final Pitch pitch = mTuning.pitches[index];
                double interval = 1200 * Utils.log2(freq / pitch.frequency); // interval in cents
                final float needlePos = (float) (interval / 100);
                final boolean goodPitch = Math.abs(interval) < 5.0;

                runOnUiThread(new Runnable() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public void run() {
                        mTuningView.setSelectedIndex(index, true);
                        mNeedleView.setTickLabel(0.0F, String.format("%.02f Hz", pitch.frequency));
                        mNeedleView.animateTip(needlePos);
                        mFrequencyView.setText(String.format("%.02f Hz", freq));

                        View linearLayout = findViewById(R.id.linearLayout);
                        mNeedleView = (NeedleView) findViewById(R.id.pitch_needle_view);

                        if(!standBy) {
                            if (goodPitch && !borderGreen) {
                                Utils.tuneful(linearLayout, mNeedleView);
                                borderGreen = true;
                                borderRed = false;

                            } else if (!goodPitch && !borderRed) {
                                Utils.tuneless(linearLayout, mNeedleView);
                                borderRed = true;
                                borderGreen = false;
                            }
                        } else {
                            Utils.restoreDefaultColors(getApplicationContext(), linearLayout, mNeedleView);
                            borderGreen = false;
                            borderRed = false;
                        }
                    }
                });
                mPitchIndex = index;
                mLastFreq = freq;
            }
        });
        mProcessing = true;
        mExecutor.execute(mAudioProcessor);
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setupActivityTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mTuning = Tuning.getTuning(this, Preferences.getString(this, getString(R.string.pref_tuning_key), getString(R.string.standard_tuning_val)));

        mNeedleView = (NeedleView) findViewById(R.id.pitch_needle_view);
        mNeedleView.setTickLabel(-1.0F, "-");
        mNeedleView.setTickLabel(0.0F, String.format("%.02f Hz", mTuning.pitches[0].frequency));
        mNeedleView.setTickLabel(1.0F, "+");

        mTuningView = (TuningView) findViewById(R.id.tuning_view);
        mTuningView.setTuning(mTuning);

        mFrequencyView = (TextView) findViewById(R.id.frequency_view);
        mFrequencyView.setText(String.format("%.02f Hz", mTuning.pitches[0].frequency));

        requestPermissions();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putFloat(STATE_NEEDLE_POS, mNeedleView.getTipPos());
        outState.putInt(STATE_PITCH_INDEX, mPitchIndex);
        outState.putDouble(STATE_LAST_FREQ, mLastFreq);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onResume(){
        if(borderGreen || borderRed) {
            View linearLayout = findViewById(R.id.linearLayout);
            Utils.restoreDefaultColors(getApplicationContext(), linearLayout, mNeedleView);
            borderGreen = false;
            borderRed = false;
        }
            super.onResume();
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mNeedleView.setTipPos(savedInstanceState.getFloat(STATE_NEEDLE_POS));
        int pitchIndex = savedInstanceState.getInt(STATE_PITCH_INDEX);
        mNeedleView.setTickLabel(0.0F, String.format("%.02f Hz", mTuning.pitches[pitchIndex].frequency));
        mTuningView.setSelectedIndex(pitchIndex);
        mFrequencyView.setText(String.format("%.02f Hz", savedInstanceState.getFloat(STATE_LAST_FREQ)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                NavUtils.showSettingsActivity(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }
}