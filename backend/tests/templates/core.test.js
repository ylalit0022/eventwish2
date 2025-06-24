const request = require('supertest');
const mongoose = require('mongoose');
const app = require('../../server');
const Template = require('../../models/Template');

describe('Template Core API', () => {
    let testTemplate;
    let validTemplateData;

    beforeAll(async () => {
        // Connect to test database
        if (mongoose.connection.readyState === 0) {
            await mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish_test');
        }
    });

    beforeEach(async () => {
        // Clean up templates collection
        await Template.deleteMany({});
        
        // Create valid template data for testing
        validTemplateData = {
            title: 'Test Birthday Card',
            category: 'birthday',
            htmlContent: '<div>Happy Birthday!</div>',
            cssContent: '.card { background: blue; }',
            jsContent: 'console.log("Birthday card loaded");',
            previewUrl: 'https://example.com/preview.jpg',
            tags: ['birthday', 'celebration'],
            styleTags: ['colorful', 'modern'],
            searchKeywords: ['birthday', 'party', 'celebration'],
            templateType: 'html',
            status: true,
            isPremium: false,
            price: 0
        };

        // Create a test template
        testTemplate = await Template.create(validTemplateData);
    });

    afterEach(async () => {
        // Clean up after each test
        await Template.deleteMany({});
    });

    afterAll(async () => {
        // Close database connection
        await mongoose.connection.close();
    });

    describe('GET /api/templates/core/content/:id', () => {
        it('should get template content by valid ID', async () => {
            const response = await request(app)
                .get(`/api/templates/core/content/${testTemplate._id}`)
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data).toHaveProperty('title', 'Test Birthday Card');
            expect(response.body.data).toHaveProperty('htmlContent');
            expect(response.body.data).toHaveProperty('cssContent');
            expect(response.body.data).toHaveProperty('jsContent');
            expect(response.body).toHaveProperty('requestId');
        });

        it('should return 400 for invalid ObjectId format', async () => {
            const response = await request(app)
                .get('/api/templates/core/content/invalid-id')
                .expect(400);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('VALIDATION_ERROR');
            expect(response.body.error.message).toContain('Invalid template ID format');
            expect(response.body.error.field).toBe('id');
        });

        it('should return 404 for non-existent template', async () => {
            const nonExistentId = new mongoose.Types.ObjectId();
            const response = await request(app)
                .get(`/api/templates/core/content/${nonExistentId}`)
                .expect(404);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('NOT_FOUND_ERROR');
            expect(response.body.error.message).toContain('not found');
        });
    });

    describe('PUT /api/templates/core/content/:id/title', () => {
        it('should update template title successfully', async () => {
            const newTitle = 'Updated Birthday Card Title';
            const response = await request(app)
                .put(`/api/templates/core/content/${testTemplate._id}/title`)
                .send({ title: newTitle })
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data.title).toBe(newTitle);
            expect(response.body.data.updatedAt).toBeDefined();

            // Verify in database
            const updatedTemplate = await Template.findById(testTemplate._id);
            expect(updatedTemplate.title).toBe(newTitle);
        });

        it('should return 400 for missing title', async () => {
            const response = await request(app)
                .put(`/api/templates/core/content/${testTemplate._id}/title`)
                .send({})
                .expect(400);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('VALIDATION_ERROR');
            expect(response.body.error.message).toContain('Title is required');
        });

        it('should return 400 for title too long', async () => {
            const longTitle = 'a'.repeat(256); // Assuming max length is 255
            const response = await request(app)
                .put(`/api/templates/core/content/${testTemplate._id}/title`)
                .send({ title: longTitle })
                .expect(400);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('VALIDATION_ERROR');
            expect(response.body.error.message).toContain('too long');
        });
    });

    describe('PUT /api/templates/core/content/:id/category', () => {
        it('should update template category successfully', async () => {
            const newCategory = 'anniversary';
            const response = await request(app)
                .put(`/api/templates/core/content/${testTemplate._id}/category`)
                .send({ category: newCategory })
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data.category).toBe(newCategory);

            // Verify in database
            const updatedTemplate = await Template.findById(testTemplate._id);
            expect(updatedTemplate.category).toBe(newCategory);
        });

        it('should return 400 for invalid category', async () => {
            const response = await request(app)
                .put(`/api/templates/core/content/${testTemplate._id}/category`)
                .send({ category: 'invalid-category' })
                .expect(400);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('VALIDATION_ERROR');
        });
    });

    describe('PUT /api/templates/core/content/:id/html', () => {
        it('should update HTML content successfully', async () => {
            const newHtmlContent = '<div class="birthday-card"><h1>Happy Birthday!</h1></div>';
            const response = await request(app)
                .put(`/api/templates/core/content/${testTemplate._id}/html`)
                .send({ htmlContent: newHtmlContent })
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data.htmlContent).toBe(newHtmlContent);
        });

        it('should sanitize dangerous HTML content', async () => {
            const dangerousHtml = '<script>alert("xss")</script><div>Safe content</div>';
            const response = await request(app)
                .put(`/api/templates/core/content/${testTemplate._id}/html`)
                .send({ htmlContent: dangerousHtml })
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data.htmlContent).not.toContain('<script>');
            expect(response.body.data.htmlContent).toContain('Safe content');
        });
    });

    describe('PUT /api/templates/core/content/:id/css', () => {
        it('should update CSS content successfully', async () => {
            const newCssContent = '.birthday-card { background: linear-gradient(45deg, #ff6b6b, #4ecdc4); }';
            const response = await request(app)
                .put(`/api/templates/core/content/${testTemplate._id}/css`)
                .send({ cssContent: newCssContent })
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data.cssContent).toBe(newCssContent);
        });
    });

    describe('PUT /api/templates/core/content/:id/javascript', () => {
        it('should update JavaScript content successfully', async () => {
            const newJsContent = 'document.addEventListener("DOMContentLoaded", function() { console.log("Birthday card ready!"); });';
            const response = await request(app)
                .put(`/api/templates/core/content/${testTemplate._id}/javascript`)
                .send({ jsContent: newJsContent })
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data.jsContent).toBe(newJsContent);
        });
    });

    describe('PUT /api/templates/core/content/:id/preview', () => {
        it('should update preview URL successfully', async () => {
            const newPreviewUrl = 'https://example.com/new-preview.jpg';
            const response = await request(app)
                .put(`/api/templates/core/content/${testTemplate._id}/preview`)
                .send({ previewUrl: newPreviewUrl })
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data.previewUrl).toBe(newPreviewUrl);
        });

        it('should return 400 for invalid URL format', async () => {
            const response = await request(app)
                .put(`/api/templates/core/content/${testTemplate._id}/preview`)
                .send({ previewUrl: 'not-a-valid-url' })
                .expect(400);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('VALIDATION_ERROR');
            expect(response.body.error.message).toContain('Invalid URL format');
        });
    });

    describe('PUT /api/templates/core/content/:id/tags', () => {
        it('should update tags successfully', async () => {
            const newTags = ['birthday', 'party', 'celebration', 'colorful'];
            const response = await request(app)
                .put(`/api/templates/core/content/${testTemplate._id}/tags`)
                .send({ tags: newTags })
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data.tags).toEqual(expect.arrayContaining(newTags));
        });

        it('should return 400 for non-array tags', async () => {
            const response = await request(app)
                .put(`/api/templates/core/content/${testTemplate._id}/tags`)
                .send({ tags: 'not-an-array' })
                .expect(400);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('VALIDATION_ERROR');
        });
    });

    describe('DELETE /api/templates/core/content/:id', () => {
        it('should delete template successfully', async () => {
            const response = await request(app)
                .delete(`/api/templates/core/content/${testTemplate._id}`)
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.message).toContain('deleted successfully');

            // Verify template is deleted
            const deletedTemplate = await Template.findById(testTemplate._id);
            expect(deletedTemplate).toBeNull();
        });

        it('should return 404 when deleting non-existent template', async () => {
            const nonExistentId = new mongoose.Types.ObjectId();
            const response = await request(app)
                .delete(`/api/templates/core/content/${nonExistentId}`)
                .expect(404);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('NOT_FOUND_ERROR');
        });
    });

    describe('POST /api/templates/core/content', () => {
        it('should create new template successfully', async () => {
            const newTemplateData = {
                title: 'New Anniversary Card',
                category: 'anniversary',
                htmlContent: '<div>Happy Anniversary!</div>',
                cssContent: '.anniversary { color: gold; }',
                templateType: 'html',
                tags: ['anniversary', 'love'],
                status: true,
                isPremium: false,
                price: 0
            };

            const response = await request(app)
                .post('/api/templates/core/content')
                .send(newTemplateData)
                .expect(201);

            expect(response.body.success).toBe(true);
            expect(response.body.data.title).toBe('New Anniversary Card');
            expect(response.body.data._id).toBeDefined();
            expect(response.body.data.createdAt).toBeDefined();
        });

        it('should return 400 for missing required fields', async () => {
            const incompleteData = {
                title: 'Incomplete Template'
                // Missing required fields
            };

            const response = await request(app)
                .post('/api/templates/core/content')
                .send(incompleteData)
                .expect(400);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('VALIDATION_ERROR');
        });
    });

    describe('Response Format Validation', () => {
        it('should include requestId in all responses', async () => {
            const response = await request(app)
                .get(`/api/templates/core/content/${testTemplate._id}`)
                .expect(200);

            expect(response.body).toHaveProperty('requestId');
            expect(typeof response.body.requestId).toBe('string');
            expect(response.body.requestId).toMatch(/^req_\d+_[a-z0-9]+$/);
        });

        it('should include timestamp in error responses', async () => {
            const response = await request(app)
                .get('/api/templates/core/content/invalid-id')
                .expect(400);

            expect(response.body.error).toHaveProperty('timestamp');
            expect(new Date(response.body.error.timestamp)).toBeInstanceOf(Date);
        });

        it('should have consistent success response structure', async () => {
            const response = await request(app)
                .get(`/api/templates/core/content/${testTemplate._id}`)
                .expect(200);

            expect(response.body).toHaveProperty('success', true);
            expect(response.body).toHaveProperty('data');
            expect(response.body).toHaveProperty('requestId');
        });

        it('should have consistent error response structure', async () => {
            const response = await request(app)
                .get('/api/templates/core/content/invalid-id')
                .expect(400);

            expect(response.body).toHaveProperty('success', false);
            expect(response.body).toHaveProperty('error');
            expect(response.body.error).toHaveProperty('code');
            expect(response.body.error).toHaveProperty('message');
            expect(response.body.error).toHaveProperty('timestamp');
            expect(response.body).toHaveProperty('requestId');
        });
    });

    describe('Performance Tests', () => {
        it('should respond within acceptable time limits', async () => {
            const startTime = Date.now();
            
            await request(app)
                .get(`/api/templates/core/content/${testTemplate._id}`)
                .expect(200);
                
            const responseTime = Date.now() - startTime;
            expect(responseTime).toBeLessThan(1000); // Should respond within 1 second
        });

        it('should handle concurrent requests', async () => {
            const concurrentRequests = Array(10).fill().map(() =>
                request(app)
                    .get(`/api/templates/core/content/${testTemplate._id}`)
                    .expect(200)
            );

            const responses = await Promise.all(concurrentRequests);
            
            responses.forEach(response => {
                expect(response.body.success).toBe(true);
                expect(response.body.data.title).toBe('Test Birthday Card');
            });
        });
    });
}); 