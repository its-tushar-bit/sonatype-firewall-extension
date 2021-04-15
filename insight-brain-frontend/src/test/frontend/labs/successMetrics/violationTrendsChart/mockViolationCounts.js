/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { range, sum, curry } from 'ramda';

/**
 * Breaks a number into n random numbers
 * @returns array of n random numbers, such that their sum is equal to numberToDistribute
 */
function distributeRandom(n, numberToDistribute) {
  const randomValues = range(0, n - 1).reduce((acc) => {
    const remainder = numberToDistribute - sum(acc);
    return [...acc, getRandomInt(remainder)];
  }, []);
  return [...randomValues, numberToDistribute - sum(randomValues)];
}

const divideByFourRandom = curry(distributeRandom)(4);

export function generateWeekCounts(timePeriodName, discoveredCountsTotal, waivedCountsTotal, fixedCountsTotal) {
  return {
    timePeriodName,
    discoveredCounts: distributeCountsPerPolicyType(discoveredCountsTotal),
    waivedCounts: distributeCountsPerPolicyType(waivedCountsTotal),
    fixedCounts: distributeCountsPerPolicyType(fixedCountsTotal),
  };
}

function distributeCountsPerPolicyType(i) {
  const counts = divideByFourRandom(i);
  return {
    SECURITY: distributeCountsPerThreatLevel(counts[0]),
    LICENSE: distributeCountsPerThreatLevel(counts[1]),
    QUALITY: distributeCountsPerThreatLevel(counts[2]),
    OTHER: distributeCountsPerThreatLevel(counts[3]),
  };
}

function distributeCountsPerThreatLevel(i) {
  const counts = divideByFourRandom(i);
  return {
    SEVERE: counts[0],
    CRITICAL: counts[1],
    MODERATE: counts[2],
    LOW: counts[3],
  };
}

function getRandomInt(max) {
  return Math.floor(Math.random() * Math.floor(max));
}
