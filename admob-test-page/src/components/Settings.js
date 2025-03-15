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

const SettingsCard = styled.div`
  background-color: ${props => props.darkMode ? '#303134' : '#fff'};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 20px;
  
  h3 {
    font-size: 18px;
    margin-bottom: 15px;
  }
`;

const FormGroup = styled.div`
  margin-bottom: 15px;
  
  label {
    display: block;
    margin-bottom: 5px;
    font-weight: 500;
  }
  
  input, select, textarea {
    width: 100%;
    padding: 10px;
    border: 1px solid #ddd;
    border-radius: 4px;
    font-size: 14px;
    background-color: ${props => props.darkMode ? '#202124' : '#fff'};
    color: ${props => props.darkMode ? '#e8eaed' : '#3c4043'};
  }
`;

const FormRow = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 15px;
`;

const Button = styled.button`
  background-color: ${props => props.primary ? '#4285f4' : '#f1f3f4'};
  color: ${props => props.primary ? '#fff' : '#3c4043'};
  border: none;
  border-radius: 4px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  
  &:hover {
    background-color: ${props => props.primary ? '#3367d6' : '#e8eaed'};
  }
`;

const ButtonGroup = styled.div`
  display: flex;
  gap: 10px;
  margin-top: 20px;
`;

const SwitchContainer = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  
  label {
    margin-right: 10px;
    font-weight: 500;
  }
`;

const Switch = styled.label`
  position: relative;
  display: inline-block;
  width: 50px;
  height: 24px;
  
  input {
    opacity: 0;
    width: 0;
    height: 0;
  }
  
  .slider {
    position: absolute;
    cursor: pointer;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: #ccc;
    transition: .4s;
    border-radius: 24px;
  }
  
  .slider:before {
    position: absolute;
    content: "";
    height: 16px;
    width: 16px;
    left: 4px;
    bottom: 4px;
    background-color: white;
    transition: .4s;
    border-radius: 50%;
  }
  
  input:checked + .slider {
    background-color: #4285f4;
  }
  
  input:checked + .slider:before {
    transform: translateX(26px);
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

const SuccessState = styled.div`
  background-color: #d4edda;
  color: #155724;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
`;

const Settings = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [settings, setSettings] = useState({
    apiKey: '',
    appSignature: '',
    testMode: true,
    adTypes: {
      banner: true,
      interstitial: true,
      rewarded: true,
      native: true
    },
    refreshRate: 60,
    logLevel: 'info',
    analyticsEnabled: true,
    fraudDetectionEnabled: true,
    abTestingEnabled: true,
    serverEndpoint: 'https://eventwish2.onrender.com/api'
  });
  
  useEffect(() => {
    const loadSettings = () => {
      try {
        setLoading(true);
        
        // In a real implementation, we would load settings from localStorage or API
        // For now, we'll use mock data
        
        // Try to load settings from localStorage
        const storedApiKey = localStorage.getItem('api_key');
        const storedAppSignature = localStorage.getItem('app_signature');
        const storedServerEndpoint = localStorage.getItem('server_endpoint');
        
        if (storedApiKey) {
          setSettings(prev => ({
            ...prev,
            apiKey: storedApiKey
          }));
        }
        
        if (storedAppSignature) {
          setSettings(prev => ({
            ...prev,
            appSignature: storedAppSignature
          }));
        }
        
        if (storedServerEndpoint) {
          setSettings(prev => ({
            ...prev,
            serverEndpoint: storedServerEndpoint
          }));
        }
        
        setLoading(false);
      } catch (error) {
        console.error('Error loading settings:', error);
        setError('Failed to load settings. Please try again later.');
        setLoading(false);
      }
    };
    
    loadSettings();
  }, []);
  
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setSettings(prev => ({
      ...prev,
      [name]: value
    }));
  };
  
  const handleSwitchChange = (e) => {
    const { name, checked } = e.target;
    
    if (name.includes('.')) {
      const [parent, child] = name.split('.');
      setSettings(prev => ({
        ...prev,
        [parent]: {
          ...prev[parent],
          [child]: checked
        }
      }));
    } else {
      setSettings(prev => ({
        ...prev,
        [name]: checked
      }));
    }
  };
  
  const handleSaveSettings = () => {
    try {
      setLoading(true);
      
      // In a real implementation, we would save settings to API
      // For now, we'll just save to localStorage
      
      localStorage.setItem('api_key', settings.apiKey);
      localStorage.setItem('app_signature', settings.appSignature);
      localStorage.setItem('server_endpoint', settings.serverEndpoint);
      
      setSuccess('Settings saved successfully!');
      
      // Clear success message after 3 seconds
      setTimeout(() => {
        setSuccess(null);
      }, 3000);
      
      setLoading(false);
    } catch (error) {
      console.error('Error saving settings:', error);
      setError('Failed to save settings. Please try again later.');
      setLoading(false);
    }
  };
  
  const handleResetSettings = () => {
    try {
      setLoading(true);
      
      // Reset settings to default
      setSettings({
        apiKey: '',
        appSignature: '',
        testMode: true,
        adTypes: {
          banner: true,
          interstitial: true,
          rewarded: true,
          native: true
        },
        refreshRate: 60,
        logLevel: 'info',
        analyticsEnabled: true,
        fraudDetectionEnabled: true,
        abTestingEnabled: true,
        serverEndpoint: 'https://eventwish2.onrender.com/api'
      });
      
      // Clear localStorage
      localStorage.removeItem('api_key');
      localStorage.removeItem('app_signature');
      localStorage.removeItem('server_endpoint');
      
      setSuccess('Settings reset successfully!');
      
      // Clear success message after 3 seconds
      setTimeout(() => {
        setSuccess(null);
      }, 3000);
      
      setLoading(false);
    } catch (error) {
      console.error('Error resetting settings:', error);
      setError('Failed to reset settings. Please try again later.');
      setLoading(false);
    }
  };
  
  const handleClearCache = () => {
    try {
      setLoading(true);
      
      // In a real implementation, we would clear cache via API
      // For now, we'll just simulate it
      
      setTimeout(() => {
        setSuccess('Cache cleared successfully!');
        
        // Clear success message after 3 seconds
        setTimeout(() => {
          setSuccess(null);
        }, 3000);
        
        setLoading(false);
      }, 1000);
    } catch (error) {
      console.error('Error clearing cache:', error);
      setError('Failed to clear cache. Please try again later.');
      setLoading(false);
    }
  };
  
  if (loading && !settings.apiKey) {
    return <LoadingState>Loading settings...</LoadingState>;
  }
  
  return (
    <Container>
      <Header>
        <h2>Settings</h2>
        <p>Configure your AdMob test page settings</p>
      </Header>
      
      {error && <ErrorState>{error}</ErrorState>}
      {success && <SuccessState>{success}</SuccessState>}
      
      <SettingsCard>
        <h3>API Configuration</h3>
        
        <FormGroup>
          <label>API Key</label>
          <input 
            type="text" 
            name="apiKey" 
            value={settings.apiKey} 
            onChange={handleInputChange} 
            placeholder="Enter your API key"
          />
        </FormGroup>
        
        <FormGroup>
          <label>App Signature</label>
          <input 
            type="text" 
            name="appSignature" 
            value={settings.appSignature} 
            onChange={handleInputChange} 
            placeholder="Enter your app signature"
          />
        </FormGroup>
        
        <FormGroup>
          <label>Server Endpoint</label>
          <input 
            type="text" 
            name="serverEndpoint" 
            value={settings.serverEndpoint} 
            onChange={handleInputChange} 
            placeholder="Enter server endpoint URL"
          />
        </FormGroup>
        
        <SwitchContainer>
          <label>Test Mode</label>
          <Switch>
            <input 
              type="checkbox" 
              name="testMode" 
              checked={settings.testMode} 
              onChange={handleSwitchChange} 
            />
            <span className="slider"></span>
          </Switch>
        </SwitchContainer>
      </SettingsCard>
      
      <SettingsCard>
        <h3>Ad Configuration</h3>
        
        <FormRow>
          <div>
            <SwitchContainer>
              <label>Banner Ads</label>
              <Switch>
                <input 
                  type="checkbox" 
                  name="adTypes.banner" 
                  checked={settings.adTypes.banner} 
                  onChange={handleSwitchChange} 
                />
                <span className="slider"></span>
              </Switch>
            </SwitchContainer>
            
            <SwitchContainer>
              <label>Interstitial Ads</label>
              <Switch>
                <input 
                  type="checkbox" 
                  name="adTypes.interstitial" 
                  checked={settings.adTypes.interstitial} 
                  onChange={handleSwitchChange} 
                />
                <span className="slider"></span>
              </Switch>
            </SwitchContainer>
          </div>
          
          <div>
            <SwitchContainer>
              <label>Rewarded Ads</label>
              <Switch>
                <input 
                  type="checkbox" 
                  name="adTypes.rewarded" 
                  checked={settings.adTypes.rewarded} 
                  onChange={handleSwitchChange} 
                />
                <span className="slider"></span>
              </Switch>
            </SwitchContainer>
            
            <SwitchContainer>
              <label>Native Ads</label>
              <Switch>
                <input 
                  type="checkbox" 
                  name="adTypes.native" 
                  checked={settings.adTypes.native} 
                  onChange={handleSwitchChange} 
                />
                <span className="slider"></span>
              </Switch>
            </SwitchContainer>
          </div>
        </FormRow>
        
        <FormGroup>
          <label>Refresh Rate (seconds)</label>
          <input 
            type="number" 
            name="refreshRate" 
            value={settings.refreshRate} 
            onChange={handleInputChange} 
            min="0"
            max="300"
          />
        </FormGroup>
      </SettingsCard>
      
      <SettingsCard>
        <h3>Features</h3>
        
        <FormRow>
          <div>
            <SwitchContainer>
              <label>Analytics</label>
              <Switch>
                <input 
                  type="checkbox" 
                  name="analyticsEnabled" 
                  checked={settings.analyticsEnabled} 
                  onChange={handleSwitchChange} 
                />
                <span className="slider"></span>
              </Switch>
            </SwitchContainer>
            
            <SwitchContainer>
              <label>Fraud Detection</label>
              <Switch>
                <input 
                  type="checkbox" 
                  name="fraudDetectionEnabled" 
                  checked={settings.fraudDetectionEnabled} 
                  onChange={handleSwitchChange} 
                />
                <span className="slider"></span>
              </Switch>
            </SwitchContainer>
          </div>
          
          <div>
            <SwitchContainer>
              <label>A/B Testing</label>
              <Switch>
                <input 
                  type="checkbox" 
                  name="abTestingEnabled" 
                  checked={settings.abTestingEnabled} 
                  onChange={handleSwitchChange} 
                />
                <span className="slider"></span>
              </Switch>
            </SwitchContainer>
          </div>
        </FormRow>
        
        <FormGroup>
          <label>Log Level</label>
          <select 
            name="logLevel" 
            value={settings.logLevel} 
            onChange={handleInputChange}
          >
            <option value="debug">Debug</option>
            <option value="info">Info</option>
            <option value="warn">Warning</option>
            <option value="error">Error</option>
          </select>
        </FormGroup>
      </SettingsCard>
      
      <SettingsCard>
        <h3>Maintenance</h3>
        
        <ButtonGroup>
          <Button onClick={handleClearCache}>
            Clear Cache
          </Button>
        </ButtonGroup>
      </SettingsCard>
      
      <ButtonGroup>
        <Button primary onClick={handleSaveSettings}>
          Save Settings
        </Button>
        <Button onClick={handleResetSettings}>
          Reset to Default
        </Button>
      </ButtonGroup>
    </Container>
  );
};

export default Settings; 