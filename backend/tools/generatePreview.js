const fs = require('fs');
const path = require('path');
const { createCanvas } = require('canvas');

// Create a 600x400 canvas
const canvas = createCanvas(600, 400);
const ctx = canvas.getContext('2d');

// Background
ctx.fillStyle = '#FFC107';
ctx.fillRect(0, 0, 600, 400);

// Decorative elements
ctx.fillStyle = '#FFE082';
ctx.globalAlpha = 0.5;

// Circles
function drawCircle(x, y, radius) {
    ctx.beginPath();
    ctx.arc(x, y, radius, 0, Math.PI * 2);
    ctx.fill();
}

drawCircle(100, 100, 50);
drawCircle(500, 300, 70);

ctx.globalAlpha = 0.3;
drawCircle(300, 200, 100);

// Reset opacity
ctx.globalAlpha = 1;

// Gift box
ctx.fillStyle = '#FFFFFF';
ctx.fillRect(250, 150, 100, 100);

ctx.fillStyle = '#FF5722';
ctx.fillRect(290, 150, 20, 100); // Vertical ribbon
ctx.fillRect(250, 190, 100, 20); // Horizontal ribbon

// Gift box bow
ctx.beginPath();
ctx.moveTo(300, 130);
ctx.lineTo(320, 150);
ctx.lineTo(280, 150);
ctx.closePath();
ctx.fill();

// Text
ctx.fillStyle = '#FFFFFF';
ctx.textAlign = 'center';

// Title
ctx.font = 'bold 36px Arial';
ctx.fillText('EventWish', 300, 100);

// Subtitle
ctx.font = '24px Arial';
ctx.fillText('A Special Wish For You', 300, 300);

// Save the image
const buffer = canvas.toBuffer('image/png');
const outputPath = path.join(__dirname, '..', 'public', 'images', 'default-preview.png');

// Ensure directory exists
const dir = path.dirname(outputPath);
if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
}

fs.writeFileSync(outputPath, buffer);
console.log(`Preview image saved to ${outputPath}`); 