import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import adMobApi from '../services/api';

const Container = styled.div`
  padding: 20px;
`;

const Header = styled.div`
  margin-bottom: 30px;
  
  h2 {
    font-size: 24px;
    margin-bottom: 10px;
  }
  
  p {
    color: #5f6368;
  }
`;

const InfoBox = styled.div`
  background-color: #d1ecf1;
  color: #0c5460;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
  
  h3 {
    margin-top: 0;
    margin-bottom: 10px;
    font-size: 18px;
  }
  
  ul {
    margin: 0;
    padding-left: 20px;
  }
  
  li {
    margin-bottom: 5px;
  }
`;

const FraudTestsContainer = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
`;

const FraudTestCard = styled.div`
  background-color: ${props => props.darkMode ? '#303134' : '#fff'};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 20px;
  
  h3 {
    font-size: 18px;
    margin-bottom: 10px;
  }
  
  p {
    color: #5f6368;
    margin-bottom: 20px;
  }
`;

const Button = styled.button`
  background-color: ${props => props.primary ? '#4285f4' : '#f1f3f4'};
  color: ${props => props.primary ? '#fff' : '#3c4043'};
  border: none;
  border-radius: 4px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  margin-right: 10px;
  margin-bottom: 10px;
  
  &:hover {
    background-color: ${props => props.primary ? '#3367d6' : '#e8eaed'};
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

const FormGroup = styled.div`
  margin-bottom: 15px;
  
  label {
    display: block;
    margin-bottom: 5px;
    font-weight: 500;
  }
  
  input, select {
    width: 100%;
    padding: 8px;
    border: 1px solid #ddd;
    border-radius: 4px;
  }
`;

const ResultContainer = styled.div`
  background-color: ${props => props.darkMode ? '#303134' : '#fff'};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 30px;
  
  h3 {
    font-size: 18px;
    margin-bottom: 15px;
  }
`;

const ResultCard = styled.div`
  background-color: ${props => props.success ? '#d4edda' : '#f8d7da'};
  color: ${props => props.success ? '#155724' : '#721c24'};
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
  
  h4 {
    margin-top: 0;
    margin-bottom: 10px;
    display: flex;
    align-items: center;
    
    span {
      margin-left: 10px;
    }
  }
  
  p {
    margin: 0;
  }
`;

const FraudScoreContainer = styled.div`
  margin-top: 20px;
  
  h4 {
    margin-bottom: 10px;
  }
`;

const FraudScoreBar = styled.div`
  height: 20px;
  background-color: #f1f3f4;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 10px;
  
  .score-fill {
    height: 100%;
    background: linear-gradient(90deg, #34a853, #fbbc05, #ea4335);
    width: ${props => props.score}%;
    transition: width 0.5s ease;
  }
`;

const FraudScoreLabel = styled.div`
  display: flex;
  justify-content: space-between;
  
  .low {
    color: #34a853;
  }
  
  .medium {
    color: #fbbc05;
  }
  
  .high {
    color: #ea4335;
  }
`;

const ReasonsList = styled.ul`
  margin-top: 15px;
  padding-left: 20px;
  
  li {
    margin-bottom: 5px;
  }
`;

const LogContainer = styled.div`
  background-color: ${props => props.darkMode ? '#303134' : '#fff'};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 30px;
  
  h3 {
    font-size: 18px;
    margin-bottom: 15px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  
  .clear-button {
    font-size: 12px;
    padding: 4px 8px;
    background-color: #f1f3f4;
    color: #3c4043;
    border: none;
    border-radius: 4px;
    cursor: pointer;
  }
`;

const LogEntries = styled.div`
  max-height: 300px;
  overflow-y: auto;
  font-family: monospace;
  font-size: 14px;
  
  .log-entry {
    padding: 5px 0;
    border-bottom: 1px solid #f1f3f4;
    
    .timestamp {
      color: #5f6368;
      margin-right: 10px;
    }
    
    .event-type {
      font-weight: bold;
      margin-right: 10px;
      
      &.test { color: #4285f4; }
      &.fraud { color: #ea4335; }
      &.success { color: #34a853; }
      &.warning { color: #fbbc05; }
    }
  }
`;

const LoadingState = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #5f6368;
`;

const ErrorState = styled.div`
  background-color: #f8d7da;
  color: #721c24;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
`;

const FraudDetection = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [testResults, setTestResults] = useState(null);
  const [fraudScore, setFraudScore] = useState(0);
  const [logEntries, setLogEntries] = useState([]);
  const [rapidClickSettings, setRapidClickSettings] = useState({
    clickCount: 10,
    timeWindow: 5
  });
  const [patternClickSettings, setPatternClickSettings] = useState({
    pattern: 'grid',
    clickCount: 10
  });
  const [multiDeviceSettings, setMultiDeviceSettings] = useState({
    deviceCount: 3,
    sameIp: true
  });
  
  const addLogEntry = (eventType, message) => {
    const timestamp = new Date().toISOString().split('T')[1].split('.')[0];
    const newEntry = {
      id: Date.now(),
      timestamp,
      eventType,
      message
    };
    
    setLogEntries(prev => [newEntry, ...prev].slice(0, 50));
  };
  
  const clearLog = () => {
    setLogEntries([]);
  };
  
  const handleRapidClickSettingChange = (e) => {
    const { name, value } = e.target;
    setRapidClickSettings(prev => ({
      ...prev,
      [name]: parseInt(value, 10)
    }));
  };
  
  const handlePatternClickSettingChange = (e) => {
    const { name, value } = e.target;
    setPatternClickSettings(prev => ({
      ...prev,
      [name]: name === 'clickCount' ? parseInt(value, 10) : value
    }));
  };
  
  const handleMultiDeviceSettingChange = (e) => {
    const { name, value, type, checked } = e.target;
    setMultiDeviceSettings(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : parseInt(value, 10)
    }));
  };
  
  const testRapidClicks = async () => {
    try {
      setLoading(true);
      setError(null);
      setTestResults(null);
      
      addLogEntry('test', `Starting rapid click test with ${rapidClickSettings.clickCount} clicks in ${rapidClickSettings.timeWindow} seconds`);
      
      // In a real implementation, we would call the API
      // For now, we'll simulate the API call
      
      // Simulate API call delay
      await new Promise(resolve => setTimeout(resolve, 1500));
      
      // Calculate fraud score based on settings
      const calculatedScore = Math.min(100, (rapidClickSettings.clickCount / rapidClickSettings.timeWindow) * 10);
      setFraudScore(calculatedScore);
      
      // Determine if fraud is detected
      const isFraud = calculatedScore > 60;
      
      // Generate reasons
      const reasons = [];
      if (calculatedScore > 60) {
        reasons.push(`${rapidClickSettings.clickCount} clicks in ${rapidClickSettings.timeWindow} seconds exceeds threshold`);
      }
      if (calculatedScore > 80) {
        reasons.push('Click pattern appears automated');
      }
      
      // Set test results
      setTestResults({
        success: !isFraud,
        title: isFraud ? 'Fraud Detected' : 'No Fraud Detected',
        message: isFraud 
          ? `Rapid click fraud detected with a score of ${calculatedScore.toFixed(0)}` 
          : `No fraud detected. Score: ${calculatedScore.toFixed(0)}`,
        score: calculatedScore,
        reasons: reasons
      });
      
      // Log result
      if (isFraud) {
        addLogEntry('fraud', `Fraud detected: Rapid clicks (${rapidClickSettings.clickCount} clicks in ${rapidClickSettings.timeWindow}s)`);
      } else {
        addLogEntry('success', `No fraud detected for rapid click test`);
      }
      
      setLoading(false);
    } catch (error) {
      console.error('Error testing rapid clicks:', error);
      setError('Failed to test rapid clicks. Please try again later.');
      setLoading(false);
      addLogEntry('error', `Error testing rapid clicks: ${error.message}`);
    }
  };
  
  const testPatternClicks = async () => {
    try {
      setLoading(true);
      setError(null);
      setTestResults(null);
      
      addLogEntry('test', `Starting pattern click test with pattern: ${patternClickSettings.pattern}, clicks: ${patternClickSettings.clickCount}`);
      
      // In a real implementation, we would call the API
      // For now, we'll simulate the API call
      
      // Simulate API call delay
      await new Promise(resolve => setTimeout(resolve, 1500));
      
      // Calculate fraud score based on settings
      let calculatedScore = 0;
      
      switch (patternClickSettings.pattern) {
        case 'grid':
          calculatedScore = Math.min(100, patternClickSettings.clickCount * 5);
          break;
        case 'linear':
          calculatedScore = Math.min(100, patternClickSettings.clickCount * 4);
          break;
        case 'random':
          calculatedScore = Math.min(100, patternClickSettings.clickCount * 2);
          break;
        default:
          calculatedScore = Math.min(100, patternClickSettings.clickCount * 3);
      }
      
      setFraudScore(calculatedScore);
      
      // Determine if fraud is detected
      const isFraud = calculatedScore > 60;
      
      // Generate reasons
      const reasons = [];
      if (patternClickSettings.pattern === 'grid' || patternClickSettings.pattern === 'linear') {
        reasons.push(`${patternClickSettings.pattern} pattern detected in click sequence`);
      }
      if (patternClickSettings.clickCount > 5) {
        reasons.push(`${patternClickSettings.clickCount} clicks in pattern exceeds threshold`);
      }
      
      // Set test results
      setTestResults({
        success: !isFraud,
        title: isFraud ? 'Fraud Detected' : 'No Fraud Detected',
        message: isFraud 
          ? `Pattern click fraud detected with a score of ${calculatedScore.toFixed(0)}` 
          : `No fraud detected. Score: ${calculatedScore.toFixed(0)}`,
        score: calculatedScore,
        reasons: reasons
      });
      
      // Log result
      if (isFraud) {
        addLogEntry('fraud', `Fraud detected: Pattern clicks (${patternClickSettings.pattern} pattern with ${patternClickSettings.clickCount} clicks)`);
      } else {
        addLogEntry('success', `No fraud detected for pattern click test`);
      }
      
      setLoading(false);
    } catch (error) {
      console.error('Error testing pattern clicks:', error);
      setError('Failed to test pattern clicks. Please try again later.');
      setLoading(false);
      addLogEntry('error', `Error testing pattern clicks: ${error.message}`);
    }
  };
  
  const testMultiDevice = async () => {
    try {
      setLoading(true);
      setError(null);
      setTestResults(null);
      
      addLogEntry('test', `Starting multi-device test with ${multiDeviceSettings.deviceCount} devices, same IP: ${multiDeviceSettings.sameIp}`);
      
      // In a real implementation, we would call the API
      // For now, we'll simulate the API call
      
      // Simulate API call delay
      await new Promise(resolve => setTimeout(resolve, 1500));
      
      // Calculate fraud score based on settings
      let calculatedScore = Math.min(100, multiDeviceSettings.deviceCount * 20);
      
      if (!multiDeviceSettings.sameIp) {
        calculatedScore = Math.max(0, calculatedScore - 30);
      }
      
      setFraudScore(calculatedScore);
      
      // Determine if fraud is detected
      const isFraud = calculatedScore > 60;
      
      // Generate reasons
      const reasons = [];
      if (multiDeviceSettings.deviceCount > 2) {
        reasons.push(`${multiDeviceSettings.deviceCount} devices clicking on same ad`);
      }
      if (multiDeviceSettings.sameIp) {
        reasons.push('Multiple devices from same IP address');
      }
      
      // Set test results
      setTestResults({
        success: !isFraud,
        title: isFraud ? 'Fraud Detected' : 'No Fraud Detected',
        message: isFraud 
          ? `Multi-device fraud detected with a score of ${calculatedScore.toFixed(0)}` 
          : `No fraud detected. Score: ${calculatedScore.toFixed(0)}`,
        score: calculatedScore,
        reasons: reasons
      });
      
      // Log result
      if (isFraud) {
        addLogEntry('fraud', `Fraud detected: Multi-device (${multiDeviceSettings.deviceCount} devices, same IP: ${multiDeviceSettings.sameIp})`);
      } else {
        addLogEntry('success', `No fraud detected for multi-device test`);
      }
      
      setLoading(false);
    } catch (error) {
      console.error('Error testing multi-device:', error);
      setError('Failed to test multi-device scenario. Please try again later.');
      setLoading(false);
      addLogEntry('error', `Error testing multi-device: ${error.message}`);
    }
  };
  
  return (
    <Container>
      <Header>
        <h2>Fraud Detection Testing</h2>
        <p>Test and visualize AdMob fraud detection mechanisms</p>
      </Header>
      
      <InfoBox>
        <h3>About Fraud Detection</h3>
        <p>
          AdMob's fraud detection system identifies and prevents invalid clicks and impressions.
          This page allows you to test different fraud scenarios and see how they would be detected.
        </p>
        <ul>
          <li><strong>Rapid Clicks</strong>: Multiple clicks in a short time period</li>
          <li><strong>Pattern Clicks</strong>: Clicks that follow a specific pattern</li>
          <li><strong>Multi-Device</strong>: Same user clicking from multiple devices</li>
        </ul>
      </InfoBox>
      
      <FraudTestsContainer>
        <FraudTestCard>
          <h3>Rapid Click Test</h3>
          <p>Test detection of multiple clicks in a short time period</p>
          
          <FormGroup>
            <label>Number of Clicks</label>
            <input 
              type="number" 
              name="clickCount" 
              value={rapidClickSettings.clickCount} 
              onChange={handleRapidClickSettingChange} 
              min="1" 
              max="100" 
            />
          </FormGroup>
          
          <FormGroup>
            <label>Time Window (seconds)</label>
            <input 
              type="number" 
              name="timeWindow" 
              value={rapidClickSettings.timeWindow} 
              onChange={handleRapidClickSettingChange} 
              min="1" 
              max="60" 
            />
          </FormGroup>
          
          <Button primary onClick={testRapidClicks} disabled={loading}>
            {loading ? 'Testing...' : 'Run Test'}
          </Button>
        </FraudTestCard>
        
        <FraudTestCard>
          <h3>Pattern Click Test</h3>
          <p>Test detection of clicks that follow a specific pattern</p>
          
          <FormGroup>
            <label>Pattern Type</label>
            <select 
              name="pattern" 
              value={patternClickSettings.pattern} 
              onChange={handlePatternClickSettingChange}
            >
              <option value="grid">Grid Pattern</option>
              <option value="linear">Linear Pattern</option>
              <option value="random">Random Pattern</option>
            </select>
          </FormGroup>
          
          <FormGroup>
            <label>Number of Clicks</label>
            <input 
              type="number" 
              name="clickCount" 
              value={patternClickSettings.clickCount} 
              onChange={handlePatternClickSettingChange} 
              min="1" 
              max="100" 
            />
          </FormGroup>
          
          <Button primary onClick={testPatternClicks} disabled={loading}>
            {loading ? 'Testing...' : 'Run Test'}
          </Button>
        </FraudTestCard>
        
        <FraudTestCard>
          <h3>Multi-Device Test</h3>
          <p>Test detection of same user clicking from multiple devices</p>
          
          <FormGroup>
            <label>Number of Devices</label>
            <input 
              type="number" 
              name="deviceCount" 
              value={multiDeviceSettings.deviceCount} 
              onChange={handleMultiDeviceSettingChange} 
              min="1" 
              max="10" 
            />
          </FormGroup>
          
          <FormGroup>
            <label>
              <input 
                type="checkbox" 
                name="sameIp" 
                checked={multiDeviceSettings.sameIp} 
                onChange={handleMultiDeviceSettingChange} 
                style={{ width: 'auto', marginRight: '10px' }}
              />
              Same IP Address
            </label>
          </FormGroup>
          
          <Button primary onClick={testMultiDevice} disabled={loading}>
            {loading ? 'Testing...' : 'Run Test'}
          </Button>
        </FraudTestCard>
      </FraudTestsContainer>
      
      {error && <ErrorState>{error}</ErrorState>}
      
      {testResults && (
        <ResultContainer>
          <h3>Test Results</h3>
          
          <ResultCard success={testResults.success}>
            <h4>
              {testResults.success ? '✓' : '✗'}
              <span>{testResults.title}</span>
            </h4>
            <p>{testResults.message}</p>
            
            {testResults.reasons.length > 0 && (
              <ReasonsList>
                {testResults.reasons.map((reason, index) => (
                  <li key={index}>{reason}</li>
                ))}
              </ReasonsList>
            )}
            
            <FraudScoreContainer>
              <h4>Fraud Score: {testResults.score.toFixed(0)}/100</h4>
              <FraudScoreBar score={testResults.score}>
                <div className="score-fill"></div>
              </FraudScoreBar>
              <FraudScoreLabel>
                <span className="low">Low Risk (0-30)</span>
                <span className="medium">Medium Risk (31-60)</span>
                <span className="high">High Risk (61-100)</span>
              </FraudScoreLabel>
            </FraudScoreContainer>
          </ResultCard>
        </ResultContainer>
      )}
      
      <LogContainer>
        <h3>
          Event Log
          <button className="clear-button" onClick={clearLog}>Clear</button>
        </h3>
        <LogEntries>
          {logEntries.length === 0 ? (
            <div style={{ padding: '10px 0', color: '#5f6368' }}>No events logged yet</div>
          ) : (
            logEntries.map(entry => (
              <div key={entry.id} className="log-entry">
                <span className="timestamp">{entry.timestamp}</span>
                <span className={`event-type ${entry.eventType}`}>{entry.eventType.toUpperCase()}</span>
                <span className="message">{entry.message}</span>
              </div>
            ))
          )}
        </LogEntries>
      </LogContainer>
    </Container>
  );
};

export default FraudDetection; 