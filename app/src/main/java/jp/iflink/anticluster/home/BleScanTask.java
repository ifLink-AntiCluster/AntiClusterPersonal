package jp.iflink.anticluster.home;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import jp.iflink.anticluster.R;
import jp.iflink.anticluster.main.MainActivity;

import static jp.iflink.anticluster.main.MainActivity.prefs;


public class BleScanTask  implements Runnable {
    private static final String TAG = "BLE";
    public static final String ACTION_SCAN = TAG+".SCAN";
    public static final String NAME = "BleScan";
    private static final int REQUEST_ENABLE_BT = 1048;

    // コンテキスト
    private Context applicationContext;
    // ブロードキャストマネージャ
    private LocalBroadcastManager broadcastMgr;

    private BluetoothManager mBtManager;
    private BluetoothAdapter mBtAdapter;
    private BluetoothLeScanner mBTLeScanner;
    private BluetoothGatt mBtGatt;
    private int mStatus;

    private ScanCallback mScanCallback;
    private AtomicInteger mAlertCounter = new AtomicInteger(0);
    private AtomicInteger mCautionCounter = new AtomicInteger(0);
    private AtomicInteger mDistantCounter = new AtomicInteger(0);
    private List<ScannedDevice> mScanDeviceList;

    // 設定値
    private int THRESHOLD_2M;
    private int THRESHOLD_10M;
    private int ALERT_TIMER;
    private int LOGGING_SETTING;

    private ScanSettings scSettings;
    private List<ScanFilter> mScanFilters;

    private FileHandler fileHandler;
    private Timer collectorTimer ;
    private String mRecordTime;
    // データストア
    private DataStore dataStore;

    // BLE scan off/onタイマ
    private int ScanRestartCount;
    private static final int SCAN_RESTART_TIMER = 3;

    // Advertise Flags
    private static final int BLE_CAPABLE_CONTROLLER = 0x08;
    private static final int BLE_CAPABLE_HOST = 0x10;
    // BLE Company code
    private static final int COMPANY_CODE_APPLE = 0x004C;
    private static final int IPHONE_THRESHOLD = -80;

    @Override
    public void run(){
        // デバイス一覧を読み込み
        int Saved_time = Integer.parseInt( MainActivity.prefs.getString("Saved_time","0"));
        if( (Integer.parseInt(getSystemTime()) - Saved_time) < 1){
            this.mScanDeviceList = loadList("device_list");
            // Alert,Caution,Distantを復元
            for (ScannedDevice device : this.mScanDeviceList) {
                if(device.getmSaved2mFlag()){
                    mAlertCounter.incrementAndGet();
                }
                else {
                    if( THRESHOLD_2M <= device.getRssi()){
                        mCautionCounter.incrementAndGet();
                    }
                    else{
                        mDistantCounter.incrementAndGet();
                    }
                }
            }
        }

        // データ集計用定期処理（1分間隔）
        int timerDelay = getDelayMillisToNextMinute();
        collectorTimer.cancel();
        collectorTimer = new Timer(true);

        collectorTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "BLE Service timer");
                final boolean justTime10m = isJustTimeMinuteOf(10);
                // 10分でScan Stop/Start
                if(ScanRestartCount == SCAN_RESTART_TIMER){
                    Log.d(TAG, "Scan Stop/Start");
                    stopScan();
                    startScan();
                    ScanRestartCount = 0;
                }
                else{
                    ScanRestartCount++;
                }
                // 10分毎に実施
                if (justTime10m){
                    return;
                }
                // データ保存
                Log.d(TAG, "record data");
                CounterDevice data = new CounterDevice();
                data.mAlertCounter = mAlertCounter.get();
                data.mCautionCounter = mCautionCounter.get();
                data.mDistantCounter = mDistantCounter.get();
                try {
                    // 集計を実施
                    Date recordTime = new Date();
                    dataStore.writeRecord(data, new Date());
                    // 前回集計時刻を更新
                    mRecordTime = getDateTimeText(recordTime);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                    showMessage(applicationContext.getResources().getString(R.string.data_save_fail));
                }
                // データをクリア
                scanDataClear();
            }
        }, timerDelay, 1000*60);

    }

    private boolean isJustTimeMinuteOf(int minute){
        // 時刻を1分単位の精度にして指定したminute毎かどうかを判定
        return (int)(new Date().getTime()/60000) % minute != 0;
    }

    String getmRecordTime(){ return mRecordTime;}

    public boolean init(Context applicationContext) {
        // アプリケーションコンテキストを設定
        this.applicationContext = applicationContext;
        // ブロードキャストマネージャを生成
        this.broadcastMgr = LocalBroadcastManager.getInstance(applicationContext);
        // BLEステータス更新
        this.mStatus = BluetoothProfile.STATE_DISCONNECTED;

        // Bluetoothマネージャの生成
        if (this.mBtManager == null) {
            this.mBtManager = (BluetoothManager)applicationContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (this.mBtManager == null) {
                return false;
            }
        }
        // Bluetoothアダプタの生成
        this.mBtAdapter = this.mBtManager.getAdapter();

        // 設定値の読み込み
        Resources resources = applicationContext.getResources();
        this.THRESHOLD_2M = Integer.parseInt(prefs.getString("rssi_2m",resources.getString(R.string.default_rssi)));
        this.THRESHOLD_10M = Integer.parseInt(prefs.getString("rssi_around",resources.getString(R.string.default_around_rssi)));
        this.ALERT_TIMER = Integer.parseInt(prefs.getString("alert_timer",resources.getString(R.string.default_alert_timer)));
        this.LOGGING_SETTING = Integer.parseInt(prefs.getString("logging_setting",resources.getString(R.string.default_logging_setting)));

        return this.mBtAdapter != null;
    }

    private void close() {
        if (this.mBtGatt == null) {
            return;
        }
        this.mBtGatt.close();
        this.mBtGatt = null;
    }

    public void initScan() {
        // データストアの初期化
        this.dataStore = new DataStore(this.applicationContext);
        // タイマーの初期化
        this.collectorTimer = new Timer(true);
        // デバイス一覧の初期化
        this.mScanDeviceList = new ArrayList<>();
        // カウンターの初期化
        this.mAlertCounter = new AtomicInteger(0);
        this.mCautionCounter = new AtomicInteger(0);
        this.mDistantCounter = new AtomicInteger(0);
        // スキャンモードの取得
        int scan_setting = Integer.parseInt(prefs.getString("scan_setting",String.valueOf(0)));

        ScanSettings.Builder scanSettings = new ScanSettings.Builder();
        scanSettings.setScanMode(scan_setting).build();
        this.scSettings = scanSettings.build();
        this.mScanFilters = new ArrayList<>();

        this.mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                Log.d(TAG, result.toString());
                // listに追加
                update(result.getDevice(), result.getRssi(), result);
            }
        };

        this.fileHandler = new FileHandler(this.applicationContext);

        this.ScanRestartCount = 0;

        // スキャン初期化完了を配信
        Intent intent = new Intent(ACTION_SCAN);
        broadcastMgr.sendBroadcast(intent);
    }

    public void stop(){
        close();
        // スキャンを停止
        stopScan();
        // タイマーを停止
        if (collectorTimer != null){
            collectorTimer.cancel();
        }
        if (mScanDeviceList != null){
            // デバイスリストを保存
            saveList("device_list", (ArrayList<ScannedDevice>) this.mScanDeviceList);
        }
    }

    public void enableBluetooth(Activity activity){
        // Bluetooth有効化
        BluetoothAdapter btAdapter = getBtAdapter();
        if (btAdapter != null) {
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    public BluetoothAdapter getBtAdapter() {
        return this.mBtAdapter;
    }

    public void startScan() {

        this.mBTLeScanner = this.mBtAdapter.getBluetoothLeScanner();

        if (this.mBtAdapter != null) {
            Log.d(TAG, "startScan()");
            this.mBTLeScanner.startScan(mScanFilters, scSettings, mScanCallback);
        }
    }

    private void stopScan() {

        if (this.mBtAdapter != null) {
            Log.d(TAG, "stopScan()");
            if (this.mBTLeScanner != null){
                this.mBTLeScanner.stopScan(mScanCallback);
            }
        }
        else {
            Log.d(TAG, "BluetoothAdapter :null");
        }
    }

    private void update(BluetoothDevice newDevice, int rssi, ScanResult result) {
        if (newDevice == null || newDevice.getAddress() == null || newDevice.getAddress().isEmpty()) {
            // アドレスが未設定のデータは対象外
            return;
        }
        SparseArray<byte[]> mnfrData = null;
        Integer advertiseFlags = null;
        if (result.getScanRecord() != null){
            mnfrData = result.getScanRecord().getManufacturerSpecificData();
            advertiseFlags = result.getScanRecord().getAdvertiseFlags();
        } else {
            // スキャンレコードが無いデータは対象外
            return;
        }
        if( advertiseFlags == -1 || (advertiseFlags & (BLE_CAPABLE_CONTROLLER | BLE_CAPABLE_HOST)) == 0){
            // DeviceCapableにControllerとHostが無い場合は対象外
            return;
        }
        // スキャン日時を取得
        long rxTimestampMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime() + result.getTimestampNanos() / 1000000;
        Date scanDate = new Date(rxTimestampMillis);

        boolean contains = false;

        for (ScannedDevice device : this.mScanDeviceList) {
            // 受信済みチェック
            if (newDevice.getAddress().equals(device.getDevice().getAddress())) {
                // 受信済みの場合
                contains = true;
                device.setRssi(rssi); // update
                if(THRESHOLD_2M <= rssi){
                    if(!device.getm1stDetect()){
                        device.setmStartTime(rxTimestampMillis);
                        device.setm1stDetect(true);
                        // many near yellow
                        debug_log(scanDate, newDevice, rssi,1,0, mnfrData,0,1, advertiseFlags);
                    }
                    else{
                        if(!device.getmSaved2mFlag()) {
                            device.setCurrentTime(rxTimestampMillis);
                            // many near yellow
                            debug_log(scanDate, newDevice, rssi,1,(rxTimestampMillis - device.getmStartTime()), mnfrData,0,1, advertiseFlags);
                            if((rxTimestampMillis - device.getmStartTime()) > (ALERT_TIMER*60*1000)) {
                                mAlertCounter.incrementAndGet();
                                if (mCautionCounter.getAndDecrement()==0){
                                    mCautionCounter.set(0);
                                }

                                device.setmSaved2mFlag(true);
                                // many near red
                                debug_log(scanDate, newDevice, rssi,1,(rxTimestampMillis - device.getmStartTime()), mnfrData,0,0, advertiseFlags);
                                Log.d(TAG, "alert count up");
                            }
                        }
                        else{
                            // many near red
                            device.setCurrentTime(rxTimestampMillis);
                            debug_log(scanDate, newDevice, rssi,1,(rxTimestampMillis - device.getmStartTime()), mnfrData,0,0, advertiseFlags);
                        }
                    }
                }
                else if(THRESHOLD_10M < rssi){
                    device.setm1stDetect(false);
                    // many around purple
                    debug_log(scanDate, newDevice, rssi,1,(rxTimestampMillis - device.getmStartTime()), mnfrData,1,1, advertiseFlags);
                }
                else{
                    device.setm1stDetect(false);
                    // many far purple
                    debug_log(scanDate,newDevice,rssi,1,(rxTimestampMillis - device.getmStartTime()), mnfrData,2,2, advertiseFlags);
                }
                break;
            }
        }
        if (!contains) {
            // add new BluetoothDevice
            if(newDevice.getName() == null || newDevice.getName().isEmpty()) {
                if (THRESHOLD_2M <= rssi) {
                    this.mScanDeviceList.add(new ScannedDevice(newDevice, rssi));
                    mCautionCounter.incrementAndGet();
                    // once near yellow
                    debug_log(scanDate, newDevice, rssi,0,0, mnfrData,0, 1, advertiseFlags);
                } else if (THRESHOLD_10M < rssi) {
                    this.mScanDeviceList.add(new ScannedDevice(newDevice, rssi));
                    mDistantCounter.incrementAndGet();
                    // once around purple
                    debug_log(scanDate, newDevice, rssi,0,0, mnfrData,1,2, advertiseFlags);
                } else {
                    // once far purple
                    debug_log(scanDate, newDevice, rssi,0,0, mnfrData,2,2, advertiseFlags);
                }
            }
        }
    }

    int getAlertCounter(){
        return mAlertCounter.get();
    }

    int getCautionCounter(){
        return mCautionCounter.get();
    }

    int getDistantCounter(){
        return mDistantCounter.get();
    }

    void setThreshold_2m(int threshold){
        this.THRESHOLD_2M = threshold;
    }

    void setThreshold_10m(int threshold){
        this.THRESHOLD_10M = threshold;
    }

    void setAlertTimer(int timer){
        this.ALERT_TIMER = timer;
    }

    void setLoggingSetting(int check){
        this.LOGGING_SETTING = check;
    }

    private void scanDataClear(){
        Log.d(TAG, "Clear scan Data");
        long systemTime = System.currentTimeMillis();
        Iterator<ScannedDevice> listIt = this.mScanDeviceList.iterator();
        while(listIt.hasNext()) {
            ScannedDevice device = listIt.next();
            long deviceTime = device.getmCurrentTime();
            if((device.getmSaved2mFlag())&&(( systemTime - deviceTime)>(ALERT_TIMER*60*1000))){
                mAlertCounter.decrementAndGet();
                if (mAlertCounter.get()<0){
                    mAlertCounter.set(0);
                }
                listIt.remove();
            }
            else if(!device.getm1stDetect()){
                listIt.remove();
            }
            else if(!device.getmSaved2mFlag()&&((systemTime - deviceTime)>(ALERT_TIMER*60*1000))){
                listIt.remove();
            }
        }
        mCautionCounter.set(0);
        mDistantCounter.set(0);
}

    private static void saveList(String key, ArrayList<ScannedDevice> list) {
    Gson gson = new Gson();

    SharedPreferences.Editor editor = MainActivity.prefs.edit();
        editor.putString(key, gson.toJson(list));
        editor.apply();
}

    private static ArrayList<ScannedDevice> loadList(String key) {
        ArrayList<ScannedDevice> list = new ArrayList<>();

        String strJson = prefs.getString(key, "");
        if(!strJson.equals("")) {
            try {
                Gson gson = new Gson();
                list = gson.fromJson(prefs.getString(key, ""), new TypeToken<ArrayList<ScannedDevice>>(){}.getType());
            } catch(Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return list;
    }

    private String getSystemTime() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH", Locale.getDefault());
        return sdf.format(date);
    }

    private int getDelayMillisToNextMinute(){
        Calendar nextMinute = Calendar.getInstance();
        nextMinute.set(Calendar.SECOND, 0);
        nextMinute.set(Calendar.MILLISECOND, 0);
        nextMinute.add(Calendar.MINUTE, 1);
        return (int)(nextMinute.getTime() .getTime() - new Date().getTime());
    }

    private String getDateTimeText(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    private void showMessage(final String message){
        if(this.applicationContext != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void debug_log(Date scanDate, BluetoothDevice device, int rssi, int continuous, long count_time, SparseArray<byte[]> mnfrData, int distance, int judge, Integer flags){
        if(LOGGING_SETTING==0){
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        String dateStr = sdf.format(scanDate);
        String occur, dist, jud;
        String mnfrDataStr = spArrayToStr(mnfrData);
        final int company = getCompanyFromSpArray(mnfrData);

        SimpleDateFormat sdfYMDH = new SimpleDateFormat("yyyyMMdd_HH", Locale.US);
        String csvFileName = "AntiClusterLog_"+sdfYMDH.format(scanDate)+".txt";
        File csvFile = fileHandler.getFile(csvFileName);
        boolean isNewFile = !csvFile.exists();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile,true))) {
            if (isNewFile){
                // ヘッダー行を出力する
                writer.write("count_time,address,name,rssi,continuous,distance,judge,count_time,alert,caution,distant,manufacturer_specific_data,flags,company");
                writer.newLine();
            }
            writer.write(dateStr + ",");
            writer.write(device.getAddress()+ ",");
            writer.write(device.getName() + ",");
            writer.write(rssi + ",");
            if(continuous == 0){
                writer.write("once,");
                occur = "once";
            }else if(continuous == 1){
                writer.write("many,");
                occur = "many";
            } else {
                occur = "";
            }
            if(distance == 0){
                writer.write("near,");
                dist = "near";
            }else if(distance == 1){
                writer.write("around,");
                dist = "around";
            }else{
                writer.write("far,");
                dist = "far";
            }
            if(judge == 0){
                writer.write("red,");
                jud = "red";
            }else if(judge == 1){
                writer.write("yellow,");
                jud = "yellow";
            }else{
                writer.write("purple,");
                jud = "purple";
            }
            writer.write(count_time + ",");
            writer.write(mAlertCounter + ",");
            writer.write(mCautionCounter + ",");
            writer.write(mDistantCounter + ",");
            writer.write(mnfrDataStr + ",");
            writer.write(flags + ",");
            writer.write(company + "");
            writer.newLine();
            Log.i(TAG,device.getAddress()+ ","+device.getName() + ","+ rssi + ","+ occur +"," +
                    ""+dist+","+jud+","+count_time+","+mAlertCounter+","+mCautionCounter+","+mDistantCounter+","+
                    mnfrDataStr +","+flags +","+company);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private static int getCompanyFromSpArray(SparseArray<byte[]> data){
        if (data != null && data.size()>0){
            return data.keyAt(0);
        }
        return -1;
    }

    private static String spArrayToStr(SparseArray<byte[]> data){
        if (data == null){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (data.size()>0){
            for (int idx=0; idx<data.size(); idx++){
                int key = data.keyAt(idx);
                byte[] array = data.valueAt(idx);
                sb.append(key).append("=0x").append(binToHexStr(array)).append(",");
            }
            sb.setLength(sb.length()-",".length());
        }
        sb.append("}");
        return sb.toString();
    }

    private static String binToHexStr(byte[] array){
        StringBuilder sb = new StringBuilder();
        for (byte d : array) {
            sb.append(String.format("%02X", d));
        }
        return sb.toString();
    }

}
