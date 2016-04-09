package com.beatboxers.instruments;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

import com.beatboxers.Broadcasts;
import com.beatboxers.R;

import java.util.ArrayList;

public class AudioPlayer {
    private final static String LOG_TAG = "bb_"+AudioPlayer.class.getSimpleName();

    private Context mContext;

    private SoundPool mSoundPool;

    static private AudioPlayer mSharedInstance;

    //loopback variables
    private ArrayList<LoopedInstrument> mLoopedInstruments = new ArrayList<>();
    private LoopbackPlayerThread mLoopbackPlayerThread;
    private long mLastHitTime = 0;
    private boolean mIsRecording = false;

    //instrument variables
    private int mBass;
    private int mCrash;
    private int mFloorTom;
    private int mHighHat;
    private int mRide;
    private int mSnare;
    private int mTom1;
    private int mTom2;
    private int fart;

    static public AudioPlayer sharedInstance() throws UnsetVariableException {
        if (null == mSharedInstance) {
            throw new UnsetVariableException();
        }

        return mSharedInstance;
    }

    public AudioPlayer(Context context) {
        mContext = context;

        //SoundPool(SOUND_COUNT = INSTRUMENT_COUNT * 2
        mSoundPool = new SoundPool(50, AudioManager.STREAM_MUSIC, 0);

        mBass = mSoundPool.load(context, R.raw.bass, 1);
        mCrash = mSoundPool.load(context, R.raw.crash, 1);
        mFloorTom = mSoundPool.load(context, R.raw.floor_tom, 1);
        mHighHat = mSoundPool.load(context, R.raw.high_hat, 1);
        mRide = mSoundPool.load(context, R.raw.ride, 1);
        mSnare = mSoundPool.load(context, R.raw.snare, 1);
        mTom1 = mSoundPool.load(context, R.raw.tom_1, 1);
        mTom2 = mSoundPool.load(context, R.raw.tom_2, 1);
        fart = mSoundPool.load(context, R.raw.fart_1, 1);

        //play nothing to fix a bug in soundpool
        mSoundPool.play(-914, 0, 0, 1, -1, 1f);

        mSharedInstance = this;
    }

    public void play(int instrumentid) {
        /*
        float actualVolume = (float) mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float maxVolume = (float) mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float volume = actualVolume / maxVolume;*/
        float volume = 1.0f;

        switch (instrumentid) {
            case Instruments.BASS:
                mSoundPool.play(mBass, volume, volume, 1, 0, 1f);
                break;
            case Instruments.CRASH:
                mSoundPool.play(mCrash, volume, volume, 1, 0, 1f);
                break;
            case Instruments.FLOOR_TOM:
                mSoundPool.play(mFloorTom, volume, volume, 1, 0, 1f);
                break;
            case Instruments.HIGH_HAT:
                mSoundPool.play(mHighHat, volume, volume, 1, 0, 1f);
                break;
            case Instruments.RIDE:
                mSoundPool.play(mRide, volume, volume, 1, 0, 1f);
                break;
            case Instruments.SNARE:
                mSoundPool.play(mSnare, volume, volume, 1, 0, 1f);
                break;
            case Instruments.TOM_1:
                mSoundPool.play(mTom1, volume, volume, 1, 0, 1f);
                break;
            case Instruments.TOM_2:
                mSoundPool.play(mTom2, volume, volume, 1, 0, 1f);
                break;
            case Instruments.FART:
                mSoundPool.play(fart, volume, volume, 1, 0, 1f);
                break;
            case Instruments.LOOPBACK:
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
                }
                else {
                    //stop recording and start playing back the loop
                    if (mLoopedInstruments.size() > 0) {
                        Log.d(LOG_TAG, "start loopback thread");

                        //add a disabled instrument to the end to reflect the pause from the last instrument hit till this one
                        addLoopedInstrument(Instruments.DISABLED);
                        mLastHitTime = 0;

                        //start the loop play thread
                        mLoopbackPlayerThread = new LoopbackPlayerThread();
                        mLoopbackPlayerThread.start();

                        sendLoopbackBroadcast(Broadcasts.ACTION_LOOPBACK_PLAY_STARTED);
                    }
                    else {
                        sendLoopbackBroadcast(Broadcasts.ACTION_LOOPBACK_STOPPED);
                    }
                }
                break;
        }

        if (Instruments.LOOPBACK != instrumentid && mIsRecording) {
            //record it to the list
            addLoopedInstrument(instrumentid);
        }
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
                }
                catch (InterruptedException e) {
                    break;
                }
                catch (Exception e) {
                    break;
                }
            }

            mmIsPlaying = false;//if we break out due to an exception, we should still make sure we are false here
            Log.v(LOG_TAG, "loopbackPlayerThread stopped");
        }
    }
}