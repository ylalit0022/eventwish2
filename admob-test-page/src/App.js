import React, { useState } from 'react';
import { Routes, Route, Link } from 'react-router-dom';
import styled from 'styled-components';

// Components
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import Dashboard from './components/Dashboard';
import AdConfigDisplay from './components/AdConfigDisplay';
import AdDisplay from './components/AdDisplay';
import AnalyticsDashboard from './components/AnalyticsDashboard';
import FraudDetection from './components/FraudDetection';
import ABTesting from './components/ABTesting';
import Settings from './components/Settings';

const AppContainer = styled.div`
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  transition: background-color 0.3s, color 0.3s;
`;

const MainContainer = styled.div`
  display: flex;
  flex: 1;
`;

const ContentContainer = styled.main`
  flex: 1;
  padding: 20px;
  overflow-y: auto;
`;

function App() {
  const [darkMode, setDarkMode] = useState(false);

  const toggleDarkMode = () => {
    setDarkMode(!darkMode);
  };

  return (
    <AppContainer className={darkMode ? 'dark-mode' : ''}>
      <Header darkMode={darkMode} toggleDarkMode={toggleDarkMode} />
      <MainContainer>
        <Sidebar darkMode={darkMode} />
        <ContentContainer>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/ad-config" element={<AdConfigDisplay />} />
            <Route path="/ad-display" element={<AdDisplay />} />
            <Route path="/analytics" element={<AnalyticsDashboard />} />
            <Route path="/fraud-detection" element={<FraudDetection />} />
            <Route path="/ab-testing" element={<ABTesting />} />
            <Route path="/settings" element={<Settings />} />
          </Routes>
        </ContentContainer>
      </MainContainer>
    </AppContainer>
  );
}

export default App; 