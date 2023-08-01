/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import {
  getNewestRisksUrl,
  getApplicationRisksUrl,
  getComponentRisksUrl,
  getWaiversUrl,
} from '../../../../main/frontend/util/CLMLocation';
import { waiverMatcherStrategy } from 'MainRoot/util/waiverUtils';

describe('dashboard.data.service.spec', function () {
  let classyBrewSpy, getNewestRisks, getApplicationRisks, getComponentRisks, getWaivers;

  const filter = { filterParam: 'filter value' };

  const expectedRequestPayload = {
    organizationIds: undefined,
    applicationIds: undefined,
    repositoryIds: undefined,
    stageIds: undefined,
    tagIds: undefined,
    policyViolationStates: undefined,
    maxDaysOld: undefined,
    policyThreatLevelRange: undefined,
    expirationDate: undefined,
    pageSize: 100,
    page: 0,
  };

  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  beforeEach(function () {
    classyBrewSpy = jasmine.createSpy('classyBrew').and.returnValue('classyBrew');
    const dashboardDataService = require('inject-loader!../../../../main/frontend/dashboard/services/' +
      'dashboard.data.service')({ '../utils/classybrew.factory': { createClassyBrew: classyBrewSpy } });
    getNewestRisks = dashboardDataService.getNewestRisks;
    getApplicationRisks = dashboardDataService.getApplicationRisks;
    getComponentRisks = dashboardDataService.getComponentRisks;
    getWaivers = dashboardDataService.getWaivers;
  });

  describe('getNewestRisks()', function () {
    it('returns data on success', function (done) {
      const newRisksUrl = getNewestRisksUrl();

      const data = {
        dashboardResults: [
          {
            hash: 'f60e9504841ba867a692',
            displayName: {
              parts: [{ field: 'any', value: 'foo' }, { value: ' : ' }, { field: 'any', value: 'bar' }],
            },
            derivedComponentName: 'foo : bar',
            stageTypeId: 'stage-release',
            firstOccurrenceTime: 123456789,
          },
          {
            hash: '1249e25aebb15358bedd',
            derivedComponentName: 'Unknown',
          },
        ],
        numResults: 2,
      };

      mockAxiosCalls({
        post: {
          [newRisksUrl]: Promise.resolve({ data }),
        },
      });

      getNewestRisks(filter, [], 0).then(function (data) {
        const { results, numResults } = data;

        expect(axios.post).toHaveBeenCalledWith(newRisksUrl, expectedRequestPayload);
        expect(results[0].hash).toBe('f60e9504841ba867a692');
        expect(results[0].derivedComponentName).toBe('foo : bar');
        expect(results[1].hash).toBe('1249e25aebb15358bedd');
        expect(results[1].derivedComponentName).toBe('Unknown');
        expect(numResults).toBe(2);
        done();
      });
    });

    it('translates sortFields', function () {
      const newRisksUrl = getNewestRisksUrl(),
        expectedSortFields = ['-AGE', '-THREAT_LEVEL', 'POLICY_NAME', '-COMPONENT_NAME', 'APPLICATION_NAME'];

      const expectedRequestData = {
        ...expectedRequestPayload,
        orderBy: expectedSortFields.join(','),
      };

      mockAxiosCalls({
        post: {
          [newRisksUrl]: Promise.resolve({
            data: { dashboardResults: [], numResults: 0 },
          }),
        },
      });

      getNewestRisks(
        filter,
        ['-firstOccurrenceTime', '-threatLevel', 'policyName', '-derivedComponentName', 'applicationName'],
        0
      );

      expect(axios.post).toHaveBeenCalledWith(newRisksUrl, expectedRequestData);
    });
  });

  describe('getApplicationRisks()', function () {
    // To be replaced by expectedRequestPayload when CLM-26399 is being done
    const expectedRequestPayloadWithoutPagination = { ...expectedRequestPayload, pageSize: 100 };
    delete expectedRequestPayloadWithoutPagination.page;

    function createRisk(total, critical, severe, moderate, low) {
      return {
        totalRisk: total,
        criticalRisk: critical,
        severeRisk: severe,
        moderateRisk: moderate,
        lowRisk: low,
      };
    }

    const originalRisks = [
      {
        applicationName: 'application1',
        applicationId: 'app1',
        totalApplicationRisk: createRisk(5, 4, 3, 2, 1),
        stages: [],
      },
      {
        applicationName: 'application2',
        applicationId: 'app2',
        totalApplicationRisk: createRisk(6, 0),
        stages: [],
      },
    ];

    it('returns data on success', function (done) {
      const data = {
        dashboardResults: [
          {
            applicationName: 'application1',
            applicationId: 'app1',
            totalApplicationRisk: createRisk(5, 4, 3, 2, 1),
            stages: [],
          },
          {
            applicationName: 'application2',
            applicationId: 'app2',
            totalApplicationRisk: createRisk(6, 0),
            stages: [],
          },
        ],
        numResults: 2,
      };
      const applicationRiskUrl = getApplicationRisksUrl(),
        expectedApplicationSeries = [1, 2, 3, 4, 5, 6];

      mockAxiosCalls({
        post: {
          [applicationRiskUrl]: Promise.resolve({ data }),
        },
      });

      getApplicationRisks(filter, [], 0).then((response) => {
        const { results, numResults, classyBrew } = response;
        expect(axios.post).toHaveBeenCalledWith(applicationRiskUrl, expectedRequestPayloadWithoutPagination);
        expect(results).toEqual(originalRisks);
        expect(numResults).toEqual(2);
        expect(classyBrew).toEqual('classyBrew');
        expect(classyBrewSpy).toHaveBeenCalledWith(expectedApplicationSeries);

        done();
      });
    });

    it('translates sortFields', function () {
      const applicationsRiskUrl = getApplicationRisksUrl(),
        expectedSortFields = ['-LOW_RISK', 'SEVERE_RISK', '-MODERATE_RISK', '-CRITICAL_RISK', 'NAME'];

      const expectedRequestData = {
        ...expectedRequestPayloadWithoutPagination,
        orderBy: expectedSortFields.join(','),
      };

      mockAxiosCalls({
        post: {
          [applicationsRiskUrl]: Promise.resolve({
            data: { dashboardResults: [], numResults: 0 },
          }),
        },
      });

      getApplicationRisks(filter, [
        '-totalApplicationRisk.lowRisk',
        'totalApplicationRisk.severeRisk',
        '-totalApplicationRisk.moderateRisk',
        '-totalApplicationRisk.criticalRisk',
        'applicationName',
      ]);

      expect(axios.post).toHaveBeenCalledWith(applicationsRiskUrl, expectedRequestData);
    });
  });

  describe('getComponentRisks()', function () {
    it('populates component name', function (done) {
      const data = {
        dashboardResults: [
          {
            hash: 'f60e9504841ba867a692',
            displayName: {
              parts: [{ field: 'any', value: 'foo' }, { value: ' : ' }, { field: 'any', value: 'bar' }],
            },
            derivedComponentName: 'foo : bar',
            score: 12,
          },
          {
            hash: '1249e25aebb15358bedd',
            derivedComponentName: 'Unknown',
            scoreSevere: 8,
          },
        ],
        numResults: 2,
      };
      const componentRiskUrl = getComponentRisksUrl(),
        expectedComponentSeries = [12, 8];

      mockAxiosCalls({
        post: {
          [componentRiskUrl]: Promise.resolve({ data }),
        },
      });

      getComponentRisks(filter, [], 0).then(function (data) {
        const { results, numResults, classyBrew } = data;
        expect(axios.post).toHaveBeenCalledWith(componentRiskUrl, { ...expectedRequestPayload, pageSize: 100 });
        expect(results[0].hash).toBe('f60e9504841ba867a692');
        expect(results[0].derivedComponentName).toBe('foo : bar');
        expect(results[1].hash).toBe('1249e25aebb15358bedd');
        expect(results[1].derivedComponentName).toBe('Unknown');
        expect(classyBrew).toEqual('classyBrew');
        expect(classyBrewSpy).toHaveBeenCalledWith(expectedComponentSeries);
        expect(numResults).toBe(2);
        done();
      });
    });

    it('translates sortFields', function () {
      const componentRisksUrl = getComponentRisksUrl(),
        expectedSortFields = [
          '-NUMBER_OF_AFFECTED_APPS',
          'NAME',
          '-TOTAL_RISK',
          'CRITICAL_RISK',
          '-SEVERE_RISK',
          'MODERATE_RISK',
          'LOW_RISK',
        ];

      const expectedRequestData = {
        ...expectedRequestPayload,
        orderBy: expectedSortFields.join(','),
      };

      mockAxiosCalls({
        post: {
          [componentRisksUrl]: Promise.resolve({
            data: { dashboardResults: [], numResults: 0 },
          }),
        },
      });

      getComponentRisks(
        filter,
        [
          '-affectedApplications',
          'derivedComponentName',
          '-score',
          'scoreCritical',
          '-scoreSevere',
          'scoreModerate',
          'scoreLow',
        ],
        0
      );

      expect(axios.post).toHaveBeenCalledWith(componentRisksUrl, expectedRequestData);
    });
  });

  describe('getWaivers()', function () {
    // To be replaced by expectedRequestPayload when CLM-26400 is being done
    const expectedRequestPayloadWithoutPagination = { ...expectedRequestPayload, pageSize: 100 };
    delete expectedRequestPayloadWithoutPagination.page;

    it('populates component name', function (done) {
      const data = {
        dashboardResults: [
          {
            id: '35513cecc0214e0cb0207238dc1fba6e',
            threatLevel: 7,
            policyId: '67a74447c2bf4c53b8e26f93b16ad4ee',
            policyName: 'Component-Similar',
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            componentMatchStrategy: waiverMatcherStrategy.ALL_VERSIONS,
            displayName: 'org.sonatype.nexus : nexus-rest-client',
          },
          {
            id: 'bbb045cb733d4868bd6d30e4384e19f4',
            threatLevel: 9,
            policyId: '358f08a34c7b47739f6962b35b84fbea',
            policyName: 'Security-High',
            ownerId: '79e2b6864a4d4f5fbce461cf930c3f2c',
            ownerName: 'unprotected zip big java app',
            ownerType: 'application',
            componentMatchStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
            displayName: 'commons-beanutils : commons-beanutils : 1.8.3',
          },
        ],
        numResults: 2,
      };
      const waiverDetailsUrl = getWaiversUrl();

      mockAxiosCalls({
        post: {
          [waiverDetailsUrl]: Promise.resolve({ data }),
        },
      });

      getWaivers(filter, []).then(function (data) {
        const { results, numResults } = data;
        expect(axios.post).toHaveBeenCalledWith(waiverDetailsUrl, expectedRequestPayloadWithoutPagination);
        expect(results[0].id).toBe('35513cecc0214e0cb0207238dc1fba6e');
        expect(results[0].displayName).toBe('org.sonatype.nexus : nexus-rest-client');
        expect(results[0].ownerName).toBe('Root Organization');
        expect(results[0].ownerType).toBe('organization');
        expect(results[0].threatLevel).toBe(7);
        expect(results[0].policyName).toBe('Component-Similar');
        expect(results[1].id).toBe('bbb045cb733d4868bd6d30e4384e19f4');
        expect(results[1].displayName).toBe('commons-beanutils : commons-beanutils : 1.8.3');
        expect(results[1].ownerName).toBe('unprotected zip big java app');
        expect(results[1].ownerType).toBe('application');
        expect(results[1].threatLevel).toBe(9);
        expect(results[1].policyName).toBe('Security-High');
        expect(numResults).toBe(2);
        done();
      });
    });

    it('translates sortFields', function () {
      const waiversUrl = getWaiversUrl(),
        expectedSortFields = [
          '-COMPONENT_SCOPE',
          'CREATION_DATE',
          '-EXPIRATION_DATE',
          '-OWNER_SCOPE',
          'POLICY_NAME',
          'THREAT_LEVEL',
        ];

      const expectedRequestData = {
        ...expectedRequestPayloadWithoutPagination,
        orderBy: expectedSortFields.join(','),
      };

      mockAxiosCalls({
        post: {
          [waiversUrl]: Promise.resolve({
            data: { dashboardResults: [], numResults: 0 },
          }),
        },
      });

      getWaivers(filter, ['-component', 'createTime', '-expiryTime', '-scope', 'policyName', 'threatLevel']);

      expect(axios.post).toHaveBeenCalledWith(waiversUrl, expectedRequestData);
    });
  });
});
