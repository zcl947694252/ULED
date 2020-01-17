package com.dadoutek.uled.switches.bean;

import java.io.Serializable;
import java.util.List;

/**
 * 创建者     ZCL
 * 创建时间   2020/1/16 17:30
 * 描述
 * <p>
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
public class EightSwitchItemBean implements Serializable {

        /**
         * belongRegionId : 1
         * id : 1
         * index : 1
         * keys : [{"keyId":1,"params":[0,0],"featureId":0}]
         * macAddr : aabbccddeeff
         * meshAddr : 255
         * name : 我是一个八键开关
         * productUUID : 100
         * uid : 300460
         */

        private int belongRegionId;
        private int id;
        private int index;
        private String macAddr;
        private int meshAddr;
        private String name;
        private int productUUID;
        private int uid;
        private List<KeysBean> keys;

        public int getBelongRegionId() {
            return belongRegionId;
        }

        public void setBelongRegionId(int belongRegionId) {
            this.belongRegionId = belongRegionId;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getMacAddr() {
            return macAddr;
        }

        public void setMacAddr(String macAddr) {
            this.macAddr = macAddr;
        }

        public int getMeshAddr() {
            return meshAddr;
        }

        public void setMeshAddr(int meshAddr) {
            this.meshAddr = meshAddr;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getProductUUID() {
            return productUUID;
        }

        public void setProductUUID(int productUUID) {
            this.productUUID = productUUID;
        }

        public int getUid() {
            return uid;
        }

        public void setUid(int uid) {
            this.uid = uid;
        }

        public List<KeysBean> getKeys() {
            return keys;
        }

        public void setKeys(List<KeysBean> keys) {
            this.keys = keys;
        }

        public static class KeysBean implements Serializable{
            /**
             * keyId : 1
             * params : [0,0]
             * featureId : 0
             */

            private int keyId;
            private int featureId;
            private List<Integer> params;

            public int getKeyId() {
                return keyId;
            }

            public void setKeyId(int keyId) {
                this.keyId = keyId;
            }

            public int getFeatureId() {
                return featureId;
            }

            public void setFeatureId(int featureId) {
                this.featureId = featureId;
            }

            public List<Integer> getParams() {
                return params;
            }

            public void setParams(List<Integer> params) {
                this.params = params;
            }
    }
}
