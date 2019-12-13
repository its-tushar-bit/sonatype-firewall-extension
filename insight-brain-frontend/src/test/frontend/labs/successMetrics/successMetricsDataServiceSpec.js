/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global inject, beforeEach, afterEach, it, describe, expect */
import successMetricsModule from '../../../../main/frontend/labs/successMetrics/module';
import PolicyViolationAggregationResourceMockData from './mock.data/policy.violation.aggregation.mock.data';

describe('successMetricsDataService', function() {
  var $httpBackend,
      successMetricsDataService,
      CLMLocations,
      $timeout,
      $q,
      applicationStoreDeferred,
      mockApplicationStore = {
        get: function() {
          return applicationStoreDeferred.promise;
        }
      };

  beforeEach(angular.mock.module(successMetricsModule.name));

  beforeEach(angular.mock.module(function($provide) {
    $provide.value('ApplicationStore', mockApplicationStore);
  }));

  beforeEach(inject(function(_$q_, _$timeout_, _$httpBackend_, _successMetricsDataService_, _CLMLocations_) {
    $httpBackend = _$httpBackend_;
    successMetricsDataService = _successMetricsDataService_;
    CLMLocations = _CLMLocations_;
    $q = _$q_;
    $timeout = _$timeout_;

    applicationStoreDeferred = $q.defer();
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  describe('getChartData', function() {

    it('fetches the lastUpdated and monthCount values from the backend', function() {
      var output,
          reportId = '1234',
          includeLatestData = true,
          serviceParams = { id: reportId, includeLatestData: includeLatestData };

      $httpBackend.expectGET(CLMLocations.getSuccessMetricsChartDataUrl(reportId, includeLatestData)).respond(
          PolicyViolationAggregationResourceMockData.getFullChartData());

      successMetricsDataService.getChartData(serviceParams).then(function(o) { output = o; });

      $httpBackend.flush();

      expect(output.monthCount).toBe(11);
      expect(output.lastUpdated).toBe(1507218887089);
    });

    it('fetches averages data from the backend', function() {
      var output,
          reportId = '1234',
          includeLatestData = true,
          serviceParams = { id: reportId, includeLatestData: includeLatestData },
          mockData = PolicyViolationAggregationResourceMockData.getFullChartData();

      $httpBackend.expectGET(CLMLocations.getSuccessMetricsChartDataUrl(reportId, includeLatestData)).respond(mockData);

      successMetricsDataService.getChartData(serviceParams).then(function(o) {
        output = o.averagesData;
      });

      $httpBackend.flush();

      expect(output).toEqual(mockData.averages);
    });

    it('fetches application counts data', function() {
      var output,
          reportId = '1234',
          includeLatestData = true,
          serviceParams = { id: reportId, includeLatestData: includeLatestData },
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

      $httpBackend.expectGET(CLMLocations.getSuccessMetricsChartDataUrl(reportId, includeLatestData)).respond({
        mttrs: [],
        averages: PolicyViolationAggregationResourceMockData.getEmptyAverages(),
        applicationCounts: response
      });

      successMetricsDataService.getChartData(serviceParams).then(function(o) {
        output = o.applicationCountsData;
      });

      $httpBackend.flush();

      expect(output).toEqual(response);
    });

    it('fetches mttr data', function() {
      var output,
          reportId = '1234',
          includeLatestData = true,
          serviceParams = { id: reportId, includeLatestData: includeLatestData };

      $httpBackend.expectGET(CLMLocations.getSuccessMetricsChartDataUrl(reportId, includeLatestData)).respond(
          PolicyViolationAggregationResourceMockData.getFullChartData());

      successMetricsDataService.getChartData(serviceParams).then(function(o) {
        output = o.mttrData;
      });

      $httpBackend.flush();

      expect(output).toBeDefined();
      expect(output).toEqual(PolicyViolationAggregationResourceMockData.getMttrData());
    });

    it('fetches mttr data and properly pads missing results', function() {
      var output,
          reportId = '1234',
          includeLatestData = true,
          serviceParams = { id: reportId, includeLatestData: includeLatestData };

      $httpBackend.expectGET(CLMLocations.getSuccessMetricsChartDataUrl(reportId, includeLatestData)).respond(
          PolicyViolationAggregationResourceMockData.getPartialChartData());

      successMetricsDataService.getChartData(serviceParams).then(function(o) {
        output = o.mttrData;
      });

      $httpBackend.flush();

      expect(output).toBeDefined();
      expect(output.length).toBe(12);

      assertMttrData(output[0], 'Apr');
      assertMttrData(output[1], 'May');
      assertMttrData(output[2], 'Jun');
      assertMttrData(output[3], 'Jul');
      assertMttrData(output[4], 'Aug');
      assertMttrData(output[5], 'Sep');
      assertMttrData(output[6], 'Oct');
      assertMttrData(output[7], 'Nov', null, null);
      assertMttrData(output[8], 'Dec', 1209714, 1209714);
      assertMttrData(output[9], 'Jan', 484000, 484000);
      assertMttrData(output[10], 'Feb', null, null);
      assertMttrData(output[11], 'Mar', null, null);
    });

    it('fetches empty mttr data does not pad it', function() {
      var output,
          reportId = '1234',
          includeLatestData = true,
          serviceParams = { id: reportId, includeLatestData: includeLatestData };

      $httpBackend.expectGET(CLMLocations.getSuccessMetricsChartDataUrl(reportId, includeLatestData)).respond({
        averages: PolicyViolationAggregationResourceMockData.getEmptyAverages(),
        mttrs: []
      });

      successMetricsDataService.getChartData(serviceParams).then(function(o) {
        output = o.mttrData;
      });

      $httpBackend.flush();
      var date = new Date();
      date.setDate(1); // set to the first to avoid wrapping

      expect(output).toBeDefined();
      expect(output.length).toBe(0);
    });

    it('passes on a rejected promise', function() {
      var caughtError,
          reportId = '1234',
          includeLatestData = true,
          serviceParams = { id: reportId, includeLatestData: includeLatestData };

      $httpBackend.expectGET(CLMLocations.getSuccessMetricsChartDataUrl(reportId, includeLatestData))
          .respond(403, 'Forbidden');

      successMetricsDataService.getChartData(serviceParams).catch(function(e) {
        caughtError = e;
      });

      $httpBackend.flush();

      expect(caughtError).toBeDefined();
      expect(caughtError.data).toBe('Forbidden');
      expect(caughtError.status).toBe(403);
    });

    function assertMttrData(mttr, expectedMonth, expectedMttrInSeconds, expectedCriticalMttrInSeconds) {
      expect(mttr.timePeriodName).toBe(expectedMonth);
      expect(mttr.mttrInSeconds).toBe(expectedMttrInSeconds);
      expect(mttr.criticalMttrInSeconds).toBe(expectedCriticalMttrInSeconds);
    }
  });

  describe('getComponentCountsData', function() {

    it('fetches component counts data', function() {
      var output,
          reportId = '1234',
          serviceParams = { id: reportId };

      $httpBackend.expectGET(CLMLocations.getSuccessMetricsComponentCountsUrl(reportId)).respond(
          PolicyViolationAggregationResourceMockData.getComponentCountsData());

      successMetricsDataService.getComponentCountsData(serviceParams).then(function(o) {
        output = o;
      });

      $httpBackend.flush();

      expect(output).toBeDefined();
      expect(output).toEqual(PolicyViolationAggregationResourceMockData.getComponentCountsData());
    });

    it('fetches component counts data and properly pads missing results', function() {
      var output,
          reportId = '1234',
          serviceParams = { id: reportId };

      $httpBackend.expectGET(CLMLocations.getSuccessMetricsComponentCountsUrl(reportId)).respond(
          PolicyViolationAggregationResourceMockData.getPartialComponentCountsData());

      successMetricsDataService.getComponentCountsData(serviceParams).then(function(o) {
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
      var output,
          reportId = '1234',
          serviceParams = { id: reportId };

      $httpBackend.expectGET(CLMLocations.getSuccessMetricsComponentCountsUrl(reportId)).respond({
        componentsPerApplication: 0,
        componentsInTheMostApplications: [],
        componentsWithTheMostViolations: []
      });

      successMetricsDataService.getComponentCountsData(serviceParams).then(function(o) {
        output = o;
      });

      $httpBackend.flush();

      expect(output).toBeDefined();
      expect(output.componentsPerApplication).toBe(0);
      expect(output.componentsInTheMostApplications.length).toBe(0);
      expect(output.componentsWithTheMostViolations.length).toBe(0);
    });

    it('passes on a rejected promise', function() {
      var caughtError,
          reportId = '1234',
          serviceParams = { id: reportId };

      $httpBackend.expectGET(CLMLocations.getSuccessMetricsComponentCountsUrl(reportId)).respond(403, 'Forbidden');

      successMetricsDataService.getComponentCountsData(serviceParams).catch(function(e) {
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

  describe('getApplicationByInternalId', function() {
    it('fetches application properly', function() {
      var output;
      var applications = [{id: 'app1', name: 'app 1'}, {id: 'app2', name: 'app 2'}];

      applicationStoreDeferred.resolve(applications);
      successMetricsDataService.getApplicationByInternalId('app1').then(function(result) {
        output = result;
      });

      $timeout.flush();

      expect(output).toBeDefined();
      expect(output.id).toBe('app1');
      expect(output.name).toBe('app 1');
    });

    it('rejects promise if application not found', function() {
      var caughtError;
      var applications = [{id: 'app2', name: 'app 2'}];

      applicationStoreDeferred.resolve(applications);
      successMetricsDataService.getApplicationByInternalId('app1').catch(function(e) {
        caughtError = e;
      });

      $timeout.flush();

      expect(caughtError).toBeDefined();
      expect(caughtError).toBe('Could not find Application with internal id app1');
    });
  });
});
