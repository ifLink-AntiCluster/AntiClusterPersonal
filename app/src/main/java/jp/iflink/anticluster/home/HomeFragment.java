package jp.iflink.anticluster.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.github.mikephil.charting.charts.BarChart;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import jp.iflink.anticluster.main.MainActivity;
import jp.iflink.anticluster.R;
import jp.iflink.anticluster.main.MainService;
import jp.iflink.anticluster.listener.SendDataListener;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class HomeFragment extends Fragment {
    private static final String TAG = "Home";

    private TextView mLevelDescription;
    private TextView mPeriod;

    private ToggleButton m4Hours;
    private ToggleButton m1Day;
    private ToggleButton m2Week;

    // レベル値
    public static short mLevelValue;
    // レベル表示ブロック
    private LinearLayout mLevelBlock;
    // 前回集計時時刻
    private Date mPrevRecordTime;
    // データ送信ボタンの設定
    private Button mDataSend;
    // 集計値切り替えボタン、集計値モードテキストの設定
    private ImageButton mValueSwitch;
    private TextView mValueMode;
    // 集計値モード定数
    private enum ValueMode {REALTIME, DURINGMAX}
    private ValueMode valueMode = ValueMode.REALTIME;
    // 現在値表示テーブル
    private TableLayout mTableRealtime;
    // 期間中最大値表示テーブル
    private TableLayout mTableDuringmax;
    // リスク判定画像
    private ImageView mLevel0_Image;
    private ImageView mLevel1_Image;
    private ImageView mLevel2_Image;
    private ImageView mLevel3_Image;
    private ImageView mLevel4_Image;
    private ImageView mLevel5_Image;

    private Timer timer ;
    // データ集計用タイマー
    private Timer recordTimeTimer;

    private Handler mHandler;
    private int mAlertDistance;
    private int mAlertTimer;
    private int mAroundDistance;
    private double mAlertCounterCoefficient;
    private double mCautionCounterCoefficient;
    private double mDistantCounterCoefficient;
    private int mLevel1JudgeLine;
    private int mLevel2JudgeLine;
    private int mLevel3JudgeLine;
    private int mLevel4JudgeLine;
    private int mLevel5JudgeLine;
    private int mLoggingSetting;
    private int mUpdateTime;

    private BarChart mBarChartGraph;

    private Boolean m1st_Flag;
    private Boolean m4HoursPushFlag;
    private Boolean mDayPushFlag;
    private Boolean m2WeekPushFlag;

    // 現在値テキスト表示
    private TextView mRealTimeAlert;
    private TextView mRealTimeCaution;
    private TextView mRealTimeDistant;
    // 期間中最大値テキスト表示
    private TextView mDuringMaxAlert;
    private TextView mDuringMaxCaution;
    private TextView mDuringMaxDistant;
    // 前回集計時刻テキスト表示
    private TextView mRecordTime;
    private LinearLayout mRecordTimeBlock;

    // データストア
    private DataStore dataStore;
    // グラフマネージャ
    private GraphManager graphManager;

    @SuppressLint("ResourceAsColor")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        Log.d(TAG, "HomeFragment onCreateView");
        mLevelDescription = root.findViewById(R.id.tv_level_description);
        mLevelBlock = root.findViewById(R.id.lyt_level);

        mLevel0_Image = root.findViewById(R.id.image_level0);
        mLevel1_Image = root.findViewById(R.id.image_level1);
        mLevel2_Image = root.findViewById(R.id.image_level2);
        mLevel3_Image = root.findViewById(R.id.image_level3);
        mLevel4_Image = root.findViewById(R.id.image_level4);
        mLevel5_Image = root.findViewById(R.id.image_level5);
        mBarChartGraph = root.findViewById(R.id.bar_chart_graph);
        graphManager = new GraphManager(mBarChartGraph);

        mRealTimeAlert = root.findViewById(R.id.tv_realtime_alert);
        mRealTimeCaution = root.findViewById(R.id.tv_realtime_caution);
        mRealTimeDistant = root.findViewById(R.id.tv_realtime_distant);

        mDuringMaxAlert = root.findViewById(R.id.tv_duringmax_alert);
        mDuringMaxCaution = root.findViewById(R.id.tv_duringmax_caution);
        mDuringMaxDistant = root.findViewById(R.id.tv_duringmax_distant);

        mRecordTime = root.findViewById(R.id.tv_recordtime);
        mRecordTimeBlock = root.findViewById(R.id.tv_recordtime_block);

        Resources resources = getResources();
        final String title_hour = resources.getString(R.string.title_hour);
        final String title_day = resources.getString(R.string.title_day);
        final String title_period = resources.getString(R.string.title_period);

        final String day_format = resources.getString(R.string.day_format);
        mPeriod = root.findViewById(R.id.tv_period);

        m4Hours = root.findViewById(R.id.toggleButton_hours);
        m4Hours.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("ResourceAsColor")
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    m4HoursPushFlag = TRUE;
                    // The toggle is enabled
                    m4Hours.setTextColor(Color.WHITE);
                    m4Hours.setBackgroundColor(R.color.colorButtonPress);
                    // 1Day設定
                    mDayPushFlag = FALSE;
                    m1Day.setTextColor(R.color.colorText);
                    m1Day.setBackgroundColor(Color.WHITE);
                    // 2Week設定
                    m2WeekPushFlag = FALSE;
                    m2Week.setTextColor(R.color.colorText);
                    m2Week.setBackgroundColor(Color.WHITE);
                    // タイトル設定
                    String dayText = new SimpleDateFormat(day_format, Locale.getDefault()).format(new Date());
                    mPeriod.setText(title_hour);
                    // 4Hoursのグラフを更新
                    CounterDevice maxValueInfo = graphManager.updateGraph4Hours();
                    if (maxValueInfo != null){
                        // 期間中最大値を更新
                        mDuringMaxAlert.setText(String.valueOf(maxValueInfo.mAlertCounter));
                        mDuringMaxCaution.setText(String.valueOf(maxValueInfo.mCautionCounter));
                        mDuringMaxDistant.setText(String.valueOf(maxValueInfo.mDistantCounter));
                    }
                    // リスク判定の更新
                    updateRiskLevel(maxValueInfo);
                } else {
                    if(m4HoursPushFlag != TRUE){
                        // The toggle is disabled
                        m4Hours.setTextColor(R.color.colorText);
                        m4Hours.setBackgroundColor(Color.WHITE);
                    }
                }
            }
        });

        m1Day = root.findViewById(R.id.toggleButton_1day);
        m1Day.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("ResourceAsColor")
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mDayPushFlag = TRUE;
                    // The toggle is enabled
                    m1Day.setTextColor(Color.WHITE);
                    m1Day.setBackgroundColor(R.color.colorButtonPress);
                    // 4Hours設定
                    m4HoursPushFlag = FALSE;
                    m4Hours.setTextColor(R.color.colorText);
                    m4Hours.setBackgroundColor(Color.WHITE);
                    // 2Week設定
                    m2WeekPushFlag = FALSE;
                    m2Week.setTextColor(R.color.colorText);
                    m2Week.setBackgroundColor(Color.WHITE);
                    // タイトル設定
                    String dayText = new SimpleDateFormat(day_format, Locale.getDefault()).format(new Date());
                    mPeriod.setText(title_day.replace("{0}",dayText));
                    // 1Dayのグラフを更新
                    CounterDevice maxValueInfo = graphManager.updateGraph1Day();
                    if (maxValueInfo != null){
                        // 期間中最大値を更新
                        mDuringMaxAlert.setText(String.valueOf(maxValueInfo.mAlertCounter));
                        mDuringMaxCaution.setText(String.valueOf(maxValueInfo.mCautionCounter));
                        mDuringMaxDistant.setText(String.valueOf(maxValueInfo.mDistantCounter));
                    }
                    // リスク判定の更新
                    updateRiskLevel(maxValueInfo);
                } else {
                    if(mDayPushFlag!=TRUE){
                        // The toggle is disabled
                        m1Day.setTextColor(R.color.colorText);
                        m1Day.setBackgroundColor(Color.WHITE);
                    }
                }
            }
        });

        m2Week = root.findViewById(R.id.toggleButton_2week);
        m2Week.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("ResourceAsColor")
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    m2WeekPushFlag = TRUE;
                    // The toggle is enabled
                    m2Week.setTextColor(Color.WHITE);
                    m2Week.setBackgroundColor(R.color.colorButtonPress);
                    // 4Hours設定
                    m4HoursPushFlag = FALSE;
                    m4Hours.setTextColor(R.color.colorText);
                    m4Hours.setBackgroundColor(Color.WHITE);
                    // 1Day設定
                    mDayPushFlag = FALSE;
                    m1Day.setTextColor(R.color.colorText);
                    m1Day.setBackgroundColor(Color.WHITE);
                    // タイトル設定
                    Calendar cal = Calendar.getInstance();
                    Date dayTo = cal.getTime();
                    cal.add(Calendar.DATE, -13);
                    Date dayFrom = cal.getTime();
                    String dayFromText = new SimpleDateFormat(day_format, Locale.getDefault()).format(dayFrom);
                    String dayToText = new SimpleDateFormat(day_format, Locale.getDefault()).format(dayTo);
                    mPeriod.setText(title_period.replace("{0}",dayFromText).replace("{1}",dayToText));
                    // 2Weekのグラフを更新
                    CounterDevice maxValueInfo = graphManager.updateGraph2Week();
                    if (maxValueInfo != null){
                        // 期間中最大値を更新
                        mDuringMaxAlert.setText(String.valueOf(maxValueInfo.mAlertCounter));
                        mDuringMaxCaution.setText(String.valueOf(maxValueInfo.mCautionCounter));
                        mDuringMaxDistant.setText(String.valueOf(maxValueInfo.mDistantCounter));
                    }
                    // リスク判定の更新
                    updateRiskLevel(maxValueInfo);
                } else {
                    if(m2WeekPushFlag != TRUE){
                        // The toggle is disabled
                        m2Week.setTextColor(R.color.colorText);
                        m2Week.setBackgroundColor(Color.WHITE);
                    }
                }
            }
        });

        // 集計値モードテキスト
        mValueMode = root.findViewById(R.id.tv_valueMode);
        final String realtime = resources.getString(R.string.realtime);
        final String duringmax = resources.getString(R.string.duringmax);
        // 現在値表示テーブル
        mTableRealtime = root.findViewById(R.id.table_realtime);
        // 期間中最大値表示テーブル
        mTableDuringmax = root.findViewById(R.id.table_duringmax);
        // 集計値切り替えボタンの設定
        mValueSwitch = root.findViewById(R.id.btn_valueSwitch);
        mValueSwitch.setOnClickListener(new View.OnClickListener (){
            @Override
            public void onClick(View view) {
                if (valueMode == ValueMode.REALTIME){
                    // 集計値モードを期間最大値に切り替え
                    valueMode = ValueMode.DURINGMAX;
                    mValueMode.setText(duringmax);
                    // テーブルの表示切替
                    mTableRealtime.setVisibility(View.GONE);
                    mTableDuringmax.setVisibility(View.VISIBLE);
                } else {
                    // 集計値モードを現在値に切り替え
                    valueMode = ValueMode.REALTIME;
                    mValueMode.setText(realtime);
                    // テーブルの表示切替
                    mTableDuringmax.setVisibility(View.GONE);
                    mTableRealtime.setVisibility(View.VISIBLE);
                }
            }
        });

        // データ送信ボタンの設定
        mDataSend = root.findViewById(R.id.btn_send);
        mDataSend.setOnClickListener(new SendDataListener());

        return root;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "HomeFragment onCreate");

        timer = new Timer(true);
        recordTimeTimer = new Timer(true);
        mHandler = new Handler();

        // データストアの初期化
        dataStore = new DataStore(this.getContext());
        dataStore.init();

        m1st_Flag = FALSE;
        m4HoursPushFlag = FALSE;
        mDayPushFlag = FALSE;
        m2WeekPushFlag = FALSE;

    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "HomeFragment onStart");

        mRealTimeAlert.setText("");
        mRealTimeCaution.setText("");
        mRealTimeDistant.setText("");

        mDuringMaxAlert.setText("");
        mDuringMaxCaution.setText("");
        mDuringMaxDistant.setText("");

        Resources resources = getResources();
        mAlertDistance = Integer.parseInt(MainActivity.prefs.getString("rssi_2m", resources.getString(R.string.default_rssi)));
        mAlertTimer = Integer.parseInt(MainActivity.prefs.getString("alert_timer", resources.getString(R.string.default_alert_timer)));
        mAroundDistance = Integer.parseInt(MainActivity.prefs.getString("rssi_around", resources.getString(R.string.default_around_rssi)));
        mUpdateTime = Integer.parseInt(MainActivity.prefs.getString("update_time", resources.getString(R.string.default_update_time)));
        mLoggingSetting = Integer.parseInt(MainActivity.prefs.getString("logging_setting", resources.getString(R.string.default_logging_setting)));

        mAlertCounterCoefficient = Double.parseDouble(MainActivity.prefs.getString("alert_count_coefficient", resources.getString(R.string.default_alert_counter_coefficient)));
        mCautionCounterCoefficient = Double.parseDouble(MainActivity.prefs.getString("caution_count_coefficient", resources.getString(R.string.default_caution_counter_coefficient)));
        mDistantCounterCoefficient = Double.parseDouble(MainActivity.prefs.getString("distant_count_coefficient", resources.getString(R.string.default_distant_counter_coefficient)));
        mLevel1JudgeLine = Integer.parseInt(MainActivity.prefs.getString("judge_line1", resources.getString(R.string.default_level_judge_1)));
        mLevel2JudgeLine = Integer.parseInt(MainActivity.prefs.getString("judge_line2", resources.getString(R.string.default_level_judge_2)));
        mLevel3JudgeLine = Integer.parseInt(MainActivity.prefs.getString("judge_line3", resources.getString(R.string.default_level_judge_3)));
        mLevel4JudgeLine = Integer.parseInt(MainActivity.prefs.getString("judge_line4", resources.getString(R.string.default_level_judge_4)));
        mLevel5JudgeLine = Integer.parseInt(MainActivity.prefs.getString("judge_line5", resources.getString(R.string.default_level_judge_5)));

        // 画面更新用定期処理（5秒間隔）
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // BLEスキャンタスク取得
                        BleScanTask bleService = getBleScanTask();
                        if (bleService == null) {
                            Log.w(TAG, "bleService is null");
                            return;
                        }

                        // 初期起動時の処理
                        if (m1st_Flag==FALSE){
                            bleService.setThreshold_2m(mAlertDistance);
                            bleService.setAlertTimer(mAlertTimer);
                            bleService.setThreshold_10m(mAroundDistance);
                            bleService.setLoggingSetting(mLoggingSetting);

                            m1st_Flag=TRUE;
                        }
                        // 現在値の更新
                        mRealTimeAlert.setText(String.valueOf(bleService.getAlertCounter()));
                        mRealTimeCaution.setText(String.valueOf(bleService.getCautionCounter()));
                        mRealTimeDistant.setText(String.valueOf(bleService.getDistantCounter()));
                    }
                });
            }

        }, 0, mUpdateTime*1000);

        // 初期実行待ち時間（次の分の05秒）
        int timerDelay = getDelayMillisToNextMinute() + 5000;

        // 前回集計時刻、グラフ更新用定期処理（1分間隔）
        recordTimeTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // 10分毎に実施
                if (isJustTimeMinuteOf(10)){
                    return;
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // BLEスキャンタスク取得
                        BleScanTask bleService = getBleScanTask();
                        if (bleService == null) {
                            Log.w(TAG, "bleService is null");
                            return;
                        }
                        // 前回集計時刻を更新
                        mRecordTime.setText(bleService.getmRecordTime());
                        mRecordTimeBlock.setVisibility(View.VISIBLE);
                        // グラフ更新
                        mPrevRecordTime = graphManager.loadData(new Date());
                        CounterDevice maxValueInfo = graphManager.updateGraph();
                        if (maxValueInfo != null){
                            // 期間中最大値を更新
                            mDuringMaxAlert.setText(String.valueOf(maxValueInfo.mAlertCounter));
                            mDuringMaxCaution.setText(String.valueOf(maxValueInfo.mCautionCounter));
                            mDuringMaxDistant.setText(String.valueOf(maxValueInfo.mDistantCounter));
                            // リスク判定の更新
                            updateRiskLevel(maxValueInfo);
                        }
                    }
                });
            }
        }, timerDelay, 1000*60);

        // グラフマネージャの初期化
        graphManager.init(dataStore);
        // データ読み込み、前回集計時刻の取得
        this.mPrevRecordTime = graphManager.loadData(new Date());
        if (mPrevRecordTime != null){
            // 前回集計時刻を更新
            mRecordTime.setText(getDateTimeText(mPrevRecordTime));
            mRecordTimeBlock.setVisibility(View.VISIBLE);
        }
        // グラフの初期選択：1日
        m1Day.setChecked(true);
    }

    private boolean isJustTimeMinuteOf(int minute){
        // 時刻を1分単位の精度にして指定したminute毎かどうかを判定
        return (int)(new Date().getTime()/60000) % minute != 0;
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "HomeFragment onStop");
        // BLEスキャンタスク取得
        BleScanTask bleService = getBleScanTask();
        if (bleService == null) {
            Log.w(TAG, "bleService is null");
            return;
        }
        // 設定値の保存
        SharedPreferences.Editor editor = MainActivity.prefs.edit();
        editor.putString("alert_counter",String.valueOf(bleService.getAlertCounter()));
        editor.putString("caution_counter",String.valueOf(bleService.getCautionCounter()));
        editor.putString("distant_counter",String.valueOf(bleService.getDistantCounter()));
        editor.putString("Saved_time",getSystemTime());
        editor.apply();
   }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, "HomeFragment onResume");
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.d(TAG, "HomeFragment onPause");
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "HomeFragment onDestroy");
    }

    private BleScanTask getBleScanTask(){
        MainService service = MainActivity.getService();
        if (service == null){
            return null;
        }
        return service.getBleScanTask();
    }

    private void updateRiskLevel(CounterDevice maxValueInfo){

        // 期間中最大値の取得
        int alertCounter = 0;
        int cautionCounter = 0;
        int distantCounter = 0;
        if (maxValueInfo != null){
            alertCounter = maxValueInfo.mAlertCounter;
            cautionCounter = maxValueInfo.mCautionCounter;
            distantCounter = maxValueInfo.mDistantCounter;
        }

        // リスク値を計算
        int totalCounter = (int)(
                (alertCounter * mAlertCounterCoefficient) +
                (cautionCounter * mCautionCounterCoefficient) +
                (distantCounter * mDistantCounterCoefficient)
        );

        if (this.mPrevRecordTime == null){
            mLevelValue = 0;
            mLevel0_Image.setVisibility(View.VISIBLE);
            mLevel1_Image.setVisibility(View.INVISIBLE);
            mLevel2_Image.setVisibility(View.INVISIBLE);
            mLevel3_Image.setVisibility(View.INVISIBLE);
            mLevel4_Image.setVisibility(View.INVISIBLE);
            mLevel5_Image.setVisibility(View.INVISIBLE);
            mLevelDescription.setText(R.string.level_0_description);
        } else
        if((0 <= totalCounter)&&(totalCounter < mLevel1JudgeLine)) {
            mLevelValue = 1;
            mLevel0_Image.setVisibility(View.INVISIBLE);
            mLevel1_Image.setVisibility(View.VISIBLE);
            mLevel2_Image.setVisibility(View.INVISIBLE);
            mLevel3_Image.setVisibility(View.INVISIBLE);
            mLevel4_Image.setVisibility(View.INVISIBLE);
            mLevel5_Image.setVisibility(View.INVISIBLE);
            mLevelDescription.setText(R.string.level_1_description);
        }
        else if((mLevel1JudgeLine <= totalCounter)&&(totalCounter < mLevel2JudgeLine)){
            mLevelValue = 2;
            mLevel0_Image.setVisibility(View.INVISIBLE);
            mLevel1_Image.setVisibility(View.INVISIBLE);
            mLevel2_Image.setVisibility(View.VISIBLE);
            mLevel3_Image.setVisibility(View.INVISIBLE);
            mLevel4_Image.setVisibility(View.INVISIBLE);
            mLevel5_Image.setVisibility(View.INVISIBLE);
            mLevelDescription.setText(R.string.level_2_description);
        }
        else if((mLevel2JudgeLine <= totalCounter)&&(totalCounter < mLevel3JudgeLine)){
            mLevelValue = 3;
            mLevel0_Image.setVisibility(View.INVISIBLE);
            mLevel1_Image.setVisibility(View.INVISIBLE);
            mLevel2_Image.setVisibility(View.INVISIBLE);
            mLevel3_Image.setVisibility(View.VISIBLE);
            mLevel4_Image.setVisibility(View.INVISIBLE);
            mLevel5_Image.setVisibility(View.INVISIBLE);
            mLevelDescription.setText(R.string.level_3_description);
        }
        else if((mLevel3JudgeLine <= totalCounter)&&(totalCounter < mLevel4JudgeLine)){
            mLevelValue = 4;
            mLevel0_Image.setVisibility(View.INVISIBLE);
            mLevel1_Image.setVisibility(View.INVISIBLE);
            mLevel2_Image.setVisibility(View.INVISIBLE);
            mLevel3_Image.setVisibility(View.INVISIBLE);
            mLevel4_Image.setVisibility(View.VISIBLE);
            mLevel5_Image.setVisibility(View.INVISIBLE);
            mLevelDescription.setText(R.string.level_4_description);
        }
        else if(mLevel5JudgeLine <= totalCounter){
            mLevelValue = 5;
            mLevel0_Image.setVisibility(View.INVISIBLE);
            mLevel1_Image.setVisibility(View.INVISIBLE);
            mLevel2_Image.setVisibility(View.INVISIBLE);
            mLevel3_Image.setVisibility(View.INVISIBLE);
            mLevel4_Image.setVisibility(View.INVISIBLE);
            mLevel5_Image.setVisibility(View.VISIBLE);
            mLevelDescription.setText(R.string.level_5_description);
        }
        // レベル表示のブロックを表示
        mLevelBlock.setVisibility(View.VISIBLE);
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

    private String getSystemTime() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH", Locale.getDefault());
        return sdf.format(date);
    }

    private void showMessage(final String message){
        Activity activity = getActivity();
        if (activity == null){
            return;
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

}

