describe('sort.directive.spec.js', function() {
  var $compile,
      scope,
      sortVm;

  beforeEach(module('utility.directives'));
  beforeEach(inject(function(_$compile_, $rootScope) {
    scope = $rootScope.$new();

    $compile = _$compile_;
  }));

  afterEach(function() {
    scope.$destroy();
  });

  it('Test sort', function() {

    $compile('<table sort="foo"></table>')(scope);
    scope.$digest();

    sortVm = scope.sortVm;

    expect(sortVm.sortFields.length).toBe(1);
    expect(sortVm.sortFields).toEqual(['foo']);
    expect(sortVm.extractSortField('foo')).toBe('foo');

    sortVm.setSort(['foo', '-bar']);
    expect(sortVm.sortFields.length).toBe(2);
    expect(sortVm.sortFields).toContain('foo');

    expect(sortVm.extractSortField('foo')).toBe('foo');
    expect(sortVm.extractSortField('-bar')).toBe('bar');

    sortVm.setSort(['bar']);
    expect(sortVm.sortFields.length).toBe(1);
    expect(sortVm.sortFields).toEqual(['bar']);
    expect(sortVm.extractSortField('bar')).toBe('bar');
  });
});
