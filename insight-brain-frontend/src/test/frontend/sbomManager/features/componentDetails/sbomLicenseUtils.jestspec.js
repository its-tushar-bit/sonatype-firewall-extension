/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { normalizeComponentIdentifier } from 'MainRoot/sbomManager/features/componentDetails/sbomLicenseUtils';

describe('sbomLicenseUtils', () => {
  describe('normalizeComponentIdentifier', () => {
    it('testNormalize_addsMissingClassifierForMaven', () => {
      const input = {
        format: 'maven',
        coordinates: {
          artifactId: 'xstream',
          extension: 'jar',
          groupId: 'com.thoughtworks.xstream',
          version: '1.4.5',
        },
      };

      const result = normalizeComponentIdentifier(input);

      expect(result.coordinates.classifier).toBe('');
      expect(result.coordinates.artifactId).toBe('xstream');
      expect(result.coordinates.groupId).toBe('com.thoughtworks.xstream');
    });

    it('testNormalize_preservesExistingClassifier', () => {
      const input = {
        format: 'maven',
        coordinates: {
          artifactId: 'xstream',
          classifier: 'sources',
          extension: 'jar',
          groupId: 'com.thoughtworks.xstream',
          version: '1.4.5',
        },
      };

      const result = normalizeComponentIdentifier(input);

      expect(result).toBe(input);
    });

    it('testNormalize_preservesEmptyClassifier', () => {
      const input = {
        format: 'maven',
        coordinates: {
          artifactId: 'xstream',
          classifier: '',
          extension: 'jar',
          groupId: 'com.thoughtworks.xstream',
          version: '1.4.5',
        },
      };

      const result = normalizeComponentIdentifier(input);

      expect(result).toBe(input);
    });

    it('testNormalize_doesNotModifyNonMavenFormat', () => {
      const input = {
        format: 'npm',
        coordinates: {
          packageId: 'lodash',
          version: '4.17.21',
        },
      };

      const result = normalizeComponentIdentifier(input);

      expect(result).toBe(input);
    });

    it('testNormalize_handlesNull', () => {
      expect(normalizeComponentIdentifier(null)).toBe(null);
      expect(normalizeComponentIdentifier(undefined)).toBe(undefined);
    });
  });
});
