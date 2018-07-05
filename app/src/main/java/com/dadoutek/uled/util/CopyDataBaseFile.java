package com.dadoutek.uled.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;

import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.SharedPreferencesHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;


/**
 * @author 复制数据库到本应用当中
 */
@SuppressLint("SdCardPath")
public class CopyDataBaseFile {
    // 保存数据库的路径
    private final static String DATABASE_PATH = "/data/data/com.dadoutek.uled/databases/";
    private final static String dbName = SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
            Constant.DB_NAME_KEY,"uled")+".db";;
    private Context context;

    public CopyDataBaseFile(Context context) {
        this.context = context;
    }

    /**
     * 检查数据库是否存在
     *
     * @return
     */
    public boolean checkDataBase() {
        SQLiteDatabase checkDB = null;
        try {
            String databaseFilename = DATABASE_PATH + dbName;
            checkDB = SQLiteDatabase.openDatabase(databaseFilename, null, SQLiteDatabase.OPEN_READWRITE);
        } catch (SQLiteException e) {
            LogUtils.e("CopyDataBaseFile -> 复制数据库出错------------------>");
        }
        return checkDB != null ? true : false;
    }


    /**
     * 数据库拷贝
     */
    public void backupDataBase() {
        String databaseFilenames = DATABASE_PATH + dbName;
        File dbfile = new File(databaseFilenames);

        File backupFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/" + dbName);
        outputFile(dbfile, backupFile);

    }

    public void backupDataBaseWithTime() {
        String databaseFilenames = DATABASE_PATH + dbName;
        File dbfile = new File(databaseFilenames);
        String time = "";
        Calendar calendar = Calendar.getInstance();
        time = (calendar.get(Calendar.MONTH) + 1) + "" + calendar.get(Calendar.DAY_OF_MONTH) + "" + calendar.get(Calendar.HOUR_OF_DAY) + "" + calendar.get(Calendar.MINUTE);
        File backupFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/uled" + time + ".db");
        outputFile(dbfile, backupFile);

    }

    void outputFile(File dbfile, File backupFile) {
        File dir = backupFile.getParentFile();
        if (dir.exists() == false) {
            dir.mkdirs();
        }

        FileOutputStream os = null;
        InputStream is = null;
        try {
            backupFile.createNewFile();
            os = new FileOutputStream(backupFile);// 得到数据库文件的写入流
            is = new FileInputStream(dbfile);
            byte[] buffer = new byte[8192];
            int count = 0;
            while ((count = is.read(buffer)) > 0) {
                os.write(buffer, 0, count);
                os.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
                if (os != null)
                    os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 数据库拷贝
     */
    public void copyDataBase() {
        String databaseFilenames = DATABASE_PATH + dbName;
        File dbfile = new File(databaseFilenames);
        File dir = dbfile.getParentFile();
        inputFile(dbfile, null);
    }

    public void copyDataBase(File file) {
        String databaseFilenames = DATABASE_PATH + dbName;
        File dbfile = new File(databaseFilenames);
        try {
            FileInputStream inputStream = new FileInputStream(file);
            inputFile(dbfile, inputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    void inputFile(File dbfile, InputStream is) {
        File dir = dbfile.getParentFile();
        if (dir.exists() == false) {
            dir.mkdirs();
        }

        if (dbfile.exists()) {
            dbfile.delete();
        }


        FileOutputStream os = null;
        try {
            os = new FileOutputStream(dbfile);// 得到数据库文件的写入流
            if (is == null) {
//                is = TelinkLightApplication.getInstance().getApplicationContext().getResources().openRawResource(R.raw.hinteen);// 得到数据库文件的数据流
            }

            byte[] buffer = new byte[8192];
            int count = 0;
            while ((count = is.read(buffer)) > 0) {
                os.write(buffer, 0, count);
                os.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
                if (os != null)
                    os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 数据库拷贝
     */
    public void copyDataBaseWithSD() {
        String databaseFilenames = DATABASE_PATH + dbName;
        File dbfile = new File(databaseFilenames);
        File dir = dbfile.getParentFile();
        if (dir.exists() == false) {
            dir.mkdirs();
        }

        if (dbfile.exists()) {
            dbfile.delete();
        }
        File copyFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/copyuled.db");
        if (!copyFile.exists()) {
            return;
        }

        FileOutputStream os = null;
        InputStream is = null;
        try {
            os = new FileOutputStream(dbfile);// 得到数据库文件的写入流
            is = new FileInputStream(copyFile);// 得到数据库文件的数据流
            byte[] buffer = new byte[8192];
            int count = 0;
            while ((count = is.read(buffer)) > 0) {
                os.write(buffer, 0, count);
                os.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
                if (os != null)
                    os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
