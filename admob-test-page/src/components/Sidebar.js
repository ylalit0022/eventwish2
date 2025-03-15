import React from 'react';
import styled from 'styled-components';
import { Link, useLocation } from 'react-router-dom';

const SidebarContainer = styled.aside`
  width: 250px;
  background-color: ${props => props.darkMode ? '#303134' : '#f8f9fa'};
  padding: 20px 0;
  transition: background-color 0.3s;
`;

const SidebarNav = styled.nav`
  display: flex;
  flex-direction: column;
`;

const SidebarLink = styled(Link)`
  display: flex;
  align-items: center;
  padding: 12px 20px;
  text-decoration: none;
  color: ${props => props.darkMode ? '#e8eaed' : '#3c4043'};
  font-weight: 500;
  border-left: 3px solid transparent;
  transition: background-color 0.3s, border-color 0.3s;
  
  ${props => props.active && `
    background-color: ${props.darkMode ? '#3c4043' : '#e8f0fe'};
    border-left-color: #4285f4;
    color: ${props.darkMode ? '#8ab4f8' : '#4285f4'};
  `}
  
  &:hover {
    background-color: ${props => props.darkMode ? '#3c4043' : '#e8f0fe'};
  }
`;

const SidebarSection = styled.div`
  margin-bottom: 20px;
`;

const SidebarSectionTitle = styled.h3`
  padding: 0 20px;
  margin: 20px 0 10px;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: ${props => props.darkMode ? '#9aa0a6' : '#5f6368'};
`;

const Sidebar = ({ darkMode }) => {
  const location = useLocation();
  
  return (
    <SidebarContainer darkMode={darkMode}>
      <SidebarNav>
        <SidebarSection>
          <SidebarSectionTitle darkMode={darkMode}>Main</SidebarSectionTitle>
          <SidebarLink 
            to="/" 
            darkMode={darkMode} 
            active={location.pathname === '/' ? 1 : 0}
          >
            Dashboard
          </SidebarLink>
        </SidebarSection>
        
        <SidebarSection>
          <SidebarSectionTitle darkMode={darkMode}>Ad Management</SidebarSectionTitle>
          <SidebarLink 
            to="/ad-config" 
            darkMode={darkMode} 
            active={location.pathname === '/ad-config' ? 1 : 0}
          >
            Ad Configuration
          </SidebarLink>
          <SidebarLink 
            to="/ad-display" 
            darkMode={darkMode} 
            active={location.pathname === '/ad-display' ? 1 : 0}
          >
            Ad Display
          </SidebarLink>
        </SidebarSection>
        
        <SidebarSection>
          <SidebarSectionTitle darkMode={darkMode}>Analytics</SidebarSectionTitle>
          <SidebarLink 
            to="/analytics" 
            darkMode={darkMode} 
            active={location.pathname === '/analytics' ? 1 : 0}
          >
            Analytics Dashboard
          </SidebarLink>
          <SidebarLink 
            to="/fraud-detection" 
            darkMode={darkMode} 
            active={location.pathname === '/fraud-detection' ? 1 : 0}
          >
            Fraud Detection
          </SidebarLink>
          <SidebarLink 
            to="/ab-testing" 
            darkMode={darkMode} 
            active={location.pathname === '/ab-testing' ? 1 : 0}
          >
            A/B Testing
          </SidebarLink>
        </SidebarSection>
        
        <SidebarSection>
          <SidebarSectionTitle darkMode={darkMode}>Settings</SidebarSectionTitle>
          <SidebarLink 
            to="/settings" 
            darkMode={darkMode} 
            active={location.pathname === '/settings' ? 1 : 0}
          >
            Settings
          </SidebarLink>
        </SidebarSection>
      </SidebarNav>
    </SidebarContainer>
  );
};

export default Sidebar; 