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

const AdTypeTabs = styled.div`
  display: flex;
  border-bottom: 1px solid #ddd;
  margin-bottom: 20px;
`;

const AdTypeTab = styled.button`
  padding: 10px 20px;
  background: none;
  border: none;
  border-bottom: 3px solid ${props => props.active ? '#4285f4' : 'transparent'};
  color: ${props => props.active ? '#4285f4' : '#5f6368'};
  font-weight: ${props => props.active ? '500' : 'normal'};
  cursor: pointer;
  transition: all 0.3s;
  
  &:hover {
    color: #4285f4;
  }
`;

const AdContainer = styled.div`
  margin-bottom: 30px;
`;

const AdPreview = styled.div`
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

const BannerAd = styled.div`
  width: 320px;
  height: 50px;
  background-color: #4285f4;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin: 20px auto;
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
    animation: shimmer 2s infinite;
  }
  
  @keyframes shimmer {
    0% {
      left: -100%;
    }
    100% {
      left: 100%;
    }
  }
`;

const InterstitialAd = styled.div`
  width: 100%;
  max-width: 320px;
  height: 480px;
  background-color: #34a853;
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin: 20px auto;
  position: relative;
  
  .close-button {
    position: absolute;
    top: 10px;
    right: 10px;
    width: 24px;
    height: 24px;
    background-color: rgba(0, 0, 0, 0.3);
    color: white;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
  }
  
  .ad-content {
    text-align: center;
    padding: 20px;
  }
  
  .cta-button {
    margin-top: 20px;
    padding: 10px 20px;
    background-color: white;
    color: #34a853;
    border: none;
    border-radius: 4px;
    font-weight: bold;
    cursor: pointer;
  }
`;

const RewardedAd = styled.div`
  width: 100%;
  max-width: 320px;
  height: 480px;
  background-color: #fbbc05;
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin: 20px auto;
  position: relative;
  
  .close-button {
    position: absolute;
    top: 10px;
    right: 10px;
    width: 24px;
    height: 24px;
    background-color: rgba(0, 0, 0, 0.3);
    color: white;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
  }
  
  .ad-content {
    text-align: center;
    padding: 20px;
  }
  
  .reward-info {
    margin-top: 20px;
    padding: 10px;
    background-color: rgba(0, 0, 0, 0.1);
    border-radius: 4px;
  }
  
  .cta-button {
    margin-top: 20px;
    padding: 10px 20px;
    background-color: white;
    color: #fbbc05;
    border: none;
    border-radius: 4px;
    font-weight: bold;
    cursor: pointer;
  }
  
  .timer {
    position: absolute;
    bottom: 20px;
    font-size: 14px;
  }
`;

const NativeAd = styled.div`
  width: 100%;
  max-width: 320px;
  background-color: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  overflow: hidden;
  margin: 20px auto;
  
  .ad-header {
    display: flex;
    align-items: center;
    padding: 10px;
    
    .ad-icon {
      width: 40px;
      height: 40px;
      background-color: #4285f4;
      border-radius: 4px;
      margin-right: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-weight: bold;
    }
    
    .ad-title {
      flex: 1;
      
      h4 {
        margin: 0;
        font-size: 16px;
      }
      
      p {
        margin: 0;
        font-size: 12px;
        color: #5f6368;
      }
    }
    
    .ad-badge {
      font-size: 10px;
      padding: 2px 4px;
      background-color: #f1f3f4;
      color: #5f6368;
      border-radius: 2px;
    }
  }
  
  .ad-image {
    width: 100%;
    height: 180px;
    background-color: #e8f0fe;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #4285f4;
    font-weight: bold;
  }
  
  .ad-description {
    padding: 10px;
    font-size: 14px;
    color: #3c4043;
  }
  
  .ad-cta {
    padding: 10px;
    
    button {
      width: 100%;
      padding: 8px;
      background-color: #4285f4;
      color: white;
      border: none;
      border-radius: 4px;
      font-weight: bold;
      cursor: pointer;
    }
  }
`;

const AdControls = styled.div`
  background-color: ${props => props.darkMode ? '#303134' : '#f8f9fa'};
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
`;

const ControlsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
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

const ButtonGroup = styled.div`
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
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

const EventLog = styled.div`
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
  max-height: 200px;
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
      
      &.load { color: #4285f4; }
      &.impression { color: #34a853; }
      &.click { color: #ea4335; }
      &.reward { color: #fbbc05; }
      &.error { color: #ea4335; }
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

const AdDisplay = () => {
  const [activeAdType, setActiveAdType] = useState('banner');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [adLoaded, setAdLoaded] = useState(false);
  const [adConfig, setAdConfig] = useState(null);
  const [showInterstitial, setShowInterstitial] = useState(false);
  const [showRewarded, setShowRewarded] = useState(false);
  const [rewardedTimer, setRewardedTimer] = useState(0);
  const [logEntries, setLogEntries] = useState([]);
  const [adSettings, setAdSettings] = useState({
    position: 'bottom',
    backgroundColor: '#FFFFFF',
    textColor: '#000000',
    refreshRate: 60
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
  
  const handleAdTypeChange = (type) => {
    setActiveAdType(type);
    setAdLoaded(false);
    setShowInterstitial(false);
    setShowRewarded(false);
    setAdConfig(null);
  };
  
  const handleSettingChange = (e) => {
    const { name, value } = e.target;
    setAdSettings(prev => ({
      ...prev,
      [name]: value
    }));
  };
  
  const loadAd = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // In a real implementation, we would call the API
      // For now, we'll simulate the API call
      
      // Simulate API call delay
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Mock ad config based on ad type
      let mockConfig;
      
      switch (activeAdType) {
        case 'banner':
          mockConfig = {
            id: '1',
            adName: 'Banner Ad - Test',
            adType: 'Banner',
            adUnitCode: 'ca-app-pub-3940256099942544/6300978111',
            parameters: {
              position: adSettings.position,
              backgroundColor: adSettings.backgroundColor,
              textColor: adSettings.textColor,
              refreshRate: adSettings.refreshRate
            }
          };
          break;
        case 'interstitial':
          mockConfig = {
            id: '2',
            adName: 'Interstitial Ad - Test',
            adType: 'Interstitial',
            adUnitCode: 'ca-app-pub-3940256099942544/1033173712',
            parameters: {
              frequency: '3',
              timeout: '5'
            }
          };
          break;
        case 'rewarded':
          mockConfig = {
            id: '3',
            adName: 'Rewarded Ad - Test',
            adType: 'Rewarded',
            adUnitCode: 'ca-app-pub-3940256099942544/5224354917',
            parameters: {
              rewardAmount: '3',
              rewardType: 'COINS'
            }
          };
          break;
        case 'native':
          mockConfig = {
            id: '4',
            adName: 'Native Ad - Test',
            adType: 'Native',
            adUnitCode: 'ca-app-pub-3940256099942544/2247696110',
            parameters: {
              template: 'MEDIUM',
              backgroundColor: adSettings.backgroundColor
            }
          };
          break;
        default:
          mockConfig = {
            id: '1',
            adName: 'Banner Ad - Test',
            adType: 'Banner',
            adUnitCode: 'ca-app-pub-3940256099942544/6300978111',
            parameters: {
              position: adSettings.position,
              backgroundColor: adSettings.backgroundColor,
              textColor: adSettings.textColor,
              refreshRate: adSettings.refreshRate
            }
          };
      }
      
      setAdConfig(mockConfig);
      setAdLoaded(true);
      addLogEntry('load', `${mockConfig.adType} ad loaded: ${mockConfig.adName}`);
      
      setLoading(false);
    } catch (error) {
      console.error('Error loading ad:', error);
      setError('Failed to load ad. Please try again later.');
      setLoading(false);
      addLogEntry('error', `Failed to load ad: ${error.message}`);
    }
  };
  
  const showAd = () => {
    if (activeAdType === 'interstitial') {
      setShowInterstitial(true);
      addLogEntry('impression', `Interstitial ad shown: ${adConfig.adName}`);
    } else if (activeAdType === 'rewarded') {
      setShowRewarded(true);
      setRewardedTimer(5);
      addLogEntry('impression', `Rewarded ad shown: ${adConfig.adName}`);
    }
  };
  
  const closeInterstitial = () => {
    setShowInterstitial(false);
    addLogEntry('close', `Interstitial ad closed: ${adConfig.adName}`);
  };
  
  const closeRewarded = (completed = false) => {
    setShowRewarded(false);
    
    if (completed) {
      addLogEntry('reward', `Reward earned: ${adConfig.parameters.rewardAmount} ${adConfig.parameters.rewardType}`);
    } else {
      addLogEntry('close', `Rewarded ad closed without completion: ${adConfig.adName}`);
    }
  };
  
  const trackImpression = () => {
    if (!adConfig) return;
    
    addLogEntry('impression', `Impression tracked for: ${adConfig.adName}`);
  };
  
  const trackClick = () => {
    if (!adConfig) return;
    
    addLogEntry('click', `Click tracked for: ${adConfig.adName}`);
  };
  
  // Effect for rewarded ad timer
  useEffect(() => {
    let timer;
    
    if (showRewarded && rewardedTimer > 0) {
      timer = setTimeout(() => {
        setRewardedTimer(prev => prev - 1);
      }, 1000);
    } else if (showRewarded && rewardedTimer === 0) {
      closeRewarded(true);
    }
    
    return () => {
      if (timer) clearTimeout(timer);
    };
  }, [showRewarded, rewardedTimer]);
  
  return (
    <Container>
      <Header>
        <h2>Ad Display</h2>
        <p>Test and preview different ad formats</p>
      </Header>
      
      <AdTypeTabs>
        <AdTypeTab 
          active={activeAdType === 'banner'} 
          onClick={() => handleAdTypeChange('banner')}
        >
          Banner
        </AdTypeTab>
        <AdTypeTab 
          active={activeAdType === 'interstitial'} 
          onClick={() => handleAdTypeChange('interstitial')}
        >
          Interstitial
        </AdTypeTab>
        <AdTypeTab 
          active={activeAdType === 'rewarded'} 
          onClick={() => handleAdTypeChange('rewarded')}
        >
          Rewarded
        </AdTypeTab>
        <AdTypeTab 
          active={activeAdType === 'native'} 
          onClick={() => handleAdTypeChange('native')}
        >
          Native
        </AdTypeTab>
      </AdTypeTabs>
      
      <AdControls>
        <h3>Ad Settings</h3>
        <p>Configure settings for the selected ad type</p>
        
        <ControlsGrid>
          {activeAdType === 'banner' && (
            <>
              <FormGroup>
                <label>Position</label>
                <select 
                  name="position" 
                  value={adSettings.position} 
                  onChange={handleSettingChange}
                >
                  <option value="top">Top</option>
                  <option value="bottom">Bottom</option>
                </select>
              </FormGroup>
              
              <FormGroup>
                <label>Background Color</label>
                <input 
                  type="color" 
                  name="backgroundColor" 
                  value={adSettings.backgroundColor} 
                  onChange={handleSettingChange} 
                />
              </FormGroup>
              
              <FormGroup>
                <label>Text Color</label>
                <input 
                  type="color" 
                  name="textColor" 
                  value={adSettings.textColor} 
                  onChange={handleSettingChange} 
                />
              </FormGroup>
              
              <FormGroup>
                <label>Refresh Rate (seconds)</label>
                <input 
                  type="number" 
                  name="refreshRate" 
                  value={adSettings.refreshRate} 
                  onChange={handleSettingChange} 
                  min="0" 
                  max="300" 
                />
              </FormGroup>
            </>
          )}
        </ControlsGrid>
        
        <ButtonGroup>
          <Button primary onClick={loadAd} disabled={loading}>
            {loading ? 'Loading...' : 'Load Ad'}
          </Button>
          
          {adLoaded && (activeAdType === 'interstitial' || activeAdType === 'rewarded') && (
            <Button onClick={showAd}>
              Show Ad
            </Button>
          )}
          
          {adLoaded && (
            <>
              <Button onClick={trackImpression}>
                Track Impression
              </Button>
              <Button onClick={trackClick}>
                Track Click
              </Button>
            </>
          )}
        </ButtonGroup>
      </AdControls>
      
      {error && <ErrorState>{error}</ErrorState>}
      
      <AdContainer>
        <AdPreview>
          <h3>Ad Preview</h3>
          
          {!adLoaded ? (
            <div style={{ textAlign: 'center', padding: '20px' }}>
              {loading ? 'Loading ad...' : 'Load an ad to preview it here'}
            </div>
          ) : (
            <>
              {activeAdType === 'banner' && (
                <BannerAd style={{ backgroundColor: adSettings.backgroundColor, color: adSettings.textColor }}>
                  BANNER AD - {adConfig.adUnitCode}
                </BannerAd>
              )}
              
              {activeAdType === 'native' && (
                <NativeAd>
                  <div className="ad-header">
                    <div className="ad-icon">Ad</div>
                    <div className="ad-title">
                      <h4>Test Native Ad</h4>
                      <p>Advertiser Name</p>
                    </div>
                    <div className="ad-badge">Ad</div>
                  </div>
                  <div className="ad-image">AD IMAGE</div>
                  <div className="ad-description">
                    This is a test native ad for the AdMob integration. Native ads blend with your app's content.
                  </div>
                  <div className="ad-cta">
                    <button onClick={trackClick}>INSTALL NOW</button>
                  </div>
                </NativeAd>
              )}
              
              {showInterstitial && (
                <div style={{ 
                  position: 'fixed', 
                  top: 0, 
                  left: 0, 
                  width: '100%', 
                  height: '100%', 
                  backgroundColor: 'rgba(0, 0, 0, 0.8)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  zIndex: 1000
                }}>
                  <InterstitialAd>
                    <div className="close-button" onClick={closeInterstitial}>✕</div>
                    <div className="ad-content">
                      <h3>INTERSTITIAL AD</h3>
                      <p>{adConfig.adUnitCode}</p>
                      <button className="cta-button" onClick={() => {
                        trackClick();
                        closeInterstitial();
                      }}>
                        INSTALL NOW
                      </button>
                    </div>
                  </InterstitialAd>
                </div>
              )}
              
              {showRewarded && (
                <div style={{ 
                  position: 'fixed', 
                  top: 0, 
                  left: 0, 
                  width: '100%', 
                  height: '100%', 
                  backgroundColor: 'rgba(0, 0, 0, 0.8)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  zIndex: 1000
                }}>
                  <RewardedAd>
                    <div className="close-button" onClick={() => closeRewarded(false)}>✕</div>
                    <div className="ad-content">
                      <h3>REWARDED AD</h3>
                      <p>{adConfig.adUnitCode}</p>
                      <div className="reward-info">
                        Watch to earn {adConfig.parameters.rewardAmount} {adConfig.parameters.rewardType}
                      </div>
                      <button className="cta-button" onClick={() => {
                        trackClick();
                      }}>
                        LEARN MORE
                      </button>
                    </div>
                    <div className="timer">
                      {rewardedTimer > 0 ? `Reward in ${rewardedTimer}s` : 'Reward earned!'}
                    </div>
                  </RewardedAd>
                </div>
              )}
            </>
          )}
        </AdPreview>
      </AdContainer>
      
      <EventLog>
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
      </EventLog>
    </Container>
  );
};

export default AdDisplay; 