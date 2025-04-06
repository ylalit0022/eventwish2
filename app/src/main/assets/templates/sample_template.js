// Wait for the DOM to be fully loaded
document.addEventListener('DOMContentLoaded', function() {
    // Initialize color buttons
    const colorButtons = document.querySelectorAll('.color-btn');
    const messageBox = document.querySelector('.message-box');
    
    colorButtons.forEach(button => {
        button.addEventListener('click', function() {
            const color = this.getAttribute('data-color');
            messageBox.style.backgroundColor = color;
            
            // Add animation effect
            messageBox.style.transform = 'scale(1.05)';
            setTimeout(() => {
                messageBox.style.transform = 'scale(1)';
            }, 200);
        });
    });
    
    // Add hover effect to the card
    const card = document.querySelector('.card');
    if (card) {
        card.addEventListener('mouseenter', function() {
            this.style.transform = 'scale(1.05)';
        });
        
        card.addEventListener('mouseleave', function() {
            this.style.transform = 'scale(1)';
        });
    }
    
    // Add greeting animation
    const greeting = document.querySelector('.greeting');
    if (greeting) {
        greeting.style.opacity = '0';
        greeting.style.transform = 'translateY(20px)';
        
        setTimeout(() => {
            greeting.style.transition = 'all 0.5s ease';
            greeting.style.opacity = '1';
            greeting.style.transform = 'translateY(0)';
        }, 500);
    }
    
    // Add signature animation
    const signature = document.querySelector('.signature');
    if (signature) {
        signature.style.opacity = '0';
        signature.style.transform = 'translateX(-20px)';
        
        setTimeout(() => {
            signature.style.transition = 'all 0.5s ease';
            signature.style.opacity = '1';
            signature.style.transform = 'translateX(0)';
        }, 1000);
    }
    
    // Add message box hover effect
    if (messageBox) {
        messageBox.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-5px)';
            this.style.transition = 'transform 0.3s ease';
        });
        
        messageBox.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0)';
        });
    }
    
    // Add smooth scroll behavior
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth'
                });
            }
        });
    });
}); 