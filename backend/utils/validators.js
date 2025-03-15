/**
 * Validators
 * 
 * This module provides validation functions for various data types and formats.
 */

const mongoose = require('mongoose');

/**
 * Validates if a string is a valid MongoDB ObjectId
 * @param {string} id - The ID to validate
 * @returns {boolean} True if valid, false otherwise
 */
const validateObjectId = (id) => {
  if (!id) return false;
  return mongoose.Types.ObjectId.isValid(id);
};

/**
 * Validates if a string is a valid email address
 * @param {string} email - The email to validate
 * @returns {boolean} True if valid, false otherwise
 */
const validateEmail = (email) => {
  if (!email) return false;
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

/**
 * Validates if a string is a valid URL
 * @param {string} url - The URL to validate
 * @returns {boolean} True if valid, false otherwise
 */
const validateUrl = (url) => {
  if (!url) return false;
  try {
    new URL(url);
    return true;
  } catch (error) {
    return false;
  }
};

/**
 * Validates if a value is a valid number
 * @param {*} value - The value to validate
 * @returns {boolean} True if valid, false otherwise
 */
const validateNumber = (value) => {
  if (value === undefined || value === null) return false;
  return !isNaN(parseFloat(value)) && isFinite(value);
};

/**
 * Validates if a value is a valid integer
 * @param {*} value - The value to validate
 * @returns {boolean} True if valid, false otherwise
 */
const validateInteger = (value) => {
  if (value === undefined || value === null) return false;
  return Number.isInteger(Number(value));
};

/**
 * Validates if a value is a valid boolean
 * @param {*} value - The value to validate
 * @returns {boolean} True if valid, false otherwise
 */
const validateBoolean = (value) => {
  return typeof value === 'boolean' || value === 'true' || value === 'false';
};

/**
 * Validates if a value is a valid date
 * @param {*} value - The value to validate
 * @returns {boolean} True if valid, false otherwise
 */
const validateDate = (value) => {
  if (!value) return false;
  const date = new Date(value);
  return !isNaN(date.getTime());
};

/**
 * Validates if a value is a valid JSON string
 * @param {string} value - The value to validate
 * @returns {boolean} True if valid, false otherwise
 */
const validateJson = (value) => {
  if (!value) return false;
  try {
    JSON.parse(value);
    return true;
  } catch (error) {
    return false;
  }
};

module.exports = {
  validateObjectId,
  validateEmail,
  validateUrl,
  validateNumber,
  validateInteger,
  validateBoolean,
  validateDate,
  validateJson
}; 