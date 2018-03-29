package com.dadoutek.uled.model;

import java.io.Serializable;

public class Groups extends DataStorageImpl<Group> implements Serializable{

    private static Groups mThis;

    private Groups() {
        super();
    }

    public static Groups getInstance() {

        if (mThis == null)
            mThis = new Groups();

        return mThis;
    }

    public boolean contains(int meshAddress) {
        return this.contains("meshAddress", meshAddress);
    }

    public Group getByMeshAddress(int meshAddress) {
        return this.get("meshAddress", meshAddress);
    }

}
