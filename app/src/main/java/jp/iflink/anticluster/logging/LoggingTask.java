package jp.iflink.anticluster.logging;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jp.iflink.anticluster.R;
import jp.iflink.anticluster.home.FileHandler;


public class LoggingTask implements Runnable {
    private static final String TAG = "LOG";
    public static final String ACTION_LOGGING = TAG+".LOGGING";
    public static final String NAME = "Logging";

    // コンテキスト
    private Context applicationContext;
    // logcatプロセス
    private Process logcatProc;
    // ブロードキャストマネージャ
    private LocalBroadcastManager broadcastMgr;
    // 実行状態
    private boolean running = false;

    public void init(Context applicationContext){
        // アプリケーションコンテキストを設定
        this.applicationContext = applicationContext;
        // ブロードキャストマネージャを生成
        this.broadcastMgr = LocalBroadcastManager.getInstance(applicationContext);
    }

    @Override
    public void run(){
        // 実行中状態に設定
        running = true;
        Log.d(TAG, "service run");

        // アプリのプロセスIDを取得
        final String pId = Integer.toString(android.os.Process.myPid());
        Resources resources = applicationContext.getResources();
        //  ログの再チェック間隔を取得
        final int logging_check_millis = resources.getInteger(R.integer.logging_check_millis);
        //  ログ取得間隔を取得
        final int logging_interval_millis = resources.getInteger(R.integer.logging_interval_millis);
        // ログ画面使用可否を取得
        final boolean logging_screen = resources.getBoolean(R.bool.logging_screen);
        // ログファイル出力有無を取得
        final boolean logging_file = resources.getBoolean(R.bool.logging_file);

        try {
            PrintWriter writer = null;
            String logFileName = null;
            // logcatプロセスを起動
            // threadtime形式(時刻まで出力する既定のログ形式)でログを取得する。
            logcatProc = Runtime.getRuntime().exec(new String[] { "logcat", "-v", "threadtime", "-v", "year", "--pid="+pId});
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(logcatProc.getInputStream()), 1024)){
                while(isProcessAlive(logcatProc) && running){
                    //  ログ出力行を読み込み
                    String line = reader.readLine();
                    if (line == null || line.length() == 0) {
                        // ログ出力が無い場合は、再チェック間隔まで待機して続行
                        try {
                            Thread.sleep(logging_check_millis);
                        } catch (InterruptedException e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                        continue;
                    }
                    if (logging_screen){
                        // 送信データを作成
                        Intent intent = new Intent(ACTION_LOGGING);
                        //  ログから日付・時刻とメッセージを取り出す。書式は以下の通り
                        //  year-month-day time  PID  TID priority tag : message
                        String[] columns = line.split("\\s+", 6);
                        if (columns.length != 6){
                            continue;
                        }
                        intent.putExtra("date", columns[0]);
                        intent.putExtra("time", columns[1]);
                        //intent.putExtra("PID", columns[2]);
                        //intent.putExtra("TID", columns[3]);
                        intent.putExtra("priority", columns[4]);
                        intent.putExtra("message", columns[5]);
                        intent.putExtra("raw", line);
                        // ログ画面に送信
                        broadcastMgr.sendBroadcast(intent);
                    }
                    if (logging_file){
                        // ログファイル名を作成
                        String fileName = createLogFileName(new Date());
                        if (!fileName.equals(logFileName)){
                            if (writer != null){
                                writer.close();
                            }
                            // 時間単位でログファイルをローテート
                            writer = createWriter(fileName);
                            logFileName = fileName;
                        }
                        // ログファイルに出力
                        writer.println(line);
                        writer.flush();
                    }
                    // 指定秒数待機して続行
                    try {
                        Thread.sleep(logging_interval_millis);
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            } finally {
                if (writer != null){
                    writer.close();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        stop();
    }

    private String createLogFileName(Date date){
        SimpleDateFormat sdfYMDH = new SimpleDateFormat("yyyyMMdd_HH", Locale.US);
        String fileName = "anticluster_debug_"+sdfYMDH.format(date)+".txt";
        return fileName;
    }

    private PrintWriter createWriter(String fileName) throws FileNotFoundException {
        FileHandler fileHandler = new FileHandler(applicationContext);
        File logFile = fileHandler.getFile(fileName);
        return new PrintWriter(new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8));
    }


    private boolean isProcessAlive(Process process){
        try {
            if (process != null) {
                process.exitValue();
            }
            // プロセスがnullまたは終了コードが取得できた場合は、終了状態
            return false;
        } catch (IllegalThreadStateException e){
            // IllegalThreadStateExceptionが発生する場合は、まだ実行中
            return true;
        }
    }

    public void stop(){
        if (logcatProc != null){
            // logプロセスを終了
            logcatProc.destroy();
            logcatProc = null;
        }
        // 終了状態に設定
        running = false;
    }
}
