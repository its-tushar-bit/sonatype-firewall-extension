describe('sort.directive.spec.js', function() {
  var $compile,
      scope,
      sortVm,
      element;

  beforeEach(module('utility'));
  beforeEach(inject(function(_$compile_, $rootScope) {
    scope = $rootScope.$new();

    $compile = _$compile_;
  }));

  afterEach(function() {
    scope.$destroy();
  });

  it('Test sort', function() {

    element = $compile('<table sort="foo"></table>')(scope);
    scope.$digest();

    sortVm = scope.sortVm;
    
    expect(sortVm.sortFields.length).toBe(1);
    expect(sortVm.sortFields).toEqual(["foo"]);
    expect(sortVm.extractSortField("foo")).toBe("foo");

    sortVm.setSort(["foo","-bar"]);
    expect(sortVm.sortFields.length).toBe(2);
    expect(sortVm.sortFields).toContain("foo");
    expect(sortVm.sortFields).toContain("-bar");

    expect(sortVm.extractSortField("foo")).toBe("foo");
    expect(sortVm.extractSortField("-bar")).toBe("bar");

    sortVm.setSort(["bar"]);
    expect(sortVm.sortFields.length).toBe(1);
    expect(sortVm.sortFields).toEqual(["bar"]);
    expect(sortVm.extractSortField("bar")).toBe("bar");
  });
});
