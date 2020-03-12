package com.dadoutek.uled.gateway.bean;

import android.os.Parcel;
import android.os.Parcelable;

import org.jetbrains.annotations.NotNull;

/**
 * 创建者     ZCL
 * 创建时间   2020/3/12 9:57
 * 描述
 * <p>
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
public class GatewayTagsBean implements Parcelable {
    private Long tagId;
    private String tagName;
    private int status; //开1 关0
    private int startHour;
    private int endHour;
    private int startMins;
    private int endMins;
    private int week;//bit位 0-6 周日-周六
    private String  weekStr;//bit位 0-6 周日-周六 7代表当天
    private boolean isCreateNew;
    private GatewayTasksBean tasks;//json字符串

    public GatewayTagsBean(Long tagId) {
        this.tagId = tagId;
    }

    protected GatewayTagsBean(Parcel in) {
        if (in.readByte() == 0) {
            tagId = null;
        } else {
            tagId = in.readLong();
        }
        tagName = in.readString();
        status = in.readInt();
        startHour = in.readInt();
        endHour = in.readInt();
        startMins = in.readInt();
        endMins = in.readInt();
        week = in.readInt();
        isCreateNew = in.readByte() != 0;
        tasks = in.readParcelable(GatewayTagsBean.class.getClassLoader());
    }

    public static final Creator<GatewayTagsBean> CREATOR = new Creator<GatewayTagsBean>() {
        @Override
        public GatewayTagsBean createFromParcel(Parcel in) {
            return new GatewayTagsBean(in);
        }

        @Override
        public GatewayTagsBean[] newArray(int size) {
            return new GatewayTagsBean[size];
        }
    };

    public GatewayTagsBean(long Id, @NotNull String name, @NotNull String weekString, int weekInt) {
        tagId =Id;
        tagName = name;
        weekStr = weekString;
        week = weekInt;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (tagId == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(tagId);
        }
        dest.writeString(tagName);
        dest.writeInt(status);
        dest.writeInt(startHour);
        dest.writeInt(endHour);
        dest.writeInt(startMins);
        dest.writeInt(endMins);
        dest.writeInt(week);
        dest.writeByte((byte) (isCreateNew ? 1 : 0));
        dest.writeParcelable(tasks, flags);
    }

    @Override
    public String toString() {
        return "GatewayTagsBean{" +
                "tagId=" + tagId +
                ", tagName='" + tagName + '\'' +
                ", status=" + status +
                ", startHour=" + startHour +
                ", endHour=" + endHour +
                ", startMins=" + startMins +
                ", endMins=" + endMins +
                ", week=" + week +
                ", isCreateNew=" + isCreateNew +
                ", tasks=" + tasks +
                '}';
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
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

    public int getWeek() {
        return week;
    }

    public void setWeek(int week) {
        this.week = week;
    }

    public boolean isCreateNew() {
        return isCreateNew;
    }

    public void setCreateNew(boolean createNew) {
        isCreateNew = createNew;
    }

    public GatewayTasksBean getTasks() {
        return tasks;
    }

    public void setTasks(GatewayTasksBean tasks) {
        this.tasks = tasks;
    }
}
