/**
 * Swagger Configuration
 * 
 * This module provides configuration for Swagger UI API documentation.
 */

const swaggerJsdoc = require('swagger-jsdoc');
const swaggerUi = require('swagger-ui-express');
const path = require('path');
const packageJson = require('../package.json');

// Swagger definition
const swaggerDefinition = {
  openapi: '3.0.0',
  info: {
    title: 'EventWish AdMob API',
    version: packageJson.version,
    description: 'API documentation for EventWish AdMob integration',
    license: {
      name: 'Private',
      url: 'https://eventwish.com',
    },
    contact: {
      name: 'EventWish Support',
      url: 'https://eventwish.com/support',
      email: 'support@eventwish.com',
    },
  },
  servers: [
    {
      url: process.env.API_BASE_URL || 'https://eventwish2.onrender.com',
      description: 'Development server',
    },
    {
      url: 'https://api.eventwish.com',
      description: 'Production server',
    },
  ],
  components: {
    securitySchemes: {
      ApiKeyAuth: {
        type: 'apiKey',
        in: 'header',
        name: 'x-api-key',
      },
    },
  },
  security: [
    {
      ApiKeyAuth: [],
    },
  ],
  tags: [
    {
      name: 'AdMob',
      description: 'AdMob configuration and management',
    },
    {
      name: 'Client',
      description: 'Client-side ad serving',
    },
    {
      name: 'Analytics',
      description: 'Ad performance analytics',
    },
    {
      name: 'Fraud',
      description: 'Fraud detection and prevention',
    },
    {
      name: 'Suspicious Activity',
      description: 'Suspicious activity monitoring',
    },
    {
      name: 'A/B Testing',
      description: 'A/B testing for ad configurations',
    },
    {
      name: 'User Targeting',
      description: 'User targeting and segmentation',
    },
    {
      name: 'Health',
      description: 'Health checks and monitoring',
    },
  ],
};

// Options for the swagger docs
const options = {
  swaggerDefinition,
  // Path to the API docs
  apis: [
    path.resolve(__dirname, '../routes/*.js'),
    path.resolve(__dirname, '../models/*.js'),
    path.resolve(__dirname, '../controllers/*.js'),
  ],
};

// Initialize swagger-jsdoc
const swaggerSpec = swaggerJsdoc(options);

/**
 * Setup Swagger UI
 * @param {Object} app - Express app
 */
function setupSwagger(app) {
  // Serve swagger docs
  app.use(
    '/api-docs',
    swaggerUi.serve,
    swaggerUi.setup(swaggerSpec, {
      explorer: true,
      customCss: '.swagger-ui .topbar { display: none }',
      customSiteTitle: 'EventWish AdMob API Documentation',
      customfavIcon: '/favicon.ico',
    })
  );

  // Serve swagger spec as JSON
  app.get('/api-docs.json', (req, res) => {
    res.setHeader('Content-Type', 'application/json');
    res.send(swaggerSpec);
  });

  console.log('Swagger UI initialized at /api-docs');
}

module.exports = {
  setupSwagger,
  swaggerSpec,
}; 