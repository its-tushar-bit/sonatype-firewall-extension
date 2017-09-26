describe('sortable.directive.spec', function() {

  beforeEach(module('ReportViolations'));

  describe('sortable', function() {
    var barScope, fooScope, invertedScope, scope;

    beforeEach(inject(function($rootScope, $compile) {
      scope = $rootScope.$new();
      $compile('<div sortable="bar">' +
          '<span sort-columns="foo">foo</span>' +
          '<span sort-columns="-bar">bar</span>' +
          '<span sort-columns="-foobar" sort-inverted="true">foobar</span>' +
          '</div>')(scope);
      fooScope = scope.$$childHead;
      barScope = scope.$$childHead.$$nextSibling;
      invertedScope = barScope.$$nextSibling;
    }));

    it('tests', function() {
      // by default we should sort by the top-level sortable attributes
      expect(fooScope.isUp()).toBeFalsy();
      expect(fooScope.isDown()).toBeFalsy();
      expect(invertedScope.isUp()).toBeFalsy();
      expect(invertedScope.isDown()).toBeFalsy();
      expect(barScope.isUp()).toBeTruthy();
      expect(barScope.isDown()).toBeFalsy();
      expect(scope.getSortField()).toEqual(['bar']);

      // when sorting by a specific child, should respect its initial sort-columns
      fooScope.$apply(function() {
        barScope.setSort();
      });
      expect(fooScope.isUp()).toBeFalsy();
      expect(fooScope.isDown()).toBeFalsy();
      expect(invertedScope.isUp()).toBeFalsy();
      expect(invertedScope.isDown()).toBeFalsy();
      expect(barScope.isUp()).toBeFalsy();
      expect(barScope.isDown()).toBeTruthy(); //DESC sort is expected for '-bar'
      expect(scope.getSortField()).toEqual(['-bar']);

      //default sort in this case is ASC for 'foo'
      fooScope.$apply(function() {
        fooScope.setSort();
      });
      expect(fooScope.isUp()).toBeTruthy();
      expect(fooScope.isDown()).toBeFalsy();
      expect(invertedScope.isUp()).toBeFalsy();
      expect(invertedScope.isDown()).toBeFalsy();
      expect(barScope.isUp()).toBeFalsy();
      expect(barScope.isDown()).toBeFalsy();
      expect(scope.getSortField()).toEqual(['foo']);

      //setting the sort a second time should reverse the operator and icons
      fooScope.$apply(function() {
        fooScope.setSort();
      });
      expect(fooScope.isUp()).toBeFalsy();
      expect(fooScope.isDown()).toBeTruthy();
      expect(invertedScope.isUp()).toBeFalsy();
      expect(invertedScope.isDown()).toBeFalsy();
      expect(barScope.isUp()).toBeFalsy();
      expect(barScope.isDown()).toBeFalsy();
      expect(scope.getSortField()).toEqual(['-foo']);

      //sorting on a field which is marked as sort-inverted should invert all expectations
      fooScope.$apply(function() {
        invertedScope.setSort();
      });

      expect(fooScope.isUp()).toBeFalsy();
      expect(fooScope.isDown()).toBeFalsy();
      expect(invertedScope.isUp()).toBeTruthy();
      expect(invertedScope.isDown()).toBeFalsy();
      expect(barScope.isUp()).toBeFalsy();
      expect(barScope.isDown()).toBeFalsy();
      expect(scope.getSortField()).toEqual(['-foobar']);

      //sorting on a field which is marked as sort-inverted should invert all expectations
      fooScope.$apply(function() {
        invertedScope.setSort();
      });

      expect(fooScope.isUp()).toBeFalsy();
      expect(fooScope.isDown()).toBeFalsy();
      expect(invertedScope.isUp()).toBeFalsy();
      expect(invertedScope.isDown()).toBeTruthy();
      expect(barScope.isUp()).toBeFalsy();
      expect(barScope.isDown()).toBeFalsy();
      expect(scope.getSortField()).toEqual(['foobar']);
    });
  });

  describe('sortable with secondary sort', function() {
    var barScope, scope, fooScope;

    beforeEach(inject(function($rootScope, $compile) {
      scope = $rootScope.$new();
      $compile('<div sortable="bar,foo">' +
          '<span sort-columns="foo">foo</span>' +
          '<span sort-columns="-bar,foo">bar</span></div>')(scope);
      fooScope = scope.$$childHead;
      barScope = scope.$$childHead.$$nextSibling;
    }));

    it('tests', function() {
      expect(scope.getSortField()).toEqual(['bar', 'foo']);

      fooScope.$apply(function() {
        barScope.setSort();
      });

      expect(scope.getSortField()).toEqual(['-bar', 'foo']);
    });
  });
});
