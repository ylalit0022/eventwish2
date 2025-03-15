import React from 'react';
import styled from 'styled-components';
import { Link } from 'react-router-dom';

const HeaderContainer = styled.header`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background-color: ${props => props.darkMode ? '#202124' : '#fff'};
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: background-color 0.3s;
`;

const Logo = styled.div`
  display: flex;
  align-items: center;
  
  h1 {
    font-size: 20px;
    margin: 0;
    margin-left: 10px;
    color: ${props => props.darkMode ? '#e8eaed' : '#4285f4'};
  }
`;

const LogoIcon = styled.div`
  width: 32px;
  height: 32px;
  background-color: #4285f4;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
`;

const Nav = styled.nav`
  display: flex;
  align-items: center;
`;

const NavLink = styled(Link)`
  margin-left: 20px;
  text-decoration: none;
  color: ${props => props.darkMode ? '#e8eaed' : '#5f6368'};
  font-weight: 500;
  
  &:hover {
    color: ${props => props.darkMode ? '#8ab4f8' : '#4285f4'};
  }
`;

const ThemeToggle = styled.button`
  background: none;
  border: none;
  cursor: pointer;
  margin-left: 20px;
  color: ${props => props.darkMode ? '#e8eaed' : '#5f6368'};
  
  &:hover {
    color: ${props => props.darkMode ? '#8ab4f8' : '#4285f4'};
  }
`;

const Header = ({ darkMode, toggleDarkMode }) => {
  return (
    <HeaderContainer darkMode={darkMode}>
      <Logo darkMode={darkMode}>
        <LogoIcon>A</LogoIcon>
        <h1>AdMob Test Page</h1>
      </Logo>
      <Nav>
        <NavLink to="/" darkMode={darkMode}>Dashboard</NavLink>
        <NavLink to="/ad-config" darkMode={darkMode}>Ad Config</NavLink>
        <NavLink to="/ad-display" darkMode={darkMode}>Ad Display</NavLink>
        <NavLink to="/analytics" darkMode={darkMode}>Analytics</NavLink>
        <NavLink to="/settings" darkMode={darkMode}>Settings</NavLink>
        <ThemeToggle onClick={toggleDarkMode} darkMode={darkMode}>
          {darkMode ? '☀️' : '🌙'}
        </ThemeToggle>
      </Nav>
    </HeaderContainer>
  );
};

export default Header; 