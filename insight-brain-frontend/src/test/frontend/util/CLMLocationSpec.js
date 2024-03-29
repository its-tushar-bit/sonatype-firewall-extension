/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as urlUtil from 'MainRoot/util/urlUtil';
import * as clmLocation from 'MainRoot/util/CLMLocation';

describe('CLMLocation.js', function () {
  let CLMLocation, CLMLocationsService, $window;

  beforeEach(function () {
    CLMLocation = require('inject-loader!../../../main/frontend/util/CLMLocation')({
      './urlUtil': {
        ...urlUtil,
        getBaseUrl: () => 'http://localhost',
      },
    });

    angular.mock.module(CLMLocation.default.name);
  });

  beforeEach(
    angular.mock.module(function ($provide) {
      $provide.value('$window', {});
    })
  );

  beforeEach(inject(function (CLMLocations, _$window_) {
    CLMLocationsService = CLMLocations;
    $window = _$window_;
  }));

  describe('browseReportUrl', () => {
    beforeEach(inject(function (BaseUrl) {
      spyOn(BaseUrl, 'get').and.returnValue('http://localhost');
      urlUtil._setBaseUrlForTesting('http://localhost');
    }));

    afterEach(function () {
      urlUtil.setBaseUrl();
    });

    it('should return the correct URL for the report policy threats URL', () => {
      expect(CLMLocationsService.getReportPolicyThreatsUrl('foo', 'bar')).toBe(
        'http://localhost/rest/report/foo/bar/browseReport/policythreats.json'
      );
    });

    it('should return the correct URL for the report BOM URL', () => {
      expect(clmLocation.getReportBomUrl('foo', 'bar')).toBe(
        'http://localhost/rest/report/foo/bar/browseReport/bom.json'
      );
    });

    it('should return the correct URL for the report data URL', () => {
      expect(clmLocation.getReportDataUrl('foo', 'bar')).toBe(
        'http://localhost/rest/report/foo/bar/browseReport/data.json'
      );
    });

    it('should return the correct URL for the report security URL', () => {
      expect(clmLocation.getReportSecurityUrl('foo', 'bar')).toBe(
        'http://localhost/rest/report/foo/bar/browseReport/security.json'
      );
    });

    it('should return the correct URL for the report licenses URL', () => {
      expect(clmLocation.getReportLicenseUrl('foo', 'bar')).toBe(
        'http://localhost/rest/report/foo/bar/browseReport/licenses.json'
      );
    });

    it('should return the correct URL for the report unknownJS URL', () => {
      expect(clmLocation.getReportUnknownJsUrl('foo', 'bar')).toBe(
        'http://localhost/rest/report/foo/bar/browseReport/unknownjs.json'
      );
    });

    it('should return the correct URL for the report partial matched URL', () => {
      expect(clmLocation.getReportPartialMatchedUrl('foo', 'bar')).toBe(
        'http://localhost/rest/report/foo/bar/browseReport/partialmatched.json'
      );
    });

    it('should return the correct URL for the report metadata URL', () => {
      expect(clmLocation.getReportMetadataUrl('foo', 'bar')).toBe('http://localhost/rest/report/foo/bar/metadata');
    });

    it('should return the correct URL for the expanded coverage embeddable URL', () => {
      expect(clmLocation.getExpandedCoverageEmbeddableUrl('foo', 'bar')).toBe(
        'http://localhost/rest/report/foo/bar/browseReport/index.html'
      );
    });

    it('should return the correct URL for the report dependencies URL', () => {
      expect(clmLocation.getDependenciesUrl('foo', 'bar')).toBe(
        'http://localhost/rest/report/foo/bar/browseReport/dependencies.json'
      );
    });

    it('should return the correct URL for the report reevaluation', () => {
      expect(clmLocation.getReportReevaluateUrl('foo', 'bar')).toBe(
        'http://localhost/rest/report/foo/bar/reevaluatePolicy'
      );
    });

    it('should return the correct URL for the SBOM report', () => {
      expect(CLMLocationsService.getExportCycloneDxUrl('foo', 'bar')).toBe(
        'http://localhost/ui/links/cycloneDx/foo/reports/bar'
      );
    });

    it('should return the correct URL for the SPDX report', () => {
      expect(CLMLocationsService.getExportSpdxUrl('foo', 'bar')).toBe('http://localhost/ui/links/spdx/foo/reports/bar');
    });

    it('should return the correct URL to query the latest version of an InnerSource component', () => {
      const componentIdentifier = { coordinates: 'a-coordinate' };
      expect(CLMLocationsService.getInnerSourceComponentLatestVersionUrl(componentIdentifier)).toBe(
        'http://localhost/rest/innerSource/component/latestVersion?componentIdentifier=' +
          '%7B%22coordinates%22%3A%22a-coordinate%22%7D'
      );
    });
  });

  // map of user-telemetry method names and their respective unique postfixes
  var userTelemetryLocations = {
    getUserTelemetryConfig: 'config',
    getUserTelemetryJavascript: 'javascript',
    getUserTelemetryProxy: 'events',
  };

  for (var methodName in userTelemetryLocations) {
    var postfix = userTelemetryLocations[methodName];

    describe(methodName, function () {
      beforeEach(inject(function (BaseUrl) {
        spyOn(BaseUrl, 'get').and.returnValue('http://localhost');
      }));

      it('returns the expected path', function () {
        expect(CLMLocationsService[methodName]()).toBe('http://localhost/rest/user-telemetry/' + postfix);
      });

      it('returns the expected rm path when clmEndpoint.type is "rm"', function () {
        $window.clmEndpoint = { type: 'rm' };

        expect(CLMLocationsService[methodName]()).toBe('http://localhost/rest/rm/user-telemetry/' + postfix);
      });
    });
  }

  it('should return the all licenses url', function () {
    expect(CLMLocation.getAllLicensesUrl()).toBe('/rest/license');
  });

  describe('getVulnerabilityJsonDetailUrl', function () {
    let mockRefId, mockComponentIdentifier, mockExtraQueryParameters;

    beforeEach(function () {
      urlUtil._setBaseUrlForTesting('http://localhost');
      mockRefId = 'refId';
      mockComponentIdentifier = { coordinates: 'a-coordinate' };
      mockExtraQueryParameters = {
        identificationSource: 'CLAIR',
        scanId: 'bf5f6cf419',
        ownerId: 'appId',
        ownerType: 'APPLICATION',
      };
    });

    afterEach(function () {
      urlUtil.setBaseUrl();
    });

    it('returns URL to get the vulnerability details without query params', function () {
      const expectedUrl = 'http://localhost/api/v2/vulnerabilities/refId';
      const actualUrl = CLMLocation.getVulnerabilityJsonDetailUrl(mockRefId);

      expect(actualUrl).toEqual(expectedUrl);
    });

    it('returns URL to get the vulnerability details when componentIdentifier param is passed', function () {
      const expectedUrl =
        'http://localhost/api/v2/vulnerabilities/refId' +
        '?componentIdentifier=%7B%22coordinates%22%3A%22a-coordinate%22%7D';
      const actualUrl = CLMLocation.getVulnerabilityJsonDetailUrl(mockRefId, mockComponentIdentifier);

      expect(actualUrl).toEqual(expectedUrl);
    });

    it('returns URL to get the vulnerability details when componentIdentifier and extraQueryParameters are passed', function () {
      const expectedUrl =
        'http://localhost/api/v2/vulnerabilities/refId' +
        '?componentIdentifier=%7B%22coordinates%22%3A%22a-coordinate%22%7D&identificationSource=CLAIR' +
        '&scanId=bf5f6cf419&ownerId=appId&ownerType=APPLICATION';
      const actualUrl = CLMLocation.getVulnerabilityJsonDetailUrl(
        mockRefId,
        mockComponentIdentifier,
        mockExtraQueryParameters
      );
      expect(actualUrl).toEqual(expectedUrl);
    });

    it('returns URL to get the vulnerability details when only one third party query param is passed', function () {
      const expectedUrl = 'http://localhost/api/v2/vulnerabilities/refId?scanId=scanId';
      const actualUrl = CLMLocation.getVulnerabilityJsonDetailUrl(mockRefId, null, {
        scanId: 'scanId',
      });
      expect(actualUrl).toEqual(expectedUrl);
    });
  });

  describe('getClaimComponentUrl', function () {
    it('returns the base claim URL when called with no argument', function () {
      expect(CLMLocationsService.getClaimComponentUrl()).toBe('/rest/component/identified');
    });

    it('returns the claim URL of the hash specified via an argument', function () {
      expect(CLMLocationsService.getClaimComponentUrl('foo bar')).toBe('/rest/component/identified/foo%20bar');
    });
  });

  it('should return the url for fecthing users', function () {
    expect(CLMLocation.getFindUsersUrl('queryTerm')).toBe('/rest/user/global/global/query?q=queryTerm');
  });

  it('should return the url for saving a waiver request ', function () {
    expect(CLMLocation.saveRequestWaiverUrl('violationId')).toBe('/api/v2/policyWaivers/waiverRequests/violationId');
  });

  it('should return the url to get the role info', function () {
    expect(CLMLocation.getRoleMappingUrl('idForTheRole')).toBe(
      '/rest/membershipMapping/global/global/role/idForTheRole'
    );
  });

  it('should return the url to get the role mapping info for repositories', function () {
    expect(CLMLocation.getRoleMappingsForRepositories()).toBe('/rest/membershipMapping/repository_container');
  });

  it('should return the delete url for waivers', function () {
    expect(CLMLocation.deleteWaiverUrl('organization', 'orgId', 'waiverId')).toBe(
      '/api/v2/policyWaivers/organization/orgId/waiverId/'
    );
  });

  it('should return the scm repositories url', function () {
    expect(CLMLocation.getScmRepositoriesUrl('organizationId', 'http://localhost:1234')).toBe(
      '/rest/onboarding/loadRepositories?orgId=organizationId' + '&defaultHostUrl=http%3A%2F%2Flocalhost%3A1234'
    );
  });

  it('should return the license legal metadata url for the application', function () {
    expect(CLMLocation.getLicenseLegalApplicationReportUrl('appId')).toBe(
      '/api/v2/licenseLegalMetadata/application/appId'
    );
  });

  it('should return the license legal component url for the application', function () {
    expect(CLMLocation.getLicenseLegalComponentUrl('orgOrApp', 'ownerId', 'hash')).toBe(
      '/api/v2/licenseLegalMetadata/orgOrApp/ownerId/component?hash=hash'
    );
  });

  it('should return the license legal component url by component identifier for the application', function () {
    expect(CLMLocation.getLicenseLegalComponentByComponentIdentifierUrl('componentIdentifier')).toBe(
      '/api/v2/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component?componentIdentifier=componentIdentifier'
    );

    expect(
      CLMLocation.getLicenseLegalComponentByComponentIdentifierUrl('componentIdentifier', 'application', 'app')
    ).toBe('/api/v2/licenseLegalMetadata/application/app/component?componentIdentifier=componentIdentifier');
  });

  it('should return the legal dashboard applicationsUrl url', function () {
    expect(CLMLocation.getLegalDashboardApplicationsUrl()).toBe(
      '/api/experimental/licenseLegalMetadata/dashboard/applications'
    );
  });

  it('should return the legal dashboard componentsUrl url', function () {
    expect(CLMLocation.getLegalDashboardComponentsUrl()).toBe(
      '/api/experimental/licenseLegalMetadata/dashboard/components'
    );
  });

  it('should return the legal dashboard AttributionReportMultiApplication url', function () {
    expect(CLMLocation.getAttributionReportMultiApplicationUrl()).toBe(
      '/rest/legal/attribution/multiApplication/activeUserFilter/report'
    );
  });

  it('should return the legal dashboard get filters url', function () {
    expect(CLMLocation.getLegalDashboardFilters()).toBe('/rest/userFilter/active?type=ADVANCED_LEGAL_PACK_DASHBOARD');
  });

  it('should return the legal dashboard saved filters url', function () {
    expect(CLMLocation.getLegalDashboardSavedFilters()).toBe(
      '/rest/userFilter/named?type=ADVANCED_LEGAL_PACK_DASHBOARD'
    );
  });

  it('should return the legal dashboard delete filters url', function () {
    expect(CLMLocation.getLegalDashboardDeleteFilterUrl('theFilterName')).toBe(
      '/rest/userFilter/?name=theFilterName&type=ADVANCED_LEGAL_PACK_DASHBOARD'
    );
  });

  it('should return the all licenses url', function () {
    expect(CLMLocation.getAllLicensesUrl()).toBe('/rest/license');
  });

  describe('getLicenseGroupsUrl', () => {
    it('returns the license threat group url with application', () => {
      expect(CLMLocation.getLicenseGroupsUrl('application', 'applicationId')).toBe(
        '/rest/licenseThreatGroup/application/applicationId'
      );
    });

    it('returns the license threat group url with organization', () => {
      expect(CLMLocation.getLicenseGroupsUrl('organization', 'organizationId')).toBe(
        '/rest/licenseThreatGroup/organization/organizationId'
      );
    });
  });

  describe('getApplicableLicenseGroupsUrl', () => {
    it('returns the applicable license threat group url with application', () => {
      expect(CLMLocation.getApplicableLicenseGroupsUrl('application', 'applicationId')).toBe(
        '/rest/licenseThreatGroup/application/applicationId/applicable'
      );
    });

    it('returns the applicable license threat group url with organization', () => {
      expect(CLMLocation.getApplicableLicenseGroupsUrl('organization', 'organizationId')).toBe(
        '/rest/licenseThreatGroup/organization/organizationId/applicable'
      );
    });
  });

  describe('getDeleteLicenseGroupUrl', () => {
    it('returns the delete license threat group url with application', () => {
      expect(CLMLocation.getDeleteLicenseGroupUrl('application', 'applicationId', 'ltgId')).toBe(
        '/rest/licenseThreatGroup/application/applicationId/ltgId'
      );
    });

    it('returns the delete license threat group url with organization', () => {
      expect(CLMLocation.getDeleteLicenseGroupUrl('organization', 'organizationId', 'ltgId')).toBe(
        '/rest/licenseThreatGroup/organization/organizationId/ltgId'
      );
    });
  });

  describe('getLicenseGroupLicensesUrl', () => {
    it('returns the license threat group licenses url with application', () => {
      expect(CLMLocation.getLicenseGroupLicensesUrl('application', 'applicationId', 'ltgId')).toBe(
        '/rest/licenseThreatGroupLicense/application/applicationId/ltgId'
      );
    });

    it('returns the license threat group licenses url with organization', () => {
      expect(CLMLocation.getLicenseGroupLicensesUrl('organization', 'organizationId', 'ltgId')).toBe(
        '/rest/licenseThreatGroupLicense/organization/organizationId/ltgId'
      );
    });
  });

  it('should return the application save component copyright override url', function () {
    expect(CLMLocation.getSaveComponentCopyrightOverrideUrl('application', 'appId')).toBe(
      '/api/experimental/licenseLegalMetadata/application/appId/component/copyright'
    );
  });

  it('should return the application get component copyright override url', function () {
    const compIdentifier = {
      format: 'maven',
      coordinates: {
        artifactId: 'logback-access',
        classifier: '',
        extension: 'jar',
        groupId: 'ch.qos.logback',
        version: '0.6',
      },
    };
    expect(CLMLocation.getComponentCopyrightOverrideUrl('application', 'appId', compIdentifier)).toBe(
      '/api/experimental/licenseLegalMetadata/application/appId/component/copyright?componentIdentifier=' +
        '%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22logback-access%22%2C%22' +
        'classifier%22%3A%22%22%2C%22extension%22%3A%22jar%22%2C%22groupId%22%3A%22ch.qos.logback%22%2C%22' +
        'version%22%3A%220.6%22%7D%7D'
    );
  });

  it('should return the owner hierarchy url', function () {
    expect(CLMLocation.getOwnerHierarchyUrl('ownerType', 'ownerId')).toBe('/rest/owner/ownerType/ownerId/hierarchy');
  });

  it('should return the owner hierarchy url for legal reviewers', function () {
    expect(CLMLocation.getOwnerHierarchyLegalReviewerUrl('ownerType', 'ownerId')).toBe(
      '/rest/owner/ownerType/ownerId/hierarchy/legalReviewer'
    );
  });

  it('should return the saml sso login url', function () {
    expect(CLMLocation.getSamlSsoLoginUrl('http://localhost:8080/hola/mundo')).toBe(
      '/saml/login?hash=http%3A%2F%2Flocalhost%3A8080%2Fhola%2Fmundo'
    );
  });

  it('should return the save component obligation attribution url', function () {
    expect(CLMLocation.getSaveComponentObligationAttributionUrl('ownerType', 'ownerId')).toBe(
      '/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/obligation/attribution'
    );
  });

  it('should return the get component obligation attribution url with an obligation name', function () {
    const compIdentifier = {
      format: 'maven',
      coordinates: {
        artifactId: 'logback-access',
        classifier: '',
        extension: 'jar',
        groupId: 'ch.qos.logback',
        version: '0.6',
      },
    };
    expect(
      CLMLocation.getComponentObligationAttributionUrl('ownerType', 'ownerId', compIdentifier, 'obligationName')
    ).toBe(
      '/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/obligation/attribution?' +
        'componentIdentifier=%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22' +
        'logback-access%22%2C%22classifier%22%3A%22%22%2C%22extension%22%3A%22jar%22%2C%22groupId%22%3A%22' +
        'ch.qos.logback%22%2C%22version%22%3A%220.6%22%7D%7D&obligationName=obligationName'
    );
  });

  it('should return the get component obligation attribution url without an obligation name', function () {
    const compIdentifier = {
      format: 'maven',
      coordinates: {
        artifactId: 'logback-access',
        classifier: '',
        extension: 'jar',
        groupId: 'ch.qos.logback',
        version: '0.6',
      },
    };
    expect(CLMLocation.getComponentObligationAttributionUrl('ownerType', 'ownerId', compIdentifier, null)).toBe(
      '/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/obligation/attribution?' +
        'componentIdentifier=%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22' +
        'logback-access%22%2C%22classifier%22%3A%22%22%2C%22extension%22%3A%22jar%22%2C%22groupId%22%3A%22' +
        'ch.qos.logback%22%2C%22version%22%3A%220.6%22%7D%7D'
    );
  });

  it('should return the delete component obligation attribution url', function () {
    expect(CLMLocation.getDeleteComponentObligationAttributionUrl('attributionId')).toBe(
      '/api/experimental/licenseLegalMetadata/component/obligation/attribution/attributionId'
    );
  });

  it('should return the save original sources override url', function () {
    expect(CLMLocation.getSaveComponentOriginalSourcesOverrideUrl('orgOrApp', 'ownerId')).toBe(
      '/api/experimental/licenseLegalMetadata/orgOrApp/ownerId/component/sourceLink'
    );
  });

  it('should return the save component obligation url', function () {
    expect(CLMLocation.getSaveComponentObligationUrl('ownerType', 'ownerId')).toBe(
      '/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/obligation'
    );
  });

  it('should return the save component obligations url', function () {
    expect(CLMLocation.getSaveComponentObligationsUrl('ownerType', 'ownerId')).toBe(
      '/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/obligations'
    );
  });

  it('should return the set component proprietary matchers url', function () {
    expect(CLMLocation.setProprietaryMatchers('ownerId')).toBe('/rest/proprietary/application/ownerId/add');
  });

  it('should return the get component licenses url', function () {
    expect(
      CLMLocation.getComponentLicensesUrl({
        clientType: 'ci',
        componentIdentifier: JSON.stringify({ format: 'format', coordinates: 'coordinates' }),
        ownerType: 'application',
        ownerId: 'appPublicId',
        identificationSource: 'identificationSource',
        scanId: 'currentScanId',
      })
    ).toBe(
      '/rest/ci/componentDetails/application/appPublicId/licenses?componentIdentifier=%7B%22format%22%3A%22format%22%2C%22coordinates%22%3A%22coordinates%22%7D&identificationSource=identificationSource&scanId=currentScanId'
    );
  });

  it('should return the get component multi-licenses url', function () {
    expect(
      CLMLocation.getComponentMultiLicensesUrl({
        clientType: 'ci',
        componentIdentifier: JSON.stringify({ format: 'format', coordinates: 'coordinates' }),
        ownerType: 'application',
        ownerId: 'appPublicId',
        identificationSource: 'identificationSource',
        scanId: 'currentScanId',
      })
    ).toBe(
      '/rest/ci/componentDetails/application/appPublicId/multiLicenses?componentIdentifier=%7B%22format%22%3A%22format%22%2C%22coordinates%22%3A%22coordinates%22%7D&identificationSource=identificationSource&scanId=currentScanId'
    );
  });

  it('should return the get component multi-licenses url for legal reviewers', function () {
    expect(
      CLMLocation.getComponentMultiLicensesLegalReviewerUrl({
        clientType: 'ci',
        componentIdentifier: JSON.stringify({ format: 'format', coordinates: 'coordinates' }),
        ownerType: 'application',
        ownerId: 'appPublicId',
        identificationSource: 'identificationSource',
        scanId: 'currentScanId',
      })
    ).toBe(
      '/rest/ci/componentDetails/application/appPublicId/multiLicenses/legalReviewer?componentIdentifier=%7B%22format%22%3A%22format%22%2C%22coordinates%22%3A%22coordinates%22%7D&identificationSource=identificationSource&scanId=currentScanId'
    );
  });

  it('should return the get component obligation url', function () {
    const componentIdentifier = {
      format: 'maven',
      coordinates: {
        artifactId: 'logback-access',
        classifier: '',
        extension: 'jar',
        groupId: 'ch.qos.logback',
        version: '0.6',
      },
    };
    expect(CLMLocation.getComponentObligationUrl('ownerType', 'ownerId', componentIdentifier, 'obligationName')).toBe(
      '/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/obligation?' +
        'componentIdentifier=%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22' +
        'logback-access%22%2C%22classifier%22%3A%22%22%2C%22extension%22%3A%22jar%22%2C%22groupId%22%3A%22' +
        'ch.qos.logback%22%2C%22version%22%3A%220.6%22%7D%7D&obligationName=obligationName'
    );
  });

  it('should return the delete component obligation url', function () {
    expect(CLMLocation.getDeleteComponentObligationsUrl(['obligationIdOne', 'obligationIdTwo'])).toBe(
      '/api/experimental/licenseLegalMetadata/component/obligation' +
        '?componentObligationId=obligationIdOne&componentObligationId=obligationIdTwo'
    );
  });

  it('should return the save legal file url', function () {
    expect(CLMLocation.getSaveLegalFileUrl('ownerType', 'ownerId')).toBe(
      '/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/legalFile'
    );
  });

  it('should return the legal file url', function () {
    const componentIdentifier = {
      format: 'maven',
      coordinates: {
        artifactId: 'logback-access',
        classifier: '',
        extension: 'jar',
        groupId: 'ch.qos.logback',
        version: '0.6',
      },
    };
    expect(CLMLocation.getLegalFileUrl('ownerType', 'ownerId', componentIdentifier, 'type')).toBe(
      '/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/legalFile?' +
        'componentIdentifier=%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22' +
        'logback-access%22%2C%22classifier%22%3A%22%22%2C%22extension%22%3A%22jar%22%2C%22groupId%22%3A%22' +
        'ch.qos.logback%22%2C%22version%22%3A%220.6%22%7D%7D&legalFileType=type'
    );
  });

  it('should return the application applied tags url', function () {
    expect(CLMLocation.getApplicationCategoriesUrl('application-id')).toBe(
      '/rest/appliedTag/application/application-id'
    );
  });

  it('should return the applicable categories url', function () {
    expect(CLMLocation.getApplicableOrganizationCategories('application-id')).toBe(
      '/api/v2/applicationCategories/application/application-id/applicable'
    );
  });

  it('should return the application details url', function () {
    expect(CLMLocation.getApplicationUrl('application-id')).toBe('/rest/application/application-id');
  });

  it('should return the application details url for legal reviewers', function () {
    expect(CLMLocation.getApplicationLegalReviewerUrl('application-id')).toBe(
      '/rest/application/legalReviewer/application-id'
    );
  });

  it('should return the legal application details url', function () {
    expect(CLMLocation.getLegalDashboardApplicationUrl('application-id')).toBe(
      '/api/experimental/licenseLegalMetadata/dashboard/application/application-id'
    );
  });

  it('should return the firewall release quarantine url with params', function () {
    let urlStart = '/api/v2/firewall/components/autoReleasedFromQuarantine?',
      page = 1,
      pageSize = 12,
      sortBy = 'quarantineTime',
      sortAsc = false;

    // Test required params
    expect(CLMLocation.getFirewallReleaseQuarantineListUrl(page, pageSize)).toBe(
      urlStart + `page=${page}&pageSize=${pageSize}`
    );

    // Test optional params
    expect(CLMLocation.getFirewallReleaseQuarantineListUrl(page, pageSize, sortBy, sortAsc)).toBe(
      urlStart + `page=${page}&pageSize=${pageSize}&sortBy=${sortBy}&asc=${sortAsc}`
    );
  });

  it('should return the firewall tile metrics url', function () {
    expect(CLMLocation.getFirewallTileMetricsUrl()).toBe('/api/v2/firewall/metrics/embedded');
  });

  describe('ComponentCopyrightDetails', function () {
    it('getCopyrightFilePathsUrl should return the URL for copyright file paths', function () {
      expect(
        CLMLocation.getCopyrightFilePathsUrl('organization', 'org', 'hash', 'identifier', 'copyrightHash', 10, 15)
      ).toBe(
        '/api/experimental/licenseLegalMetadata/organization/org/' +
          'component/hash/copyright/copyrightHash/filePaths' +
          '?componentIdentifier=%22identifier%22&pageStart=10&pageLength=15'
      );
    });

    it('getCopyrightContextUrl should return the URL for copyright context', function () {
      expect(
        CLMLocation.getCopyrightContextUrl('organization', 'org', 'hash', 'identifier', 'copyrightHash', 'path/file')
      ).toBe(
        '/api/experimental/licenseLegalMetadata/organization/org/' +
          'component/hash/copyright/copyrightHash/context' +
          '?componentIdentifier=%22identifier%22&filePath=path%2Ffile'
      );
    });

    it('getCopyrightFileCountUrl should return the URL for copyright file count', function () {
      expect(CLMLocation.getCopyrightFileCountUrl('organization', 'org', 'hash', 'identifier')).toBe(
        '/api/experimental/licenseLegalMetadata/organization/org/' +
          'component/hash/copyright/fileCount?componentIdentifier=%22identifier%22'
      );
    });

    it('getTransitiveViolationsUrl should return the URL for transitive policy violations', function () {
      expect(CLMLocation.getTransitiveViolationsUrl('someOwnerType', 'someOwnerId', 'someScanId', 'someHash')).toBe(
        '/api/v2/policyViolations/transitive/someOwnerType/someOwnerId/someScanId?hash=someHash'
      );
    });

    it('getLatestReportUrl should return the URL for the latest report for the given app and stage', function () {
      expect(CLMLocation.getLatestReportUrl('someAppId', 'someStageTypeId')).toBe(
        '/ui/links/application/someAppId/latestReport/someStageTypeId'
      );
    });

    it('getWaiveTransitiveViolationsUrl should return the URL with params', function () {
      expect(CLMLocation.getWaiveTransitiveViolationsUrl('someAppId', 'someScanId', 'someHash')).toBe(
        '/api/v2/policyWaivers/transitive/application/someAppId/someScanId?hash=someHash'
      );
    });

    it('getLicenseOverrideUrl should return the URL with params', function () {
      expect(CLMLocation.getLicenseOverrideUrl('ownerType', 'ownerId', 'componentIdentifier')).toBe(
        '/rest/licenseOverride/ownerType/ownerId?componentIdentifier=componentIdentifier'
      );
    });

    it('getBaseLicenseOverrideUrl should return the URL', () => {
      expect(CLMLocation.getBaseLicenseOverrideUrl('ownerType', 'ownerId')).toBe(
        '/rest/licenseOverride/ownerType/ownerId'
      );
    });

    it('getDeleteLicenseOverrideUrl should return the URL', () => {
      expect(CLMLocation.getDeleteLicenseOverrideUrl('ownerType', 'ownerId', 'licenseOverrideId')).toBe(
        '/rest/licenseOverride/ownerType/ownerId/licenseOverrideId'
      );
    });

    it('getLicenseOverrideUrl should return the URL with no component identifier', function () {
      expect(CLMLocation.getLicenseOverrideUrl('ownerType', 'ownerId')).toBe('/rest/licenseOverride/ownerType/ownerId');
    });

    it('getLicensesWithSyntheticFilterUrl should return the URL with params', function () {
      expect(CLMLocation.getLicensesWithSyntheticFilterUrl()).toBe('/rest/license?filterSynthetic=true');
    });

    it('getAttributionReportUrl should return the URL with params', function () {
      expect(CLMLocation.getAttributionReportUrl('testApplication', 'stageTypeId')).toBe(
        '/api/v2/licenseLegalMetadata/application/testApplication/stage/stageTypeId/report'
      );
    });
  });

  describe('AttributionReports', function () {
    it('getAttributionReportTemplatesUrl should return an URL without params', function () {
      expect(CLMLocation.getAttributionReportTemplatesUrl()).toBe('/api/v2/licenseLegalMetadata/report-template');
    });

    it('getAttributionReportTemplateUrl should return the URL with params', function () {
      expect(CLMLocation.getAttributionReportTemplateUrl('123')).toBe(
        '/api/v2/licenseLegalMetadata/report-template/123'
      );
    });
  });

  describe('getLicenseOverrideUrl', function () {
    it('should return a URL without componentIdentifier if it is not provided', function () {
      const expectedUrl = '/rest/licenseOverride/app/appId';
      expect(clmLocation.getLicenseOverrideUrl('app', 'appId')).toEqual(expectedUrl);
    });

    it('should return a URL with encoded componentIdentifier if it is provided', function () {
      const stringComponentIdentifier = JSON.stringify({
        format: 'maven',
        coordinates: { version: 'version', group: 'group' },
      });
      const expectedUrl =
        '/rest/licenseOverride/app/appId?componentIdentifier=%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22version%22%3A%22version%22%2C%22group%22%3A%22group%22%7D%7D';
      expect(clmLocation.getLicenseOverrideUrl('app', 'appId', stringComponentIdentifier)).toEqual(expectedUrl);
    });
  });

  describe('getLicenseOverrideLegalReviewerUrl', function () {
    it('should return a URL without componentIdentifier if it is not provided', function () {
      const expectedUrl = '/rest/licenseOverride/app/appId/legalReviewer';
      expect(clmLocation.getLicenseOverrideLegalReviewerUrl('app', 'appId')).toEqual(expectedUrl);
    });

    it('should return a URL with encoded componentIdentifier if it is provided', function () {
      const stringComponentIdentifier = JSON.stringify({
        format: 'maven',
        coordinates: { version: 'version', group: 'group' },
      });
      const expectedUrl =
        '/rest/licenseOverride/app/appId/legalReviewer?componentIdentifier=%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22version%22%3A%22version%22%2C%22group%22%3A%22group%22%7D%7D';
      expect(clmLocation.getLicenseOverrideLegalReviewerUrl('app', 'appId', stringComponentIdentifier)).toEqual(
        expectedUrl
      );
    });
  });

  describe('getImportSbomUrl', function () {
    it('should return the url for importing SBOM and appending the application id at the end', function () {
      expect(CLMLocation.getImportSbomUrl('applicationId')).toBe('/rest/sbom/detect/applicationId');
    });
  });

  describe('getCommitImportedSbomUrl', function () {
    it('should return the url for committing SBOM import', function () {
      expect(CLMLocation.getCommitImportedSbomUrl('applicationId', 'requestId')).toBe(
        '/rest/sbom/commit/applicationId/requestId'
      );
    });
  });

  describe('getExportCycloneDx', function () {
    it('should return SBOM url', () => {
      expect(CLMLocation.getExportCycloneDxUrl('applicationId', 'scanId')).toBe(
        '/ui/links/cycloneDx/applicationId/reports/scanId'
      );
    });
  });

  describe('getExportSpdxUrl', function () {
    it('should return SPDX url', () => {
      expect(CLMLocation.getExportSpdxUrl('applicationId', 'scanId')).toBe(
        '/ui/links/spdx/applicationId/reports/scanId'
      );
    });
  });

  describe('getDownloadPdfUrl', function () {
    it('should return pdf url', () => {
      expect(CLMLocation.getDownloadPdfUrl('applicationPublicId', 'scanId')).toBe(
        '/rest/report/applicationPublicId/scanId/printReport'
      );
    });
  });

  describe('getProprietaryConfigUrl', () => {
    it('should return a URL with proper ownerType and ownerId', () => {
      expect(CLMLocation.getProprietaryConfigUrl('application', 'ownerId')).toBe(
        '/rest/proprietary/application/ownerId'
      );
    });
  });

  describe('getLabelsUrl', () => {
    it('should return a URL with proper ownerType and ownerId', () => {
      expect(CLMLocation.getLabelsUrl('application', 'application')).toBe('/api/v2/labels/application/application');
    });
  });

  describe('getDeleteLabelsUrl', () => {
    it('should return a URL with proper ownerType, ownerId and labelId', () => {
      expect(CLMLocation.getDeleteLabelsUrl('application', 'application', '1240987fd8sdf')).toBe(
        '/api/v2/labels/application/application/1240987fd8sdf'
      );
    });
  });

  describe('getConditionValueTypeUrl', () => {
    it('should return a URL with proper ownerType, ownerId', () => {
      expect(CLMLocation.getConditionValueTypeUrl('application', 'ownerId')).toBe(
        '/rest/conditionValueType/application/ownerId'
      );
    });
  });

  describe('getTestRepositoryConnectionUrl', function () {
    it('should return a URL without a repositoryConnectionId if it is not provided', function () {
      const expectedUrl = '/api/v2/config/repositoryConnection/some%3AOwnerType/some%3AOwnerId/test';
      expect(clmLocation.getTestRepositoryConnectionUrl('some:OwnerType', 'some:OwnerId')).toEqual(expectedUrl);
    });

    it('should return a URL with a repositoryConnectionId if it is provided', function () {
      const expectedUrl =
        '/api/v2/config/repositoryConnection/some%3AOwnerType/some%3AOwnerId/some%3ARepositoryConnectionId/test';
      expect(
        clmLocation.getTestRepositoryConnectionUrl('some:OwnerType', 'some:OwnerId', 'some:RepositoryConnectionId')
      ).toEqual(expectedUrl);
    });
  });

  describe('getRepositoryConnectionUrl', function () {
    it('should return a URL without a repositoryConnectionId if it is not provided', function () {
      const expectedUrl = '/api/v2/config/repositoryConnection/some%3AOwnerType/some%3AOwnerId';
      expect(clmLocation.getRepositoryConnectionUrl('some:OwnerType', 'some:OwnerId')).toEqual(expectedUrl);
    });

    it('should return a URL with a repositoryConnectionId if it is provided', function () {
      const expectedUrl =
        '/api/v2/config/repositoryConnection/some%3AOwnerType/some%3AOwnerId/some%3ARepositoryConnectionId';
      expect(
        clmLocation.getRepositoryConnectionUrl('some:OwnerType', 'some:OwnerId', 'some:RepositoryConnectionId')
      ).toEqual(expectedUrl);
    });

    it('should return a URL with inherit query param if it is provided', function () {
      const expectedUrl = '/api/v2/config/repositoryConnection/some%3AOwnerType/some%3AOwnerId?inherit=true';
      expect(clmLocation.getRepositoryConnectionUrl('some:OwnerType', 'some:OwnerId', null, true)).toEqual(expectedUrl);
    });
  });

  describe('getTestArtifactoryConnectionUrl', function () {
    it('should return a URL without an artifactoryConnectionId if it is not provided', function () {
      const expectedUrl = '/api/v2/config/artifactoryConnection/some%3AOwnerType/some%3AOwnerId/test';
      expect(clmLocation.getTestArtifactoryConnectionUrl('some:OwnerType', 'some:OwnerId')).toEqual(expectedUrl);
    });

    it('should return a URL with an artifactoryConnectionId if it is provided', function () {
      const expectedUrl =
        '/api/v2/config/artifactoryConnection/some%3AOwnerType/some%3AOwnerId/some%3AArtifactoryConnectionId/test';
      expect(
        clmLocation.getTestArtifactoryConnectionUrl('some:OwnerType', 'some:OwnerId', 'some:ArtifactoryConnectionId')
      ).toEqual(expectedUrl);
    });
  });

  describe('getArtifactoryConnectionUrl', function () {
    it('should return a URL without an artifactoryConnectionId if it is not provided', function () {
      const expectedUrl = '/api/v2/config/artifactoryConnection/some%3AOwnerType/some%3AOwnerId';
      expect(clmLocation.getArtifactoryConnectionUrl('some:OwnerType', 'some:OwnerId')).toEqual(expectedUrl);
    });

    it('should return a URL with an artifactoryConnectionId if it is provided', function () {
      const expectedUrl =
        '/api/v2/config/artifactoryConnection/some%3AOwnerType/some%3AOwnerId/some%3AArtifactoryConnectionId';
      expect(
        clmLocation.getArtifactoryConnectionUrl('some:OwnerType', 'some:OwnerId', 'some:ArtifactoryConnectionId')
      ).toEqual(expectedUrl);
    });

    it('should return a URL with inherit query param if it is provided', function () {
      const expectedUrl = '/api/v2/config/artifactoryConnection/some%3AOwnerType/some%3AOwnerId?inherit=true';
      expect(clmLocation.getArtifactoryConnectionUrl('some:OwnerType', 'some:OwnerId', null, true)).toEqual(
        expectedUrl
      );
    });
  });

  describe('getPolicyMonitoringUrl', () => {
    it('should return a URL with proper ownerType and ownerId', () => {
      expect(CLMLocation.getPolicyMonitoringUrl('application', 'application')).toBe(
        '/rest/policyMonitoring/application/application'
      );
    });
  });

  describe('getApplicablePolicyMonitoringUrl', () => {
    it('should return a URL with proper ownerType and ownerId', () => {
      expect(CLMLocation.getApplicablePolicyMonitoringUrl('application', 'application')).toBe(
        '/rest/policyMonitoring/application/application/applicable'
      );
    });
  });

  describe('getOwnerDetailsUrl', () => {
    it('should return a URL with proper ownerType and ownerId', () => {
      expect(CLMLocation.getOwnerDetailsUrl('application', 'application')).toBe(
        '/rest/sidebar/application/application/details'
      );
    });

    it('should return a URL with repository_container', () => {
      expect(CLMLocation.getOwnerDetailsUrl('application', 'application', true)).toBe(
        '/rest/sidebar/repository_container/details'
      );
    });
  });

  describe('getCategoriesUrl', () => {
    it('returns url for applicationCategories', () => {
      const expectedUrl = '/api/v2/applicationCategories/organization/ROOT_ORGANIZATION_ID';

      expect(clmLocation.getCategoriesUrl('organization', 'ROOT_ORGANIZATION_ID')).toEqual(expectedUrl);
    });
  });

  describe('getOrganizationAppliedTagUrl', () => {
    it('returns url for organization applied categories', () => {
      const expectedUrl = '/api/v2/applicationCategories/organization/ROOT_ORGANIZATION_ID/applied';

      expect(clmLocation.getOrganizationAppliedTagUrl('ROOT_ORGANIZATION_ID')).toEqual(expectedUrl);
    });
  });

  describe('getApplicableCategoriesUrl', () => {
    it('returns url for applicationCategories', () => {
      const expectedUrl = '/api/v2/applicationCategories/application/someApplication';

      expect(clmLocation.getApplicableCategoriesUrl('application', 'someApplication')).toEqual(expectedUrl);
    });

    it('returns url for applicable applicationCategories', () => {
      const expectedUrl = '/api/v2/applicationCategories/organization/someOrganization/applicable';

      expect(clmLocation.getApplicableCategoriesUrl('organization', 'someOrganization')).toEqual(expectedUrl);
    });
  });

  describe('getDeleteCategoriesUrl', () => {
    it('returns url for delete category', () => {
      const expectedUrl = '/api/v2/applicationCategories/organization/someOrganization/categoryId';

      expect(clmLocation.getDeleteCategoriesUrl('organization', 'someOrganization', 'categoryId')).toEqual(expectedUrl);
    });
  });

  describe('getOrganizationPolicyTagUrl', () => {
    it('returns url for delete category', () => {
      const expectedUrl = '/api/v2/applicationCategories/organization/someOrganization/policy';

      expect(clmLocation.getOrganizationPolicyTagUrl('someOrganization')).toEqual(expectedUrl);
    });
  });

  describe('getPolicyUrl', () => {
    it('returns base url for policy', () => {
      const expectedUrl = '/rest/policy/organization/someOrganization';

      expect(clmLocation.getPolicyUrl('organization', 'someOrganization')).toBe(expectedUrl);
    });
  });

  describe('getPoliciesWithProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCodeUrl', () => {
    it('returns url for get policies with conditions', () => {
      const expectedUrl =
        '/rest/policy/repository_container/REPOSITORY_CONTAINER_ID/withProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCode';

      expect(clmLocation.getPoliciesWithProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCodeUrl()).toBe(
        expectedUrl
      );
    });
  });

  describe('getPolicyCRUDUrl', () => {
    it('returns CRUD url for policy', () => {
      const expectedUrl = '/rest/policy/organization/someOrganization/somePolicyId';

      expect(clmLocation.getPolicyCRUDUrl('organization', 'someOrganization', 'somePolicyId')).toBe(expectedUrl);
    });
  });

  describe('getPolicyOverridesUrl', () => {
    it('returns policy overrides url ', () => {
      const expectedUrl = '/rest/policy/organization/someOrganization/somePolicyId/overrides';

      expect(clmLocation.getPolicyOverridesUrl('organization', 'someOrganization', 'somePolicyId')).toBe(expectedUrl);
    });
  });

  describe('getApplicablePolicies', () => {
    it('returns applicable policies url', () => {
      const expectedUrl = '/rest/policy/organization/someOrganization/applicable';

      expect(clmLocation.getApplicablePolicies('organization', 'someOrganization')).toBe(expectedUrl);
    });
  });

  describe('getPolicyTagUrl', () => {
    it('returns applicable policies url', () => {
      const expectedUrl = '/rest/appliedTag/policy/somePolicyId/organization/someOrganization';

      expect(clmLocation.getPolicyTagUrl('somePolicyId', 'organization', 'someOrganization')).toBe(expectedUrl);
    });
  });
  describe('getEnableUnauthenticatedPages', () => {
    it('returns url to get getEnableUnauthenticatedPages feature configuration', () => {
      expect(clmLocation.getEnableUnauthenticatedPages()).toEqual('/rest/product/features/enableUnauthenticatedPages');
    });
  });

  describe('getQuarantinedComponentViewAnonymousAccessEnabledState', () => {
    it('returns url for anonymous access configuration', () => {
      expect(clmLocation.getQuarantinedComponentViewAnonymousAccessEnabledState()).toEqual(
        '/api/v2/firewall/quarantinedComponentView/configuration/anonymousAccess/'
      );
    });
  });

  describe('getCrowdConfigurationUrl', () => {
    it('returns url for atlassian crowd configuration', () => {
      const expectedUrl = '/api/v2/config/crowd';

      expect(clmLocation.getCrowdConfigurationUrl()).toEqual(expectedUrl);
    });
  });

  describe('getCrowdConfigurationTestUrl', () => {
    it('returns url for atlassian crowd configuration', () => {
      const expectedUrl = '/api/v2/config/crowd/test';

      expect(clmLocation.getCrowdConfigurationTestUrl()).toEqual(expectedUrl);
    });
  });

  describe('getRepositoriesUrl', () => {
    it('returns repositories url', () => {
      const expectedUrl = '/rest/repositories';

      expect(clmLocation.getRepositoriesUrl()).toEqual(expectedUrl);
    });
  });

  describe('getRepositoryInfoUrl', () => {
    it('returns url for delete repository', () => {
      const expectedUrl = '/rest/repositories/someRepositoryId';

      expect(clmLocation.getRepositoryInfoUrl('someRepositoryId')).toEqual(expectedUrl);
    });
  });

  describe('getPolicyEvaluationTimestampUrl', () => {
    it('returns url to get component policy evaluation timestamps', () => {
      const expectedUrl =
        '/rest/repositories/repositoryId/policyEvaluationTimestamps?componentIdentifier=componentIdentifier';
      expect(clmLocation.getPolicyEvaluationTimestampUrl('repositoryId', 'componentIdentifier')).toEqual(expectedUrl);
    });
  });

  describe('getEndpointsUrl', () => {
    it('returns url for endpoints with the given api type parameter', () => {
      const expectedUrl = '/api/v2/endpoints/api-type';

      expect(clmLocation.getEndpointsUrl('api-type')).toEqual(expectedUrl);
    });
  });

  describe('getRepositoryComponentsUrl', () => {
    it('returns url to get repository components details', () => {
      const expectedUrl = '/api/experimental/repositories/repository/repositoryId/results/details';

      expect(clmLocation.getRepositoryComponentsUrl('repository', 'repositoryId')).toEqual(expectedUrl);
    });
  });

  describe('getIsJiraEnabledUrl', () => {
    it('returns url for whether jira is enabled', () => {
      const expectedUrl = '/rest/jira/enabled';

      expect(clmLocation.getIsJiraEnabledUrl()).toEqual(expectedUrl);
    });
  });

  describe('getJiraProjectsUrl', () => {
    it('returns url for jira projects', () => {
      const expectedUrl = '/rest/jira/project';

      expect(clmLocation.getJiraProjectsUrl()).toEqual(expectedUrl);
    });
  });

  describe('getComponentPolicyViolationsUrl', () => {
    it('returns url to get component policy violations', () => {
      const expectedUrl =
        '/rest/repositories/repositoryId/policyViolations/some/component-path/1.0.0/component-name-1.0.0.jar';
      expect(
        clmLocation.getComponentPolicyViolationsUrl(
          'some/component-path/1.0.0/component-name-1.0.0.jar',
          'repositoryId'
        )
      ).toEqual(expectedUrl);
    });
  });

  describe('getOwnerListUrl', () => {
    it('returns url to get ownerList', () => {
      const expectedUrl = '/rest/sidebar';
      expect(clmLocation.getOwnerListUrl()).toEqual(expectedUrl);
    });
  });

  describe('getPermissionContextTestUrl', () => {
    const ownerType = 'ownerType';

    it('returns url to get permissions for ownerType', () => {
      const expectedUrl = `/rest/user/permissions/${ownerType}`;

      expect(clmLocation.getPermissionContextTestUrl(ownerType)).toEqual(expectedUrl);
    });

    it('returns url to get permissions for ownerType and ownerId', () => {
      const ownerId = 'ownerId';
      const expectedUrl = `/rest/user/permissions/${ownerType}/${ownerId}`;

      expect(clmLocation.getPermissionContextTestUrl(ownerType, ownerId)).toEqual(expectedUrl);
    });
  });
  describe('getRepositoryPolicyViolationUrl', () => {
    it('should return a URL with proper repositoryId and repositoryPolicyId', () => {
      expect(CLMLocation.getRepositoryPolicyViolationUrl('repositoryId', 'repositoryPolicyId')).toBe(
        '/rest/repositories/repositoryId/policyViolation/repositoryPolicyId'
      );
    });
  });

  describe('getRepositoryPolicyViolationUrl', () => {
    it('should return a URL with proper repositoryId and repositoryPolicyId', () => {
      expect(CLMLocation.getRepositoryEvaluateUrl('repositoryId')).toBe('/rest/repositories/repositoryId/evaluate');
    });
  });

  describe('getComponentDisplayNameByIdentifierUrl', () => {
    it('should return a URL to get the Display Name based on a component identifier string', () => {
      const componentIdentifier = '{"coordinates": "name"}';
      expect(CLMLocation.getComponentDisplayNameByIdentifierUrl(componentIdentifier)).toBe(
        '/rest/componentDetails/nameByIdentifier?componentIdentifier=%7B%22coordinates%22%3A%20%22name%22%7D'
      );
    });
  });

  describe('getVulnerabilityCustomRemediationRefIdUrl', () => {
    it('should return a URL to customize the remediation of a vulnerability without component identifier', () => {
      expect(CLMLocation.getVulnerabilityCustomRemediationRefIdUrl('application', 'testId', 'CVE-123')).toBe(
        '/api/experimental/vulnerability/customData/application/testId/remediation/refId/CVE-123'
      );
    });

    it('should return a URL to customize the remediation of a vulnerability with component identifier', () => {
      const componentIdentifier = '{"coordinates": "name"}';
      expect(
        CLMLocation.getVulnerabilityCustomRemediationRefIdUrl('application', 'testId', 'CVE-123', componentIdentifier)
      ).toBe(
        '/api/experimental/vulnerability/customData/application/testId/remediation/refId/CVE-123' +
          '?componentIdentifier=' +
          componentIdentifier
      );
    });
  });

  describe('getVulnerabilityCustomRemediationUrl', () => {
    it('should return a URL to get the custom remediation of a vulnerability by scope', () => {
      expect(CLMLocation.getVulnerabilityCustomRemediationUrl('application', 'testId')).toBe(
        '/api/experimental/vulnerability/customData/application/testId/remediation'
      );
    });
  });

  describe('getVulnerabilityCustomRemediationIdUrl', () => {
    it('should return a URL to get the custom remediation of a vulnerability by ID', () => {
      expect(CLMLocation.getVulnerabilityCustomRemediationIdUrl('application', 'testId', 'some-id')).toBe(
        '/api/experimental/vulnerability/customData/application/testId/remediation/some-id'
      );
    });
  });

  describe('getVulnerabilityCustomCweRefIdUrl', () => {
    it('should return a URL to customize the cwe of a vulnerability without component identifier', () => {
      expect(CLMLocation.getVulnerabilityCustomCweRefIdUrl('application', 'testId', 'CVE-123')).toBe(
        '/api/experimental/vulnerability/customData/application/testId/cwe/refId/CVE-123'
      );
    });

    it('should return a URL to customize the cwe of a vulnerability with component identifier', () => {
      const componentIdentifier = '{"coordinates": "name"}';
      expect(
        CLMLocation.getVulnerabilityCustomCweRefIdUrl('application', 'testId', 'CVE-123', componentIdentifier)
      ).toBe(
        '/api/experimental/vulnerability/customData/application/testId/cwe/refId/CVE-123' +
          '?componentIdentifier=' +
          componentIdentifier
      );
    });
  });

  describe('getVulnerabilityCustomCweUrl', () => {
    it('should return a URL to get the custom cwe of a vulnerability by scope', () => {
      expect(CLMLocation.getVulnerabilityCustomCweUrl('application', 'testId')).toBe(
        '/api/experimental/vulnerability/customData/application/testId/cwe'
      );
    });
  });

  describe('getVulnerabilityCustomCweIdUrl', () => {
    it('should return a URL to get the custom cwe of a vulnerability by ID', () => {
      expect(CLMLocation.getVulnerabilityCustomCweIdUrl('application', 'testId', 'some-id')).toBe(
        '/api/experimental/vulnerability/customData/application/testId/cwe/some-id'
      );
    });
  });

  describe('getVulnerabilityCustomCvssVectorRefIdUrl', () => {
    it('should return a URL to customize the CVSS vector of a vulnerability without component identifier', () => {
      expect(CLMLocation.getVulnerabilityCustomCvssVectorRefIdUrl('application', 'testId', 'CVE-123')).toBe(
        '/api/experimental/vulnerability/customData/application/testId/cvss/vector/refId/CVE-123'
      );
    });

    it('should return a URL to customize the CVSS vector of a vulnerability with component identifier', () => {
      const componentIdentifier = '{"coordinates": "name"}';
      expect(
        CLMLocation.getVulnerabilityCustomCvssVectorRefIdUrl('application', 'testId', 'CVE-123', componentIdentifier)
      ).toBe(
        '/api/experimental/vulnerability/customData/application/testId/cvss/vector/refId/CVE-123' +
          '?componentIdentifier=' +
          componentIdentifier
      );
    });
  });

  describe('getVulnerabilityCustomCvssVectorUrl', () => {
    it('should return a URL to get the custom CVSS vector of a vulnerability by scope', () => {
      expect(CLMLocation.getVulnerabilityCustomCvssVectorUrl('application', 'testId')).toBe(
        '/api/experimental/vulnerability/customData/application/testId/cvss/vector'
      );
    });
  });

  describe('getVulnerabilityCustomCvssVectorIdUrl', () => {
    it('should return a URL to get the custom CVSS vector of a vulnerability by ID', () => {
      expect(CLMLocation.getVulnerabilityCustomCvssVectorIdUrl('application', 'testId', 'some-id')).toBe(
        '/api/experimental/vulnerability/customData/application/testId/cvss/vector/some-id'
      );
    });
  });

  describe('getVulnerabilityCustomCvssSeverityRefIdUrl', () => {
    it('should return a URL to customize the CVSS severity of a vulnerability without component identifier', () => {
      expect(CLMLocation.getVulnerabilityCustomCvssSeverityRefIdUrl('application', 'testId', 'CVE-123')).toBe(
        '/api/experimental/vulnerability/customData/application/testId/cvss/severity/refId/CVE-123'
      );
    });

    it('should return a URL to customize the CVSS severity of a vulnerability with component identifier', () => {
      const componentIdentifier = '{"coordinates": "name"}';
      expect(
        CLMLocation.getVulnerabilityCustomCvssSeverityRefIdUrl('application', 'testId', 'CVE-123', componentIdentifier)
      ).toBe(
        '/api/experimental/vulnerability/customData/application/testId/cvss/severity/refId/CVE-123' +
          '?componentIdentifier=' +
          componentIdentifier
      );
    });
  });

  describe('getVulnerabilityCustomCvssSeverityUrl', () => {
    it('should return a URL to get the custom CVSS severity of a vulnerability by scope', () => {
      expect(CLMLocation.getVulnerabilityCustomCvssSeverityUrl('application', 'testId')).toBe(
        '/api/experimental/vulnerability/customData/application/testId/cvss/severity'
      );
    });
  });

  describe('getVulnerabilityCustomCvssSeverityIdUrl', () => {
    it('should return a URL to get the custom CVSS severity of a vulnerability by ID', () => {
      expect(CLMLocation.getVulnerabilityCustomCvssSeverityIdUrl('application', 'testId', 'some-id')).toBe(
        '/api/experimental/vulnerability/customData/application/testId/cvss/severity/some-id'
      );
    });
  });

  describe('getFirewallQuarantineListUrl', () => {
    it('should return a URL to get the firewall quarantine list', () => {
      expect(CLMLocation.getFirewallQuarantineListUrl()).toBe('/api/v2/firewall/components/quarantined');
    });

    it('should return a URL to get the firewall quarantine list with parameters', () => {
      expect(CLMLocation.getFirewallQuarantineListUrl(1, 2, 'field', true, 'id', 'name')).toBe(
        '/api/v2/firewall/components/quarantined?page=1&pageSize=2&sortBy=field&asc=true&policyId=id&componentName=name'
      );
    });

    it('handles a single parameter correctly', () => {
      expect(CLMLocation.getFirewallQuarantineListUrl(2)).toBe('/api/v2/firewall/components/quarantined?page=2');
    });

    it('handles a different parameter order correctly', () => {
      expect(CLMLocation.getFirewallQuarantineListUrl(null, 3, 'field')).toBe(
        '/api/v2/firewall/components/quarantined?pageSize=3&sortBy=field'
      );
    });
  });

  describe('getSourceControlRateLimitsUrl', () => {
    it('returns url for source control rate limits with the given owner type and owner id parameters', () => {
      const expectedUrl = '/api/experimental/sourceControl/someType/someId/rateLimits';

      expect(clmLocation.getSourceControlRateLimitsUrl('someType', 'someId')).toEqual(expectedUrl);
    });
  });

  describe('getPolicyViolationUiLink', () => {
    it('returns url for the ui policy violation', () => {
      const expectedUrl = '/ui/links/policyViolation/%23%2Fviolation%2FsomeViolationId';

      expect(clmLocation.getPolicyViolationUiLink('#/violation/someViolationId')).toEqual(expectedUrl);
    });
  });

  describe('getAddWaiverUiLink', () => {
    it('returns url for the add waiver form with a comment as a query param', () => {
      const expectedUrl = '/ui/links/addWaiver/someViolationId?comments=new%20comment';

      expect(clmLocation.getAddWaiverUiLink('someViolationId', 'new comment')).toEqual(expectedUrl);
    });
  });

  describe('getRepositoryManagerUrl', () => {
    it('returns the url for the repository manager with the given id and name', () => {
      const expectedUrl = '/rest/repositories/repositoryManager/someRepositoryManagerId/someRepositoryManagerName';

      expect(clmLocation.getRepositoryManagerUrl('someRepositoryManagerId', 'someRepositoryManagerName')).toEqual(
        expectedUrl
      );
    });
  });

  describe('getWaiverRequestWebhooksCountUrl', () => {
    it('returns the url for the internal endpoint for getting the ids of waiver request webhooks', () => {
      const expectedUrl = '/rest/config/webhook/waiverRequestCount';

      expect(clmLocation.getWaiverRequestWebhooksCountUrl()).toEqual(expectedUrl);
    });
  });

  describe('getRepositoryContainerUrl', () => {
    it('returns the url for the repository container', () => {
      const expectedUrl = '/api/v2/firewall/repositoryContainer';

      expect(clmLocation.getRepositoryContainer()).toEqual(expectedUrl);
    });
  });

  describe('getRepositoryComponentNameUrl', () => {
    it('returns the url to get the proprietary component name patterns', () => {
      const expectedUrl = '/rest/repositories/someOwnerType/someOwnerId/proprietaryComponentNamePatterns';

      expect(clmLocation.getRepositoryComponentNameUrl('someOwnerType', 'someOwnerId')).toEqual(expectedUrl);
    });
  });

  describe('getRepositoryComponentNamePatternUpdateUrl', () => {
    it('returns the url to update the proprietary component name pattern', () => {
      const expectedUrl = '/rest/repositories/proprietaryComponentNamePatterns/update';

      expect(clmLocation.getRepositoryComponentNamePatternUpdateUrl()).toEqual(expectedUrl);
    });
  });

  describe('getAdvancedSearchUrl', () => {
    it('returns the url with the query, page, and allComponents false', () => {
      const expectedUrl = '/api/v2/search/advanced?query=some%26Query&page=0&allComponents=false';

      expect(clmLocation.getAdvancedSearchUrl('some&Query', 0, false, false)).toEqual(expectedUrl);
    });

    it('returns the url with the query, page, and allComponents true', () => {
      const expectedUrl = '/api/v2/search/advanced?query=some%26Query&page=0&allComponents=true';

      expect(clmLocation.getAdvancedSearchUrl('some&Query', 0, true, false)).toEqual(expectedUrl);
    });

    it('returns the url with the query, page, allComponents false, and mode sbomManager', () => {
      const expectedUrl = '/api/v2/search/advanced?query=some%26Query&page=0&allComponents=false&mode=sbomManager';

      expect(clmLocation.getAdvancedSearchUrl('some&Query', 0, false, true)).toEqual(expectedUrl);
    });

    it('returns the url with the query, page, allComponents true, and mode sbomManager', () => {
      const expectedUrl = '/api/v2/search/advanced?query=some%26Query&page=0&allComponents=true&mode=sbomManager';

      expect(clmLocation.getAdvancedSearchUrl('some&Query', 0, true, true)).toEqual(expectedUrl);
    });
  });

  describe('getSbomsByApplicationUrl', () => {
    it('should return the correct URL with the given parameters', () => {
      const applicationId = 'abc123';
      const sortDir = 'desc';
      const pageSize = 10;
      const page = 0;
      const expectedURL = `/api/v2/sbom/application/${applicationId}?sortByDate=${sortDir}&pageSize=${pageSize}&page=${page}`;

      expect(clmLocation.getSbomsByApplicationUrl(applicationId, pageSize, page, sortDir)).toBe(expectedURL);
    });
  });

  describe('getDownloadSbomFromSbomTableUrl', () => {
    it('should return the correct URL with the given parameters', () => {
      const applicationId = 'abc123';
      const applicationVersion = 'v1';
      const expectedURL = `/api/v2/sbom/${applicationId}/version/${applicationVersion}/?state=original`;

      expect(clmLocation.getDownloadSbomFromSbomTableUrl(applicationId, applicationVersion)).toBe(expectedURL);
    });
  });

  describe('getDeleteSbomByApplicationIdAndVersionUrl', () => {
    it('should return the correct URL with the given parameters', () => {
      const applicationId = 'abc123';
      const applicationVersion = 'v1';
      const expectedURL = `/api/v2/sbom/${applicationId}/version/${applicationVersion}`;

      expect(clmLocation.getDeleteSbomByApplicationIdAndVersionUrl(applicationId, applicationVersion)).toBe(
        expectedURL
      );
    });
  });
});
