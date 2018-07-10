import {zipWith, reduce, head, tail, map, negate} from 'ramda';

import {security, license, quality, other} from './data';

export default function controller() {
  const vm = this;

  const totals = {
    discovered: getTotals(security.discovered, license.discovered, quality.discovered, other.discovered),
    fixed: getTotals(security.fixed, license.fixed, quality.fixed, other.fixed),
    waived: getTotals(security.waived, license.waived, quality.waived, other.waived)
  };

  const deltaTotals = getDelta(totals);

  const statistics = {
    deltaMax: getMax(deltaTotals),
    deltaMin: getMin(deltaTotals),
    newMax: getMax(totals.discovered),
    fixedMax: getMax(totals.fixed),
    waivedMax: getMax(totals.waived)
  };

  let active = null;

  Object.assign(vm, {
    totals: {...totals, delta: deltaTotals},
    security: {...security, delta: getDelta(security)},
    license: {...license, delta: getDelta(license)},
    quality: {...quality, delta: getDelta(quality)},
    other: {...other, delta: getDelta(other)},
    statistics,
    setActive(type) {
      active = type;
    },
    clearActive() {
      active = null;
    },
    isInactive(type) {
      return active != null && active !== type;
    }
  });
}

const sumData = zipWith((a, b) => ({week: a.week, violations: a.violations + b.violations}));

const negateData = map(({week, violations}) => ({week, violations: negate(violations)}));

function getTotals(...arrays) {
  return reduce(sumData, head(arrays), tail(arrays));
}

function getDelta({discovered, fixed, waived}) {
  return getTotals(discovered, negateData(fixed), negateData(waived));
}

const getMax = reduce((acc, {violations}) => acc > violations ? acc : violations, 0);
const getMin = reduce((acc, {violations}) => acc < violations ? acc : violations, 0);
