package com.dadoutek.uled.gateway.bean;

import android.os.Parcel;
import android.os.Parcelable;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * 创建者     ZCL
 * 创建时间   2020/3/10 15:12
 * 描述
 * {
 * "id": 1,
 * "meshAddr": 32768,
 * "gatewayName": "我是一个gateway",
 * "macAddr": "aabbccddeeff",
 * "productUUID": 100, // for example
 * "firmwareVersion": "gateway-1.0.0",
 * "tags": "[{
 * \"id\": 1,
 * \"tagName\": \"标签名\",
 * \"status\": 0,
 * ...
 * },
 * {
 * ...
 * }
 * ]"
 * }
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
@Entity
public class DbGatewayBean implements Parcelable {
    @Id(autoincrement = true)
    private  Long id;
    private  int meshAddr;
    private  String name;
    private  String macAddr;
    private  int type;
    private  int productUUID;
    private  String version;
    private  int belongRegionId;
    private  String tags;

    protected DbGatewayBean(Parcel in) {
        if (in.readByte() == 0) {
            id = null;
        } else {
            id = in.readLong();
        }
        meshAddr = in.readInt();
        name = in.readString();
        macAddr = in.readString();
        type = in.readInt();
        productUUID = in.readInt();
        version = in.readString();
        belongRegionId = in.readInt();
        tags = in.readString();
    }

    @Generated(hash = 810737412)
    public DbGatewayBean(Long id, int meshAddr, String name, String macAddr, int type,
            int productUUID, String version, int belongRegionId, String tags) {
        this.id = id;
        this.meshAddr = meshAddr;
        this.name = name;
        this.macAddr = macAddr;
        this.type = type;
        this.productUUID = productUUID;
        this.version = version;
        this.belongRegionId = belongRegionId;
        this.tags = tags;
    }

    @Generated(hash = 1615511649)
    public DbGatewayBean() {
    }

    public static final Creator<DbGatewayBean> CREATOR = new Creator<DbGatewayBean>() {
        @Override
        public DbGatewayBean createFromParcel(Parcel in) {
            return new DbGatewayBean(in);
        }

        @Override
        public DbGatewayBean[] newArray(int size) {
            return new DbGatewayBean[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (id == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(id);
        }
        dest.writeInt(meshAddr);
        dest.writeString(name);
        dest.writeString(macAddr);
        dest.writeInt(type);
        dest.writeInt(productUUID);
        dest.writeString(version);
        dest.writeInt(belongRegionId);
        dest.writeString(tags);
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getMeshAddr() {
        return this.meshAddr;
    }

    public void setMeshAddr(int meshAddr) {
        this.meshAddr = meshAddr;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMacAddr() {
        return this.macAddr;
    }

    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getProductUUID() {
        return this.productUUID;
    }

    public void setProductUUID(int productUUID) {
        this.productUUID = productUUID;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getBelongRegionId() {
        return this.belongRegionId;
    }

    public void setBelongRegionId(int belongRegionId) {
        this.belongRegionId = belongRegionId;
    }

    public String getTags() {
        return this.tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

}
