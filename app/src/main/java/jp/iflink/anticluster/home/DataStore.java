package jp.iflink.anticluster.home;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class DataStore {
    private static final String TAG = "DataStore";

    private CounterDevice[] counterInfo_4Hours;
    private CounterDevice[] counterInfo_1Day;
    private CounterDevice[] counterInfo_2Week;

    private FileHandler fileHandler;
    private static final String fileName = "data.txt";
    private static final String TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    public DataStore(Context context){
        this.fileHandler = new FileHandler(context);
    }

    public void init() {
        // データの初期化
        initData();
    }

    public File writeRecord(CounterDevice data, Date recordTime) throws IOException {
        List<CounterDeviceRecord> recordList;
        // データファイルの存在チェック
        File file = fileHandler.getFile(fileName);
        if (file.exists() && file.canRead()){
            // 既にデータファイルが存在する場合はすべて読み込み
            recordList = load();
        } else {
            recordList = new ArrayList<>();
        }
        // レコードを作成
        CounterDeviceRecord record = new CounterDeviceRecord(data, recordTime);
        //  リストに追加
        recordList.add(record);
        // リストから範囲外（現在～２週間前でない）のレコードを削除
        removeOutdatedRecord(recordList, recordTime);
        // リストを保存
        return save(recordList);
    }

    public List<CounterDeviceRecord> load() throws IOException {
        SimpleDateFormat dtFmt = new SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US);
        List<CounterDeviceRecord> recordList = new ArrayList<>();
        // データファイルを取得
        File file = fileHandler.getFile(fileName);
        if (file.exists() && file.canRead()){
            try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
                // ヘッダ行を読み飛ばす
                String line = reader.readLine();
                //  データ行をすべて読み込む
                while((line = reader.readLine()) != null){
                    String[] columns = line.split(",");
                    if (columns.length != 4){
                        continue;
                    }
                    try {
                        // レコードを作成
                        CounterDeviceRecord record = new CounterDeviceRecord();
                        record.recordTime  = dtFmt.parse(columns[0]);
                        record.alertCounter = Integer.parseInt(columns[1]);
                        record.cautionCounter = Integer.parseInt(columns[2]);
                        record.distantCounter = Integer.parseInt(columns[3]);
                        //  リストに追加
                        recordList.add(record);
                    } catch (ParseException e){
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return recordList;
    }

    protected File save(List<CounterDeviceRecord> recordList) throws IOException {
        SimpleDateFormat dtFmt = new SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US);
        // データファイルを取得
        File file = fileHandler.getFile(fileName);
        try (
                BufferedWriter writer = new BufferedWriter(new FileWriter(file))
        ) {
            // ヘッダ行を出力
            writer.write("recordTime,alertCounter,cautionCounter,distantCounter");
            writer.newLine();
            // データ行をすべて出力
            for (CounterDeviceRecord record : recordList){
                writer.write(dtFmt.format(record.recordTime) + ",");
                writer.write(record.alertCounter+ ",");
                writer.write(record.cautionCounter+ ",");
                writer.write(record.distantCounter + "");
                writer.newLine();
            }
        }
        return file;
    }

    public Date update(List<CounterDeviceRecord> recordList, Date baseDate){
        // 前回集計時刻
        Date prevRecordTime = null;
        // 10分単位のグラフデータマップ
        Map<Date, CounterDeviceRecord> graphDataMap = new TreeMap<>();
        if (!recordList.isEmpty()){
            // グラフデータの読み込み
            Date toDate = getTenMinutesDate(baseDate, false);
            Date baseHourDate = getHourDate(toDate);
            Date fromDate = getFromDateByDays(baseHourDate, -13);
            // 基準時刻から2週間前までのデータを取得する
            for (CounterDeviceRecord record : recordList) {
                if (!fromDate.after(record.recordTime) && !toDate.before(record.recordTime)) {
                    // 10分単位の時刻を取得
                    Date recordTime10m = getTenMinutesDate(record.recordTime, true);
                    // 10分単位でデータをサマリする
                    CounterDeviceRecord summary = graphDataMap.get(recordTime10m);
                    if (summary == null){
                        summary = new CounterDeviceRecord();
                        summary.cautionCounter = record.cautionCounter;
                        summary.alertCounter = record.alertCounter;
                        summary.distantCounter = record.distantCounter;
                        summary.recordTime = recordTime10m;
                        graphDataMap.put(recordTime10m, summary);
                    } else {
                        summary.cautionCounter += record.cautionCounter;
                        summary.alertCounter += record.alertCounter;
                        summary.distantCounter += record.distantCounter;
                    }
                    // 最も近い記録時刻を前回集計時刻とする
                    if (prevRecordTime == null || record.recordTime.after(prevRecordTime)){
                        prevRecordTime = record.recordTime;
                    }
                }
            }
        }
        // 10分単位のグラフデータリストに変換
        Collection<CounterDeviceRecord> graphDataList = graphDataMap.values();
        // 4Hoursグラフデータの初期化
        counterInfo_4Hours = new CounterDevice[24];
        {
            // 4Hoursグラフデータの読み込み
            Date toDate = getTenMinutesDate(baseDate, true);
            Date baseTime = toDate;
            Date fromDate = getFromDateByHours(baseTime, -3);
            // 基準時刻から4時間前までのデータを取得する
            for (CounterDeviceRecord data : graphDataList) {
                if (!fromDate.after(data.recordTime) && !toDate.before(data.recordTime)){
                    //  グラフ時刻と基準時刻との時分差を求める
                    int diffMinutes = getDiffMinutes(baseTime, data.recordTime);
                    // インデックスを求める
                    int idx = 23 - (diffMinutes/10);
                    // 対象の分[MINUTE]に加算する
                    if (counterInfo_4Hours[idx]  == null){
                        counterInfo_4Hours[idx] = new CounterDevice();
                    }
                    counterInfo_4Hours[idx].mAlertCounter += data.alertCounter;
                    counterInfo_4Hours[idx].mCautionCounter += data.cautionCounter;
                    counterInfo_4Hours[idx].mDistantCounter += data.distantCounter;
                }
            }
            // 無実行時間のデータを-1に設定する
            for (int idx=0; idx<counterInfo_4Hours.length; idx++){
                if (counterInfo_4Hours[idx] == null ){
                    counterInfo_4Hours[idx] = new CounterDevice();
                    counterInfo_4Hours[idx].mAlertCounter = -1;
                    counterInfo_4Hours[idx].mCautionCounter = -1;
                    counterInfo_4Hours[idx].mDistantCounter = -1;
                }
            }
        }
        // 1Dayグラフデータの初期化
        counterInfo_1Day = new CounterDevice[24];
        {
            // 1時間単位で最大値を持つデータリストを作成
            Map<Date, CounterDeviceRecord> maxDataMap = new TreeMap<>();
            for (CounterDeviceRecord data : graphDataList){
                // 1時間単位の時刻を取得する
                Date recordTimeHour = getHourDate(data.recordTime);
                // 1時間単位で最大値のデータを設定する
                CounterDeviceRecord max= maxDataMap.get(recordTimeHour);
                if (max == null){
                    max = new CounterDeviceRecord();
                    max.alertCounter = data.alertCounter;
                    max.cautionCounter = data.cautionCounter;
                    max.distantCounter = data.distantCounter;
                    max.recordTime = recordTimeHour;
                    maxDataMap.put(recordTimeHour, max);
                } else {
                    // alert数について、対象時[HOUR]の中で最大値を設定する
                    if (max.alertCounter < data.alertCounter) {
                        max.alertCounter = data.alertCounter;
                    }
                    // caution数について、対象時[HOUR]の中で最大値を設定する
                    if (max.cautionCounter < data.cautionCounter) {
                        max.cautionCounter = data.cautionCounter;
                    }
                    // distant数について、対象時[HOUR]の中で最大値を設定する
                    if (max.distantCounter < data.distantCounter) {
                        max.distantCounter = data.distantCounter;
                    }
                }
            }
            Collection<CounterDeviceRecord> dataList = maxDataMap.values();
            // 1Dayグラフデータの読み込み
            Date toDate = getTenMinutesDate(baseDate, true);
            Date fromDate = getDayOnly(toDate);
            // 基準時刻を含む当日分のデータのみ取得する
            for (CounterDeviceRecord record : dataList) {
                if (!fromDate.after(record.recordTime) && !toDate.before(record.recordTime)){
                    // 時間[HOUR]を取得する
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(record.recordTime);
                    int hour = cal.get(Calendar.HOUR_OF_DAY);
                    // 対象の時間[HOUR]に加算する
                    if (counterInfo_1Day[hour]  == null){
                        counterInfo_1Day[hour] = new CounterDevice();
                    }
                    counterInfo_1Day[hour].mAlertCounter += record.alertCounter;
                    counterInfo_1Day[hour].mCautionCounter += record.cautionCounter;
                    counterInfo_1Day[hour].mDistantCounter += record.distantCounter;
                }
            }
            for (int idx=0; idx<counterInfo_1Day.length; idx++){
                if (counterInfo_1Day[idx] == null ){
                    // 基準日時の時間[HOUR]を取得する
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(baseDate);
                    int hour = cal.get(Calendar.HOUR_OF_DAY);
                    if (idx < hour) {
                        // 過去の無実行時間のデータは-1に設定する
                        counterInfo_1Day[idx] = new CounterDevice();
                        counterInfo_1Day[idx].mAlertCounter = -1;
                        counterInfo_1Day[idx].mCautionCounter = -1;
                        counterInfo_1Day[idx].mDistantCounter = -1;
                    } else {
                        // 未来の無実行時間のデータは0に設定する
                        counterInfo_1Day[idx] = new CounterDevice();
                    }
                }
            }
        }
        // 2Weekグラフデータの初期化
        counterInfo_2Week = new CounterDevice[14];
        {
            // 1日単位で最大値を持つデータリストを作成
            Map<Date, CounterDeviceRecord> maxDataMap = new TreeMap<>();
            for (CounterDeviceRecord data : graphDataList){
                Date recordDay = getDayOnly(data.recordTime);
                // 1日単位で最大値のデータを設定する
                CounterDeviceRecord max= maxDataMap.get(recordDay);
                if (max == null){
                    max = new CounterDeviceRecord();
                    max.alertCounter = data.alertCounter;
                    max.cautionCounter = data.cautionCounter;
                    max.distantCounter = data.distantCounter;
                    max.recordTime = recordDay;
                    maxDataMap.put(recordDay, max);
                } else {
                    // alert数について、対象日の中で最大値を設定する
                    if (max.alertCounter < data.alertCounter) {
                        max.alertCounter = data.alertCounter;
                    }
                    // caution数について、対象日の中で最大値を設定する
                    if (max.cautionCounter < data.cautionCounter) {
                        max.cautionCounter = data.cautionCounter;
                    }
                    // distant数について、対象日の中で最大値を設定する
                    if (max.distantCounter < data.distantCounter) {
                        max.distantCounter = data.distantCounter;
                    }
                }
            }
            Collection<CounterDeviceRecord> dataList = maxDataMap.values();
            // 2Weekグラフデータの読み込み
            Date toDate = getTenMinutesDate(baseDate, true);
            Date fromDate = getFromDateByDays(toDate, -13);
            Date baseDay = getDayOnly(baseDate);
            // 基準時刻から2週間前までのデータを取得する
            for (CounterDeviceRecord data : dataList) {
                if (!fromDate.after(data.recordTime) && !toDate.before(data.recordTime)){
                    //  グラフ時刻と基準時刻との日付差を求める
                    int diffDays = getDiffDays(baseDay, getDayOnly(data.recordTime));
                    // インデックスを求める
                    int idx = 13 - diffDays;
                    // 対象の週日毎の最大値の設定
                    if (counterInfo_2Week[idx]  == null){
                        // 1件目はそのまま設定
                        counterInfo_2Week[idx] = new CounterDevice();
                        counterInfo_2Week[idx].mAlertCounter = data.alertCounter;
                        counterInfo_2Week[idx].mCautionCounter = data.cautionCounter;
                        counterInfo_2Week[idx].mDistantCounter = data.distantCounter;
                    } else {
                        // alert数について、対象の週日の中で最大値を設定する
                        if (counterInfo_2Week[idx].mAlertCounter < data.alertCounter) {
                            counterInfo_2Week[idx].mAlertCounter = data.alertCounter;
                        }
                        // caution数について、対象の週日の中で最大値を設定する
                        if (counterInfo_2Week[idx].mCautionCounter < data.cautionCounter) {
                            counterInfo_2Week[idx].mCautionCounter = data.cautionCounter;
                        }
                        // distant数について、対象の週日の中で最大値を設定する
                        if (counterInfo_2Week[idx].mDistantCounter < data.distantCounter) {
                            counterInfo_2Week[idx].mDistantCounter = data.distantCounter;
                        }
                    }
                }
            }
            // 無実行時間のデータを-1に設定する
            for (int idx=0; idx<counterInfo_2Week.length; idx++){
                if (counterInfo_2Week[idx] == null ){
                    counterInfo_2Week[idx] = new CounterDevice();
                    counterInfo_2Week[idx].mAlertCounter = -1;
                    counterInfo_2Week[idx].mCautionCounter = -1;
                    counterInfo_2Week[idx].mDistantCounter = -1;
                }
            }
        }
        return prevRecordTime;
    }

    private static Date getDayOnly(Date targetDate){
        // 判定開始時刻を設定
        Calendar cal = Calendar.getInstance();
        cal.setTime(targetDate);
        // 時刻部分をクリア
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static Date getHalfDayOnly(Date targetDate){
        Calendar cal = Calendar.getInstance();
        cal.setTime(targetDate);
        if (cal.get(Calendar.HOUR_OF_DAY) < 12){
            cal.set(Calendar.HOUR_OF_DAY, 0);
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 12);
        }
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Date getTenMinutesDate(Date baseDate, boolean toGraphDate){
        Calendar cal = Calendar.getInstance();
        cal.setTime(baseDate);
        // 10分単位の時刻に調整する
        int tenMinutes = (cal.get(Calendar.MINUTE)/10)*10;
        cal.set(Calendar.MINUTE, tenMinutes);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (toGraphDate){
            // 記録時刻はグラフ時刻から10分後なので、10分引いて調整する
            cal.add(Calendar.MINUTE, -10);
        }
        return cal.getTime();
    }

    private static Date getHourDate(Date baseDate){
        Calendar cal = Calendar.getInstance();
        cal.setTime(baseDate);
        // 分[MINUTE]以下をクリア
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static Date getFromDateByHours(Date toDate, int hours){
        Calendar cal = Calendar.getInstance();
        cal.setTime(toDate);
        // 分[MINUTE]以下をクリア
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // 指定した分[MINUTE]数を加算
        cal.add(Calendar.HOUR_OF_DAY, hours);
        return cal.getTime();
    }

    private static Date getDayEndDate(Date baseDate){
        Calendar cal = Calendar.getInstance();
        cal.setTime(baseDate);
        // 時刻を23:59:59.999 に設定
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    private static Date getFromDateByDays(Date toDate, int days){
        Calendar cal = Calendar.getInstance();
        cal.setTime(toDate);
        // 時間[HOUR]以下をクリア
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        // 指定した日数を加算
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }

    private static int getDiffDays(Date baseDay, Date targetDay) {
        // ミリ秒単位での差分算出
        long diffTime = baseDay.getTime() - targetDay.getTime();
        // 日単位の差分を求める
        return (int)(diffTime / (1000 * 60 * 60 * 24));
    }

    private static int getDiffMinutes(Date baseTime, Date targetTime){
        // ミリ秒単位での差分算出
        long diffTime = baseTime.getTime() - targetTime.getTime();
        // 分単位の差分を求める
        return (int)(diffTime / (1000 * 60));
    }

    protected void removeOutdatedRecord(List<CounterDeviceRecord> recordList, Date baseDate) {
        Date toDate = getDayEndDate(baseDate);
        Date fromDate = getFromDateByDays(toDate, -13);
        // リストから範囲外（現在～２週間前でない）のレコードを削除
        Iterator<CounterDeviceRecord> recordIt = recordList.iterator();
        while (recordIt.hasNext()) {
            CounterDeviceRecord record = recordIt.next();
            if (fromDate.after(record.recordTime) || toDate.before(record.recordTime)) {
                recordIt.remove();
            }
        }
    }

    private void initData(){
        // 1Dayグラフデータの初期化
        counterInfo_1Day = new CounterDevice[24];
        for(int i = 0;i < counterInfo_1Day.length; i++){
            counterInfo_1Day[i] = new CounterDevice();
        }
        // 4Hoursグラフデータの初期化
        counterInfo_4Hours = new CounterDevice[14];
        for(int i = 0;i < counterInfo_4Hours.length; i++){
            counterInfo_4Hours[i] = new CounterDevice();
        }
        // 2Weekグラフデータの初期化
        counterInfo_2Week = new CounterDevice[14];
        for(int i = 0;i < counterInfo_2Week.length; i++){
            counterInfo_2Week[i] = new CounterDevice();
        }
    }

    public CounterDevice[] getCounterInfo_4Hours() {
        return counterInfo_4Hours;
    }

    public CounterDevice[] getCounterInfo_1Day() {
        return counterInfo_1Day;
    }

    public CounterDevice[] getCounterInfo_2Week() {
        return counterInfo_2Week;
    }
}
