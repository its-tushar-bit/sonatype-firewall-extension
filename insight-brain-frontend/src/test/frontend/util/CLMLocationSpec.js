/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as urlUtil from '../../../main/frontend/util/urlUtil';

describe('CLMLocation.js', function() {
  let CLMLocation,
      CLMLocationsService,
      $window;

  beforeEach(function() {
    CLMLocation =
        require('inject-loader!../../../main/frontend/util/CLMLocation')({
          './urlUtil': {
            ...urlUtil,
            getBaseUrl: () => 'http://localhost'
          }
        });

    angular.mock.module(CLMLocation.default.name);
  });

  beforeEach(angular.mock.module(function($provide) {
    $provide.value('$window', {});
  }));

  beforeEach(inject(function(CLMLocations, _$window_) {
    CLMLocationsService = CLMLocations;
    $window = _$window_;
  }));

  describe('browseReportUrl', () => {
    beforeEach(inject(function(BaseUrl) {
      spyOn(BaseUrl, 'get').and.returnValue('http://localhost');
    }));

    it('should return the correct URL for the report policy threats URL', () => {
      expect(CLMLocationsService.getReportPolicyThreatsUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/policythreats.json');
    });

    it('should return the correct URL for the report BOM URL', () => {
      expect(CLMLocationsService.getReportBomUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/bom.json');
    });

    it('should return the correct URL for the report data URL', () => {
      expect(CLMLocationsService.getReportDataUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/data.json');
    });

    it('should return the correct URL for the report security URL', () => {
      expect(CLMLocationsService.getReportSecurityUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/security.json');
    });

    it('should return the correct URL for the report licenses URL', () => {
      expect(CLMLocationsService.getReportLicenseUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/licenses.json');
    });

    it('should return the correct URL for the report unknownJS URL', () => {
      expect(CLMLocationsService.getReportUnknownJsUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/unknownjs.json');
    });

    it('should return the correct URL for the report partial matched URL', () => {
      expect(CLMLocationsService.getReportPartialMatchedUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/partialmatched.json');
    });
  });

  it('Test noFormData added to license upload', function() {
    var formData = $window.FormData || 'mock';
    $window.FormData = null;
    expect(CLMLocationsService.getLicenseUploadUrl()).toMatch(/.*noFormData=true/);
    $window.FormData = formData;
    expect(CLMLocationsService.getLicenseUploadUrl()).not.toMatch(/.*noFormData=true/);
  });

  // map of user-telemetry method names and their respective unique postfixes
  var userTelemetryLocations = {
    getUserTelemetryConfig: 'config',
    getUserTelemetryJavascript: 'javascript',
    getUserTelemetryProxy: 'events'
  };

  for (var methodName in userTelemetryLocations) {
    var postfix = userTelemetryLocations[methodName];

    describe(methodName, function() {
      beforeEach(inject(function(BaseUrl) {
        spyOn(BaseUrl, 'get').and.returnValue('http://localhost');
      }));

      it('returns the expected path', function() {
        expect(CLMLocationsService[methodName]()).toBe('http://localhost/rest/user-telemetry/' + postfix);
      });

      it('returns the expected rm path when clmEndpoint.type is "rm"', function() {
        $window.clmEndpoint = { type: 'rm' };

        expect(CLMLocationsService[methodName]()).toBe('http://localhost/rest/rm/user-telemetry/' + postfix);
      });
    });
  }

  describe('getVulnerabilityJsonDetailUrl', function() {
    let mockRefId, mockComponentIdentifier, mockThirdPartyScanParameters;

    beforeEach(function() {
      mockRefId = 'refId';
      mockComponentIdentifier = { coordinates: 'a-coordinate' };
      mockThirdPartyScanParameters = {
        'identificationSource': 'CLAIR',
        'scanId': 'bf5f6cf419',
        'ownerId': 'appId',
        'ownerType': 'APPLICATION'
      };
    });

    it('returns URL to get the vulnerability details without query params', function() {
      const expectedUrl = 'http://localhost/api/v2/vulnerabilities/refId';
      const actualUrl = CLMLocation.getVulnerabilityJsonDetailUrl(mockRefId);

      expect(actualUrl).toEqual(expectedUrl);
    });

    it('returns URL to get the vulnerability details when componentIdentifier param is passed', function() {
      const expectedUrl = 'http://localhost/api/v2/vulnerabilities/refId'
          + '?componentIdentifier=%7B%22coordinates%22%3A%22a-coordinate%22%7D';
      const actualUrl = CLMLocation.getVulnerabilityJsonDetailUrl(mockRefId, mockComponentIdentifier);

      expect(actualUrl).toEqual(expectedUrl);
    });

    it('returns URL to get the vulnerability details when componentIdentifier and thirdPartyScanParameters are passed',
        function() {
          const expectedUrl = 'http://localhost/api/v2/vulnerabilities/refId'
              + '?componentIdentifier=%7B%22coordinates%22%3A%22a-coordinate%22%7D&identificationSource=CLAIR'
              + '&scanId=bf5f6cf419&ownerId=appId&ownerType=APPLICATION';
          const actualUrl = CLMLocation.getVulnerabilityJsonDetailUrl(mockRefId, mockComponentIdentifier,
              mockThirdPartyScanParameters);
          expect(actualUrl).toEqual(expectedUrl);
        });

    it('returns URL to get the vulnerability details when only one third party query param is passed', function() {
      const expectedUrl = 'http://localhost/api/v2/vulnerabilities/refId?scanId=scanId';
      const actualUrl = CLMLocation.getVulnerabilityJsonDetailUrl(mockRefId, null, {
        scanId: 'scanId'
      });
      expect(actualUrl).toEqual(expectedUrl);
    });
  });

  describe('getClaimComponentUrl', function() {
    beforeEach(inject(function(BaseUrl) {
      spyOn(BaseUrl, 'get').and.returnValue('http://localhost');
    }));

    it('returns the base claim URL when called with no argument', function() {
      expect(CLMLocationsService.getClaimComponentUrl()).toBe('http://localhost/rest/component/identified');
    });

    it('returns the claim URL of the hash specified via an argument', function() {
      expect(CLMLocationsService.getClaimComponentUrl('foo bar'))
          .toBe('http://localhost/rest/component/identified/foo%20bar');
    });
  });
});
