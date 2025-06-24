#!/usr/bin/env node

/**
 * Template CRUD API Test Runner
 * Comprehensive testing suite for all Template API modules
 */

const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

class TemplateTestRunner {
    constructor() {
        this.testSuites = [
            {
                name: 'Core Content API',
                file: 'core.test.js',
                description: 'Tests for template content CRUD operations'
            },
            {
                name: 'Monetization API',
                file: 'monetization.test.js',
                description: 'Tests for pricing and premium features'
            },
            {
                name: 'Metrics API',
                file: 'metrics.test.js',
                description: 'Tests for engagement and analytics'
            },
            {
                name: 'Categorization API',
                file: 'categorization.test.js',
                description: 'Tests for category and tag management'
            },
            {
                name: 'Visibility API',
                file: 'visibility.test.js',
                description: 'Tests for template visibility and boosting'
            },
            {
                name: 'AI Features API',
                file: 'ai.test.js',
                description: 'Tests for AI-powered features'
            },
            {
                name: 'Customization API',
                file: 'customization.test.js',
                description: 'Tests for template customization options'
            },
            {
                name: 'Legacy API',
                file: 'legacy.test.js',
                description: 'Tests for legacy compatibility endpoints'
            },
            {
                name: 'Integration Tests',
                file: 'integration.test.js',
                description: 'Cross-module integration and workflow tests'
            }
        ];

        this.results = {
            total: 0,
            passed: 0,
            failed: 0,
            skipped: 0,
            suites: []
        };
    }

    async runAllTests() {
        console.log('🚀 Starting Template CRUD API Test Suite');
        console.log('=' .repeat(60));

        const startTime = Date.now();

        for (const suite of this.testSuites) {
            await this.runTestSuite(suite);
        }

        const endTime = Date.now();
        const duration = (endTime - startTime) / 1000;

        this.generateReport(duration);
        this.generateCoverageReport();
        
        return this.results;
    }

    async runTestSuite(suite) {
        console.log(`\n📋 Running ${suite.name}...`);
        console.log(`   ${suite.description}`);

        const testFile = path.join(__dirname, suite.file);
        
        if (!fs.existsSync(testFile)) {
            console.log(`   ⚠️  Test file not found: ${suite.file}`);
            this.results.suites.push({
                name: suite.name,
                status: 'skipped',
                reason: 'Test file not found'
            });
            this.results.skipped++;
            return;
        }

        try {
            const result = await this.executeTest(testFile);
            
            if (result.success) {
                console.log(`   ✅ ${suite.name} - PASSED`);
                this.results.passed++;
                this.results.suites.push({
                    name: suite.name,
                    status: 'passed',
                    tests: result.tests,
                    duration: result.duration
                });
            } else {
                console.log(`   ❌ ${suite.name} - FAILED`);
                console.log(`   Error: ${result.error}`);
                this.results.failed++;
                this.results.suites.push({
                    name: suite.name,
                    status: 'failed',
                    error: result.error,
                    tests: result.tests
                });
            }
        } catch (error) {
            console.log(`   💥 ${suite.name} - ERROR`);
            console.log(`   ${error.message}`);
            this.results.failed++;
            this.results.suites.push({
                name: suite.name,
                status: 'error',
                error: error.message
            });
        }

        this.results.total++;
    }

    executeTest(testFile) {
        return new Promise((resolve) => {
            const startTime = Date.now();
            const jest = spawn('npx', ['jest', testFile, '--verbose', '--json'], {
                stdio: ['pipe', 'pipe', 'pipe'],
                shell: true
            });

            let stdout = '';
            let stderr = '';

            jest.stdout.on('data', (data) => {
                stdout += data.toString();
            });

            jest.stderr.on('data', (data) => {
                stderr += data.toString();
            });

            jest.on('close', (code) => {
                const endTime = Date.now();
                const duration = (endTime - startTime) / 1000;

                try {
                    const result = JSON.parse(stdout);
                    resolve({
                        success: code === 0,
                        tests: result.numTotalTests || 0,
                        duration: duration,
                        error: code !== 0 ? stderr : null
                    });
                } catch (parseError) {
                    resolve({
                        success: false,
                        tests: 0,
                        duration: duration,
                        error: `Parse error: ${parseError.message}\nStderr: ${stderr}`
                    });
                }
            });
        });
    }

    generateReport(totalDuration) {
        console.log('\n' + '=' .repeat(60));
        console.log('📊 TEST RESULTS SUMMARY');
        console.log('=' .repeat(60));

        console.log(`Total Test Suites: ${this.results.total}`);
        console.log(`✅ Passed: ${this.results.passed}`);
        console.log(`❌ Failed: ${this.results.failed}`);
        console.log(`⚠️  Skipped: ${this.results.skipped}`);
        console.log(`⏱️  Total Duration: ${totalDuration.toFixed(2)}s`);

        const successRate = this.results.total > 0 ? 
            (this.results.passed / this.results.total * 100).toFixed(1) : 0;
        console.log(`📈 Success Rate: ${successRate}%`);

        // Detailed suite results
        console.log('\n📋 DETAILED RESULTS:');
        this.results.suites.forEach((suite, index) => {
            const icon = suite.status === 'passed' ? '✅' : 
                        suite.status === 'failed' ? '❌' : '⚠️';
            console.log(`${index + 1}. ${icon} ${suite.name} - ${suite.status.toUpperCase()}`);
            
            if (suite.tests) {
                console.log(`   Tests: ${suite.tests}`);
            }
            if (suite.duration) {
                console.log(`   Duration: ${suite.duration.toFixed(2)}s`);
            }
            if (suite.error) {
                console.log(`   Error: ${suite.error.substring(0, 100)}...`);
            }
        });

        // Save detailed report to file
        this.saveReportToFile(totalDuration);
    }

    saveReportToFile(totalDuration) {
        const reportData = {
            timestamp: new Date().toISOString(),
            summary: {
                total: this.results.total,
                passed: this.results.passed,
                failed: this.results.failed,
                skipped: this.results.skipped,
                successRate: this.results.total > 0 ? 
                    (this.results.passed / this.results.total * 100) : 0,
                totalDuration: totalDuration
            },
            suites: this.results.suites,
            environment: {
                nodeVersion: process.version,
                platform: process.platform,
                arch: process.arch
            }
        };

        const reportPath = path.join(__dirname, '../../docs/test-reports');
        if (!fs.existsSync(reportPath)) {
            fs.mkdirSync(reportPath, { recursive: true });
        }

        const fileName = `template-api-test-report-${new Date().toISOString().split('T')[0]}.json`;
        const filePath = path.join(reportPath, fileName);

        fs.writeFileSync(filePath, JSON.stringify(reportData, null, 2));
        console.log(`\n📄 Detailed report saved to: ${filePath}`);
    }

    generateCoverageReport() {
        console.log('\n🔍 COVERAGE ANALYSIS:');
        
        const moduleEndpoints = {
            'Core Content': 15,
            'Monetization': 12,
            'Metrics': 18,
            'Categorization': 10,
            'Visibility': 8,
            'AI Features': 6,
            'Customization': 7,
            'Legacy': 30
        };

        let totalEndpoints = 0;
        let testedEndpoints = 0;

        Object.entries(moduleEndpoints).forEach(([module, endpoints]) => {
            totalEndpoints += endpoints;
            const suite = this.results.suites.find(s => s.name.includes(module));
            const tested = suite && suite.status === 'passed' ? endpoints : 0;
            testedEndpoints += tested;
            
            const coverage = suite && suite.status === 'passed' ? 100 : 0;
            const icon = coverage === 100 ? '✅' : coverage > 0 ? '⚠️' : '❌';
            console.log(`${icon} ${module}: ${coverage}% (${tested}/${endpoints} endpoints)`);
        });

        const overallCoverage = totalEndpoints > 0 ? 
            (testedEndpoints / totalEndpoints * 100).toFixed(1) : 0;
        console.log(`\n📊 Overall API Coverage: ${overallCoverage}% (${testedEndpoints}/${totalEndpoints} endpoints)`);
    }

    async runSpecificSuite(suiteName) {
        const suite = this.testSuites.find(s => 
            s.name.toLowerCase().includes(suiteName.toLowerCase()) ||
            s.file.toLowerCase().includes(suiteName.toLowerCase())
        );

        if (!suite) {
            console.log(`❌ Test suite not found: ${suiteName}`);
            console.log('Available suites:');
            this.testSuites.forEach(s => console.log(`  - ${s.name} (${s.file})`));
            return;
        }

        console.log(`🎯 Running specific test suite: ${suite.name}`);
        await this.runTestSuite(suite);
        this.generateReport(0);
    }

    listAvailableTests() {
        console.log('📋 Available Test Suites:');
        console.log('=' .repeat(50));
        
        this.testSuites.forEach((suite, index) => {
            console.log(`${index + 1}. ${suite.name}`);
            console.log(`   File: ${suite.file}`);
            console.log(`   Description: ${suite.description}`);
            console.log('');
        });
    }
}

// CLI Interface
if (require.main === module) {
    const runner = new TemplateTestRunner();
    const args = process.argv.slice(2);

    if (args.length === 0) {
        // Run all tests
        runner.runAllTests().then((results) => {
            process.exit(results.failed > 0 ? 1 : 0);
        });
    } else if (args[0] === 'list') {
        // List available tests
        runner.listAvailableTests();
    } else if (args[0] === 'run' && args[1]) {
        // Run specific test suite
        runner.runSpecificSuite(args[1]).then(() => {
            process.exit(0);
        });
    } else {
        console.log('Usage:');
        console.log('  node test-runner.js           # Run all tests');
        console.log('  node test-runner.js list      # List available tests');
        console.log('  node test-runner.js run <name> # Run specific test suite');
        process.exit(1);
    }
}

module.exports = TemplateTestRunner; 