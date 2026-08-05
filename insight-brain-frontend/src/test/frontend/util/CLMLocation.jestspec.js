/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as urlUtil from 'MainRoot/util/urlUtil';
import * as clmLocation from 'MainRoot/util/CLMLocation';

describe('clmLocation.js', function () {
  beforeEach(function () {
    urlUtil._setBaseUrlForTesting('http://localhost');
  });

  afterEach(function () {
    urlUtil.setBaseUrl();
  });

  describe('browseReportUrl', () => {
    it('should return the correct URL for the report policy threats URL', () => {
      expect(clmLocation.getReportPolicyThreatsUrl('foo', 'bar')).toBe(
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

    it('should return the correct URL for the report reevaluation status (keyed by statusId, no scanId)', () => {
      expect(clmLocation.getReportReevaluateStatusUrl('foo', 'baz')).toBe(
        'http://localhost/rest/report/foo/reevaluatePolicy/status/baz'
      );
    });

    it('should return the correct URL for the SBOM report', () => {
      expect(clmLocation.getExportCycloneDxUrl('foo', 'bar')).toBe(
        'http://localhost/ui/links/cycloneDx/foo/reports/bar'
      );
    });

    it('should return the correct URL for the SPDX report', () => {
      expect(clmLocation.getExportSpdxUrl('foo', 'bar')).toBe('http://localhost/ui/links/spdx/foo/reports/bar');
    });

    it('should return the correct URL to query the latest version of an InnerSource component', () => {
      const componentIdentifier = { coordinates: 'a-coordinate' };
      expect(clmLocation.getInnerSourceComponentLatestVersionUrl(componentIdentifier)).toBe(
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
      it('returns the expected path', function () {
        urlUtil._setBaseUrlForTesting('http://localhost');
        expect(clmLocation[methodName]()).toBe('http://localhost/rest/user-telemetry/' + postfix);
        urlUtil.setBaseUrl();
      });

      it('returns the expected rm path the base URL indicates that we are in RM', function () {
        urlUtil._setBaseUrlForTesting('http://localhost/rest/healthcheck/clm');

        expect(clmLocation[methodName]()).toBe(
          'http://localhost/rest/healthcheck/clm/rest/rm/user-telemetry/' + postfix
        );

        urlUtil.setBaseUrl();
      });
    });
  }

  it('should return the all licenses url', function () {
    expect(clmLocation.getAllLicensesUrl()).toBe('http://localhost/rest/license');
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
      const actualUrl = clmLocation.getVulnerabilityJsonDetailUrl(mockRefId);

      expect(actualUrl).toEqual(expectedUrl);
    });

    it('returns URL to get the vulnerability details when componentIdentifier param is passed', function () {
      const expectedUrl =
        'http://localhost/api/v2/vulnerabilities/refId' +
        '?componentIdentifier=%7B%22coordinates%22%3A%22a-coordinate%22%7D';
      const actualUrl = clmLocation.getVulnerabilityJsonDetailUrl(mockRefId, mockComponentIdentifier);

      expect(actualUrl).toEqual(expectedUrl);
    });

    it('returns URL to get the vulnerability details when componentIdentifier and extraQueryParameters are passed', function () {
      const expectedUrl =
        'http://localhost/api/v2/vulnerabilities/refId' +
        '?componentIdentifier=%7B%22coordinates%22%3A%22a-coordinate%22%7D&identificationSource=CLAIR' +
        '&scanId=bf5f6cf419&ownerId=appId&ownerType=APPLICATION';
      const actualUrl = clmLocation.getVulnerabilityJsonDetailUrl(
        mockRefId,
        mockComponentIdentifier,
        mockExtraQueryParameters
      );
      expect(actualUrl).toEqual(expectedUrl);
    });

    it('returns URL to get the vulnerability details when only one third party query param is passed', function () {
      const expectedUrl = 'http://localhost/api/v2/vulnerabilities/refId?scanId=scanId';
      const actualUrl = clmLocation.getVulnerabilityJsonDetailUrl(mockRefId, null, {
        scanId: 'scanId',
      });
      expect(actualUrl).toEqual(expectedUrl);
    });
  });

  describe('getClaimComponentUrl', function () {
    it('returns the base claim URL when called with no argument', function () {
      expect(clmLocation.getClaimComponentUrl()).toBe('http://localhost/rest/component/identified');
    });

    it('returns the claim URL of the hash specified via an argument', function () {
      expect(clmLocation.getClaimComponentUrl('foo bar')).toBe('http://localhost/rest/component/identified/foo%20bar');
    });
  });

  it('should return the url for fecthing users', function () {
    expect(clmLocation.getFindUsersUrl('queryTerm')).toBe('http://localhost/rest/user/global/global/query?q=queryTerm');
  });

  it('should return the url for creating a waiver request ', function () {
    expect(clmLocation.getCreatePolicyWaiverRequestUrl('application', 'applicationPublicId', 'violationId')).toBe(
      'http://localhost/api/v2/policyWaiverRequests/application/applicationPublicId/policyViolation/violationId'
    );
  });

  it('should return the url for retrieving the waiver request details', function () {
    expect(
      clmLocation.getViewOrUpdatePolicyWaiverRequestUrl('organization', 'organizationId', 'policyWaiverRequestId')
    ).toBe('http://localhost/api/v2/policyWaiverRequests/organization/organizationId/policyWaiverRequestId');
  });

  it('should return the url for reviewing the waiver request', function () {
    expect(clmLocation.getReviewPolicyWaiverRequestUrl('organization', 'organizationId', 'policyWaiverRequestId')).toBe(
      'http://localhost/api/v2/policyWaiverRequests/organization/organizationId/review/policyWaiverRequestId'
    );
  });

  it('should return the url to get the role info', function () {
    expect(clmLocation.getRoleMappingUrl('idForTheRole')).toBe(
      'http://localhost/api/v2/roleMemberships/global/role/idForTheRole/members'
    );
  });

  it('should return the url to get the role mapping info for repositories', function () {
    expect(clmLocation.getRoleMappingsForRepositories()).toBe(
      'http://localhost/api/v2/roleMemberships/repository_container/roles'
    );
  });

  it('should return the url to get the role mapping for current owner - application', function () {
    expect(clmLocation.getRoleMappingForCurrentOwnerUrl('application', 'appId123')).toBe(
      'http://localhost/api/v2/roleMemberships/application/appId123/roles'
    );
  });

  it('should return the url to get the role mapping for current owner - organization', function () {
    expect(clmLocation.getRoleMappingForCurrentOwnerUrl('organization', 'orgId456')).toBe(
      'http://localhost/api/v2/roleMemberships/organization/orgId456/roles'
    );
  });

  it('should return the url to get the role mapping for current owner - repository_container', function () {
    expect(clmLocation.getRoleMappingForCurrentOwnerUrl('repository_container', null)).toBe(
      'http://localhost/api/v2/roleMemberships/repository_container/roles'
    );
  });

  it('should return the url to get the role mapping for current owner - global', function () {
    expect(clmLocation.getRoleMappingForCurrentOwnerUrl('global', null)).toBe(
      'http://localhost/api/v2/roleMemberships/global/roles'
    );
  });

  it('should return the delete url for waivers', function () {
    expect(clmLocation.deleteWaiverUrl('organization', 'orgId', 'waiverId')).toBe(
      'http://localhost/api/v2/policyWaivers/organization/orgId/waiverId/'
    );
  });

  it('should return the scm repositories url', function () {
    expect(clmLocation.getScmRepositoriesUrl('organizationId', 'http://localhost:1234')).toBe(
      'http://localhost/rest/onboarding/loadRepositories?orgId=organizationId' +
        '&defaultHostUrl=http%3A%2F%2Flocalhost%3A1234'
    );
  });

  it('should return the license legal metadata url for the application', function () {
    expect(clmLocation.getLicenseLegalApplicationReportUrl('appId')).toBe(
      'http://localhost/api/v2/licenseLegalMetadata/application/appId'
    );
  });

  it('should return the license legal component url for the application', function () {
    expect(clmLocation.getLicenseLegalComponentUrl('orgOrApp', 'ownerId', 'hash')).toBe(
      'http://localhost/api/v2/licenseLegalMetadata/orgOrApp/ownerId/component?hash=hash'
    );
  });

  it('should return the license legal component url by component identifier for the application', function () {
    expect(clmLocation.getLicenseLegalComponentByComponentIdentifierUrl('componentIdentifier')).toBe(
      'http://localhost/api/v2/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component?componentIdentifier=componentIdentifier'
    );

    expect(
      clmLocation.getLicenseLegalComponentByComponentIdentifierUrl('componentIdentifier', 'application', 'app')
    ).toBe(
      'http://localhost/api/v2/licenseLegalMetadata/application/app/component?componentIdentifier=componentIdentifier'
    );
  });

  it('should return the legal dashboard applicationsUrl url', function () {
    expect(clmLocation.getLegalDashboardApplicationsUrl()).toBe(
      'http://localhost/api/experimental/licenseLegalMetadata/dashboard/applications'
    );
  });

  it('should return the legal dashboard componentsUrl url', function () {
    expect(clmLocation.getLegalDashboardComponentsUrl()).toBe(
      'http://localhost/api/experimental/licenseLegalMetadata/dashboard/components'
    );
  });

  it('should return the legal dashboard AttributionReportMultiApplication url', function () {
    expect(clmLocation.getAttributionReportMultiApplicationUrl()).toBe(
      'http://localhost/rest/legal/attribution/multiApplication/activeUserFilter/report'
    );
  });

  it('should return the legal dashboard get filters url', function () {
    expect(clmLocation.getLegalDashboardFilters()).toBe(
      'http://localhost/rest/userFilter/active?type=ADVANCED_LEGAL_PACK_DASHBOARD'
    );
  });

  it('should return the legal dashboard saved filters url', function () {
    expect(clmLocation.getLegalDashboardSavedFilters()).toBe(
      'http://localhost/rest/userFilter/named?type=ADVANCED_LEGAL_PACK_DASHBOARD'
    );
  });

  it('should return the legal dashboard delete filters url', function () {
    expect(clmLocation.getLegalDashboardDeleteFilterUrl('theFilterName')).toBe(
      'http://localhost/rest/userFilter/?name=theFilterName&type=ADVANCED_LEGAL_PACK_DASHBOARD'
    );
  });

  it('should return the all licenses url', function () {
    expect(clmLocation.getAllLicensesUrl()).toBe('http://localhost/rest/license');
  });

  describe('getLicenseGroupsUrl', () => {
    it('returns the license threat group url with application', () => {
      expect(clmLocation.getLicenseGroupsUrl('application', 'applicationId')).toBe(
        'http://localhost/rest/licenseThreatGroup/application/applicationId'
      );
    });

    it('returns the license threat group url with organization', () => {
      expect(clmLocation.getLicenseGroupsUrl('organization', 'organizationId')).toBe(
        'http://localhost/rest/licenseThreatGroup/organization/organizationId'
      );
    });
  });

  describe('getApplicableLicenseGroupsUrl', () => {
    it('returns the applicable license threat group url with application', () => {
      expect(clmLocation.getApplicableLicenseGroupsUrl('application', 'applicationId')).toBe(
        'http://localhost/rest/licenseThreatGroup/application/applicationId/applicable'
      );
    });

    it('returns the applicable license threat group url with organization', () => {
      expect(clmLocation.getApplicableLicenseGroupsUrl('organization', 'organizationId')).toBe(
        'http://localhost/rest/licenseThreatGroup/organization/organizationId/applicable'
      );
    });
  });

  describe('getDeleteLicenseGroupUrl', () => {
    it('returns the delete license threat group url with application', () => {
      expect(clmLocation.getDeleteLicenseGroupUrl('application', 'applicationId', 'ltgId')).toBe(
        'http://localhost/rest/licenseThreatGroup/application/applicationId/ltgId'
      );
    });

    it('returns the delete license threat group url with organization', () => {
      expect(clmLocation.getDeleteLicenseGroupUrl('organization', 'organizationId', 'ltgId')).toBe(
        'http://localhost/rest/licenseThreatGroup/organization/organizationId/ltgId'
      );
    });
  });

  describe('getLicenseGroupLicensesUrl', () => {
    it('returns the license threat group licenses url with application', () => {
      expect(clmLocation.getLicenseGroupLicensesUrl('application', 'applicationId', 'ltgId')).toBe(
        'http://localhost/rest/licenseThreatGroupLicense/application/applicationId/ltgId'
      );
    });

    it('returns the license threat group licenses url with organization', () => {
      expect(clmLocation.getLicenseGroupLicensesUrl('organization', 'organizationId', 'ltgId')).toBe(
        'http://localhost/rest/licenseThreatGroupLicense/organization/organizationId/ltgId'
      );
    });
  });

  it('should return the application save component copyright override url', function () {
    expect(clmLocation.getSaveComponentCopyrightOverrideUrl('application', 'appId')).toBe(
      'http://localhost/api/experimental/licenseLegalMetadata/application/appId/component/copyright'
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
    expect(clmLocation.getComponentCopyrightOverrideUrl('application', 'appId', compIdentifier)).toBe(
      'http://localhost/api/experimental/licenseLegalMetadata/application/appId/component/copyright?componentIdentifier=' +
        '%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22logback-access%22%2C%22' +
        'classifier%22%3A%22%22%2C%22extension%22%3A%22jar%22%2C%22groupId%22%3A%22ch.qos.logback%22%2C%22' +
        'version%22%3A%220.6%22%7D%7D'
    );
  });

  it('should return the owner hierarchy url', function () {
    expect(clmLocation.getOwnerHierarchyUrl('ownerType', 'ownerId')).toBe(
      'http://localhost/rest/owner/ownerType/ownerId/hierarchy'
    );
  });

  it('should return the owner hierarchy url for legal reviewers', function () {
    expect(clmLocation.getOwnerHierarchyLegalReviewerUrl('ownerType', 'ownerId')).toBe(
      'http://localhost/rest/owner/ownerType/ownerId/hierarchy/legalReviewer'
    );
  });

  it('should return the save component obligation attribution url', function () {
    expect(clmLocation.getSaveComponentObligationAttributionUrl('ownerType', 'ownerId')).toBe(
      'http://localhost/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/obligation/attribution'
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
      clmLocation.getComponentObligationAttributionUrl('ownerType', 'ownerId', compIdentifier, 'obligationName')
    ).toBe(
      'http://localhost/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/obligation/attribution?' +
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
    expect(clmLocation.getComponentObligationAttributionUrl('ownerType', 'ownerId', compIdentifier, null)).toBe(
      'http://localhost/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/obligation/attribution?' +
        'componentIdentifier=%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22' +
        'logback-access%22%2C%22classifier%22%3A%22%22%2C%22extension%22%3A%22jar%22%2C%22groupId%22%3A%22' +
        'ch.qos.logback%22%2C%22version%22%3A%220.6%22%7D%7D'
    );
  });

  it('should return the delete component obligation attribution url', function () {
    expect(clmLocation.getDeleteComponentObligationAttributionUrl('attributionId')).toBe(
      'http://localhost/api/experimental/licenseLegalMetadata/component/obligation/attribution/attributionId'
    );
  });

  it('should return the save original sources override url', function () {
    expect(clmLocation.getSaveComponentOriginalSourcesOverrideUrl('orgOrApp', 'ownerId')).toBe(
      'http://localhost/api/experimental/licenseLegalMetadata/orgOrApp/ownerId/component/sourceLink'
    );
  });

  it('should return the save component obligation url', function () {
    expect(clmLocation.getSaveComponentObligationUrl('ownerType', 'ownerId')).toBe(
      'http://localhost/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/obligation'
    );
  });

  it('should return the save component obligations url', function () {
    expect(clmLocation.getSaveComponentObligationsUrl('ownerType', 'ownerId')).toBe(
      'http://localhost/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/obligations'
    );
  });

  it('should return the set component proprietary matchers url', function () {
    expect(clmLocation.setProprietaryMatchers('ownerId')).toBe(
      'http://localhost/rest/proprietary/application/ownerId/add'
    );
  });

  it('should return the get component licenses url', function () {
    expect(
      clmLocation.getComponentLicensesUrl({
        clientType: 'ci',
        componentIdentifier: JSON.stringify({ format: 'format', coordinates: 'coordinates' }),
        ownerType: 'application',
        ownerId: 'appPublicId',
        identificationSource: 'identificationSource',
        scanId: 'currentScanId',
      })
    ).toBe(
      'http://localhost/rest/ci/componentDetails/application/appPublicId/licenses?componentIdentifier=%7B%22format%22%3A%22format%22%2C%22coordinates%22%3A%22coordinates%22%7D&identificationSource=identificationSource&scanId=currentScanId'
    );
  });

  it('should return the get component multi-licenses url', function () {
    expect(
      clmLocation.getComponentMultiLicensesUrl({
        clientType: 'ci',
        componentIdentifier: JSON.stringify({ format: 'format', coordinates: 'coordinates' }),
        ownerType: 'application',
        ownerId: 'appPublicId',
        identificationSource: 'identificationSource',
        scanId: 'currentScanId',
      })
    ).toBe(
      'http://localhost/rest/ci/componentDetails/application/appPublicId/multiLicenses?componentIdentifier=%7B%22format%22%3A%22format%22%2C%22coordinates%22%3A%22coordinates%22%7D&identificationSource=identificationSource&scanId=currentScanId'
    );
  });

  it('should return the get component multi-licenses url for legal reviewers', function () {
    expect(
      clmLocation.getComponentMultiLicensesLegalReviewerUrl({
        clientType: 'ci',
        componentIdentifier: JSON.stringify({ format: 'format', coordinates: 'coordinates' }),
        ownerType: 'application',
        ownerId: 'appPublicId',
        identificationSource: 'identificationSource',
        scanId: 'currentScanId',
      })
    ).toBe(
      'http://localhost/rest/ci/componentDetails/application/appPublicId/multiLicenses/legalReviewer?componentIdentifier=%7B%22format%22%3A%22format%22%2C%22coordinates%22%3A%22coordinates%22%7D&identificationSource=identificationSource&scanId=currentScanId'
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
    expect(clmLocation.getComponentObligationUrl('ownerType', 'ownerId', componentIdentifier, 'obligationName')).toBe(
      'http://localhost/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/obligation?' +
        'componentIdentifier=%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22' +
        'logback-access%22%2C%22classifier%22%3A%22%22%2C%22extension%22%3A%22jar%22%2C%22groupId%22%3A%22' +
        'ch.qos.logback%22%2C%22version%22%3A%220.6%22%7D%7D&obligationName=obligationName'
    );
  });

  it('should return the delete component obligation url', function () {
    expect(clmLocation.getDeleteComponentObligationsUrl(['obligationIdOne', 'obligationIdTwo'])).toBe(
      'http://localhost/api/experimental/licenseLegalMetadata/component/obligation' +
        '?componentObligationId=obligationIdOne&componentObligationId=obligationIdTwo'
    );
  });

  it('should return the save legal file url', function () {
    expect(clmLocation.getSaveLegalFileUrl('ownerType', 'ownerId')).toBe(
      'http://localhost/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/legalFile'
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
    expect(clmLocation.getLegalFileUrl('ownerType', 'ownerId', componentIdentifier, 'type')).toBe(
      'http://localhost/api/experimental/licenseLegalMetadata/ownerType/ownerId/component/legalFile?' +
        'componentIdentifier=%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22artifactId%22%3A%22' +
        'logback-access%22%2C%22classifier%22%3A%22%22%2C%22extension%22%3A%22jar%22%2C%22groupId%22%3A%22' +
        'ch.qos.logback%22%2C%22version%22%3A%220.6%22%7D%7D&legalFileType=type'
    );
  });

  it('should return the application applied tags url', function () {
    expect(clmLocation.getApplicationCategoriesUrl('application-id')).toBe(
      'http://localhost/rest/appliedTag/application/application-id'
    );
  });

  it('should return the applicable categories url', function () {
    expect(clmLocation.getApplicableOrganizationCategories('application-id')).toBe(
      'http://localhost/api/v2/applicationCategories/application/application-id/applicable'
    );
  });

  it('should return the application details url', function () {
    expect(clmLocation.getApplicationUrl('application-id')).toBe('http://localhost/rest/application/application-id');
  });

  it('should return the application details url for legal reviewers', function () {
    expect(clmLocation.getApplicationLegalReviewerUrl('application-id')).toBe(
      'http://localhost/rest/application/legalReviewer/application-id'
    );
  });

  it('should return the legal application details url', function () {
    expect(clmLocation.getLegalDashboardApplicationUrl('application-id')).toBe(
      'http://localhost/api/experimental/licenseLegalMetadata/dashboard/application/application-id'
    );
  });

  it('should return the firewall release quarantine url with params', function () {
    let urlStart = 'http://localhost/api/v2/firewall/components/autoReleasedFromQuarantine?',
      page = 1,
      pageSize = 12,
      sortBy = 'quarantineTime',
      sortAsc = false;

    // Test required params
    expect(clmLocation.getFirewallReleaseQuarantineListUrl(page, pageSize)).toBe(
      urlStart + `page=${page}&pageSize=${pageSize}`
    );

    // Test optional params
    expect(clmLocation.getFirewallReleaseQuarantineListUrl(page, pageSize, sortBy, sortAsc)).toBe(
      urlStart + `page=${page}&pageSize=${pageSize}&sortBy=${sortBy}&asc=${sortAsc}`
    );
  });

  it('should return the firewall tile metrics url', function () {
    expect(clmLocation.getFirewallTileMetricsUrl()).toBe('http://localhost/api/v2/firewall/metrics/embedded');
  });

  describe('ComponentCopyrightDetails', function () {
    it('getCopyrightFilePathsUrl should return the URL for copyright file paths', function () {
      expect(
        clmLocation.getCopyrightFilePathsUrl('organization', 'org', 'hash', 'identifier', 'copyrightHash', 10, 15)
      ).toBe(
        'http://localhost/api/experimental/licenseLegalMetadata/organization/org/' +
          'component/hash/copyright/copyrightHash/filePaths' +
          '?componentIdentifier=%22identifier%22&pageStart=10&pageLength=15'
      );
    });

    it('getCopyrightContextUrl should return the URL for copyright context', function () {
      expect(
        clmLocation.getCopyrightContextUrl('organization', 'org', 'hash', 'identifier', 'copyrightHash', 'path/file')
      ).toBe(
        'http://localhost/api/experimental/licenseLegalMetadata/organization/org/' +
          'component/hash/copyright/copyrightHash/context' +
          '?componentIdentifier=%22identifier%22&filePath=path%2Ffile'
      );
    });

    it('getCopyrightFileCountUrl should return the URL for copyright file count', function () {
      expect(clmLocation.getCopyrightFileCountUrl('organization', 'org', 'hash', 'identifier')).toBe(
        'http://localhost/api/experimental/licenseLegalMetadata/organization/org/' +
          'component/hash/copyright/fileCount?componentIdentifier=%22identifier%22'
      );
    });

    it('getTransitiveViolationsUrl should return the URL for transitive policy violations', function () {
      expect(clmLocation.getTransitiveViolationsUrl('someOwnerType', 'someOwnerId', 'someScanId', 'someHash')).toBe(
        'http://localhost/api/v2/policyViolations/transitive/someOwnerType/someOwnerId/someScanId?hash=someHash'
      );
    });

    it('getLatestReportUrl should return the URL for the latest report for the given app and stage', function () {
      expect(clmLocation.getLatestReportUrl('someAppId', 'someStageTypeId')).toBe(
        'http://localhost/ui/links/application/someAppId/latestReport/someStageTypeId'
      );
    });

    it('getWaiveTransitiveViolationsUrl should return the URL with params', function () {
      expect(clmLocation.getWaiveTransitiveViolationsUrl('someAppId', 'someScanId', 'someHash')).toBe(
        'http://localhost/api/v2/policyWaivers/transitive/application/someAppId/someScanId?hash=someHash'
      );
    });

    it('getLicenseOverrideUrl should return the URL with params', function () {
      expect(clmLocation.getLicenseOverrideUrl('ownerType', 'ownerId', 'componentIdentifier')).toBe(
        'http://localhost/api/v2/licenseOverrides/ownerType/ownerId?componentIdentifier=componentIdentifier'
      );
    });

    it('getBaseLicenseOverrideUrl should return the URL', () => {
      expect(clmLocation.getBaseLicenseOverrideUrl('ownerType', 'ownerId')).toBe(
        'http://localhost/api/v2/licenseOverrides/ownerType/ownerId'
      );
    });

    it('getDeleteLicenseOverrideUrl should return the URL', () => {
      expect(clmLocation.getDeleteLicenseOverrideUrl('ownerType', 'ownerId', 'licenseOverrideId')).toBe(
        'http://localhost/api/v2/licenseOverrides/ownerType/ownerId/licenseOverrideId'
      );
    });

    it('getLicenseOverrideUrl should return the URL with no component identifier', function () {
      expect(clmLocation.getLicenseOverrideUrl('ownerType', 'ownerId')).toBe(
        'http://localhost/api/v2/licenseOverrides/ownerType/ownerId'
      );
    });

    it('getLicensesWithSyntheticFilterUrl should return the URL with params', function () {
      expect(clmLocation.getLicensesWithSyntheticFilterUrl()).toBe(
        'http://localhost/rest/license?filterSynthetic=true'
      );
    });

    it('getAttributionReportUrl should return the URL with params', function () {
      expect(clmLocation.getAttributionReportUrl('testApplication', 'stageTypeId')).toBe(
        'http://localhost/api/v2/licenseLegalMetadata/application/testApplication/stage/stageTypeId/report'
      );
    });
  });

  describe('AttributionReports', function () {
    it('getAttributionReportTemplatesUrl should return an URL without params', function () {
      expect(clmLocation.getAttributionReportTemplatesUrl()).toBe(
        'http://localhost/api/v2/licenseLegalMetadata/report-template'
      );
    });

    it('getAttributionReportTemplateUrl should return the URL with params', function () {
      expect(clmLocation.getAttributionReportTemplateUrl('123')).toBe(
        'http://localhost/api/v2/licenseLegalMetadata/report-template/123'
      );
    });
  });

  describe('getLicenseOverrideUrl', function () {
    it('should return a URL without componentIdentifier if it is not provided', function () {
      const expectedUrl = 'http://localhost/api/v2/licenseOverrides/app/appId';
      expect(clmLocation.getLicenseOverrideUrl('app', 'appId')).toEqual(expectedUrl);
    });

    it('should return a URL with encoded componentIdentifier if it is provided', function () {
      const stringComponentIdentifier = JSON.stringify({
        format: 'maven',
        coordinates: { version: 'version', group: 'group' },
      });
      const expectedUrl =
        'http://localhost/api/v2/licenseOverrides/app/appId?componentIdentifier=%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22version%22%3A%22version%22%2C%22group%22%3A%22group%22%7D%7D';
      expect(clmLocation.getLicenseOverrideUrl('app', 'appId', stringComponentIdentifier)).toEqual(expectedUrl);
    });
  });

  describe('getLicenseOverrideLegalReviewerUrl', function () {
    it('should return a URL without componentIdentifier if it is not provided', function () {
      const expectedUrl = 'http://localhost/api/v2/licenseOverrides/app/appId/legalReviewer';
      expect(clmLocation.getLicenseOverrideLegalReviewerUrl('app', 'appId')).toEqual(expectedUrl);
    });

    it('should return a URL with encoded componentIdentifier if it is provided', function () {
      const stringComponentIdentifier = JSON.stringify({
        format: 'maven',
        coordinates: { version: 'version', group: 'group' },
      });
      const expectedUrl =
        'http://localhost/api/v2/licenseOverrides/app/appId/legalReviewer?componentIdentifier=%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22version%22%3A%22version%22%2C%22group%22%3A%22group%22%7D%7D';
      expect(clmLocation.getLicenseOverrideLegalReviewerUrl('app', 'appId', stringComponentIdentifier)).toEqual(
        expectedUrl
      );
    });
  });

  describe('getImportSbomUrl', function () {
    it('should return the url for importing SBOM and appending the application id at the end', function () {
      expect(clmLocation.getImportSbomUrl('applicationId')).toBe('http://localhost/rest/sbom/detect/applicationId');
    });
  });

  describe('getCommitImportedSbomUrl', function () {
    it('should return the url for committing SBOM import', function () {
      expect(clmLocation.getCommitImportedSbomUrl('applicationId', 'requestId')).toBe(
        'http://localhost/rest/sbom/commit/applicationId/requestId'
      );
    });
  });

  describe('getSbomPolicyViolationReportUrl', function () {
    it('should return the url for getting SBOM policy violation report', function () {
      expect(clmLocation.getSbomPolicyViolationReportUrl('application-public-id', 'sbom-version')).toBe(
        'http://localhost/rest/report/application-public-id/sbom/sbom-version/sbomPolicyViolationReport'
      );
    });

    it('should return the url for getting SBOM policy violation report with componentRef', function () {
      expect(
        clmLocation.getSbomPolicyViolationReportUrl('application-public-id', 'sbom-version', 'component-ref')
      ).toBe(
        'http://localhost/rest/report/application-public-id/sbom/sbom-version/sbomPolicyViolationReport?componentRef=component-ref'
      );
    });

    it('should return the url for getting SBOM policy violation report with fileCoordinateId and hash', function () {
      expect(
        clmLocation.getSbomPolicyViolationReportUrl(
          'application-public-id',
          'sbom-version',
          null,
          'file-coordinate-id',
          'some-hash'
        )
      ).toBe(
        'http://localhost/rest/report/application-public-id/sbom/sbom-version/sbomPolicyViolationReport?fileCoordinateId=file-coordinate-id&hash=some-hash'
      );
    });

    it('should return the url for getting SBOM policy violation report with fileCoordinateId', function () {
      expect(
        clmLocation.getSbomPolicyViolationReportUrl(
          'application-public-id',
          'sbom-version',
          null,
          'file-coordinate-id',
          null
        )
      ).toBe(
        'http://localhost/rest/report/application-public-id/sbom/sbom-version/sbomPolicyViolationReport?fileCoordinateId=file-coordinate-id'
      );
    });

    it('should return the url for getting SBOM policy violation report with hash', function () {
      expect(
        clmLocation.getSbomPolicyViolationReportUrl('application-public-id', 'sbom-version', null, null, 'some-hash')
      ).toBe(
        'http://localhost/rest/report/application-public-id/sbom/sbom-version/sbomPolicyViolationReport?hash=some-hash'
      );
    });

    it('should return the url for getting SBOM policy violation report with componentRef, fileCoordinateId and hash', function () {
      expect(
        clmLocation.getSbomPolicyViolationReportUrl(
          'application-public-id',
          'sbom-version',
          'component-ref',
          'file-coordinate-id',
          'some-hash'
        )
      ).toBe(
        'http://localhost/rest/report/application-public-id/sbom/sbom-version/sbomPolicyViolationReport?componentRef=component-ref&fileCoordinateId=file-coordinate-id&hash=some-hash'
      );
    });
  });

  describe('getTotalSbomsAnalyzedUrl', function () {
    it('should return the url for getting total SBOMs analyzed', function () {
      expect(clmLocation.getTotalSbomsAnalyzedUrl()).toBe('http://localhost/rest/sbom/dashboard/sbomsAnalyzed');
    });
  });

  describe('getSbomsHistoryUrl', function () {
    it('should return the url for getting SBOMs history', function () {
      expect(clmLocation.getSbomsHistoryUrl()).toBe('http://localhost/rest/sbom/dashboard/sbomsHistoryMetrics');
    });
  });

  describe('getSbomsHighPriorityVulnerabilitiesUrl', function () {
    it('should return the url for dashboard SBOM high priority vulnerabilities', function () {
      expect(clmLocation.getSbomsHighPriorityVulnerabilitiesUrl()).toBe(
        'http://localhost/rest/sbom/dashboard/highPriorityVulnerabilities'
      );
    });
  });

  describe('getSbomReleaseStatusUrl', function () {
    it('should return the url for dashboard SBOM release status', function () {
      expect(clmLocation.getSbomReleaseStatusUrl()).toBe('http://localhost/rest/sbom/dashboard/sbomReleaseStatus');
    });
  });

  describe('getRecentlyImportedSbomsUrl', function () {
    it('should return the url for dashboard recently imported SBOMs', function () {
      expect(clmLocation.getRecentlyImportedSbomsUrl()).toBe(
        'http://localhost/rest/sbom/dashboard/recentlyImportedSboms'
      );
    });
  });

  describe('getVulnerabilitesByThreatLevelUrl', function () {
    it('should return the url for getting vulnerabilities by threat level', function () {
      expect(clmLocation.getVulnerabilitesByThreatLevelUrl()).toBe(
        'http://localhost/rest/sbom/dashboard/vulnerabilitiesByThreatLevel'
      );
    });
  });

  describe('getExportCycloneDx', function () {
    it('should return SBOM url', () => {
      expect(clmLocation.getExportCycloneDxUrl('applicationId', 'scanId')).toBe(
        'http://localhost/ui/links/cycloneDx/applicationId/reports/scanId'
      );
    });
  });

  describe('getExportSpdxUrl', function () {
    it('should return SPDX url', () => {
      expect(clmLocation.getExportSpdxUrl('applicationId', 'scanId')).toBe(
        'http://localhost/ui/links/spdx/applicationId/reports/scanId'
      );
    });
  });

  describe('getDownloadPdfUrl', function () {
    it('should return pdf url', () => {
      expect(clmLocation.getDownloadPdfUrl('applicationPublicId', 'scanId')).toBe(
        'http://localhost/rest/report/applicationPublicId/scanId/printReport'
      );
    });
  });

  describe('getProprietaryConfigUrl', () => {
    it('should return a URL with proper ownerType and ownerId', () => {
      expect(clmLocation.getProprietaryConfigUrl('application', 'ownerId')).toBe(
        'http://localhost/rest/proprietary/application/ownerId'
      );
    });
  });

  describe('getLabelsUrl', () => {
    it('should return a URL with proper ownerType and ownerId', () => {
      expect(clmLocation.getLabelsUrl('application', 'application')).toBe(
        'http://localhost/api/v2/labels/application/application'
      );
    });
  });

  describe('getDeleteLabelsUrl', () => {
    it('should return a URL with proper ownerType, ownerId and labelId', () => {
      expect(clmLocation.getDeleteLabelsUrl('application', 'application', '1240987fd8sdf')).toBe(
        'http://localhost/api/v2/labels/application/application/1240987fd8sdf'
      );
    });
  });

  describe('getConditionValueTypeUrl', () => {
    it('should return a URL with proper ownerType, ownerId', () => {
      expect(clmLocation.getConditionValueTypeUrl('application', 'ownerId')).toBe(
        'http://localhost/rest/conditionValueType/application/ownerId'
      );
    });
  });

  describe('getTestRepositoryConnectionUrl', function () {
    it('should return a URL without a repositoryConnectionId if it is not provided', function () {
      const expectedUrl = 'http://localhost/api/v2/config/repositoryConnection/some%3AOwnerType/some%3AOwnerId/test';
      expect(clmLocation.getTestRepositoryConnectionUrl('some:OwnerType', 'some:OwnerId')).toEqual(expectedUrl);
    });

    it('should return a URL with a repositoryConnectionId if it is provided', function () {
      const expectedUrl =
        'http://localhost/api/v2/config/repositoryConnection/some%3AOwnerType/some%3AOwnerId/some%3ARepositoryConnectionId/test';
      expect(
        clmLocation.getTestRepositoryConnectionUrl('some:OwnerType', 'some:OwnerId', 'some:RepositoryConnectionId')
      ).toEqual(expectedUrl);
    });
  });

  describe('getRepositoryConnectionUrl', function () {
    it('should return a URL without a repositoryConnectionId if it is not provided', function () {
      const expectedUrl = 'http://localhost/api/v2/config/repositoryConnection/some%3AOwnerType/some%3AOwnerId';
      expect(clmLocation.getRepositoryConnectionUrl('some:OwnerType', 'some:OwnerId')).toEqual(expectedUrl);
    });

    it('should return a URL with a repositoryConnectionId if it is provided', function () {
      const expectedUrl =
        'http://localhost/api/v2/config/repositoryConnection/some%3AOwnerType/some%3AOwnerId/some%3ARepositoryConnectionId';
      expect(
        clmLocation.getRepositoryConnectionUrl('some:OwnerType', 'some:OwnerId', 'some:RepositoryConnectionId')
      ).toEqual(expectedUrl);
    });

    it('should return a URL with inherit query param if it is provided', function () {
      const expectedUrl =
        'http://localhost/api/v2/config/repositoryConnection/some%3AOwnerType/some%3AOwnerId?inherit=true';
      expect(clmLocation.getRepositoryConnectionUrl('some:OwnerType', 'some:OwnerId', null, true)).toEqual(expectedUrl);
    });
  });

  describe('getTestArtifactoryConnectionUrl', function () {
    it('should return a URL without an artifactoryConnectionId if it is not provided', function () {
      const expectedUrl = 'http://localhost/api/v2/config/artifactoryConnection/some%3AOwnerType/some%3AOwnerId/test';
      expect(clmLocation.getTestArtifactoryConnectionUrl('some:OwnerType', 'some:OwnerId')).toEqual(expectedUrl);
    });

    it('should return a URL with an artifactoryConnectionId if it is provided', function () {
      const expectedUrl =
        'http://localhost/api/v2/config/artifactoryConnection/some%3AOwnerType/some%3AOwnerId/some%3AArtifactoryConnectionId/test';
      expect(
        clmLocation.getTestArtifactoryConnectionUrl('some:OwnerType', 'some:OwnerId', 'some:ArtifactoryConnectionId')
      ).toEqual(expectedUrl);
    });
  });

  describe('getArtifactoryConnectionUrl', function () {
    it('should return a URL without an artifactoryConnectionId if it is not provided', function () {
      const expectedUrl = 'http://localhost/api/v2/config/artifactoryConnection/some%3AOwnerType/some%3AOwnerId';
      expect(clmLocation.getArtifactoryConnectionUrl('some:OwnerType', 'some:OwnerId')).toEqual(expectedUrl);
    });

    it('should return a URL with an artifactoryConnectionId if it is provided', function () {
      const expectedUrl =
        'http://localhost/api/v2/config/artifactoryConnection/some%3AOwnerType/some%3AOwnerId/some%3AArtifactoryConnectionId';
      expect(
        clmLocation.getArtifactoryConnectionUrl('some:OwnerType', 'some:OwnerId', 'some:ArtifactoryConnectionId')
      ).toEqual(expectedUrl);
    });

    it('should return a URL with inherit query param if it is provided', function () {
      const expectedUrl =
        'http://localhost/api/v2/config/artifactoryConnection/some%3AOwnerType/some%3AOwnerId?inherit=true';
      expect(clmLocation.getArtifactoryConnectionUrl('some:OwnerType', 'some:OwnerId', null, true)).toEqual(
        expectedUrl
      );
    });
  });

  describe('getPolicyMonitoringUrl', () => {
    it('should return a URL with proper ownerType and ownerId', () => {
      expect(clmLocation.getPolicyMonitoringUrl('application', 'application')).toBe(
        'http://localhost/rest/policyMonitoring/application/application'
      );
    });
  });

  describe('getApplicablePolicyMonitoringUrl', () => {
    it('should return a URL with proper ownerType and ownerId', () => {
      expect(clmLocation.getApplicablePolicyMonitoringUrl('application', 'application')).toBe(
        'http://localhost/rest/policyMonitoring/application/application/applicable'
      );
    });
  });

  describe('getOwnerDetailsUrl', () => {
    it('should return a URL with proper ownerType and ownerId', () => {
      expect(clmLocation.getOwnerDetailsUrl('application', 'application')).toBe(
        'http://localhost/rest/sidebar/application/application/details'
      );
    });

    it('should return a URL with repository_container', () => {
      expect(clmLocation.getOwnerDetailsUrl('application', 'application', true)).toBe(
        'http://localhost/rest/sidebar/repository_container/details'
      );
    });
  });

  describe('getCategoriesUrl', () => {
    it('returns url for applicationCategories', () => {
      const expectedUrl = 'http://localhost/api/v2/applicationCategories/organization/ROOT_ORGANIZATION_ID';

      expect(clmLocation.getCategoriesUrl('organization', 'ROOT_ORGANIZATION_ID')).toEqual(expectedUrl);
    });
  });

  describe('getOrganizationAppliedTagUrl', () => {
    it('returns url for organization applied categories', () => {
      const expectedUrl = 'http://localhost/api/v2/applicationCategories/organization/ROOT_ORGANIZATION_ID/applied';

      expect(clmLocation.getOrganizationAppliedTagUrl('ROOT_ORGANIZATION_ID')).toEqual(expectedUrl);
    });
  });

  describe('getApplicableCategoriesUrl', () => {
    it('returns url for applicationCategories', () => {
      const expectedUrl = 'http://localhost/api/v2/applicationCategories/application/someApplication';

      expect(clmLocation.getApplicableCategoriesUrl('application', 'someApplication')).toEqual(expectedUrl);
    });

    it('returns url for applicable applicationCategories', () => {
      const expectedUrl = 'http://localhost/api/v2/applicationCategories/organization/someOrganization/applicable';

      expect(clmLocation.getApplicableCategoriesUrl('organization', 'someOrganization')).toEqual(expectedUrl);
    });
  });

  describe('getDeleteCategoriesUrl', () => {
    it('returns url for delete category', () => {
      const expectedUrl = 'http://localhost/api/v2/applicationCategories/organization/someOrganization/categoryId';

      expect(clmLocation.getDeleteCategoriesUrl('organization', 'someOrganization', 'categoryId')).toEqual(expectedUrl);
    });
  });

  describe('getOrganizationPolicyTagUrl', () => {
    it('returns url for delete category', () => {
      const expectedUrl = 'http://localhost/api/v2/applicationCategories/organization/someOrganization/policy';

      expect(clmLocation.getOrganizationPolicyTagUrl('someOrganization')).toEqual(expectedUrl);
    });
  });

  describe('getPolicyUrl', () => {
    it('returns base url for policy', () => {
      const expectedUrl = 'http://localhost/rest/policy/organization/someOrganization';

      expect(clmLocation.getPolicyUrl('organization', 'someOrganization')).toBe(expectedUrl);
    });
  });

  describe('getPolicyNotificationsUrl', () => {
    it('returns base url for policy notifications', () => {
      const expectedUrl = 'http://localhost/rest/policy/organization/someOrganization/notifications';

      expect(clmLocation.getPolicyNotificationsUrl('organization', 'someOrganization')).toBe(expectedUrl);
    });
  });

  describe('getPoliciesWithProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCodeUrl', () => {
    it('returns url for get policies with conditions', () => {
      const expectedUrl =
        'http://localhost/rest/policy/repository_container/REPOSITORY_CONTAINER_ID/withProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCode';

      expect(clmLocation.getPoliciesWithProprietaryNameConflictAndSecurityVulnerabilityCategoryMaliciousCodeUrl()).toBe(
        expectedUrl
      );
    });
  });

  describe('getPolicyCRUDUrl', () => {
    it('returns CRUD url for policy', () => {
      const expectedUrl = 'http://localhost/rest/policy/organization/someOrganization/somePolicyId';

      expect(clmLocation.getPolicyCRUDUrl('organization', 'someOrganization', 'somePolicyId')).toBe(expectedUrl);
    });
  });

  describe('getPolicyOverridesUrl', () => {
    it('returns policy overrides url ', () => {
      const expectedUrl = 'http://localhost/rest/policy/organization/someOrganization/somePolicyId/overrides';

      expect(clmLocation.getPolicyOverridesUrl('organization', 'someOrganization', 'somePolicyId')).toBe(expectedUrl);
    });
  });

  describe('getApplicablePolicies', () => {
    it('returns applicable policies url', () => {
      const expectedUrl = 'http://localhost/rest/policy/organization/someOrganization/applicable';

      expect(clmLocation.getApplicablePolicies('organization', 'someOrganization')).toBe(expectedUrl);
    });
  });

  describe('getPolicyTagUrl', () => {
    it('returns applicable policies url', () => {
      const expectedUrl = 'http://localhost/rest/appliedTag/policy/somePolicyId/organization/someOrganization';

      expect(clmLocation.getPolicyTagUrl('somePolicyId', 'organization', 'someOrganization')).toBe(expectedUrl);
    });
  });
  describe('getEnableUnauthenticatedPages', () => {
    it('returns url to get getEnableUnauthenticatedPages feature configuration', () => {
      expect(clmLocation.getEnableUnauthenticatedPages()).toEqual(
        'http://localhost/rest/product/features/enableUnauthenticatedPages'
      );
    });
  });

  describe('getEnableSsoOnly', () => {
    it('returns url to get ENABLE_SSO_ONLY flag', () => {
      expect(clmLocation.getEnableSsoOnly()).toEqual('http://localhost/rest/product/features/enableSsoOnly');
    });
  });

  describe('getOAuth2Enabled', () => {
    it('returns url to get OAUTH2_ENABLED flag', () => {
      expect(clmLocation.getOAuth2Enabled()).toEqual('http://localhost/rest/product/features/oauth2Enabled');
    });
  });

  describe('getQuarantinedComponentViewAnonymousAccessEnabledState', () => {
    it('returns url for anonymous access configuration', () => {
      expect(clmLocation.getQuarantinedComponentViewAnonymousAccessEnabledState()).toEqual(
        'http://localhost/api/v2/firewall/quarantinedComponentView/configuration/anonymousAccess/'
      );
    });
  });

  describe('getCrowdConfigurationUrl', () => {
    it('returns url for atlassian crowd configuration', () => {
      const expectedUrl = 'http://localhost/api/v2/config/crowd';

      expect(clmLocation.getCrowdConfigurationUrl()).toEqual(expectedUrl);
    });
  });

  describe('getCrowdConfigurationTestUrl', () => {
    it('returns url for atlassian crowd configuration', () => {
      const expectedUrl = 'http://localhost/api/v2/config/crowd/test';

      expect(clmLocation.getCrowdConfigurationTestUrl()).toEqual(expectedUrl);
    });
  });

  describe('getRepositoriesUrl', () => {
    it('returns repositories url', () => {
      const expectedUrl = 'http://localhost/rest/repositories';

      expect(clmLocation.getRepositoriesUrl()).toEqual(expectedUrl);
    });
  });

  describe('getRepositoryInfoUrl', () => {
    it('returns url for delete repository', () => {
      const expectedUrl = 'http://localhost/rest/repositories/someRepositoryId';

      expect(clmLocation.getRepositoryInfoUrl('someRepositoryId')).toEqual(expectedUrl);
    });
  });

  describe('getPolicyEvaluationTimestampUrl', () => {
    it('returns url to get component policy evaluation timestamps', () => {
      const expectedUrl =
        'http://localhost/rest/repositories/repositoryId/policyEvaluationTimestamps?componentIdentifier=componentIdentifier';
      expect(clmLocation.getPolicyEvaluationTimestampUrl('repositoryId', 'componentIdentifier')).toEqual(expectedUrl);
    });
  });

  describe('getEndpointsUrl', () => {
    it('returns url for endpoints with the given api type parameter', () => {
      const expectedUrl = 'http://localhost/api/v2/endpoints/api-type';

      expect(clmLocation.getEndpointsUrl('api-type')).toEqual(expectedUrl);
    });
  });

  describe('getRepositoryComponentsUrl', () => {
    it('returns url to get repository components details', () => {
      const expectedUrl = 'http://localhost/api/experimental/repositories/repository/repositoryId/results/details';

      expect(clmLocation.getRepositoryComponentsUrl('repository', 'repositoryId')).toEqual(expectedUrl);
    });
  });

  describe('getIsJiraEnabledUrl', () => {
    it('returns url for whether jira is enabled', () => {
      const expectedUrl = 'http://localhost/rest/jira/enabled';

      expect(clmLocation.getIsJiraEnabledUrl()).toEqual(expectedUrl);
    });
  });

  describe('getJiraProjectsUrl', () => {
    it('returns url for jira projects', () => {
      const expectedUrl = 'http://localhost/rest/jira/project';

      expect(clmLocation.getJiraProjectsUrl()).toEqual(expectedUrl);
    });
  });

  describe('getComponentPolicyViolationsUrl', () => {
    it('returns url to get component policy violations', () => {
      const expectedUrl =
        'http://localhost/rest/repositories/repositoryId/policyViolations/some/component-path/1.0.0/component-name-1.0.0.jar';
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
      const expectedUrl = 'http://localhost/rest/sidebar';
      expect(clmLocation.getOwnerListUrl()).toEqual(expectedUrl);
    });
  });

  describe('getPermissionContextTestUrl', () => {
    const ownerType = 'ownerType';

    it('returns url to get permissions for ownerType', () => {
      const expectedUrl = `http://localhost/rest/user/permissions/${ownerType}`;

      expect(clmLocation.getPermissionContextTestUrl(ownerType)).toEqual(expectedUrl);
    });

    it('returns url to get permissions for ownerType and ownerId', () => {
      const ownerId = 'ownerId';
      const expectedUrl = `http://localhost/rest/user/permissions/${ownerType}/${ownerId}`;

      expect(clmLocation.getPermissionContextTestUrl(ownerType, ownerId)).toEqual(expectedUrl);
    });
  });
  describe('getRepositoryPolicyViolationUrl', () => {
    it('should return a URL with proper repositoryId and repositoryPolicyId', () => {
      expect(clmLocation.getRepositoryPolicyViolationUrl('repositoryId', 'repositoryPolicyId')).toBe(
        'http://localhost/rest/repositories/repositoryId/policyViolation/repositoryPolicyId'
      );
    });
  });

  describe('getRepositoryPolicyViolationUrl', () => {
    it('should return a URL with proper repositoryId and repositoryPolicyId', () => {
      expect(clmLocation.getRepositoryEvaluateUrl('repositoryId')).toBe(
        'http://localhost/rest/repositories/repositoryId/evaluate'
      );
    });
  });

  describe('getComponentDisplayNameByIdentifierUrl', () => {
    it('should return a URL to get the Display Name based on a component identifier string', () => {
      const componentIdentifier = '{"coordinates": "name"}';
      expect(clmLocation.getComponentDisplayNameByIdentifierUrl(componentIdentifier)).toBe(
        'http://localhost/rest/componentDetails/nameByIdentifier?componentIdentifier=%7B%22coordinates%22%3A%20%22name%22%7D'
      );
    });
  });

  describe('getVulnerabilityCustomRemediationRefIdUrl', () => {
    it('should return a URL to customize the remediation of a vulnerability without component identifier', () => {
      expect(clmLocation.getVulnerabilityCustomRemediationRefIdUrl('application', 'testId', 'CVE-123')).toBe(
        'http://localhost/api/experimental/vulnerability/customData/application/testId/remediation/refId/CVE-123'
      );
    });

    it('should return a URL to customize the remediation of a vulnerability with component identifier', () => {
      const componentIdentifier = '{"coordinates": "name"}';
      expect(
        clmLocation.getVulnerabilityCustomRemediationRefIdUrl('application', 'testId', 'CVE-123', componentIdentifier)
      ).toBe(
        'http://localhost/api/experimental/vulnerability/customData/application/testId/remediation/refId/CVE-123' +
          '?componentIdentifier=' +
          componentIdentifier
      );
    });
  });

  describe('getVulnerabilityCustomRemediationUrl', () => {
    it('should return a URL to get the custom remediation of a vulnerability by scope', () => {
      expect(clmLocation.getVulnerabilityCustomRemediationUrl('application', 'testId')).toBe(
        'http://localhost/api/experimental/vulnerability/customData/application/testId/remediation'
      );
    });
  });

  describe('getVulnerabilityCustomRemediationIdUrl', () => {
    it('should return a URL to get the custom remediation of a vulnerability by ID', () => {
      expect(clmLocation.getVulnerabilityCustomRemediationIdUrl('application', 'testId', 'some-id')).toBe(
        'http://localhost/api/experimental/vulnerability/customData/application/testId/remediation/some-id'
      );
    });
  });

  describe('getVulnerabilityCustomCweRefIdUrl', () => {
    it('should return a URL to customize the cwe of a vulnerability without component identifier', () => {
      expect(clmLocation.getVulnerabilityCustomCweRefIdUrl('application', 'testId', 'CVE-123')).toBe(
        'http://localhost/api/experimental/vulnerability/customData/application/testId/cwe/refId/CVE-123'
      );
    });

    it('should return a URL to customize the cwe of a vulnerability with component identifier', () => {
      const componentIdentifier = '{"coordinates": "name"}';
      expect(
        clmLocation.getVulnerabilityCustomCweRefIdUrl('application', 'testId', 'CVE-123', componentIdentifier)
      ).toBe(
        'http://localhost/api/experimental/vulnerability/customData/application/testId/cwe/refId/CVE-123' +
          '?componentIdentifier=' +
          componentIdentifier
      );
    });
  });

  describe('getVulnerabilityCustomCweUrl', () => {
    it('should return a URL to get the custom cwe of a vulnerability by scope', () => {
      expect(clmLocation.getVulnerabilityCustomCweUrl('application', 'testId')).toBe(
        'http://localhost/api/experimental/vulnerability/customData/application/testId/cwe'
      );
    });
  });

  describe('getVulnerabilityCustomCweIdUrl', () => {
    it('should return a URL to get the custom cwe of a vulnerability by ID', () => {
      expect(clmLocation.getVulnerabilityCustomCweIdUrl('application', 'testId', 'some-id')).toBe(
        'http://localhost/api/experimental/vulnerability/customData/application/testId/cwe/some-id'
      );
    });
  });

  describe('getVulnerabilityCustomCvssVectorRefIdUrl', () => {
    it('should return a URL to customize the CVSS vector of a vulnerability without component identifier', () => {
      expect(clmLocation.getVulnerabilityCustomCvssVectorRefIdUrl('application', 'testId', 'CVE-123')).toBe(
        'http://localhost/api/experimental/vulnerability/customData/application/testId/cvss/vector/refId/CVE-123'
      );
    });

    it('should return a URL to customize the CVSS vector of a vulnerability with component identifier', () => {
      const componentIdentifier = '{"coordinates": "name"}';
      expect(
        clmLocation.getVulnerabilityCustomCvssVectorRefIdUrl('application', 'testId', 'CVE-123', componentIdentifier)
      ).toBe(
        'http://localhost/api/experimental/vulnerability/customData/application/testId/cvss/vector/refId/CVE-123' +
          '?componentIdentifier=' +
          componentIdentifier
      );
    });
  });

  describe('getVulnerabilityCustomCvssVectorUrl', () => {
    it('should return a URL to get the custom CVSS vector of a vulnerability by scope', () => {
      expect(clmLocation.getVulnerabilityCustomCvssVectorUrl('application', 'testId')).toBe(
        'http://localhost/api/experimental/vulnerability/customData/application/testId/cvss/vector'
      );
    });
  });

  describe('getVulnerabilityCustomCvssVectorIdUrl', () => {
    it('should return a URL to get the custom CVSS vector of a vulnerability by ID', () => {
      expect(clmLocation.getVulnerabilityCustomCvssVectorIdUrl('application', 'testId', 'some-id')).toBe(
        'http://localhost/api/experimental/vulnerability/customData/application/testId/cvss/vector/some-id'
      );
    });
  });

  describe('getVulnerabilityCustomCvssSeverityRefIdUrl', () => {
    it('should return a URL to customize the CVSS severity of a vulnerability without component identifier', () => {
      expect(clmLocation.getVulnerabilityCustomCvssSeverityRefIdUrl('application', 'testId', 'CVE-123')).toBe(
        'http://localhost/api/experimental/vulnerability/customData/application/testId/cvss/severity/refId/CVE-123'
      );
    });

    it('should return a URL to customize the CVSS severity of a vulnerability with component identifier', () => {
      const componentIdentifier = '{"coordinates": "name"}';
      expect(
        clmLocation.getVulnerabilityCustomCvssSeverityRefIdUrl('application', 'testId', 'CVE-123', componentIdentifier)
      ).toBe(
        'http://localhost/api/experimental/vulnerability/customData/application/testId/cvss/severity/refId/CVE-123' +
          '?componentIdentifier=' +
          componentIdentifier
      );
    });
  });

  describe('getVulnerabilityCustomCvssSeverityUrl', () => {
    it('should return a URL to get the custom CVSS severity of a vulnerability by scope', () => {
      expect(clmLocation.getVulnerabilityCustomCvssSeverityUrl('application', 'testId')).toBe(
        'http://localhost/api/experimental/vulnerability/customData/application/testId/cvss/severity'
      );
    });
  });

  describe('getVulnerabilityCustomCvssSeverityIdUrl', () => {
    it('should return a URL to get the custom CVSS severity of a vulnerability by ID', () => {
      expect(clmLocation.getVulnerabilityCustomCvssSeverityIdUrl('application', 'testId', 'some-id')).toBe(
        'http://localhost/api/experimental/vulnerability/customData/application/testId/cvss/severity/some-id'
      );
    });
  });

  describe('getFirewallQuarantineListUrl', () => {
    it('should return a URL to get the firewall quarantine list', () => {
      expect(clmLocation.getFirewallQuarantineListUrl()).toBe(
        'http://localhost/api/v2/firewall/components/quarantined'
      );
    });

    it('should return a URL to get the firewall quarantine list with parameters', () => {
      expect(clmLocation.getFirewallQuarantineListUrl(1, 2, 'field', true, 'id', 'name', 'publicId', 1)).toBe(
        'http://localhost/api/v2/firewall/components/quarantined?page=1&pageSize=2&sortBy=field&asc=true&policyId=id&componentName=name&repositoryPublicId=publicId&quarantineTime=1'
      );
    });

    it('handles a single parameter correctly', () => {
      expect(clmLocation.getFirewallQuarantineListUrl(2)).toBe(
        'http://localhost/api/v2/firewall/components/quarantined?page=2'
      );
    });

    it('handles a different parameter order correctly', () => {
      expect(clmLocation.getFirewallQuarantineListUrl(null, 3, 'field')).toBe(
        'http://localhost/api/v2/firewall/components/quarantined?pageSize=3&sortBy=field'
      );
    });
  });

  describe('getSourceControlRateLimitsUrl', () => {
    it('returns url for source control rate limits with the given owner type and owner id parameters', () => {
      const expectedUrl = 'http://localhost/api/experimental/sourceControl/someType/someId/rateLimits';

      expect(clmLocation.getSourceControlRateLimitsUrl('someType', 'someId')).toEqual(expectedUrl);
    });
  });

  describe('getPolicyViolationUiLink', () => {
    it('returns url for the ui policy violation', () => {
      const expectedUrl = 'http://localhost/ui/links/policyViolation/%23%2Fviolation%2FsomeViolationId';

      expect(clmLocation.getPolicyViolationUiLink('#/violation/someViolationId')).toEqual(expectedUrl);
    });
  });

  describe('getRepositoryManagerUrl', () => {
    it('returns the url for the repository manager with the given id and name', () => {
      const expectedUrl =
        'http://localhost/rest/repositories/repositoryManager/someRepositoryManagerId/someRepositoryManagerName';

      expect(clmLocation.getRepositoryManagerUrl('someRepositoryManagerId', 'someRepositoryManagerName')).toEqual(
        expectedUrl
      );
    });
  });

  describe('getWaiverRequestWebhooksCountUrl', () => {
    it('returns the url for the internal endpoint for getting the ids of waiver request webhooks', () => {
      const expectedUrl = 'http://localhost/rest/config/webhook/waiverRequestCount';

      expect(clmLocation.getWaiverRequestWebhooksCountUrl()).toEqual(expectedUrl);
    });
  });

  describe('getRepositoryContainerUrl', () => {
    it('returns the url for the repository container', () => {
      const expectedUrl = 'http://localhost/api/v2/firewall/repositoryContainer';

      expect(clmLocation.getRepositoryContainer()).toEqual(expectedUrl);
    });
  });

  describe('getRepositoryComponentNameUrl', () => {
    it('returns the url to get the proprietary component name patterns', () => {
      const expectedUrl =
        'http://localhost/rest/repositories/someOwnerType/someOwnerId/proprietaryComponentNamePatterns';

      expect(clmLocation.getRepositoryComponentNameUrl('someOwnerType', 'someOwnerId')).toEqual(expectedUrl);
    });
  });

  describe('getRepositoryComponentNamePatternUpdateUrl', () => {
    it('returns the url to update the proprietary component name pattern', () => {
      const expectedUrl = 'http://localhost/rest/repositories/proprietaryComponentNamePatterns/update';

      expect(clmLocation.getRepositoryComponentNamePatternUpdateUrl()).toEqual(expectedUrl);
    });
  });

  describe('getAdvancedSearchUrl', () => {
    it('returns the url with the query, page, and allComponents false', () => {
      const expectedUrl = 'http://localhost/api/v2/search/advanced?query=some%26Query&page=0&allComponents=false';

      expect(clmLocation.getAdvancedSearchUrl('some&Query', 0, false, false)).toEqual(expectedUrl);
    });

    it('returns the url with the query, page, and allComponents true', () => {
      const expectedUrl = 'http://localhost/api/v2/search/advanced?query=some%26Query&page=0&allComponents=true';

      expect(clmLocation.getAdvancedSearchUrl('some&Query', 0, true, false)).toEqual(expectedUrl);
    });

    it('returns the url with the query, page, allComponents false, and mode sbomManager', () => {
      const expectedUrl =
        'http://localhost/api/v2/search/advanced?query=some%26Query&page=0&allComponents=false&mode=sbomManager';

      expect(clmLocation.getAdvancedSearchUrl('some&Query', 0, false, true)).toEqual(expectedUrl);
    });

    it('returns the url with the query, page, allComponents true, and mode sbomManager', () => {
      const expectedUrl =
        'http://localhost/api/v2/search/advanced?query=some%26Query&page=0&allComponents=true&mode=sbomManager';

      expect(clmLocation.getAdvancedSearchUrl('some&Query', 0, true, true)).toEqual(expectedUrl);
    });

    it('returns the url with the query, page, allComponents true, mode sbomManager, and searchAfter', () => {
      const expectedUrl =
        'http://localhost/api/v2/search/advanced?query=some%26Query&page=0&allComponents=true&mode=sbomManager' +
        '&searchAfter=1.0%2CYbqImJgBqQdzRfhlOQzg';

      expect(clmLocation.getAdvancedSearchUrl('some&Query', 0, true, true, ['1.0', 'YbqImJgBqQdzRfhlOQzg'])).toEqual(
        expectedUrl
      );
    });
  });

  describe('getSbomsByApplicationUrl', () => {
    it('should return the correct URL with the given parameters', () => {
      const applicationId = 'abc123';
      const sortBy = 'import_date';
      const asc = false;
      const pageSize = 10;
      const page = 0;
      const expectedURL = `http://localhost/api/v2/sbom/applications/${applicationId}?page=${page}&pageSize=${pageSize}&sortBy=${sortBy}&asc=${asc}`;

      expect(clmLocation.getSbomsByApplicationUrl(applicationId, page, pageSize, sortBy, asc)).toBe(expectedURL);
    });
  });

  describe('getDownloadSbomFileUrl', () => {
    const applicationId = 'application-id';
    const applicationVersion = 'application-version';

    it('should return the correct URL with minimal parameters', () => {
      const expectedURL = `http://localhost/api/v2/sbom/applications/${applicationId}/versions/${applicationVersion}/?state=original`;
      expect(clmLocation.getDownloadSbomFileUrl(applicationId, applicationVersion)).toBe(expectedURL);
    });

    it('should return the correct URL with state and specification', () => {
      const baseURL = `http://localhost/api/v2/sbom/applications/${applicationId}/versions/${applicationVersion}/`;
      const queryParams = `?state=current&specification=cyclonedx1.5`;
      const expectedURL = baseURL + queryParams;
      expect(clmLocation.getDownloadSbomFileUrl(applicationId, applicationVersion, 'current', 'cyclonedx1.5')).toBe(
        expectedURL
      );
    });
  });

  describe('getDeleteSbomByApplicationIdAndVersionUrl', () => {
    it('should return the correct URL with the given parameters', () => {
      const applicationId = 'abc123';
      const applicationVersion = 'v1';
      const expectedURL = `http://localhost/api/v2/sbom/applications/${applicationId}/versions/${applicationVersion}`;

      expect(clmLocation.getDeleteSbomByApplicationIdAndVersionUrl(applicationId, applicationVersion)).toBe(
        expectedURL
      );
    });
  });

  describe('getBillOfMaterialsComponentsUrl', () => {
    const applicationId = 'abc-123';
    const sbomVersion = 'sbom-version';

    it('should return the correct URL with minimum parameters', () => {
      const expectedURL = `http://localhost/api/v2/sbom/applications/${applicationId}/versions/${sbomVersion}/components`;
      expect(clmLocation.getBillOfMaterialsComponentsUrl(applicationId, sbomVersion)).toBe(expectedURL);
    });

    it('should return the correct URL with pagination only', () => {
      const expectedParams = `?page=1&pageSize=50`;
      const expectedURL =
        `http://localhost/api/v2/sbom/applications/${applicationId}/versions/${sbomVersion}/components` +
        expectedParams;
      expect(clmLocation.getBillOfMaterialsComponentsUrl(applicationId, sbomVersion, 1, 50)).toBe(expectedURL);
    });

    it('should return the correct URL with componentName only', () => {
      const expectedParams = `?filter=Hello`;
      const expectedURL =
        `http://localhost/api/v2/sbom/applications/${applicationId}/versions/${sbomVersion}/components` +
        expectedParams;
      expect(
        clmLocation.getBillOfMaterialsComponentsUrl(
          applicationId,
          sbomVersion,
          null,
          null,
          null,
          null,
          null,
          null,
          'Hello'
        )
      ).toBe(expectedURL);
    });

    it('should return the correct URL with sortConfiguration', () => {
      const expectedParams = `?page=1&sortBy=vulnerabilities&asc=true`;
      const expectedURL =
        `http://localhost/api/v2/sbom/applications/${applicationId}/versions/${sbomVersion}/components` +
        expectedParams;
      expect(
        clmLocation.getBillOfMaterialsComponentsUrl(applicationId, sbomVersion, 1, null, 'vulnerabilities', true)
      ).toBe(expectedURL);
    });

    it('should return the correct URL with only list params', () => {
      const expectedVulnerabilityThreatLevelsParams = `vulnerabilityThreatLevels=critical&vulnerabilityThreatLevels=medium`;
      const expectedDependencyTypesParams = `dependencyTypes=direct&dependencyTypes=transitive`;

      const vulnerabilityThreatLevels = ['critical', 'medium'];
      const dependencyTypes = ['direct', 'transitive'];

      const expectedURL = `http://localhost/api/v2/sbom/applications/${applicationId}/versions/${sbomVersion}/components`;
      expect(
        clmLocation.getBillOfMaterialsComponentsUrl(
          applicationId,
          sbomVersion,
          null,
          null,
          null,
          null,
          vulnerabilityThreatLevels
        )
      ).toBe(expectedURL + '?' + expectedVulnerabilityThreatLevelsParams);

      expect(
        clmLocation.getBillOfMaterialsComponentsUrl(
          applicationId,
          sbomVersion,
          null,
          null,
          null,
          null,
          null,
          dependencyTypes
        )
      ).toBe(expectedURL + '?' + expectedDependencyTypesParams);

      expect(
        clmLocation.getBillOfMaterialsComponentsUrl(
          applicationId,
          sbomVersion,
          null,
          null,
          null,
          null,
          vulnerabilityThreatLevels,
          dependencyTypes
        )
      ).toBe(expectedURL + '?' + expectedVulnerabilityThreatLevelsParams + '&' + expectedDependencyTypesParams);
    });

    it('should return the correct URL with normal and list params', () => {
      const vulnerabilityThreatLevels = ['critical', 'medium'];
      const dependencyTypes = ['direct', 'transitive'];
      const expectedParams = `?page=1&pageSize=50&sortBy=vulnerabilities&asc=false`;
      const componentNameParam = `&filter=component%20%2B%20%3A%20name`;
      const expectedDependencyTypesParams = `&dependencyTypes=direct&dependencyTypes=transitive`;
      const expectedVulnerabilityThreatLevelsParams = `&vulnerabilityThreatLevels=critical&vulnerabilityThreatLevels=medium`;
      const expectedURL =
        `http://localhost/api/v2/sbom/applications/${applicationId}/versions/${sbomVersion}/components` +
        expectedParams +
        componentNameParam +
        expectedVulnerabilityThreatLevelsParams +
        expectedDependencyTypesParams;

      expect(
        clmLocation.getBillOfMaterialsComponentsUrl(
          applicationId,
          sbomVersion,
          1,
          50,
          'vulnerabilities',
          false,
          vulnerabilityThreatLevels,
          dependencyTypes,
          'component + : name'
        )
      ).toBe(expectedURL);
    });
  });

  describe('getSbomApplicationsUrl', () => {
    it('should return the correct URL with minimum parameters', () => {
      const expectedURL = 'http://localhost/rest/sbom/applications?';
      expect(clmLocation.getSbomApplicationsUrl()).toBe(expectedURL);
    });

    it('should return the correct URL with pagination only', () => {
      const expectedParams = `?page=1&pageSize=50`;
      const expectedURL = 'http://localhost/rest/sbom/applications' + expectedParams;
      expect(clmLocation.getSbomApplicationsUrl(1, 50)).toBe(expectedURL);
    });

    it('should return the correct URL with applicationName only', () => {
      const expectedParams = `?applicationName=Hello`;
      const expectedURL = 'http://localhost/rest/sbom/applications' + expectedParams;
      expect(clmLocation.getSbomApplicationsUrl(null, null, null, null, 'Hello')).toBe(expectedURL);
    });

    it('should return the correct URL with sortConfiguration', () => {
      const expectedParams = `?page=1&sortBy=applicationName&asc=true`;
      const expectedURL = 'http://localhost/rest/sbom/applications' + expectedParams;
      expect(clmLocation.getSbomApplicationsUrl(1, null, 'applicationName', true)).toBe(expectedURL);
    });
  });

  describe('getAllApplicationSbomVersions', () => {
    it('should return the correct URL with the given parameters', () => {
      const applicationId = 'abc123';
      const expectedURL = `http://localhost/api/v2/sbom/applications/${applicationId}/versions`;

      expect(clmLocation.getAllApplicationSbomVersions(applicationId)).toBe(expectedURL);
    });
  });

  describe('getSbomMetadataUrl', () => {
    it('should return the correct URL with the given parameters', () => {
      const applicationId = 'abc123';
      const version = 'def246';
      const expectedURL = `http://localhost/rest/sbom/applications/${applicationId}/versions/${version}/sbomMetadata`;
      expect(clmLocation.getSbomMetadataUrl(applicationId, version)).toBe(expectedURL);
    });
  });

  describe('getSbomSummaryUrl', () => {
    it('should return the correct URL with the given parameters', () => {
      const applicationId = 'abc123';
      const version = 'def246';
      const expectedURL = `http://localhost/rest/sbom/applications/${applicationId}/versions/${version}/summary`;
      expect(clmLocation.getSbomSummaryUrl(applicationId, version)).toBe(expectedURL);
    });
  });

  describe('getLicensedSolutionsUrl', () => {
    it('returns the url for the licensed solutions', () => {
      const expectedUrl = 'http://localhost/api/v2/solutions/licensed?allowRelativeUrls=true';

      expect(clmLocation.getLicensedSolutionsUrl()).toEqual(expectedUrl);
    });
  });

  describe('getOwnerDetailsByTypeAndInternalId', () => {
    it('returns the url to get the owner details by type and internal ID', () => {
      const applicationId = 'abc123';
      const expectedUrl = `http://localhost/rest/owner/application/${applicationId}/details`;

      expect(clmLocation.getOwnerDetailsByTypeAndInternalId('application', applicationId)).toEqual(expectedUrl);
    });
  });

  describe('getEnterpriseReportingSelectedDashboardUrl', () => {
    it('returns the correct URL with the given parameter', () => {
      const dashboardId = 'success-metrics';
      const expectedUrl = `http://localhost/ui/links/enterpriseReporting/${dashboardId}`;

      expect(clmLocation.getEnterpriseReportingSelectedDashboardUrl(dashboardId)).toEqual(expectedUrl);
    });
  });

  describe('getEnterpriseReportingBaseUrl', () => {
    it('returns the correct URL with the given parameter', () => {
      const expectedUrl = `http://localhost/rest/enterpriseReporting/getBaseUrl`;
      expect(clmLocation.getEnterpriseReportingBaseUrl()).toEqual(expectedUrl);
    });
  });

  describe('getEnterpriseReportingDashboardsUrl', () => {
    it('returns the correct URL with the given parameter', () => {
      const expectedUrl = `http://localhost/rest/enterpriseReporting/dashboards`;
      expect(clmLocation.getEnterpriseReportingDashboardsUrl()).toEqual(expectedUrl);
    });
  });

  describe('getTelemetryStatusUrl', () => {
    it('returns the correct URL with the given parameter', () => {
      const expectedUrl = `http://localhost/rest/telemetry/status`;
      expect(clmLocation.getTelemetryStatusUrl()).toEqual(expectedUrl);
    });
  });

  describe('getApplicationReportHistoryUrl', () => {
    it('returns the url to get the application report history by application ID or public ID and stage ID', () => {
      const applicationId = 'someApplicationId';
      const stageId = 'someStageId';
      const expectedUrl = `http://localhost/api/v2/reports/applications/${applicationId}/history?stage=${stageId}&limit=20`;

      expect(clmLocation.getApplicationReportHistoryUrl(applicationId, stageId)).toEqual(expectedUrl);
    });

    it('sends the page size the caller asks for', () => {
      const applicationId = 'someApplicationId';
      const stageId = 'someStageId';
      const expectedUrl = `http://localhost/api/v2/reports/applications/${applicationId}/history?stage=${stageId}&limit=5`;

      expect(clmLocation.getApplicationReportHistoryUrl(applicationId, stageId, 5)).toEqual(expectedUrl);
    });
  });

  describe('getRoiConfigurationUrl', () => {
    it('returns the url for the ROI configuration', () => {
      const expectedUrl = 'http://localhost/rest/roiConfiguration';
      expect(clmLocation.getRoiConfigurationUrl()).toEqual(expectedUrl);
    });

    it('returns the url for the ROI configuration with expected currencyType', () => {
      const expectedUrl = 'http://localhost/rest/roiConfiguration/currencyType/usd';
      expect(clmLocation.getRoiConfigurationUrl('usd')).toEqual(expectedUrl);
    });
  });

  describe('getRoiConfigurationRestoreDefaultsUrl', () => {
    it('returns the url for the ROI configuration', () => {
      const expectedUrl = 'http://localhost/rest/roiConfiguration/defaultValues/currencyType/usd';
      expect(clmLocation.getRoiConfigurationRestoreDefaultsUrl('usd')).toEqual(expectedUrl);
    });
  });

  describe('getContainerRepositoryResultsUrl', () => {
    it('returns the url for the container repository results', () => {
      const repositoryId = 'someRepositoryId';
      const expectedUrl = `http://localhost/api/v2/firewall/container-images/repositories/repository/${repositoryId}/results/image-details`;
      expect(clmLocation.getContainerRepositoryResultsUrl(repositoryId)).toEqual(expectedUrl);
    });
  });

  describe('getContainerRepositoryReportSummaryUrl', () => {
    it('should return the correct URL with the given repositoryId', () => {
      const repositoryId = 'repo123';
      const expectedUrl = `http://localhost/rest/firewall/container-images/repositories/${repositoryId}/report/containerImageReportSummary`;
      expect(clmLocation.getContainerRepositoryReportSummaryUrl(repositoryId)).toBe(expectedUrl);
    });
  });

  describe('getDeleteContainerImagePolicyWaiverUrl', () => {
    it('should return correct URL with the given containerImageId', () => {
      const containerImageId = 'containerImage123';
      const expectedUrl = `http://localhost/api/v2/firewall/container-image/${containerImageId}/policyWaiver`;
      expect(clmLocation.getDeleteContainerImagePolicyWaiverUrl(containerImageId)).toBe(expectedUrl);
    });
  });

  describe('getNotificationWebhooksUrl', () => {
    it('returns base URL with no eventType (defaults to POLICY_ALERT on server)', () => {
      expect(clmLocation.getNotificationWebhooksUrl('organization', 'org-1')).toBe(
        'http://localhost/rest/config/webhook/policy/organization/org-1'
      );
    });

    it('appends eventType=FIREWALL_POLICY_ALERT for repository policies (NEXUS-52728)', () => {
      expect(clmLocation.getNotificationWebhooksUrl('repository', 'repo-1', 'FIREWALL_POLICY_ALERT')).toBe(
        'http://localhost/rest/config/webhook/policy/repository/repo-1?eventType=FIREWALL_POLICY_ALERT'
      );
    });

    it('appends eventType=POLICY_ALERT when explicitly provided', () => {
      expect(clmLocation.getNotificationWebhooksUrl('organization', 'org-1', 'POLICY_ALERT')).toBe(
        'http://localhost/rest/config/webhook/policy/organization/org-1?eventType=POLICY_ALERT'
      );
    });
  });

  describe('estate component detail URLs (CLM-43961)', () => {
    it('returns the HDS component details URL', () => {
      expect(clmLocation.getApiV2ComponentDetailsUrl()).toBe(
        'http://localhost/api/v2/components/details'
      );
    });

    it('returns component usage applications and organizations URLs', () => {
      expect(clmLocation.getComponentUsageApplicationsUrl()).toBe(
        'http://localhost/rest/dashboard/components/usage/applications'
      );
      expect(clmLocation.getComponentUsageOrganizationsUrl()).toBe(
        'http://localhost/rest/dashboard/components/usage/organizations'
      );
    });
  });
});
