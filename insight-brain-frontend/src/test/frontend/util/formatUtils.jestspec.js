/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { formatNumberLocale } from '../../../main/frontend/util/formatUtils';

describe('formatUtils', function () {
  describe('formatNumberLocale', function () {
    it('formats numbers to locale string en-US', function () {
      expect(formatNumberLocale(299792458)).toBe('299,792,458');
    });

    it('returns null if a number is not passed', function () {
      expect(formatNumberLocale('hello')).toBe(null);
    });
  });
});
