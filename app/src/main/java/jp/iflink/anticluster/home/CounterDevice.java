package jp.iflink.anticluster.home;

public class CounterDevice {
    int mAlertCounter;
    int mCautionCounter;
    int mDistantCounter;

    public int getTotal(){
        return Math.max(mAlertCounter, 0) +
                Math.max(mCautionCounter, 0)  +
                Math.max(mDistantCounter, 0) ;
    }

    public boolean hasData(){
        return (mCautionCounter >= 0) && (mAlertCounter >= 0) && (mDistantCounter >= 0);
    }
}
