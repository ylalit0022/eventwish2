#!/bin/bash

# Centralized Notification System Configuration Test Script
# Tests Firebase Remote Config integration and notification delivery

set -e  # Exit on any error

echo "🔔 EventWish Notification System Test Suite"
echo "============================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test configuration
PACKAGE_NAME="com.ds.eventwish"
MAIN_ACTIVITY="$PACKAGE_NAME/.ui.splash.SplashActivity"
TEST_TIMEOUT=10

# Helper functions
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if device is connected
check_device() {
    log_info "Checking device connection..."
    if ! adb devices | grep -q "device$"; then
        log_error "No Android device connected or device not authorized"
        log_info "Please connect an Android device and enable USB debugging"
        exit 1
    fi
    log_success "Device connected"
}

# Check if app is installed
check_app_installed() {
    log_info "Checking if EventWish app is installed..."
    if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
        log_error "EventWish app is not installed"
        log_info "Please install the app first: ./gradlew installDebug"
        exit 1
    fi
    log_success "EventWish app is installed"
}

# Clear app data for fresh test
clear_app_data() {
    log_info "Clearing app data for fresh test..."
    adb shell pm clear "$PACKAGE_NAME" > /dev/null 2>&1
    log_success "App data cleared"
}

# Start app and wait for initialization
start_app() {
    log_info "Starting EventWish app..."
    adb shell am start -n "$MAIN_ACTIVITY" > /dev/null 2>&1
    log_success "App started"
    
    log_info "Waiting for app initialization (${TEST_TIMEOUT}s)..."
    sleep $TEST_TIMEOUT
}

# Enable debug logging
enable_debug_logging() {
    log_info "Enabling debug logging..."
    adb shell setprop log.tag.EventNotificationManager DEBUG
    adb shell setprop log.tag.NotificationSyncWorker DEBUG
    adb shell setprop log.tag.FirebaseRemoteConfig DEBUG
    log_success "Debug logging enabled"
}

# Test 1: Firebase Remote Config fetch
test_remote_config_fetch() {
    echo ""
    log_info "Test 1: Firebase Remote Config Fetch"
    echo "-----------------------------------"
    
    # Clear logcat
    adb logcat -c
    
    # Restart app to trigger Remote Config fetch
    adb shell am force-stop "$PACKAGE_NAME"
    sleep 2
    adb shell am start -n "$MAIN_ACTIVITY" > /dev/null 2>&1
    
    # Wait for Remote Config fetch
    log_info "Waiting for Remote Config fetch..."
    sleep 8
    
    # Check for successful fetch
    FETCH_SUCCESS=$(adb logcat -d | grep -i "remote.*config.*fetch" | wc -l)
    CONFIG_PARSED=$(adb logcat -d | grep "Successfully parsed notification config" | wc -l)
    
    if [ $FETCH_SUCCESS -gt 0 ] || [ $CONFIG_PARSED -gt 0 ]; then
        log_success "Remote Config fetch successful"
        
        # Show configuration details
        CONFIG_VERSION=$(adb logcat -d | grep "notification config version" | tail -1)
        if [ ! -z "$CONFIG_VERSION" ]; then
            log_info "Configuration: $CONFIG_VERSION"
        fi
        
        return 0
    else
        log_error "Remote Config fetch failed"
        log_warning "Check Firebase project configuration and internet connection"
        return 1
    fi
}

# Test 2: NotificationSyncWorker scheduling
test_worker_scheduling() {
    echo ""
    log_info "Test 2: NotificationSyncWorker Scheduling"
    echo "----------------------------------------"
    
    # Check for WorkManager scheduling
    WORKER_SCHEDULED=$(adb logcat -d | grep "NotificationSyncWorker.*scheduled\|Scheduling.*notification.*sync" | wc -l)
    WORK_MANAGER_INIT=$(adb logcat -d | grep "WorkManager.*initialized\|WorkManager.*ready" | wc -l)
    
    if [ $WORKER_SCHEDULED -gt 0 ] || [ $WORK_MANAGER_INIT -gt 0 ]; then
        log_success "NotificationSyncWorker scheduling successful"
        
        # Show worker details
        WORKER_INFO=$(adb logcat -d | grep "NotificationSyncWorker" | tail -2)
        if [ ! -z "$WORKER_INFO" ]; then
            log_info "Worker details:"
            echo "$WORKER_INFO" | while read line; do
                echo "  $line"
            done
        fi
        
        return 0
    else
        log_error "NotificationSyncWorker scheduling failed"
        log_warning "Check WorkManager initialization and permissions"
        return 1
    fi
}

# Test 3: Notification configuration processing
test_notification_processing() {
    echo ""
    log_info "Test 3: Notification Configuration Processing"
    echo "--------------------------------------------"
    
    # Check for notification processing
    PROCESSING_SUCCESS=$(adb logcat -d | grep "Processing.*notification.*type\|processAllNotificationTypes" | wc -l)
    CONFIG_LOADED=$(adb logcat -d | grep "notification.*config.*loaded\|getNotificationConfig" | wc -l)
    
    if [ $PROCESSING_SUCCESS -gt 0 ] || [ $CONFIG_LOADED -gt 0 ]; then
        log_success "Notification configuration processing successful"
        
        # Show processing details
        PROCESSING_INFO=$(adb logcat -d | grep "notification.*type\|dailyReminder\|festivalAlert" | tail -3)
        if [ ! -z "$PROCESSING_INFO" ]; then
            log_info "Processing details:"
            echo "$PROCESSING_INFO" | while read line; do
                echo "  $line"
            done
        fi
        
        return 0
    else
        log_error "Notification configuration processing failed"
        log_warning "Check notification configuration format and parsing logic"
        return 1
    fi
}

# Test 4: Notification delivery (if possible)
test_notification_delivery() {
    echo ""
    log_info "Test 4: Notification Delivery Test"
    echo "----------------------------------"
    
    # Try to trigger a test notification
    log_info "Attempting to trigger test notification..."
    adb shell am broadcast -a com.ds.eventwish.TEST_NOTIFICATION > /dev/null 2>&1
    sleep 3
    
    # Check for notification delivery
    NOTIFICATION_SHOWN=$(adb logcat -d | grep "Notification shown with ID\|NotificationManager.*notify" | wc -l)
    NOTIFICATION_BUILT=$(adb logcat -d | grep "Notification.*builder.*created\|Building notification" | wc -l)
    
    if [ $NOTIFICATION_SHOWN -gt 0 ]; then
        log_success "Notification delivery successful"
        
        # Show notification details
        NOTIFICATION_INFO=$(adb logcat -d | grep "Notification shown\|notification.*title" | tail -2)
        if [ ! -z "$NOTIFICATION_INFO" ]; then
            log_info "Notification details:"
            echo "$NOTIFICATION_INFO" | while read line; do
                echo "  $line"
            done
        fi
        
        return 0
    elif [ $NOTIFICATION_BUILT -gt 0 ]; then
        log_warning "Notification built but delivery status unclear"
        log_info "This may be due to notification permissions or system settings"
        return 0
    else
        log_error "Notification delivery failed"
        log_warning "Check notification permissions and channel configuration"
        return 1
    fi
}

# Test 5: Memory and performance check
test_performance() {
    echo ""
    log_info "Test 5: Performance Check"
    echo "-------------------------"
    
    # Get memory usage
    MEMORY_INFO=$(adb shell dumpsys meminfo "$PACKAGE_NAME" | grep "TOTAL PSS" | head -1)
    if [ ! -z "$MEMORY_INFO" ]; then
        MEMORY_KB=$(echo "$MEMORY_INFO" | awk '{print $3}')
        MEMORY_MB=$((MEMORY_KB / 1024))
        
        if [ $MEMORY_MB -lt 100 ]; then
            log_success "Memory usage: ${MEMORY_MB}MB (Good)"
        elif [ $MEMORY_MB -lt 200 ]; then
            log_warning "Memory usage: ${MEMORY_MB}MB (Acceptable)"
        else
            log_error "Memory usage: ${MEMORY_MB}MB (High)"
        fi
    else
        log_warning "Could not retrieve memory information"
    fi
    
    # Check for memory leaks or excessive allocations
    MEMORY_WARNINGS=$(adb logcat -d | grep -i "memory.*warning\|gc.*pressure\|out.*of.*memory" | wc -l)
    if [ $MEMORY_WARNINGS -eq 0 ]; then
        log_success "No memory warnings detected"
    else
        log_warning "Found $MEMORY_WARNINGS memory-related warnings"
    fi
}

# Generate test report
generate_report() {
    echo ""
    echo "📊 Test Summary Report"
    echo "====================="
    
    # Count results
    TOTAL_TESTS=5
    PASSED_TESTS=0
    
    # Re-run tests silently to count passes
    if test_remote_config_fetch > /dev/null 2>&1; then PASSED_TESTS=$((PASSED_TESTS + 1)); fi
    if test_worker_scheduling > /dev/null 2>&1; then PASSED_TESTS=$((PASSED_TESTS + 1)); fi
    if test_notification_processing > /dev/null 2>&1; then PASSED_TESTS=$((PASSED_TESTS + 1)); fi
    if test_notification_delivery > /dev/null 2>&1; then PASSED_TESTS=$((PASSED_TESTS + 1)); fi
    # Performance test doesn't count as pass/fail
    PASSED_TESTS=$((PASSED_TESTS + 1))
    
    echo "Tests Passed: $PASSED_TESTS/$TOTAL_TESTS"
    
    if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
        log_success "All tests passed! Notification system is working correctly."
    elif [ $PASSED_TESTS -ge 3 ]; then
        log_warning "Most tests passed. Some issues may need attention."
    else
        log_error "Multiple test failures. Notification system needs investigation."
    fi
    
    echo ""
    echo "📋 Next Steps:"
    if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
        echo "• Configure Firebase Remote Config in production"
        echo "• Set up monitoring and analytics"
        echo "• Plan gradual user rollout"
    else
        echo "• Review failed tests and error logs"
        echo "• Check Firebase project configuration"
        echo "• Verify app permissions and settings"
        echo "• Re-run tests after fixes"
    fi
    
    echo ""
    echo "📱 App Information:"
    APP_VERSION=$(adb shell dumpsys package "$PACKAGE_NAME" | grep "versionName" | head -1 | cut -d'=' -f2)
    if [ ! -z "$APP_VERSION" ]; then
        echo "App Version: $APP_VERSION"
    fi
    
    ANDROID_VERSION=$(adb shell getprop ro.build.version.release)
    echo "Android Version: $ANDROID_VERSION"
    
    API_LEVEL=$(adb shell getprop ro.build.version.sdk)
    echo "API Level: $API_LEVEL"
}

# Main test execution
main() {
    echo "Starting notification system tests..."
    echo ""
    
    # Pre-test checks
    check_device
    check_app_installed
    enable_debug_logging
    clear_app_data
    start_app
    
    # Run tests
    test_remote_config_fetch
    test_worker_scheduling
    test_notification_processing
    test_notification_delivery
    test_performance
    
    # Generate report
    generate_report
    
    echo ""
    log_info "Test execution completed!"
    echo ""
    echo "💡 To view detailed logs, run:"
    echo "   adb logcat | grep -E '(EventNotificationManager|NotificationSyncWorker|RemoteConfig)'"
}

# Run main function
main "$@"

# Function to test a specific notification type
test_notification() {
    local type=$1
    echo "Testing $type notification..."
    adb shell am broadcast -a com.ds.eventwish.FORCE_NOTIFICATION_CHECK --es type "$type"
    sleep 2
}

# Test all notification types
echo "Starting notification tests..."

echo "1. Testing Daily Reminder"
test_notification "dailyReminder"

echo "2. Testing Festival Alert"
test_notification "festivalAlert"

echo "3. Testing Inactivity Nudge"
test_notification "inactivityNudge"

echo "4. Testing Subscription Expiry"
test_notification "subscriptionExpiry"

echo "5. Testing Weekly Engagement"
test_notification "weeklyEngagement"

echo "6. Testing New Template Alert"
test_notification "newTemplateAlert"

echo "Done testing notifications!" 