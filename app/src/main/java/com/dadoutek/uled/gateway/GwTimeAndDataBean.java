package com.dadoutek.uled.gateway;

import com.dadoutek.uled.gateway.bean.GwTasksBean;

/**
 * 创建者     ZCL
 * 创建时间   2020/4/2 16:31
 * 描述
 * <p>
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
public class GwTimeAndDataBean {
    private  String name;
    private  int endTime;
    private  int startTime;
    private  Long id;
    private  String week;
    private  GwTasksBean timeTask;

    public GwTimeAndDataBean(Long id,String name ,String week, int startTime,int endTime) {
        this.id = id;
        this.name = name;
        this.week = week;
        this.startTime = startTime;
        this.endTime = endTime;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public String getWeek() {
        return week;
    }

    public void setWeek(String week) {
        this.week = week;
    }

    public GwTasksBean getTimeTask() {
        return timeTask;
    }

    public void setTimeTask(GwTasksBean timeTask) {
        this.timeTask = timeTask;
    }

    @Override
    public String toString() {
        return "GwTimeAndDataBean{" + "name='" + name + '\'' + ", endTime=" + endTime + ", " +
                "startTime=" + startTime + ", id=" + id + ", week='" + week + '\'' + ", timeTask" +
                "=" + timeTask + '}';
    }
}
