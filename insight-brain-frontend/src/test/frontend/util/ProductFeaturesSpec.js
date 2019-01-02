import productFeaturesModule from '../../../main/frontend/util/ProductFeatures';

describe('ProductFeatures.js', function() {
  beforeEach(angular.mock.module(productFeaturesModule.name));

  var $httpBackend,
      CLMLocations,
      ProductFeatures;

  beforeEach(inject(function(_$httpBackend_, _CLMLocations_, _ProductFeatures_) {
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    ProductFeatures = _ProductFeatures_;
  }));

  it('Test that a feature from server is properly found in UI', function() {
    $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['some-feature']);
    ProductFeatures.load();
    $httpBackend.flush();
    expect(ProductFeatures.isAvailable('some-feature')).toEqual(true);
  });

  it('Test that a feature not from server is properly missing in UI', function() {
    $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['some-feature']);
    ProductFeatures.load();
    $httpBackend.flush();
    expect(ProductFeatures.isAvailable('some-other-feature')).toEqual(false);
  });

  describe('isEnforcementSupportedForStage()', function() {
    it('disables proxy stage when firewall is off and enforcement is off', function() {
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      ProductFeatures.load();
      $httpBackend.flush();

      expect(ProductFeatures.isEnforcementSupportedForStage('proxy')).toBe(false);
    });

    it('enables proxy stage when enforcement is on', function() {
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['enforcement']);
      ProductFeatures.load();
      $httpBackend.flush();

      expect(ProductFeatures.isEnforcementSupportedForStage('proxy')).toBe(true);
    });

    it('enables proxy stage when firewall is on and enforcement is off', function() {
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['firewall']);
      ProductFeatures.load();
      $httpBackend.flush();

      expect(ProductFeatures.isEnforcementSupportedForStage('proxy')).toBe(true);
    });

    it('disables any stage other than proxy when enforcement is off', function() {
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      ProductFeatures.load();
      $httpBackend.flush();

      expect(ProductFeatures.isEnforcementSupportedForStage('develop')).toBe(false);
      expect(ProductFeatures.isEnforcementSupportedForStage('build')).toBe(false);
      expect(ProductFeatures.isEnforcementSupportedForStage('stage-release')).toBe(false);
      expect(ProductFeatures.isEnforcementSupportedForStage('release')).toBe(false);
      expect(ProductFeatures.isEnforcementSupportedForStage('operate')).toBe(false);
    });

    it('enables any stage when enforcement is on', function() {
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['enforcement']);
      ProductFeatures.load();
      $httpBackend.flush();

      expect(ProductFeatures.isEnforcementSupportedForStage('develop')).toBe(true);
      expect(ProductFeatures.isEnforcementSupportedForStage('build')).toBe(true);
      expect(ProductFeatures.isEnforcementSupportedForStage('stage-release')).toBe(true);
      expect(ProductFeatures.isEnforcementSupportedForStage('release')).toBe(true);
      expect(ProductFeatures.isEnforcementSupportedForStage('operate')).toBe(true);
    });
  });
});
