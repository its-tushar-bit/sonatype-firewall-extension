/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  formatViolationRiskPercentage,
  setAppRiskAndSortViolationsByThreat,
} from 'MainRoot/dashboard/results/componentRisk/componentRiskUtils.js';

describe('componentRiskUtils', function () {
  describe('setAppRiskAndSortViolationsByThreat', function () {
    it('calculates risk and sorts violations by threat level for each app', function () {
      let components = [
        {
          application: { name: 'testApp1' },
          policyViolations: [{ threatLevel: 1 }, { threatLevel: 2 }, { threatLevel: 3 }],
          risk: 10,
        },
        {
          application: { name: 'testApp2' },
          policyViolations: [{ threatLevel: 7 }, { threatLevel: 5 }, { threatLevel: 10 }],
          risk: 100,
        },
      ];

      const sortedViolationsAndRisk = setAppRiskAndSortViolationsByThreat(components);

      // Test it correctly sets risk
      expect(sortedViolationsAndRisk[0].risk).toEqual(6);
      expect(sortedViolationsAndRisk[1].risk).toEqual(22);

      // Test it sorts policy violations
      expect(sortedViolationsAndRisk[0].policyViolations[0].threatLevel).toEqual(3);
      expect(sortedViolationsAndRisk[0].policyViolations[1].threatLevel).toEqual(2);
      expect(sortedViolationsAndRisk[0].policyViolations[2].threatLevel).toEqual(1);
      expect(sortedViolationsAndRisk[1].policyViolations[0].threatLevel).toEqual(10);
      expect(sortedViolationsAndRisk[1].policyViolations[1].threatLevel).toEqual(7);
      expect(sortedViolationsAndRisk[1].policyViolations[2].threatLevel).toEqual(5);
    });
  });

  describe('formatViolationRiskPercentage', function () {
    it('returns correct text-formatted percentages for given values', function () {
      expect(formatViolationRiskPercentage(1, 150)).toEqual('< 1%');
      expect(formatViolationRiskPercentage(50, 100)).toEqual('50%');
    });
  });
});
