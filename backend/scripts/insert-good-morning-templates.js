const mongoose = require('mongoose');
const Template = require('../models/Template');
const dotenv = require('dotenv');

// Load environment variables
dotenv.config();

// Connect to MongoDB
mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish')
  .then(() => console.log('MongoDB connected'))
  .catch(err => {
    console.error('MongoDB connection error:', err);
    process.exit(1);
  });

// Good Morning templates
const goodMorningTemplates = [
  {
    title: 'Sunrise Good Morning',
    category: 'good morning',
    htmlContent: `<div class="morning-greeting">
      <div class="sun-container">
        <div class="sun"></div>
        <div class="rays"></div>
      </div>
      <div class="greeting-text">
        <h1>Good Morning!</h1>
        <p>May your day be as bright as the morning sun.</p>
        <p class="signature">Warmly,<br><span class="sender">[Your Name]</span></p>
      </div>
    </div>`,
    cssContent: `.morning-greeting {
      font-family: 'Arial', sans-serif;
      height: 100vh;
      background: linear-gradient(to bottom, #87CEEB, #E0F7FA);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 20px;
      overflow: hidden;
      position: relative;
    }
    .sun-container {
      position: relative;
      margin-bottom: 40px;
    }
    .sun {
      width: 100px;
      height: 100px;
      background-color: #FFD700;
      border-radius: 50%;
      box-shadow: 0 0 50px #FFD700;
      animation: rise 3s ease-out forwards;
    }
    @keyframes rise {
      0% { transform: translateY(50px); opacity: 0.3; }
      100% { transform: translateY(0); opacity: 1; }
    }
    .rays {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
    }
    .rays:before, .rays:after {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      width: 160px;
      height: 160px;
      border-radius: 50%;
      border: 2px solid rgba(255, 215, 0, 0.3);
      transform: translate(-50%, -50%);
      animation: pulse 2s infinite;
    }
    .rays:after {
      animation-delay: 0.5s;
    }
    @keyframes pulse {
      0% { width: 100px; height: 100px; opacity: 1; }
      100% { width: 200px; height: 200px; opacity: 0; }
    }
    .greeting-text {
      background-color: rgba(255, 255, 255, 0.8);
      padding: 30px;
      border-radius: 10px;
      text-align: center;
      max-width: 80%;
      box-shadow: 0 5px 15px rgba(0,0,0,0.1);
      animation: fadeIn 2s;
    }
    @keyframes fadeIn {
      0% { opacity: 0; transform: translateY(20px); }
      100% { opacity: 1; transform: translateY(0); }
    }
    .greeting-text h1 {
      color: #FF9800;
      font-size: 2.5em;
      margin-bottom: 15px;
    }
    .greeting-text p {
      color: #424242;
      font-size: 1.2em;
      line-height: 1.5;
      margin-bottom: 20px;
    }
    .signature {
      font-style: italic;
      color: #616161;
      margin-top: 20px;
    }
    .sender {
      font-weight: bold;
      color: #FF9800;
    }`,
    tags: ['good morning', 'sunrise', 'greeting', 'animated'],
    styleTags: ['bright', 'animated', 'cheerful'],
    searchKeywords: ['good morning', 'sunrise', 'greeting', 'day', 'animated'],
    status: true,
    isFeatured: true
  },
  {
    title: 'Coffee Morning Wish',
    category: 'good morning',
    htmlContent: `<div class="coffee-morning">
      <div class="coffee-cup">
        <div class="cup">
          <div class="coffee"></div>
          <div class="handle"></div>
          <div class="steam steam-1"></div>
          <div class="steam steam-2"></div>
          <div class="steam steam-3"></div>
        </div>
      </div>
      <div class="morning-message">
        <h1>Good Morning!</h1>
        <p>Here's a cup of motivation to start your day. Have a wonderful morning!</p>
        <p class="signature">Cheers,<br><span class="sender">[Your Name]</span></p>
      </div>
    </div>`,
    cssContent: `.coffee-morning {
      font-family: 'Roboto', sans-serif;
      min-height: 100vh;
      background-color: #f5f5f5;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 20px;
    }
    .coffee-cup {
      margin-bottom: 40px;
    }
    .cup {
      position: relative;
      width: 120px;
      height: 100px;
      background-color: white;
      border-radius: 0 0 60px 60px;
      box-shadow: 0 5px 15px rgba(0,0,0,0.1);
    }
    .coffee {
      position: absolute;
      top: 15px;
      left: 10px;
      width: 100px;
      height: 70px;
      background-color: #6F4E37;
      border-radius: 0 0 50px 50px;
      overflow: hidden;
    }
    .coffee:before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 20px;
      background: linear-gradient(90deg, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0.3) 50%, rgba(255,255,255,0.1) 100%);
      animation: swirl 4s linear infinite;
    }
    @keyframes swirl {
      0% { transform: translateX(-100%); }
      100% { transform: translateX(100%); }
    }
    .handle {
      position: absolute;
      top: 30px;
      right: -25px;
      width: 30px;
      height: 50px;
      border: 8px solid white;
      border-left: none;
      border-radius: 0 25px 25px 0;
    }
    .steam {
      position: absolute;
      top: -20px;
      width: 8px;
      height: 20px;
      background-color: rgba(255,255,255,0.7);
      border-radius: 5px;
      animation: steam 3s infinite;
    }
    .steam-1 { left: 20px; animation-delay: 0.2s; }
    .steam-2 { left: 50px; animation-delay: 0.6s; }
    .steam-3 { left: 80px; animation-delay: 0.4s; }
    @keyframes steam {
      0% { transform: translateY(0) scaleX(1); opacity: 0; }
      15% { opacity: 1; }
      50% { transform: translateY(-20px) scaleX(3); }
      95% { opacity: 0; }
      100% { transform: translateY(-40px) scaleX(1); opacity: 0; }
    }
    .morning-message {
      background-color: white;
      padding: 30px;
      border-radius: 10px;
      text-align: center;
      max-width: 80%;
      box-shadow: 0 5px 15px rgba(0,0,0,0.1);
    }
    .morning-message h1 {
      color: #6F4E37;
      font-size: 2.5em;
      margin-bottom: 15px;
    }
    .morning-message p {
      color: #616161;
      font-size: 1.2em;
      line-height: 1.6;
      margin-bottom: 20px;
    }
    .signature {
      font-style: italic;
      color: #9E9E9E;
      margin-top: 20px;
    }
    .sender {
      font-weight: bold;
      color: #6F4E37;
    }`,
    tags: ['good morning', 'coffee', 'animated', 'greeting'],
    styleTags: ['animated', 'warm', 'cheerful'],
    searchKeywords: ['good morning', 'coffee', 'cup', 'warm', 'greeting'],
    status: true
  },
  {
    title: 'Floral Morning Greeting',
    category: 'good morning',
    htmlContent: `<div style="text-align: center; padding: 30px; font-family: 'Georgia', serif; background-color: #f8f8f8;">
      <div style="background-color: white; padding: 40px; border-radius: 10px; box-shadow: 0 5px 15px rgba(0,0,0,0.1); max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; background-image: url('data:image/svg+xml;utf8,<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\" viewBox=\"0 0 100 100\"><path d=\"M30,30 Q40,20 50,30 Q60,40 70,30\" stroke=\"%23F8BBD0\" fill=\"none\" stroke-width=\"2\"/><path d=\"M20,50 Q50,30 80,50\" stroke=\"%23E1BEE7\" fill=\"none\" stroke-width=\"2\"/><path d=\"M20,70 Q50,50 80,70\" stroke=\"%23D1C4E9\" fill=\"none\" stroke-width=\"2\"/></svg>'); background-repeat: no-repeat; background-position: top right; background-size: 150px;">
        <h1 style="color: #9C27B0; font-size: 2.5em; margin-bottom: 20px;">Good Morning</h1>
        <div style="color: #E91E63; font-size: 1.8em; margin: 15px 0;">🌸 🌿 🌸</div>
        <p style="font-size: 1.2em; color: #424242; margin: 20px 0; line-height: 1.6;">
          Dear <span class="recipient" style="color: #9C27B0; font-weight: bold;">[Recipient]</span>,
        </p>
        <p style="font-size: 1.1em; color: #616161; line-height: 1.8;">
          May your morning be as beautiful as blooming flowers,<br>
          filling your day with fragrance of happiness<br>
          and colors of joy.
        </p>
        <div style="margin: 25px 0; color: #E91E63; font-size: 1.5em;">🌺 🦋 🌷</div>
        <p style="font-size: 1.1em; font-style: italic; color: #616161;">
          With warm thoughts,<br><span class="sender" style="color: #9C27B0; font-weight: bold;">[Your Name]</span>
        </p>
      </div>
    </div>`,
    tags: ['good morning', 'floral', 'elegant', 'greeting'],
    styleTags: ['elegant', 'floral', 'gentle'],
    searchKeywords: ['good morning', 'flowers', 'floral', 'elegant', 'gentle'],
    status: true
  },
  {
    title: 'Morning Motivational Quote',
    category: 'good morning',
    htmlContent: `<div class="motivational-morning">
      <div class="quote-container">
        <div class="quote-icon">"</div>
        <div class="quote-content">
          <p class="quote-text">The morning sun has risen. Your opportunity to shine is now.</p>
          <p class="quote-author">— Inspirational Quote</p>
        </div>
      </div>
      <div class="morning-greeting">
        <h1>Good Morning!</h1>
        <p>Start your day with positivity and purpose.</p>
        <p class="signature">Stay inspired,<br><span class="sender">[Your Name]</span></p>
      </div>
    </div>`,
    cssContent: `.motivational-morning {
      font-family: 'Helvetica', sans-serif;
      min-height: 100vh;
      background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 20px;
    }
    .quote-container {
      position: relative;
      background-color: white;
      padding: 40px;
      border-radius: 10px;
      max-width: 80%;
      margin-bottom: 30px;
      box-shadow: 0 10px 20px rgba(0,0,0,0.1);
    }
    .quote-icon {
      position: absolute;
      top: -20px;
      left: 20px;
      font-size: 80px;
      color: #4A90E2;
      font-family: 'Georgia', serif;
      line-height: 1;
    }
    .quote-content {
      text-align: center;
    }
    .quote-text {
      font-size: 1.8em;
      color: #333;
      line-height: 1.4;
      margin-bottom: 20px;
    }
    .quote-author {
      font-size: 1.2em;
      color: #666;
      font-style: italic;
    }
    .morning-greeting {
      text-align: center;
      background-color: rgba(255, 255, 255, 0.9);
      padding: 30px;
      border-radius: 10px;
      max-width: 80%;
      box-shadow: 0 5px 15px rgba(0,0,0,0.1);
    }
    .morning-greeting h1 {
      color: #4A90E2;
      font-size: 2.5em;
      margin-bottom: 15px;
    }
    .morning-greeting p {
      color: #555;
      font-size: 1.2em;
      line-height: 1.6;
      margin-bottom: 20px;
    }
    .signature {
      font-style: italic;
      color: #777;
      margin-top: 20px;
    }
    .sender {
      font-weight: bold;
      color: #4A90E2;
    }`,
    jsContent: `// Array of motivational quotes
const quotes = [
  { text: "The morning sun has risen. Your opportunity to shine is now.", author: "Inspirational Quote" },
  { text: "Today is a new day. Don't let yesterday's failures ruin the beauty of today.", author: "Unknown" },
  { text: "Wake up with determination. Go to bed with satisfaction.", author: "Morning Wisdom" },
  { text: "Your attitude determines your direction. Choose positivity this morning.", author: "Motivational Thought" },
  { text: "The only limit to your impact is your imagination and commitment.", author: "Morning Reflection" }
];

// Set a random quote when the page loads
window.addEventListener('load', function() {
  const randomQuote = quotes[Math.floor(Math.random() * quotes.length)];
  document.querySelector('.quote-text').textContent = randomQuote.text;
  document.querySelector('.quote-author').textContent = "— " + randomQuote.author;
});`,
    tags: ['good morning', 'motivational', 'quote', 'inspirational'],
    styleTags: ['clean', 'modern', 'inspiring'],
    searchKeywords: ['good morning', 'motivation', 'quote', 'inspiration', 'positive'],
    status: true,
    isFeatured: true
  },
  {
    title: 'Birds & Nature Morning',
    category: 'good morning',
    htmlContent: `<div class="nature-morning">
      <div class="sky">
        <div class="sun"></div>
        <div class="cloud cloud-1"></div>
        <div class="cloud cloud-2"></div>
        <div class="bird bird-1"></div>
        <div class="bird bird-2"></div>
        <div class="bird bird-3"></div>
      </div>
      <div class="grass"></div>
      <div class="morning-message">
        <h1>Good Morning!</h1>
        <p>Rise and shine with the beauty of nature.</p>
        <p>May the birds' melodies bring joy to your day.</p>
        <p class="signature">Nature's greetings,<br><span class="sender">[Your Name]</span></p>
      </div>
    </div>`,
    cssContent: `.nature-morning {
      font-family: 'Arial', sans-serif;
      height: 100vh;
      position: relative;
      overflow: hidden;
    }
    .sky {
      height: 70%;
      background: linear-gradient(to bottom, #87CEEB, #E0F7FA);
      position: relative;
    }
    .sun {
      position: absolute;
      top: 50px;
      left: 80px;
      width: 80px;
      height: 80px;
      background-color: #FFD700;
      border-radius: 50%;
      box-shadow: 0 0 30px #FFD700;
    }
    .cloud {
      position: absolute;
      background-color: white;
      border-radius: 50px;
    }
    .cloud-1 {
      top: 100px;
      right: 100px;
      width: 100px;
      height: 40px;
      animation: float 20s linear infinite;
    }
    .cloud-1:before, .cloud-1:after {
      content: '';
      position: absolute;
      background-color: white;
      border-radius: 50%;
    }
    .cloud-1:before {
      width: 50px;
      height: 50px;
      top: -20px;
      left: 15px;
    }
    .cloud-1:after {
      width: 40px;
      height: 40px;
      top: -15px;
      right: 15px;
    }
    .cloud-2 {
      top: 180px;
      left: 200px;
      width: 120px;
      height: 45px;
      animation: float 25s linear infinite reverse;
    }
    .cloud-2:before, .cloud-2:after {
      content: '';
      position: absolute;
      background-color: white;
      border-radius: 50%;
    }
    .cloud-2:before {
      width: 55px;
      height: 55px;
      top: -25px;
      left: 20px;
    }
    .cloud-2:after {
      width: 45px;
      height: 45px;
      top: -15px;
      right: 20px;
    }
    @keyframes float {
      0% { transform: translateX(0); }
      50% { transform: translateX(100px); }
      100% { transform: translateX(0); }
    }
    .bird {
      position: absolute;
      width: 15px;
      height: 8px;
      background-color: transparent;
      border-radius: 50%;
    }
    .bird:before, .bird:after {
      content: '';
      position: absolute;
      width: 15px;
      height: 3px;
      background-color: #333;
      border-radius: 5px;
      transform-origin: 0% 50%;
      animation: flap 0.5s ease-in-out infinite alternate;
    }
    .bird:before {
      transform: rotateZ(20deg);
    }
    .bird:after {
      transform: rotateZ(-20deg);
    }
    @keyframes flap {
      0% { transform: rotateZ(10deg); }
      100% { transform: rotateZ(-10deg); }
    }
    .bird-1 {
      top: 100px;
      left: 300px;
      animation: fly 10s linear infinite;
    }
    .bird-2 {
      top: 150px;
      left: 320px;
      animation: fly 12s linear infinite;
      animation-delay: 1s;
    }
    .bird-3 {
      top: 120px;
      left: 340px;
      animation: fly 11s linear infinite;
      animation-delay: 2s;
    }
    @keyframes fly {
      0% { transform: translateX(0) translateY(0); }
      25% { transform: translateX(100px) translateY(-20px); }
      50% { transform: translateX(200px) translateY(0); }
      75% { transform: translateX(100px) translateY(20px); }
      100% { transform: translateX(0) translateY(0); }
    }
    .grass {
      height: 30%;
      background-color: #4CAF50;
      position: relative;
    }
    .grass:before {
      content: '';
      position: absolute;
      top: -20px;
      left: 0;
      width: 100%;
      height: 20px;
      background-color: #4CAF50;
      border-radius: 50% 50% 0 0 / 100% 100% 0 0;
    }
    .morning-message {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      background-color: rgba(255, 255, 255, 0.9);
      padding: 30px;
      border-radius: 10px;
      text-align: center;
      max-width: 80%;
      box-shadow: 0 5px 15px rgba(0,0,0,0.1);
    }
    .morning-message h1 {
      color: #4CAF50;
      font-size: 2.5em;
      margin-bottom: 15px;
    }
    .morning-message p {
      color: #555;
      font-size: 1.2em;
      line-height: 1.6;
      margin-bottom: 15px;
    }
    .signature {
      font-style: italic;
      color: #777;
      margin-top: 20px;
    }
    .sender {
      font-weight: bold;
      color: #4CAF50;
    }`,
    tags: ['good morning', 'nature', 'birds', 'animated'],
    styleTags: ['animated', 'nature', 'peaceful'],
    searchKeywords: ['good morning', 'nature', 'birds', 'peaceful', 'sunrise'],
    status: true
  }
];

// Function to insert templates
async function insertGoodMorningTemplates() {
  try {
    // Delete existing Good Morning templates (optional)
    await Template.deleteMany({ category: 'good morning' });
    
    // Insert new templates
    const result = await Template.insertMany(goodMorningTemplates);
    console.log(`${result.length} Good Morning templates inserted successfully`);
    
    mongoose.disconnect();
    console.log('MongoDB disconnected');
  } catch (error) {
    console.error('Error inserting templates:', error);
    mongoose.disconnect();
    process.exit(1);
  }
}

// Run the function
insertGoodMorningTemplates(); 