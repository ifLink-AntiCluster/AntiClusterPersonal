package jp.iflink.anticluster.model;

public enum CountType {
    /** 濃厚接触(3m以内、15分以上) */
    ALERT(1),
    /** 至近距離(3m以内、15分以下) */
    CAUTION(2),
    /** 周囲(3～10m) */
    DISTANT(3),
    /** 遠距離(10m以上) */
    FAR(4)
    ;
    private int type;

    private CountType(int type){
        this.type = type;
    }

    public int type(){
        return this.type;
    }

    public boolean isNear(){
        return this == ALERT || this == CAUTION;
    }

    public boolean isAround(){
        return this == DISTANT;
    }

    public boolean isFar(){
        return this == FAR;
    }

    public static CountType of(int type){
        for (CountType value: CountType.values()){
            if (value.type() == type){
                return value;
            }
        }
        return null;
    }
}
