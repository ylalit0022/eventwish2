/**
 * Run all template ID tests sequentially
 * 
 * This script executes all template ID tests to ensure:
 * 1. Template objects have proper id fields
 * 2. API responses include correct id fields
 * 3. Template IDs are Android-compatible
 */

const { execSync } = require('child_process');
const path = require('path');

// Test scripts to run
const testScripts = [
  'testTemplateId.js',
  'testTemplateApiResponse.js',
  'testAndroidCompatibility.js'
];

// Run all tests
async function runAllTests() {
  console.log('===== RUNNING ALL TEMPLATE ID TESTS =====\n');
  
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
    console.log('🎉 All tests passed successfully!');
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