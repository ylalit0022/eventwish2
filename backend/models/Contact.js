const mongoose = require('mongoose');

const contactSchema = new mongoose.Schema({
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

// Ensure only one active contact content
contactSchema.pre('save', async function(next) {
    if (this.isActive) {
        await this.constructor.updateMany(
            { _id: { $ne: this._id } },
            { isActive: false }
        );
    }
    next();
});

// Get active contact content
contactSchema.statics.getActive = async function() {
    return await this.findOne({ isActive: true });
};

const Contact = mongoose.model('Contact', contactSchema);

module.exports = Contact; 