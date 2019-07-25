package com.dadoutek.uled.network;

import java.io.Serializable;

/**
 * 创建者     ZCL
 * 创建时间   2019/7/25 15:14
 * 描述	      ${TODO}
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
public class VersionBean /*extends BaseBean*/ implements Serializable {

    /**
     * data : {"create_time":"2019-03-27 14:32:02","description":"安卓最新版本2.3.1.7","platform":0,"url":"https://dadou-bucket-shenzhen.oss-cn-shenzhen.aliyuncs.com/ ","version":"2.3.1.7"}
     * errorCode : 0
     * message : 获取最新版本成功!
     * serverTime : 1564043319035
     */

    private DataBean data;
    private int errorCode;
    private String message;
    private long serverTime;

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }

    public static class DataBean {
        /**
         * create_time : 2019-03-27 14:32:02
         * description : 安卓最新版本2.3.1.7
         * platform : 0
         * url : https://dadou-bucket-shenzhen.oss-cn-shenzhen.aliyuncs.com/
         * version : 2.3.1.7
         */

        private String create_time;
        private String description;
        private int platform;
        private String url;
        private String version;

        public String getCreate_time() {
            return create_time;
        }

        public void setCreate_time(String create_time) {
            this.create_time = create_time;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getPlatform() {
            return platform;
        }

        public void setPlatform(int platform) {
            this.platform = platform;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
