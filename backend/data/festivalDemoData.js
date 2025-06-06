const mongoose = require('mongoose');

// Sample category icons (assuming these IDs exist in your CategoryIcon collection)
const sampleCategoryIcons = [
    new mongoose.Types.ObjectId(), // Religious
    new mongoose.Types.ObjectId(), // National
    new mongoose.Types.ObjectId(), // Cultural
    new mongoose.Types.ObjectId(), // Personal
    new mongoose.Types.ObjectId()  // International
];

// Sample template IDs (assuming these exist in your Template collection)
const sampleTemplates = [
    new mongoose.Types.ObjectId(),
    new mongoose.Types.ObjectId(),
    new mongoose.Types.ObjectId()
];

const festivalDemoData = [
    {
        name: "New Year's Day",
        date: new Date("2026-01-01"),
        description: "The first day of the year in the modern Gregorian calendar.",
        category: "International",
        categoryIcon: sampleCategoryIcons[4],
        imageUrl: "https://example.com/images/new-year.jpg",
        templates: [sampleTemplates[0], sampleTemplates[1]],
        isActive: true
    },
    {
        name: "Christmas",
        date: new Date("2025-12-25"),
        description: "Annual festival commemorating the birth of Jesus Christ.",
        category: "Religious",
        categoryIcon: sampleCategoryIcons[0],
        imageUrl: "https://example.com/images/christmas.jpg",
        templates: [sampleTemplates[1], sampleTemplates[2]],
        isActive: true
    },
    {
        name: "Independence Day",
        date: new Date("2025-07-04"),
        description: "Celebration of national independence.",
        category: "National",
        categoryIcon: sampleCategoryIcons[1],
        imageUrl: "https://example.com/images/independence.jpg",
        templates: [sampleTemplates[0]],
        isActive: true
    },
    {
        name: "Diwali",
        date: new Date("2025-11-01"),
        description: "Festival of lights celebrated in Indian culture.",
        category: "Cultural",
        categoryIcon: sampleCategoryIcons[2],
        imageUrl: "https://example.com/images/diwali.jpg",
        templates: [sampleTemplates[1]],
        isActive: true
    },
    {
        name: "Mother's Day",
        date: new Date("2026-05-10"),
        description: "Celebration honoring mothers and motherhood.",
        category: "Personal",
        categoryIcon: sampleCategoryIcons[3],
        imageUrl: "https://example.com/images/mothers-day.jpg",
        templates: [sampleTemplates[2]],
        isActive: true
    },
    {
        name: "Easter",
        date: new Date("2026-04-05"),
        description: "Christian festival celebrating the resurrection of Jesus.",
        category: "Religious",
        categoryIcon: sampleCategoryIcons[0],
        imageUrl: "https://example.com/images/easter.jpg",
        templates: [sampleTemplates[0], sampleTemplates[2]],
        isActive: true
    },
    {
        name: "Valentine's Day",
        date: new Date("2026-02-14"),
        description: "Day celebrating love and affection between intimate companions.",
        category: "Personal",
        categoryIcon: sampleCategoryIcons[3],
        imageUrl: "https://example.com/images/valentines.jpg",
        templates: [sampleTemplates[1]],
        isActive: true
    }
];

module.exports = festivalDemoData; 