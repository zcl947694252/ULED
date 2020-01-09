package com.dadoutek.uled.network.bean;

/**
 * 创建者     ZCL
 * 创建时间   2019/12/23 11:15
 * 描述
 * <p>
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
public class TransferRegionBean {
        /**
         * code : dadoueyJhbGciOiJIUzI1NiJ9.eyJhdXRob3JpemVyX2lkIjo0MDAxMDMsImxldmVsIjotMSwicmVnaW9uX2lkIjoyfQ.y4lDVkI5OLwnAq0stnS25gUIIq2d_4ysNUAZ1ofMLoosmartlight
         * expire : 180
         * type : -1
         */

        private String code;
        private int expire;
        private int type;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public int getExpire() {
            return expire;
        }

        public void setExpire(int expire) {
            this.expire = expire;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }
}
