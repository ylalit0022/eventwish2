const mongoose = require('mongoose');

const aboutSchema = new mongoose.Schema({
    title: {
        type: String,
        required: true,
        trim: true
    },
    htmlCode: {
        type: String,
        required: true
    },
    isActive: {
        type: Boolean,
        default: true
    }
}, {
    timestamps: true
});

// Ensure only one active about content
aboutSchema.pre('save', async function(next) {
    if (this.isActive) {
        await this.constructor.updateMany(
            { _id: { $ne: this._id } },
            { isActive: false }
        );
    }
    next();
});

// Get active about content
aboutSchema.statics.getActive = async function() {
    return await this.findOne({ isActive: true });
};

const About = mongoose.model('About', aboutSchema);

module.exports = About; 