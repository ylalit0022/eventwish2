const request = require('supertest');
const mongoose = require('mongoose');
const app = require('../../server');
const Template = require('../../models/Template');

describe('Template Monetization API', () => {
    let testTemplate;
    let premiumTemplate;

    beforeAll(async () => {
        if (mongoose.connection.readyState === 0) {
            await mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish_test');
        }
    });

    beforeEach(async () => {
        await Template.deleteMany({});
        
        // Create a free template
        testTemplate = await Template.create({
            title: 'Free Birthday Card',
            category: 'birthday',
            htmlContent: '<div>Happy Birthday!</div>',
            templateType: 'html',
            status: true,
            isPremium: false,
            price: 0,
            tags: ['birthday', 'free']
        });

        // Create a premium template
        premiumTemplate = await Template.create({
            title: 'Premium Anniversary Card',
            category: 'anniversary',
            htmlContent: '<div class="premium">Happy Anniversary!</div>',
            templateType: 'html',
            status: true,
            isPremium: true,
            price: 99,
            tags: ['anniversary', 'premium']
        });
    });

    afterEach(async () => {
        await Template.deleteMany({});
    });

    afterAll(async () => {
        await mongoose.connection.close();
    });

    describe('GET /api/templates/monetization/pricing/:id', () => {
        it('should get pricing information for free template', async () => {
            const response = await request(app)
                .get(`/api/templates/monetization/pricing/${testTemplate._id}`)
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data).toHaveProperty('isPremium', false);
            expect(response.body.data).toHaveProperty('price', 0);
            expect(response.body.data).toHaveProperty('currency', 'USD');
            expect(response.body.data).toHaveProperty('isFree', true);
        });

        it('should get pricing information for premium template', async () => {
            const response = await request(app)
                .get(`/api/templates/monetization/pricing/${premiumTemplate._id}`)
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data).toHaveProperty('isPremium', true);
            expect(response.body.data).toHaveProperty('price', 99);
            expect(response.body.data).toHaveProperty('currency', 'USD');
            expect(response.body.data).toHaveProperty('isFree', false);
        });

        it('should return 404 for non-existent template', async () => {
            const nonExistentId = new mongoose.Types.ObjectId();
            const response = await request(app)
                .get(`/api/templates/monetization/pricing/${nonExistentId}`)
                .expect(404);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('NOT_FOUND_ERROR');
        });
    });

    describe('PUT /api/templates/monetization/pricing/:id/price', () => {
        it('should update template price successfully', async () => {
            const newPrice = 149;
            const response = await request(app)
                .put(`/api/templates/monetization/pricing/${testTemplate._id}/price`)
                .send({ price: newPrice })
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data.price).toBe(newPrice);

            // Verify in database
            const updatedTemplate = await Template.findById(testTemplate._id);
            expect(updatedTemplate.price).toBe(newPrice);
        });

        it('should return 400 for negative price', async () => {
            const response = await request(app)
                .put(`/api/templates/monetization/pricing/${testTemplate._id}/price`)
                .send({ price: -10 })
                .expect(400);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('VALIDATION_ERROR');
            expect(response.body.error.message).toContain('Price cannot be negative');
        });

        it('should return 400 for non-numeric price', async () => {
            const response = await request(app)
                .put(`/api/templates/monetization/pricing/${testTemplate._id}/price`)
                .send({ price: 'invalid' })
                .expect(400);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('VALIDATION_ERROR');
        });
    });

    describe('PUT /api/templates/monetization/pricing/:id/premium-status', () => {
        it('should update premium status to true', async () => {
            const response = await request(app)
                .put(`/api/templates/monetization/pricing/${testTemplate._id}/premium-status`)
                .send({ isPremium: true })
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data.isPremium).toBe(true);

            // Verify in database
            const updatedTemplate = await Template.findById(testTemplate._id);
            expect(updatedTemplate.isPremium).toBe(true);
        });

        it('should update premium status to false', async () => {
            const response = await request(app)
                .put(`/api/templates/monetization/pricing/${premiumTemplate._id}/premium-status`)
                .send({ isPremium: false })
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data.isPremium).toBe(false);
        });

        it('should return 400 for non-boolean premium status', async () => {
            const response = await request(app)
                .put(`/api/templates/monetization/pricing/${testTemplate._id}/premium-status`)
                .send({ isPremium: 'not-boolean' })
                .expect(400);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('VALIDATION_ERROR');
        });
    });

    describe('PUT /api/templates/monetization/pricing/:id/discount', () => {
        it('should apply discount successfully', async () => {
            const discountData = {
                discountPercentage: 20,
                discountStartDate: new Date(),
                discountEndDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000) // 7 days from now
            };

            const response = await request(app)
                .put(`/api/templates/monetization/pricing/${premiumTemplate._id}/discount`)
                .send(discountData)
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data.discountPercentage).toBe(20);
            expect(response.body.data.discountedPrice).toBe(79); // 20% off 99
        });

        it('should return 400 for invalid discount percentage', async () => {
            const response = await request(app)
                .put(`/api/templates/monetization/pricing/${premiumTemplate._id}/discount`)
                .send({ discountPercentage: 150 }) // > 100%
                .expect(400);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('VALIDATION_ERROR');
            expect(response.body.error.message).toContain('Discount percentage must be between 0 and 100');
        });

        it('should return 400 for past end date', async () => {
            const response = await request(app)
                .put(`/api/templates/monetization/pricing/${premiumTemplate._id}/discount`)
                .send({
                    discountPercentage: 20,
                    discountEndDate: new Date(Date.now() - 24 * 60 * 60 * 1000) // Yesterday
                })
                .expect(400);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('VALIDATION_ERROR');
            expect(response.body.error.message).toContain('End date must be in the future');
        });
    });

    describe('DELETE /api/templates/monetization/pricing/:id/discount', () => {
        beforeEach(async () => {
            // Add discount to premium template
            await Template.findByIdAndUpdate(premiumTemplate._id, {
                discountPercentage: 20,
                discountStartDate: new Date(),
                discountEndDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)
            });
        });

        it('should remove discount successfully', async () => {
            const response = await request(app)
                .delete(`/api/templates/monetization/pricing/${premiumTemplate._id}/discount`)
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.message).toContain('Discount removed successfully');

            // Verify discount is removed
            const updatedTemplate = await Template.findById(premiumTemplate._id);
            expect(updatedTemplate.discountPercentage).toBeUndefined();
            expect(updatedTemplate.discountStartDate).toBeUndefined();
            expect(updatedTemplate.discountEndDate).toBeUndefined();
        });
    });

    describe('GET /api/templates/monetization/revenue/summary', () => {
        beforeEach(async () => {
            // Create templates with different pricing
            await Template.create([
                {
                    title: 'Premium Card 1',
                    category: 'birthday',
                    htmlContent: '<div>Card 1</div>',
                    templateType: 'html',
                    isPremium: true,
                    price: 99,
                    downloadCount: 50,
                    status: true
                },
                {
                    title: 'Premium Card 2',
                    category: 'anniversary',
                    htmlContent: '<div>Card 2</div>',
                    templateType: 'html',
                    isPremium: true,
                    price: 149,
                    downloadCount: 30,
                    status: true
                },
                {
                    title: 'Free Card',
                    category: 'birthday',
                    htmlContent: '<div>Free Card</div>',
                    templateType: 'html',
                    isPremium: false,
                    price: 0,
                    downloadCount: 100,
                    status: true
                }
            ]);
        });

        it('should get revenue summary', async () => {
            const response = await request(app)
                .get('/api/templates/monetization/revenue/summary')
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data).toHaveProperty('totalRevenue');
            expect(response.body.data).toHaveProperty('premiumTemplates');
            expect(response.body.data).toHaveProperty('freeTemplates');
            expect(response.body.data).toHaveProperty('averagePrice');
            expect(response.body.data.premiumTemplates).toBeGreaterThan(0);
        });
    });

    describe('GET /api/templates/monetization/revenue/by-category', () => {
        beforeEach(async () => {
            await Template.create([
                {
                    title: 'Birthday Premium',
                    category: 'birthday',
                    htmlContent: '<div>Birthday</div>',
                    templateType: 'html',
                    isPremium: true,
                    price: 99,
                    downloadCount: 20,
                    status: true
                },
                {
                    title: 'Anniversary Premium',
                    category: 'anniversary',
                    htmlContent: '<div>Anniversary</div>',
                    templateType: 'html',
                    isPremium: true,
                    price: 149,
                    downloadCount: 15,
                    status: true
                }
            ]);
        });

        it('should get revenue breakdown by category', async () => {
            const response = await request(app)
                .get('/api/templates/monetization/revenue/by-category')
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data).toBeInstanceOf(Array);
            
            const birthdayCategory = response.body.data.find(cat => cat.category === 'birthday');
            expect(birthdayCategory).toBeDefined();
            expect(birthdayCategory).toHaveProperty('revenue');
            expect(birthdayCategory).toHaveProperty('templateCount');
        });
    });

    describe('GET /api/templates/monetization/top-earning', () => {
        beforeEach(async () => {
            await Template.create([
                {
                    title: 'High Earner',
                    category: 'birthday',
                    htmlContent: '<div>High</div>',
                    templateType: 'html',
                    isPremium: true,
                    price: 199,
                    downloadCount: 100,
                    status: true
                },
                {
                    title: 'Medium Earner',
                    category: 'anniversary',
                    htmlContent: '<div>Medium</div>',
                    templateType: 'html',
                    isPremium: true,
                    price: 99,
                    downloadCount: 50,
                    status: true
                }
            ]);
        });

        it('should get top earning templates', async () => {
            const response = await request(app)
                .get('/api/templates/monetization/top-earning')
                .query({ limit: 10 })
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data).toBeInstanceOf(Array);
            expect(response.body.data.length).toBeGreaterThan(0);
            
            // Should be sorted by revenue (price * downloads) descending
            if (response.body.data.length > 1) {
                const first = response.body.data[0];
                const second = response.body.data[1];
                expect(first.estimatedRevenue).toBeGreaterThanOrEqual(second.estimatedRevenue);
            }
        });

        it('should respect limit parameter', async () => {
            const response = await request(app)
                .get('/api/templates/monetization/top-earning')
                .query({ limit: 1 })
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data.length).toBeLessThanOrEqual(1);
        });
    });

    describe('PUT /api/templates/monetization/bulk-pricing', () => {
        let template1, template2;

        beforeEach(async () => {
            template1 = await Template.create({
                title: 'Bulk Template 1',
                category: 'birthday',
                htmlContent: '<div>Template 1</div>',
                templateType: 'html',
                isPremium: false,
                price: 0,
                status: true
            });

            template2 = await Template.create({
                title: 'Bulk Template 2',
                category: 'anniversary',
                htmlContent: '<div>Template 2</div>',
                templateType: 'html',
                isPremium: false,
                price: 0,
                status: true
            });
        });

        it('should update pricing for multiple templates', async () => {
            const bulkUpdate = {
                templateIds: [template1._id, template2._id],
                price: 99,
                isPremium: true
            };

            const response = await request(app)
                .put('/api/templates/monetization/bulk-pricing')
                .send(bulkUpdate)
                .expect(200);

            expect(response.body.success).toBe(true);
            expect(response.body.data.updatedCount).toBe(2);

            // Verify updates in database
            const updated1 = await Template.findById(template1._id);
            const updated2 = await Template.findById(template2._id);
            
            expect(updated1.price).toBe(99);
            expect(updated1.isPremium).toBe(true);
            expect(updated2.price).toBe(99);
            expect(updated2.isPremium).toBe(true);
        });

        it('should return 400 for empty template IDs array', async () => {
            const response = await request(app)
                .put('/api/templates/monetization/bulk-pricing')
                .send({
                    templateIds: [],
                    price: 99
                })
                .expect(400);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('VALIDATION_ERROR');
            expect(response.body.error.message).toContain('At least one template ID is required');
        });

        it('should return 400 for invalid template IDs', async () => {
            const response = await request(app)
                .put('/api/templates/monetization/bulk-pricing')
                .send({
                    templateIds: ['invalid-id'],
                    price: 99
                })
                .expect(400);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('VALIDATION_ERROR');
        });
    });

    describe('Analytics and Insights', () => {
        it('should calculate correct revenue metrics', async () => {
            // Create test data with known values
            await Template.create({
                title: 'Revenue Test',
                category: 'birthday',
                htmlContent: '<div>Test</div>',
                templateType: 'html',
                isPremium: true,
                price: 100,
                downloadCount: 10, // Should generate 1000 in revenue
                status: true
            });

            const response = await request(app)
                .get('/api/templates/monetization/revenue/summary')
                .expect(200);

            expect(response.body.data.totalRevenue).toBeGreaterThanOrEqual(1000);
        });

        it('should handle templates with zero downloads', async () => {
            await Template.create({
                title: 'Zero Downloads',
                category: 'birthday',
                htmlContent: '<div>Zero</div>',
                templateType: 'html',
                isPremium: true,
                price: 99,
                downloadCount: 0,
                status: true
            });

            const response = await request(app)
                .get('/api/templates/monetization/top-earning')
                .expect(200);

            expect(response.body.success).toBe(true);
            // Should still include templates with zero revenue
        });
    });

    describe('Error Handling', () => {
        it('should handle database errors gracefully', async () => {
            // Simulate database error by using invalid ObjectId
            const response = await request(app)
                .get('/api/templates/monetization/pricing/invalid-object-id')
                .expect(400);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('VALIDATION_ERROR');
        });

        it('should validate required fields in pricing updates', async () => {
            const response = await request(app)
                .put(`/api/templates/monetization/pricing/${testTemplate._id}/price`)
                .send({}) // Missing price field
                .expect(400);

            expect(response.body.success).toBe(false);
            expect(response.body.error.code).toBe('VALIDATION_ERROR');
        });
    });
}); 