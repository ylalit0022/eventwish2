/**
 * Job Scheduler
 * 
 * This module sets up and manages scheduled jobs using node-cron.
 * It handles:
 * - Inactivity notifications (daily)
 * - Scheduled push notifications (every minute)
 */

const cron = require('node-cron');
const logger = require('../config/logger');
const inactivityNotificationsJob = require('./inactivityNotifications');
const { processScheduledNotifications } = require('./scheduledNotifications');

// Initialize jobs map to track running jobs
const jobs = new Map();

/**
 * Start the inactivity notifications job (runs daily at configurable time)
 */
async function startInactivityNotificationsJob() {
  try {
    // Get configuration
    const config = await inactivityNotificationsJob.getInactivityConfig();
    
    // Default to 10:00 AM if not configured
    const hour = config.automaticSendHour || 10;
    
    // Schedule job to run daily at the configured hour
    const cronExpression = `0 ${hour} * * *`;
    
    const job = cron.schedule(cronExpression, async () => {
      logger.info(`Running scheduled inactivity notifications job (hour: ${hour})`);
      try {
        // Check if automatic sending is enabled in the latest config
        const currentConfig = await inactivityNotificationsJob.getInactivityConfig();
        
        if (currentConfig.isAutomaticSendEnabled) {
          await inactivityNotificationsJob.runInactivityNotificationsJob({
            triggeredBy: 'system'
          });
        } else {
          logger.info('Automatic inactivity notifications are disabled, skipping scheduled run');
        }
      } catch (error) {
        logger.error('Error in inactivity notifications job', { error });
      }
    });
    
    jobs.set('inactivityNotifications', job);
    logger.info(`Inactivity notifications job scheduled (daily at ${hour}:00)`);
    
    return job;
  } catch (error) {
    logger.error('Error starting inactivity notifications job', { error });
    throw error;
  }
}

/**
 * Start the scheduled notifications job (runs every minute)
 */
function startScheduledNotificationsJob() {
  // Schedule job to run every minute to check for notifications that need to be sent
  const job = cron.schedule('* * * * *', async () => {
    try {
      await processScheduledNotifications();
    } catch (error) {
      logger.error('Error in scheduled notifications job', { error });
    }
  });
  
  jobs.set('scheduledNotifications', job);
  logger.info('Scheduled notifications job started (runs every minute)');
  
  return job;
}

/**
 * Initialize all scheduled jobs
 */
function initializeJobs() {
  logger.info('Initializing scheduled jobs');
  
  startInactivityNotificationsJob();
  startScheduledNotificationsJob();
  
  logger.info(`${jobs.size} scheduled jobs initialized`);
}

/**
 * Stop all scheduled jobs
 */
function stopAllJobs() {
  logger.info('Stopping all scheduled jobs');
  
  for (const [name, job] of jobs.entries()) {
    job.stop();
    logger.info(`Stopped job: ${name}`);
  }
  
  jobs.clear();
}

module.exports = {
  initializeJobs,
  stopAllJobs,
  startInactivityNotificationsJob,
  startScheduledNotificationsJob
}; 