document.addEventListener('DOMContentLoaded', () => {
    // Initial load of category icons
    loadCategoryIcons();

    // Form submission handler
    const iconForm = document.getElementById('iconForm');
    iconForm.addEventListener('submit', handleFormSubmit);
});

// Function to load and display category icons
async function loadCategoryIcons() {
    try {
        const response = await fetch('/api/categoryIcons');
        const data = await response.json();

        if (data.success) {
            displayIcons(data.data);
        } else {
            console.error('Failed to load category icons:', data.message);
        }
    } catch (error) {
        console.error('Error loading category icons:', error);
    }
}

// Function to display icons in the grid
function displayIcons(icons) {
    const iconsList = document.getElementById('iconsList');
    iconsList.innerHTML = '';

    icons.forEach(icon => {
        const iconCard = document.createElement('div');
        iconCard.className = 'icon-card';
        iconCard.innerHTML = `
            <img src="${icon.categoryIcon}" alt="${icon.category}" class="icon-preview">
            <div class="icon-info">
                <h3>${icon.category}</h3>
            </div>
            <div class="icon-actions">
                <button class="btn-edit" onclick="handleEdit('${icon._id}')">Edit</button>
                <button class="btn-delete" onclick="handleDelete('${icon._id}')">Delete</button>
            </div>
        `;
        iconsList.appendChild(iconCard);
    });
}

// Function to handle form submission
async function handleFormSubmit(event) {
    event.preventDefault();

    const category = document.getElementById('category').value;
    const categoryIcon = document.getElementById('categoryIcon').value;

    try {
        const response = await fetch('/api/categoryIcons', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ category, categoryIcon })
        });

        const data = await response.json();

        if (data.success) {
            // Reset form and reload icons
            event.target.reset();
            loadCategoryIcons();
        } else {
            console.error('Failed to add category icon:', data.message);
        }
    } catch (error) {
        console.error('Error adding category icon:', error);
    }
}

// Function to handle icon deletion
async function handleDelete(iconId) {
    if (!confirm('Are you sure you want to delete this icon?')) return;

    try {
        const response = await fetch(`/api/categoryIcons/${iconId}`, {
            method: 'DELETE'
        });

        const data = await response.json();

        if (data.success) {
            loadCategoryIcons();
        } else {
            console.error('Failed to delete category icon:', data.message);
        }
    } catch (error) {
        console.error('Error deleting category icon:', error);
    }
}

// Function to handle icon editing
async function handleEdit(iconId) {
    const newIconUrl = prompt('Enter new icon URL:');
    if (!newIconUrl) return;

    try {
        const response = await fetch(`/api/categoryIcons/${iconId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ categoryIcon: newIconUrl })
        });

        const data = await response.json();

        if (data.success) {
            loadCategoryIcons();
        } else {
            console.error('Failed to update category icon:', data.message);
        }
    } catch (error) {
        console.error('Error updating category icon:', error);
    }
}