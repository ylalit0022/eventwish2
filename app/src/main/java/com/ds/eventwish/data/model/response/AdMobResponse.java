package com.ds.eventwish.data.model.response;

import com.ds.eventwish.data.model.ads.AdUnit;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Response model for AdMob API calls
 */
public class AdMobResponse extends BaseResponse<AdMobResponse.AdMobData> {

    /**
     * Data container for AdMob response
     */
    public static class AdMobData {
        @SerializedName("adUnits")
        private List<AdUnit> adUnits;
        
        @SerializedName("status")
        private Map<String, AdStatus> adStatus;
        
        @SerializedName("canShow")
        private boolean canShow;
        
        @SerializedName("reason")
        private String reason;
        
        @SerializedName("nextAvailable")
        private String nextAvailable;

        public List<AdUnit> getAdUnits() {
            return adUnits;
        }

        public void setAdUnits(List<AdUnit> adUnits) {
            this.adUnits = adUnits;
        }

        public Map<String, AdStatus> getAdStatus() {
            return adStatus;
        }

        public void setAdStatus(Map<String, AdStatus> adStatus) {
            this.adStatus = adStatus;
        }

        public boolean isCanShow() {
            return canShow;
        }

        public void setCanShow(boolean canShow) {
            this.canShow = canShow;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getNextAvailable() {
            return nextAvailable;
        }

        public void setNextAvailable(String nextAvailable) {
            this.nextAvailable = nextAvailable;
        }
    }
    
    /**
     * Status of an ad unit
     */
    public static class AdStatus {
        @SerializedName("adUnitId")
        private String adUnitId;
        
        @SerializedName("adType")
        private String adType;
        
        @SerializedName("canShow")
        private boolean canShow;
        
        @SerializedName("reason")
        private String reason;
        
        @SerializedName("nextAvailable")
        private String nextAvailable;
        
        @SerializedName("reward")
        private AdReward reward;

        public String getAdUnitId() {
            return adUnitId;
        }

        public void setAdUnitId(String adUnitId) {
            this.adUnitId = adUnitId;
        }

        public String getAdType() {
            return adType;
        }

        public void setAdType(String adType) {
            this.adType = adType;
        }

        public boolean isCanShow() {
            return canShow;
        }

        public void setCanShow(boolean canShow) {
            this.canShow = canShow;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getNextAvailable() {
            return nextAvailable;
        }

        public void setNextAvailable(String nextAvailable) {
            this.nextAvailable = nextAvailable;
        }

        public AdReward getReward() {
            return reward;
        }

        public void setReward(AdReward reward) {
            this.reward = reward;
        }
    }
    
    /**
     * Reward information for rewarded ads
     */
    public static class AdReward {
        @SerializedName("canReward")
        private boolean canReward;
        
        @SerializedName("requiredCoins")
        private int requiredCoins;
        
        @SerializedName("coinsPerReward")
        private int coinsPerReward;
        
        @SerializedName("cooldownUntil")
        private String cooldownUntil;

        public boolean isCanReward() {
            return canReward;
        }

        public void setCanReward(boolean canReward) {
            this.canReward = canReward;
        }

        public int getRequiredCoins() {
            return requiredCoins;
        }

        public void setRequiredCoins(int requiredCoins) {
            this.requiredCoins = requiredCoins;
        }

        public int getCoinsPerReward() {
            return coinsPerReward;
        }

        public void setCoinsPerReward(int coinsPerReward) {
            this.coinsPerReward = coinsPerReward;
        }

        public String getCooldownUntil() {
            return cooldownUntil;
        }

        public void setCooldownUntil(String cooldownUntil) {
            this.cooldownUntil = cooldownUntil;
        }
    }
} 