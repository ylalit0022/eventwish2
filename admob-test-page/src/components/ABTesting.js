import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { Bar } from 'react-chartjs-2';
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

const TestsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
`;

const TestCard = styled.div`
  background-color: ${props => props.darkMode ? '#303134' : '#fff'};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 20px;
  
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
    background-color: ${props => props.status === 'active' ? '#34a853' : props.status === 'completed' ? '#4285f4' : '#ea4335'};
    color: white;
  }
`;

const TestDetails = styled.div`
  margin-top: 20px;
  
  .test-item {
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

const TestActions = styled.div`
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
    background-color: ${props => props.primary ? '#a4c2f4' : '#f1f3f4'};
    cursor: not-allowed;
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

const VariantGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
`;

const VariantCard = styled.div`
  background-color: ${props => props.darkMode ? '#303134' : '#fff'};
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 20px;
  
  h4 {
    font-size: 16px;
    margin-bottom: 15px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  
  .badge {
    font-size: 12px;
    padding: 4px 8px;
    border-radius: 4px;
    background-color: ${props => props.isWinner ? '#34a853' : '#4285f4'};
    color: white;
  }
`;

const VariantDetails = styled.div`
  margin-top: 20px;
  
  .variant-item {
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

const CreateTestForm = styled.div`
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
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
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

const ABTesting = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [tests, setTests] = useState([]);
  const [selectedTest, setSelectedTest] = useState(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newTest, setNewTest] = useState({
    name: '',
    description: '',
    adType: 'banner',
    targetMetric: 'ctr',
    duration: 7,
    trafficAllocation: 50,
    variants: [
      { name: 'Control', description: 'Original ad configuration' },
      { name: 'Variant A', description: 'Test variation' }
    ]
  });
  
  useEffect(() => {
    const fetchABTests = async () => {
      try {
        setLoading(true);
        
        // In a real implementation, we would fetch this data from the API
        // For now, we'll use mock data
        
        // Simulate API call delay
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // Mock A/B tests data
        const mockTests = [
          {
            id: '1',
            name: 'Banner Position Test',
            description: 'Testing different banner positions on the home screen',
            status: 'active',
            startDate: '2023-11-01',
            endDate: '2023-11-15',
            adType: 'banner',
            targetMetric: 'ctr',
            trafficAllocation: 50,
            variants: [
              {
                id: 'v1',
                name: 'Control',
                description: 'Banner at the bottom of the screen',
                isControl: true,
                isWinner: false,
                impressions: 5432,
                clicks: 243,
                ctr: 4.47,
                revenue: 432.56
              },
              {
                id: 'v2',
                name: 'Variant A',
                description: 'Banner at the top of the screen',
                isControl: false,
                isWinner: false,
                impressions: 5387,
                clicks: 312,
                ctr: 5.79,
                revenue: 498.32
              }
            ]
          },
          {
            id: '2',
            name: 'Interstitial Frequency Test',
            description: 'Testing different frequencies for interstitial ads',
            status: 'completed',
            startDate: '2023-10-15',
            endDate: '2023-10-31',
            adType: 'interstitial',
            targetMetric: 'revenue',
            trafficAllocation: 100,
            variants: [
              {
                id: 'v1',
                name: 'Control',
                description: 'Show interstitial every 3 levels',
                isControl: true,
                isWinner: false,
                impressions: 3245,
                clicks: 287,
                ctr: 8.84,
                revenue: 865.43
              },
              {
                id: 'v2',
                name: 'Variant A',
                description: 'Show interstitial every 5 levels',
                isControl: false,
                isWinner: true,
                impressions: 2187,
                clicks: 198,
                ctr: 9.05,
                revenue: 932.76
              }
            ]
          },
          {
            id: '3',
            name: 'Rewarded Ad Value Test',
            description: 'Testing different reward values for rewarded ads',
            status: 'scheduled',
            startDate: '2023-11-20',
            endDate: '2023-12-05',
            adType: 'rewarded',
            targetMetric: 'impressions',
            trafficAllocation: 75,
            variants: [
              {
                id: 'v1',
                name: 'Control',
                description: '3 coins per rewarded ad',
                isControl: true,
                isWinner: false,
                impressions: 0,
                clicks: 0,
                ctr: 0,
                revenue: 0
              },
              {
                id: 'v2',
                name: 'Variant A',
                description: '5 coins per rewarded ad',
                isControl: false,
                isWinner: false,
                impressions: 0,
                clicks: 0,
                ctr: 0,
                revenue: 0
              }
            ]
          }
        ];
        
        setTests(mockTests);
        setSelectedTest(mockTests[0]);
        setLoading(false);
      } catch (error) {
        console.error('Error fetching A/B tests:', error);
        setError('Failed to load A/B tests. Please try again later.');
        setLoading(false);
      }
    };
    
    fetchABTests();
  }, []);
  
  const handleTestSelect = (test) => {
    setSelectedTest(test);
  };
  
  const handleCreateTest = () => {
    setShowCreateForm(true);
  };
  
  const handleCancelCreate = () => {
    setShowCreateForm(false);
  };
  
  const handleNewTestChange = (e) => {
    const { name, value } = e.target;
    setNewTest(prev => ({
      ...prev,
      [name]: value
    }));
  };
  
  const handleSubmitNewTest = async () => {
    try {
      setLoading(true);
      
      // In a real implementation, we would call the API
      // For now, we'll simulate the API call
      
      // Simulate API call delay
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Create a new test object
      const newTestObj = {
        id: `${tests.length + 1}`,
        ...newTest,
        status: 'scheduled',
        startDate: new Date().toISOString().split('T')[0],
        endDate: new Date(Date.now() + newTest.duration * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
        variants: newTest.variants.map((variant, index) => ({
          id: `v${index + 1}`,
          ...variant,
          isControl: index === 0,
          isWinner: false,
          impressions: 0,
          clicks: 0,
          ctr: 0,
          revenue: 0
        }))
      };
      
      // Add the new test to the list
      setTests(prev => [...prev, newTestObj]);
      
      // Reset the form
      setNewTest({
        name: '',
        description: '',
        adType: 'banner',
        targetMetric: 'ctr',
        duration: 7,
        trafficAllocation: 50,
        variants: [
          { name: 'Control', description: 'Original ad configuration' },
          { name: 'Variant A', description: 'Test variation' }
        ]
      });
      
      setShowCreateForm(false);
      setSuccess('A/B test created successfully!');
      
      // Clear success message after 3 seconds
      setTimeout(() => {
        setSuccess(null);
      }, 3000);
      
      setLoading(false);
    } catch (error) {
      console.error('Error creating A/B test:', error);
      setError('Failed to create A/B test. Please try again later.');
      setLoading(false);
    }
  };
  
  const handleStartTest = async (testId) => {
    try {
      setLoading(true);
      
      // In a real implementation, we would call the API
      // For now, we'll simulate the API call
      
      // Simulate API call delay
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Update the test status
      setTests(prev => prev.map(test => 
        test.id === testId ? { ...test, status: 'active' } : test
      ));
      
      if (selectedTest && selectedTest.id === testId) {
        setSelectedTest(prev => ({ ...prev, status: 'active' }));
      }
      
      setSuccess('A/B test started successfully!');
      
      // Clear success message after 3 seconds
      setTimeout(() => {
        setSuccess(null);
      }, 3000);
      
      setLoading(false);
    } catch (error) {
      console.error('Error starting A/B test:', error);
      setError('Failed to start A/B test. Please try again later.');
      setLoading(false);
    }
  };
  
  const handleStopTest = async (testId) => {
    try {
      setLoading(true);
      
      // In a real implementation, we would call the API
      // For now, we'll simulate the API call
      
      // Simulate API call delay
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Update the test status
      setTests(prev => prev.map(test => 
        test.id === testId ? { ...test, status: 'completed' } : test
      ));
      
      if (selectedTest && selectedTest.id === testId) {
        setSelectedTest(prev => ({ ...prev, status: 'completed' }));
      }
      
      setSuccess('A/B test stopped successfully!');
      
      // Clear success message after 3 seconds
      setTimeout(() => {
        setSuccess(null);
      }, 3000);
      
      setLoading(false);
    } catch (error) {
      console.error('Error stopping A/B test:', error);
      setError('Failed to stop A/B test. Please try again later.');
      setLoading(false);
    }
  };
  
  const handleDeclareWinner = async (testId, variantId) => {
    try {
      setLoading(true);
      
      // In a real implementation, we would call the API
      // For now, we'll simulate the API call
      
      // Simulate API call delay
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Update the test variants
      setTests(prev => prev.map(test => 
        test.id === testId ? {
          ...test,
          status: 'completed',
          variants: test.variants.map(variant => ({
            ...variant,
            isWinner: variant.id === variantId
          }))
        } : test
      ));
      
      if (selectedTest && selectedTest.id === testId) {
        setSelectedTest(prev => ({
          ...prev,
          status: 'completed',
          variants: prev.variants.map(variant => ({
            ...variant,
            isWinner: variant.id === variantId
          }))
        }));
      }
      
      setSuccess('Winner declared successfully!');
      
      // Clear success message after 3 seconds
      setTimeout(() => {
        setSuccess(null);
      }, 3000);
      
      setLoading(false);
    } catch (error) {
      console.error('Error declaring winner:', error);
      setError('Failed to declare winner. Please try again later.');
      setLoading(false);
    }
  };
  
  const getChartData = (test) => {
    if (!test) return null;
    
    const labels = test.variants.map(variant => variant.name);
    
    const impressionsData = {
      labels,
      datasets: [
        {
          label: 'Impressions',
          data: test.variants.map(variant => variant.impressions),
          backgroundColor: '#4285f4',
        }
      ]
    };
    
    const clicksData = {
      labels,
      datasets: [
        {
          label: 'Clicks',
          data: test.variants.map(variant => variant.clicks),
          backgroundColor: '#34a853',
        }
      ]
    };
    
    const ctrData = {
      labels,
      datasets: [
        {
          label: 'CTR (%)',
          data: test.variants.map(variant => variant.ctr),
          backgroundColor: '#fbbc05',
        }
      ]
    };
    
    const revenueData = {
      labels,
      datasets: [
        {
          label: 'Revenue ($)',
          data: test.variants.map(variant => variant.revenue),
          backgroundColor: '#ea4335',
        }
      ]
    };
    
    return {
      impressions: impressionsData,
      clicks: clicksData,
      ctr: ctrData,
      revenue: revenueData
    };
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
  
  if (loading && tests.length === 0) {
    return <LoadingState>Loading A/B tests...</LoadingState>;
  }
  
  return (
    <Container>
      <Header>
        <h2>A/B Testing</h2>
        <p>Create and monitor A/B tests for your AdMob ads</p>
      </Header>
      
      {error && <ErrorState>{error}</ErrorState>}
      {success && <SuccessState>{success}</SuccessState>}
      
      <TestActions style={{ marginBottom: '20px' }}>
        <Button primary onClick={handleCreateTest}>
          Create New Test
        </Button>
      </TestActions>
      
      {showCreateForm && (
        <CreateTestForm>
          <h3>Create New A/B Test</h3>
          
          <FormGroup>
            <label>Test Name</label>
            <input 
              type="text" 
              name="name" 
              value={newTest.name} 
              onChange={handleNewTestChange} 
              placeholder="Enter test name"
            />
          </FormGroup>
          
          <FormGroup>
            <label>Description</label>
            <textarea 
              name="description" 
              value={newTest.description} 
              onChange={handleNewTestChange} 
              placeholder="Enter test description"
              rows="3"
            />
          </FormGroup>
          
          <FormRow>
            <FormGroup>
              <label>Ad Type</label>
              <select 
                name="adType" 
                value={newTest.adType} 
                onChange={handleNewTestChange}
              >
                <option value="banner">Banner</option>
                <option value="interstitial">Interstitial</option>
                <option value="rewarded">Rewarded</option>
                <option value="native">Native</option>
              </select>
            </FormGroup>
            
            <FormGroup>
              <label>Target Metric</label>
              <select 
                name="targetMetric" 
                value={newTest.targetMetric} 
                onChange={handleNewTestChange}
              >
                <option value="impressions">Impressions</option>
                <option value="clicks">Clicks</option>
                <option value="ctr">CTR</option>
                <option value="revenue">Revenue</option>
              </select>
            </FormGroup>
            
            <FormGroup>
              <label>Duration (days)</label>
              <input 
                type="number" 
                name="duration" 
                value={newTest.duration} 
                onChange={handleNewTestChange} 
                min="1"
                max="30"
              />
            </FormGroup>
            
            <FormGroup>
              <label>Traffic Allocation (%)</label>
              <input 
                type="number" 
                name="trafficAllocation" 
                value={newTest.trafficAllocation} 
                onChange={handleNewTestChange} 
                min="1"
                max="100"
              />
            </FormGroup>
          </FormRow>
          
          <h4 style={{ marginTop: '20px', marginBottom: '10px' }}>Variants</h4>
          
          {newTest.variants.map((variant, index) => (
            <div key={index} style={{ marginBottom: '15px', padding: '15px', border: '1px solid #ddd', borderRadius: '4px' }}>
              <FormGroup>
                <label>{index === 0 ? 'Control Variant Name' : `Variant ${index} Name`}</label>
                <input 
                  type="text" 
                  value={variant.name} 
                  onChange={(e) => {
                    const updatedVariants = [...newTest.variants];
                    updatedVariants[index].name = e.target.value;
                    setNewTest(prev => ({ ...prev, variants: updatedVariants }));
                  }} 
                  placeholder="Enter variant name"
                />
              </FormGroup>
              
              <FormGroup>
                <label>Description</label>
                <textarea 
                  value={variant.description} 
                  onChange={(e) => {
                    const updatedVariants = [...newTest.variants];
                    updatedVariants[index].description = e.target.value;
                    setNewTest(prev => ({ ...prev, variants: updatedVariants }));
                  }} 
                  placeholder="Enter variant description"
                  rows="2"
                />
              </FormGroup>
            </div>
          ))}
          
          <TestActions>
            <Button 
              primary 
              onClick={handleSubmitNewTest}
              disabled={!newTest.name || !newTest.description}
            >
              Create Test
            </Button>
            <Button onClick={handleCancelCreate}>
              Cancel
            </Button>
          </TestActions>
        </CreateTestForm>
      )}
      
      <TestsGrid>
        {tests.map(test => (
          <TestCard key={test.id} status={test.status}>
            <h3>
              {test.name}
              <span className="badge">{test.status}</span>
            </h3>
            <p>{test.description}</p>
            
            <TestDetails>
              <div className="test-item">
                <div className="label">Ad Type:</div>
                <div className="value">{test.adType.charAt(0).toUpperCase() + test.adType.slice(1)}</div>
              </div>
              <div className="test-item">
                <div className="label">Target Metric:</div>
                <div className="value">{test.targetMetric.toUpperCase()}</div>
              </div>
              <div className="test-item">
                <div className="label">Start Date:</div>
                <div className="value">{test.startDate}</div>
              </div>
              <div className="test-item">
                <div className="label">End Date:</div>
                <div className="value">{test.endDate}</div>
              </div>
              <div className="test-item">
                <div className="label">Traffic Allocation:</div>
                <div className="value">{test.trafficAllocation}%</div>
              </div>
              <div className="test-item">
                <div className="label">Variants:</div>
                <div className="value">{test.variants.length}</div>
              </div>
            </TestDetails>
            
            <TestActions>
              <Button primary onClick={() => handleTestSelect(test)}>
                View Details
              </Button>
              {test.status === 'scheduled' && (
                <Button onClick={() => handleStartTest(test.id)}>
                  Start Test
                </Button>
              )}
              {test.status === 'active' && (
                <Button onClick={() => handleStopTest(test.id)}>
                  Stop Test
                </Button>
              )}
            </TestActions>
          </TestCard>
        ))}
      </TestsGrid>
      
      {selectedTest && (
        <>
          <Header>
            <h2>{selectedTest.name} - Details</h2>
            <p>{selectedTest.description}</p>
          </Header>
          
          {selectedTest.status !== 'scheduled' && (
            <>
              <ChartContainer>
                <h3>Impressions by Variant</h3>
                <Bar data={getChartData(selectedTest).impressions} options={chartOptions} />
              </ChartContainer>
              
              <ChartContainer>
                <h3>Clicks by Variant</h3>
                <Bar data={getChartData(selectedTest).clicks} options={chartOptions} />
              </ChartContainer>
              
              <ChartContainer>
                <h3>CTR by Variant</h3>
                <Bar data={getChartData(selectedTest).ctr} options={chartOptions} />
              </ChartContainer>
              
              <ChartContainer>
                <h3>Revenue by Variant</h3>
                <Bar data={getChartData(selectedTest).revenue} options={chartOptions} />
              </ChartContainer>
            </>
          )}
          
          <Header>
            <h2>Variants</h2>
          </Header>
          
          <VariantGrid>
            {selectedTest.variants.map(variant => (
              <VariantCard key={variant.id} isWinner={variant.isWinner}>
                <h4>
                  {variant.name}
                  {variant.isControl && <span className="badge">Control</span>}
                  {variant.isWinner && <span className="badge">Winner</span>}
                </h4>
                <p>{variant.description}</p>
                
                <VariantDetails>
                  <div className="variant-item">
                    <div className="label">Impressions:</div>
                    <div className="value">{variant.impressions.toLocaleString()}</div>
                  </div>
                  <div className="variant-item">
                    <div className="label">Clicks:</div>
                    <div className="value">{variant.clicks.toLocaleString()}</div>
                  </div>
                  <div className="variant-item">
                    <div className="label">CTR:</div>
                    <div className="value">{variant.ctr.toFixed(2)}%</div>
                  </div>
                  <div className="variant-item">
                    <div className="label">Revenue:</div>
                    <div className="value">${variant.revenue.toLocaleString()}</div>
                  </div>
                </VariantDetails>
                
                {selectedTest.status === 'completed' && !variant.isWinner && (
                  <TestActions>
                    <Button onClick={() => handleDeclareWinner(selectedTest.id, variant.id)}>
                      Declare Winner
                    </Button>
                  </TestActions>
                )}
              </VariantCard>
            ))}
          </VariantGrid>
        </>
      )}
    </Container>
  );
};

export default ABTesting; 