import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { Link } from 'react-router-dom';
import adMobApi from '../services/api';

const DashboardContainer = styled.div`
  padding: 20px;
`;

const DashboardHeader = styled.div`
  margin-bottom: 30px;
  
  h2 {
    font-size: 24px;
    margin-bottom: 10px;
  }
  
  p {
    color: #5f6368;
  }
`;

const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
`;

const StatCard = styled.div`
  background-color: ${props => props.darkMode ? '#303134' : '#fff'};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 20px;
  
  h3 {
    font-size: 14px;
    color: ${props => props.darkMode ? '#9aa0a6' : '#5f6368'};
    margin-bottom: 10px;
    text-transform: uppercase;
  }
  
  .value {
    font-size: 28px;
    font-weight: 500;
    color: ${props => props.darkMode ? '#e8eaed' : '#3c4043'};
  }
  
  .change {
    font-size: 14px;
    margin-top: 5px;
    
    &.positive {
      color: #34a853;
    }
    
    &.negative {
      color: #ea4335;
    }
  }
`;

const QuickActions = styled.div`
  margin-bottom: 30px;
  
  h3 {
    font-size: 18px;
    margin-bottom: 15px;
  }
`;

const ActionGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
`;

const ActionCard = styled(Link)`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: ${props => props.darkMode ? '#303134' : '#fff'};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 20px;
  text-decoration: none;
  transition: transform 0.2s, box-shadow 0.2s;
  
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.15);
  }
  
  h4 {
    font-size: 16px;
    margin-top: 10px;
    color: ${props => props.darkMode ? '#e8eaed' : '#3c4043'};
    text-align: center;
  }
  
  p {
    font-size: 14px;
    color: ${props => props.darkMode ? '#9aa0a6' : '#5f6368'};
    text-align: center;
    margin-top: 5px;
  }
`;

const RecentActivity = styled.div`
  margin-bottom: 30px;
  
  h3 {
    font-size: 18px;
    margin-bottom: 15px;
  }
`;

const ActivityList = styled.div`
  background-color: ${props => props.darkMode ? '#303134' : '#fff'};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  overflow: hidden;
`;

const ActivityItem = styled.div`
  display: flex;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid ${props => props.darkMode ? '#5f6368' : '#e8eaed'};
  
  &:last-child {
    border-bottom: none;
  }
  
  .activity-icon {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background-color: ${props => props.type === 'impression' ? '#4285f4' : props.type === 'click' ? '#34a853' : props.type === 'fraud' ? '#ea4335' : '#fbbc05'};
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-weight: bold;
    margin-right: 15px;
  }
  
  .activity-details {
    flex: 1;
    
    h4 {
      font-size: 16px;
      margin-bottom: 5px;
      color: ${props => props.darkMode ? '#e8eaed' : '#3c4043'};
    }
    
    p {
      font-size: 14px;
      color: ${props => props.darkMode ? '#9aa0a6' : '#5f6368'};
    }
  }
  
  .activity-time {
    font-size: 14px;
    color: ${props => props.darkMode ? '#9aa0a6' : '#5f6368'};
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

const Dashboard = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState({
    impressions: 0,
    clicks: 0,
    ctr: 0,
    revenue: 0
  });
  const [activities, setActivities] = useState([]);
  
  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setLoading(true);
        
        // In a real implementation, we would fetch this data from the API
        // For now, we'll use mock data
        
        // Simulate API call delay
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // Mock stats data
        setStats({
          impressions: 12458,
          clicks: 843,
          ctr: 6.77,
          revenue: 1245.87
        });
        
        // Mock activities data
        setActivities([
          {
            id: 1,
            type: 'impression',
            title: 'Banner Ad Impression',
            description: 'Banner ad was displayed on the home screen',
            time: '5 minutes ago'
          },
          {
            id: 2,
            type: 'click',
            title: 'Interstitial Ad Click',
            description: 'User clicked on interstitial ad',
            time: '15 minutes ago'
          },
          {
            id: 3,
            type: 'fraud',
            title: 'Potential Fraud Detected',
            description: 'Multiple rapid clicks from same device',
            time: '1 hour ago'
          },
          {
            id: 4,
            type: 'revenue',
            title: 'Revenue Update',
            description: 'Daily revenue calculation completed',
            time: '3 hours ago'
          },
          {
            id: 5,
            type: 'impression',
            title: 'Rewarded Ad Impression',
            description: 'Rewarded ad was displayed after level completion',
            time: '5 hours ago'
          }
        ]);
        
        setLoading(false);
      } catch (error) {
        console.error('Error fetching dashboard data:', error);
        setError('Failed to load dashboard data. Please try again later.');
        setLoading(false);
      }
    };
    
    fetchDashboardData();
  }, []);
  
  if (loading) {
    return <LoadingState>Loading dashboard data...</LoadingState>;
  }
  
  if (error) {
    return <ErrorState>{error}</ErrorState>;
  }
  
  return (
    <DashboardContainer>
      <DashboardHeader>
        <h2>AdMob Test Dashboard</h2>
        <p>Monitor and test your AdMob integration</p>
      </DashboardHeader>
      
      <StatsGrid>
        <StatCard>
          <h3>Impressions</h3>
          <div className="value">{stats.impressions.toLocaleString()}</div>
          <div className="change positive">+12.5% from last week</div>
        </StatCard>
        
        <StatCard>
          <h3>Clicks</h3>
          <div className="value">{stats.clicks.toLocaleString()}</div>
          <div className="change positive">+8.3% from last week</div>
        </StatCard>
        
        <StatCard>
          <h3>CTR</h3>
          <div className="value">{stats.ctr.toFixed(2)}%</div>
          <div className="change negative">-1.2% from last week</div>
        </StatCard>
        
        <StatCard>
          <h3>Revenue</h3>
          <div className="value">${stats.revenue.toLocaleString()}</div>
          <div className="change positive">+15.7% from last week</div>
        </StatCard>
      </StatsGrid>
      
      <QuickActions>
        <h3>Quick Actions</h3>
        <ActionGrid>
          <ActionCard to="/ad-display">
            <h4>Test Ad Display</h4>
            <p>View and test different ad formats</p>
          </ActionCard>
          
          <ActionCard to="/fraud-detection">
            <h4>Fraud Detection</h4>
            <p>Test fraud detection mechanisms</p>
          </ActionCard>
          
          <ActionCard to="/analytics">
            <h4>Analytics</h4>
            <p>View detailed analytics data</p>
          </ActionCard>
          
          <ActionCard to="/ab-testing">
            <h4>A/B Testing</h4>
            <p>Configure and monitor A/B tests</p>
          </ActionCard>
        </ActionGrid>
      </QuickActions>
      
      <RecentActivity>
        <h3>Recent Activity</h3>
        <ActivityList>
          {activities.map(activity => (
            <ActivityItem key={activity.id} type={activity.type}>
              <div className="activity-icon">
                {activity.type === 'impression' ? 'I' : 
                 activity.type === 'click' ? 'C' : 
                 activity.type === 'fraud' ? 'F' : 'R'}
              </div>
              <div className="activity-details">
                <h4>{activity.title}</h4>
                <p>{activity.description}</p>
              </div>
              <div className="activity-time">{activity.time}</div>
            </ActivityItem>
          ))}
        </ActivityList>
      </RecentActivity>
    </DashboardContainer>
  );
};

export default Dashboard; 