/**
 * Tests for the Suspicious Activity Monitoring Service
 */

const { expect } = require('chai');
const sinon = require('sinon');
const suspiciousActivityService = require('../services/suspiciousActivityService');
const redisClient = require('../config/redis');
const logger = require('../utils/logger');

describe('Suspicious Activity Service', () => {
  let redisStub;
  let loggerStub;

  beforeEach(() => {
    // Stub Redis client methods
    redisStub = {
      hset: sinon.stub().resolves('OK'),
      hget: sinon.stub().resolves('10'),
      hincrby: sinon.stub().resolves(11),
      zadd: sinon.stub().resolves(1),
      zrange: sinon.stub().resolves(['user1', 'user2']),
      zrevrange: sinon.stub().resolves(['user1', 'user2']),
      hgetall: sinon.stub().resolves({ type: 'CLICK_FRAUD', severity: 'HIGH' }),
      expire: sinon.stub().resolves(1),
      multi: sinon.stub().returns({
        hset: sinon.stub().returns({ hset: true }),
        hincrby: sinon.stub().returns({ hincrby: true }),
        zadd: sinon.stub().returns({ zadd: true }),
        expire: sinon.stub().returns({ expire: true }),
        exec: sinon.stub().resolves([])
      })
    };

    // Replace Redis client methods with stubs
    sinon.stub(redisClient, 'getClient').returns(redisStub);

    // Stub logger
    loggerStub = {
      info: sinon.stub(),
      error: sinon.stub(),
      warn: sinon.stub(),
      debug: sinon.stub()
    };
    sinon.stub(logger, 'getLogger').returns(loggerStub);
  });

  afterEach(() => {
    // Restore stubs
    sinon.restore();
  });

  describe('trackActivity', () => {
    it('should track a suspicious activity', async () => {
      const activityData = {
        entityType: 'USER',
        entityId: 'user123',
        type: 'CLICK_FRAUD',
        severity: 'HIGH',
        details: { fraudScore: 85, clickId: 'click123' }
      };

      const result = await suspiciousActivityService.trackActivity(activityData);

      expect(result).to.be.true;
      expect(redisStub.multi().hset.called).to.be.true;
      expect(redisStub.multi().hincrby.called).to.be.true;
      expect(redisStub.multi().zadd.called).to.be.true;
      expect(redisStub.multi().expire.called).to.be.true;
      expect(redisStub.multi().exec.called).to.be.true;
      expect(loggerStub.info.called).to.be.true;
    });

    it('should handle errors when tracking activity', async () => {
      const activityData = {
        entityType: 'USER',
        entityId: 'user123',
        type: 'CLICK_FRAUD',
        severity: 'HIGH',
        details: { fraudScore: 85, clickId: 'click123' }
      };

      // Make the Redis multi().exec() method throw an error
      redisStub.multi().exec = sinon.stub().rejects(new Error('Redis error'));

      const result = await suspiciousActivityService.trackActivity(activityData);

      expect(result).to.be.false;
      expect(loggerStub.error.called).to.be.true;
    });

    it('should validate activity data before tracking', async () => {
      // Missing required fields
      const invalidData = {
        entityType: 'USER',
        // Missing entityId
        type: 'CLICK_FRAUD',
        // Missing severity
        details: { fraudScore: 85 }
      };

      const result = await suspiciousActivityService.trackActivity(invalidData);

      expect(result).to.be.false;
      expect(loggerStub.error.called).to.be.true;
      expect(redisStub.multi().exec.called).to.be.false;
    });
  });

  describe('updateReputationScore', () => {
    it('should update reputation score for an entity', async () => {
      const entityType = 'USER';
      const entityId = 'user123';
      const scoreChange = -10;

      const result = await suspiciousActivityService.updateReputationScore(entityType, entityId, scoreChange);

      expect(result).to.equal(90); // 100 (default) - 10
      expect(redisStub.hget.called).to.be.true;
      expect(redisStub.hset.called).to.be.true;
      expect(loggerStub.debug.called).to.be.true;
    });

    it('should handle errors when updating reputation score', async () => {
      const entityType = 'USER';
      const entityId = 'user123';
      const scoreChange = -10;

      // Make the Redis hset method throw an error
      redisStub.hset = sinon.stub().rejects(new Error('Redis error'));

      const result = await suspiciousActivityService.updateReputationScore(entityType, entityId, scoreChange);

      expect(result).to.be.null;
      expect(loggerStub.error.called).to.be.true;
    });

    it('should validate input parameters', async () => {
      // Invalid entity type
      const result1 = await suspiciousActivityService.updateReputationScore('INVALID_TYPE', 'user123', -10);
      expect(result1).to.be.null;
      expect(loggerStub.error.called).to.be.true;

      // Reset logger stub
      loggerStub.error.reset();

      // Invalid score change (not a number)
      const result2 = await suspiciousActivityService.updateReputationScore('USER', 'user123', 'not-a-number');
      expect(result2).to.be.null;
      expect(loggerStub.error.called).to.be.true;
    });
  });

  describe('getReputationScore', () => {
    it('should get reputation score for an entity', async () => {
      const entityType = 'USER';
      const entityId = 'user123';

      // Set up Redis stub to return a specific score
      redisStub.hget = sinon.stub().resolves('75');

      const result = await suspiciousActivityService.getReputationScore(entityType, entityId);

      expect(result).to.equal(75);
      expect(redisStub.hget.called).to.be.true;
    });

    it('should return default score if no score exists', async () => {
      const entityType = 'USER';
      const entityId = 'user123';

      // Set up Redis stub to return null (no score exists)
      redisStub.hget = sinon.stub().resolves(null);

      const result = await suspiciousActivityService.getReputationScore(entityType, entityId);

      expect(result).to.equal(100); // Default score
      expect(redisStub.hget.called).to.be.true;
    });

    it('should handle errors when getting reputation score', async () => {
      const entityType = 'USER';
      const entityId = 'user123';

      // Make the Redis hget method throw an error
      redisStub.hget = sinon.stub().rejects(new Error('Redis error'));

      const result = await suspiciousActivityService.getReputationScore(entityType, entityId);

      expect(result).to.be.null;
      expect(loggerStub.error.called).to.be.true;
    });
  });

  describe('getActivities', () => {
    it('should get activities for an entity', async () => {
      const entityType = 'USER';
      const entityId = 'user123';
      const limit = 10;

      // Set up Redis stub to return activity IDs
      redisStub.zrevrange = sinon.stub().resolves(['activity1', 'activity2']);

      // Set up Redis stub to return activity data
      redisStub.hgetall = sinon.stub()
        .onFirstCall().resolves({
          type: 'CLICK_FRAUD',
          severity: 'HIGH',
          timestamp: '1620000000000',
          details: JSON.stringify({ fraudScore: 85, clickId: 'click123' })
        })
        .onSecondCall().resolves({
          type: 'PROXY_USAGE',
          severity: 'MEDIUM',
          timestamp: '1620000001000',
          details: JSON.stringify({ proxyType: 'VPN', ipAddress: '1.2.3.4' })
        });

      const result = await suspiciousActivityService.getActivities(entityType, entityId, limit);

      expect(result).to.be.an('array').with.lengthOf(2);
      expect(result[0].type).to.equal('CLICK_FRAUD');
      expect(result[0].severity).to.equal('HIGH');
      expect(result[0].details.fraudScore).to.equal(85);
      expect(result[1].type).to.equal('PROXY_USAGE');
      expect(result[1].severity).to.equal('MEDIUM');
      expect(result[1].details.proxyType).to.equal('VPN');
      expect(redisStub.zrevrange.called).to.be.true;
      expect(redisStub.hgetall.calledTwice).to.be.true;
    });

    it('should handle errors when getting activities', async () => {
      const entityType = 'USER';
      const entityId = 'user123';
      const limit = 10;

      // Make the Redis zrevrange method throw an error
      redisStub.zrevrange = sinon.stub().rejects(new Error('Redis error'));

      const result = await suspiciousActivityService.getActivities(entityType, entityId, limit);

      expect(result).to.be.an('array').that.is.empty;
      expect(loggerStub.error.called).to.be.true;
    });
  });

  describe('analyzeTrafficPatterns', () => {
    it('should analyze traffic patterns for an entity', async () => {
      const entityType = 'USER';
      const entityId = 'user123';

      // Set up Redis stub to return activity data
      redisStub.zrevrange = sinon.stub().resolves(['activity1', 'activity2', 'activity3']);
      redisStub.hgetall = sinon.stub()
        .onFirstCall().resolves({
          type: 'CLICK_FRAUD',
          severity: 'HIGH',
          timestamp: '1620000000000',
          details: JSON.stringify({ fraudScore: 85, clickId: 'click123' })
        })
        .onSecondCall().resolves({
          type: 'PROXY_USAGE',
          severity: 'MEDIUM',
          timestamp: '1620000001000',
          details: JSON.stringify({ proxyType: 'VPN', ipAddress: '1.2.3.4' })
        })
        .onThirdCall().resolves({
          type: 'CLICK_FRAUD',
          severity: 'HIGH',
          timestamp: '1620000002000',
          details: JSON.stringify({ fraudScore: 90, clickId: 'click124' })
        });

      const result = await suspiciousActivityService.analyzeTrafficPatterns(entityType, entityId);

      expect(result).to.be.an('object');
      expect(result.activityCount).to.equal(3);
      expect(result.activityTypes).to.be.an('object');
      expect(result.activityTypes.CLICK_FRAUD).to.equal(2);
      expect(result.activityTypes.PROXY_USAGE).to.equal(1);
      expect(result.severityLevels).to.be.an('object');
      expect(result.severityLevels.HIGH).to.equal(2);
      expect(result.severityLevels.MEDIUM).to.equal(1);
      expect(result.timeDistribution).to.be.an('object');
      expect(redisStub.zrevrange.called).to.be.true;
      expect(redisStub.hgetall.calledThrice).to.be.true;
    });

    it('should handle errors when analyzing traffic patterns', async () => {
      const entityType = 'USER';
      const entityId = 'user123';

      // Make the Redis zrevrange method throw an error
      redisStub.zrevrange = sinon.stub().rejects(new Error('Redis error'));

      const result = await suspiciousActivityService.analyzeTrafficPatterns(entityType, entityId);

      expect(result).to.be.null;
      expect(loggerStub.error.called).to.be.true;
    });
  });

  describe('getDashboardData', () => {
    it('should get dashboard data', async () => {
      // Set up Redis stubs for various dashboard data
      redisStub.hget
        .onFirstCall().resolves('100') // Total activities
        .onSecondCall().resolves('20') // Critical severity
        .onThirdCall().resolves('30'); // High severity

      redisStub.hgetall
        .onFirstCall().resolves({ // Activity by type
          CLICK_FRAUD: '50',
          PROXY_USAGE: '30',
          VPN_USAGE: '20'
        })
        .onSecondCall().resolves({ // Activity by severity
          CRITICAL: '20',
          HIGH: '30',
          MEDIUM: '40',
          LOW: '10'
        });

      // Set up stubs for recent activities
      redisStub.zrevrange
        .onFirstCall().resolves(['activity1', 'activity2', 'activity3']) // Recent activity IDs
        .onSecondCall().resolves(['user1', 'user2', 'user3']) // Top suspicious users
        .onThirdCall().resolves(['ip1', 'ip2', 'ip3']); // Top suspicious IPs

      // Set up stubs for activity details
      const activityStub = sinon.stub(suspiciousActivityService, 'getActivityDetails');
      activityStub.onFirstCall().resolves({
        id: 'activity1',
        entityType: 'USER',
        entityId: 'user1',
        type: 'CLICK_FRAUD',
        severity: 'HIGH',
        timestamp: 1620000000000,
        details: { fraudScore: 85 }
      });
      activityStub.onSecondCall().resolves({
        id: 'activity2',
        entityType: 'IP',
        entityId: 'ip1',
        type: 'PROXY_USAGE',
        severity: 'MEDIUM',
        timestamp: 1620000001000,
        details: { proxyType: 'VPN' }
      });
      activityStub.onThirdCall().resolves({
        id: 'activity3',
        entityType: 'DEVICE',
        entityId: 'device1',
        type: 'VPN_USAGE',
        severity: 'CRITICAL',
        timestamp: 1620000002000,
        details: { vpnProvider: 'Unknown' }
      });

      // Set up stubs for user and IP details
      const getEntityDetailsStub = sinon.stub(suspiciousActivityService, 'getEntityDetails');
      getEntityDetailsStub
        .onCall(0).resolves({ userId: 'user1', reputationScore: 30, activityCount: 10 })
        .onCall(1).resolves({ userId: 'user2', reputationScore: 40, activityCount: 8 })
        .onCall(2).resolves({ userId: 'user3', reputationScore: 50, activityCount: 6 })
        .onCall(3).resolves({ ipAddress: 'ip1', reputationScore: 20, activityCount: 15 })
        .onCall(4).resolves({ ipAddress: 'ip2', reputationScore: 35, activityCount: 12 })
        .onCall(5).resolves({ ipAddress: 'ip3', reputationScore: 45, activityCount: 9 });

      const result = await suspiciousActivityService.getDashboardData();

      expect(result).to.be.an('object');
      expect(result.stats).to.be.an('object');
      expect(result.stats.totalActivities).to.equal(100);
      expect(result.stats.criticalSeverity).to.equal(20);
      expect(result.stats.highSeverity).to.equal(30);
      expect(result.activityByType).to.be.an('object');
      expect(result.activityByType.CLICK_FRAUD).to.equal(50);
      expect(result.activityBySeverity).to.be.an('object');
      expect(result.activityBySeverity.CRITICAL).to.equal(20);
      expect(result.recentActivities).to.be.an('array').with.lengthOf(3);
      expect(result.topSuspiciousUsers).to.be.an('array').with.lengthOf(3);
      expect(result.topSuspiciousIps).to.be.an('array').with.lengthOf(3);
    });

    it('should handle errors when getting dashboard data', async () => {
      // Make the Redis hget method throw an error
      redisStub.hget = sinon.stub().rejects(new Error('Redis error'));

      const result = await suspiciousActivityService.getDashboardData();

      expect(result).to.be.null;
      expect(loggerStub.error.called).to.be.true;
    });
  });

  describe('isSuspicious', () => {
    it('should return true if entity is suspicious', async () => {
      const entityType = 'USER';
      const entityId = 'user123';
      const threshold = 50;

      // Set up Redis stub to return a low reputation score
      redisStub.hget = sinon.stub().resolves('30');

      const result = await suspiciousActivityService.isSuspicious(entityType, entityId, threshold);

      expect(result).to.be.true;
      expect(redisStub.hget.called).to.be.true;
    });

    it('should return false if entity is not suspicious', async () => {
      const entityType = 'USER';
      const entityId = 'user123';
      const threshold = 50;

      // Set up Redis stub to return a high reputation score
      redisStub.hget = sinon.stub().resolves('80');

      const result = await suspiciousActivityService.isSuspicious(entityType, entityId, threshold);

      expect(result).to.be.false;
      expect(redisStub.hget.called).to.be.true;
    });

    it('should handle errors when checking if entity is suspicious', async () => {
      const entityType = 'USER';
      const entityId = 'user123';
      const threshold = 50;

      // Make the Redis hget method throw an error
      redisStub.hget = sinon.stub().rejects(new Error('Redis error'));

      const result = await suspiciousActivityService.isSuspicious(entityType, entityId, threshold);

      expect(result).to.be.false;
      expect(loggerStub.error.called).to.be.true;
    });
  });

  describe('triggerAlert', () => {
    it('should trigger an alert for high-severity activity', async () => {
      const activity = {
        id: 'activity1',
        entityType: 'USER',
        entityId: 'user123',
        type: 'CLICK_FRAUD',
        severity: 'CRITICAL',
        timestamp: Date.now(),
        details: { fraudScore: 95 }
      };

      // Create a stub for the alert notification function
      const notifyStub = sinon.stub(suspiciousActivityService, 'notifyAlert').resolves(true);

      const result = await suspiciousActivityService.triggerAlert(activity);

      expect(result).to.be.true;
      expect(notifyStub.called).to.be.true;
      expect(loggerStub.info.called).to.be.true;
    });

    it('should not trigger an alert for low-severity activity', async () => {
      const activity = {
        id: 'activity1',
        entityType: 'USER',
        entityId: 'user123',
        type: 'PROXY_USAGE',
        severity: 'LOW',
        timestamp: Date.now(),
        details: { proxyType: 'Unknown' }
      };

      // Create a stub for the alert notification function
      const notifyStub = sinon.stub(suspiciousActivityService, 'notifyAlert').resolves(true);

      const result = await suspiciousActivityService.triggerAlert(activity);

      expect(result).to.be.false;
      expect(notifyStub.called).to.be.false;
    });

    it('should handle errors when triggering an alert', async () => {
      const activity = {
        id: 'activity1',
        entityType: 'USER',
        entityId: 'user123',
        type: 'CLICK_FRAUD',
        severity: 'CRITICAL',
        timestamp: Date.now(),
        details: { fraudScore: 95 }
      };

      // Make the notify function throw an error
      const notifyStub = sinon.stub(suspiciousActivityService, 'notifyAlert').rejects(new Error('Notification error'));

      const result = await suspiciousActivityService.triggerAlert(activity);

      expect(result).to.be.false;
      expect(notifyStub.called).to.be.true;
      expect(loggerStub.error.called).to.be.true;
    });
  });
}); 