package com.dadoutek.uled.model;

import java.io.Serializable;

public class Lights extends DataStorageImpl<Light> implements Serializable, Cloneable {

    private static Lights mThis;

    private Lights() {
        super();
    }

    public static Lights getInstance() {

        if (mThis == null)
            mThis = new Lights();

        return mThis;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Lights lights = new Lights();
        if (mThis == null)
            mThis = new Lights();
        for (int i = 0; i < mThis.size(); i++) {
            Light light = (Light) mThis.get(i).clone();
            lights.add(light);
        }
        return lights;
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
