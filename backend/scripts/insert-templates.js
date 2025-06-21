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

// Template data for different categories
const templates = [
  // Birthday templates
  {
    title: 'Birthday Cake Card',
    category: 'birthday',
    htmlContent: `<div class="card">
      <div class="outside">
        <div class="front">
          <div class="cake">
            <div class="top-layer"></div>
            <div class="middle-layer"></div>
            <div class="bottom-layer"></div>
            <div class="candle"></div>
          </div>
        </div>
        <div class="back"></div>
      </div>
    </div>`,
    cssContent: `.card {
      perspective: 1000px;
      width: 300px;
      height: 400px;
      margin: 0 auto;
    }
    .outside, .front, .back {
      width: 100%;
      height: 100%;
    }
    .outside {
      position: relative;
      transform-style: preserve-3d;
      transition: transform 1s;
    }
    .front, .back {
      position: absolute;
      backface-visibility: hidden;
      border-radius: 15px;
    }
    .front {
      background-color: #f8c9d4;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .back {
      background-color: #f5f5f5;
      transform: rotateY(180deg);
    }
    .cake {
      position: relative;
      width: 200px;
      height: 160px;
    }
    .top-layer, .middle-layer, .bottom-layer {
      position: absolute;
      border-radius: 10px;
      background-color: #f9c5d5;
    }
    .bottom-layer {
      width: 200px;
      height: 60px;
      bottom: 0;
      border: 3px solid #f06292;
    }
    .middle-layer {
      width: 150px;
      height: 50px;
      bottom: 60px;
      left: 25px;
      border: 3px solid #f06292;
    }
    .top-layer {
      width: 100px;
      height: 40px;
      bottom: 110px;
      left: 50px;
      border: 3px solid #f06292;
    }
    .candle {
      position: absolute;
      width: 10px;
      height: 30px;
      background-color: #ffa726;
      bottom: 150px;
      left: 95px;
      border-radius: 5px 5px 0 0;
    }
    .candle:before {
      content: "";
      position: absolute;
      width: 8px;
      height: 12px;
      background: linear-gradient(#ffeb3b, #ff5722);
      top: -12px;
      left: 1px;
      border-radius: 50% 50% 20% 20%;
    }`,
    jsContent: `document.querySelector('.card').addEventListener('click', function() {
      document.querySelector('.outside').style.transform = 'rotateY(180deg)';
    });`,
    tags: ['birthday', 'cake', 'card', 'interactive'],
    styleTags: ['colorful', 'interactive', 'cute'],
    searchKeywords: ['birthday', 'cake', 'card', 'celebration', 'interactive'],
    status: true,
    isFeatured: true
  },
  {
    title: 'Simple Birthday Wish',
    category: 'birthday',
    htmlContent: `<div style="text-align: center; padding: 20px; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;">
    <h1 style="color: #FF6B6B; font-size: 2.5em; margin-bottom: 20px;">🎉 Happy Birthday! 🎂</h1>
    <p style="font-size: 1.2em; color: #4A4A4A; margin: 15px 0;">
        Dear <span class="recipient-name" style="color: red;" >[Recipient]</span>,
    </p>
    <p style="font-size: 1.1em; color: #666; line-height: 1.6;">
        May your special day be filled with beautiful moments,<br>
        happy smiles, and endless joy!<br>
        Wishing you a fantastic birthday celebration!
    </p>
    <div style="margin: 25px 0; font-size: 1.5em;">🎈 🎁 🎊</div>
    <p style="font-size: 1.1em; color: #666;">
        With love,<br><span class="sender-name" style="color: red;" >[Your Name]</span>
    </p>
</div>`,
    tags: ['birthday', 'simple', 'elegant'],
    styleTags: ['clean', 'modern', 'simple'],
    searchKeywords: ['birthday', 'simple', 'greeting', 'wish'],
    status: true
  },
  {
    title: 'Birthday Balloons Animation',
    category: 'birthday',
    htmlContent: `<div class="birthday-container">
    <div class="balloon-container">
      <div class="balloon balloon-1"></div>
      <div class="balloon balloon-2"></div>
      <div class="balloon balloon-3"></div>
      <div class="balloon balloon-4"></div>
      <div class="balloon balloon-5"></div>
    </div>
    <div class="message">
      <h1>Happy Birthday!</h1>
      <p>Wishing you a day filled with happiness and a year filled with joy.</p>
      <p class="signature">From: <span class="sender">[Sender]</span></p>
    </div>
  </div>`,
    cssContent: `.birthday-container {
      font-family: 'Arial', sans-serif;
      background: linear-gradient(to bottom, #e0f7fa, #b2ebf2);
      height: 100vh;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      overflow: hidden;
      position: relative;
    }
    .balloon-container {
      position: absolute;
      width: 100%;
      height: 100%;
    }
    .balloon {
      position: absolute;
      width: 50px;
      height: 65px;
      border-radius: 50%;
      animation: float 8s ease-in-out infinite;
    }
    .balloon-1 {
      background: radial-gradient(circle at 20% 30%, #f48fb1, #ec407a);
      left: 10%;
      top: 50%;
      animation-delay: 0s;
    }
    .balloon-2 {
      background: radial-gradient(circle at 20% 30%, #90caf9, #42a5f5);
      left: 30%;
      top: 40%;
      animation-delay: 1s;
    }
    .balloon-3 {
      background: radial-gradient(circle at 20% 30%, #a5d6a7, #66bb6a);
      left: 50%;
      top: 60%;
      animation-delay: 2s;
    }
    .balloon-4 {
      background: radial-gradient(circle at 20% 30%, #ffcc80, #ffa726);
      left: 70%;
      top: 45%;
      animation-delay: 3s;
    }
    .balloon-5 {
      background: radial-gradient(circle at 20% 30%, #ce93d8, #ab47bc);
      left: 85%;
      top: 55%;
      animation-delay: 4s;
    }
    .balloon:before {
      content: '';
      position: absolute;
      width: 10px;
      height: 25px;
      bottom: -25px;
      left: 20px;
      background: rgba(0, 0, 0, 0.1);
    }
    @keyframes float {
      0% { transform: translateY(0); }
      50% { transform: translateY(-100px); }
      100% { transform: translateY(0); }
    }
    .message {
      background-color: rgba(255, 255, 255, 0.8);
      border-radius: 10px;
      padding: 30px;
      text-align: center;
      max-width: 80%;
      z-index: 10;
    }
    .message h1 {
      color: #e91e63;
      font-size: 3em;
      margin-bottom: 20px;
    }
    .message p {
      color: #333;
      font-size: 1.2em;
      line-height: 1.6;
      margin-bottom: 20px;
    }
    .signature {
      font-style: italic;
      color: #555;
    }
    .sender {
      font-weight: bold;
    }`,
    tags: ['birthday', 'animation', 'balloons'],
    styleTags: ['animated', 'colorful', 'playful'],
    searchKeywords: ['birthday', 'balloon', 'animation', 'floating'],
    status: true,
    isFeatured: true
  },
  {
    title: 'Birthday Gift Box',
    category: 'birthday',
    htmlContent: `<div class="gift-container">
    <div class="gift">
      <div class="gift-lid"></div>
      <div class="gift-box"></div>
      <div class="gift-ribbon"></div>
    </div>
    <div class="birthday-message">
      <h1>Happy Birthday!</h1>
      <p>Click the gift to open your surprise</p>
      <div class="message hidden">
        <p>Wishing you a day that is as special as you are!</p>
        <p>From: <span class="sender">[Sender Name]</span></p>
      </div>
    </div>
  </div>`,
    cssContent: `.gift-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100vh;
      background-color: #f8f9fa;
      font-family: 'Arial', sans-serif;
    }
    .gift {
      position: relative;
      width: 150px;
      height: 150px;
      margin-bottom: 30px;
      cursor: pointer;
      perspective: 600px;
    }
    .gift-box {
      position: absolute;
      width: 100%;
      height: 100%;
      background-color: #ff8a80;
      transform-origin: bottom;
      transition: transform 0.5s;
    }
    .gift-lid {
      position: absolute;
      width: 110%;
      height: 25px;
      left: -5%;
      top: -25px;
      background-color: #ff8a80;
      transform-origin: bottom;
      transition: transform 0.5s;
    }
    .gift-ribbon {
      position: absolute;
      width: 30px;
      height: 100%;
      background-color: #ff5252;
      left: 60px;
    }
    .gift-ribbon:before {
      content: "";
      position: absolute;
      width: 100%;
      height: 30px;
      background-color: #ff5252;
      left: -60px;
      top: 60px;
    }
    .gift.open .gift-lid {
      transform: rotateX(-110deg);
    }
    .birthday-message {
      text-align: center;
    }
    .birthday-message h1 {
      color: #ff5252;
      margin-bottom: 10px;
    }
    .birthday-message p {
      color: #555;
      margin-bottom: 20px;
    }
    .message {
      background-color: #fff;
      padding: 20px;
      border-radius: 10px;
      box-shadow: 0 5px 15px rgba(0,0,0,0.1);
      opacity: 0;
      transform: translateY(20px);
      transition: all 0.5s;
    }
    .message.show {
      opacity: 1;
      transform: translateY(0);
    }
    .hidden {
      display: none;
    }
    .sender {
      font-weight: bold;
      color: #ff5252;
    }`,
    jsContent: `document.querySelector('.gift').addEventListener('click', function() {
      this.classList.toggle('open');
      setTimeout(() => {
        const message = document.querySelector('.message');
        message.classList.remove('hidden');
        setTimeout(() => {
          message.classList.add('show');
        }, 100);
      }, 500);
    });`,
    tags: ['birthday', 'gift', 'interactive', 'surprise'],
    styleTags: ['animated', 'interactive', 'cute'],
    searchKeywords: ['birthday', 'gift', 'surprise', 'present', 'box'],
    status: true
  },
  {
    title: 'Birthday Confetti Celebration',
    category: 'birthday',
    htmlContent: `<div class="birthday-confetti">
    <div class="confetti-container"></div>
    <div class="birthday-card">
      <div class="card-front">
        <h1>Happy Birthday!</h1>
        <p>Click to celebrate</p>
      </div>
      <div class="card-inside">
        <h2>Today is your special day!</h2>
        <p>Here's to celebrating you and all the joy you bring to the world.</p>
        <p>May your day be filled with laughter and love.</p>
        <p class="signature">Warmest wishes,<br><span class="sender">[Your Name]</span></p>
      </div>
    </div>
  </div>`,
    cssContent: `.birthday-confetti {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100vh;
      background-color: #f9f9f9;
      font-family: 'Roboto', sans-serif;
      perspective: 1000px;
    }
    .birthday-card {
      position: relative;
      width: 300px;
      height: 400px;
      transform-style: preserve-3d;
      transition: transform 1s;
      cursor: pointer;
    }
    .card-front, .card-inside {
      position: absolute;
      width: 100%;
      height: 100%;
      backface-visibility: hidden;
      border-radius: 15px;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      padding: 20px;
      box-sizing: border-box;
    }
    .card-front {
      background: linear-gradient(135deg, #6a11cb 0%, #2575fc 100%);
      color: white;
      text-align: center;
    }
    .card-inside {
      background-color: white;
      color: #333;
      transform: rotateY(180deg);
      border: 5px solid #6a11cb;
      overflow-y: auto;
    }
    .birthday-card.flipped {
      transform: rotateY(180deg);
    }
    .card-front h1 {
      font-size: 2.5em;
      margin-bottom: 0.5em;
      text-shadow: 2px 2px 4px rgba(0,0,0,0.3);
    }
    .card-inside h2 {
      font-size: 1.8em;
      color: #6a11cb;
      margin-bottom: 20px;
    }
    .card-inside p {
      font-size: 1.1em;
      line-height: 1.5;
      margin-bottom: 15px;
    }
    .signature {
      align-self: flex-end;
      margin-top: 20px;
      font-style: italic;
    }
    .sender {
      font-weight: bold;
      color: #6a11cb;
    }
    .confetti-container {
      position: absolute;
      width: 100%;
      height: 100%;
      pointer-events: none;
    }
    .confetti {
      position: absolute;
      width: 10px;
      height: 10px;
      opacity: 0;
    }`,
    jsContent: `document.querySelector('.birthday-card').addEventListener('click', function() {
      this.classList.toggle('flipped');
      if (this.classList.contains('flipped')) {
        createConfetti();
      }
    });
    
    function createConfetti() {
      const container = document.querySelector('.confetti-container');
      container.innerHTML = '';
      
      const colors = ['#f94144', '#f3722c', '#f8961e', '#f9c74f', '#90be6d', '#43aa8b', '#577590'];
      
      for (let i = 0; i < 100; i++) {
        const confetti = document.createElement('div');
        confetti.className = 'confetti';
        confetti.style.left = Math.random() * 100 + '%';
        confetti.style.top = -Math.random() * 20 + '%';
        confetti.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
        confetti.style.transform = 'rotate(' + Math.random() * 360 + 'deg)';
        
        container.appendChild(confetti);
        
        animateConfetti(confetti);
      }
    }
    
    function animateConfetti(confetti) {
      const duration = Math.random() * 3 + 2;
      const delay = Math.random();
      
      confetti.animate([
        { transform: 'translate3d(0,0,0) rotate(0)', opacity: 1 },
        { transform: 'translate3d(' + (Math.random() * 300 - 150) + 'px,' + (Math.random() * 400 + 400) + 'px,0) rotate(' + Math.random() * 520 + 'deg)', opacity: 0 }
      ], {
        duration: duration * 1000,
        delay: delay * 1000,
        fill: 'forwards',
        easing: 'cubic-bezier(0,0.5,0.5,1)'
      });
    }`,
    tags: ['birthday', 'confetti', 'celebration', 'interactive'],
    styleTags: ['animated', 'interactive', 'colorful'],
    searchKeywords: ['birthday', 'confetti', 'celebration', 'card', 'interactive'],
    status: true,
    isFeatured: true
  },
  
  // Christmas templates
  {
    title: 'Christmas Tree Card',
    category: 'christmas',
    htmlContent: `<div class="christmas-tree-card">
      <div class="tree">
        <div class="star"></div>
        <div class="tree-top"></div>
        <div class="tree-middle"></div>
        <div class="tree-bottom"></div>
        <div class="trunk"></div>
      </div>
      <div class="message">
        <h1>Merry Christmas!</h1>
        <p>Wishing you a season filled with warmth, joy, and all the things that make you happy.</p>
        <p class="signature">From: <span class="sender">[Sender]</span></p>
      </div>
    </div>`,
    cssContent: `.christmas-tree-card {
      font-family: 'Arial', sans-serif;
      background-color: #f8f9fa;
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 40px;
      height: 100vh;
      box-sizing: border-box;
    }
    .tree {
      position: relative;
      margin-bottom: 30px;
    }
    .star {
      position: absolute;
      top: -20px;
      left: 45px;
      width: 0;
      height: 0;
      border-left: 10px solid transparent;
      border-right: 10px solid transparent;
      border-bottom: 20px solid #FFD700;
      transform: rotate(180deg);
    }
    .star:before {
      content: '';
      position: absolute;
      top: 0;
      left: -10px;
      width: 0;
      height: 0;
      border-left: 10px solid transparent;
      border-right: 10px solid transparent;
      border-bottom: 20px solid #FFD700;
      transform: rotate(72deg);
    }
    .star:after {
      content: '';
      position: absolute;
      top: 0;
      left: -10px;
      width: 0;
      height: 0;
      border-left: 10px solid transparent;
      border-right: 10px solid transparent;
      border-bottom: 20px solid #FFD700;
      transform: rotate(-72deg);
    }
    .tree-top, .tree-middle, .tree-bottom {
      width: 0;
      height: 0;
      border-left: 50px solid transparent;
      border-right: 50px solid transparent;
      margin: 0 auto;
    }
    .tree-top {
      border-bottom: 60px solid #2E7D32;
    }
    .tree-middle {
      border-bottom: 70px solid #2E7D32;
      margin-top: -20px;
    }
    .tree-bottom {
      border-bottom: 80px solid #2E7D32;
      margin-top: -20px;
    }
    .trunk {
      width: 30px;
      height: 50px;
      background-color: #795548;
      margin: 0 auto;
      margin-top: -10px;
    }
    .message {
      text-align: center;
      background-color: white;
      padding: 20px;
      border-radius: 10px;
      box-shadow: 0 4px 8px rgba(0,0,0,0.1);
      max-width: 80%;
    }
    .message h1 {
      color: #D32F2F;
      margin-bottom: 15px;
    }
    .message p {
      color: #424242;
      line-height: 1.6;
      margin-bottom: 15px;
    }
    .signature {
      font-style: italic;
      color: #616161;
    }
    .sender {
      font-weight: bold;
      color: #D32F2F;
    }`,
    tags: ['christmas', 'tree', 'card'],
    styleTags: ['festive', 'colorful', 'simple'],
    searchKeywords: ['christmas', 'tree', 'card', 'holiday'],
    status: true,
    isFeatured: true
  },
  {
    title: 'Christmas Snow Globe',
    category: 'christmas',
    htmlContent: `<div class="snow-globe-container">
      <div class="snow-globe">
        <div class="tree"></div>
        <div class="snow"></div>
        <div class="base"></div>
      </div>
      <div class="christmas-message">
        <h1>Happy Holidays!</h1>
        <p>May your days be merry and bright this Christmas season.</p>
        <p class="signature">Warm wishes,<br><span class="sender">[Your Name]</span></p>
      </div>
    </div>`,
    cssContent: `.snow-globe-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      height: 100vh;
      background-color: #f9f9f9;
      font-family: 'Roboto', sans-serif;
      padding: 20px;
    }
    .snow-globe {
      position: relative;
      width: 200px;
      height: 200px;
      border-radius: 50%;
      background: linear-gradient(to bottom, #e3f2fd, #bbdefb);
      box-shadow: 0 0 20px rgba(0,0,0,0.2), inset 0 0 50px rgba(255,255,255,0.5);
      margin-bottom: 30px;
      overflow: hidden;
    }
    .tree {
      position: absolute;
      bottom: 20px;
      left: 50%;
      transform: translateX(-50%);
      width: 0;
      height: 0;
      border-left: 40px solid transparent;
      border-right: 40px solid transparent;
      border-bottom: 100px solid #388e3c;
    }
    .tree:before {
      content: '';
      position: absolute;
      bottom: -10px;
      left: -10px;
      width: 20px;
      height: 20px;
      background-color: #795548;
    }
    .snow {
      position: absolute;
      width: 100%;
      height: 100%;
    }
    .snow:after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-image: 
        radial-gradient(circle at 20% 30%, white 2px, transparent 2px),
        radial-gradient(circle at 40% 70%, white 2px, transparent 2px),
        radial-gradient(circle at 60% 30%, white 2px, transparent 2px),
        radial-gradient(circle at 80% 50%, white 2px, transparent 2px);
      background-size: 100px 100px;
      animation: snow 8s linear infinite;
    }
    @keyframes snow {
      0% { background-position: 0 0; }
      100% { background-position: 100px 100px; }
    }
    .base {
      position: absolute;
      bottom: -25px;
      left: 50%;
      transform: translateX(-50%);
      width: 160px;
      height: 50px;
      background-color: #d32f2f;
      border-radius: 10px 10px 20px 20px;
    }
    .christmas-message {
      text-align: center;
      background-color: white;
      padding: 20px;
      border-radius: 10px;
      box-shadow: 0 4px 8px rgba(0,0,0,0.1);
      max-width: 80%;
    }
    .christmas-message h1 {
      color: #d32f2f;
      margin-bottom: 15px;
    }
    .christmas-message p {
      color: #424242;
      line-height: 1.6;
      margin-bottom: 15px;
    }
    .signature {
      font-style: italic;
      color: #616161;
    }
    .sender {
      font-weight: bold;
      color: #d32f2f;
    }`,
    jsContent: `const globe = document.querySelector('.snow-globe');
    globe.addEventListener('click', function() {
      this.style.animation = 'shake 0.5s';
      setTimeout(() => {
        this.style.animation = '';
      }, 500);
    });
    
    document.head.insertAdjacentHTML('beforeend', 
      '<style>@keyframes shake { 0%, 100% { transform: translateX(0); } 25% { transform: translateX(-5px); } 50% { transform: translateX(5px); } 75% { transform: translateX(-5px); } }</style>'
    );`,
    tags: ['christmas', 'snow globe', 'interactive', 'snow'],
    styleTags: ['festive', 'animated', 'winter'],
    searchKeywords: ['christmas', 'snow', 'globe', 'winter', 'holidays'],
    status: true
  },
  {
    title: 'Classic Christmas Greeting',
    category: 'christmas',
    htmlContent: `<div style="text-align: center; padding: 30px; font-family: 'Georgia', serif; background-color: #fafafa;">
      <div style="background-color: white; padding: 40px; border-radius: 10px; box-shadow: 0 5px 15px rgba(0,0,0,0.1); max-width: 600px; margin: 0 auto;">
        <h1 style="color: #c62828; font-size: 2.5em; margin-bottom: 20px;">Merry Christmas</h1>
        <div style="color: #388e3c; font-size: 2em; margin: 15px 0;">&#10052; &#127876; &#10052;</div>
        <p style="font-size: 1.2em; color: #424242; margin: 20px 0; line-height: 1.6;">
          Dear <span class="recipient" style="color: #c62828; font-weight: bold;">[Recipient]</span>,
        </p>
        <p style="font-size: 1.1em; color: #616161; line-height: 1.8;">
          May the magic and wonder of the Christmas season<br>
          stay with you throughout the coming year.<br>
          Wishing you peace, joy, and prosperity!
        </p>
        <div style="margin: 25px 0; color: #388e3c; font-size: 1.5em;">&#127873; &#127877; &#127876;</div>
        <p style="font-size: 1.1em; font-style: italic; color: #616161;">
          Warmest wishes,<br><span class="sender" style="color: #c62828; font-weight: bold;">[Your Name]</span>
        </p>
      </div>
    </div>`,
    tags: ['christmas', 'classic', 'elegant', 'greeting'],
    styleTags: ['traditional', 'clean', 'festive'],
    searchKeywords: ['christmas', 'greeting', 'classic', 'holiday', 'card'],
    status: true
  },
  {
    title: 'Christmas Lights Animation',
    category: 'christmas',
    htmlContent: `<div class="christmas-lights-container">
      <div class="lights-string">
        <div class="light light-1"></div>
        <div class="light light-2"></div>
        <div class="light light-3"></div>
        <div class="light light-4"></div>
        <div class="light light-5"></div>
        <div class="light light-6"></div>
        <div class="light light-7"></div>
        <div class="light light-8"></div>
      </div>
      <div class="christmas-greeting">
        <h1>Season's Greetings</h1>
        <p>May your holidays sparkle with moments of love, laughter, and goodwill.</p>
        <p class="signature">With love from,<br><span class="sender">[Sender Name]</span></p>
      </div>
    </div>`,
    cssContent: `.christmas-lights-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      min-height: 100vh;
      background-color: #263238;
      font-family: 'Arial', sans-serif;
      padding: 40px 20px;
      position: relative;
    }
    .lights-string {
      position: absolute;
      top: 50px;
      left: 0;
      width: 100%;
      height: 10px;
      background-color: #004D40;
      display: flex;
      justify-content: space-around;
    }
    .light {
      width: 20px;
      height: 20px;
      border-radius: 50%;
      position: relative;
      top: 10px;
      animation: glow 1.5s alternate infinite;
    }
    .light-1 { background-color: #f44336; animation-delay: 0s; }
    .light-2 { background-color: #2196F3; animation-delay: 0.2s; }
    .light-3 { background-color: #4CAF50; animation-delay: 0.4s; }
    .light-4 { background-color: #FFEB3B; animation-delay: 0.6s; }
    .light-5 { background-color: #9C27B0; animation-delay: 0.8s; }
    .light-6 { background-color: #FF9800; animation-delay: 1s; }
    .light-7 { background-color: #03A9F4; animation-delay: 1.2s; }
    .light-8 { background-color: #E91E63; animation-delay: 1.4s; }
    @keyframes glow {
      0% { box-shadow: 0 0 5px rgba(255,255,255,0.5); }
      100% { box-shadow: 0 0 20px 5px; }
    }
    .christmas-greeting {
      text-align: center;
      background-color: white;
      padding: 40px;
      border-radius: 10px;
      box-shadow: 0 4px 20px rgba(0,0,0,0.2);
      max-width: 80%;
      margin-top: 100px;
    }
    .christmas-greeting h1 {
      color: #c62828;
      font-size: 2.5em;
      margin-bottom: 20px;
    }
    .christmas-greeting p {
      color: #37474F;
      font-size: 1.2em;
      line-height: 1.6;
      margin-bottom: 20px;
    }
    .signature {
      font-style: italic;
      color: #546E7A;
      margin-top: 30px;
    }
    .sender {
      font-weight: bold;
      color: #c62828;
    }`,
    tags: ['christmas', 'lights', 'animation', 'greeting'],
    styleTags: ['animated', 'colorful', 'festive'],
    searchKeywords: ['christmas', 'lights', 'animation', 'holiday', 'greeting'],
    status: true,
    isFeatured: true
  },
  {
    title: 'Christmas Gift Wishlist',
    category: 'christmas',
    htmlContent: `<div class="christmas-wishlist">
      <div class="header">
        <div class="santa-hat"></div>
        <h1>My Christmas Wishlist</h1>
      </div>
      <div class="wishlist-content">
        <p class="intro">Dear <span class="recipient">[Recipient]</span>,</p>
        <p>Here are a few things I'd love for Christmas this year:</p>
        <ul class="gift-list">
          <li class="gift-item">
            <span class="gift-icon">🎁</span>
            <span class="gift-text">[Gift Item 1]</span>
          </li>
          <li class="gift-item">
            <span class="gift-icon">🎁</span>
            <span class="gift-text">[Gift Item 2]</span>
          </li>
          <li class="gift-item">
            <span class="gift-icon">🎁</span>
            <span class="gift-text">[Gift Item 3]</span>
          </li>
          <li class="gift-item">
            <span class="gift-icon">🎁</span>
            <span class="gift-text">[Gift Item 4]</span>
          </li>
          <li class="gift-item">
            <span class="gift-icon">🎁</span>
            <span class="gift-text">[Gift Item 5]</span>
          </li>
        </ul>
        <p class="signature">With love and holiday cheer,<br><span class="sender">[Your Name]</span></p>
      </div>
      <div class="footer">
        <div class="decoration"></div>
        <div class="decoration"></div>
        <div class="decoration"></div>
      </div>
    </div>`,
    cssContent: `.christmas-wishlist {
      font-family: 'Arial', sans-serif;
      max-width: 600px;
      margin: 0 auto;
      background-color: #fff;
      border-radius: 10px;
      box-shadow: 0 5px 15px rgba(0,0,0,0.1);
      overflow: hidden;
    }
    .header {
      background-color: #c62828;
      color: white;
      padding: 20px;
      text-align: center;
      position: relative;
    }
    .santa-hat {
      position: absolute;
      top: -15px;
      right: 30px;
      width: 50px;
      height: 50px;
      background-color: #c62828;
      border-radius: 50% 50% 0 50%;
      transform: rotate(30deg);
    }
    .santa-hat:before {
      content: '';
      position: absolute;
      top: -10px;
      left: 0;
      width: 20px;
      height: 20px;
      background-color: white;
      border-radius: 50%;
    }
    .header h1 {
      margin: 0;
      font-size: 2em;
    }
    .wishlist-content {
      padding: 30px;
      background-color: #fff;
    }
    .intro {
      font-size: 1.2em;
      margin-bottom: 20px;
    }
    .recipient {
      font-weight: bold;
      color: #c62828;
    }
    .gift-list {
      list-style-type: none;
      padding: 0;
      margin: 20px 0;
    }
    .gift-item {
      display: flex;
      align-items: center;
      margin-bottom: 15px;
      padding: 10px;
      background-color: #f5f5f5;
      border-radius: 5px;
      transition: transform 0.3s;
    }
    .gift-item:hover {
      transform: translateX(10px);
    }
    .gift-icon {
      font-size: 1.5em;
      margin-right: 15px;
    }
    .gift-text {
      font-size: 1.1em;
    }
    .signature {
      margin-top: 30px;
      font-style: italic;
      text-align: right;
    }
    .sender {
      font-weight: bold;
      color: #c62828;
    }
    .footer {
      background-color: #388e3c;
      height: 30px;
      display: flex;
      justify-content: center;
      align-items: center;
    }
    .decoration {
      width: 15px;
      height: 15px;
      border-radius: 50%;
      background-color: #f9a825;
      margin: 0 10px;
    }`,
    jsContent: `document.querySelectorAll('.gift-item').forEach(item => {
      item.addEventListener('click', function() {
        this.style.backgroundColor = '#e0f7fa';
        setTimeout(() => {
          this.style.backgroundColor = '#f5f5f5';
        }, 300);
      });
    });`,
    tags: ['christmas', 'wishlist', 'gift', 'interactive'],
    styleTags: ['festive', 'interactive', 'fun'],
    searchKeywords: ['christmas', 'wishlist', 'gift', 'list', 'santa'],
    status: true
  },
  
  // Happy New Year templates
  {
    title: 'New Year Countdown',
    category: 'happy new year',
    htmlContent: `<div class="new-year-container">
      <div class="countdown">
        <div class="countdown-display">
          <div id="days" class="countdown-number">00</div>
          <div class="countdown-label">Days</div>
        </div>
        <div class="countdown-display">
          <div id="hours" class="countdown-number">00</div>
          <div class="countdown-label">Hours</div>
        </div>
        <div class="countdown-display">
          <div id="minutes" class="countdown-number">00</div>
          <div class="countdown-label">Minutes</div>
        </div>
        <div class="countdown-display">
          <div id="seconds" class="countdown-number">00</div>
          <div class="countdown-label">Seconds</div>
        </div>
      </div>
      <div class="new-year-message">
        <h1>Happy New Year <span class="year">2023</span>!</h1>
        <p>Wishing you 365 days of success, happiness, and all the good things you deserve.</p>
        <p class="signature">Best wishes,<br><span class="sender">[Your Name]</span></p>
      </div>
      <div class="fireworks"></div>
    </div>`,
    cssContent: `.new-year-container {
      font-family: 'Arial', sans-serif;
      min-height: 100vh;
      background: linear-gradient(to bottom, #0D1B2A, #1B263B);
      color: white;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 20px;
      position: relative;
      overflow: hidden;
    }
    .countdown {
      display: flex;
      justify-content: center;
      flex-wrap: wrap;
      margin-bottom: 40px;
    }
    .countdown-display {
      background-color: rgba(255, 255, 255, 0.1);
      border-radius: 10px;
      padding: 15px;
      margin: 10px;
      min-width: 80px;
      text-align: center;
    }
    .countdown-number {
      font-size: 2.5em;
      font-weight: bold;
      margin-bottom: 5px;
    }
    .countdown-label {
      font-size: 0.9em;
      text-transform: uppercase;
      opacity: 0.7;
    }
    .new-year-message {
      background-color: rgba(255, 255, 255, 0.1);
      padding: 30px;
      border-radius: 15px;
      text-align: center;
      max-width: 600px;
      margin-bottom: 30px;
    }
    .new-year-message h1 {
      font-size: 2.5em;
      margin-bottom: 20px;
      color: #FFD700;
    }
    .year {
      font-weight: bold;
      color: #FFD700;
    }
    .new-year-message p {
      font-size: 1.2em;
      line-height: 1.6;
      margin-bottom: 20px;
    }
    .signature {
      font-style: italic;
      opacity: 0.9;
      margin-top: 30px;
    }
    .sender {
      font-weight: bold;
      color: #FFD700;
    }
    .fireworks {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      pointer-events: none;
      z-index: -1;
    }`,
    jsContent: `// Set the New Year's date - update this each year
const newYearDate = new Date(new Date().getFullYear() + 1, 0, 1).getTime();

// Update the countdown every second
const countdownTimer = setInterval(function() {
  // Get today's date and time
  const now = new Date().getTime();
  
  // Find the distance between now and New Year
  const distance = newYearDate - now;
  
  // Time calculations for days, hours, minutes and seconds
  const days = Math.floor(distance / (1000 * 60 * 60 * 24));
  const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
  const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
  const seconds = Math.floor((distance % (1000 * 60)) / 1000);
  
  // Display the result
  document.getElementById("days").textContent = days.toString().padStart(2, '0');
  document.getElementById("hours").textContent = hours.toString().padStart(2, '0');
  document.getElementById("minutes").textContent = minutes.toString().padStart(2, '0');
  document.getElementById("seconds").textContent = seconds.toString().padStart(2, '0');
  
  // Update the year
  document.querySelector('.year').textContent = new Date().getFullYear() + 1;
  
  // If the countdown is finished, display Happy New Year
  if (distance < 0) {
    clearInterval(countdownTimer);
    document.querySelector('.countdown').innerHTML = "<h2>Happy New Year!</h2>";
    createFireworks();
  }
}, 1000);

function createFireworks() {
  const fireworks = document.querySelector('.fireworks');
  fireworks.innerHTML = '';
  
  for (let i = 0; i < 10; i++) {
    setTimeout(() => {
      const firework = document.createElement('div');
      firework.className = 'firework';
      firework.style.left = Math.random() * 100 + '%';
      firework.style.top = Math.random() * 50 + '%';
      firework.style.backgroundColor = 'hsl(' + Math.random() * 360 + ', 100%, 50%)';
      firework.style.width = '5px';
      firework.style.height = '5px';
      firework.style.borderRadius = '50%';
      firework.style.position = 'absolute';
      firework.style.boxShadow = '0 0 10px 5px ' + firework.style.backgroundColor;
      
      fireworks.appendChild(firework);
      
      firework.animate([
        { transform: 'scale(1)', opacity: 1 },
        { transform: 'scale(30)', opacity: 0 }
      ], {
        duration: 1000,
        easing: 'cubic-bezier(0,0,0.2,1)'
      });
      
      setTimeout(() => {
        firework.remove();
      }, 1000);
    }, i * 300);
  }
}`,
    tags: ['new year', 'countdown', 'celebration', 'interactive'],
    styleTags: ['modern', 'dark', 'animated'],
    searchKeywords: ['new year', 'countdown', 'celebration', 'timer', 'interactive'],
    status: true,
    isFeatured: true
  },
  {
    title: 'Elegant New Year Greeting',
    category: 'happy new year',
    htmlContent: `<div style="text-align: center; padding: 30px; font-family: 'Playfair Display', serif; background-color: #f9f9f9;">
      <div style="background-color: white; padding: 40px; border-radius: 10px; box-shadow: 0 5px 15px rgba(0,0,0,0.1); max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0;">
        <h1 style="color: #212121; font-size: 2.8em; margin-bottom: 20px;">Happy New Year</h1>
        <div style="color: #FFD700; font-size: 2em; margin: 15px 0;">✨ <span class="year" style="color: #212121;">2023</span> ✨</div>
        <p style="font-size: 1.2em; color: #424242; margin: 20px 0; line-height: 1.6;">
          Dear <span class="recipient" style="color: #212121; font-weight: bold;">[Recipient]</span>,
        </p>
        <p style="font-size: 1.1em; color: #616161; line-height: 1.8;">
          As we bid farewell to the past year and welcome the new one,<br>
          I'd like to thank you for being a part of my journey.<br>
          May the coming year bring you joy, prosperity, and countless<br>
          opportunities to create beautiful memories.
        </p>
        <div style="margin: 25px 0; color: #FFD700; font-size: 1.5em;">✨ 🥂 🎊</div>
        <p style="font-size: 1.1em; font-style: italic; color: #616161;">
          With gratitude and best wishes,<br><span class="sender" style="color: #212121; font-weight: bold;">[Your Name]</span>
        </p>
      </div>
    </div>`,
    jsContent: `// Update the year dynamically
document.querySelector('.year').textContent = new Date().getFullYear() + 1;`,
    tags: ['new year', 'elegant', 'greeting', 'simple'],
    styleTags: ['elegant', 'clean', 'minimalist'],
    searchKeywords: ['new year', 'greeting', 'elegant', 'wishes', 'simple'],
    status: true
  },
  {
    title: 'New Year Resolutions List',
    category: 'happy new year',
    htmlContent: `<div class="resolution-container">
      <header>
        <h1>My New Year Resolutions</h1>
        <div class="year-badge"><span class="current-year">2023</span></div>
      </header>
      <div class="resolutions-list">
        <div class="resolution-item">
          <input type="checkbox" id="resolution1">
          <label for="resolution1">[Resolution 1]</label>
        </div>
        <div class="resolution-item">
          <input type="checkbox" id="resolution2">
          <label for="resolution2">[Resolution 2]</label>
        </div>
        <div class="resolution-item">
          <input type="checkbox" id="resolution3">
          <label for="resolution3">[Resolution 3]</label>
        </div>
        <div class="resolution-item">
          <input type="checkbox" id="resolution4">
          <label for="resolution4">[Resolution 4]</label>
        </div>
        <div class="resolution-item">
          <input type="checkbox" id="resolution5">
          <label for="resolution5">[Resolution 5]</label>
        </div>
        <div class="add-resolution">
          <input type="text" placeholder="Add a new resolution...">
          <button>Add</button>
        </div>
      </div>
      <div class="message">
        <p>This year I'm committed to becoming the best version of myself!</p>
        <p class="signature">- <span class="name">[Your Name]</span></p>
      </div>
    </div>`,
    cssContent: `.resolution-container {
      font-family: 'Roboto', sans-serif;
      max-width: 600px;
      margin: 0 auto;
      background-color: #f5f5f5;
      border-radius: 10px;
      box-shadow: 0 5px 20px rgba(0,0,0,0.1);
      overflow: hidden;
    }
    header {
      background-color: #3949ab;
      color: white;
      padding: 30px;
      text-align: center;
      position: relative;
    }
    header h1 {
      margin: 0;
      font-size: 2.2em;
    }
    .year-badge {
      position: absolute;
      top: 10px;
      right: 10px;
      background-color: #ffeb3b;
      color: #212121;
      padding: 5px 10px;
      border-radius: 20px;
      font-weight: bold;
    }
    .resolutions-list {
      padding: 30px;
      background-color: white;
    }
    .resolution-item {
      display: flex;
      align-items: center;
      margin-bottom: 15px;
      padding-bottom: 15px;
      border-bottom: 1px solid #f0f0f0;
    }
    .resolution-item input[type="checkbox"] {
      margin-right: 15px;
      transform: scale(1.2);
    }
    .resolution-item label {
      font-size: 1.1em;
      color: #424242;
      cursor: pointer;
      transition: color 0.3s, text-decoration 0.3s;
    }
    .resolution-item input[type="checkbox"]:checked + label {
      color: #9e9e9e;
      text-decoration: line-through;
    }
    .add-resolution {
      display: flex;
      margin-top: 20px;
    }
    .add-resolution input {
      flex: 1;
      padding: 10px;
      border: 1px solid #e0e0e0;
      border-radius: 4px 0 0 4px;
      font-size: 1em;
    }
    .add-resolution button {
      padding: 10px 15px;
      background-color: #3949ab;
      color: white;
      border: none;
      border-radius: 0 4px 4px 0;
      cursor: pointer;
      font-size: 1em;
    }
    .message {
      padding: 20px 30px;
      background-color: #e8eaf6;
      text-align: center;
    }
    .message p {
      margin: 0;
      font-size: 1.1em;
      color: #3949ab;
      line-height: 1.6;
    }
    .signature {
      margin-top: 10px;
      font-style: italic;
    }
    .name {
      font-weight: bold;
    }`,
    jsContent: `// Update the year
document.querySelector('.current-year').textContent = new Date().getFullYear() + 1;

// Handle adding new resolutions
const addButton = document.querySelector('.add-resolution button');
const addInput = document.querySelector('.add-resolution input');
const resolutionsList = document.querySelector('.resolutions-list');

addButton.addEventListener('click', addResolution);
addInput.addEventListener('keypress', function(e) {
  if (e.key === 'Enter') {
    addResolution();
  }
});

function addResolution() {
  const text = addInput.value.trim();
  if (text) {
    const id = 'resolution' + (document.querySelectorAll('.resolution-item').length + 1);
    
    const item = document.createElement('div');
    item.className = 'resolution-item';
    
    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.id = id;
    
    const label = document.createElement('label');
    label.setAttribute('for', id);
    label.textContent = text;
    
    item.appendChild(checkbox);
    item.appendChild(label);
    
    resolutionsList.insertBefore(item, document.querySelector('.add-resolution'));
    
    addInput.value = '';
  }
}`,
    tags: ['new year', 'resolutions', 'interactive', 'list'],
    styleTags: ['modern', 'interactive', 'functional'],
    searchKeywords: ['new year', 'resolutions', 'list', 'goals', 'interactive'],
    status: true
  },
  {
    title: 'New Year Fireworks Animation',
    category: 'happy new year',
    htmlContent: `<div class="fireworks-container">
      <div class="sky"></div>
      <div class="message-container">
        <h1 class="new-year-text">Happy New Year <span class="year">2023</span>!</h1>
        <p class="wishes">May all your dreams and wishes come true in the coming year.</p>
        <p class="signature">Cheers to new beginnings!<br><span class="sender">[Your Name]</span></p>
      </div>
    </div>`,
    cssContent: `.fireworks-container {
      position: relative;
      min-height: 100vh;
      background-color: #0a0e21;
      overflow: hidden;
      font-family: 'Arial', sans-serif;
    }
    .sky {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
    }
    .message-container {
      position: relative;
      z-index: 10;
      text-align: center;
      color: white;
      padding: 40px 20px;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background-color: rgba(10, 14, 33, 0.5);
    }
    .new-year-text {
      font-size: 3em;
      margin-bottom: 20px;
      text-shadow: 0 0 10px rgba(255,255,255,0.7);
      animation: pulse 2s infinite;
    }
    @keyframes pulse {
      0% { transform: scale(1); }
      50% { transform: scale(1.05); }
      100% { transform: scale(1); }
    }
    .year {
      color: #FFD700;
      font-weight: bold;
    }
    .wishes {
      font-size: 1.5em;
      margin-bottom: 30px;
      max-width: 600px;
    }
    .signature {
      font-style: italic;
      margin-top: 30px;
      font-size: 1.2em;
    }
    .sender {
      font-weight: bold;
      color: #FFD700;
    }
    .firework {
      position: absolute;
      border-radius: 50%;
    }`,
    jsContent: `// Update the year
document.querySelector('.year').textContent = new Date().getFullYear() + 1;

// Create fireworks
function createFireworks() {
  const sky = document.querySelector('.sky');
  
  // Create fireworks at random positions
  setInterval(() => {
    const xPos = Math.random() * 100;
    const yPos = Math.random() * 60 + 10;
    
    createFirework(sky, xPos, yPos);
  }, 800);
}

function createFirework(container, x, y) {
  // Create the firework center
  const firework = document.createElement('div');
  firework.className = 'firework';
  firework.style.left = x + '%';
  firework.style.top = y + '%';
  
  // Random color
  const hue = Math.floor(Math.random() * 360);
  const color = 'hsl(' + hue + ', 100%, 60%)';
  
  firework.style.backgroundColor = color;
  firework.style.boxShadow = '0 0 8px ' + color;
  
  container.appendChild(firework);
  
  // Animate the explosion
  setTimeout(() => {
    firework.remove();
    createExplosion(container, x, y, color);
  }, 300);
}

function createExplosion(container, x, y, color) {
  // Number of particles
  const particles = 30;
  
  for (let i = 0; i < particles; i++) {
    const particle = document.createElement('div');
    particle.className = 'firework';
    particle.style.left = x + '%';
    particle.style.top = y + '%';
    particle.style.backgroundColor = color;
    particle.style.width = '3px';
    particle.style.height = '3px';
    
    container.appendChild(particle);
    
    // Random angle and distance
    const angle = Math.random() * Math.PI * 2;
    const distance = Math.random() * 10 + 5;
    
    // Calculate end position
    const endX = x + Math.cos(angle) * distance;
    const endY = y + Math.sin(angle) * distance;
    
    // Animate particle
    particle.animate([
      { transform: 'translate(0, 0) scale(1)', opacity: 1 },
      { transform: 'translate(' + (endX - x) + 'vw, ' + (endY - y) + 'vh) scale(0)', opacity: 0 }
    ], {
      duration: 1000,
      easing: 'cubic-bezier(0,0,0.2,1)',
      fill: 'forwards'
    });
    
    // Remove particle after animation
    setTimeout(() => {
      particle.remove();
    }, 1000);
  }
}

// Start fireworks when page loads
window.addEventListener('load', createFireworks);`,
    tags: ['new year', 'fireworks', 'animation', 'celebration'],
    styleTags: ['animated', 'colorful', 'festive'],
    searchKeywords: ['new year', 'fireworks', 'animation', 'celebration', 'effects'],
    status: true,
    isFeatured: true
  },
  {
    title: 'New Year Photo Frame',
    category: 'happy new year',
    htmlContent: `<div class="new-year-frame">
      <div class="frame-header">
        <div class="decoration left"></div>
        <h1>Happy New Year <span class="year">2023</span></h1>
        <div class="decoration right"></div>
      </div>
      <div class="photo-container">
        <div class="photo-placeholder">
          <div class="placeholder-icon">📷</div>
          <div class="placeholder-text">Your Photo Here</div>
        </div>
      </div>
      <div class="frame-footer">
        <p class="new-year-wish">Cheers to a new year and another chance for us to get it right!</p>
        <p class="signature">With love,<br><span class="sender">[Your Name]</span></p>
      </div>
      <div class="confetti-container"></div>
    </div>`,
    cssContent: `.new-year-frame {
      font-family: 'Arial', sans-serif;
      max-width: 600px;
      margin: 0 auto;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 15px;
      box-shadow: 0 10px 30px rgba(0,0,0,0.2);
      padding: 30px;
      position: relative;
      overflow: hidden;
      color: white;
    }
    .frame-header {
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 30px;
    }
    .decoration {
      width: 40px;
      height: 40px;
      background-color: #FFD700;
      border-radius: 50%;
      margin: 0 15px;
      position: relative;
    }
    .decoration:before {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 30px;
      height: 30px;
      border: 2px solid white;
      border-radius: 50%;
    }
    .frame-header h1 {
      font-size: 2.2em;
      margin: 0;
      text-align: center;
      text-shadow: 0 2px 4px rgba(0,0,0,0.2);
    }
    .year {
      color: #FFD700;
      font-weight: bold;
    }
    .photo-container {
      background-color: white;
      border-radius: 10px;
      height: 300px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 30px;
      position: relative;
      overflow: hidden;
      box-shadow: 0 5px 15px rgba(0,0,0,0.1);
    }
    .photo-placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      color: #9e9e9e;
    }
    .placeholder-icon {
      font-size: 3em;
      margin-bottom: 10px;
    }
    .placeholder-text {
      font-size: 1.2em;
    }
    .frame-footer {
      text-align: center;
      padding: 20px 0;
    }
    .new-year-wish {
      font-size: 1.2em;
      margin-bottom: 20px;
      line-height: 1.6;
    }
    .signature {
      font-style: italic;
      margin-top: 20px;
    }
    .sender {
      font-weight: bold;
      color: #FFD700;
    }
    .confetti-container {
      position: absolute;
      width: 100%;
      height: 100%;
      top: 0;
      left: 0;
      pointer-events: none;
    }
    .confetti {
      position: absolute;
      width: 8px;
      height: 8px;
      opacity: 0.7;
    }`,
    jsContent: `// Update the year
document.querySelector('.year').textContent = new Date().getFullYear() + 1;

// Create confetti
function createConfetti() {
  const container = document.querySelector('.confetti-container');
  const colors = ['#FFD700', '#FF6B6B', '#4ecdc4', '#45B8AC', '#9D65C9', '#FF9A76'];
  
  for (let i = 0; i < 50; i++) {
    const confetti = document.createElement('div');
    confetti.className = 'confetti';
    confetti.style.left = Math.random() * 100 + '%';
    confetti.style.top = -Math.random() * 20 - 10 + '%';
    confetti.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
    confetti.style.transform = 'rotate(' + Math.random() * 360 + 'deg)';
    
    container.appendChild(confetti);
    
    animateConfetti(confetti);
  }
}

function animateConfetti(confetti) {
  const speed = Math.random() * 3 + 2;
  const rotation = Math.random() * 360;
  const delay = Math.random() * 5;
  
  confetti.animate([
    { transform: 'translate3d(0,0,0) rotate(0)', opacity: 1 },
    { transform: 'translate3d(' + (Math.random() * 100 - 50) + 'px, 100vh, 0) rotate(' + rotation + 'deg)', opacity: 0 }
  ], {
    duration: speed * 1000,
    delay: delay * 1000,
    fill: 'forwards'
  });
  
  setTimeout(() => {
    confetti.remove();
    const newConfetti = confetti.cloneNode();
    newConfetti.style.left = Math.random() * 100 + '%';
    newConfetti.style.top = -10 + '%';
    document.querySelector('.confetti-container').appendChild(newConfetti);
    animateConfetti(newConfetti);
  }, (speed + delay) * 1000);
}

window.addEventListener('load', createConfetti);

// Allow photo upload functionality (placeholder)
const photoContainer = document.querySelector('.photo-container');
photoContainer.addEventListener('click', function() {
  alert('In a real application, this would open a file picker to let you upload a photo.');
});`,
    tags: ['new year', 'photo frame', 'interactive', 'celebration'],
    styleTags: ['colorful', 'interactive', 'modern'],
    searchKeywords: ['new year', 'photo', 'frame', 'celebration', 'picture'],
    status: true
  },
  
  // Good Morning templates
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
async function insertTemplates() {
  try {
    // Delete existing templates with these categories (optional)
    await Template.deleteMany({ 
      category: { 
        $in: ['birthday', 'christmas', 'happy new year', 'good morning'] 
      } 
    });
    
    // Insert new templates
    const result = await Template.insertMany(templates);
    console.log(`${result.length} templates inserted successfully`);
    
    mongoose.disconnect();
    console.log('MongoDB disconnected');
  } catch (error) {
    console.error('Error inserting templates:', error);
    mongoose.disconnect();
    process.exit(1);
  }
}

// Run the function
insertTemplates(); 