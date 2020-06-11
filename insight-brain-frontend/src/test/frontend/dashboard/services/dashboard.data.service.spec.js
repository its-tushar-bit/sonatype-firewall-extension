/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import {
  getNewestRisksUrl,
  getApplicationRisksUrl,
  getComponentRisksUrl
} from '../../../../main/frontend/util/CLMLocation';

describe('dashboard.data.service.spec', function() {
  let classyBrewSpy, getNewestRisks, getApplicationRisks, getComponentRisks;

  const filter = { filterParam: 'filter value' };

  const expectedRequestPayload = {
    maxResults: 101,
    organizationIds: undefined,
    applicationIds: undefined,
    stageIds: undefined,
    tagIds: undefined,
    policyViolationStates: undefined,
    maxDaysOld: undefined,
    policyThreatLevelRange: undefined
  };

  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  beforeEach(function() {
    classyBrewSpy = jasmine.createSpy('classyBrew').and.returnValue('classyBrew');
    const dashboardDataService =
      require('inject-loader!../../../../main/frontend/dashboard/services/dashboard.data.service')({
        '../utils/classybrew.factory': {
          createClassyBrew: classyBrewSpy
        }
      });
    getNewestRisks = dashboardDataService.getNewestRisks;
    getApplicationRisks = dashboardDataService.getApplicationRisks;
    getComponentRisks = dashboardDataService.getComponentRisks;
  });

  describe('getNewestRisks()', function() {
    it('returns data on success', function(done) {
      const newRisksUrl = getNewestRisksUrl();

      const data = {
        dashboardResults: [
          {
            hash: 'f60e9504841ba867a692',
            displayName: {
              parts: [
                {field: 'any', value: 'foo'},
                {value: ' : '},
                {field: 'any', value: 'bar'}
              ]
            },
            derivedComponentName: 'foo : bar',
            stageTypeId: 'stage-release',
            firstOccurrenceTime: 123456789
          },
          {
            hash: '1249e25aebb15358bedd',
            derivedComponentName: 'Unknown'
          }
        ],
        numResults: 2
      };

      mockAxiosCalls({
        post: {
          [newRisksUrl]: Promise.resolve({ data })
        }
      });

      getNewestRisks(filter, [])
          .then(function(data) {
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

    it('translates sortFields', function() {
      const newRisksUrl = getNewestRisksUrl(),
          expectedSortFields = ['-AGE', '-THREAT_LEVEL', 'POLICY_NAME', '-COMPONENT_NAME', 'APPLICATION_NAME'];

      const expectedRequestData = {
        ...expectedRequestPayload,
        orderBy: expectedSortFields.join(',')
      };

      mockAxiosCalls({
        post: {
          [newRisksUrl]: Promise.resolve({})
        }
      });

      getNewestRisks(filter,
          ['-firstOccurrenceTime', '-threatLevel', 'policyName', '-derivedComponentName', 'applicationName']);

      expect(axios.post).toHaveBeenCalledWith(newRisksUrl, expectedRequestData);
    });
  });

  describe('getApplicationRisks()', function() {
    function createRisk(total, critical, severe, moderate, low) {
      return {
        totalRisk: total,
        criticalRisk: critical,
        severeRisk: severe,
        moderateRisk: moderate,
        lowRisk: low
      };
    }

    const originalRisks = [
      {
        applicationName: 'application1',
        applicationId: 'app1',
        totalApplicationRisk: createRisk(5, 4, 3, 2, 1),
        stages: []
      }, {
        applicationName: 'application2',
        applicationId: 'app2',
        totalApplicationRisk: createRisk(6, 0),
        stages: []
      }];

    it('returns data on success', function(done) {
      const data = {
        dashboardResults: [
          {
            applicationName: 'application1',
            applicationId: 'app1',
            totalApplicationRisk: createRisk(5, 4, 3, 2, 1),
            stages: []
          }, {
            applicationName: 'application2',
            applicationId: 'app2',
            totalApplicationRisk: createRisk(6, 0),
            stages: []
          }
        ],
        numResults: 2
      };
      const applicationRiskUrl = getApplicationRisksUrl(),
          expectedApplicationSeries = [1, 2, 3, 4, 5, 6];

      mockAxiosCalls({
        post: {
          [applicationRiskUrl]: Promise.resolve({ data })
        }
      });

      getApplicationRisks(filter, [])
          .then((response) => {
            const { results, numResults, classyBrew } = response;
            expect(axios.post).toHaveBeenCalledWith(applicationRiskUrl, expectedRequestPayload);
            expect(results).toEqual(originalRisks);
            expect(numResults).toEqual(2);
            expect(classyBrew).toEqual('classyBrew');
            expect(classyBrewSpy)
                .toHaveBeenCalledWith(expectedApplicationSeries);

            done();
          });
    });

    it('translates sortFields', function() {
      const applicationsRiskUrl = getApplicationRisksUrl(),
          expectedSortFields = ['-LOW_RISK', 'SEVERE_RISK', '-MODERATE_RISK', '-CRITICAL_RISK', 'NAME'];

      const expectedRequestData = {
        ...expectedRequestPayload,
        orderBy: expectedSortFields.join(',')
      };

      mockAxiosCalls({
        post: {
          [applicationsRiskUrl]: Promise.resolve({})
        }
      });

      getApplicationRisks(filter, [
        '-totalApplicationRisk.lowRisk',
        'totalApplicationRisk.severeRisk',
        '-totalApplicationRisk.moderateRisk',
        '-totalApplicationRisk.criticalRisk',
        'applicationName'
      ]);

      expect(axios.post).toHaveBeenCalledWith(applicationsRiskUrl, expectedRequestData);
    });
  });

  describe('getComponentRisks()', function() {
    it('populates component name', function(done) {
      const data = {
        dashboardResults: [
          {
            hash: 'f60e9504841ba867a692',
            displayName: {
              parts: [
                {field: 'any', value: 'foo'},
                {value: ' : '},
                {field: 'any', value: 'bar'}
              ]
            },
            derivedComponentName: 'foo : bar',
            score: 12
          },
          {
            hash: '1249e25aebb15358bedd',
            derivedComponentName: 'Unknown',
            scoreSevere: 8
          }
        ],
        numResults: 2
      };
      const componentRiskUrl = getComponentRisksUrl(),
          expectedComponentSeries = [12, 8];

      mockAxiosCalls({
        post: {
          [componentRiskUrl]: Promise.resolve({ data })
        }
      });

      getComponentRisks(filter, [])
          .then(function(data) {
            const { results, numResults, classyBrew } = data;
            expect(axios.post).toHaveBeenCalledWith(componentRiskUrl, expectedRequestPayload);
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

    it('translates sortFields', function() {
      const componentRisksUrl = getComponentRisksUrl(),
          expectedSortFields = [
            '-NUMBER_OF_AFFECTED_APPS',
            'NAME',
            '-TOTAL_RISK',
            'CRITICAL_RISK',
            '-SEVERE_RISK',
            'MODERATE_RISK',
            'LOW_RISK'
          ];

      const expectedRequestData = {
        ...expectedRequestPayload,
        orderBy: expectedSortFields.join(',')
      };

      mockAxiosCalls({
        post: {
          [componentRisksUrl]: Promise.resolve({})
        }
      });

      getComponentRisks(filter, [
        '-affectedApplications',
        'derivedComponentName',
        '-score',
        'scoreCritical',
        '-scoreSevere',
        'scoreModerate',
        'scoreLow'
      ]);

      expect(axios.post).toHaveBeenCalledWith(componentRisksUrl, expectedRequestData);
    });
  });
});
