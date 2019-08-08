package com.dadoutek.uled.region.bean;

/**
 * 创建者     ZCL
 * 创建时间   2019/8/8 10:04
 * 描述	      ${TODO}
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
public class TransferData {
        /**
         * expire : 86400
         * code : dadoueyJhbGciOiJIUzI1NiJ9.eyJhdXRob3JpemVyX2lkIjoyNjQ0NywicmVnaW9uX2lkIjowLCJsZXZlbCI6MH0.IqcdLPRgBj3BMy_piagIybESLTiqIe0-r531q8pOcSUsmartlight
         * type : 0
         */

        private int expire;
        private String code;
        private int type;

        public int getExpire() {
            return expire;
        }

        public void setExpire(int expire) {
            this.expire = expire;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }
}
