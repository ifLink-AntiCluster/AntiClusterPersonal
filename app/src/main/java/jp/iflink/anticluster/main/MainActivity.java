package jp.iflink.anticluster.main;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;

import com.google.android.material.navigation.NavigationView;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import jp.iflink.anticluster.R;
import jp.iflink.anticluster.home.BleScanTask;
import jp.iflink.anticluster.logging.LoggingTask;

import android.content.pm.PackageManager;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "main";
    private AppBarConfiguration mAppBarConfiguration;

    public static SharedPreferences prefs;

    private static MainService mService;

    public static MainService getService(){
        return mService;
    }

    String[] permissions = new String[] {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    public MainActivity() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_setting
        )
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Authority
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermission();
        }

        prefs = getSharedPreferences("SaveData", Context.MODE_PRIVATE);

        // BLEスキャン用のレシーバーを登録
        LocalBroadcastManager broadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        broadcastMgr.registerReceiver(bleScanReceiver, new IntentFilter(BleScanTask.ACTION_SCAN));

        // メインサービスを開始
        startMainService();
    }

    private void startMainService(){
        // パラメータの取得
        boolean  logging_task = getResources().getBoolean(R.bool.logging_task);
        // メインサービスの開始
        Intent serviceIntent = new Intent(this, MainService.class);
        serviceIntent.putExtra(BleScanTask.NAME, true);
        serviceIntent.putExtra(LoggingTask.NAME, logging_task);

        if (Build.VERSION.SDK_INT >= 26) {
            // Android8以降の場合
            startForegroundService(serviceIntent);
        } else {
            // Android7以前の場合
            startService(serviceIntent);
        }
    }

    // BLEスキャン用レシーバ
    private BroadcastReceiver bleScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // メインサービスにバインド
            Intent serviceIntent = new Intent(MainActivity.this, MainService.class);
            bindService(serviceIntent, mServiceConnection, 0);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity onDestroy");
        // BLEスキャン用のレシーバーを解除
        LocalBroadcastManager broadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        broadcastMgr.unregisterReceiver(bleScanReceiver);
        // メインサービスのアンバインド
        unbindService(mServiceConnection);
        mService = null;
        // メインサービスの停止
        Intent serviceIntent = new Intent(this, MainService.class);
        stopService(serviceIntent);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentname, IBinder service) {
            Log.d(TAG, "MainActivity onServiceConnected");
            // メインサービス取得
            MainService mainService = ((MainService.LocalBinder) service).getService();
            // BLEスキャンタスク取得
            BleScanTask bleScanTask = mainService.getBleScanTask();
            if (bleScanTask != null){
                // Bluetooth有効化
                bleScanTask.enableBluetooth(MainActivity.this);
                // BLEスキャン開始
                bleScanTask.startScan();
            }

            // メインサービスを設定
            mService = mainService;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentname) {
            Log.d(TAG, "MainActivity onServiceDisconnected");
            mService = null;
        }
    };

    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermission() {
        ArrayList<String> listRequestPermissions = new ArrayList<>();

        for (String p:permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(getBaseContext(), p);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                listRequestPermissions.add(p);
            }
        }
        if (!listRequestPermissions.isEmpty()) {
            String[] array = new String[listRequestPermissions.size()];
            requestPermissions(listRequestPermissions.toArray(array), 1);
        }
    }

    @Override
    public void onBackPressed() {
        // 戻るボタンを無効化
    }
}


