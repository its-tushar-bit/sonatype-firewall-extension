/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  props,
  prop,
  sum,
  compose,
  zipWith,
  reduce,
  head,
  tail,
  map,
  negate,
  lensProp,
  over,
  apply,
  prepend
} from 'ramda';
import template from './violationTrendsChart.html';

export default {
  template,
  controllerAs: 'vm',
  controller: violationTrendsChartController,
  bindings: {
    violationCounts: '<'
  }
};

function violationTrendsChartController() {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      vm.weekCount = vm.violationCounts.length;
      vm.data = getViolationTrendsData(vm.violationCounts);
      vm.totals = calculateTotals(vm.data.all);
    }
  });
}

const getViolationsSum = compose(sum, map(prop('violations')));

function calculateTotals({discovered, waived, fixed}) {
  const totalDiscovered = getViolationsSum(discovered),
      totalWaived = getViolationsSum(waived),
      totalFixed = getViolationsSum(fixed),
      totalDelta = totalDiscovered - totalWaived - totalFixed;
  return {
    totalDiscovered,
    totalWaived,
    totalFixed,
    totalDelta
  };
}

function getEmptyDataset() {
  return {
    discovered: [],
    fixed: [],
    waived: []
  };
}

function getViolationTrendsData(violationCounts) {
  const security = getEmptyDataset(),
      license = getEmptyDataset(),
      quality = getEmptyDataset(),
      other = getEmptyDataset();

  violationCounts.forEach(({timePeriodName, discoveredCounts, waivedCounts, fixedCounts}, i) => {
    security.discovered.push({timePeriodIndex: i, timePeriodName, violations: sumCounts(discoveredCounts.SECURITY)});
    security.waived.push({timePeriodIndex: i, timePeriodName, violations: sumCounts(waivedCounts.SECURITY)});
    security.fixed.push({timePeriodIndex: i, timePeriodName, violations: sumCounts(fixedCounts.SECURITY)});

    license.discovered.push({timePeriodIndex: i, timePeriodName, violations: sumCounts(discoveredCounts.LICENSE)});
    license.waived.push({timePeriodIndex: i, timePeriodName, violations: sumCounts(waivedCounts.LICENSE)});
    license.fixed.push({timePeriodIndex: i, timePeriodName, violations: sumCounts(fixedCounts.LICENSE)});

    quality.discovered.push({timePeriodIndex: i, timePeriodName, violations: sumCounts(discoveredCounts.QUALITY)});
    quality.waived.push({timePeriodIndex: i, timePeriodName, violations: sumCounts(waivedCounts.QUALITY)});
    quality.fixed.push({timePeriodIndex: i, timePeriodName, violations: sumCounts(fixedCounts.QUALITY)});

    other.discovered.push({timePeriodIndex: i, timePeriodName, violations: sumCounts(discoveredCounts.OTHER)});
    other.waived.push({timePeriodIndex: i, timePeriodName, violations: sumCounts(waivedCounts.OTHER)});
    other.fixed.push({timePeriodIndex: i, timePeriodName, violations: sumCounts(fixedCounts.OTHER)});
  });

  // calculate deltas
  security.delta = getDelta(security);
  license.delta = getDelta(license);
  quality.delta = getDelta(quality);
  other.delta = getDelta(other);

  // all violations
  const all = {
    discovered: getAllCounts(security.discovered, license.discovered, quality.discovered, other.discovered),
    fixed: getAllCounts(security.fixed, license.fixed, quality.fixed, other.fixed),
    waived: getAllCounts(security.waived, license.waived, quality.waived, other.waived)
  };
  all.delta = getDelta(all);

  const statistics = {
    deltaMax: getPositiveMaxOrZero(all.delta),
    deltaMin: getNegativeMinOrZero(all.delta),
    discoveredMax: getPositiveMaxOrZero(all.discovered),
    fixedMax: getPositiveMaxOrZero(all.fixed),
    waivedMax: getPositiveMaxOrZero(all.waived)
  };

  return {
    all,
    security,
    license,
    quality,
    other,
    statistics
  };
}

const sumCounts = compose(sum, props(['LOW', 'MODERATE', 'SEVERE', 'CRITICAL']));

const sumData = zipWith((a, b) => ({...a, violations: a.violations + b.violations}));

const negateData = map(over(lensProp('violations'), negate));

function getAllCounts(...arrays) {
  return reduce(sumData, head(arrays), tail(arrays));
}

function getDelta({discovered, fixed, waived}) {
  return getAllCounts(discovered, negateData(fixed), negateData(waived));
}

const getPositiveMaxOrZero = compose(apply(Math.max), prepend(0), map(prop('violations')));
const getNegativeMinOrZero = compose(apply(Math.min), prepend(0), map(prop('violations')));
