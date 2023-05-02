/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { steps, next, prev } from 'MainRoot/firewallOnboarding/firewallOnboardingUtils';

describe('FirewallOnboardingUtils', () => {
  describe('prev', () => {
    it('return previous step', () => {
      expect(prev(steps[0])).toBe(undefined);
      expect(prev(steps[1])).toBe(steps[0]);
    });
  });

  describe('next', () => {
    it('return next step', () => {
      expect(next(steps[0])).toBe(steps[1]);
      expect(next(steps[1])).toBe(undefined);
    });
  });
});
