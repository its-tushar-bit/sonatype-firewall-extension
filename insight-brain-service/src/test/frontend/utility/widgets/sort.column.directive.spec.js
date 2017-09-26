describe('sort.column.directive.spec.js', function() {
  var $compile,
      scope,
      isolatedScope,
      vm,
      element;

  beforeEach(module('utility'));
  beforeEach(inject(function(_$compile_, $rootScope) {
    scope = $rootScope.$new();

    $compile = _$compile_;
  }));

  afterEach(function() {
    scope.$destroy();
  });

  it('Test initial sort with no default', function() {
    element = $compile('<table sort="" class="policy-list" cols="8"><tr class="simple">' +
        '<th sort-column="foo"><span>COL HEADER</span></th></tr></table>')(scope).find('th');

    isolatedScope = element.isolateScope();
    isolatedScope.$digest();

    vm = isolatedScope.vm;

    spyOn(vm, 'updateHeader');
    expect(vm.isUp()).toBeFalsy();
    expect(vm.isDown()).toBeFalsy();
    expect(vm.field).toBe('foo');
    expect(vm.inverted).toBeFalsy();

    expect(vm.updateHeader).not.toHaveBeenCalled();
    expect(element.hasClass('selected-column')).toBe(false);
  });

  it('Test initial sort with default', function() {
    element = $compile('<table sort="foo"><tr">' +
        '<th sort-column="foo"><span>COL HEADER</span></th></tr></table>')(scope).find('th');

    isolatedScope = element.isolateScope();
    isolatedScope.$digest();

    vm = isolatedScope.vm;

    spyOn(vm, 'updateHeader');
    expect(vm.isUp()).toBeTruthy();
    expect(vm.isDown()).toBeFalsy();
    expect(vm.field).toBe('foo');
    expect(vm.inverted).toBeFalsy();

    expect(vm.updateHeader).not.toHaveBeenCalled();
    expect(element.hasClass('selected-column')).toBe(true);
  });

  it('Check initial sort with inverted', function() {
    element = $compile('<table sort="foo"><tr>' +
        '<th sort-column="foo" sort-inverted="true"><span>COL HEADER</span></th></tr></table>')(scope).find('th');

    isolatedScope = element.isolateScope();
    isolatedScope.$digest();

    vm = isolatedScope.vm;

    spyOn(vm, 'updateHeader');
    expect(vm.isUp()).toBeFalsy();
    expect(vm.isDown()).toBeTruthy();
    expect(vm.field).toBe('foo');
    expect(vm.inverted).toBeTruthy();

    expect(vm.updateHeader).not.toHaveBeenCalled();
    expect(element.hasClass('selected-column')).toBe(true);
  });

  it('Test sort calls', function() {
    element = $compile('<table sort=""><tr>' +
        '<th sort-column="foo"><span>COL HEADER</span></th></tr></table>')(scope).find('th');

    isolatedScope = element.isolateScope();
    isolatedScope.$digest();

    vm = isolatedScope.vm;

    expect(vm.isUp()).toBeFalsy();
    expect(vm.isDown()).toBeFalsy();
    expect(vm.field).toBe('foo');
    expect(vm.inverted).toBeFalsy();
    expect(element.hasClass('selected-column')).toBe(false);

    // simulate click 
    vm.setSort();
    scope.$digest();

    expect(element.hasClass('selected-column')).toBe(true);
    expect(vm.isUp()).toBeTruthy();
    expect(vm.isDown()).toBeFalsy();
    expect(vm.field).toBe('foo');
    expect(vm.inverted).toBeFalsy();

    // simulate click 
    vm.setSort();
    scope.$digest();

    expect(element.hasClass('selected-column')).toBe(true);
    expect(vm.isUp()).toBeFalsy();
    expect(vm.isDown()).toBeTruthy();
    expect(vm.field).toBe('foo');
    expect(vm.inverted).toBeFalsy();
  });
});
