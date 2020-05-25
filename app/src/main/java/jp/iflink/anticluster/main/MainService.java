package jp.iflink.anticluster.main;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import jp.iflink.anticluster.R;
import jp.iflink.anticluster.home.BleScanTask;
import jp.iflink.anticluster.logging.LoggingTask;


public class MainService extends IntentService {
    private static final String TAG = "main";
    private static final String SERVICE_NAME = "MainService";
    private static final String CHANNEL_ID = "mainService";
    private static final String CONTENT_TEXT = "サービスを実行中です";

    // BLEスキャンタスク
    private BleScanTask bleScanTask;
    // ロギングタスク
    private LoggingTask loggingTask;
   // バインダー
    private final IBinder binder = new MainService.LocalBinder();
    public class LocalBinder extends Binder {
        public MainService getService() {
            return MainService.this;
        }
    }

    // コンストラクタ
    public MainService() {
        this(SERVICE_NAME);
    }

    // コンストラクタ（サービス名指定）
    public MainService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "onHandleIntent");
        // 処理実行
        execute(intent);
    }

    public void execute(Intent intent){
        Log.d(TAG, "service execute");
        // パラメータを取得
        boolean blescan_task = intent.getBooleanExtra(BleScanTask.NAME, false);
        boolean logging_task = intent.getBooleanExtra(LoggingTask.NAME, false);
        Log.d(TAG, "blescan_task="+blescan_task+", logging_task="+logging_task);
        if (blescan_task){
            // BLEスキャンタスクを起動
            this.bleScanTask = new BleScanTask();
            bleScanTask.init(getApplicationContext());
            bleScanTask.initScan();
            new Thread(bleScanTask).start();
        }
        if (logging_task){
            // ロギングタスクをスレッド起動
            this.loggingTask = new LoggingTask();
            loggingTask.init(getApplicationContext());
            new Thread(loggingTask).start();
        }
        while (bleScanTask != null || loggingTask != null) {
            Thread.yield();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "service create");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "service start");

        if(Build.VERSION.SDK_INT >= 26) {
            // Android8以降の場合、フォアグラウンド開始処理を実施
            startForeground();
        }
        // Serviceがkillされたときは再起動しない
        return super.onStartCommand(intent, flags, startId);
    }

    @RequiresApi(api = 26)
    private void startForeground(){
        Log.d(TAG, "service start foreground");

        Context context = getApplicationContext();
        Resources resources = context.getResources();
        // タイトルを取得
        final String TITLE = resources.getString(R.string.app_name);
        // 通知マネージャを生成
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // 通知チャンネルを生成
        NotificationChannel channel =
                new NotificationChannel(CHANNEL_ID, TITLE, NotificationManager.IMPORTANCE_DEFAULT);
        if(notificationManager != null) {
            // サービス起動の通知を送信
            notificationManager.createNotificationChannel(channel);
            Notification notification = new Notification.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentTitle(TITLE)
                    .setContentText(CONTENT_TEXT)
                    .build();
            // フォアグラウンドで実行
            startForeground(1, notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "service done");
        // BLEスキャンタスクを停止
        if (bleScanTask != null){
            this.bleScanTask.stop();
        }
        // ロギングタスクを停止
        if (loggingTask != null){
            this.loggingTask.stop();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    public BleScanTask getBleScanTask() {
        return this.bleScanTask;
    }

    public BluetoothAdapter getBtAdapter() {
        if (this.bleScanTask == null){
            return null;
        }
        return this.bleScanTask.getBtAdapter();
    }
}
