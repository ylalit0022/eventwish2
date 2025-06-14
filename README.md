# EventWish

Event wish creation and sharing application.

## Local Development Setup

### Testing with Local API Server

This app supports testing with a local API server using build variants. This allows you to run your API server locally and test with real Android devices without constant redeployment.

#### Setup Instructions

1. **Configure your local API server**
   - Run your Node.js API server locally
   - Make sure it's accessible on your network (listening on 0.0.0.0 instead of localhost)
   - Find your computer's IP address on the local network:
     - Windows: Run `.\tools\find_local_ip.ps1` in PowerShell
     - Linux/Mac: Run `./tools/find_local_ip.sh` in Terminal

2. **Update the local build variant configuration**
   - Open `app/build.gradle`
   - Find the `local` build type
   - Update the `BASE_URL` to match your computer's IP address and port:
     ```groovy
     buildConfigField "String", "BASE_URL", "\"http://YOUR_IP_ADDRESS:PORT/api/\""
     ```

3. **Build and run the local variant**
   - In Android Studio, select the "local" build variant from the Build Variants panel
   - Build and run the app on your device
   - The app will now connect to your local API server

4. **Switching between environments**
   - Use "local" variant for local development
   - Use "debug" variant for testing with production API
   - Use "release" variant for production builds

#### Network Security Configuration

If you encounter cleartext traffic issues (using http instead of https), you may need to update the network security configuration:

1. Create or update `app/src/main/res/xml/network_security_config.xml`:
   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <network-security-config>
       <domain-config cleartextTrafficPermitted="true">
           <domain includeSubdomains="true">YOUR_IP_ADDRESS</domain>
       </domain-config>
   </network-security-config>
   ```

2. Reference it in your AndroidManifest.xml:
   ```xml
   <application
       ...
       android:networkSecurityConfig="@xml/network_security_config"
       ... >
   ```
