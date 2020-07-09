package jp.iflink.anticluster.setting;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import jp.iflink.anticluster.R;
import jp.iflink.anticluster.main.MainActivity;

public class SettingFragment extends Fragment {

    private EditText mSetting_2m;
    private EditText mSettingAlertTimer;
    private EditText mSetting_around;
    private EditText mAlertCount;
    private EditText mCautionCount;
    private EditText mJudgeLine1;
    private EditText mJudgeLine2;
    private EditText mJudgeLine3;
    private EditText mJudgeLine4;
    private EditText mJudgeLine5;

    private Button mSaveButton;
    private RadioGroup mScanIntervalGroup;
    private int mScanSetting;

    private int mCheckBoxValue;

    private CheckBox mLoggingCheck;
    private int mLoggingCheckValue;

    private EditText mUpdateSetting;
    private EditText mUpdatePerMinutes;
    private RadioGroup mUpdateMethodGroup;
    private int mUpdateMethod;

    // カウント更新方法
    public static final class UpdateMethod {
        //  逐次
        public static final int SEQUENTIAL = 1;
        //  毎回指定分毎
        public static final int EVERY_TIME = 2;
    };

    private EditText mSendUUID;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SettingViewModel viewModel = ViewModelProviders.of(this).get(SettingViewModel.class);
        View root = inflater.inflate(R.layout.fragment_setting, container, false);

        mSetting_2m = root.findViewById(R.id.txt_rssi_2m);
        mSettingAlertTimer = root.findViewById(R.id.et_alert_timer);
        mSetting_around = root.findViewById(R.id.et_rssi_around);
        mSaveButton = root.findViewById(R.id.button_save);
        mAlertCount =  root.findViewById(R.id.et_alert_count);
        mCautionCount =  root.findViewById(R.id.et_caution_count);
        mJudgeLine1 =  root.findViewById(R.id.et_judge_line1);
        mJudgeLine2 =  root.findViewById(R.id.et_judge_line2);
        mJudgeLine3 =  root.findViewById(R.id.et_judge_line3);
        mJudgeLine4 =  root.findViewById(R.id.et_judge_line4);
        mJudgeLine5 =  root.findViewById(R.id.et_judge_line5);
        mUpdateSetting = root.findViewById(R.id.et_update_time);
        mUpdatePerMinutes = root.findViewById(R.id.et_update_per_minuts);
        mSendUUID =  root.findViewById(R.id.et_send_uuid);

        mScanIntervalGroup = root.findViewById(R.id.rg_scan_interval);
        mLoggingCheck = root.findViewById(R.id.logging_setting);

        final Resources res = getResources();
        int logging_setting = Integer.parseInt(MainActivity.prefs.getString("logging_setting", res.getString(R.string.default_logging_setting)));
        if(logging_setting==0){
            mLoggingCheck.setChecked(false);
            mLoggingCheckValue = 0;
        }
        else {
            mLoggingCheck.setChecked(true);
            mLoggingCheckValue = 1;
        }
        mLoggingCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean check_log = mLoggingCheck.isChecked();
                if(check_log){
                    mLoggingCheckValue = 1;
                }
                else{
                    mLoggingCheckValue = 0;
                }
            }
        });

        int scanSetting = Integer.parseInt(MainActivity.prefs.getString("scan_setting", String.valueOf(0))) ;
        switch (scanSetting){
            case 1:
                mScanIntervalGroup.check(R.id.rb_scan_interval2);
                break;
            case 2:
                mScanIntervalGroup.check(R.id.rb_scan_interval3);
                break;
            default:
                mScanIntervalGroup.check(R.id.rb_scan_interval1);
                break;
        }

        mScanIntervalGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = group.findViewById(checkedId);
                switch (checkedId){
                    case R.id.rb_scan_interval1:
                        mScanSetting = 0;
                        break;
                    case R.id.rb_scan_interval2:
                        mScanSetting = 1;
                        break;
                    case R.id.rb_scan_interval3:
                        mScanSetting = 2;
                }
            }
        });

        mUpdateMethodGroup = root.findViewById(R.id.rg_update_method);
        mUpdateMethodGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = group.findViewById(checkedId);
                switch (checkedId){
                    case R.id.rb_update_method1:
                        mUpdateMethod = UpdateMethod.SEQUENTIAL;
                        mUpdatePerMinutes.setEnabled(false);
                        break;
                    case R.id.rb_update_method2:
                        mUpdateMethod = UpdateMethod.EVERY_TIME;
                        mUpdatePerMinutes.setEnabled(true);
                        break;
                }
            }
        });

        int updateMethod = MainActivity.prefs.getInt("update_method", UpdateMethod.EVERY_TIME);
        switch (updateMethod){
            case UpdateMethod.EVERY_TIME:
                mUpdateMethodGroup.check(R.id.rb_update_method2);
                break;
            case UpdateMethod.SEQUENTIAL:
            default:
                mUpdateMethodGroup.check(R.id.rb_update_method1);
                break;
        }

        // 保存値読み込み
        mSetting_2m.setText(MainActivity.prefs.getString("rssi_2m", res.getString(R.string.default_rssi)));
        mSettingAlertTimer.setText(MainActivity.prefs.getString("alert_timer", res.getString(R.string.default_alert_timer)));
        mSetting_around.setText(MainActivity.prefs.getString("rssi_around", res.getString(R.string.default_around_rssi)));
        mAlertCount.setText(MainActivity.prefs.getString("alert_count_coefficient", res.getString(R.string.default_alert_counter_coefficient)));
        mCautionCount.setText(MainActivity.prefs.getString("caution_count_coefficient", res.getString(R.string.default_caution_counter_coefficient)));
        mJudgeLine1.setText(MainActivity.prefs.getString("judge_line1", res.getString(R.string.default_level_judge_1)));
        mJudgeLine2.setText(MainActivity.prefs.getString("judge_line2", res.getString(R.string.default_level_judge_2)));
        mJudgeLine3.setText(MainActivity.prefs.getString("judge_line3", res.getString(R.string.default_level_judge_3)));
        mJudgeLine4.setText(MainActivity.prefs.getString("judge_line4", res.getString(R.string.default_level_judge_4)));
        mJudgeLine5.setText(MainActivity.prefs.getString("judge_line5", res.getString(R.string.default_level_judge_5)));
        mUpdateSetting.setText(MainActivity.prefs.getString("update_time", res.getString(R.string.default_update_time)));
        mUpdatePerMinutes.setText(""+MainActivity.prefs.getInt("update_per_minutes", res.getInteger(R.integer.default_update_per_minutes)));
        mSendUUID.setText(MainActivity.prefs.getString("send_uuid", res.getString(R.string.send_data_UUID)));

        // 設定値保存
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = MainActivity.prefs.edit();
                editor.putString("rssi_2m",mSetting_2m.getText().toString());
                editor.putString("alert_timer",mSettingAlertTimer.getText().toString());
                editor.putString("rssi_around",mSetting_around.getText().toString());
                editor.putString("scan_setting",String.valueOf(mScanSetting));
                editor.putString("alert_count_coefficient",mAlertCount.getText().toString());
                editor.putString("caution_count_coefficient",mCautionCount.getText().toString());
                editor.putString("judge_line1",mJudgeLine1.getText().toString());
                editor.putString("judge_line2",mJudgeLine2.getText().toString());
                editor.putString("judge_line3",mJudgeLine3.getText().toString());
                editor.putString("judge_line4",mJudgeLine4.getText().toString());
                editor.putString("judge_line5",mJudgeLine5.getText().toString());
                editor.putString("device_name_check",String.valueOf(mCheckBoxValue));
                editor.putString("logging_setting",String.valueOf(mLoggingCheckValue));
                editor.putString("update_time",mUpdateSetting.getText().toString());
                editor.putString("send_uuid",mSendUUID.getText().toString());
                editor.putInt("update_method",mUpdateMethod);
                editor.putInt("update_per_minutes",Integer.valueOf(mUpdatePerMinutes.getText().toString()));
                editor.apply();
                showMessage(res.getString(R.string.finish_save));
            }
        });

        return root;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
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
