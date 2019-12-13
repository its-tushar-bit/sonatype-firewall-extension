/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
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

  describe('isNotificationsSupportedForStage()', function() {
    it('disables proxy stage when firewall is off and notifications is off', function() {
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      ProductFeatures.load();
      $httpBackend.flush();

      expect(ProductFeatures.isNotificationsSupportedForStage('proxy')).toBe(false);
    });

    it('enables proxy stage when firewall is on and notifications is off', function() {
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['firewall']);
      ProductFeatures.load();
      $httpBackend.flush();

      expect(ProductFeatures.isNotificationsSupportedForStage('proxy')).toBe(true);
    });

    it('disables any stage other than proxy when notifications is off', function() {
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      ProductFeatures.load();
      $httpBackend.flush();

      expect(ProductFeatures.isNotificationsSupportedForStage('develop')).toBe(false);
      expect(ProductFeatures.isNotificationsSupportedForStage('build')).toBe(false);
      expect(ProductFeatures.isNotificationsSupportedForStage('stage-release')).toBe(false);
      expect(ProductFeatures.isNotificationsSupportedForStage('release')).toBe(false);
      expect(ProductFeatures.isNotificationsSupportedForStage('operate')).toBe(false);
    });

    it('enables any stage when notifications is on', function() {
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['notifications']);
      ProductFeatures.load();
      $httpBackend.flush();

      expect(ProductFeatures.isNotificationsSupportedForStage('develop')).toBe(true);
      expect(ProductFeatures.isNotificationsSupportedForStage('build')).toBe(true);
      expect(ProductFeatures.isNotificationsSupportedForStage('stage-release')).toBe(true);
      expect(ProductFeatures.isNotificationsSupportedForStage('release')).toBe(true);
      expect(ProductFeatures.isNotificationsSupportedForStage('operate')).toBe(true);
      expect(ProductFeatures.isNotificationsSupportedForStage('proxy')).toBe(true);
    });
  });

  describe('isNotificationsSupportedForAnyStage()', function() {
    it('returns false if notifications are off and firewall is not available', function() {
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      ProductFeatures.load();
      $httpBackend.flush();

      expect(ProductFeatures.isNotificationsSupportedForAnyStage()).toBe(false);
    });

    it('returns true if notifications are off and firewall is available', function() {
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['firewall']);
      ProductFeatures.load();
      $httpBackend.flush();

      expect(ProductFeatures.isNotificationsSupportedForAnyStage()).toBe(true);
    });

    it('returns true if notifications are on', function() {
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['notifications']);
      ProductFeatures.load();
      $httpBackend.flush();

      expect(ProductFeatures.isNotificationsSupportedForAnyStage()).toBe(true);
    });

    it('returns true if notifications are on and firewall is available', function() {
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['notifications', 'firewall']);
      ProductFeatures.load();
      $httpBackend.flush();

      expect(ProductFeatures.isNotificationsSupportedForAnyStage()).toBe(true);
    });
  });
});
