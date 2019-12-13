/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function generateDataset() {
  const week0Discovered = 10;
  const week1Discovered = 20;
  const week2Discovered = 4;

  const week0Waived = 1;
  const week1Waived = 7;
  const week2Waived = 5;

  const week0Fixed = 5;
  const week1Fixed = 10;
  const week2Fixed = 2;

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
