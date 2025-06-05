require('dotenv').config();

module.exports = {
    jwt: {
        secret: process.env.JWT_SECRET,
        refreshSecret: process.env.REFRESH_TOKEN_SECRET,
        tokenExpiry: parseInt(process.env.TOKEN_EXPIRY, 10),
        refreshTokenExpiry: parseInt(process.env.REFRESH_TOKEN_EXPIRY, 10)
    },
    mongodb: {
        uri: process.env.MONGODB_URI,
        debug: process.env.MONGODB_DEBUG === 'true'
    },
    security: {
        validAppSignatures: process.env.VALID_APP_SIGNATURES?.split(',') || [],
        maxFailedAttempts: parseInt(process.env.MAX_FAILED_ATTEMPTS, 10),
        lockoutDuration: parseInt(process.env.LOCKOUT_DURATION, 10)
    },
    rateLimit: {
        windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS || '900000', 10),
        max: Infinity
    }
};
