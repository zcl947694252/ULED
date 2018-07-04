package com.dadoutek.uled.intf;

public interface SyncCallback {
    public void complete();
    public void error(String msg);
    public void start();
}
