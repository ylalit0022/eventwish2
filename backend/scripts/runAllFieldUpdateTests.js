/**
 * Run all field update tests
 * 
 * This script runs all the tests for field updates:
 * 1. Template field updates
 * 2. Template API updates
 * 3. Client compatibility
 * 4. SponsoredAd field updates
 */

const { execSync } = require('child_process');
const path = require('path');

// Test scripts to run
const testScripts = [
  'testTemplateFieldUpdates.js',
  'testTemplateApiUpdates.js',
  'testClientCompatibility.js',
  'testSponsoredAdFieldUpdates.js'
];

// Run all tests
async function runAllTests() {
  console.log('===== RUNNING ALL FIELD UPDATE TESTS =====\n');
  
  let allPassed = true;
  
  for (const script of testScripts) {
    const scriptPath = path.join(__dirname, script);
    console.log(`\n===== RUNNING ${script} =====\n`);
    
    try {
      // Run script and capture output
      const output = execSync(`node ${scriptPath}`, { stdio: 'inherit' });
      
      console.log(`\n✅ ${script} passed\n`);
    } catch (error) {
      console.error(`\n❌ ${script} failed with error code: ${error.status}`);
      allPassed = false;
    }
    
    console.log('======================================\n');
  }
  
  // Show final results
  if (allPassed) {
    console.log('🎉 All field update tests passed successfully!');
  } else {
    console.log('⚠️ Some tests failed. See output above for details.');
    process.exit(1); // Exit with error code
  }
}

// Run the tests
runAllTests().catch(error => {
  console.error('Error running tests:', error);
  process.exit(1);
}); 