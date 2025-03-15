import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { Line, Bar, Pie } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import adMobApi from '../services/api';

// Register ChartJS components
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend
);

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

const FilterBar = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 20px;
  background-color: ${props => props.darkMode ? '#303134' : '#f8f9fa'};
  padding: 15px;
  border-radius: 8px;
`;

const FilterGroup = styled.div`
  display: flex;
  flex-direction: column;
  
  label {
    font-size: 12px;
    margin-bottom: 5px;
    color: ${props => props.darkMode ? '#9aa0a6' : '#5f6368'};
  }
  
  select, input {
    padding: 8px;
    border: 1px solid #ddd;
    border-radius: 4px;
    background-color: ${props => props.darkMode ? '#202124' : '#fff'};
    color: ${props => props.darkMode ? '#e8eaed' : '#3c4043'};
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
  align-self: flex-end;
  
  &:hover {
    background-color: ${props => props.primary ? '#3367d6' : '#e8eaed'};
  }
`;

const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
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

const ChartContainer = styled.div`
  background-color: ${props => props.darkMode ? '#303134' : '#fff'};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 30px;
  
  h3 {
    font-size: 18px;
    margin-bottom: 20px;
  }
`;

const ChartGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 30px;
  
  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
`;

const TableContainer = styled.div`
  background-color: ${props => props.darkMode ? '#303134' : '#fff'};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 30px;
  
  h3 {
    font-size: 18px;
    margin-bottom: 20px;
  }
  
  table {
    width: 100%;
    border-collapse: collapse;
    
    th, td {
      padding: 12px 15px;
      text-align: left;
      border-bottom: 1px solid ${props => props.darkMode ? '#5f6368' : '#f1f3f4'};
    }
    
    th {
      font-weight: 500;
      color: ${props => props.darkMode ? '#9aa0a6' : '#5f6368'};
      text-transform: uppercase;
      font-size: 12px;
    }
    
    td {
      color: ${props => props.darkMode ? '#e8eaed' : '#3c4043'};
    }
    
    tr:last-child td {
      border-bottom: none;
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

const AnalyticsDashboard = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState({
    impressions: 0,
    clicks: 0,
    ctr: 0,
    revenue: 0
  });
  const [timeRange, setTimeRange] = useState('7d');
  const [adType, setAdType] = useState('all');
  const [chartData, setChartData] = useState({
    impressions: [],
    clicks: [],
    revenue: []
  });
  const [adPerformance, setAdPerformance] = useState([]);
  
  useEffect(() => {
    const fetchAnalyticsData = async () => {
      try {
        setLoading(true);
        
        // In a real implementation, we would fetch this data from the API
        // For now, we'll use mock data
        
        // Simulate API call delay
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // Mock stats data
        setStats({
          impressions: 24567,
          clicks: 1432,
          ctr: 5.83,
          revenue: 2345.67
        });
        
        // Mock chart data
        const labels = generateDateLabels(timeRange);
        
        setChartData({
          labels,
          impressions: generateRandomData(labels.length, 1000, 5000),
          clicks: generateRandomData(labels.length, 50, 300),
          revenue: generateRandomData(labels.length, 100, 500, 2)
        });
        
        // Mock ad performance data
        setAdPerformance([
          {
            id: '1',
            name: 'Banner Ad - Home Screen',
            type: 'Banner',
            impressions: 12458,
            clicks: 623,
            ctr: 5.0,
            revenue: 845.32
          },
          {
            id: '2',
            name: 'Interstitial Ad - Level Complete',
            type: 'Interstitial',
            impressions: 5432,
            clicks: 487,
            ctr: 8.97,
            revenue: 1432.65
          },
          {
            id: '3',
            name: 'Rewarded Ad - Extra Lives',
            type: 'Rewarded',
            impressions: 3254,
            clicks: 287,
            ctr: 8.82,
            revenue: 965.43
          },
          {
            id: '4',
            name: 'Native Ad - Feed',
            type: 'Native',
            impressions: 3423,
            clicks: 35,
            ctr: 1.02,
            revenue: 102.27
          }
        ]);
        
        setLoading(false);
      } catch (error) {
        console.error('Error fetching analytics data:', error);
        setError('Failed to load analytics data. Please try again later.');
        setLoading(false);
      }
    };
    
    fetchAnalyticsData();
  }, [timeRange, adType]);
  
  const generateDateLabels = (range) => {
    const labels = [];
    const today = new Date();
    let days;
    
    switch (range) {
      case '7d':
        days = 7;
        break;
      case '30d':
        days = 30;
        break;
      case '90d':
        days = 90;
        break;
      default:
        days = 7;
    }
    
    for (let i = days - 1; i >= 0; i--) {
      const date = new Date(today);
      date.setDate(date.getDate() - i);
      labels.push(date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }));
    }
    
    return labels;
  };
  
  const generateRandomData = (length, min, max, decimals = 0) => {
    return Array.from({ length }, () => {
      const value = Math.random() * (max - min) + min;
      return decimals > 0 ? parseFloat(value.toFixed(decimals)) : Math.floor(value);
    });
  };
  
  const handleTimeRangeChange = (e) => {
    setTimeRange(e.target.value);
  };
  
  const handleAdTypeChange = (e) => {
    setAdType(e.target.value);
  };
  
  const handleRefresh = () => {
    // Refetch data
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
    }, 1000);
  };
  
  const impressionsChartData = {
    labels: chartData.labels,
    datasets: [
      {
        label: 'Impressions',
        data: chartData.impressions,
        borderColor: '#4285f4',
        backgroundColor: 'rgba(66, 133, 244, 0.1)',
        fill: true,
        tension: 0.4
      }
    ]
  };
  
  const clicksChartData = {
    labels: chartData.labels,
    datasets: [
      {
        label: 'Clicks',
        data: chartData.clicks,
        borderColor: '#34a853',
        backgroundColor: 'rgba(52, 168, 83, 0.1)',
        fill: true,
        tension: 0.4
      }
    ]
  };
  
  const revenueChartData = {
    labels: chartData.labels,
    datasets: [
      {
        label: 'Revenue ($)',
        data: chartData.revenue,
        borderColor: '#fbbc05',
        backgroundColor: 'rgba(251, 188, 5, 0.1)',
        fill: true,
        tension: 0.4
      }
    ]
  };
  
  const adTypeChartData = {
    labels: ['Banner', 'Interstitial', 'Rewarded', 'Native'],
    datasets: [
      {
        label: 'Impressions by Ad Type',
        data: [12458, 5432, 3254, 3423],
        backgroundColor: [
          '#4285f4',
          '#34a853',
          '#fbbc05',
          '#ea4335'
        ],
        borderWidth: 1
      }
    ]
  };
  
  const chartOptions = {
    responsive: true,
    plugins: {
      legend: {
        position: 'top',
      },
      title: {
        display: false
      }
    },
    scales: {
      y: {
        beginAtZero: true
      }
    }
  };
  
  if (loading && !chartData.labels) {
    return <LoadingState>Loading analytics data...</LoadingState>;
  }
  
  if (error) {
    return <ErrorState>{error}</ErrorState>;
  }
  
  return (
    <Container>
      <Header>
        <h2>Analytics Dashboard</h2>
        <p>Monitor your AdMob performance metrics</p>
      </Header>
      
      <FilterBar>
        <FilterGroup>
          <label>Time Range</label>
          <select value={timeRange} onChange={handleTimeRangeChange}>
            <option value="7d">Last 7 Days</option>
            <option value="30d">Last 30 Days</option>
            <option value="90d">Last 90 Days</option>
          </select>
        </FilterGroup>
        
        <FilterGroup>
          <label>Ad Type</label>
          <select value={adType} onChange={handleAdTypeChange}>
            <option value="all">All Types</option>
            <option value="banner">Banner</option>
            <option value="interstitial">Interstitial</option>
            <option value="rewarded">Rewarded</option>
            <option value="native">Native</option>
          </select>
        </FilterGroup>
        
        <Button primary onClick={handleRefresh}>
          Refresh Data
        </Button>
      </FilterBar>
      
      <StatsGrid>
        <StatCard>
          <h3>Impressions</h3>
          <div className="value">{stats.impressions.toLocaleString()}</div>
          <div className="change positive">+15.2% from previous period</div>
        </StatCard>
        
        <StatCard>
          <h3>Clicks</h3>
          <div className="value">{stats.clicks.toLocaleString()}</div>
          <div className="change positive">+8.7% from previous period</div>
        </StatCard>
        
        <StatCard>
          <h3>CTR</h3>
          <div className="value">{stats.ctr.toFixed(2)}%</div>
          <div className="change negative">-1.5% from previous period</div>
        </StatCard>
        
        <StatCard>
          <h3>Revenue</h3>
          <div className="value">${stats.revenue.toLocaleString()}</div>
          <div className="change positive">+12.3% from previous period</div>
        </StatCard>
      </StatsGrid>
      
      <ChartGrid>
        <ChartContainer>
          <h3>Impressions Over Time</h3>
          <Line data={impressionsChartData} options={chartOptions} />
        </ChartContainer>
        
        <ChartContainer>
          <h3>Clicks Over Time</h3>
          <Line data={clicksChartData} options={chartOptions} />
        </ChartContainer>
        
        <ChartContainer>
          <h3>Revenue Over Time</h3>
          <Line data={revenueChartData} options={chartOptions} />
        </ChartContainer>
        
        <ChartContainer>
          <h3>Impressions by Ad Type</h3>
          <Pie data={adTypeChartData} />
        </ChartContainer>
      </ChartGrid>
      
      <TableContainer>
        <h3>Ad Performance</h3>
        <table>
          <thead>
            <tr>
              <th>Ad Name</th>
              <th>Type</th>
              <th>Impressions</th>
              <th>Clicks</th>
              <th>CTR</th>
              <th>Revenue</th>
            </tr>
          </thead>
          <tbody>
            {adPerformance.map(ad => (
              <tr key={ad.id}>
                <td>{ad.name}</td>
                <td>{ad.type}</td>
                <td>{ad.impressions.toLocaleString()}</td>
                <td>{ad.clicks.toLocaleString()}</td>
                <td>{ad.ctr.toFixed(2)}%</td>
                <td>${ad.revenue.toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </TableContainer>
    </Container>
  );
};

export default AnalyticsDashboard; 