package jp.iflink.anticluster.home;

import java.util.Date;

public class CounterDeviceRecord {

    public CounterDeviceRecord(){}

    public CounterDeviceRecord(CounterDevice data, Date recordTime){
        this.recordTime = recordTime;
        this.alertCounter = data.mAlertCounter;
        this.cautionCounter = data.mCautionCounter;
        this.distantCounter = data.mDistantCounter;
    }

    public int getTotal(){
        return Math.max(alertCounter, 0) +
                Math.max(cautionCounter, 0)  +
                Math.max(distantCounter, 0) ;
    }

    // 記録時刻
    Date recordTime;
    //  濃厚接触数
    int alertCounter;
    //  至近距離数
    int cautionCounter;
    //  周囲数
    int distantCounter;
}
