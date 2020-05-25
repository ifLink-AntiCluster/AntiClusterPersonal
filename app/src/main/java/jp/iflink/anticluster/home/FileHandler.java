package jp.iflink.anticluster.home;

import android.content.Context;
import java.io.File;

public class FileHandler {
    private final Context context;
    private final File filesDir;

    public FileHandler(Context applicationContext) {
        context = applicationContext;
        filesDir = applicationContext.getFilesDir();
    }

    // 内部ストレージのディレクトリを取得
    public File getFilesDir() {
        return filesDir;
    }

    // 内部ストレージのファイルを取得
    public File getFile(String fileName) {
        //return new File(filesDir, fileName);
        return new File(getExternalFilesDir(null), fileName);
    }

    // 外部ストレージのディレクトリを取得
    public File getExternalFilesDir(String dirName) {
        return context.getExternalFilesDir( dirName);
    }

}
