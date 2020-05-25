package jp.iflink.anticluster.listener;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.nio.ByteBuffer;

import jp.iflink.anticluster.R;
import jp.iflink.anticluster.main.MainActivity;
import jp.iflink.anticluster.main.MainService;
import jp.iflink.anticluster.home.HomeFragment;

public class SendDataListener implements View.OnClickListener {

    private static final String TAG = "SendData";

    @Override
    public void onClick(final View view) {
        Log.d(TAG, "click");
        // 処理起動
        new Thread(new Runnable() {
            public void run() {
                try {
                    short sendValue = HomeFragment.mLevelValue;
                    Log.d(TAG, "start: sendValue="+sendValue);
                    // データ送信
                    sendAdvertise(sendValue, view);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    view.post(new Runnable() {
                        public void run() {
                            // エラー表示
                            showMessage(view, view.getResources().getString(R.string.send_data_error));
                        }
                    });
                } finally {
                    Log.d(TAG, "end");
                }
            }
        }).start();
    }

    /**
     * 送信データを付加したアドバタイズパケットを発信する
     * @param value 送信データ
     */
    private void sendAdvertise(short value, final View view) {
        // メインサービス取得
        MainService bleService = MainActivity.getService();
        if(bleService == null || bleService.getBtAdapter() == null){
            Log.d(TAG, "BleAdvertiser is null");
            return;
        }
        // サービスデータ取得
        byte[] service_data = ByteBuffer.allocate(2).putShort(value).array();

        // データ送信設定値の取得
        String defaultUUID = view.getResources().getString(R.string.send_data_UUID);
        ParcelUuid UUID = ParcelUuid.fromString(MainActivity.prefs.getString("send_uuid",defaultUUID));
        int timeout = Integer.parseInt(view.getResources().getString(R.string.send_data_timeout_millis));

        // アドバタイズ設定の作成
        AdvertiseSettings.Builder settingBuilder = new AdvertiseSettings.Builder();
        settingBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        settingBuilder.setConnectable(false);
        settingBuilder.setTimeout(timeout);
        settingBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW);
        AdvertiseSettings settings = settingBuilder.build();

        // アドバタイズデータの作成
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addServiceUuid(UUID);
        dataBuilder.addServiceData(UUID, service_data);
        final AdvertiseData advertiseData = dataBuilder.build();
        Log.d(TAG, "created: "+advertiseData.toString());

        // アドバタイザー取得
        BluetoothLeAdvertiser advertiser = bleService.getBtAdapter().getBluetoothLeAdvertiser();

        //アドバタイズを開始
        advertiser.startAdvertising(settings, advertiseData, new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                showMessage(view, view.getResources().getString(R.string.send_data_success));
                super.onStartSuccess(settingsInEffect);
            }
            @Override
            public void onStartFailure(int errorCode) {
                Log.e(TAG, "startFailure: errorCode="+errorCode);
                showMessage(view, view.getResources().getString(R.string.send_data_fail));
                super.onStartFailure(errorCode);
            }
        });
    }

    private void showMessage(final View view, final String message){
        view.post(new Runnable() {
            public void run() {
                Toast.makeText(view.getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
