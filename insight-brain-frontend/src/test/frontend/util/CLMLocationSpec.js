import clmLocation from '../../../main/frontend/util/CLMLocation';

describe('CLMLocation.js', function() {
  var CLMLocations,
      $window;

  beforeEach(angular.mock.module(clmLocation.name, function($provide) {
    $provide.value('$window', {
    });
  }));

  beforeEach(inject(function(_CLMLocations_, _$window_) {
    CLMLocations = _CLMLocations_;
    $window = _$window_;
  }));

  describe('browseReportUrl', () => {
    beforeEach(inject(function(BaseUrl) {
      spyOn(BaseUrl, 'get').and.returnValue('http://localhost');
    }));

    it('should return the correct URL for the report policy threats URL', () => {
      expect(CLMLocations.getReportPolicyThreatsUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/policythreats.json');
    });

    it('should return the correct URL for the report BOM URL', () => {
      expect(CLMLocations.getReportBomUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/bom.json');
    });

    it('should return the correct URL for the report data URL', () => {
      expect(CLMLocations.getReportDataUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/data.json');
    });

    it('should return the correct URL for the report security URL', () => {
      expect(CLMLocations.getReportSecurityUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/security.json');
    });

    it('should return the correct URL for the report licenses URL', () => {
      expect(CLMLocations.getReportLicenseUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/licenses.json');
    });

    it('should return the correct URL for the report unknownJS URL', () => {
      expect(CLMLocations.getReportUnknownJsUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/unknownjs.json');
    });

    it('should return the correct URL for the report partial matched URL', () => {
      expect(CLMLocations.getReportPartialMatchedUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/partialmatched.json');
    });
  });

  it('Test noFormData added to license upload', function() {
    var formData = $window.FormData || 'mock';
    $window.FormData = null;
    expect(CLMLocations.getLicenseUploadUrl()).toMatch(/.*noFormData=true/);
    $window.FormData = formData;
    expect(CLMLocations.getLicenseUploadUrl()).not.toMatch(/.*noFormData=true/);
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
        expect(CLMLocations[methodName]()).toBe('http://localhost/rest/user-telemetry/' + postfix);
      });

      it('returns the expected rm path when clmEndpoint.type is "rm"', function() {
        $window.clmEndpoint = { type: 'rm' };

        expect(CLMLocations[methodName]()).toBe('http://localhost/rest/rm/user-telemetry/' + postfix);
      });
    });
  }

  describe('getVulnerabilityDetailUrl', function() {
    let mockHash, mockRefId, mockComponentIdentifier, mockSource;

    beforeEach(inject(function(BaseUrl) {
      spyOn(BaseUrl, 'get').and.returnValue('http://localhost');
      mockHash = 'hash';
      mockRefId = 'refId';
      mockComponentIdentifier = { coordinates: 'a-coordinate' };
      mockSource = 'sonatype';
    }));

    it('returns URL to get the vulnerability details', function() {
      const expectedUrl = 'http://localhost/rest/vulnerability/details/sonatype/refId';
      const actualUrl = CLMLocations.getVulnerabilityDetailUrl(mockSource, mockRefId);

      expect(actualUrl).toEqual(expectedUrl);
    });

    it('returns URL to get the vulnerability details with the supplied params', function() {
      const expectedUrl = 'http://localhost/rest/vulnerability/details/sonatype/refId'
          + '?hash=hash&componentIdentifier=%7B%22coordinates%22%3A%22a-coordinate%22%7D';
      const actualUrl = CLMLocations
          .getVulnerabilityDetailUrl(mockSource, mockRefId, mockComponentIdentifier, mockHash);

      expect(actualUrl).toEqual(expectedUrl);
    });
  });

  describe('getClaimComponentUrl', function() {
    beforeEach(inject(function(BaseUrl) {
      spyOn(BaseUrl, 'get').and.returnValue('http://localhost');
    }));

    it('returns the base claim URL when called with no argument', function() {
      expect(CLMLocations.getClaimComponentUrl()).toBe('http://localhost/rest/component/identified');
    });

    it('returns the claim URL of the hash specified via an argument', function() {
      expect(CLMLocations.getClaimComponentUrl('foo bar')).toBe('http://localhost/rest/component/identified/foo%20bar');
    });
  });
});
