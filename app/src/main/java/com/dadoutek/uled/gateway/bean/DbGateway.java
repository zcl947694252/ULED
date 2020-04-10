package com.dadoutek.uled.gateway.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.dadoutek.uled.R;
import com.google.gson.annotations.Expose;
import com.telink.bluetooth.light.ConnectionStatus;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Transient;
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
public class DbGateway implements Parcelable {
    @Id(autoincrement = true)
    private Long id;
    private int meshAddr;
    private String name;//标签名
    private String macAddr;
    private String sixByteMacAddr;
    private int type;//网关模式 0定时 1循环
    private int productUUID;
    private String version;
    private int belongRegionId;
    private int pos;
    private int uid;
    private String tags;
    private String timePeriodTags;
    private int addTag = 0;//0 添加新的tag 1编辑已有tag
    private int state = 1; //1代表在线 0代表离线
    private int openTag =1; //1代表开 0代表关
    @Expose(serialize = false, deserialize = false)
    @Transient
    public int icon = R.drawable.icon_gw_open;//灯状态显示图

    public DbGateway(Long id) {
        this.id = id;
    }


    protected DbGateway(Parcel in) {
        if (in.readByte() == 0) { id = null; } else { id = in.readLong(); }
        meshAddr = in.readInt();
        name = in.readString();
        macAddr = in.readString();
        sixByteMacAddr = in.readString();
        type = in.readInt();
        productUUID = in.readInt();
        version = in.readString();
        belongRegionId = in.readInt();
        pos = in.readInt();
        uid = in.readInt();
        tags = in.readString();
        timePeriodTags = in.readString();
        addTag = in.readInt();
        state = in.readInt();
        openTag = in.readInt();
        icon = in.readInt();
    }


    @Generated(hash = 874974623)
    public DbGateway(Long id, int meshAddr, String name, String macAddr, String sixByteMacAddr, int type, int productUUID, String version, int belongRegionId, int pos, int uid, String tags, String timePeriodTags, int addTag, int state, int openTag) {
        this.id = id;
        this.meshAddr = meshAddr;
        this.name = name;
        this.macAddr = macAddr;
        this.sixByteMacAddr = sixByteMacAddr;
        this.type = type;
        this.productUUID = productUUID;
        this.version = version;
        this.belongRegionId = belongRegionId;
        this.pos = pos;
        this.uid = uid;
        this.tags = tags;
        this.timePeriodTags = timePeriodTags;
        this.addTag = addTag;
        this.state = state;
        this.openTag = openTag;
    }


    @Generated(hash = 1696080529)
    public DbGateway() {
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (id == null) { dest.writeByte((byte) 0); } else {
            dest.writeByte((byte) 1);
            dest.writeLong(id);
        }
        dest.writeInt(meshAddr);
        dest.writeString(name);
        dest.writeString(macAddr);
        dest.writeString(sixByteMacAddr);
        dest.writeInt(type);
        dest.writeInt(productUUID);
        dest.writeString(version);
        dest.writeInt(belongRegionId);
        dest.writeInt(pos);
        dest.writeInt(uid);
        dest.writeString(tags);
        dest.writeString(timePeriodTags);
        dest.writeInt(addTag);
        dest.writeInt(state);
        dest.writeInt(openTag);
        dest.writeInt(icon);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DbGateway> CREATOR = new Creator<DbGateway>() {
        @Override
        public DbGateway createFromParcel(Parcel in) {
            return new DbGateway(in);
        }

        @Override
        public DbGateway[] newArray(int size) {
            return new DbGateway[size];
        }
    };

    public void updateIcon() {
        if (this.state == ConnectionStatus.OFF.getValue()) {
            this.icon = R.drawable.icon_gw_close;
        } else if (this.state == ConnectionStatus.ON.getValue()) {
            this.icon = R.drawable.icon_gw_open;
        }
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

    public int getAddTag() {
        return addTag;
    }

    public void setAddTag(int addTag) {
        this.addTag = addTag;
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

    public int getState() {
        return this.state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public int getPos() {
        return this.pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getTimePeriodTags() {
        return timePeriodTags;
    }

    public void setTimePeriodTags(String timePeriodTags) {
        this.timePeriodTags = timePeriodTags;
    }

    public String getSixByteMacAddr() {
        return sixByteMacAddr;
    }

    public void setSixByteMacAddr(String sixByteMacAddr) {
        this.sixByteMacAddr = sixByteMacAddr;
    }

    @Override
    public String toString() {
        return "DbGateway{" + "id=" + id + ", meshAddr=" + meshAddr + ", name='" + name + '\'' +
                ", macAddr='" + macAddr + '\'' + ", sixByteMacAddr='" + sixByteMacAddr + '\'' +
                ", type=" + type + ", productUUID=" + productUUID + ", version='" + version + '\'' + ", belongRegionId=" + belongRegionId + ", pos=" + pos + ", uid=" + uid + ", tags='" + tags + '\'' + ", timePeriodTags='" + timePeriodTags + '\'' + ", addTag=" + addTag + ", state=" + state + ", openTag=" + openTag + ", icon=" + icon + '}';
    }

    public int getOpenTag() {
        return this.openTag;
    }

    public void setOpenTag(int openTag) {
        this.openTag = openTag;
    }
}
