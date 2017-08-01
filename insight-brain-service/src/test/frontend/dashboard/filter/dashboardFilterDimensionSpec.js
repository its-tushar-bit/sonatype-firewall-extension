describe('dashboardFilterDimension', function() {
  function entity(id, name) {
    return {
      id: id,
      name: name
    };
  }
  var scope, dScope;

  beforeEach(module('dashboard.module'));

  beforeEach(inject(function($compile, $rootScope, $templateCache) {
    scope = $rootScope.$new();
    scope.available = [entity('1', 'bar'), entity('2', 'foo'), entity('3', 'aaa')];
    scope.selected = {'3': true};
    
    $templateCache.put('entity-filter-template', '<div/>');

    var element = $compile('<dashboard-filter-dimension available="available" selected="selected" short-name="foo" long-name="bar"></dashboard-filter-dimension>')(scope);
    scope.$digest();
    dScope = element.isolateScope();
  }));

  afterEach(function() {
    scope.$destroy();
  });

  describe('allSelected', function() {
    it('not filtered', function() {
      expect(dScope.vm.allSelected()).toBeFalsy();

      scope.selected['1'] = true;
      dScope.vm.notifySelectionChanged();
      scope.$digest();

      expect(dScope.vm.allSelected()).toBeFalsy();

      scope.selected['2'] = true;
      dScope.vm.notifySelectionChanged();
      scope.$digest();

      expect(dScope.vm.allSelected()).toBeTruthy();
    });

    it('filtered', function() {
      dScope.vm.filter = 'a';

      expect(dScope.vm.allSelected()).toBeFalsy();

      scope.selected['1'] = true;
      dScope.vm.notifySelectionChanged();
      expect(dScope.vm.allSelected()).toBeTruthy();
    });
  });

  describe('toggleSelectAll', function() {
    it('not filtered', function() {
      dScope.vm.toggleSelectAll();
      scope.$digest();

      expect(dScope.selected['1']).toBeTruthy();
      expect(dScope.selected['2']).toBeTruthy();
      expect(dScope.selected['3']).toBeTruthy();
      expect(dScope.vm.selectedCount).toEqual(3);
      dScope.vm.toggleSelectAll();
      scope.$digest();

      expect(dScope.selected['1']).toBeFalsy();
      expect(dScope.selected['2']).toBeFalsy();
      expect(dScope.selected['3']).toBeFalsy();
      expect(dScope.vm.selectedCount).toEqual(0);
    });

    it('filtered', function() {
      dScope.vm.filter = 'a';

      dScope.vm.toggleSelectAll();
      scope.$digest();

      expect(dScope.selected['2']).toBeFalsy();
      expect(dScope.selected['3']).toBeTruthy();
      expect(dScope.vm.selectedCount).toEqual(2);

      dScope.vm.toggleSelectAll();
      scope.$digest();

      expect(dScope.selected['1']).toBeFalsy();
      expect(dScope.selected['2']).toBeFalsy();
      expect(dScope.selected['3']).toBeFalsy();
      expect(dScope.vm.selectedCount).toEqual(0);
    });
  });

  it('updateSelectedCount', function() {
    expect(dScope.vm.selectedCount).toEqual(1);

    scope.selected['2'] = true;
    dScope.vm.notifySelectionChanged();
    scope.$digest();

    expect(dScope.vm.selectedCount).toEqual(2);

    scope.selected['3'] = false;
    dScope.vm.notifySelectionChanged();
    scope.$digest();

    expect(dScope.vm.selectedCount).toEqual(1);

    scope.selected['2'] = false;
    dScope.vm.notifySelectionChanged();
    scope.$digest();

    expect(dScope.vm.selectedCount).toEqual(0);
  });
});
