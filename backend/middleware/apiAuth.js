const apiAuth = (req, res, next) => {
    const apiKey = req.headers['x-api-key'];
    const validApiKey = process.env.API_KEY;
    
    if (!apiKey) {
        return res.status(401).json({
            success: false,
            message: 'API key is missing',
            error: 'API_KEY_MISSING'
        });
    }
    
    // Trim both keys to remove any whitespace issues
    const trimmedApiKey = apiKey.trim();
    const trimmedValidApiKey = validApiKey.trim();
    
    if (trimmedApiKey !== trimmedValidApiKey) {
        console.log(`API key validation failed. Received: ${trimmedApiKey.substring(0, 10)}... Expected: ${trimmedValidApiKey.substring(0, 10)}...`);
        console.log(`API key lengths - Received: ${trimmedApiKey.length}, Expected: ${trimmedValidApiKey.length}`);
        return res.status(401).json({
            success: false,
            message: 'API key is not valid',
            error: 'API_KEY_INVALID'
        });
    }
    
    next();
};

module.exports = apiAuth;
