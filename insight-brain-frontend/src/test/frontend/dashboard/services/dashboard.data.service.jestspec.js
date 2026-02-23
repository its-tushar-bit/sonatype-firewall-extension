/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  getNewestRisksUrl,
  getApplicationRisksUrl,
  getComponentRisksUrl,
  getWaiversUrl,
  getWaiversAndAutoWaiversUrl,
} from 'MainRoot/util/CLMLocation';
import { waiverMatcherStrategy } from 'MainRoot/util/waiverUtils';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';

// Mock the classybrew factory
jest.mock('MainRoot/dashboard/utils/classybrew.factory', () => ({
  createClassyBrew: jest.fn(),
}));

import { createClassyBrew } from 'MainRoot/dashboard/utils/classybrew.factory';
import {
  getNewestRisks,
  getApplicationRisks,
  getComponentRisks,
  getWaivers,
  getWaiversAndAutoWaivers,
} from 'MainRoot/dashboard/services/dashboard.data.service';

describe('dashboard.data.service.spec', () => {
  let axiosMock;

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
    policyWaiverReasonIds: undefined,
    pageSize: 100,
    page: 0,
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    jest.clearAllMocks();
    createClassyBrew.mockReturnValue('classyBrew');
  });

  describe('getNewestRisks()', () => {
    it('returns data on success', async () => {
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
      };

      axiosMock.onPost(newRisksUrl).reply(200, data);

      const response = await getNewestRisks(filter, [], 0);
      const { results } = response;

      expect(axiosMock.history.post).toHaveLength(1);
      expect(axiosMock.history.post[0].url).toBe(newRisksUrl);
      expect(JSON.parse(axiosMock.history.post[0].data)).toEqual(expectedRequestPayload);
      expect(results[0].hash).toBe('f60e9504841ba867a692');
      expect(results[0].derivedComponentName).toBe('foo : bar');
      expect(results[1].hash).toBe('1249e25aebb15358bedd');
      expect(results[1].derivedComponentName).toBe('Unknown');
    });

    it('translates sortFields', async () => {
      const newRisksUrl = getNewestRisksUrl();
      const expectedSortFields = ['-AGE', '-THREAT_LEVEL', 'POLICY_NAME', '-COMPONENT_NAME', 'APPLICATION_NAME'];

      const expectedRequestData = {
        ...expectedRequestPayload,
        orderBy: expectedSortFields.join(','),
      };

      axiosMock.onPost(newRisksUrl).reply(200, { dashboardResults: [] });

      await getNewestRisks(
        filter,
        ['-firstOccurrenceTime', '-threatLevel', 'policyName', '-derivedComponentName', 'applicationName'],
        0
      );

      expect(axiosMock.history.post).toHaveLength(1);
      expect(axiosMock.history.post[0].url).toBe(newRisksUrl);
      expect(JSON.parse(axiosMock.history.post[0].data)).toEqual(expectedRequestData);
    });
  });

  describe('getApplicationRisks()', () => {
    const createRisk = (total, critical, severe, moderate, low) => {
      return {
        totalRisk: total,
        criticalRisk: critical,
        severeRisk: severe,
        moderateRisk: moderate,
        lowRisk: low,
      };
    };

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

    it('returns data on success', async () => {
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
      };
      const applicationRiskUrl = getApplicationRisksUrl();
      const expectedApplicationSeries = [1, 2, 3, 4, 5, 6];

      axiosMock.onPost(applicationRiskUrl).reply(200, data);

      const response = await getApplicationRisks(filter, [], 0);
      const { results, classyBrew } = response;

      expect(axiosMock.history.post).toHaveLength(1);
      expect(axiosMock.history.post[0].url).toBe(applicationRiskUrl);
      expect(JSON.parse(axiosMock.history.post[0].data)).toEqual(expectedRequestPayload);
      expect(results).toEqual(originalRisks);
      expect(classyBrew).toEqual('classyBrew');
      expect(createClassyBrew).toHaveBeenCalledWith(expectedApplicationSeries);
    });

    it('translates sortFields', async () => {
      const applicationsRiskUrl = getApplicationRisksUrl();
      const expectedSortFields = ['-LOW_RISK', 'SEVERE_RISK', '-MODERATE_RISK', '-CRITICAL_RISK', 'NAME'];

      const expectedRequestData = {
        ...expectedRequestPayload,
        orderBy: expectedSortFields.join(','),
      };

      axiosMock.onPost(applicationsRiskUrl).reply(200, { dashboardResults: [] });

      await getApplicationRisks(
        filter,
        [
          '-totalApplicationRisk.lowRisk',
          'totalApplicationRisk.severeRisk',
          '-totalApplicationRisk.moderateRisk',
          '-totalApplicationRisk.criticalRisk',
          'applicationName',
        ],
        0
      );

      expect(axiosMock.history.post).toHaveLength(1);
      expect(axiosMock.history.post[0].url).toBe(applicationsRiskUrl);
      expect(JSON.parse(axiosMock.history.post[0].data)).toEqual(expectedRequestData);
    });
  });

  describe('getComponentRisks()', () => {
    it('populates component name', async () => {
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
      };
      const componentRiskUrl = getComponentRisksUrl();
      const expectedComponentSeries = [12, 8];

      axiosMock.onPost(componentRiskUrl).reply(200, data);

      const response = await getComponentRisks(filter, [], 0);
      const { results, classyBrew } = response;

      expect(axiosMock.history.post).toHaveLength(1);
      expect(axiosMock.history.post[0].url).toBe(componentRiskUrl);
      expect(JSON.parse(axiosMock.history.post[0].data)).toEqual({ ...expectedRequestPayload, pageSize: 100 });
      expect(results[0].hash).toBe('f60e9504841ba867a692');
      expect(results[0].derivedComponentName).toBe('foo : bar');
      expect(results[1].hash).toBe('1249e25aebb15358bedd');
      expect(results[1].derivedComponentName).toBe('Unknown');
      expect(classyBrew).toEqual('classyBrew');
      expect(createClassyBrew).toHaveBeenCalledWith(expectedComponentSeries);
    });

    it('translates sortFields', async () => {
      const componentRisksUrl = getComponentRisksUrl();
      const expectedSortFields = [
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

      axiosMock.onPost(componentRisksUrl).reply(200, { dashboardResults: [] });

      await getComponentRisks(
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

      expect(axiosMock.history.post).toHaveLength(1);
      expect(axiosMock.history.post[0].url).toBe(componentRisksUrl);
      expect(JSON.parse(axiosMock.history.post[0].data)).toEqual(expectedRequestData);
    });
  });

  describe('getWaivers()', () => {
    it('populates component name', async () => {
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
      };
      const waiverDetailsUrl = getWaiversUrl();

      axiosMock.onPost(waiverDetailsUrl).reply(200, data);

      const response = await getWaivers(filter, [], 0);
      const { results } = response;

      expect(axiosMock.history.post).toHaveLength(1);
      expect(axiosMock.history.post[0].url).toBe(waiverDetailsUrl);
      expect(JSON.parse(axiosMock.history.post[0].data)).toEqual({ ...expectedRequestPayload, pageSize: 100 });
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
    });

    it('translates sortFields', async () => {
      const waiversUrl = getWaiversUrl();
      const expectedSortFields = [
        '-COMPONENT_SCOPE',
        'CREATION_DATE',
        '-EXPIRATION_DATE',
        '-OWNER_SCOPE',
        'POLICY_NAME',
        'THREAT_LEVEL',
      ];

      const expectedRequestData = {
        ...expectedRequestPayload,
        orderBy: expectedSortFields.join(','),
      };

      axiosMock.onPost(waiversUrl).reply(200, { dashboardResults: [] });

      await getWaivers(filter, ['-component', 'createTime', '-expiryTime', '-scope', 'policyName', 'threatLevel'], 0);

      expect(axiosMock.history.post).toHaveLength(1);
      expect(axiosMock.history.post[0].url).toBe(waiversUrl);
      expect(JSON.parse(axiosMock.history.post[0].data)).toEqual(expectedRequestData);
    });
  });

  describe('getWaiversAndAutoWaivers()', () => {
    it('populates component name', async () => {
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
      };
      const autoWaiversUrl = getWaiversAndAutoWaiversUrl();

      axiosMock.onPost(autoWaiversUrl).reply(200, data);

      const response = await getWaiversAndAutoWaivers(filter, [], 0);
      const { results } = response;

      expect(axiosMock.history.post).toHaveLength(1);
      expect(axiosMock.history.post[0].url).toBe(autoWaiversUrl);
      expect(JSON.parse(axiosMock.history.post[0].data)).toEqual({ ...expectedRequestPayload, pageSize: 100 });
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
    });

    it('translates sortFields', async () => {
      const autoWaiversUrl = getWaiversAndAutoWaiversUrl();
      const expectedSortFields = [
        '-COMPONENT_SCOPE',
        'CREATION_DATE',
        '-EXPIRATION_DATE',
        '-OWNER_SCOPE',
        'POLICY_NAME',
        'THREAT_LEVEL',
      ];

      const expectedRequestData = {
        ...expectedRequestPayload,
        orderBy: expectedSortFields.join(','),
      };

      axiosMock.onPost(autoWaiversUrl).reply(200, { dashboardResults: [] });

      await getWaiversAndAutoWaivers(
        filter,
        ['-component', 'createTime', '-expiryTime', '-scope', 'policyName', 'threatLevel'],
        0
      );

      expect(axiosMock.history.post).toHaveLength(1);
      expect(axiosMock.history.post[0].url).toBe(autoWaiversUrl);
      expect(JSON.parse(axiosMock.history.post[0].data)).toEqual(expectedRequestData);
    });
  });
});
