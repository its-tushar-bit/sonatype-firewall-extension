describe('ProductFeatures.js', function() {
  beforeEach(module('ProductFeaturesModule'));

  it('Test that a feature from server is properly found in UI', inject(function(ProductFeatures, CLMLocations, $httpBackend) {
    $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['some-feature']);
    ProductFeatures.load();
    $httpBackend.flush();
    expect(ProductFeatures.isAvailable('some-feature')).toEqual(true);
  }));
  
  it('Test that a feature not from server is properly missing in UI', inject(function(ProductFeatures, CLMLocations, $httpBackend) {
    $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['some-feature']);
    ProductFeatures.load();
    $httpBackend.flush();
    expect(ProductFeatures.isAvailable('some-other-feature')).toEqual(false);
  }));
});
