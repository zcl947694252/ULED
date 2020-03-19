package com.dadoutek.uled.gateway.bean;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * 创建者     ZCL
 * 创建时间   2020/3/12 10:21
 * 描述
 * <p>
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
public class GwTasksBean implements Parcelable {
    private int index;//循环模式下时间段下标
    private int stateTime;//停留时间
    private long sceneId;//场景id
    private String senceName;//场景名
    private boolean createNew;//是否是新创建对象
    private int startHour;
    private int endHour;
    private int startMins;
    private int endMins;
    private int selectPos;//本地使用
    private ArrayList<GwTimePeriodsBean> timingPeriods;//时间段list

    public GwTasksBean(int index) {
        this.index = index;
    }

    protected GwTasksBean(Parcel in) {
        index = in.readInt();
        stateTime = in.readInt();
        sceneId = in.readLong();
        senceName = in.readString();
        createNew = in.readByte() != 0;
        startHour = in.readInt();
        endHour = in.readInt();
        startMins = in.readInt();
        endMins = in.readInt();
        selectPos = in.readInt();
        timingPeriods = in.createTypedArrayList(GwTimePeriodsBean.CREATOR);
    }

    public static final Creator<GwTasksBean> CREATOR = new Creator<GwTasksBean>() {
        @Override
        public GwTasksBean createFromParcel(Parcel in) {
            return new GwTasksBean(in);
        }

        @Override
        public GwTasksBean[] newArray(int size) {
            return new GwTasksBean[size];
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

    public int getSelectPos() {
        return selectPos;
    }

    public void setSelectPos(int selectPos) {
        this.selectPos = selectPos;
    }

    public ArrayList<GwTimePeriodsBean> getTimingPeriods() {
        return timingPeriods;
    }

    public void setTimingPeriods(ArrayList<GwTimePeriodsBean> timingPeriods) {
        this.timingPeriods = timingPeriods;
    }


    @Override
    public String toString() {
        return "GwTasksBean{" +
                "index=" + index +
                ", stateTime=" + stateTime +
                ", sceneId=" + sceneId +
                ", senceName='" + senceName + '\'' +
                ", createNew=" + createNew +
                ", startHour=" + startHour +
                ", endHour=" + endHour +
                ", startMins=" + startMins +
                ", endMins=" + endMins +
                ", selectPos=" + selectPos +
                ", timingPeriods='" + timingPeriods + '\'' +
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
        dest.writeInt(selectPos);
        dest.writeTypedList(timingPeriods);
    }
}
