/* global inject, beforeEach, afterEach, it, describe, expect, PolicyViolationAggregationResourceMockData */
describe('successMetricsDataService', function() {
  beforeEach(module('successMetricsModule'));

  var $httpBackend,
      successMetricsDataService,
      CLMLocations;

  beforeEach(inject(function(_$httpBackend_, _successMetricsDataService_, _CLMLocations_) {
    $httpBackend = _$httpBackend_;
    successMetricsDataService = _successMetricsDataService_;
    CLMLocations = _CLMLocations_;
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  describe('getAveragesData', function() {

    it('fetches averages data from the backend and merges into into a single record with rounded values', function() {
      var output;

      $httpBackend.expectPOST(CLMLocations.getViolationAveragesUrl()).respond(
          PolicyViolationAggregationResourceMockData.getAverages());

      successMetricsDataService.getAveragesData().then(function(o) {
        output = o;
      });

      $httpBackend.flush();

      expect(output).toBeDefined();
      expect(output.activeApplicationCount).toBe(12);
      expect(output.averageEvaluations).toBe(1.8333333333333333);
      expect(output.averagePolicyViolations).toBe(36);
      expect(output.averageCriticalPolicyViolations).toBe(7);
      expect(output.security).toBeDefined();
      expect(output.security.averageDiscoveredLow).toBe(0);
      expect(output.security.averageDiscoveredModerate).toBe(2);
      expect(output.security.averageDiscoveredSevere).toBe(6);
      expect(output.security.averageDiscoveredCritical).toBe(2);
      expect(output.license).toBeDefined();
      expect(output.license.averageDiscoveredLow).toBe(12);
      expect(output.license.averageDiscoveredModerate).toBe(3);
      expect(output.license.averageDiscoveredSevere).toBe(0);
      expect(output.license.averageDiscoveredCritical).toBe(1);
      expect(output.quality).toBeDefined();
      expect(output.quality.averageDiscoveredLow).toBe(0);
      expect(output.quality.averageDiscoveredModerate).toBe(0);
      expect(output.quality.averageDiscoveredSevere).toBe(0);
      expect(output.quality.averageDiscoveredCritical).toBe(0);
      expect(output.other).toBeDefined();
      expect(output.other.averageDiscoveredLow).toBe(1);
      expect(output.other.averageDiscoveredModerate).toBe(2);
      expect(output.other.averageDiscoveredSevere).toBe(3);
      expect(output.other.averageDiscoveredCritical).toBe(4);
    });

    it('fetches empty averages data properly', function() {
      var output;

      $httpBackend.expectPOST(CLMLocations.getViolationAveragesUrl()).respond(
          PolicyViolationAggregationResourceMockData.getEmptyAverages());

      successMetricsDataService.getAveragesData().then(function(o) {
        output = o;
      });

      $httpBackend.flush();

      expect(output).toBeDefined();
      expect(output.activeApplicationCount).toBe(0);
      expect(output.averageEvaluations).toBe(0);
      expect(output.averagePolicyViolations).toBe(0);
      expect(output.averageCriticalPolicyViolations).toBe(0);
      expect(output.security).toBeDefined();
      expect(output.security.averageDiscoveredLow).toBe(0);
      expect(output.security.averageDiscoveredModerate).toBe(0);
      expect(output.security.averageDiscoveredSevere).toBe(0);
      expect(output.security.averageDiscoveredCritical).toBe(0);
      expect(output.license).toBeDefined();
      expect(output.license.averageDiscoveredLow).toBe(0);
      expect(output.license.averageDiscoveredModerate).toBe(0);
      expect(output.license.averageDiscoveredSevere).toBe(0);
      expect(output.license.averageDiscoveredCritical).toBe(0);
      expect(output.quality).toBeDefined();
      expect(output.quality.averageDiscoveredLow).toBe(0);
      expect(output.quality.averageDiscoveredModerate).toBe(0);
      expect(output.quality.averageDiscoveredSevere).toBe(0);
      expect(output.quality.averageDiscoveredCritical).toBe(0);
      expect(output.other).toBeDefined();
      expect(output.other.averageDiscoveredLow).toBe(0);
      expect(output.other.averageDiscoveredModerate).toBe(0);
      expect(output.other.averageDiscoveredSevere).toBe(0);
      expect(output.other.averageDiscoveredCritical).toBe(0);
    });

    it('passes on a rejected promise', function() {
      var caughtError;

      $httpBackend.expectPOST(CLMLocations.getViolationAveragesUrl()).respond(403, 'Forbidden');

      successMetricsDataService.getAveragesData().catch(function(e) {
        caughtError = e;
      });

      $httpBackend.flush();

      expect(caughtError).toBeDefined();
      expect(caughtError.data).toBe('Forbidden');
      expect(caughtError.status).toBe(403);
    });
  });

  describe('getApplicationCountsData', function() {
    it('fetches the Application Counts URL and returns a Promise of the parsed JSON response', function() {
      var output,
          response = {
            totalApplications: 5,
            activeApplications: 4,
            total: {
              applicationsWithViolations: 3,
              applicationsWithCriticalViolations: 2
            },
            security: {
              applicationsWithViolations: 2,
              applicationsWithCriticalViolations: 2
            },
            license: {
              applicationsWithViolations: 1,
              applicationsWithCriticalViolations: 1
            },
            quality: {
              applicationsWithViolations: 1,
              applicationsWithCriticalViolations: 0
            },
            other: {
              applicationsWithViolations: 0,
              applicationsWithCriticalViolations: 0
            }
          };

      $httpBackend.expectPOST(CLMLocations.getSuccessMetricsApplicationCountsUrl()).respond(response);

      successMetricsDataService.getApplicationCountsData().then(function(o) {
        output = o;
      });

      $httpBackend.flush();

      expect(output).toEqual(response);
    });

    it('passes on a rejected promise', function() {
      var caughtError;

      $httpBackend.expectPOST(CLMLocations.getSuccessMetricsApplicationCountsUrl()).respond(403, 'Forbidden');

      successMetricsDataService.getApplicationCountsData().catch(function(e) {
        caughtError = e;
      });

      $httpBackend.flush();

      expect(caughtError).toBeDefined();
      expect(caughtError.data).toBe('Forbidden');
      expect(caughtError.status).toBe(403);
    });
  });

  describe('getMttrData', function() {

    it('fetches mttr data', function() {
      var output;

      $httpBackend.expectPOST(CLMLocations.getMttrUrl()).respond(
          PolicyViolationAggregationResourceMockData.getMttrData());

      successMetricsDataService.getMttrData().then(function(o) {
        output = o;
      });

      $httpBackend.flush();

      expect(output).toBeDefined();
      expect(output).toEqual(PolicyViolationAggregationResourceMockData.getMttrData());
    });

    it('fetches mttr data and properly pads missing results', function() {
      var output;

      $httpBackend.expectPOST(CLMLocations.getMttrUrl()).respond(
          PolicyViolationAggregationResourceMockData.getPartialMttrData());

      successMetricsDataService.getMttrData().then(function(o) {
        output = o;
      });

      $httpBackend.flush();

      expect(output).toBeDefined();
      expect(output.length).toBe(12);

      var date = new Date("June 1, 2016 00:00:00");

      // first 7 months are padded
      for (var i = 0; i < 7; i++) {
        assertMttrData(output[i], date);
        date.setMonth(date.getMonth() + 1);
      }
      assertMttrData(output[7], date, null, null);
      date.setMonth(date.getMonth() + 1);
      assertMttrData(output[8], date, 1209714, 1209714);
      date.setMonth(date.getMonth() + 1);
      assertMttrData(output[9], date, 484000, 484000);
      date.setMonth(date.getMonth() + 1);
      assertMttrData(output[10], date, null, null);
      date.setMonth(date.getMonth() + 1);
      assertMttrData(output[11], date, null, null);
    });

    it('fetches empty mttr data and properly pads missing results', function() {
      var output;

      $httpBackend.expectPOST(CLMLocations.getMttrUrl()).respond([]);

      successMetricsDataService.getMttrData().then(function(o) {
        output = o;
      });

      $httpBackend.flush();
      var date = new Date();
      date.setDate(1); // set to the first to avoid wrapping

      expect(output).toBeDefined();
      expect(output.length).toBe(12);
      for (var i = 0; i < output.length; i++) {
        assertMttrData(output[i], date);
        date.setMonth(date.getMonth() + 1);
      }
    });

    it('passes on a rejected promise', function() {
      var caughtError;

      $httpBackend.expectPOST(CLMLocations.getMttrUrl()).respond(403, 'Forbidden');

      successMetricsDataService.getMttrData().catch(function(e) {
        caughtError = e;
      });

      $httpBackend.flush();

      expect(caughtError).toBeDefined();
      expect(caughtError.data).toBe('Forbidden');
      expect(caughtError.status).toBe(403);
    });

    function assertMttrData(mttr, expectedDate, expectedMttrInSeconds, expectedCriticalMttrInSeconds)
    {
      expect(new Date(mttr.timePeriodStart).getMonth()).toBe(expectedDate.getMonth());
      expect(mttr.mttrInSeconds).toBe(expectedMttrInSeconds);
      expect(mttr.criticalMttrInSeconds).toBe(expectedCriticalMttrInSeconds);
    }
  });

  describe('getComponentCountsData', function() {

    it('fetches component counts data', function() {
      var output;

      $httpBackend.expectPOST(CLMLocations.getSuccessMetricsComponentCountsUrl()).respond(
          PolicyViolationAggregationResourceMockData.getComponentCountsData());

      successMetricsDataService.getComponentCountsData().then(function(o) {
        output = o;
      });

      $httpBackend.flush();

      expect(output).toBeDefined();
      expect(output).toEqual(PolicyViolationAggregationResourceMockData.getComponentCountsData());
    });

    it('fetches component counts data and properly pads missing results', function() {
      var output;

      $httpBackend.expectPOST(CLMLocations.getSuccessMetricsComponentCountsUrl()).respond(
          PolicyViolationAggregationResourceMockData.getPartialComponentCountsData());

      successMetricsDataService.getComponentCountsData().then(function(o) {
        output = o;
      });

      $httpBackend.flush();

      expect(output).toBeDefined();
      expect(output.componentsPerApplication).toBe(32);
      expect(output.componentsInTheMostApplications.length).toBe(5);
      expect(output.componentsWithTheMostViolations.length).toBe(5);

      assertComponentData(output.componentsInTheMostApplications[0], 1);
      assertComponentData(output.componentsWithTheMostViolations[0], 1);
      assertComponentData(output.componentsInTheMostApplications[1], 1);
      assertComponentData(output.componentsWithTheMostViolations[1], 1);
      assertComponentData(output.componentsInTheMostApplications[2], 1);
      assertComponentData(output.componentsWithTheMostViolations[2], 1);

      // last 2 components are padded
      for (var i = 4; i > 2; i--) {
        assertComponentData(output.componentsInTheMostApplications[i], 0);
        assertComponentData(output.componentsWithTheMostViolations[i], 0);
      }
    });

    it('fetches empty component counts data and does not pad missing results', function() {
      var output;

      $httpBackend.expectPOST(CLMLocations.getSuccessMetricsComponentCountsUrl()).respond({
        componentsPerApplication: 0,
        componentsInTheMostApplications: [],
        componentsWithTheMostViolations: []
      });

      successMetricsDataService.getComponentCountsData().then(function(o) {
        output = o;
      });

      $httpBackend.flush();

      expect(output).toBeDefined();
      expect(output.componentsPerApplication).toBe(0);
      expect(output.componentsInTheMostApplications.length).toBe(0);
      expect(output.componentsWithTheMostViolations.length).toBe(0);
    });

    it('passes on a rejected promise', function() {
      var caughtError;

      $httpBackend.expectPOST(CLMLocations.getSuccessMetricsComponentCountsUrl()).respond(403, 'Forbidden');

      successMetricsDataService.getComponentCountsData().catch(function(e) {
        caughtError = e;
      });

      $httpBackend.flush();

      expect(caughtError).toBeDefined();
      expect(caughtError.data).toBe('Forbidden');
      expect(caughtError.status).toBe(403);
    });

    function assertComponentData(componentData, expectedCount) {
      expect(componentData.count).toBe(expectedCount);
    }
  });
});
