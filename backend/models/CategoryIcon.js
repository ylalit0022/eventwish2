const mongoose = require('mongoose');

const categoryIconSchema = new mongoose.Schema({
    category: {
        type: String,
        required: true,
        unique: true
    },
    categoryIcon: {
        type: String,
        required: true,
        validate: {
            validator: function(v) {
                return /^(http|https):\/\/[^ "]+$/.test(v);
            },
            message: props => `${props.value} is not a valid URL!`
        }
    }
}, { timestamps: true });

module.exports = mongoose.model('CategoryIcon', categoryIconSchema, 'categoryicons');