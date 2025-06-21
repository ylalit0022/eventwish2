const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const webpack = require('webpack');

module.exports = (env, argv) => {
  const isProduction = argv.mode === 'production';
  
  return {
  entry: './src/index.js',
  output: {
      filename: isProduction ? 'bundle.[contenthash].js' : 'bundle.js',
    path: path.resolve(__dirname, 'build'),
      publicPath: '/'
  },
  module: {
    rules: [
      {
        test: /\.(js|jsx)$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['@babel/preset-env', '@babel/preset-react']
          }
        }
      },
      {
        test: /\.css$/,
        use: ['style-loader', 'css-loader']
      },
      {
          test: /\.(png|svg|jpg|jpeg|gif|ico)$/,
        type: 'asset/resource'
      }
    ]
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: './public/index.html',
        favicon: './public/favicon.ico',
        scriptLoading: 'defer',
        inject: true,
        minify: isProduction,
    }),
    new webpack.DefinePlugin({
        'process.env.NODE_ENV': JSON.stringify(isProduction ? 'production' : 'development')
    })
  ],
    resolve: {
      extensions: ['.js', '.jsx']
    },
  devServer: {
    historyApiFallback: true,
    port: 3000,
      open: true,
      hot: true,
    proxy: {
      '/api': {
        target: 'http://localhost:3001',
        secure: false,
        changeOrigin: true
        },
        '/admin': {
          target: 'http://localhost:3001',
          secure: false,
          changeOrigin: true
        },
        '/bundle.js': {
          target: 'http://localhost:3001',
          secure: false,
          headers: {
            'Content-Type': 'application/javascript'
      }
    }
      },
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, PATCH, OPTIONS',
        'Access-Control-Allow-Headers': 'X-Requested-With, content-type, Authorization, X-Dev-Email, X-Dev-Admin'
      }
    },
    devtool: isProduction ? false : 'eval-source-map'
  };
}; 