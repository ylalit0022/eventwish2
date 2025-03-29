const coins = {
    deviceId: "b21ec989-47f9-d593-64ad-3643ff97f239",
    coins: 0,
    isUnlocked: false,
    unlockTimestamp: null,
    unlockDuration: 30,
    unlockSignature: null,
    lastRewardTimestamp: null,
    rewardHistory: [],
    securityViolations: [],
    lastSyncTimestamp: new Date(),
    timeOffset: 0,
    deviceIntegrity: {
        fingerprint: null,
        lastVerifiedAt: null,
        verified: false,
        failedVerifications: 0
    },
    plan: {
        requiredCoins: 100,
        coinsPerReward: 10,
        defaultUnlockDuration: 30
    },
    auth: {
        token: null,
        refreshToken: null,
        tokenExpiry: null,
        refreshTokenExpiry: null,
        lastLogin: null,
        isAuthenticated: false,
        deviceInfo: {}
    }
};

module.exports = coins;
