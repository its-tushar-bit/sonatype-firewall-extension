/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import successMetricsModule from '../../../../../main/frontend/labs/successMetrics/module';
import legacyConfigurationModule from '../../../../../main/frontend/LegacyConfigurationModule';

describe('mttr-chart component', function () {
  beforeEach(angular.mock.module(successMetricsModule.name, legacyConfigurationModule.name));

  var getVm;

  beforeEach(inject(function ($componentController) {
    getVm = function (mttrData) {
      return $componentController('mttrChart', null, { mttrData: mttrData });
    };
  }));

  it('creates the mttr chart with passed-in data', function () {
    var mttrData = [
        {
          timePeriodStart: 1483254000000,
          mttrInSeconds: null,
          criticalMttrInSeconds: null,
        },
        {
          timePeriodStart: 1485932400000,
          mttrInSeconds: 1209714,
          criticalMttrInSeconds: 1209714,
        },
        {
          timePeriodStart: 1488351600000,
          mttrInSeconds: 484000,
          criticalMttrInSeconds: 484000,
        },
      ],
      vm = getVm(mttrData);

    expect(vm.mttrChart).toBeDefined();
  });
});
