const apiAuth = (req, res, next) => {
    const apiKey = req.headers['x-api-key'];
    
    if (!apiKey || apiKey !== process.env.API_KEY) {
        return res.status(401).json({
            success: false,
            message: 'No API key, authorization denied',
            error: 'API_KEY_MISSING'
        });
    }
    
    next();
};

module.exports = apiAuth;
