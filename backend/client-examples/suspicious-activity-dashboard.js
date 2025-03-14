/**
 * Suspicious Activity Dashboard JavaScript
 * 
 * This file contains the JavaScript code for the suspicious activity dashboard.
 * It handles API calls, data processing, and UI updates.
 */

// Configuration
const API_BASE_URL = 'https://api.eventwish.com';
const API_KEY = 'YOUR_API_KEY'; // Replace with your actual API key
const REFRESH_INTERVAL = 60000; // Refresh dashboard every 60 seconds

// DOM Elements
let dashboardStats;
let activityTypeChart;
let activitySeverityChart;
let recentActivitiesList;
let topUsersTable;
let topIpsTable;
let loadingSpinner;
let errorMessage;
let lastUpdatedTime;

// Dashboard Data
let dashboardData = {
  stats: {
    totalActivities: 0,
    criticalSeverity: 0,
    highSeverity: 0,
    fraudRate: 0
  },
  activityByType: {},
  activityBySeverity: {},
  recentActivities: [],
  topSuspiciousUsers: [],
  topSuspiciousIps: []
};

// Initialize the dashboard
document.addEventListener('DOMContentLoaded', () => {
  // Get DOM elements
  dashboardStats = {
    totalActivities: document.getElementById('total-activities'),
    criticalSeverity: document.getElementById('critical-severity'),
    highSeverity: document.getElementById('high-severity'),
    fraudRate: document.getElementById('fraud-rate')
  };
  
  // Chart elements
  const activityTypeChartEl = document.getElementById('activity-type-chart');
  const activitySeverityChartEl = document.getElementById('activity-severity-chart');
  
  // Lists and tables
  recentActivitiesList = document.getElementById('recent-activities-list');
  topUsersTable = document.getElementById('top-users-table').querySelector('tbody');
  topIpsTable = document.getElementById('top-ips-table').querySelector('tbody');
  
  // UI elements
  loadingSpinner = document.getElementById('loading-spinner');
  errorMessage = document.getElementById('error-message');
  lastUpdatedTime = document.getElementById('last-updated-time');
  
  // Initialize charts
  initializeCharts(activityTypeChartEl, activitySeverityChartEl);
  
  // Load initial data
  fetchDashboardData();
  
  // Set up refresh interval
  setInterval(fetchDashboardData, REFRESH_INTERVAL);
  
  // Set up refresh button
  document.getElementById('refresh-button').addEventListener('click', fetchDashboardData);
});

/**
 * Initialize the charts for the dashboard
 */
function initializeCharts(typeChartEl, severityChartEl) {
  // Activity Type Chart
  activityTypeChart = new Chart(typeChartEl, {
    type: 'doughnut',
    data: {
      labels: [],
      datasets: [{
        data: [],
        backgroundColor: [
          '#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF',
          '#FF9F40', '#C9CBCF', '#7CFC00', '#00FFFF', '#FF00FF'
        ]
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      legend: {
        position: 'right'
      },
      title: {
        display: true,
        text: 'Activity by Type'
      }
    }
  });
  
  // Activity Severity Chart
  activitySeverityChart = new Chart(severityChartEl, {
    type: 'bar',
    data: {
      labels: ['Low', 'Medium', 'High', 'Critical'],
      datasets: [{
        label: 'Activities by Severity',
        data: [0, 0, 0, 0],
        backgroundColor: [
          '#4BC0C0', '#FFCE56', '#FF9F40', '#FF6384'
        ]
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        yAxes: [{
          ticks: {
            beginAtZero: true
          }
        }]
      },
      title: {
        display: true,
        text: 'Activity by Severity'
      }
    }
  });
}

/**
 * Fetch dashboard data from the API
 */
async function fetchDashboardData() {
  showLoading(true);
  hideError();
  
  try {
    const response = await fetch(`${API_BASE_URL}/api/suspicious-activity/dashboard`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': API_KEY
      }
    });
    
    if (!response.ok) {
      throw new Error(`API error: ${response.status} ${response.statusText}`);
    }
    
    const data = await response.json();
    
    if (!data.success) {
      throw new Error(data.message || 'Unknown API error');
    }
    
    // Update dashboard data
    dashboardData = data.data;
    
    // Update UI
    updateDashboard();
    
    // Update last updated time
    updateLastUpdatedTime();
  } catch (error) {
    console.error('Error fetching dashboard data:', error);
    showError(error.message);
  } finally {
    showLoading(false);
  }
}

/**
 * Update the dashboard with the latest data
 */
function updateDashboard() {
  // Update stats
  dashboardStats.totalActivities.textContent = dashboardData.stats.totalActivities.toLocaleString();
  dashboardStats.criticalSeverity.textContent = dashboardData.stats.criticalSeverity.toLocaleString();
  dashboardStats.highSeverity.textContent = dashboardData.stats.highSeverity.toLocaleString();
  dashboardStats.fraudRate.textContent = `${dashboardData.stats.fraudRate.toFixed(2)}%`;
  
  // Update charts
  updateActivityTypeChart();
  updateActivitySeverityChart();
  
  // Update lists and tables
  updateRecentActivitiesList();
  updateTopUsersTable();
  updateTopIpsTable();
}

/**
 * Update the activity type chart
 */
function updateActivityTypeChart() {
  const types = Object.keys(dashboardData.activityByType);
  const counts = types.map(type => dashboardData.activityByType[type]);
  
  activityTypeChart.data.labels = types.map(formatActivityType);
  activityTypeChart.data.datasets[0].data = counts;
  activityTypeChart.update();
}

/**
 * Update the activity severity chart
 */
function updateActivitySeverityChart() {
  const severities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  const counts = severities.map(severity => 
    dashboardData.activityBySeverity[severity] || 0
  );
  
  activitySeverityChart.data.datasets[0].data = counts;
  activitySeverityChart.update();
}

/**
 * Update the recent activities list
 */
function updateRecentActivitiesList() {
  recentActivitiesList.innerHTML = '';
  
  if (dashboardData.recentActivities.length === 0) {
    const li = document.createElement('li');
    li.className = 'list-group-item text-center';
    li.textContent = 'No recent activities';
    recentActivitiesList.appendChild(li);
    return;
  }
  
  dashboardData.recentActivities.forEach(activity => {
    const li = document.createElement('li');
    li.className = 'list-group-item';
    
    // Add severity badge
    const badge = document.createElement('span');
    badge.className = `badge badge-${getSeverityClass(activity.severity)} float-right`;
    badge.textContent = activity.severity.toLowerCase();
    li.appendChild(badge);
    
    // Add activity details
    const details = document.createElement('div');
    details.innerHTML = `
      <strong>${formatActivityType(activity.type)}</strong>
      <div class="text-muted small">
        ${activity.entityType}: ${activity.entityId}
        <br>
        ${new Date(activity.timestamp).toLocaleString()}
      </div>
    `;
    li.appendChild(details);
    
    recentActivitiesList.appendChild(li);
  });
}

/**
 * Update the top suspicious users table
 */
function updateTopUsersTable() {
  topUsersTable.innerHTML = '';
  
  if (dashboardData.topSuspiciousUsers.length === 0) {
    const tr = document.createElement('tr');
    tr.innerHTML = '<td colspan="3" class="text-center">No suspicious users</td>';
    topUsersTable.appendChild(tr);
    return;
  }
  
  dashboardData.topSuspiciousUsers.forEach(user => {
    const tr = document.createElement('tr');
    
    // User ID
    const tdId = document.createElement('td');
    tdId.textContent = user.userId;
    tr.appendChild(tdId);
    
    // Reputation score
    const tdScore = document.createElement('td');
    tdScore.innerHTML = `<span class="badge badge-${getReputationClass(user.reputationScore)}">${user.reputationScore}</span>`;
    tr.appendChild(tdScore);
    
    // Activity count
    const tdCount = document.createElement('td');
    tdCount.textContent = user.activityCount;
    tr.appendChild(tdCount);
    
    topUsersTable.appendChild(tr);
  });
}

/**
 * Update the top suspicious IPs table
 */
function updateTopIpsTable() {
  topIpsTable.innerHTML = '';
  
  if (dashboardData.topSuspiciousIps.length === 0) {
    const tr = document.createElement('tr');
    tr.innerHTML = '<td colspan="3" class="text-center">No suspicious IPs</td>';
    topIpsTable.appendChild(tr);
    return;
  }
  
  dashboardData.topSuspiciousIps.forEach(ip => {
    const tr = document.createElement('tr');
    
    // IP address
    const tdIp = document.createElement('td');
    tdIp.textContent = ip.ipAddress;
    tr.appendChild(tdIp);
    
    // Reputation score
    const tdScore = document.createElement('td');
    tdScore.innerHTML = `<span class="badge badge-${getReputationClass(ip.reputationScore)}">${ip.reputationScore}</span>`;
    tr.appendChild(tdScore);
    
    // Activity count
    const tdCount = document.createElement('td');
    tdCount.textContent = ip.activityCount;
    tr.appendChild(tdCount);
    
    topIpsTable.appendChild(tr);
  });
}

/**
 * Update the last updated time
 */
function updateLastUpdatedTime() {
  const now = new Date();
  lastUpdatedTime.textContent = now.toLocaleString();
}

/**
 * Show or hide the loading spinner
 */
function showLoading(show) {
  loadingSpinner.style.display = show ? 'block' : 'none';
}

/**
 * Show an error message
 */
function showError(message) {
  errorMessage.textContent = message;
  errorMessage.style.display = 'block';
}

/**
 * Hide the error message
 */
function hideError() {
  errorMessage.style.display = 'none';
}

/**
 * Format an activity type for display
 */
function formatActivityType(type) {
  return type
    .replace(/_/g, ' ')
    .split(' ')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
    .join(' ');
}

/**
 * Get the CSS class for a severity level
 */
function getSeverityClass(severity) {
  switch (severity) {
    case 'CRITICAL':
      return 'danger';
    case 'HIGH':
      return 'warning';
    case 'MEDIUM':
      return 'info';
    case 'LOW':
      return 'success';
    default:
      return 'secondary';
  }
}

/**
 * Get the CSS class for a reputation score
 */
function getReputationClass(score) {
  if (score < 30) {
    return 'danger';
  } else if (score < 60) {
    return 'warning';
  } else if (score < 80) {
    return 'info';
  } else {
    return 'success';
  }
}

// Export functions for testing
if (typeof module !== 'undefined' && module.exports) {
  module.exports = {
    formatActivityType,
    getSeverityClass,
    getReputationClass
  };
} 