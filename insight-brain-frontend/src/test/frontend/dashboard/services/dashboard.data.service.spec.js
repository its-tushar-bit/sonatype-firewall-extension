/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import dashboardServicesModule from '../../../../main/frontend/dashboard/services/module';

describe('dashboard.data.service.spec', function() {
  var $httpBackend, dashboardDataService, CLMLocations, createDashboardDataRequestPayloadMock, classyBrewMock;

  beforeEach(angular.mock.module(dashboardServicesModule.name, function ($provide) {
    createDashboardDataRequestPayloadMock = jasmine.createSpy('createDashboardDataRequestPayload');
    createDashboardDataRequestPayloadMock.and.callFake(function(filter) {
      return filter;
    });

    classyBrewMock = jasmine.createSpyObj('ClassyBrew', ['create']);

    $provide.value('createDashboardDataRequestPayload', createDashboardDataRequestPayloadMock);
    $provide.value('ClassyBrew', classyBrewMock);
  }));

  beforeEach(inject(function($injector) {
    $httpBackend = $injector.get('$httpBackend');
    dashboardDataService = $injector.get('dashboard.data.service');
    CLMLocations = $injector.get('CLMLocations');
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation(false);
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('getNewestRisks()', function() {
    it('returns data on success', function() {
      var result, numResults, filter = {filterParam: 'filter value'},
          data = {
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
      $httpBackend.expectPOST(CLMLocations.getNewestRisksUrl(), filter).respond(data);

      dashboardDataService.getNewestRisks(filter, []).then(function(data) {
        result = data.results;
        numResults = data.numResults;
      });

      $httpBackend.flush();
      expect(result[0].hash).toBe('f60e9504841ba867a692');
      expect(result[0].derivedComponentName).toBe('foo : bar');
      expect(result[1].hash).toBe('1249e25aebb15358bedd');
      expect(result[1].derivedComponentName).toBe('Unknown');
      expect(numResults).toBe(2);
    });

    it('translates sortFields', function() {
      var translatedSortFields = ['-AGE', '-THREAT_LEVEL', 'POLICY_NAME', '-COMPONENT_NAME', 'APPLICATION_NAME'];

      dashboardDataService.getNewestRisks({},
          ['-firstOccurrenceTime', '-threatLevel', 'policyName', '-derivedComponentName', 'applicationName']);

      expect(createDashboardDataRequestPayloadMock).toHaveBeenCalledWith(jasmine.any(Object), jasmine.any(Number),
          translatedSortFields);

      $httpBackend.whenPOST(CLMLocations.getNewestRisksUrl()).respond({dashboardResults: [], numResults: 0});
      $httpBackend.flush();
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

    it('returns data on success', function() {
      var originalRisks = [{
            applicationName: 'application1',
            applicationId: 'app1',
            totalApplicationRisk: createRisk(5, 4, 3, 2, 1),
            stages: []
          }, {
            applicationName: 'application2',
            applicationId: 'app2',
            totalApplicationRisk: createRisk(6, 0),
            stages: []
          }],
          filter = {
            filterParam: 'filter value'
          },
          spy = jasmine.createSpy('response');

      $httpBackend.expectPOST(CLMLocations.getApplicationRisksUrl(), filter).respond({
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
      });

      classyBrewMock.create.and.returnValue('classyBrewResult');
      dashboardDataService.getApplicationRisks(filter, []).then(spy);

      $httpBackend.flush();
      expect(classyBrewMock.create).toHaveBeenCalledWith([1, 2, 3, 4, 5, 6]);
      expect(spy).toHaveBeenCalledWith({results: originalRisks, numResults: 2, classyBrew: 'classyBrewResult'});
    });

    it('translates sortFields', function() {
      var translatedSortFields = ['-LOW_RISK', 'SEVERE_RISK', '-MODERATE_RISK', '-CRITICAL_RISK', 'NAME'];

      dashboardDataService.getApplicationRisks({}, [
        '-totalApplicationRisk.lowRisk',
        'totalApplicationRisk.severeRisk',
        '-totalApplicationRisk.moderateRisk',
        '-totalApplicationRisk.criticalRisk',
        'applicationName'
      ]);

      expect(createDashboardDataRequestPayloadMock).toHaveBeenCalledWith(jasmine.any(Object), jasmine.any(Number),
          translatedSortFields);

      $httpBackend.whenPOST(CLMLocations.getApplicationRisksUrl()).respond({dashboardResults: [], numResults: 0});
      $httpBackend.flush();
    });
  });

  describe('getComponentRisks()', function() {
    var components, numResults, filter = {filterParam: 'filter value'};
    it('populates component name', function() {
      var classyBrewResult, data = {
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

      $httpBackend.expectPOST(CLMLocations.getComponentRisksUrl(), filter).respond(data);

      dashboardDataService.getComponentRisks(filter, []).then(function(data) {
        components = data.results;
        numResults = data.numResults;
        classyBrewResult = data.classyBrew;
      });

      classyBrewMock.create.and.returnValue('classyBrewResult');

      $httpBackend.flush();
      expect(components[0].hash).toBe('f60e9504841ba867a692');
      expect(components[0].derivedComponentName).toBe('foo : bar');
      expect(components[1].hash).toBe('1249e25aebb15358bedd');
      expect(components[1].derivedComponentName).toBe('Unknown');

      expect(classyBrewMock.create).toHaveBeenCalledWith([12, 8]);
      expect(classyBrewResult).toEqual('classyBrewResult');
      expect(numResults).toBe(2);
    });

    it('translates sortFields', function() {
      var translatedSortFields = [
        '-NUMBER_OF_AFFECTED_APPS', 'NAME', '-TOTAL_RISK', 'CRITICAL_RISK', '-SEVERE_RISK', 'MODERATE_RISK', 'LOW_RISK'
      ];

      dashboardDataService.getComponentRisks({}, [
        '-affectedApplications',
        'derivedComponentName',
        '-score',
        'scoreCritical',
        '-scoreSevere',
        'scoreModerate',
        'scoreLow'
      ]);

      expect(createDashboardDataRequestPayloadMock).toHaveBeenCalledWith(jasmine.any(Object), jasmine.any(Number),
          translatedSortFields);

      $httpBackend.whenPOST(CLMLocations.getComponentRisksUrl()).respond({dashboardResults: [], numResults: 0});
      $httpBackend.flush();
    });
  });
});
