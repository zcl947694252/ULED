package com.dadoutek.uled.gateway.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 创建者     ZCL
 * 创建时间   2020/3/12 10:21
 * 描述
 * <p>
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
public class GatewayTasksBean implements Parcelable {
    private int index;//循环模式下时间段下标
    private int stateTime;//停留时间
    private long sceneId;//场景id
    private String senceName;//场景名
    private boolean createNew;//是否是新创建对象
    private int startHour;
    private int endHour;
    private int startMins;
    private int endMins;

    public GatewayTasksBean(int index) {
        this.index = index;
    }

    protected GatewayTasksBean(Parcel in) {
        index = in.readInt();
        stateTime = in.readInt();
        sceneId = in.readLong();
        senceName = in.readString();
        createNew = in.readByte() != 0;
        startHour = in.readInt();
        endHour = in.readInt();
        startMins = in.readInt();
        endMins = in.readInt();
    }

    public static final Creator<GatewayTasksBean> CREATOR = new Creator<GatewayTasksBean>() {
        @Override
        public GatewayTasksBean createFromParcel(Parcel in) {
            return new GatewayTasksBean(in);
        }

        @Override
        public GatewayTasksBean[] newArray(int size) {
            return new GatewayTasksBean[size];
        }
    };

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getStateTime() {
        return stateTime;
    }

    public void setStateTime(int stateTime) {
        this.stateTime = stateTime;
    }

    public long getSceneId() {
        return sceneId;
    }

    public void setSceneId(Long sceneId) {
        this.sceneId = sceneId;
    }

    public String getSenceName() {
        return senceName;
    }

    public void setSceneId(long sceneId) {
        this.sceneId = sceneId;
    }

    public int getStartHour() {
        return startHour;
    }

    public void setStartHour(int startHour) {
        this.startHour = startHour;
    }

    public int getEndHour() {
        return endHour;
    }

    public void setEndHour(int endHour) {
        this.endHour = endHour;
    }

    public int getStartMins() {
        return startMins;
    }

    public void setStartMins(int startMins) {
        this.startMins = startMins;
    }

    public int getEndMins() {
        return endMins;
    }

    public void setEndMins(int endMins) {
        this.endMins = endMins;
    }

    public void setSenceName(String senceName) {
        this.senceName = senceName;
    }

    public boolean isCreateNew() {
        return createNew;
    }

    public void setCreateNew(boolean createNew) {
        this.createNew = createNew;
    }

    @Override
    public String toString() {
        return "GatewayTasksBean{" +
                "index=" + index +
                ", stateTime=" + stateTime +
                ", sceneId=" + sceneId +
                ", senceName='" + senceName + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(index);
        dest.writeInt(stateTime);
        dest.writeLong(sceneId);
        dest.writeString(senceName);
        dest.writeByte((byte) (createNew ? 1 : 0));
        dest.writeInt(startHour);
        dest.writeInt(endHour);
        dest.writeInt(startMins);
        dest.writeInt(endMins);
    }
}
