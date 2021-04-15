/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { extractViolationDetails } from '../../../main/frontend/util/violationDetailsUtil';

describe('violationDetailsUtil', function () {
  describe('extractViolationDetails', () => {
    it('returns empty object for a falsy violationDetails object', function () {
      expect(extractViolationDetails(null)).toEqual({});
    });

    it('returns the correct information from the violationDetails object', function () {
      const violationDetails = {
        componentIdentifier: { format: 'maven', coordinates: null },
        constraintViolations: [
          {
            constraintId: 'constraintId',
            constraintName: 'constraintName',
            reasons: [
              {
                reason: 'reason',
                reference: {
                  value: 'vulnerabilityId',
                },
              },
            ],
          },
        ],
        displayName: { parts: [], name: 'artifactName' },
        filename: 'componentName',
        policyName: 'policyName',
        policyViolationId: 'policyViolationId',
        threatLevel: 10,
      };

      expect(extractViolationDetails(violationDetails)).toEqual({
        componentIdentifier: { format: 'maven', coordinates: null },
        artifactName: 'artifactName',
        componentName: 'componentName',
        constraintName: 'constraintName',
        policyName: 'policyName',
        policyViolationId: 'policyViolationId',
        reasons: ['reason'],
        threatLevelCategory: 'critical',
        vulnerabilityId: 'vulnerabilityId',
      });
    });
  });
});
