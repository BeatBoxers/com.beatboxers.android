package com.beatboxers.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.beatboxers.Broadcasts;
import com.beatboxers.Colors;
import com.beatboxers.R;
import com.beatboxers.views.SquareTextView;

public class FragmentDeviceHeader extends AbstractDeviceFragment {
    static private final String LOG_TAG = "bb_"+FragmentDeviceHeader.class.getSimpleName();

    static public final String EXTRAS_DEVICE_ADDRESS = "deviceAddress";
    static public final String EXTRAS_TITLE = "title";
    static public final String EXTRAS_BUTTON_TAG = "buttonTag";
    static public final String EXTRAS_IS_SCAN = "isScan";

    private View.OnClickListener mOnButtonClickListener;
    private boolean mIsScan = false;
    private boolean mIsActive = false;
    final private Handler mHitHandler = new Handler();
    final private Runnable mHitRunnable = new Runnable() {
        @Override
        public void run() {
            mTextView.setTextColor(Colors.get(getActivity(), R.color.text));
        }
    };

    private View mContainerView;
    private SquareTextView mTextView;

    private final BroadcastReceiver mHitBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Broadcasts.ACTION_HIT_RECEIVED)) {
                String address = intent.getStringExtra(Broadcasts.EXTRA_DEVICE_ADDRESS);

                if (address.equals(mDeviceAddress)) {
                    mTextView.setTextColor(Colors.get(getActivity(), R.color.highlighted_text));

                    mHitHandler.removeCallbacks(mHitRunnable);
                    mHitHandler.postDelayed(mHitRunnable, 550);
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String buttonText = getArguments().getString(EXTRAS_TITLE);
        String buttonTag = getArguments().getString(EXTRAS_BUTTON_TAG);
        mDeviceAddress = getArguments().getString(EXTRAS_DEVICE_ADDRESS);

        mIsScan = getArguments().getBoolean(EXTRAS_IS_SCAN);

        mContainerView = inflater.inflate(R.layout.fragment_device_header, container, false);

        mTextView = (SquareTextView)mContainerView.findViewById(R.id.buttonDeviceSelect);
        mTextView.setTag(buttonTag);
        mTextView.setText(buttonText);
        mTextView.setOnClickListener(mOnButtonClickListener);

        if (mIsScan) {
            mTextView.setBackgroundColor(Colors.get(getActivity(), R.color.scan));
        }
        else {
            setViewFromState();
        }

        return mContainerView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set our broadcast listeners
        getActivity().registerReceiver(mHitBroadcastReceiver, new IntentFilter(Broadcasts.ACTION_HIT_RECEIVED));
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mHitBroadcastReceiver);

        super.onDestroy();
    }

    public void setButtonOnClickListener(View.OnClickListener onClickListener) {
        mOnButtonClickListener = onClickListener;
    }

    public void setState(int state) {
        mCurrentState = state;
        setViewFromState();
    }

    public void setIsActive(boolean isActive) {
        mIsActive = isActive;
        setViewFromState();
    }

    protected void setViewFromState() {
        if (null == mTextView || null == mContainerView || mIsScan) {
            return;
        }

        mTextView.setBackgroundColor(getBackgroundColorForState());

        if (mIsActive) {
            mContainerView.setBackgroundColor(getBackgroundColorForState());
        }
        else {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }
    }

    private int getBackgroundColorForState() {
        switch (mCurrentState) {
            case STATE_CONNECTING:
                return Colors.get(getActivity(), R.color.connecting);
            case STATE_CONNECTED:
                return Colors.get(getActivity(), R.color.connected);
            case STATE_DISCONNECTED:
                return Colors.get(getActivity(), R.color.disconnected);
        }

        return 0;
    }
}