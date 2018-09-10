package com.dadoutek.uled.intf;

public interface OtaPrepareListner {
    void startGetVersion();
    void getVersionSuccess(String s);
    void getVersionFail();
    void downLoadFileSuccess();
    void downLoadFileFail(String message);
//    void downLoadProgress(int progress);
}
