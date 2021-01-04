package com.dadoutek.uled.gateway.bean;

/**
 * 创建者     ZCL
 * 创建时间   2020/5/7 11:36
 * 描述
 * <p>
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
public class ClearGwBean {
        /**
         * belongRegionId : 1
         * id : 102
         * lastOfflineTime : null
         * lastOnlineTime : 2020-03-23 06:50:43
         * macAddr : 222
         * meshAddr : 11
         * name : 未命名
         * productUUID : 1
         * state : 1
         * tags : []
         * timePeriodTags : []
         * type : 0
         * uid : 300460
         * version :
         * openTag : 1
         */

        private int belongRegionId;
        private int id;
        private Object lastOfflineTime;
        private String lastOnlineTime;
        private String macAddr;
        private int meshAddr;
        private String name;
        private int productUUID;
        private int state;
        private String tags;
        private String timePeriodTags;
        private int type;
        private int uid;
        private String version;
        private int openTag;

        public int getBelongRegionId() { return belongRegionId;}

        public void setBelongRegionId(int belongRegionId) { this.belongRegionId = belongRegionId;}

        public int getId() { return id;}

        public void setId(int id) { this.id = id;}

        public Object getLastOfflineTime() { return lastOfflineTime;}

        public void setLastOfflineTime(Object lastOfflineTime) { this.lastOfflineTime =
                lastOfflineTime;}

        public String getLastOnlineTime() { return lastOnlineTime;}

        public void setLastOnlineTime(String lastOnlineTime) { this.lastOnlineTime =
                lastOnlineTime;}

        public String getMacAddr() { return macAddr;}

        public void setMacAddr(String macAddr) { this.macAddr = macAddr;}

        public int getMeshAddr() { return meshAddr;}

        public void setMeshAddr(int meshAddr) { this.meshAddr = meshAddr;}

        public String getName() { return name;}

        public void setName(String name) { this.name = name;}

        public int getProductUUID() { return productUUID;}

        public void setProductUUID(int productUUID) { this.productUUID = productUUID;}

        public int getState() { return state;}

        public void setState(int state) { this.state = state;}

        public String getTags() { return tags;}

        public void setTags(String tags) { this.tags = tags;}

        public String getTimePeriodTags() { return timePeriodTags;}

        public void setTimePeriodTags(String timePeriodTags) { this.timePeriodTags = timePeriodTags;}

        public int getType() { return type;}

        public void setType(int type) { this.type = type;}

        public int getUid() { return uid;}

        public void setUid(int uid) { this.uid = uid;}

        public String getVersion() { return version;}

        public void setVersion(String version) { this.version = version;}

        public int getOpenTag() { return openTag;}

        public void setOpenTag(int openTag) { this.openTag = openTag;}
}
