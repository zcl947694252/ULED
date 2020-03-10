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
 * <p>[1:3]：0x111111，表示sequence no为0x111111
 * [4:5]：0x0000
 * [6:7]：0x0000
 * [8]：0xf8，表示cmd为0xf8
 * [9:10]：VendorID，默认为 0x11 0x02
 * [11]: 0x00, label_id标签id, 0-255不能重复
 * [12]: 0x01, label_on/off 标签开关 0: off, 1:on
 * [13]: 0x01, week循环,0—6代表星期天----星期六  7代表当天
 * [14]: 0x00, 时间条index
 * [15]: 0x00, 定时时间，小时
 * [16]: 0x00, 定时时间，分钟
 * [17] :0x00, 场景ID
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
@Entity
public class DbGatewayTimeBean implements Parcelable {
    @Id(autoincrement = true)
    private  Long label_id;
    private  int label_switch;
    private  int hour;
    private  int minute;
    private  String week;
    private  int index;
    private  Boolean isNew;
    private  Long sceneId;
    private  String  sceneName;

    public DbGatewayTimeBean(Parcel in) {
        if (in.readByte() == 0) {
            label_id = null;
        } else {
            label_id = in.readLong();
        }
        label_switch = in.readInt();
        hour = in.readInt();
        minute = in.readInt();
        week = in.readString();
        index = in.readInt();
        byte tmpIsNew = in.readByte();
        isNew = tmpIsNew == 0 ? null : tmpIsNew == 1;
        if (in.readByte() == 0) {
            sceneId = null;
        } else {
            sceneId = in.readLong();
        }
        sceneName = in.readString();
    }

    public static final Creator<DbGatewayTimeBean> CREATOR = new Creator<DbGatewayTimeBean>() {
        @Override
        public DbGatewayTimeBean createFromParcel(Parcel in) {
            return new DbGatewayTimeBean(in);
        }

        @Override
        public DbGatewayTimeBean[] newArray(int size) {
            return new DbGatewayTimeBean[size];
        }
    };

    public DbGatewayTimeBean(int hourTime, int minuteTime, boolean b) {
        this.hour = hourTime;
        this.minute = minuteTime;
        this.isNew = b;
    }

    @Generated(hash = 1032664304)
    public DbGatewayTimeBean(Long label_id, int label_switch, int hour, int minute, String week,
            int index, Boolean isNew, Long sceneId, String sceneName) {
        this.label_id = label_id;
        this.label_switch = label_switch;
        this.hour = hour;
        this.minute = minute;
        this.week = week;
        this.index = index;
        this.isNew = isNew;
        this.sceneId = sceneId;
        this.sceneName = sceneName;
    }

    @Generated(hash = 1040721166)
    public DbGatewayTimeBean() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (label_id == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(label_id);
        }
        dest.writeInt(label_switch);
        dest.writeInt(hour);
        dest.writeInt(minute);
        dest.writeString(week);
        dest.writeInt(index);
        dest.writeByte((byte) (isNew == null ? 0 : isNew ? 1 : 2));
        if (sceneId == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(sceneId);
        }
        dest.writeString(sceneName);
    }

    public Long getLabel_id() {
        return label_id;
    }

    public void setLabel_id(Long label_id) {
        this.label_id = label_id;
    }

    public int getLabel_switch() {
        return label_switch;
    }

    public void setLabel_switch(int label_switch) {
        this.label_switch = label_switch;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public String getWeek() {
        return week;
    }

    public void setWeek(String week) {
        this.week = week;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Boolean getNew() {
        return isNew;
    }

    public void setNew(Boolean aNew) {
        isNew = aNew;
    }

    public Long getSceneId() {
        return sceneId;
    }

    public void setSceneId(Long sceneId) {
        this.sceneId = sceneId;
    }

    public String getSceneName() {
        return sceneName;
    }

    public void setSceneName(String sceneName) {
        this.sceneName = sceneName;
    }

    public static Creator<DbGatewayTimeBean> getCREATOR() {
        return CREATOR;
    }

    public Boolean getIsNew() {
        return this.isNew;
    }

    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }

    @Override
    public String toString() {
        return "DbGatewayTimeBean{" +
                "label_id=" + label_id +
                ", label_switch=" + label_switch +
                ", hour=" + hour +
                ", minute=" + minute +
                ", week='" + week + '\'' +
                ", index=" + index +
                ", isNew=" + isNew +
                ", sceneId=" + sceneId +
                ", sceneName='" + sceneName + '\'' +
                '}';
    }
}
