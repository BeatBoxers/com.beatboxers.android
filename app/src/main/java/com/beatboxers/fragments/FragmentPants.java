package com.beatboxers.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.beatboxers.Broadcasts;
import com.beatboxers.MainActivity;
import com.beatboxers.R;
import com.beatboxers.actions.Phone;
import com.beatboxers.instruments.Instruments;

public class FragmentPants extends FragmentDevice {
    static private final String LOG_TAG = "bb_"+FragmentPants.class.getSimpleName();

    static public final int PAD_COUNT = 8;

    private final BroadcastReceiver mHitBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Broadcasts.ACTION_HIT_RECEIVED)) {
                String address = intent.getStringExtra(Broadcasts.EXTRA_DEVICE_ADDRESS);

                if (address.equals(mDeviceAddress)) {
                    int padNumber = intent.getIntExtra(Broadcasts.EXTRA_PAD_NUMBER, -1);
                    //int force = intent.getIntExtra(Broadcasts.EXTRA_HIT_FORCE, -1);

                    if (padNumber < 1 || padNumber > mPadCount) {
                        return;
                    }

                    // if we are in the phone call state, react differently
                    if (MainActivity.CURRENT_ACTION == MainActivity.ACTION_STATE_PHONE_CALL) {
                        // left pant leg
                        if (1 == padNumber || 2 == padNumber || 5 == padNumber || 6 == padNumber) {
                            Phone.disconnect();
                        } else {
                            Phone.answer(getActivity());
                        }

                        return;
                    }

                    //do not do anything with loopback pads here
                    if (getInstrument(padNumber).instrumentid == Instruments.LOOPBACK) {
                        return;
                    }

                    Animation shake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);
                    final ImageView imageView = (ImageView)mPlayerLayout.findViewById(getPadViewIdentifier(padNumber));

                    shake.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            imageView.setBackgroundResource(R.mipmap.pants_point_red);
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            imageView.setBackgroundResource(R.mipmap.pants_point_green);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) { }
                    });

                    imageView.startAnimation(shake);
                }
            }
        }
    };

    private final BroadcastReceiver mPadConfigBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Broadcasts.ACTION_PAD_CONFIG_UPDATED)) {
                String address = intent.getStringExtra(Broadcasts.EXTRA_DEVICE_ADDRESS);

                if (address.equals(mDeviceAddress)) {
                    int padNumber = intent.getIntExtra(Broadcasts.EXTRA_PAD_NUMBER, -1);

                    setupPadView(mPlayerLayout, getPadViewIdentifier(padNumber), padNumber, getInstrument(padNumber));
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mPadCount = PAD_COUNT;

        super.onCreate(savedInstanceState);

        //set our broadcast listeners
        getActivity().registerReceiver(mHitBroadcastReceiver, new IntentFilter(Broadcasts.ACTION_HIT_RECEIVED));
        getActivity().registerReceiver(mPadConfigBroadcastReceiver, new IntentFilter(Broadcasts.ACTION_PAD_CONFIG_UPDATED));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mPlayerFragmentContainer.setBackgroundResource(R.mipmap.pants);

        mPlayerLayout = inflater.inflate(R.layout.player_pants, container, false);

        for (int i = 1; i <= mPadCount; i++) {
            setupPadView(mPlayerLayout, getPadViewIdentifier(i), i, getInstrument(i));
        }

        mPlayerFragmentContainer.addView(mPlayerLayout);

        Log.d(LOG_TAG, "Pants player fragment added");

        return view;
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mHitBroadcastReceiver);
        getActivity().unregisterReceiver(mPadConfigBroadcastReceiver);

        super.onDestroy();
    }
}