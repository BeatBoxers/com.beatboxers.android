package com.beatboxers;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.LinearLayout;

import com.beatboxers.bluetooth.ConnectionCallback;
import com.beatboxers.bluetooth.ConnectionService;
import com.beatboxers.bluetooth.device.Rfduino;
import com.beatboxers.bluetooth.device.SavedDevices;
import com.beatboxers.dialogs.AboutUsDialog;
import com.beatboxers.dialogs.ScanDialog;
import com.beatboxers.fragments.FragmentDevice;
import com.beatboxers.fragments.FragmentDeviceHeader;
import com.beatboxers.fragments.FragmentPants;
import com.beatboxers.fragments.FragmentShoe;
import com.beatboxers.instruments.AudioPlayer;
import com.beatboxers.instruments.DeviceConfig;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity {
    static private final String LOG_TAG = "bb_"+MainActivity.class.getSimpleName();

    static public final int ACTION_STATE_PLAY = 1;
    static public final int ACTION_STATE_PHONE_CALL = 2;

    static public int CURRENT_ACTION = ACTION_STATE_PLAY;

    private ConnectionService mConnectionService;
    private TelephonyManager mTelephonyManager;
    private AudioPlayer mAudioPlayer;
    private DeviceConfig mDeviceConfig;
    private ScanDialog mScanDialog = new ScanDialog();
    private AboutUsDialog mAboutUsDialog = new AboutUsDialog();

    private final BroadcastReceiver mBluetoothBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

                if (state == BluetoothAdapter.STATE_ON) {
                    Log.i(LOG_TAG, "Bluetooth turned on");
                    mLayoutManager.bluetoothEnabled();
                }
                else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                    Log.i(LOG_TAG, "Bluetooth turning off...");
                    mConnectionService.disconnect();
                }
                else if (state == BluetoothAdapter.STATE_OFF) {
                    Log.i(LOG_TAG, "Bluetooth turned off");
                    mLayoutManager.bluetoothDisabled();
                }
            }
        }
    };

    private final BroadcastReceiver mDeviceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String name;
            String address;

            switch (action) {
                case Broadcasts.ACTION_CONNECT_DEVICE:
                    name = intent.getStringExtra(Broadcasts.EXTRA_DEVICE_NAME).trim();
                    address = intent.getStringExtra(Broadcasts.EXTRA_DEVICE_ADDRESS).trim();

                    //since we are connecting to it, load its config
                    mDeviceConfig.loadDevice(address);

                    mLayoutManager.connecting(name, address);
                    mConnectionService.connect(address);
                    break;
                case Broadcasts.ACTION_DISCONNECT_DEVICE:
                    address = intent.getStringExtra(Broadcasts.EXTRA_DEVICE_ADDRESS).trim();

                    mConnectionService.disconnect(address);
                    break;
                case Broadcasts.ACTION_REMOVE_DEVICE:
                    name = intent.getStringExtra(Broadcasts.EXTRA_DEVICE_NAME).trim();
                    address = intent.getStringExtra(Broadcasts.EXTRA_DEVICE_ADDRESS).trim();

                    mConnectionService.disconnect(address);
                    mLayoutManager.remove(name, address);
                    break;
            }
        }
    };

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING && !incomingNumber.equals("")) {
                Log.i(LOG_TAG, "Phone call coming in");
                CURRENT_ACTION = ACTION_STATE_PHONE_CALL;
            } else {
                Log.i(LOG_TAG, "Passing action back to player");
                CURRENT_ACTION = ACTION_STATE_PLAY;
            }
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mConnectionService = ((ConnectionService.LocalBinder) service).getService();
            mConnectionService.setCallback(mConnectionCallback);

            //connect to any devices that were saved
            HashMap<String, String> savedDevices = new SavedDevices(MainActivity.this).getAll();
            for (HashMap.Entry<String, String> device : savedDevices.entrySet()) {
                Intent intent = new Intent();
                intent.setAction(Broadcasts.ACTION_CONNECT_DEVICE);
                intent.putExtra(Broadcasts.EXTRA_DEVICE_ADDRESS, device.getKey());
                intent.putExtra(Broadcasts.EXTRA_DEVICE_NAME, device.getValue());
                sendBroadcast(intent);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mConnectionService = null;
        }
    };

    private final ConnectionCallback mConnectionCallback = new ConnectionCallback() {
        @Override
        public void connected(BluetoothGatt gatt) {
            BluetoothDevice device = gatt.getDevice();
            String name = device.getName().trim();
            String address = device.getAddress().trim();

            Log.d(LOG_TAG, "device connected: "+name+" - "+address);

            mLayoutManager.connected(name, address);
        }

        @Override
        public void connectionError(BluetoothGatt gatt) {
            BluetoothDevice device = gatt.getDevice();
            String name = device.getName().trim();
            String address = device.getAddress().trim();

            Log.d(LOG_TAG, "device connection error: "+name+" - "+address);

            mLayoutManager.disconnected(name, address);
        }

        @Override
        public void disconnected(BluetoothGatt gatt) {
            BluetoothDevice device = gatt.getDevice();
            String name = device.getName().trim();
            String address = device.getAddress().trim();

            Log.d(LOG_TAG, "device disconnected: "+name+" - "+address);

            mLayoutManager.disconnected(name, address);
        }
    };

    private LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(LOG_TAG, "onCreate");

        //setup our singletons
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mAudioPlayer = new AudioPlayer(this);
        mDeviceConfig = new DeviceConfig(this);

        //link our views
        mLayoutManager = new LayoutManager();
        /* Uncomment to load test devices to check the looks
        mLayoutManager.connected("Test", "10:10");
        mLayoutManager.connected("Test1", "10:11");
        mLayoutManager.connected("Test2", "10:12");
        mLayoutManager.connected("Test3", "10:13");
        mLayoutManager.connected("Test4", "10:14");
        */

        //register our receivers
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Broadcasts.ACTION_CONNECT_DEVICE);
        intentFilter.addAction(Broadcasts.ACTION_DISCONNECT_DEVICE);
        intentFilter.addAction(Broadcasts.ACTION_REMOVE_DEVICE);
        registerReceiver(mDeviceBroadcastReceiver, intentFilter);

        //bind our services
        Intent gattServiceIntent = new Intent(this, ConnectionService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        //be ready for telephony
        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);

        //force our action bar to use the overflow menu instead of hardware button
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        }
        catch (Exception e) {
            // Ignore
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.i(LOG_TAG, "onStart");

        registerReceiver(mBluetoothBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        Log.i(LOG_TAG, "onRestart");
        //in onStop() we disconnect all devices so when we Restart the activity, we should reconnect
        sendBroadcast(new Intent(Broadcasts.ACTION_RECONNECT_ALL_DEVICES));
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(LOG_TAG, "onResume");

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            mLayoutManager.bluetoothDisabled();
        }
        else {
            mLayoutManager.bluetoothEnabled();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.i(LOG_TAG, "onStop");

        mAudioPlayer.killLoopbackPlayerThread();

        //disconnect all of our connections since we are destroying the activity
        mConnectionService.disconnect();

        //we don't care about Bluetooth state changes when we are stopped
        unregisterReceiver(mBluetoothBroadcastReceiver);

        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.i(LOG_TAG, "onDestroy");

        //remove our receivers
        unregisterReceiver(mDeviceBroadcastReceiver);

        //unbind our services
        unbindService(mServiceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuAbout:
                mAboutUsDialog.show(getFragmentManager(), AboutUsDialog.TAG);
                return true;
            case R.id.menuSettings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /* Private methods to manage the different views based on different BT states */
    private class LayoutManager {
        public static final String TAG_SCAN_BUTTON = "scan";

        private static final String HEADER_FRAGMENT_TAG_PREFIX = "header-";

        private ArrayList<FragmentDeviceHeader> mmDeviceHeaderFragments = new ArrayList<>();
        private ArrayList<FragmentDevice> mmDeviceFragments = new ArrayList<>();
        private String mmCurrentSelectedDeviceTag;//used to re-display the active device fragment if bluetooth is switched off/on
        private String mmCurrentSelectedHeaderTag;//used for setting up the different views in the header

        private LinearLayout mLayoutNoDevices;
        private LinearLayout mLayoutBluetoothDisabled;
        private LinearLayout mLayoutFragmentContainer;

        private final View.OnClickListener mmHeaderButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tag = (String)view.getTag();

                if (null == tag) {
                    return;
                }

                switch (tag) {
                    case TAG_SCAN_BUTTON:
                        mScanDialog.show(getFragmentManager(), ScanDialog.TAG);
                        break;
                    default:
                        headerButtonClicked(tag);
                }
            }
        };

        public LayoutManager() {
            mLayoutNoDevices = (LinearLayout)findViewById(R.id.layoutNoDevices);
            mLayoutBluetoothDisabled = (LinearLayout)findViewById(R.id.layoutBluetoothDisabled);
            mLayoutFragmentContainer = (LinearLayout)findViewById(R.id.layoutFragmentContainer);

            Button buttonEnableBluetooth = (Button)findViewById(R.id.buttonEnableBluetooth);
            buttonEnableBluetooth.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BluetoothAdapter.getDefaultAdapter().enable();
                }
            });

            addNewDeviceHeaderFragment(TAG_SCAN_BUTTON, "+", true);
        }

        public void bluetoothEnabled() {
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

            for (FragmentDeviceHeader fragmentDeviceHeader : mmDeviceHeaderFragments) {
                fragmentTransaction.show(fragmentDeviceHeader);
            }
            for (FragmentDevice fragmentDevice : mmDeviceFragments) {
                if (fragmentDevice.getTag().equals(mmCurrentSelectedDeviceTag)) {
                    fragmentTransaction.show(fragmentDevice);
                    break;//we will only show 1 device fragment at a time
                }
            }

            fragmentTransaction.commit();

            //hide the "enabled bluetooth" view
            mLayoutBluetoothDisabled.setVisibility(View.GONE);
            mLayoutNoDevices.setVisibility((0 == mmDeviceFragments.size() ? View.VISIBLE : View.GONE));
            mLayoutFragmentContainer.setVisibility((0 == mmDeviceFragments.size() ? View.GONE : View.VISIBLE));
        }

        public void bluetoothDisabled() {
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

            //hide all fragments
            for (FragmentDeviceHeader fragmentDeviceHeader : mmDeviceHeaderFragments) {
                fragmentTransaction.hide(fragmentDeviceHeader);
            }
            for (FragmentDevice fragmentDevice : mmDeviceFragments) {
                fragmentTransaction.hide(fragmentDevice);
            }

            fragmentTransaction.commit();

            //show the "enable bluetooth" view
            mLayoutBluetoothDisabled.setVisibility(View.VISIBLE);
            mLayoutNoDevices.setVisibility(View.GONE);
            mLayoutFragmentContainer.setVisibility(View.GONE);
        }

        public void connecting(String name, String tag) {
            getDeviceHeaderFragment(tag).setState(FragmentDevice.STATE_CONNECTING);
            getDeviceFragment(name, tag).setState(FragmentDevice.STATE_CONNECTING);
            setupActiveButtonView();
        }

        public void connected(String name, String tag) {
            getDeviceHeaderFragment(tag).setState(FragmentDevice.STATE_CONNECTED);
            getDeviceFragment(name, tag).setState(FragmentDevice.STATE_CONNECTED);
            setupActiveButtonView();
        }

        public void disconnected(String name, String tag) {
            if (!deviceHeaderFragmentExists(tag) || !deviceFragmentExists(tag)) {
                return;
            }

            getDeviceHeaderFragment(tag).setState(FragmentDevice.STATE_DISCONNECTED);
            getDeviceFragment(name, tag).setState(FragmentDevice.STATE_DISCONNECTED);
            setupActiveButtonView();
        }

        public void remove(String name, String tag) {
            Log.i(LOG_TAG, "removing device: "+name+" "+tag);

            FragmentDeviceHeader fragmentDeviceHeader = getDeviceHeaderFragment(tag);
            mmDeviceHeaderFragments.remove(fragmentDeviceHeader);
            FragmentDevice fragmentDevice = getDeviceFragment(name, tag);
            mmDeviceFragments.remove(fragmentDevice);

            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.remove(fragmentDeviceHeader);
            fragmentTransaction.remove(fragmentDevice);
            fragmentTransaction.commit();

            if (0 == mmDeviceFragments.size()) {
                mLayoutNoDevices.setVisibility(View.VISIBLE);
                mLayoutFragmentContainer.setVisibility(View.GONE);
            }
        }

        public void headerButtonClicked(String tag) {
            FragmentManager fragmentManager = getFragmentManager();

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            for (FragmentDevice fragmentDevice : mmDeviceFragments) {
                if (fragmentDevice.getTag().equals(tag)) {
                    fragmentTransaction.show(fragmentDevice);
                }
                else {
                    fragmentTransaction.hide(fragmentDevice);
                }
            }
            fragmentTransaction.commit();

            mmCurrentSelectedDeviceTag = tag;
            mmCurrentSelectedHeaderTag = HEADER_FRAGMENT_TAG_PREFIX+tag;
            setupActiveButtonView();
        }

        private void setupActiveButtonView() {
            for (FragmentDeviceHeader deviceHeader : mmDeviceHeaderFragments) {
                if (deviceHeader.getTag().equals(mmCurrentSelectedHeaderTag)) {
                    deviceHeader.setIsActive(true);
                }
                else {
                    deviceHeader.setIsActive(false);
                }
            }
        }

        private boolean deviceFragmentExists(String tag) {
            for (FragmentDevice fragmentDevice : mmDeviceFragments) {
                if (fragmentDevice.getTag().equals(tag)) {
                    return true;
                }
            }

            return false;
        }

        private FragmentDevice getDeviceFragment(String name, String tag) {
            mLayoutNoDevices.setVisibility(View.GONE);
            mLayoutFragmentContainer.setVisibility(View.VISIBLE);

            for (FragmentDevice fragmentDevice : mmDeviceFragments) {
                if (fragmentDevice.getTag().equals(tag)) {
                    return fragmentDevice;
                }
            }

            FragmentDevice fragment;

            //it is a new fragment
            if (name.equals(Rfduino.DEVICE_NAME)) {
                fragment = new FragmentShoe();
            }
            else {
                fragment = new FragmentPants();
            }

            Bundle bundle = new Bundle();
            bundle.putString(FragmentDevice.EXTRAS_DEVICE_NAME, name);
            bundle.putString(FragmentDevice.EXTRAS_DEVICE_ADDRESS, tag);

            fragment.setArguments(bundle);

            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

            //since this is a new fragment, hide all the other fragments and make this one the default
            for (FragmentDevice fragmentDevice : mmDeviceFragments) {
                fragmentTransaction.hide(fragmentDevice);
            }

            //now that we hid all the other fragments, add this fragment to our ArrayList
            mmDeviceFragments.add(fragment);

            fragmentTransaction.add(R.id.layoutFragmentContainer, fragment, tag);
            fragmentTransaction.commit();

            mmCurrentSelectedDeviceTag = tag;
            mmCurrentSelectedHeaderTag = HEADER_FRAGMENT_TAG_PREFIX+tag;
            setupActiveButtonView();

            return fragment;
        }

        private boolean deviceHeaderFragmentExists(String tag) {
            //we have to modify our fragment tag a bit since its a header
            tag = HEADER_FRAGMENT_TAG_PREFIX+tag;

            for (FragmentDeviceHeader fragmentDeviceHeader : mmDeviceHeaderFragments) {
                if (tag.equals(fragmentDeviceHeader.getTag())) {
                    return true;
                }
            }

            return false;
        }

        private FragmentDeviceHeader getDeviceHeaderFragment(String tag) {
            //we have to modify our fragment tag a bit since its a header
            tag = HEADER_FRAGMENT_TAG_PREFIX+tag;

            for (FragmentDeviceHeader fragmentDeviceHeader : mmDeviceHeaderFragments) {
                if (fragmentDeviceHeader.getTag().equals(tag)) {
                    return fragmentDeviceHeader;
                }
            }

            return addNewDeviceHeaderFragment(tag, String.valueOf(mmDeviceHeaderFragments.size()), false);
        }

        private FragmentDeviceHeader addNewDeviceHeaderFragment(String tag, String title, boolean isScan) {
            Log.i(LOG_TAG, "Adding new device header fragment: " + tag);

            FragmentDeviceHeader fragment = new FragmentDeviceHeader();
            fragment.setButtonOnClickListener(mmHeaderButtonClickListener);

            String address = tag.replace(HEADER_FRAGMENT_TAG_PREFIX, "");

            Bundle bundle = new Bundle();
            bundle.putString(FragmentDeviceHeader.EXTRAS_TITLE, title);
            bundle.putString(FragmentDeviceHeader.EXTRAS_BUTTON_TAG, address);
            bundle.putBoolean(FragmentDeviceHeader.EXTRAS_IS_SCAN, isScan);
            bundle.putString(FragmentDeviceHeader.EXTRAS_DEVICE_ADDRESS, address);

            fragment.setArguments(bundle);

            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

            mmDeviceHeaderFragments.add(fragment);

            fragmentTransaction.add(R.id.layoutPlayerHeader, fragment, tag);
            fragmentTransaction.commit();

            mmCurrentSelectedHeaderTag = tag;
            setupActiveButtonView();

            return fragment;
        }
    }
}