/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global describe, beforeEach, it, expect, inject */
import { Component } from 'plottable';
import successMetricsModule from '../../../../../main/frontend/labs/successMetrics/module';
import legacyConfigurationModule from '../../../../../main/frontend/LegacyConfigurationModule';

describe('violation-averages-chart component', function () {
  beforeEach(
    angular.mock.module(
      successMetricsModule.name,
      legacyConfigurationModule.name
    )
  );

  var getVm;

  beforeEach(inject(function ($componentController) {
    getVm = function (averagesData) {
      return $componentController('violationAveragesChart', null, {
        averagesData: averagesData,
      });
    };
  }));

  it('sets the numeric values from the averagesData', function () {
    var averagesData = {
        securityViolations: {
          averageDiscovered: 5,
          averageDiscoveredCritical: 0,
        },
        licenseViolations: {
          averageDiscovered: 7,
          averageDiscoveredCritical: 1,
        },
        qualityViolations: {
          averageDiscovered: 15,
          averageDiscoveredCritical: 0,
        },
        otherViolations: {
          averageDiscovered: 2,
          averageDiscoveredCritical: 2,
        },
        totalViolations: {
          averageDiscovered: 29,
          averageDiscoveredCritical: 3,
        },
      },
      vm = getVm(averagesData);

    expect(vm.averageDiscoveredSecurity).toEqual(5);
    expect(vm.averageDiscoveredLicense).toEqual(7);
    expect(vm.averageDiscoveredQuality).toEqual(15);
    expect(vm.averageDiscoveredOther).toEqual(2);
    expect(vm.averageDiscoveredTotal).toEqual(29);

    expect(vm.averageDiscoveredSecurityCritical).toEqual(0);
    expect(vm.averageDiscoveredLicenseCritical).toEqual(1);
    expect(vm.averageDiscoveredQualityCritical).toEqual(0);
    expect(vm.averageDiscoveredOtherCritical).toEqual(2);
    expect(vm.averageDiscoveredTotalCritical).toEqual(3);
  });

  it('sets vm.chart to a Plottable component', function () {
    var averagesData = {
        securityViolations: {
          averageDiscovered: 5,
          averageDiscoveredCritical: 0,
        },
        licenseViolations: {
          averageDiscovered: 0,
          averageDiscoveredCritical: 7,
        },
        qualityViolations: {
          averageDiscovered: 15,
          averageDiscoveredCritical: 0,
        },
        otherViolations: {
          averageDiscovered: 2,
          averageDiscoveredCritical: 2,
        },
        totalViolations: {
          averageDiscovered: 22,
          averageDiscoveredCritical: 9,
        },
      },
      vm = getVm(averagesData);

    expect(vm.chart instanceof Component).toBe(true);
  });
});
