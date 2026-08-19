/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  IQ_LIFECYCLE_STAGE_ORDER,
  compareIqLifecycleStageIds,
  iqLifecycleStageLabel,
} from 'MainRoot/nosc/applications/iqLifecycleStages';

describe('iqLifecycleStages', () => {
  it('orders known stages by lifecycle and unknown stages alphabetically after them', () => {
    const shuffled = ['release', 'custom-z', 'build', 'custom-a', 'proxy'];
    expect([...shuffled].sort(compareIqLifecycleStageIds)).toEqual([
      'proxy',
      'build',
      'release',
      'custom-a',
      'custom-z',
    ]);
  });

  it('labels known ids from the map and Title-Cases unknown hyphenated ids', () => {
    expect(iqLifecycleStageLabel('stage-release')).toBe('Stage Release');
    expect(iqLifecycleStageLabel('custom-qa')).toBe('Custom Qa');
  });

  it('includes proxy so Firewall/list and Evaluations cannot drift on that stage', () => {
    expect(IQ_LIFECYCLE_STAGE_ORDER).toContain('proxy');
  });
});
