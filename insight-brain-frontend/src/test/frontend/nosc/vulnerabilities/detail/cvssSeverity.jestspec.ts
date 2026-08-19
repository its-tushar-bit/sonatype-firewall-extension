/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { cvssSeverityForScore } from 'MainRoot/nosc/vulnerabilities/detail/cvssSeverity';

describe('cvssSeverityForScore', () => {
  it.each([
    [10, 'Critical', 'red'],
    [9, 'Critical', 'red'],
    [8.9, 'High', 'orange'],
    [7, 'High', 'orange'],
    [6.9, 'Medium', 'yellow'],
    [4, 'Medium', 'yellow'],
    [3.9, 'Low', 'indigo'],
    [0.1, 'Low', 'indigo'],
    [0, 'None', 'gray'],
  ] as const)('maps score %s to %s / %s', (score, label, color) => {
    expect(cvssSeverityForScore(score)).toEqual({ label, color });
  });
});
