/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global describe, beforeEach, it, expect, inject */
import {sum, values, compose} from 'ramda';
import successMetricsModule from '../../../../../main/frontend/labs/successMetrics/module';
import {generateWeekCounts} from './mockViolationCounts';

describe('violationTrendsChart component', function() {
  beforeEach(angular.mock.module(successMetricsModule.name));

  let getVm;

  beforeEach(inject(function($componentController) {
    getVm = function(violationCounts) {
      return $componentController('violationTrendsChart', null, { violationCounts: violationCounts });
    };
  }));

  describe('$onInit', function() {
    it('sets vm.weekCount from the size of the violationCounts', function() {
      const mockCounts = [
        generateWeekCounts('Week of Sep 10th', 0, 1, 2),
        generateWeekCounts('Week of Sep 17th', 3, 4, 5),
        generateWeekCounts('Week of Sep 24th', 6, 7, 8)
      ];
      const vm = getVm(mockCounts);
      vm.$onInit();
      expect(vm.weekCount).toBe(3);
    });

    it('calculates totals', function() {
      const mockCounts = [
        generateWeekCounts('Week of Sep 10th', 0, 1, 2),
        generateWeekCounts('Week of Sep 17th', 3, 4, 5),
        generateWeekCounts('Week of Sep 24th', 6, 7, 8)
      ];
      const vm = getVm(mockCounts);
      vm.$onInit();
      const expectedTotalDiscovered = 9;
      const expectedTotalWaived = 12;
      const expectedTotalFixed = 15;
      const expectedTotalDelta = -18;
      expect(vm.totals.totalDiscovered).toBe(expectedTotalDiscovered);
      expect(vm.totals.totalWaived).toBe(expectedTotalWaived);
      expect(vm.totals.totalFixed).toBe(expectedTotalFixed);
      expect(vm.totals.totalDelta).toBe(expectedTotalDelta);
    });

    it('converts violationCounts to datasets consumed by charts per policy type', function() {
      const mockCounts = [
        generateWeekCounts('Week of Sep 10th', 0, 1, 2),
        generateWeekCounts('Week of Sep 17th', 3, 4, 5),
        generateWeekCounts('Week of Sep 24th', 6, 7, 8)
      ];
      const vm = getVm(mockCounts);
      vm.$onInit();

      expect(vm.data.security).toEqual(getExpectedDataset(mockCounts, 'SECURITY'));
      expect(vm.data.quality).toEqual(getExpectedDataset(mockCounts, 'QUALITY'));
      expect(vm.data.license).toEqual(getExpectedDataset(mockCounts, 'LICENSE'));
      expect(vm.data.other).toEqual(getExpectedDataset(mockCounts, 'OTHER'));
    });

    it('creates dataset for "all violations" chart', function() {
      const mockCounts = [
        generateWeekCounts('Week of Sep 10th', 0, 1, 2),
        generateWeekCounts('Week of Sep 17th', 3, 4, 5),
        generateWeekCounts('Week of Sep 24th', 6, 7, 8)
      ];
      const vm = getVm(mockCounts);
      vm.$onInit();

      const expectedDataset = {
        discovered: [
          {timePeriodIndex: 0, timePeriodName: 'Week of Sep 10th', violations: 0},
          {timePeriodIndex: 1, timePeriodName: 'Week of Sep 17th', violations: 3},
          {timePeriodIndex: 2, timePeriodName: 'Week of Sep 24th', violations: 6}
        ],
        waived: [
          {timePeriodIndex: 0, timePeriodName: 'Week of Sep 10th', violations: 1},
          {timePeriodIndex: 1, timePeriodName: 'Week of Sep 17th', violations: 4},
          {timePeriodIndex: 2, timePeriodName: 'Week of Sep 24th', violations: 7}
        ],
        fixed: [
          {timePeriodIndex: 0, timePeriodName: 'Week of Sep 10th', violations: 2},
          {timePeriodIndex: 1, timePeriodName: 'Week of Sep 17th', violations: 5},
          {timePeriodIndex: 2, timePeriodName: 'Week of Sep 24th', violations: 8}
        ],
        delta: [
          {timePeriodIndex: 0, timePeriodName: 'Week of Sep 10th', violations: -3},
          {timePeriodIndex: 1, timePeriodName: 'Week of Sep 17th', violations: -6},
          {timePeriodIndex: 2, timePeriodName: 'Week of Sep 24th', violations: -9}
        ]
      };

      expect(vm.data.all).toEqual(expectedDataset);
    });

    describe('statistics', function() {
      it('are created with proper values', function() {
        const mockCounts = [
          generateWeekCounts('Week of Sep 10th', 2, 0, 10),
          generateWeekCounts('Week of Sep 17th', 5, 3, 1),
          generateWeekCounts('Week of Sep 24th', 8, 1, 2)
        ];
        const vm = getVm(mockCounts);
        vm.$onInit();
        const expectedStatistics = {
          deltaMax: 5,
          deltaMin: -8,
          discoveredMax: 8,
          waivedMax: 3,
          fixedMax: 10
        };
        expect(vm.data.statistics).toEqual(expectedStatistics);
      });

      it('are created with zero deltaMax if there are no positive deltas', function() {
        const mockCounts = [
          generateWeekCounts('Week of Sep 10th', 0, 1, 2),
          generateWeekCounts('Week of Sep 17th', 3, 4, 5),
          generateWeekCounts('Week of Sep 24th', 6, 7, 8)
        ];
        const vm = getVm(mockCounts);
        vm.$onInit();
        const expectedStatistics = {
          deltaMax: 0,
          deltaMin: -9,
          discoveredMax: 6,
          waivedMax: 7,
          fixedMax: 8
        };
        expect(vm.data.statistics).toEqual(expectedStatistics);
      });

      it('are created with zero deltaMin if there are no negative deltas', function() {
        const mockCounts = [
          generateWeekCounts('Week of Sep 10th', 2, 0, 1),
          generateWeekCounts('Week of Sep 17th', 5, 3, 1),
          generateWeekCounts('Week of Sep 24th', 8, 1, 2)
        ];
        const vm = getVm(mockCounts);
        vm.$onInit();
        const expectedStatistics = {
          deltaMax: 5,
          deltaMin: 0,
          discoveredMax: 8,
          waivedMax: 3,
          fixedMax: 2
        };
        expect(vm.data.statistics).toEqual(expectedStatistics);
      });
    });
  });
});

const sumValues = compose(sum, values);

function getExpectedDataset(violationCounts, policyType) {
  const week0Discovered = sumValues(violationCounts[0].discoveredCounts[policyType]);
  const week1Discovered = sumValues(violationCounts[1].discoveredCounts[policyType]);
  const week2Discovered = sumValues(violationCounts[2].discoveredCounts[policyType]);

  const week0Waived = sumValues(violationCounts[0].waivedCounts[policyType]);
  const week1Waived = sumValues(violationCounts[1].waivedCounts[policyType]);
  const week2Waived = sumValues(violationCounts[2].waivedCounts[policyType]);

  const week0Fixed = sumValues(violationCounts[0].fixedCounts[policyType]);
  const week1Fixed = sumValues(violationCounts[1].fixedCounts[policyType]);
  const week2Fixed = sumValues(violationCounts[2].fixedCounts[policyType]);

  const week0Delta = week0Discovered - week0Waived - week0Fixed;
  const week1Delta = week1Discovered - week1Waived - week1Fixed;
  const week2Delta = week2Discovered - week2Waived - week2Fixed;

  return {
    discovered: [
      {timePeriodIndex: 0, timePeriodName: 'Week of Sep 10th', violations: week0Discovered},
      {timePeriodIndex: 1, timePeriodName: 'Week of Sep 17th', violations: week1Discovered},
      {timePeriodIndex: 2, timePeriodName: 'Week of Sep 24th', violations: week2Discovered}
    ],
    waived: [
      {timePeriodIndex: 0, timePeriodName: 'Week of Sep 10th', violations: week0Waived},
      {timePeriodIndex: 1, timePeriodName: 'Week of Sep 17th', violations: week1Waived},
      {timePeriodIndex: 2, timePeriodName: 'Week of Sep 24th', violations: week2Waived}
    ],
    fixed: [
      {timePeriodIndex: 0, timePeriodName: 'Week of Sep 10th', violations: week0Fixed},
      {timePeriodIndex: 1, timePeriodName: 'Week of Sep 17th', violations: week1Fixed},
      {timePeriodIndex: 2, timePeriodName: 'Week of Sep 24th', violations: week2Fixed}
    ],
    delta: [
      {timePeriodIndex: 0, timePeriodName: 'Week of Sep 10th', violations: week0Delta},
      {timePeriodIndex: 1, timePeriodName: 'Week of Sep 17th', violations: week1Delta},
      {timePeriodIndex: 2, timePeriodName: 'Week of Sep 24th', violations: week2Delta}
    ]
  };
}
