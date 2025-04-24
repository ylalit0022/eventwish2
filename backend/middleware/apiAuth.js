const apiAuth = (req, res, next) => {
    console.log(`\n=== API Authentication Check ===`);
    console.log(`Path: ${req.path}`);
    console.log(`Method: ${req.method}`);
    console.log(`IP: ${req.ip}`);
    console.log(`\nHeaders received:`);
    console.log(JSON.stringify(req.headers, null, 2));
    
    const apiKey = req.headers['x-api-key'];
    const validApiKey = process.env.API_KEY;
    
    console.log(`\nAPI Key validation:`);
    console.log(`- Received key: ${apiKey ? apiKey.substring(0, 10) + '...' : 'undefined'}`);
    console.log(`- Expected key: ${validApiKey ? validApiKey.substring(0, 10) + '...' : 'undefined'}`);
    console.log(`- Key lengths: Received=${apiKey ? apiKey.length : 0}, Expected=${validApiKey ? validApiKey.length : 0}`);
    
    if (!apiKey) {
        console.log('❌ Authentication failed: API key is missing');
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
        console.log(`❌ Authentication failed: Invalid API key`);
        console.log(`Comparison details:`);
        console.log(`- Trimmed received: ${trimmedApiKey.substring(0, 10)}...`);
        console.log(`- Trimmed expected: ${trimmedValidApiKey.substring(0, 10)}...`);
        console.log(`- Trimmed lengths: Received=${trimmedApiKey.length}, Expected=${trimmedValidApiKey.length}`);
        console.log(`- Character by character comparison:`);
        for (let i = 0; i < Math.max(trimmedApiKey.length, trimmedValidApiKey.length); i++) {
            if (trimmedApiKey[i] !== trimmedValidApiKey[i]) {
                console.log(`  Position ${i}: Received='${trimmedApiKey[i] || 'undefined'}' Expected='${trimmedValidApiKey[i] || 'undefined'}'`);
            }
        }
        return res.status(401).json({
            success: false,
            message: 'API key is not valid',
            error: 'API_KEY_INVALID',
            debug: {
                receivedLength: trimmedApiKey.length,
                expectedLength: trimmedValidApiKey.length,
                receivedPrefix: trimmedApiKey.substring(0, 10),
                expectedPrefix: trimmedValidApiKey.substring(0, 10)
            }
        });
    }
    
    console.log(`✅ API key validation successful`);
    next();
};

module.exports = apiAuth;
