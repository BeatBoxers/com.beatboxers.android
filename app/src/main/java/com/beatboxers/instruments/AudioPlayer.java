package com.beatboxers.instruments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.SoundPool;
import android.preference.PreferenceManager;
import android.util.Log;

import com.beatboxers.Broadcasts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AudioPlayer {
    private final static String LOG_TAG = "bb_" + AudioPlayer.class.getSimpleName();
    public static final String DISABLE_DOUBLE_HIT = "disableDoubleHit";

    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private Instruments mInstruments;
    private Map<Integer, Integer> mPadToSoundMap = new HashMap<>();
    Map<Integer, Long> durations = new HashMap<>();
    Map<Integer, Long> lastStartsMap = new HashMap<>();

    private SoundPool mSoundPool;

    static private AudioPlayer mSharedInstance;

    //loopback variables
    private ArrayList<LoopedInstrument> mLoopedInstruments = new ArrayList<>();
    private LoopbackPlayerThread mLoopbackPlayerThread;
    private long mLastHitTime = 0;
    private boolean mIsRecording = false;

    static public AudioPlayer sharedInstance() throws UnsetVariableException {
        if (null == mSharedInstance) {
            throw new UnsetVariableException();
        }

        return mSharedInstance;
    }

    public AudioPlayer(Context context) {
        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        mInstruments = Instruments.init(context);
        mSoundPool = new SoundPool(50, AudioManager.STREAM_MUSIC, 0);

        loadSamples(context);

        //play nothing to fix a bug in soundpool
        mSoundPool.play(-914, 0, 0, 1, -1, 1f);

        mSharedInstance = this;
    }

    private void loadSamples(Context context) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();

        for (Instrument instrument : mInstruments.getInstruments()) {
            if (instrument.sampleId != -1) {
                int instrumentId = loadAndSaveSampleDuration(context, mmr, instrument.sampleId);
                mPadToSoundMap.put(instrument.instrumentid, instrumentId);
            }
        }

        mmr.release();
    }

    private int loadAndSaveSampleDuration(Context context, MediaMetadataRetriever mmr, int resourceId) {
        AssetFileDescriptor afd = context.getResources().openRawResourceFd(resourceId);
        mmr.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long durationMs = 0;
        try {
            durationMs = Long.parseLong(duration);
        } catch (NumberFormatException e) {
            Log.w(LOG_TAG, e);
        }

        int loadedResourceId = mSoundPool.load(context, resourceId, 1);
        durations.put(loadedResourceId, durationMs);
        return loadedResourceId;
    }

    public void play(int instrumentid) {
        if (doubleHitPreventionRequired(instrumentid)) {
            return;
        }
        float volume = 1.0f;
        Instrument instrument = mInstruments.getInstrument(instrumentid);
        if (instrument.isLoopback()) {
            //if we are not recording but we are playing, kill the player thread and reset all variables
            if (null != mLoopbackPlayerThread) {
                Log.d(LOG_TAG, "loopback stop received");

                killLoopbackPlayerThread();
                return;
            }

            mIsRecording = !mIsRecording;

            //start recording
            if (mIsRecording) {
                Log.d(LOG_TAG, "started recoding for loopback");

                sendLoopbackBroadcast(Broadcasts.ACTION_LOOPBACK_RECORDING);

                mLastHitTime = System.currentTimeMillis();
            } else {
                //stop recording and start playing back the loop
                if (mLoopedInstruments.size() > 0) {
                    Log.d(LOG_TAG, "start loopback thread");

                    //add a disabled instrument to the end to reflect the pause from the last instrument hit till this one
                    addLoopedInstrument(Instruments.sharedInstance().getDisabledId());
                    mLastHitTime = 0;

                    //start the loop play thread
                    mLoopbackPlayerThread = new LoopbackPlayerThread();
                    mLoopbackPlayerThread.start();

                    sendLoopbackBroadcast(Broadcasts.ACTION_LOOPBACK_PLAY_STARTED);
                } else {
                    sendLoopbackBroadcast(Broadcasts.ACTION_LOOPBACK_STOPPED);
                }
            }
        } else {
            if (instrument.sampleId != -1) {
                mSoundPool.play(mPadToSoundMap.get(instrument.instrumentid), volume, volume, 1, 0, 1f);
            }

            if (!instrument.isLoopback() && mIsRecording) {
                //record it to the list
                addLoopedInstrument(instrumentid);
            }
        }
    }

    private boolean doubleHitPreventionRequired(int instrumentId) {
        boolean doubleHitDisabled = mSharedPreferences.getBoolean(DISABLE_DOUBLE_HIT, false);
        if (!doubleHitDisabled) {
            return false;
        }
        if (Instruments.sharedInstance().getInstrument(instrumentId).isLoopback()) {
            return false;
        }
        Long lastStarted = lastStartsMap.get(instrumentId);
        Long sampleDuration = durations.get(instrumentId);
        long currentTime = System.currentTimeMillis();

        if (lastStarted == null || currentTime > lastStarted + sampleDuration) {
            lastStartsMap.put(instrumentId, currentTime);
            return false;
        }
        return true;
    }

    private void sendLoopbackBroadcast(String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        mContext.sendBroadcast(intent);
    }

    private void addLoopedInstrument(int instrumentid) {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastHit = currentTime - mLastHitTime;
        mLastHitTime = currentTime;

        mLoopedInstruments.add(new LoopedInstrument(instrumentid, timeSinceLastHit));
    }

    public void killLoopbackPlayerThread() {
        if (null != mLoopbackPlayerThread) {
            mLoopbackPlayerThread.kill();
            mLoopbackPlayerThread = null;
        }

        mIsRecording = false;
        mLastHitTime = 0;
        mLoopedInstruments.clear();

        sendLoopbackBroadcast(Broadcasts.ACTION_LOOPBACK_STOPPED);
    }

    private class LoopedInstrument {
        public int instrumentid;
        public long delayBeforePlay;

        public LoopedInstrument(int instrumentid, long delayBeforePlay) {
            this.instrumentid = instrumentid;
            this.delayBeforePlay = delayBeforePlay;
        }
    }

    private class LoopbackPlayerThread extends Thread {
        private boolean mmIsPlaying = true;

        public void kill() {
            mmIsPlaying = false;
        }

        public void run() {
            Log.v(LOG_TAG, "loopbackPlayerThread started");

            while (mmIsPlaying) {
                try {
                    for (LoopedInstrument loopedInstrument : mLoopedInstruments) {
                        if (!mmIsPlaying) {
                            break;
                        }

                        sleep(loopedInstrument.delayBeforePlay);

                        //also check here in case the thread was stopped while sleeping
                        if (!mmIsPlaying) {
                            break;
                        }

                        play(loopedInstrument.instrumentid);
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    break;
                }
            }

            mmIsPlaying = false;//if we break out due to an exception, we should still make sure we are false here
            Log.v(LOG_TAG, "loopbackPlayerThread stopped");
        }
    }
}