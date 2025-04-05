/**
 * Script to test the recommendation service with generated user activity data
 * Analyzes recommendation quality for different user profile types
 */
const mongoose = require('mongoose');
const User = require('../models/User');
const Template = require('../models/Template');
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

// Create a simplified test version of the recommendation service without cacheService dependency
const testRecommendationService = {
    /**
     * Get recommendations for a user based on their category history
     */
    getRecommendationsForUser: async (deviceId, limit = 10) => {
        try {
            // Find the user
            const user = await User.findOne({ deviceId });
            
            if (!user || !user.categories || user.categories.length === 0) {
                console.log(`No category history for user: ${deviceId}, using default recommendations`);
                return await testRecommendationService.getDefaultRecommendations(limit);
            }
            
            // Get user categories and score them
            const categories = [...user.categories];
            
            // Find max visit count and date ranges for normalization
            const maxVisitCount = Math.max(...categories.map(cat => cat.visitCount || 1));
            const visitDates = categories.map(cat => new Date(cat.visitDate).getTime());
            const oldestVisitDate = Math.min(...visitDates);
            const newestVisitDate = Math.max(...visitDates);
            
            // Calculate scores for each category
            const scoredCategories = categories.map(category => {
                const visitCount = category.visitCount || 1;
                const visitDate = new Date(category.visitDate).getTime();
                
                // Calculate countScore (normalized by max visit count)
                const countScore = (visitCount / maxVisitCount) * 50;
                
                // Calculate recencyScore (normalized between oldest and newest)
                const timeDiff = newestVisitDate - oldestVisitDate;
                // Avoid division by zero if all visits are at the same time
                const recencyScore = timeDiff === 0 
                    ? 30 
                    : ((visitDate - oldestVisitDate) / timeDiff) * 30;
                
                // Calculate source score
                const sourceScore = category.source === 'template' ? 20 : 10;
                
                // Calculate total score
                const totalScore = countScore + recencyScore + sourceScore;
                
                return {
                    category: category.category,
                    score: totalScore,
                    visitCount: visitCount,
                    visitDate: category.visitDate
                };
            });
            
            // Sort by score (descending)
            scoredCategories.sort((a, b) => b.score - a.score);
            
            // Select top categories (up to 3)
            const topCategories = scoredCategories.slice(0, 3);
            
            // Get templates for top categories
            const templates = await testRecommendationService.getTemplatesForCategories(
                topCategories.map(c => c.category),
                Math.ceil(limit / topCategories.length)
            );
            
            return {
                templates,
                topCategories: topCategories.map(cat => ({
                    category: cat.category,
                    score: cat.score,
                    weight: cat.visitCount
                })),
                lastUpdated: new Date()
            };
            
        } catch (error) {
            console.error(`Error getting recommendations: ${error.message}`);
            return await testRecommendationService.getDefaultRecommendations(limit);
        }
    },
    
    /**
     * Get templates for a list of categories
     */
    getTemplatesForCategories: async (categories, limitPerCategory = 5) => {
        try {
            // Get templates for each category
            const templatePromises = categories.map(category => 
                Template.find({ category, status: { $ne: false } })
                    .sort({ createdAt: -1 })
                    .limit(limitPerCategory)
                    .lean()
            );
            
            const categoryResults = await Promise.all(templatePromises);
            
            // Flatten and interleave the results to ensure diversity
            const templates = [];
            let maxIndex = 0;
            
            // Find the maximum length among all category results
            categoryResults.forEach(result => {
                if (result.length > maxIndex) {
                    maxIndex = result.length;
                }
            });
            
            // Interleave results from different categories
            for (let i = 0; i < maxIndex; i++) {
                for (let j = 0; j < categoryResults.length; j++) {
                    if (categoryResults[j] && categoryResults[j][i]) {
                        templates.push(categoryResults[j][i]);
                    }
                }
            }
            
            return templates;
            
        } catch (error) {
            console.error(`Error getting templates for categories: ${error.message}`);
            return [];
        }
    },
    
    /**
     * Get default recommendations for new users or fallback
     */
    getDefaultRecommendations: async (limit = 10) => {
        try {
            // Get top/trending templates
            const templates = await Template.find({ status: { $ne: false } })
                .sort({ createdAt: -1 })
                .limit(limit)
                .lean();
            
            return {
                templates,
                topCategories: [],
                isDefault: true,
                lastUpdated: new Date()
            };
            
        } catch (error) {
            console.error(`Error getting default recommendations: ${error.message}`);
            return {
                templates: [],
                topCategories: [],
                isDefault: true,
                error: true
            };
        }
    }
};

// Connect to MongoDB
async function connectToMongoDB() {
    try {
        await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('Connected to MongoDB');
    } catch (error) {
        console.error('Error connecting to MongoDB:', error);
        process.exit(1);
    }
}

// User profile types to analyze
const USER_TYPES = [
    'Balanced',
    'BirthdayFan',
    'WeddingPlanner',
    'CulturalEnthusiast',
    'Occasional'
];

// Get a sample user for each profile type
async function getSampleUsers() {
    const sampleUsers = {};
    
    // First get all users
    const allUsers = await User.find({}).lean();
    console.log(`Found ${allUsers.length} total users in database`);
    
    // Display a sample of deviceIds to debug
    console.log("Sample deviceIds:");
    allUsers.slice(0, 3).forEach(user => {
        console.log(`- ${user.deviceId}`);
    });
    
    // Alternative approach - analyze each user's category distribution
    // and classify them into user types based on their behavior
    for (const user of allUsers) {
        // Skip users with too few interactions
        if (!user.categories || user.categories.length < 3) {
            continue;
        }
        
        // Calculate category distribution
        const distribution = {};
        let totalVisits = 0;
        
        user.categories.forEach(cat => {
            if (!distribution[cat.category]) {
                distribution[cat.category] = 0;
            }
            distribution[cat.category] += cat.visitCount || 1;
            totalVisits += cat.visitCount || 1;
        });
        
        // Calculate percentages
        const percentages = {};
        Object.keys(distribution).forEach(category => {
            percentages[category] = (distribution[category] / totalVisits) * 100;
        });
        
        // Determine user type based on category distribution
        let userType = 'Balanced';
        
        // BirthdayFan: >40% Birthday
        if (percentages['Birthday'] && percentages['Birthday'] > 40) {
            userType = 'BirthdayFan';
        }
        // WeddingPlanner: >40% Wedding + Anniversary combined
        else if ((percentages['Wedding'] || 0) + (percentages['Anniversary'] || 0) > 40) {
            userType = 'WeddingPlanner';
        }
        // CulturalEnthusiast: >40% Cultural + Religious combined
        else if ((percentages['Cultural'] || 0) + (percentages['Religious'] || 0) > 40) {
            userType = 'CulturalEnthusiast';
        }
        // Occasional: few interactions (already filtered above)
        else if (user.categories.length <= 5) {
            userType = 'Occasional';
        }
        
        // Add user to appropriate type
        if (!sampleUsers[userType]) {
            sampleUsers[userType] = [];
        }
        
        // Limit to 3 users per type
        if (sampleUsers[userType].length < 3) {
            sampleUsers[userType].push(user);
        }
    }
    
    // Log user type counts
    for (const userType of USER_TYPES) {
        const count = sampleUsers[userType] ? sampleUsers[userType].length : 0;
        if (count > 0) {
            console.log(`Found ${count} ${userType} users`);
        } else {
            console.warn(`No users found for type ${userType}`);
        }
    }
    
    return sampleUsers;
}

// Analyze category distribution in a user's interactions
async function analyzeCategoryDistribution(user) {
    const distribution = {};
    let totalVisits = 0;
    
    // Calculate visit distribution by category
    user.categories.forEach(categoryVisit => {
        const { category, visitCount } = categoryVisit;
        if (!distribution[category]) {
            distribution[category] = 0;
        }
        distribution[category] += visitCount || 1;
        totalVisits += visitCount || 1;
    });
    
    // Convert to percentages
    const percentages = {};
    Object.keys(distribution).forEach(category => {
        percentages[category] = Math.round((distribution[category] / totalVisits) * 100);
    });
    
    return {
        distribution,
        percentages,
        totalVisits
    };
}

// Test recommendation service with a specific user
async function testRecommendationsForUser(user) {
    console.log(`\nTesting recommendations for user: ${user.deviceId.substring(0, 15)}...`);
    
    // Get user's category distribution
    const categoryAnalysis = await analyzeCategoryDistribution(user);
    console.log(`User has ${user.categories.length} category interactions and ${categoryAnalysis.totalVisits} total visits`);
    console.log('Category distribution (%):');
    
    // Sort categories by percentage
    const sortedCategories = Object.entries(categoryAnalysis.percentages)
        .sort((a, b) => b[1] - a[1])
        .map(([category, percentage]) => `${category}: ${percentage}%`);
    
    console.log(sortedCategories.join(', '));
    
    // Get recommendations using the service
    console.log('\nFetching recommendations...');
    const recommendations = await testRecommendationService.getRecommendationsForUser(user.deviceId, 10);
    
    if (!recommendations || !recommendations.templates || recommendations.templates.length === 0) {
        console.log('No recommendations returned');
        return;
    }
    
    console.log(`Received ${recommendations.templates.length} recommendations`);
    
    // Analyze recommendation distribution by category
    const recDistribution = {};
    recommendations.templates.forEach(rec => {
        if (!recDistribution[rec.category]) {
            recDistribution[rec.category] = 0;
        }
        recDistribution[rec.category]++;
    });
    
    // Calculate percentage of each category in recommendations
    const recPercentages = {};
    Object.keys(recDistribution).forEach(category => {
        recPercentages[category] = Math.round((recDistribution[category] / recommendations.templates.length) * 100);
    });
    
    // Display recommendation distribution
    console.log('\nRecommendation distribution by category:');
    
    const sortedRecCategories = Object.entries(recPercentages)
        .sort((a, b) => b[1] - a[1])
        .map(([category, percentage]) => `${category}: ${percentage}%`);
    
    console.log(sortedRecCategories.join(', '));
    
    // Analyze alignment between user preferences and recommendations
    console.log('\nAlignment analysis:');
    
    // Calculate correlation between user preferences and recommendations
    let alignmentScore = 0;
    let alignmentDetails = [];
    
    Object.keys(categoryAnalysis.percentages).forEach(category => {
        const userPercentage = categoryAnalysis.percentages[category] || 0;
        const recPercentage = recPercentages[category] || 0;
        
        // Calculate similarity between percentages (0-100)
        const similarity = 100 - Math.abs(userPercentage - recPercentage);
        
        // Add weighted contribution to overall score
        alignmentScore += similarity * (userPercentage / 100);
        
        alignmentDetails.push({
            category,
            userPercentage,
            recPercentage,
            similarity
        });
    });
    
    // Normalize alignment score (0-100)
    alignmentScore = Math.round(alignmentScore);
    
    console.log(`Overall alignment score: ${alignmentScore}/100`);
    
    // Print detailed alignment for top categories
    console.log('\nTop category alignment:');
    alignmentDetails
        .sort((a, b) => b.userPercentage - a.userPercentage)
        .slice(0, 3)
        .forEach(detail => {
            console.log(`${detail.category}: User ${detail.userPercentage}% vs Rec ${detail.recPercentage}% (Similarity: ${detail.similarity}%)`);
        });
    
    return {
        userId: user.deviceId,
        recommendations: recommendations.templates,
        alignmentScore,
        categoryDistribution: categoryAnalysis.percentages,
        recommendationDistribution: recPercentages
    };
}

// Test recommendations for all user types
async function testAllUserTypes() {
    const users = await getSampleUsers();
    const results = {};
    
    for (const userType of USER_TYPES) {
        if (users[userType] && users[userType].length > 0) {
            console.log(`\n======= Testing ${userType} Users =======`);
            
            const typeResults = [];
            for (const user of users[userType]) {
                const result = await testRecommendationsForUser(user);
                if (result) {
                    typeResults.push(result);
                }
            }
            
            // Calculate average alignment score for this user type
            if (typeResults.length > 0) {
                const avgScore = Math.round(
                    typeResults.reduce((sum, r) => sum + r.alignmentScore, 0) / typeResults.length
                );
                console.log(`\nAverage alignment score for ${userType} users: ${avgScore}/100`);
                
                results[userType] = {
                    averageAlignmentScore: avgScore,
                    userResults: typeResults
                };
            }
        }
    }
    
    return results;
}

// Compare recommendation performance across user types
function compareUserTypePerformance(results) {
    console.log('\n======= Performance Comparison =======');
    
    // Sort user types by average alignment score
    const sortedTypes = Object.entries(results)
        .sort((a, b) => b[1].averageAlignmentScore - a[1].averageAlignmentScore)
        .map(([type, data]) => ({ 
            type, 
            score: data.averageAlignmentScore,
            sampleSize: data.userResults.length
        }));
    
    console.log('Alignment scores by user type:');
    sortedTypes.forEach((type, index) => {
        console.log(`${index + 1}. ${type.type}: ${type.score}/100 (${type.sampleSize} users tested)`);
    });
    
    // Calculate overall performance
    if (sortedTypes.length > 0) {
        const overallScore = Math.round(
            sortedTypes.reduce((sum, type) => sum + type.score, 0) / sortedTypes.length
        );
        console.log(`\nOverall recommendation performance: ${overallScore}/100`);
        
        // Provide recommendations for improvement
        if (overallScore < 70) {
            console.log('\nRecommendations for improvement:');
            console.log('- Consider adjusting category scoring weights in recommendationService');
            console.log('- Increase diversity of recommendations for some user types');
            console.log('- Improve handling of users with limited interaction history');
        } else {
            console.log('\nThe recommendation system is performing well overall!');
        }
    }
}

// Main function
async function main() {
    try {
        await connectToMongoDB();
        
        // Verify necessary data exists
        const templateCount = await Template.countDocuments();
        const userCount = await User.countDocuments();
        
        console.log(`Database contains ${templateCount} templates and ${userCount} users`);
        
        if (templateCount === 0 || userCount === 0) {
            console.error('Missing templates or users. Please run the data generation scripts first.');
            process.exit(1);
        }
        
        // Run tests for all user types
        const results = await testAllUserTypes();
        
        // Compare performance across user types
        compareUserTypePerformance(results);
        
        console.log('\nRecommendation testing completed successfully!');
    } catch (error) {
        console.error('Error in recommendation testing:', error);
    } finally {
        // Close the database connection
        mongoose.connection.close();
        console.log('MongoDB connection closed');
    }
}

// Run the main function
main().catch(error => {
    console.error('Unhandled error:', error);
    process.exit(1);
}); 