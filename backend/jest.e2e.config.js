/**
 * Jest Configuration for End-to-End Tests
 */

module.exports = {
  testEnvironment: 'node',
  testTimeout: 60000,
  testMatch: [
    '**/tests/e2e/**/*.test.js',
  ],
  testPathIgnorePatterns: [
    '/node_modules/',
    '/coverage/',
  ],
  setupFilesAfterEnv: [
    '<rootDir>/tests/e2e/setup.js',
  ],
  verbose: true,
  bail: 1,
  forceExit: true,
  detectOpenHandles: true,
};
