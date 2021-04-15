/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import successMetricsModule from '../../../../../main/frontend/labs/successMetrics/module';
import legacyConfigurationModule from '../../../../../main/frontend/LegacyConfigurationModule';

describe('application-counts-chart component', function () {
  beforeEach(
    angular.mock.module(
      successMetricsModule.name,
      legacyConfigurationModule.name
    )
  );

  var getVm;

  beforeEach(inject(function ($componentController) {
    getVm = function (applicationCountsData) {
      return $componentController('applicationCountsChart', null, {
        applicationCountsData: applicationCountsData,
      });
    };
  }));

  it('sets the numeric values from the applicationCountsData', function () {
    var applicationCountsData = {
        totalApplications: 5,
        activeApplications: 4,
        total: {
          applicationsWithViolations: 3,
          applicationsWithCriticalViolations: 2,
        },
        security: {
          applicationsWithViolations: 2,
          applicationsWithCriticalViolations: 2,
        },
        license: {
          applicationsWithViolations: 1,
          applicationsWithCriticalViolations: 1,
        },
        quality: {
          applicationsWithViolations: 1,
          applicationsWithCriticalViolations: 0,
        },
        other: {
          applicationsWithViolations: 0,
          applicationsWithCriticalViolations: 0,
        },
      },
      vm = getVm(applicationCountsData);

    expect(vm.applicationCount).toBe(4);
    expect(vm.applicationCountSecurity).toBe(2);
    expect(vm.applicationCountLicense).toBe(1);
    expect(vm.applicationCountQuality).toBe(1);
    expect(vm.applicationCountOther).toBe(0);
    expect(vm.applicationCountTotalViolating).toBe(3);
    expect(vm.applicationCountSecurityCritical).toBe(2);
    expect(vm.applicationCountLicenseCritical).toBe(1);
    expect(vm.applicationCountQualityCritical).toBe(0);
    expect(vm.applicationCountOtherCritical).toBe(0);
    expect(vm.applicationCountTotalViolatingCritical).toBe(2);
  });
});
