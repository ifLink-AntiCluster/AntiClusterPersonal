package jp.iflink.anticluster.home;

import android.bluetooth.BluetoothDevice;

import static java.lang.Boolean.FALSE;

class ScannedDevice {
    private static final String UNKNOWN = "Unknown";
    /** BluetoothDevice */
    private BluetoothDevice mDevice;
    /** RSSI */
    private int mRssi;
    /** Display Name */
    private String mDisplayName;
    /** Start Time **/
    private long mStartTime;
    /** Current Time **/
    private long mCurrentTime;
    /** Saved flag **/
    private boolean mSaved2mFlag;
    /** Saved flag **/
    private boolean mSaved10mFlag;
    /** Distance **/
    private int mDistance;
    /** 1st Detect flag **/
    private Boolean m1stDetect;


    ScannedDevice(BluetoothDevice device, int rssi) {
        if (device == null) {
            throw new IllegalArgumentException("BluetoothDevice is null");
        }
        mDevice = device;
        mDisplayName = device.getName();
        if ((mDisplayName == null) || (mDisplayName.length() == 0)) {
            mDisplayName = UNKNOWN;
        }
        mRssi = rssi;
        m1stDetect = FALSE;

    }

    BluetoothDevice getDevice() {
        return mDevice;
    }

    int getRssi() {
        return mRssi;
    }

    void setRssi(int rssi) {
        mRssi = rssi;
    }

    String getDisplayName() {
        return mDisplayName;
    }

    long getmStartTime() {
        return mStartTime;
    }

    void setmStartTime(long startTime) {
        mStartTime = startTime;
    }

    long getmCurrentTime() {
        return mCurrentTime;
    }

    void setCurrentTime(long currentTime) {
        mCurrentTime = currentTime;
    }

    boolean getmSaved2mFlag() {
        return mSaved2mFlag;
    }

    void setmSaved2mFlag(boolean flag) { mSaved2mFlag = flag; }

    boolean getmSaved10mFlag() {
        return mSaved10mFlag;
    }

    void setmSaved10mFlag(boolean flag) { mSaved10mFlag = flag; }

    int getDistance() {
        return mDistance;
    }

    void setDistance(int distance) {
        mDistance = distance;
    }

    boolean getm1stDetect() { return m1stDetect; }

    void setm1stDetect(boolean flag) { m1stDetect = flag; }
}
