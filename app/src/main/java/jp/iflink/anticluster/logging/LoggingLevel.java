package jp.iflink.anticluster.logging;

public enum LoggingLevel {
    Verbose("V", 1),
    Debug("D", 2),
    Info("I", 3),
    Warning("W", 4),
    Error("E", 5),
    ;
    public final String cd;
    public final int priority;

    LoggingLevel(String cd, int priority){
        this.cd = cd;
        this.priority = priority;
    }

    public boolean checkThreshold(String cd, boolean defaultValue){
        LoggingLevel level = of(cd);
        if (level != null){
            return this.priority <= level.priority;
        }
        return defaultValue;
    }

    public LoggingLevel of (String cd){
        for (LoggingLevel level : LoggingLevel.values()){
            if (level.cd.equals(cd)){
                return level;
            }
        }
        return null;
    }
}
