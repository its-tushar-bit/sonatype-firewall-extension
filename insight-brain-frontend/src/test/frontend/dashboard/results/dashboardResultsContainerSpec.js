/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import dashboardResultsModule from '../../../../main/frontend/dashboard/results/module';

describe('dashboardResultsContainer', function() {
  var vm, CLMLocations;

  beforeEach(angular.mock.module(dashboardResultsModule.name, function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function($componentController, _CLMLocations_) {
    CLMLocations = _CLMLocations_;

    vm = $componentController('dashboardResultsContainer');
    vm.$onInit();
  }));

  describe('getExportUrl()', function() {
    it('throws error when state is not one of the dashboard views', function() {
      vm.routeStateName = 'Foo';
      expect(vm.getExportUrl).toThrowError('Export is not supported for state Foo');
    });

    it('uses violations export URL when on violations view', function() {
      vm.routeStateName = 'dashboard.overview.violations';
      expect(vm.getExportUrl()).toBe(CLMLocations.getNewestRisksExportUrl());
    });

    it('uses components export URL when on components view', function() {
      vm.routeStateName = 'dashboard.overview.components';
      expect(vm.getExportUrl()).toBe(CLMLocations.getComponentRisksExportUrl());
    });

    it('uses applications export URL when on applications view', function() {
      vm.routeStateName = 'dashboard.overview.applications';
      expect(vm.getExportUrl()).toBe(CLMLocations.getApplicationRisksExportUrl());
    });
  });

  describe('getExportRequestJson()', function() {

    beforeEach(function() {

      vm.filters = {
        organizations: new Set(['org1']),
        applications: new Set(['app1', 'app2']),
        categories: new Set(),
        stages: new Set(),
        policyTypes: new Set(),
        policyThreatLevels: [3, 8]
      };
    });

    it('throws error when state is not one of the dashboard views', function() {
      vm.routeStateName = 'Foo';
      expect(vm.getExportRequestJson).toThrowError('Export is not supported for state Foo');
    });

    it('converts filters to json string with default violations sortFields', function() {
      vm.routeStateName = 'dashboard.overview.violations';
      vm.violationsSortFields = ['-firstOccurrenceTime', '-threatLevel'];
      var json = '{"orderBy":"-AGE,-THREAT_LEVEL","organizationIds":["org1"],"applicationIds":["app1","app2"],' +
          '"stageIds":[],"tagIds":[],"policyThreatLevelRange":"3,8"}';
      expect(vm.getExportRequestJson()).toBe(json);
    });

    it('converts filters to json string with default components sortFields', function() {
      vm.routeStateName = 'dashboard.overview.components';
      vm.componentsSortFields = ['-score'];
      var json = '{"orderBy":"-TOTAL_RISK","organizationIds":["org1"],"applicationIds":["app1","app2"],' +
          '"stageIds":[],"tagIds":[],"policyThreatLevelRange":"3,8"}';
      expect(vm.getExportRequestJson()).toBe(json);
    });

    it('converts filters to json string with default applications sortFields', function() {
      vm.routeStateName = 'dashboard.overview.applications';
      vm.applicationsSortFields = ['-totalApplicationRisk.totalRisk'];
      var json = '{"orderBy":"-TOTAL_RISK","organizationIds":["org1"],"applicationIds":["app1","app2"],' +
          '"stageIds":[],"tagIds":[],"policyThreatLevelRange":"3,8"}';
      expect(vm.getExportRequestJson()).toBe(json);
    });
  });

  describe('$onDestroy()', function() {
    it('unsubscribes from redux store', function() {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });
});
