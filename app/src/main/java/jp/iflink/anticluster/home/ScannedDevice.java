package jp.iflink.anticluster.home;

import android.bluetooth.BluetoothDevice;

import jp.iflink.anticluster.model.CountType;

import static java.lang.Boolean.FALSE;

class ScannedDevice {
    private static final String UNKNOWN = "Unknown";
    /** BluetoothDevice Address */
    private String mDeviceAddress;
    /** RSSI */
    private int mRssi;
    /** Display Name */
    private String mDisplayName;
    /** Start Time **/
    private long mStartTime;
    /** Current Time **/
    private long mCurrentTime;
    /** alert flag **/
    private boolean mAlertFlag;
    /** 1st Detect flag **/
    private Boolean m1stDetect;
    /** Count type */
    private CountType countType;

    ScannedDevice(BluetoothDevice device, int rssi) {
        if (device == null) {
            throw new IllegalArgumentException("BluetoothDevice is null");
        }
        mDeviceAddress = device.getAddress();
        mDisplayName = device.getName();
        if (mDisplayName == null || mDisplayName.isEmpty()) {
            mDisplayName = UNKNOWN;
        }
        mRssi = rssi;
        m1stDetect = FALSE;

    }

    public String getDeviceAddress() { return mDeviceAddress; }

    public int getRssi() { return mRssi;}

    public void setRssi(int rssi) { this.mRssi = rssi; }

    public String getDisplayName() { return mDisplayName; }

    public long getmStartTime() { return mStartTime; }

    public void setmStartTime(long startTime) { this.mStartTime = startTime; }

    public long getmCurrentTime() { return mCurrentTime; }

    public void setCurrentTime(long currentTime) { this.mCurrentTime = currentTime; }

    public boolean getAlertFlag() { return mAlertFlag; }

    public void setAlertFlag(boolean flag) { this.mAlertFlag = flag; }

    public boolean getm1stDetect() { return m1stDetect; }

    public void setm1stDetect(boolean flag) { this.m1stDetect = flag; }

    public CountType getCountType() { return countType; }

    public void setCountType(CountType countType) { this.countType = countType; }
}
