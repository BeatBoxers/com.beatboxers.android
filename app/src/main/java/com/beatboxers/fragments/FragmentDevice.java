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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.beatboxers.Broadcasts;
import com.beatboxers.Colors;
import com.beatboxers.R;
import com.beatboxers.dialogs.ChooseInstrumentDialog;
import com.beatboxers.dialogs.DeviceSettingsDialog;
import com.beatboxers.instruments.DeviceConfig;
import com.beatboxers.instruments.Instrument;
import com.beatboxers.instruments.Instruments;
import com.beatboxers.instruments.UnsetVariableException;

import java.util.ArrayList;

public class FragmentDevice extends AbstractDeviceFragment {
    static private final String LOG_TAG = "bb_"+FragmentDevice.class.getSimpleName();

    static public final String EXTRAS_DEVICE_NAME = "deviceName";

    protected String mDeviceName;
    protected int mPadCount = 0;

    private DeviceSettingsDialog mDeviceSettingsDialog = new DeviceSettingsDialog();
    private ChooseInstrumentDialog mChooseInstrumentDialog = new ChooseInstrumentDialog();

    private ProgressBar mConnectingProgressBar;
    private ImageButton mReconnectButton;
    private ImageButton mDisconnectButton;
    private RelativeLayout mLayoutHeader;
    protected LinearLayout mPlayerFragmentContainer;
    protected View mPlayerLayout;

    private final BroadcastReceiver mLoopbackBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            ArrayList<Integer> loopbackPadViewIdentifiers = getLoopbackPadViewIdentifiers(mPadCount);

            switch (action) {
                case Broadcasts.ACTION_LOOPBACK_RECORDING:
                    for (Integer identifier : loopbackPadViewIdentifiers) {
                        ImageView imageView = (ImageView)mPlayerLayout.findViewById(identifier);
                        imageView.setBackgroundResource(R.mipmap.pants_point_red);
                        imageView.clearAnimation();
                    }
                    break;
                case Broadcasts.ACTION_LOOPBACK_PLAY_STARTED:
                    //Instead of setting a new background here, start an animation that will spin the view until infinity
                    Animation spin = AnimationUtils.loadAnimation(getActivity(), R.anim.spin);

                    for (Integer identifier : loopbackPadViewIdentifiers) {
                        ImageView imageView = (ImageView)mPlayerLayout.findViewById(identifier);
                        imageView.clearAnimation();
                        imageView.startAnimation(spin);
                    }
                    break;
                case Broadcasts.ACTION_LOOPBACK_STOPPED:
                    for (Integer identifier : loopbackPadViewIdentifiers) {
                        ImageView imageView = (ImageView)mPlayerLayout.findViewById(identifier);
                        imageView.setBackgroundResource(R.mipmap.pants_point_green);
                        imageView.clearAnimation();
                    }
                    break;
            }
        }
    };

    private final BroadcastReceiver mReconnectBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Broadcasts.ACTION_RECONNECT_ALL_DEVICES)) {
                if (mCurrentState == STATE_DISCONNECTED) {
                    Log.i(LOG_TAG, "Attempting to reconnect from broadcast to: "+mDeviceAddress);

                    reconnect();
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Broadcasts.ACTION_LOOPBACK_RECORDING);
        intentFilter.addAction(Broadcasts.ACTION_LOOPBACK_PLAY_STARTED);
        intentFilter.addAction(Broadcasts.ACTION_LOOPBACK_STOPPED);

        getActivity().registerReceiver(mLoopbackBroadcastReceiver, intentFilter);
        getActivity().registerReceiver(mReconnectBroadcastReceiver, new IntentFilter(Broadcasts.ACTION_RECONNECT_ALL_DEVICES));
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mLoopbackBroadcastReceiver);
        getActivity().unregisterReceiver(mReconnectBroadcastReceiver);

        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mDeviceName = getArguments().getString(EXTRAS_DEVICE_NAME);
        mDeviceAddress = getArguments().getString(EXTRAS_DEVICE_ADDRESS);

        Log.i(LOG_TAG, "onCreateView: "+mDeviceName+" "+mDeviceAddress);

        View view = inflater.inflate(R.layout.fragment_device, container, false);

        Button deviceNameTextView = (Button)view.findViewById(R.id.buttonDeviceName);
        deviceNameTextView.setText(mDeviceName);
        deviceNameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString(DeviceSettingsDialog.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
                bundle.putString(DeviceSettingsDialog.EXTRAS_DEVICE_NAME, mDeviceName);
                mDeviceSettingsDialog.setArguments(bundle);
                mDeviceSettingsDialog.show(getFragmentManager(), DeviceSettingsDialog.TAG+mDeviceAddress);
            }
        });

        mConnectingProgressBar = (ProgressBar)view.findViewById(R.id.progressBarConnecting);
        mReconnectButton = (ImageButton)view.findViewById(R.id.buttonReconnect);
        mDisconnectButton = (ImageButton)view.findViewById(R.id.buttonDisconnect);

        mDisconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Broadcasts.ACTION_DISCONNECT_DEVICE);
                intent.putExtra(Broadcasts.EXTRA_DEVICE_ADDRESS, mDeviceAddress);

                getActivity().sendBroadcast(intent);
            }
        });

        mReconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reconnect();
            }
        });

        mLayoutHeader = (RelativeLayout)view.findViewById(R.id.layoutHeader);
        mPlayerFragmentContainer = (LinearLayout)view.findViewById(R.id.playerFragmentContainer);

        //default to connecting...
        setViewFromState();

        return view;
    }

    private void reconnect() {
        if (mCurrentState == STATE_DISCONNECTED) {
            Intent intent = new Intent(Broadcasts.ACTION_CONNECT_DEVICE);
            intent.putExtra(Broadcasts.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
            intent.putExtra(Broadcasts.EXTRA_DEVICE_NAME, mDeviceName);

            getActivity().sendBroadcast(intent);
        }
    }

    public void setState(int state) {
        mCurrentState = state;
        setViewFromState();
    }

    protected void setViewFromState() {
        if (null == mConnectingProgressBar || null == mDisconnectButton || null == mReconnectButton) {
            return;
        }

        switch (mCurrentState) {
            case STATE_CONNECTING:
                mLayoutHeader.setBackgroundColor(Colors.get(getActivity(), R.color.connecting));
                mConnectingProgressBar.setVisibility(View.VISIBLE);
                mDisconnectButton.setVisibility(View.GONE);
                mReconnectButton.setVisibility(View.GONE);
                break;
            case STATE_CONNECTED:
                mLayoutHeader.setBackgroundColor(Colors.get(getActivity(), R.color.connected));
                mConnectingProgressBar.setVisibility(View.GONE);
                mDisconnectButton.setVisibility(View.VISIBLE);
                mReconnectButton.setVisibility(View.GONE);
                break;
            case STATE_DISCONNECTED:
                mLayoutHeader.setBackgroundColor(Colors.get(getActivity(), R.color.disconnected));
                mConnectingProgressBar.setVisibility(View.GONE);
                mDisconnectButton.setVisibility(View.GONE);
                mReconnectButton.setVisibility(View.VISIBLE);
                break;
        }
    }

    protected int getPadViewIdentifier(int padNumber) {
        return getResources().getIdentifier("pad"+padNumber, "id", getActivity().getPackageName());
    }

    protected void setupPadView(View playerLayout, int padid, final int padNumber, final Instrument instrument) {
        ImageView padView = (ImageView)playerLayout.findViewById(padid);
        padView.setTag(padNumber);
        padView.setImageResource(instrument.imageResource);
        padView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putString(ChooseInstrumentDialog.EXTRAS_ADDRESS, mDeviceAddress);
                bundle.putInt(ChooseInstrumentDialog.EXTRAS_PAD_NUMBER, padNumber);
                bundle.putInt(ChooseInstrumentDialog.EXTRAS_SELECTED_INSTRUMENT_ID, instrument.instrumentid);

                mChooseInstrumentDialog.setArguments(bundle);
                mChooseInstrumentDialog.show(getFragmentManager(), ChooseInstrumentDialog.TAG);
            }
        });
    }

    protected Instrument getInstrument(int padNumber) {
        try {
            return Instruments.sharedInstance().getInstrument(DeviceConfig.sharedInstance().getInstrumentid(mDeviceAddress, padNumber));
        }
        catch (UnsetVariableException e) {
            Log.e(LOG_TAG, "Instruemnts shared instance was not set");

            return Instruments.sharedInstance().getDisabled();
        }
    }

    protected ArrayList<Integer> getLoopbackPadViewIdentifiers(int padCount) {
        ArrayList<Integer> loopbackPads = new ArrayList<>();

        for (int i = 1; i <= padCount; i++) {
            if (getInstrument(i).isLoopback()) {
                loopbackPads.add(getPadViewIdentifier(i));
            }
        }

        return loopbackPads;
    }
}