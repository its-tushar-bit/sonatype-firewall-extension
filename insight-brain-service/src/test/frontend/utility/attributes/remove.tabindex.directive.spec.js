describe('remove.tabindex.directive.spec.js', function() {
  var $compile,
      $rootScope;

  beforeEach(module('utility'));

  beforeEach(inject(function(_$compile_, _$rootScope_) {
    $compile = _$compile_;
    $rootScope = _$rootScope_;
  }));

  it('Properly removing tabindex', function() {
    var element = $compile('<div ng-model="test"></div>')($rootScope);
    expect(element.attr('tabindex')).toBeDefined();

    element = $compile('<div ng-model="test" remove-tabindex></div>')($rootScope);
    $rootScope.$digest();
    expect(element.attr('tabindex')).toBeUndefined();
  });
});
