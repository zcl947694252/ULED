package com.dadoutek.uled.gateway.bean;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * 创建者     ZCL
 * 创建时间   2020/3/12 9:57
 * 描述  tag标签时间表tasks的bean
 * <p>
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
@SuppressLint("ParcelCreator")
public class GwTagBean implements Parcelable {
    private String macAddr;//本地使用
    private int meshAddr;//本地使用
    private Long tagId;
    private String tagName;
    private int status; //开1 关0 tag的外部现实
    private int startHour;
    private int endHour;
    private int startMins;
    private int endMins;
    private int pos;
    private boolean isTimer;
    private int week;//bit位 0-6 周日-周六
    private String  weekStr;//bit位 0-6 周日-周六 7代表当天
    private boolean isCreateNew;
    private String tasks;//json字符串


    public GwTagBean(Long tagId) {
        this.tagId = tagId;
    }

    public GwTagBean(long id, String tagName, String weekStr, int week) {
        tagId = id;
        this.tagName =  tagName;
        this.weekStr =  weekStr;
        this.week =  week;
    }

    public int getMeshAddr() {
        return meshAddr;
    }

    public void setMeshAddr(int meshAddr) {
        this.meshAddr = meshAddr;
    }

    public String getMacAddr() {
        return macAddr;
    }

    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
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

    public String getWeekStr() {
        return weekStr;
    }

    public void setWeekStr(String weekStr) {
        this.weekStr = weekStr;
    }

    public boolean isCreateNew() {
        return isCreateNew;
    }

    public void setCreateNew(boolean createNew) {
        isCreateNew = createNew;
    }

    public String getTasks() {
        return tasks;
    }

    public boolean getIsTimer() {
        return isTimer;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public boolean isTimer() {
        return isTimer;
    }

    public void setTimer(boolean timer) {
        isTimer = timer;
    }

    public void setIsTimer(boolean isTimer) {
        this.isTimer = isTimer;
    }

    @Override
    public String toString() {
        return "GwTagBean{" + "macAddr='" + macAddr + '\'' + ", meshAddr=" + meshAddr + ", tagId" +
                "=" + tagId + ", macAddr='" + tagName + '\'' + ", status=" + status + ", " +
                "startHour=" + startHour + ", endHour=" + endHour + ", startMins=" + startMins +
                ", endMins=" + endMins + ", pos=" + pos + ", isTimer=" + isTimer + ", week=" + week + ", weekStr='" + weekStr + '\'' + ", isCreateNew=" + isCreateNew + ", tasks=" + tasks + '}';
    }

    public void setTasks(String tasks) {
        this.tasks = tasks;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
