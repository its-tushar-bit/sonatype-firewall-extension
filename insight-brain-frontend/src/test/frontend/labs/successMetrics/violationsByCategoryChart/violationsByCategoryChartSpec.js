/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import successMetricsModule from '../../../../../main/frontend/labs/successMetrics/module';
import legacyConfigurationModule from '../../../../../main/frontend/LegacyConfigurationModule';

describe('violations-by-category-chart component', function() {
  beforeEach(angular.mock.module(successMetricsModule.name, legacyConfigurationModule.name));

  let vm;

  beforeEach(inject(function($componentController) {
    const violationsByCategoryData = [
      { timePeriodName: '9 Apr', security: null, license: null, quality: null, other: null },
      { timePeriodName: '16 Apr', security: null, license: null, quality: null, other: null },
      { timePeriodName: '23 Apr', security: null, license: null, quality: null, other: null },
      { timePeriodName: '30 Apr', security: null, license: null, quality: null, other: null },
      { timePeriodName: '7 May', security: null, license: null, quality: null, other: null },
      { timePeriodName: '14 May', security: null, license: null, quality: null, other: null },
      { timePeriodName: '21 May', security: null, license: null, quality: null, other: null },
      { timePeriodName: '28 May', security: null, license: null, quality: null, other: null },
      { timePeriodName: '4 Jun', security: null, license: null, quality: null, other: null },
      { timePeriodName: '11 Jun', security: 5, license: 4, quality: 2, other: 1 },
      { timePeriodName: '18 Jun', security: 7, license: 5, quality: 1, other: 0 },
      { timePeriodName: '25 Jun', security: 0, license: 0, quality: 0, other: 1 }
    ];

    vm = $componentController('violationsByCategoryChart', null, { violationsByCategoryData });
  }));

  it('creates the Violation By Category chart with passed-in data', function() {
    expect(vm.violationsByCategoryChart).toBeDefined();
  });

  it('sets vm.weekCount from the size of the dataset', function() {
    expect(vm.weekCount).toBe(3);
  });
});
