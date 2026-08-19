/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { getReleaseVersion } from 'MainRoot/util/versionUtil';

describe('versionUtil', function () {
  describe('getReleaseVersion', () => {
    it('throws an error for a bogus value', function () {
      expect(() => getReleaseVersion(undefined)).toThrow(
        new TypeError("Cannot determine release version from 'undefined'.")
      );
      expect(() => getReleaseVersion(null)).toThrow(new TypeError("Cannot determine release version from 'null'."));
      expect(() => getReleaseVersion('')).toThrow(new TypeError("Cannot determine release version from ''."));
      expect(() => getReleaseVersion(' ')).toThrow(new TypeError("Cannot determine release version from ' '."));
    });

    it('returns the expected version', function () {
      expect(getReleaseVersion('someVersion')).toBe('someVersion');
      expect(getReleaseVersion(' someVersion ')).toBe('someVersion');
      expect(getReleaseVersion('1.189.0-SNAPSHOT')).toBe('189');
      expect(getReleaseVersion('1.189.1-SNAPSHOT')).toBe('189.1');
      expect(getReleaseVersion('1.189.15-SNAPSHOT')).toBe('189.15');
      expect(getReleaseVersion('1.189.0-01')).toBe('189');
      expect(getReleaseVersion('1.189.1-01')).toBe('189.1');
      expect(getReleaseVersion('1.189.15-01')).toBe('189.15');
      expect(getReleaseVersion('1.189.0')).toBe('189');
      expect(getReleaseVersion('1.189.1')).toBe('189.1');
      expect(getReleaseVersion('1.189.15')).toBe('189.15');
      expect(getReleaseVersion('189.0')).toBe('189');
      expect(getReleaseVersion('189.1')).toBe('189.1');
      expect(getReleaseVersion('189.15')).toBe('189.15');
      expect(getReleaseVersion('189')).toBe('189');
    });
  });
});
