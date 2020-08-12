package com.app.afinador_rockstar;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;


class AudioProcessor implements Runnable {

    private static final String TAG = AudioProcessor.class.getCanonicalName();

    private static final int[] SAMPLE_RATES = {44100, 22050, 16000, 11025, 8000};

    public interface PitchDetectionListener {
        void onPitchDetected(float freq, boolean standBy);
    }

    private float mLastComputedFreq = 0;

    private AudioRecord mAudioRecord;
    private PitchDetectionListener mPitchDetectionListener;
    private boolean mStop = false;
    private int standByCount = 0;
    private float standByFreq = 0;

    public void setPitchDetectionListener(PitchDetectionListener pitchDetectionListener) {
        mPitchDetectionListener = pitchDetectionListener;
    }

    public void init() {
        int bufSize = 16384;
        int avalaibleSampleRates = SAMPLE_RATES.length;
        int i = 0;
        do {
            int sampleRate = SAMPLE_RATES[i];
            int minBufSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (minBufSize != AudioRecord.ERROR_BAD_VALUE && minBufSize != AudioRecord.ERROR) {
                mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(bufSize, minBufSize * 4));
            }
            i++;
        }
        while (i < avalaibleSampleRates && (mAudioRecord == null || mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED));
    }

    public void stop() {
        mStop = true;
        mAudioRecord.stop();
        mAudioRecord.release();
    }

    @Override
    public void run() {

        Log.d(TAG, "sampleRate="+mAudioRecord.getSampleRate());

        if(mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord not initialized");
        }

        mAudioRecord.startRecording();
        int bufSize = 8192;
        final int sampleRate = mAudioRecord.getSampleRate();
        final short[] buffer = new short[bufSize];

        do {
            final int read = mAudioRecord.read(buffer, 0, bufSize);
            if (read > 0) {
                final double intensity = averageIntensity(buffer, read);

                int maxZeroCrossing = (int) (250 * (read / 8192) * (sampleRate / 44100.0));

                if (intensity >= 50 && zeroCrossingCount(buffer) <= maxZeroCrossing) {

                    float freq = getPitch(buffer, read / 4, read, sampleRate, 50, 500);
                    if (Math.abs(freq - mLastComputedFreq) <= 5f) {
                        mPitchDetectionListener.onPitchDetected(freq, false);
                        standByFreq = freq;
                        standByCount = 0;
                    }
                    mLastComputedFreq = freq;
                } else if(standByFreq > 0 && standByCount > 10){
                    mPitchDetectionListener.onPitchDetected(standByFreq, true);
                    standByCount = 0;
                    standByFreq = 0;
                }
                standByCount++;
            }
        } while (!mStop);

        Log.d(TAG, "Thread terminated");

    }

    private double averageIntensity(short[] data, int frames) {

        double sum = 0;
        for (int i = 0; i < frames; i++) {
            sum += Math.abs(data[i]);
        }
        return sum / frames;

    }

    private int zeroCrossingCount(short[] data) {
        int len = data.length;
        int count = 0;
        boolean prevValPositive = data[0] >= 0;
        for (int i = 1; i < len; i++) {
            boolean positive = data[i] >= 0;
            if (prevValPositive == !positive)
                count++;

            prevValPositive = positive;
        }
        return count;
    }

    private float getPitch(short[] data, int windowSize, int frames, float sampleRate, float minFreq, float maxFreq) {

        float maxOffset = sampleRate / minFreq;
        float minOffset = sampleRate / maxFreq;

        int minSum = Integer.MAX_VALUE;
        int minSumLag = 0;

        for (int lag = (int) minOffset; lag <= maxOffset; lag++) {
            int sum = 0;
            for (int i = 0; i < windowSize; i++) {

                int oldIndex = i - lag;

                int sample = ((oldIndex < 0) ? data[frames + oldIndex] : data[oldIndex]);

                sum += Math.abs(sample - data[i]);
            }

            if (sum < minSum) {
                minSum = sum;
                minSumLag = lag;
            }
        }

        return sampleRate / minSumLag;
    }
}
