import utilityModule from '../../../../main/frontend/utility/utility.module';

describe('double.column.picker.directive.spec.js', function() {
  var $compile,
      scope,
      isolatedScope,
      vm,
      element;

  beforeEach(angular.mock.module(utilityModule.name));
  beforeEach(inject(function(_$compile_, $rootScope) {
    scope = $rootScope.$new();

    $compile = _$compile_;

    element = $compile('<form><double-column-picker list="list" filter-placeholder="Filter" left-column-name="Left" ' +
        'right-column-name="Right" item-name-param="name"></double-column-picker></form>')(scope).children();

    scope.list = [];

    isolatedScope = element.isolateScope();
    vm = isolatedScope.vm;
  }));

  it('Directive moves 2 items from right to left', function() {
    scope.list = [{name: 'name1', picked: true, checked: true}, {name: 'name2', picked: true, checked: true}];
    scope.$apply();

    expect(vm.list).toBe(scope.list);
    vm.moveItems(true);

    expect(angular.copy(vm.list)).toEqual([
      {name: 'name1', picked: false, checked: true}, {name: 'name2', picked: false, checked: true}
    ]);
  });

  it('Directive moves 2 items from left to right', function() {
    scope.list = [{name: 'name1', picked: false, checked: true}, {name: 'name2', picked: false, checked: true}];
    scope.$apply();

    expect(vm.list).toBe(scope.list);
    vm.moveItems(false);

    expect(angular.copy(vm.list)).toEqual([
      {name: 'name1', picked: true, checked: true}, {name: 'name2', picked: true, checked: true}
    ]);
  });

  it('Directive checks all picked items and then unchecks all', function() {
    scope.list = [{name: 'name1', picked: true, checked: false}, {name: 'name2', picked: true, checked: false}];
    scope.$apply();

    expect(vm.list).toBe(scope.list);
    vm.checkAll(true, true);

    expect(angular.copy(vm.list)).toEqual([
      {name: 'name1', picked: true, checked: true}, {name: 'name2', picked: true, checked: true}
    ]);

    vm.checkAll(true, false);

    expect(angular.copy(vm.list)).toEqual([
      {name: 'name1', picked: true, checked: false}, {name: 'name2', picked: true, checked: false}
    ]);
  });

  it('Directive checks all un-picked items and then unchecks all', function() {
    scope.list = [{name: 'name1', picked: false, checked: false}, {name: 'name2', picked: false, checked: false}];
    scope.$apply();

    expect(vm.list).toBe(scope.list);
    vm.checkAll(false, true);

    expect(angular.copy(vm.list)).toEqual([
      {name: 'name1', picked: false, checked: true}, {name: 'name2', picked: false, checked: true}
    ]);

    vm.checkAll(false, false);

    expect(angular.copy(vm.list)).toEqual([
      {name: 'name1', picked: false, checked: false}, {name: 'name2', picked: false, checked: false}
    ]);
  });

  it('Directive calls checkAll when watch is triggered from filtering', function() {
    spyOn(vm, 'checkAll');

    scope.list = [{name: 'name1', picked: false, checked: false}, {name: 'name2', picked: true, checked: false}];
    scope.$apply();

    expect(vm.list).toBe(scope.list);

    vm.toggleLeftSelectAll(false);
    vm.toggleRightSelectAll(true);

    vm.search = 'random';
    isolatedScope.$apply();

    expect(vm.checkAllRight).toBe(true);
    expect(vm.checkAllLeft).toBe(true);
    expect(vm.checkAll).toHaveBeenCalledWith(false, true);
    expect(vm.checkAll).toHaveBeenCalledWith(true, true);
  });

  it('Directive unchecks all filtered items', function() {
    scope.list = [{name: 'name1', picked: false, checked: true}, {name: 'name2', picked: true, checked: true}];
    scope.$apply();

    expect(vm.list).toBe(scope.list);

    vm.search = 'random';
    isolatedScope.$apply();

    expect(angular.copy(vm.list)).toEqual([
      {name: 'name1', picked: false, checked: false}, {name: 'name2', picked: true, checked: false}
    ]);
  });
});
