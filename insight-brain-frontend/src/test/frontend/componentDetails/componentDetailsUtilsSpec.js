/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { parseOccurrencePathname } from '../../../../src/main/frontend/componentDetails/componentDetailsUtils';

describe('componentDetailsUtils', function () {
  describe('parseOccurrencePathname', function () {
    it('separates the basename and dirname of a path that includes one slash', function () {
      expect(parseOccurrencePathname('foo/bar.js')).toEqual({
        isDependency: false,
        dirname: 'foo',
        basename: 'bar.js',
      });
    });

    it('separates the basename with backslash and dirname of a path with previous and next folder', function () {
      expect(parseOccurrencePathname('dependency:/bar/go.sum/site\\baz\\foo\\foo@v1.0.1')).toEqual({
        isDependency: true,
        dirname: 'bar/go.sum',
        basename: 'site/baz/foo/foo@v1.0.1',
      });
    });

    it('separates the basename and dirname of a path that include multiple slashes', function () {
      expect(parseOccurrencePathname('foo/bar/baz.js')).toEqual({
        isDependency: false,
        dirname: 'foo/bar',
        basename: 'baz.js',
      });
    });

    it('passes through the value as the basename when there is no slash', function () {
      expect(parseOccurrencePathname('baz.js')).toEqual({
        isDependency: false,
        dirname: undefined,
        basename: 'baz.js',
      });
    });

    it('separates the basename with backslash and dirname of a path that includes no previous folder', function () {
      expect(parseOccurrencePathname('dependency:/go.sum/site\\foo\\foo@v1.0.1')).toEqual({
        isDependency: true,
        dirname: 'go.sum',
        basename: 'site/foo/foo@v1.0.1',
      });
    });

    describe('when the pathname starts with "dependency:/"', function () {
      it('separates the basename and dirname of a path that includes one slash', function () {
        expect(parseOccurrencePathname('dependency:/foo/bar.js')).toEqual({
          isDependency: true,
          dirname: 'foo',
          basename: 'bar.js',
        });
      });

      it('separates the basename and dirname of a path that include multiple slashes', function () {
        expect(parseOccurrencePathname('dependency:/foo/bar/baz.js')).toEqual({
          isDependency: true,
          dirname: 'foo/bar',
          basename: 'baz.js',
        });
      });

      it('passes through the value as the basename when there is no slash', function () {
        expect(parseOccurrencePathname('dependency:/baz.js')).toEqual({
          isDependency: true,
          dirname: undefined,
          basename: 'baz.js',
        });
      });
    });
  });
});
