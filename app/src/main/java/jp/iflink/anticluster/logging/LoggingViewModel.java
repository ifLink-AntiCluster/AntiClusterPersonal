package jp.iflink.anticluster.logging;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LoggingViewModel extends ViewModel {

    // ロギング状態
    public MutableLiveData<Boolean> logging;
    // レベル選択値
    public MutableLiveData<LoggingLevel> level;
    // キーワード入力
    public MutableLiveData<String> keyword;
    // ログ内容
    public MutableLiveData<CharSequence> contents;

    public LoggingViewModel() {
        logging = new MutableLiveData<>();
        logging.setValue(false);
        level = new MutableLiveData<>();
        level.setValue(LoggingLevel.Info);
        keyword = new MutableLiveData<>();
        contents = new MutableLiveData<>();
    }
}