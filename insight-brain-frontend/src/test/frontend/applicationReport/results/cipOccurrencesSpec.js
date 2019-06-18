import cipModalModule from '../../../../main/frontend/applicationReport/results/cipModal/module';
import { parsePathname } from
  '../../../../main/frontend/applicationReport/results/cipModal/cipOccurrences/cipOccurrences';

describe('cipOccurrences', function() {
  beforeEach(angular.mock.module(cipModalModule.name));

  describe('parsePathname', function() {
    it('separates the basename and dirname of a path that includes one slash', function() {
      expect(parsePathname('foo/bar.js')).toEqual({
        isDependency: false,
        dirname: 'foo',
        basename: 'bar.js'
      });
    });

    it('separates the basename with backslash and dirname of a path with previous and next folder', function() {
      expect(parsePathname('dependency:/bar/go.sum/site\\baz\\foo\\foo@v1.0.1')).toEqual({
        isDependency: true,
        dirname: 'bar/go.sum',
        basename: 'site/baz/foo/foo@v1.0.1'
      });
    });

    it('separates the basename and dirname of a path that include multiple slashes', function() {
      expect(parsePathname('foo/bar/baz.js')).toEqual({
        isDependency: false,
        dirname: 'foo/bar',
        basename: 'baz.js'
      });
    });

    it('passes through the value as the basename when there is no slash', function() {
      expect(parsePathname('baz.js')).toEqual({
        isDependency: false,
        dirname: undefined,
        basename: 'baz.js'
      });
    });

    it('separates the basename with backslash and dirname of a path that includes no previous folder', function() {
      expect(parsePathname('dependency:/go.sum/site\\foo\\foo@v1.0.1')).toEqual({
        isDependency: true,
        dirname: 'go.sum',
        basename: 'site/foo/foo@v1.0.1'
      });
    });

    describe('when the pathname starts with "dependency:/"', function() {
      it('separates the basename and dirname of a path that includes one slash', function() {
        expect(parsePathname('dependency:/foo/bar.js')).toEqual({
          isDependency: true,
          dirname: 'foo',
          basename: 'bar.js'
        });
      });

      it('separates the basename and dirname of a path that include multiple slashes', function() {
        expect(parsePathname('dependency:/foo/bar/baz.js')).toEqual({
          isDependency: true,
          dirname: 'foo/bar',
          basename: 'baz.js'
        });
      });

      it('passes through the value as the basename when there is no slash', function() {
        expect(parsePathname('dependency:/baz.js')).toEqual({
          isDependency: true,
          dirname: undefined,
          basename: 'baz.js'
        });
      });

    });
  });

  it('parses the provided pathnames into parsedPathnames objects', inject(function($componentController, $rootScope) {
    const pathnames = ['foo', 'foo/bar', 'dependency:/foo/bar'],
        scope = $rootScope.$new(),
        controller = $componentController('cipOccurrences', { $scope: scope }, { pathnames });

    scope.$digest();

    expect(controller.parsedPathnames).toEqual([{
      isDependency: false,
      basename: 'foo',
      dirname: undefined
    }, {
      isDependency: false,
      basename: 'bar',
      dirname: 'foo'
    }, {
      isDependency: true,
      basename: 'bar',
      dirname: 'foo'
    }]);

    controller.pathnames = [];
    scope.$digest();

    expect(controller.pathnames).toEqual([]);
  }));
});
