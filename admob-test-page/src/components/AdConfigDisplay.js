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

const ConfigSection = styled.div`
  margin-bottom: 30px;
`;

const ConfigCard = styled.div`
  background-color: ${props => props.darkMode ? '#303134' : '#fff'};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 20px;
  
  h3 {
    font-size: 18px;
    margin-bottom: 15px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  
  .badge {
    font-size: 12px;
    padding: 4px 8px;
    border-radius: 4px;
    background-color: ${props => props.active ? '#34a853' : '#ea4335'};
    color: white;
  }
`;

const ConfigDetails = styled.div`
  margin-top: 20px;
  
  .config-item {
    display: flex;
    margin-bottom: 10px;
    
    .label {
      width: 150px;
      font-weight: 500;
      color: ${props => props.darkMode ? '#9aa0a6' : '#5f6368'};
    }
    
    .value {
      flex: 1;
      color: ${props => props.darkMode ? '#e8eaed' : '#3c4043'};
    }
  }
`;

const ConfigActions = styled.div`
  display: flex;
  gap: 10px;
  margin-top: 20px;
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
  }
`;

const ContextForm = styled.div`
  background-color: ${props => props.darkMode ? '#303134' : '#fff'};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 20px;
  
  h3 {
    font-size: 18px;
    margin-bottom: 15px;
  }
  
  .form-row {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 15px;
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

const JsonViewer = styled.pre`
  background-color: ${props => props.darkMode ? '#202124' : '#f5f5f5'};
  border-radius: 4px;
  padding: 15px;
  overflow: auto;
  font-family: monospace;
  font-size: 14px;
  color: ${props => props.darkMode ? '#e8eaed' : '#3c4043'};
  max-height: 300px;
`;

const AdConfigDisplay = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [adConfigs, setAdConfigs] = useState([]);
  const [selectedConfig, setSelectedConfig] = useState(null);
  const [userContext, setUserContext] = useState({
    deviceType: 'MOBILE',
    platform: 'ANDROID',
    osVersion: '10',
    language: 'en',
    country: 'US',
    userId: 'test-user-' + Date.now(),
    userType: 'TEST',
    connectionType: 'WIFI'
  });
  
  useEffect(() => {
    const fetchAdConfigs = async () => {
      try {
        setLoading(true);
        
        // In a real implementation, we would fetch this data from the API
        // For now, we'll use mock data
        
        // Simulate API call delay
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // Mock ad configs data
        const mockConfigs = [
          {
            id: '1',
            adName: 'Banner Ad - Home Screen',
            adUnitCode: 'ca-app-pub-3940256099942544/6300978111',
            adType: 'Banner',
            status: true,
            parameters: {
              position: 'BOTTOM',
              refreshRate: '60',
              backgroundColor: '#FFFFFF'
            },
            targetingCriteria: {
              countries: ['US', 'CA', 'UK'],
              languages: ['en'],
              platforms: ['ANDROID', 'IOS'],
              minOsVersion: '9.0'
            },
            impressions: 8542,
            clicks: 423,
            ctr: 4.95,
            revenue: 845.32
          },
          {
            id: '2',
            adName: 'Interstitial Ad - Level Complete',
            adUnitCode: 'ca-app-pub-3940256099942544/1033173712',
            adType: 'Interstitial',
            status: true,
            parameters: {
              frequency: '3',
              timeout: '5'
            },
            targetingCriteria: {
              countries: ['US', 'CA', 'UK', 'AU'],
              languages: ['en'],
              platforms: ['ANDROID', 'IOS'],
              minOsVersion: '10.0'
            },
            impressions: 3254,
            clicks: 287,
            ctr: 8.82,
            revenue: 1432.65
          },
          {
            id: '3',
            adName: 'Rewarded Ad - Extra Lives',
            adUnitCode: 'ca-app-pub-3940256099942544/5224354917',
            adType: 'Rewarded',
            status: true,
            parameters: {
              rewardAmount: '3',
              rewardType: 'LIVES'
            },
            targetingCriteria: {
              countries: ['US', 'CA', 'UK', 'AU', 'DE', 'FR'],
              languages: ['en', 'de', 'fr'],
              platforms: ['ANDROID', 'IOS'],
              minOsVersion: '9.0'
            },
            impressions: 1876,
            clicks: 132,
            ctr: 7.04,
            revenue: 965.43
          },
          {
            id: '4',
            adName: 'Native Ad - Feed',
            adUnitCode: 'ca-app-pub-3940256099942544/2247696110',
            adType: 'Native',
            status: false,
            parameters: {
              template: 'MEDIUM',
              backgroundColor: '#F5F5F5'
            },
            targetingCriteria: {
              countries: ['US'],
              languages: ['en'],
              platforms: ['ANDROID'],
              minOsVersion: '10.0'
            },
            impressions: 0,
            clicks: 0,
            ctr: 0,
            revenue: 0
          }
        ];
        
        setAdConfigs(mockConfigs);
        setSelectedConfig(mockConfigs[0]);
        setLoading(false);
      } catch (error) {
        console.error('Error fetching ad configs:', error);
        setError('Failed to load ad configurations. Please try again later.');
        setLoading(false);
      }
    };
    
    fetchAdConfigs();
  }, []);
  
  const handleContextChange = (e) => {
    const { name, value } = e.target;
    setUserContext(prev => ({
      ...prev,
      [name]: value
    }));
  };
  
  const handleTestConfig = async (config) => {
    try {
      setLoading(true);
      
      // In a real implementation, we would call the API
      // For now, we'll simulate the API call
      
      // Simulate API call delay
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Mock response
      const response = {
        success: true,
        data: {
          id: config.id,
          adName: config.adName,
          adType: config.adType,
          adUnitCode: config.adUnitCode,
          parameters: config.parameters
        }
      };
      
      alert(`Ad configuration "${config.adName}" tested successfully!`);
      setLoading(false);
    } catch (error) {
      console.error('Error testing ad config:', error);
      setError('Failed to test ad configuration. Please try again later.');
      setLoading(false);
    }
  };
  
  if (loading && adConfigs.length === 0) {
    return <LoadingState>Loading ad configurations...</LoadingState>;
  }
  
  if (error) {
    return <ErrorState>{error}</ErrorState>;
  }
  
  return (
    <Container>
      <Header>
        <h2>Ad Configurations</h2>
        <p>View and test your AdMob configurations</p>
      </Header>
      
      <ContextForm>
        <h3>User Context</h3>
        <p>Configure the user context for testing ad configurations</p>
        
        <div className="form-row">
          <FormGroup>
            <label>Device Type</label>
            <select 
              name="deviceType" 
              value={userContext.deviceType} 
              onChange={handleContextChange}
            >
              <option value="MOBILE">Mobile</option>
              <option value="TABLET">Tablet</option>
              <option value="DESKTOP">Desktop</option>
            </select>
          </FormGroup>
          
          <FormGroup>
            <label>Platform</label>
            <select 
              name="platform" 
              value={userContext.platform} 
              onChange={handleContextChange}
            >
              <option value="ANDROID">Android</option>
              <option value="IOS">iOS</option>
              <option value="WEB">Web</option>
            </select>
          </FormGroup>
          
          <FormGroup>
            <label>OS Version</label>
            <input 
              type="text" 
              name="osVersion" 
              value={userContext.osVersion} 
              onChange={handleContextChange} 
            />
          </FormGroup>
          
          <FormGroup>
            <label>Language</label>
            <select 
              name="language" 
              value={userContext.language} 
              onChange={handleContextChange}
            >
              <option value="en">English</option>
              <option value="es">Spanish</option>
              <option value="fr">French</option>
              <option value="de">German</option>
              <option value="ja">Japanese</option>
              <option value="zh">Chinese</option>
            </select>
          </FormGroup>
          
          <FormGroup>
            <label>Country</label>
            <select 
              name="country" 
              value={userContext.country} 
              onChange={handleContextChange}
            >
              <option value="US">United States</option>
              <option value="CA">Canada</option>
              <option value="UK">United Kingdom</option>
              <option value="AU">Australia</option>
              <option value="DE">Germany</option>
              <option value="FR">France</option>
              <option value="JP">Japan</option>
              <option value="CN">China</option>
            </select>
          </FormGroup>
          
          <FormGroup>
            <label>Connection Type</label>
            <select 
              name="connectionType" 
              value={userContext.connectionType} 
              onChange={handleContextChange}
            >
              <option value="WIFI">WiFi</option>
              <option value="CELLULAR">Cellular</option>
              <option value="ETHERNET">Ethernet</option>
              <option value="UNKNOWN">Unknown</option>
            </select>
          </FormGroup>
        </div>
        
        <h4 style={{ marginTop: '20px', marginBottom: '10px' }}>Context JSON</h4>
        <JsonViewer>
          {JSON.stringify(userContext, null, 2)}
        </JsonViewer>
      </ContextForm>
      
      <ConfigSection>
        <h3>Available Ad Configurations</h3>
        
        {adConfigs.map(config => (
          <ConfigCard key={config.id} active={config.status}>
            <h3>
              {config.adName}
              <span className="badge">{config.status ? 'Active' : 'Inactive'}</span>
            </h3>
            <p>Type: {config.adType}</p>
            <p>Unit Code: {config.adUnitCode}</p>
            
            <ConfigDetails>
              <div className="config-item">
                <div className="label">Impressions:</div>
                <div className="value">{config.impressions.toLocaleString()}</div>
              </div>
              <div className="config-item">
                <div className="label">Clicks:</div>
                <div className="value">{config.clicks.toLocaleString()}</div>
              </div>
              <div className="config-item">
                <div className="label">CTR:</div>
                <div className="value">{config.ctr.toFixed(2)}%</div>
              </div>
              <div className="config-item">
                <div className="label">Revenue:</div>
                <div className="value">${config.revenue.toLocaleString()}</div>
              </div>
              
              <h4 style={{ marginTop: '20px', marginBottom: '10px' }}>Parameters</h4>
              <JsonViewer>
                {JSON.stringify(config.parameters, null, 2)}
              </JsonViewer>
              
              <h4 style={{ marginTop: '20px', marginBottom: '10px' }}>Targeting Criteria</h4>
              <JsonViewer>
                {JSON.stringify(config.targetingCriteria, null, 2)}
              </JsonViewer>
            </ConfigDetails>
            
            <ConfigActions>
              <Button 
                primary 
                onClick={() => handleTestConfig(config)}
                disabled={!config.status}
              >
                Test Configuration
              </Button>
              <Button onClick={() => setSelectedConfig(config)}>
                View Details
              </Button>
            </ConfigActions>
          </ConfigCard>
        ))}
      </ConfigSection>
    </Container>
  );
};

export default AdConfigDisplay; 