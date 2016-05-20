describe('change.application.id.controller.spec.js', function() {

  beforeEach(module('owner.manager.module'));

  beforeEach(module('ResourceModule'));

  var vm,
      $rootScope,
      scope,
      mockState,
      $timeout,
      app = ResourceUtils().createMockResource();

  beforeEach(inject(function(_$rootScope_, $controller, $q, _$timeout_) {
    $timeout = _$timeout_;
    mockState = jasmine.createSpyObj('state', ['go']);
    $rootScope = _$rootScope_;
    scope = $rootScope.$new();
    $rootScope.$broadcast = jasmine.createSpy('$broadcast');
    scope.$close = jasmine.createSpy('$close');

    vm = $controller('change.application.id.controller', {
      $scope: scope,
      $rootScope: $rootScope,
      $state: mockState,
      owner: app,
      siblings: [],
      ApplicationStore: {
        create: function() {
          return {publicId: null};
        }
      }
    });

    vm.applicationIdEditorMask = {wrap: SpecUtil.promiseWrapper($q)};
  }));

  afterEach(function() {
    scope.$destroy();
  });

  it('Cleans up after save', function() {
    vm.dirtyApp.publicId = 'newId';
    vm.changeApplicationId();
    app.resolveSave();
    $timeout.flush();
    expect(scope.$close).toHaveBeenCalled();
    expect($rootScope.$broadcast).toHaveBeenCalledWith('reload.owner.tree.data', app, 'application', false);
    expect(mockState.go).toHaveBeenCalledWith('management.view.application', {applicationPublicId: 'newId'});
  });

  it('Checks dirty state', function() {
    expect(vm.isDirty()).toBe(false);
    vm.dirtyApp.publicId = 'foo';
    expect(vm.isDirty()).toBe(true);
    vm.originalApp.publicId = 'foo';
    expect(vm.isDirty()).toBe(false);
  });
});
