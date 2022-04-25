/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as urlUtil from '../../../main/frontend/util/urlUtil';
import * as clmLocation from '../../../main/frontend/util/CLMLocation';

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
      expect(CLMLocationsService.getViewSbomUrl('foo', 'bar')).toBe(
        'http://localhost/ui/links/cycloneDx/foo/reports/bar'
      );
    });

    it('should return the correct URL to query the latest version of an InnerSource component', () => {
      const componentIdentifier = { coordinates: 'a-coordinate' };
      expect(CLMLocationsService.getInnerSourceComponentLatestVersionUrl(componentIdentifier)).toBe(
        'http://localhost/rest/innerSource/component/latestVersion?componentIdentifier=' +
          '%7B%22coordinates%22%3A%22a-coordinate%22%7D'
      );
    });

    it('should return the correct URL to query the InnerSource repository connections of an owner', () => {
      expect(CLMLocationsService.getRepositoryConnections('ownerType', 'ownerId', true)).toBe(
        'http://localhost/api/v2/config/repositoryConnection/ownerType/ownerId?inherit=true'
      );
      expect(CLMLocationsService.getRepositoryConnections('ownerType', 'ownerId', false)).toBe(
        'http://localhost/api/v2/config/repositoryConnection/ownerType/ownerId?inherit=false'
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

  describe('getVulnerabilityJsonDetailUrl', function () {
    let mockRefId, mockComponentIdentifier, mockThirdPartyScanParameters;

    beforeEach(function () {
      urlUtil._setBaseUrlForTesting('http://localhost');
      mockRefId = 'refId';
      mockComponentIdentifier = { coordinates: 'a-coordinate' };
      mockThirdPartyScanParameters = {
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

    it('returns URL to get the vulnerability details when componentIdentifier and thirdPartyScanParameters are passed', function () {
      const expectedUrl =
        'http://localhost/api/v2/vulnerabilities/refId' +
        '?componentIdentifier=%7B%22coordinates%22%3A%22a-coordinate%22%7D&identificationSource=CLAIR' +
        '&scanId=bf5f6cf419&ownerId=appId&ownerType=APPLICATION';
      const actualUrl = CLMLocation.getVulnerabilityJsonDetailUrl(
        mockRefId,
        mockComponentIdentifier,
        mockThirdPartyScanParameters
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

  it('should return the url to get the role info', function () {
    expect(CLMLocation.getRoleMappingUrl('idForTheRole')).toBe(
      '/rest/membershipMapping/global/global/role/idForTheRole'
    );
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
      '/api/v2/licenseLegalMetadata/multiApplication/activeUserFilter/report'
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

  describe('getViewSbomUrl', function () {
    it('should return SBOM url', () => {
      expect(CLMLocation.getViewSbomUrl('applicationId', 'scanId')).toBe(
        '/ui/links/cycloneDx/applicationId/reports/scanId'
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

  describe('getPolicyCRUDUrl', () => {
    it('returns CRUD url for policy', () => {
      const expectedUrl = '/rest/policy/organization/someOrganization/somePolicyId';

      expect(clmLocation.getPolicyCRUDUrl('organization', 'someOrganization', 'somePolicyId')).toBe(expectedUrl);
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
});
