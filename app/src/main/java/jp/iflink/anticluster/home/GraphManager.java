package jp.iflink.anticluster.home;

import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import jp.iflink.anticluster.R;
import jp.iflink.anticluster.main.MainActivity;

public class GraphManager {
    private static final String TAG = "GraphManager";
    private static final int DEFAULT_Y_VALUE = 100;

    private BarChart mBarChartGraph;
    private DataStore dataStore;
    private Date baseDate;

    public enum GraphKind {G4Hours, G1Day, G2Week}
    private GraphKind currentGraph;

//    public GraphManager() {}
   public GraphManager(BarChart mBarChartGraph) {
       this.mBarChartGraph = mBarChartGraph;
   }

    public void init(DataStore dataStore){
        this.dataStore = dataStore;
        // Y軸(左)
        YAxis left = mBarChartGraph.getAxisLeft();
        left.setAxisMinimum(0);
        left.setAxisMaximum(DEFAULT_Y_VALUE);
        left.setLabelCount(5);
        left.setDrawTopYLabelEntry(true);
        //整数表示に
        left.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return "" + (int)value;
            }
        });

        // Y軸(右)
        YAxis right = mBarChartGraph.getAxisRight();
        right.setDrawLabels(false);
        right.setDrawGridLines(false);
        right.setDrawZeroLine(true);
        right.setDrawTopYLabelEntry(true);

        // グラフ上の表示
        mBarChartGraph.setDrawValueAboveBar(true);
        mBarChartGraph.setDrawBarShadow(false);
        mBarChartGraph.getDescription().setEnabled(false);
        mBarChartGraph.setClickable(false);

        // 凡例
        mBarChartGraph.getLegend().setEnabled(false);
        // スケール無効
        mBarChartGraph.setScaleEnabled(false);
    }

    public void update(String[] labels, CounterDevice[] counter_info, int labelCount){
        // X軸設定
        setupXAxis(labels, labelCount);
        // Y軸設定
        setupYAxis(counter_info);
        // データ更新
        BarData data = new BarData(getBarData(counter_info));
        mBarChartGraph.setData(data);
        mBarChartGraph.notifyDataSetChanged();
        mBarChartGraph.invalidate();
    }

    public CounterDevice getMaxValueInfo(CounterDevice[] counter_info){
        CounterDevice maxValueInfo = new CounterDevice();
        for (CounterDevice info: counter_info){
            if (info.getTotal()==0){
                // -1データを除外
                continue;
            }
            // alert数について、対象期間で最大値を設定する
            if (maxValueInfo.mAlertCounter < info.mAlertCounter){
                maxValueInfo.mAlertCounter = info.mAlertCounter;
            }
            // caution数について、対象期間で最大値を設定する
            if (maxValueInfo.mCautionCounter < info.mCautionCounter){
                maxValueInfo.mCautionCounter = info.mCautionCounter;
            }
            // distant数について、対象期間で最大値を設定する
            if (maxValueInfo.mDistantCounter < info.mDistantCounter){
                maxValueInfo.mDistantCounter = info.mDistantCounter;
            }
        }
       return maxValueInfo;
    }

    public Date loadData(Date baseDate){
        Date prevRecordTime = null;
        try {
            // データの読み込み
            List<CounterDeviceRecord> recordList = dataStore.load();
            // データの反映
            prevRecordTime = dataStore.update(recordList, baseDate);
            this.baseDate = baseDate;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            showMessage(R.string.data_load_fail);
        }
        return prevRecordTime;
    }

    public CounterDevice updateGraph() {
        if (currentGraph == null){
            return null;
        }
        switch(currentGraph){
            case G4Hours:
                return updateGraph4Hours();
            case G1Day:
                return updateGraph1Day();
            case G2Week:
                return updateGraph2Week();
        }
        return null;
    }

    public CounterDevice updateGraph4Hours(){
        currentGraph = GraphKind.G4Hours;
        // ４時間分のデータをログから取得
        CounterDevice[] counter_info = dataStore.getCounterInfo_4Hours();
        // X軸設定
        String[] labels = new String[25];
        labels[0] = "";
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(DataStore.getTenMinutesDate(this.baseDate, true));
        calendar.add(Calendar.HOUR_OF_DAY, -4);
        for (int idx=1; idx<labels.length; idx++) {
            calendar.add(Calendar.MINUTE, +10);
            if (calendar.get(Calendar.MINUTE) == 0){
                // 00分にのみラベルを設定
                labels[idx] = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
            } else {
                labels[idx] = "";
            }
        }
        // グラフデータ更新
        update(labels, counter_info, 24);
        // 期間中最大値情報の取得、返却
        return getMaxValueInfo(counter_info);
    }

    public CounterDevice updateGraph1Day(){
        currentGraph = GraphKind.G1Day;
        // 1日分のデータをログから取得
        CounterDevice[] counter_info = dataStore.getCounterInfo_1Day();
        // X軸設定
        String[] labels = new String[25];
        labels[0] = "";
        for (int idx=1; idx<labels.length; idx++){
            labels[idx] = String.valueOf(idx-1);
        }
        // グラフデータ更新
        update(labels, counter_info, 12);
        // 期間中最大値情報の取得、返却
        return getMaxValueInfo(counter_info);
    }

    public CounterDevice updateGraph2Week(){
        currentGraph = GraphKind.G2Week;
        // 2週間分のデータをログから取得
        CounterDevice[] counter_info = dataStore.getCounterInfo_2Week();
        // X軸設定
        String[] labels = new String[15];
        labels[0] = "";
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.baseDate);
        calendar.add(Calendar.DATE, -7*2);
        for (int idx=1; idx<labels.length; idx++) {
            calendar.add(Calendar.DATE, +1);
            labels[idx] = String.valueOf(calendar.get(Calendar.DATE));
        }
        // グラフデータ更新
        update(labels, counter_info, 14);
        // 期間中最大値情報の取得、返却
        return getMaxValueInfo(counter_info);
    }

    public GraphKind getCurrentGraph(){
       return currentGraph;
    }

    private void setupXAxis(String[] labels, int labelCount){
        //X軸
        XAxis xAxis = mBarChartGraph.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawLabels(true);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setLabelCount(labelCount);
    }

    private void setupYAxis(CounterDevice[] counter_info){
        // Y軸の最大値を取得
        int maxCounter = -1;
        for (CounterDevice info : counter_info){
            int counter = info.getTotal();
            if (counter>maxCounter){
                maxCounter = counter;
            }
        }
        if (maxCounter > DEFAULT_Y_VALUE){
            // Y軸(左)の最大値をリセット
            YAxis left = mBarChartGraph.getAxisLeft();
            left.resetAxisMaximum();
        } else {
            // Y軸(左)の最大値をデフォルトに設定
            YAxis left = mBarChartGraph.getAxisLeft();
            left.setAxisMaximum(DEFAULT_Y_VALUE);
        }
    }

    List<IBarDataSet> getBarData(CounterDevice[] counter_info){
        ArrayList<BarEntry> entries = new ArrayList<>();
        for(int idx=0; idx < counter_info.length; idx++){
            CounterDevice info = counter_info[idx];
            if (info.hasData()){
                entries.add(new BarEntry(idx+1, new float[]{info.mDistantCounter, info.mCautionCounter, info.mAlertCounter, 0}));
            } else {
                entries.add(new BarEntry(idx+1, new float[]{0, 0, 0, 1}));
            }
        }

        List<IBarDataSet> bars = new ArrayList<>();
        BarDataSet dataSet = new BarDataSet(entries, "bar");

        //整数で表示
        dataSet.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return "" + (int) value;
            }
        });
        //ハイライトさせない
        dataSet.setHighlightEnabled(false);
        dataSet.setDrawValues(false);

        //Barの色をセット
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setColors(new int[]{R.color.colorDistant, R.color.colorCaution, R.color.colorAlert, R.color.colorNoData}, mBarChartGraph.getContext());
        bars.add(dataSet);

        dataSet.notifyDataSetChanged();

        return bars;
    }

    private void showMessage(final int message){
        mBarChartGraph.post(new Runnable() {
            public void run() {
                Toast.makeText(mBarChartGraph.getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
