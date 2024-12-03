package itan.com.bluetoothle;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashSet;

import static itan.com.bluetoothle.Constants.BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID;
import static itan.com.bluetoothle.Constants.HEART_RATE_SERVICE_UUID;


/**
 This activity represents the Peripheral/Server role.
 Bluetooth communication flow:
    1. advertise [peripheral]
    2. scan [central]
    3. connect [central]
    4. notify [peripheral]
    5. receive [central]
 */
public class PeripheralRoleActivity extends BluetoothActivity implements View.OnClickListener, SensorEventListener {

    private BluetoothGattService mSampleService;
    public BluetoothGattCharacteristic mSampleCharacteristic;

    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mGattServer;
    private HashSet<BluetoothDevice> mBluetoothDevices;

    private Button mNotifyButton;
    private Switch mEnableAdvertisementSwitch;
    //tomas
    public TextView mMobilCislo1;
    private TextView xValue;
    private Button mZavolaCislo1;
    public String mobilcislo;
    public String mPrikaz;
    public String mStav;
    public boolean ZapVyp;

    float zmenaX;

    private SensorManager sensorManager;
    Sensor acclerometer;
    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mEnableAdvertisementSwitch = (Switch) findViewById(R.id.advertise_switch);

        mMobilCislo1 = (TextView) findViewById(R.id.mobil_cislo_1);
        xValue = (TextView) findViewById(R.id.xValue);


        mEnableAdvertisementSwitch.setOnClickListener(this);
        //tomas
        mZavolaCislo1 = (Button) findViewById(R.id.zavola_cislo1);
        mZavolaCislo1.setOnClickListener(this);



        setGattServer();
        setBluetoothService();

        //tomas
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        acclerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(PeripheralRoleActivity.this, acclerometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_peripheral_role;
    }


    @Override
    public void onClick(View view) {


        int format = -1;

        switch(view.getId()) {

            case R.id.advertise_switch:
                Switch switchToggle = (Switch) view;
                if (switchToggle.isChecked()) {
                    startAdvertising();
                } else {
                    stopAdvertising();
                }
                break;

            case R.id.zavola_cislo1:
                zavolajCislo();
                break;


        }

        }

    //tomas
    private void zavolajCislo() {
        // Getting instance of Intent with action as ACTION_CALL
        int requestCode = 0;
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, requestCode);
        return;

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == requestCode)
        {
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + mobilcislo));
                startActivity(callIntent);
            }
        }
    }


    //tomas
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        xValue.setText("xValue: " +  String.format("%.2f", sensorEvent.values[0]));

        zmenaX = sensorEvent.values[0];

        if ((zmenaX > 1 || zmenaX < -1) && ZapVyp)
        {
      //      ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneG.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 600);
            sensorManager.unregisterListener(PeripheralRoleActivity.this);
            zavolajCislo();
        }
    }




    @Override
    protected int getTitleString() {
        return R.string.peripheral_screen;
    }


    /**
     * Starts BLE Advertising by starting {@code PeripheralAdvertiseService}.
     */
    private void startAdvertising() {
        // TODO bluetooth - maybe bindService? what happens when closing app?
        startService(getServiceIntent(this));
    }


    /**
     * Stops BLE Advertising by stopping {@code PeripheralAdvertiseService}.
     */
    private void stopAdvertising() {
        stopService(getServiceIntent(this));
        mEnableAdvertisementSwitch.setChecked(false);
    }

    private void setGattServer() {

        mBluetoothDevices = new HashSet<>();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if (mBluetoothManager != null) {
            mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        } else {
            showMsgText(R.string.error_unknown);
        }
    }

    private void setBluetoothService() {

        // create the Service
        mSampleService = new BluetoothGattService(HEART_RATE_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        /*
        create the Characteristic.
        we need to grant to the Client permission to read (for when the user clicks the "Request Characteristic" button).
        no need for notify permission as this is an action the Server initiate.
         */
        mSampleCharacteristic = new BluetoothGattCharacteristic(BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID, BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);


        // add the Characteristic to the Service
        mSampleService.addCharacteristic(mSampleCharacteristic);

        // add the Service to the Server/Peripheral
        if (mGattServer != null) {
            mGattServer.addService(mSampleService);
        }
    }




    /*
    update the value of Characteristic.
    the client will receive the Characteristic value when:
        1. the Client user clicks the "Request Characteristic" button
        2. teh Server user clicks the "Notify Client" button

    value - can be between 0-255 according to:
    https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.body_sensor_location.xml
     */
    private void setCharacteristic(int checkedId) {
        /*
        done each time the user changes a value of a Characteristic
         */
    }

    private byte[] getValue(int value) {
        return new byte[]{(byte) value};
    }

    /*
    send to the client the value of the Characteristic,
    as the user requested to notify.
     */
    private void notifyCharacteristicChanged() {
        /*
        done when the user clicks the notify button in the app.
        indicate - true for indication (acknowledge) and false for notification (un-acknowledge).
         */
        boolean indicate = (mSampleCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) == BluetoothGattCharacteristic.PROPERTY_INDICATE;

        for (BluetoothDevice device : mBluetoothDevices) {
            if (mGattServer != null) {
                mGattServer.notifyCharacteristicChanged(device, mSampleCharacteristic, indicate);
            }
        }
    }

    /**
     * Returns Intent addressed to the {@code PeripheralAdvertiseService} class.
     */
    private Intent getServiceIntent(Context context) {
        return new Intent(context, PeripheralAdvertiseService.class);
    }


    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {

            super.onConnectionStateChange(device, status, newState);

            String msg;

            if (status == BluetoothGatt.GATT_SUCCESS) {

                if (newState == BluetoothGatt.STATE_CONNECTED) {

                    mBluetoothDevices.add(device);

                    msg = "Connected to device: " + device.getAddress();
                    Log.v(MainActivity.TAG, msg);
                    showMsgText(msg);

                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {

                    mBluetoothDevices.remove(device);

                    msg = "Disconnected from device";
                    Log.v(MainActivity.TAG, msg);
                    showMsgText(msg);
                }

            } else {
                mBluetoothDevices.remove(device);

                msg = getString(R.string.status_error_when_connecting) + ": " + status;
                Log.e(MainActivity.TAG, msg);
                showMsgText(msg);

            }
        }


        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.v(MainActivity.TAG, "Notification sent. Status: " + status);
        }


        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {

            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            if (mGattServer == null) {
                return;
            }

            Log.d(MainActivity.TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            Log.d(MainActivity.TAG, "Value: " + Arrays.toString(characteristic.getValue()));

            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }


        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {

            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

      //      String msg = "Characteristic Write request: " + Arrays.toString(value);
      //      Log.v(MainActivity.TAG, msg);
      //      showMsgText(msg);

            mSampleCharacteristic.setValue(value);

            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
            }

            //tomas
            mPrikaz = mSampleCharacteristic.getStringValue(0);
            // mobilcislo = mSampleCharacteristic.getStringValue(0);
            //mMobilCislo1.setText(mobilcislo);


            String[] separated = mPrikaz.split(":");
            mobilcislo = separated [1]; // this will contain "Fruit"
            mStav = separated [0]; // this will contain " they taste good"

    //        String msg2 = mStav;
     //       Log.v(MainActivity.TAG, msg2);
      //      showMsgText(msg2);

//
            switch (mStav) {
                case "ON":
                    ZapVyp = true;
                    mMobilCislo1.setText("ZAP " + mobilcislo);
                    toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                    //      String msg1 = mobilcislo;
              //      Log.v(MainActivity.TAG, msg1);
              //      showMsgText(msg1);
                    break;

                case "OF":
                    ZapVyp = false;
                    mMobilCislo1.setText("VYp " + mobilcislo);
                    toneG.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE, 200);

                    //    String msg2 = mobilcislo;
                //    Log.v(MainActivity.TAG, msg2);
                //    showMsgText(msg2);

                    break;
            }



        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {

            super.onDescriptorReadRequest(device, requestId, offset, descriptor);

            if (mGattServer == null) {
                return;
            }

            Log.d(MainActivity.TAG, "Device tried to read descriptor: " + descriptor.getUuid());
            Log.d(MainActivity.TAG, "Value: " + Arrays.toString(descriptor.getValue()));

            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
                                             int offset,
                                             byte[] value) {

            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

            Log.v(MainActivity.TAG, "Descriptor Write Request " + descriptor.getUuid() + " " + Arrays.toString(value));

//            int status = BluetoothGatt.GATT_SUCCESS;
//            if (descriptor.getUuid() == CLIENT_CHARACTERISTIC_CONFIGURATION_UUID) {
//                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
//                boolean supportsNotifications = (characteristic.getProperties() &
//                        BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
//                boolean supportsIndications = (characteristic.getProperties() &
//                        BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;
//
//                if (!(supportsNotifications || supportsIndications)) {
//                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
//                } else if (value.length != 2) {
//                    status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
//                } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsDisabled(characteristic);
//                    descriptor.setValue(value);
//                } else if (supportsNotifications &&
//                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsEnabled(characteristic, false /* indicate */);
//                    descriptor.setValue(value);
//                } else if (supportsIndications &&
//                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsEnabled(characteristic, true /* indicate */);
//                    descriptor.setValue(value);
//                } else {
//                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
//                }
//            } else {
//                status = BluetoothGatt.GATT_SUCCESS;
//                descriptor.setValue(value);
//            }
//            if (responseNeeded) {
//                mGattServer.sendResponse(device, requestId, status,
//            /* No need to respond with offset */ 0,
//            /* No need to respond with a value */ null);
//            }

        }
    };



}
