package com.dadoutek.uled.region.bean;

import java.util.List;

/**
 * 创建者     ZCL
 * 创建时间   2019/12/26 10:03
 * 描述
 * <p>
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
public class ParseCodeBean {
        /**
         * newRegionIds : [1]
         * switchToAuthorizerUserId : 300460
         * switchToRegionId : 1
         * switchToRegionName : 区域一
         * type : 1
         *

         newRegionIds	list	接收的新区域id数组
         switchToAuthorizerUserId	int	切换使用的授权用户id，可用作自动切换
         switchToRegionId	int	切换使用的区域id，可用作自动切换
         switchToRegionName	String	切换使用的区域名称，可用作提示时显示
         type	int	码的类型         */
        private int switchToAuthorizerUserId;
        private int switchToRegionId;
        private String switchToRegionName;
        private int type;
        private List<Integer> newRegionIds;

        public int getSwitchToAuthorizerUserId() {
            return switchToAuthorizerUserId;
        }

        public void setSwitchToAuthorizerUserId(int switchToAuthorizerUserId) {
            this.switchToAuthorizerUserId = switchToAuthorizerUserId;
        }

        public int getSwitchToRegionId() {
            return switchToRegionId;
        }

        public void setSwitchToRegionId(int switchToRegionId) {
            this.switchToRegionId = switchToRegionId;
        }

        public String getSwitchToRegionName() {
            return switchToRegionName;
        }

        public void setSwitchToRegionName(String switchToRegionName) {
            this.switchToRegionName = switchToRegionName;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public List<Integer> getNewRegionIds() {
            return newRegionIds;
        }

        public void setNewRegionIds(List<Integer> newRegionIds) {
            this.newRegionIds = newRegionIds;
        }
}
