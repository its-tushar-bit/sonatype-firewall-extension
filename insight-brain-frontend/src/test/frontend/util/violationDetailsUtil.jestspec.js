/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { extractViolationDetails } from 'MainRoot/util/violationDetailsUtil';

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
        allVersionsComponentName: 'componentName',
        constraintName: 'constraintName',
        policyName: 'policyName',
        policyViolationId: 'policyViolationId',
        reasons: ['reason'],
        threatLevelCategory: 'critical',
        vulnerabilityId: 'vulnerabilityId',
      });
    });

    it('returns defaults without throwing when constraintViolations is empty (e.g. CVE reclassified and removed)', function () {
      const violationDetails = {
        componentIdentifier: { format: 'maven', coordinates: null },
        constraintViolations: [],
        policyName: 'policyName',
        policyViolationId: 'policyViolationId',
        threatLevel: 10,
      };

      expect(() => extractViolationDetails(violationDetails)).not.toThrow();
      expect(extractViolationDetails(violationDetails)).toEqual(
        expect.objectContaining({
          constraintName: undefined,
          reasons: [],
          vulnerabilityId: undefined,
          policyName: 'policyName',
          policyViolationId: 'policyViolationId',
        })
      );
    });

    it('returns defaults without throwing when constraintViolations is missing entirely', function () {
      const violationDetails = {
        componentIdentifier: { format: 'maven', coordinates: null },
        policyName: 'policyName',
        policyViolationId: 'policyViolationId',
        threatLevel: 10,
      };

      expect(() => extractViolationDetails(violationDetails)).not.toThrow();
      expect(extractViolationDetails(violationDetails)).toEqual(
        expect.objectContaining({ constraintName: undefined, reasons: [] })
      );
    });
  });
});
