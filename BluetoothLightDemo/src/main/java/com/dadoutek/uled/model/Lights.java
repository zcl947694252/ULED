package com.dadoutek.uled.model;

import java.io.Serializable;

public class Lights extends DataStorageImpl<Light> implements Serializable{

    private static Lights mThis;

    private Lights() {
        super();
    }

    public static Lights getInstance() {

        if (mThis == null)
            mThis = new Lights();

        return mThis;
    }

    public boolean contains(int meshAddress) {
        synchronized (this) {
            return this.contains("meshAddress", meshAddress);
        }
    }

    public Light getByMeshAddress(int meshAddress) {
        synchronized (this) {
            return this.get("meshAddress", meshAddress);
        }
    }
}
