/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as urlUtil from '../../../main/frontend/util/urlUtil';
import * as clmLocation from '../../../main/frontend/util/CLMLocation';

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
      urlUtil._setBaseUrlForTesting('http://localhost');
    }));

    afterEach(function() {
      urlUtil.setBaseUrl();
    });

    it('should return the correct URL for the report policy threats URL', () => {
      expect(CLMLocationsService.getReportPolicyThreatsUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/policythreats.json');
    });

    it('should return the correct URL for the report BOM URL', () => {
      expect(clmLocation.getReportBomUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/bom.json');
    });

    it('should return the correct URL for the report data URL', () => {
      expect(clmLocation.getReportDataUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/data.json');
    });

    it('should return the correct URL for the report security URL', () => {
      expect(clmLocation.getReportSecurityUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/security.json');
    });

    it('should return the correct URL for the report licenses URL', () => {
      expect(clmLocation.getReportLicenseUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/licenses.json');
    });

    it('should return the correct URL for the report unknownJS URL', () => {
      expect(clmLocation.getReportUnknownJsUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/unknownjs.json');
    });

    it('should return the correct URL for the report partial matched URL', () => {
      expect(clmLocation.getReportPartialMatchedUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/partialmatched.json');
    });

    it('should return the correct URL for the report metadata URL', () => {
      expect(clmLocation.getReportMetadataUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/metadata');
    });

    it('should return the correct URL for the expanded coverage embeddable URL', () => {
      expect(clmLocation.getExpandedCoverageEmbeddableUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/index.html');
    });

    it('should return the correct URL for the report dependencies URL', () => {
      expect(clmLocation.getDependenciesUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/browseReport/dependencies.json');
    });

    it('should return the correct URL for the report reevaluation', () => {
      expect(clmLocation.getReportReevaluateUrl('foo', 'bar')).toBe(
          'http://localhost/rest/report/foo/bar/reevaluatePolicy');
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
      urlUtil._setBaseUrlForTesting('http://localhost');
      mockRefId = 'refId';
      mockComponentIdentifier = { coordinates: 'a-coordinate' };
      mockThirdPartyScanParameters = {
        'identificationSource': 'CLAIR',
        'scanId': 'bf5f6cf419',
        'ownerId': 'appId',
        'ownerType': 'APPLICATION'
      };
    });

    afterEach(function() {
      urlUtil.setBaseUrl();
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

  it('should return the delete url for waivers', function() {
    expect(CLMLocation.deleteWaiverUrl('organization', 'orgId', 'waiverId'))
        .toBe('/api/v2/policyWaivers/organization/orgId/waiverId/');
  });

  it('should return the scm repositories url', function() {
    expect(CLMLocation.getScmRepositoriesUrl('organizationId', 'http://localhost:1234')).toBe(
        '/api/experimental/onboarding/loadRepositories?orgId=organizationId' +
        '&defaultHostUrl=http%3A%2F%2Flocalhost%3A1234');
  });

  it('should return the license legal metadata url for the application', function() {
    expect(CLMLocation.getLicenseLegalApplicationReportUrl('appId'))
        .toBe('/api/v2/licenseLegalMetadata/application/appId');
  });

  it('should return the license legal component url for the application', function() {
    expect(CLMLocation.getLicenseLegalComponentUrl('orgOrApp', 'ownerId', 'hash'))
        .toBe('/api/v2/licenseLegalMetadata/orgOrApp/ownerId/component?hash=hash');
  });

  it('should return the legal dashboard applicationsUrl url', function() {
    expect(CLMLocation.getLegalDashboardApplicationsUrl())
        .toBe('/api/experimental/licenseLegalMetadata/dashboard/applications');
  });

  it('should return the legal dashboard get filters url', function() {
    expect(CLMLocation.getLegalDashboardFilters()).toBe('/rest/userFilter/active?type=ADVANCED_LEGAL_PACK_DASHBOARD');
  });

  it('should return the legal dashboard saved filters url', function() {
    expect(CLMLocation.getLegalDashboardSavedFilters())
        .toBe('/rest/userFilter/named?type=ADVANCED_LEGAL_PACK_DASHBOARD');
  });

  it('should return the legal dashboard delete filters url', function() {
    expect(CLMLocation.getLegalDashboardDeleteFilterUrl('theFilterName'))
        .toBe('/rest/userFilter/?name=theFilterName&type=ADVANCED_LEGAL_PACK_DASHBOARD');
  });

  it('should return the application save component copyright override url', function() {
    expect(CLMLocation.getSaveComponentCopyrightOverrideUrl('application', 'appId'))
        .toBe('/api/experimental/licenseLegalMetadata/application/appId/component/copyright');
  });

  it('should return the application get component copyright override url', function() {
    const compIdentifier = {
      format: 'maven',
      coordinates: {
        artifactId: 'logback-access',
        classifier: '',
        extension: 'jar',
        groupId: 'ch.qos.logback',
        version: '0.6'
      }
    };
    expect(CLMLocation.getComponentCopyrightOverrideUrl('application', 'appId', compIdentifier))
        .toBe('/api/experimental/licenseLegalMetadata/application/appId/component/copyright?componentIdentifier=' +
            '%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22logback-access%22%2C%22' +
            'classifier%22%3A%22%22%2C%22extension%22%3A%22jar%22%2C%22groupId%22%3A%22ch.qos.logback%22%2C%22' +
            'version%22%3A%220.6%22%7D%7D');
  });

  it('should return the owner hierarchy url', function() {
    expect(CLMLocation.getOwnerHierarchyUrl('ownerType', 'ownerId'))
        .toBe('/rest/owner/ownerType/ownerId/hierarchy');
  });

  it('should return the save component obligation attribution url', function() {
    expect(CLMLocation.getSaveComponentObligationAttributionUrl('ownerType', 'ownerId'))
        .toBe('/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/obligation/attribution');
  });

  it('should return the get component obligation attribution url', function() {
    const compIdentifier = {
      format: 'maven',
      coordinates: {
        artifactId: 'logback-access',
        classifier: '',
        extension: 'jar',
        groupId: 'ch.qos.logback',
        version: '0.6'
      }
    };
    expect(CLMLocation.getComponentObligationAttributionUrl('ownerType', 'ownerId', compIdentifier, 'obligationName'))
        .toBe('/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/obligation/attribution?' +
            'componentIdentifier=%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22' +
            'logback-access%22%2C%22classifier%22%3A%22%22%2C%22extension%22%3A%22jar%22%2C%22groupId%22%3A%22' +
            'ch.qos.logback%22%2C%22version%22%3A%220.6%22%7D%7D&obligationName=obligationName');
  });

  it('should return the delete component obligation attribution url', function() {
    expect(CLMLocation.getDeleteComponentObligationAttributionUrl('attributionId'))
        .toBe('/api/experimental/licenseLegalMetadata/component/obligation/attribution/attributionId');
  });

  it('should return the save component obligation url', function() {
    expect(CLMLocation.getSaveComponentObligationUrl('ownerType', 'ownerId'))
        .toBe('/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/obligation');
  });

  it('should return the get component obligation url', function() {
    const componentIdentifier = {
      format: 'maven',
      coordinates: {
        artifactId: 'logback-access',
        classifier: '',
        extension: 'jar',
        groupId: 'ch.qos.logback',
        version: '0.6'
      }
    };
    expect(CLMLocation.getComponentObligationUrl('ownerType', 'ownerId', componentIdentifier, 'obligationName'))
        .toBe('/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/obligation?' +
            'componentIdentifier=%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22' +
            'logback-access%22%2C%22classifier%22%3A%22%22%2C%22extension%22%3A%22jar%22%2C%22groupId%22%3A%22' +
            'ch.qos.logback%22%2C%22version%22%3A%220.6%22%7D%7D&obligationName=obligationName');
  });

  it('should return the delete component obligation url', function() {
    expect(CLMLocation.getDeleteComponentObligationUrl('obligationId'))
        .toBe('/api/experimental/licenseLegalMetadata/component/obligation/obligationId');
  });

  it('should return the save legal file url', function() {
    expect(CLMLocation.getSaveLegalFileUrl('ownerType', 'ownerId'))
        .toBe('/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/legalFile');
  });

  it('should return the legal file url', function() {
    const componentIdentifier = {
      format: 'maven',
      coordinates: {
        artifactId: 'logback-access',
        classifier: '',
        extension: 'jar',
        groupId: 'ch.qos.logback',
        version: '0.6'
      }
    };
    expect(CLMLocation.getLegalFileUrl('ownerType', 'ownerId', componentIdentifier, 'type'))
        .toBe('/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/legalFile?' +
            'componentIdentifier=%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22' +
            'logback-access%22%2C%22classifier%22%3A%22%22%2C%22extension%22%3A%22jar%22%2C%22groupId%22%3A%22' +
            'ch.qos.logback%22%2C%22version%22%3A%220.6%22%7D%7D&legalFileType=type');
  });

  it('should return the application details url', function() {
    expect(CLMLocation.getApplicationUrl('application-id')).toBe('/rest/application/application-id');
  });

  it('should return the legal application details url', function() {
    expect(CLMLocation.getLegalDashboardApplicationUrl('application-id'))
        .toBe('/api/experimental/licenseLegalMetadata/dashboard/application/application-id');
  });
});
