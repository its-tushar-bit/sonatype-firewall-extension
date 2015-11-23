describe('select.application.contact.controller.spec.js', function() {

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  beforeEach(module('ResourceModule'));

  var vm,
      scope,
      $timeout,
      $httpBackend,
      deleteServiceResourceDefer,
      mockDeleteService,
      mockOwner = ResourceUtils().createMockResource();

  beforeEach(inject(function($rootScope, $q, _$timeout_, _$httpBackend_) {
      scope = $rootScope.$new();
      $timeout = _$timeout_;
      deleteServiceResourceDefer = $q.defer();
      mockDeleteService = {
        deleteResource: function() {
          return deleteServiceResourceDefer.promise;
        }
      }
      $httpBackend = _$httpBackend_;
  }));

  it('Selects current user in search results', function() {
    inject(function($controller) {
      vm = $controller('select.application.contact.controller', {$scope: scope, owner: {contact: {internalName: 'JohnDoe'}}});
    });
    vm.search();
    $httpBackend.whenGET("/rest/user/global/global/query").respond({members: [{internalName: 'Foo'}, {internalName: 'JohnDoe'}]});
    $httpBackend.flush();
    $timeout.flush();
    expect(vm.selected).toBeDefined();
    expect(vm.selected.internalName).toBe('JohnDoe');
  });

  it('Updates owner with selected contact', function() {
    mockOwner.contact = {internalName: 'John Doe'};
    scope.$close = jasmine.createSpy();
    inject(function($controller) {
      vm = $controller('select.application.contact.controller', {$scope: scope, owner: mockOwner});
    });
    vm.selected = {internalName: 'Foo Bar'};
    vm.updateContact();
    mockOwner.resolveSave();
    $timeout.flush();
    $timeout(function(){}, 1000); // mask delay = 0.8s
    $timeout.flush();
    expect(vm.owner.contactInternalName).toBe('Foo Bar');
    expect(scope.$close).toHaveBeenCalled();
  });

  it('Leaves delete mode when confirmation dialog is cancelled', function() {
    inject(function($controller) {
      vm = $controller('select.application.contact.controller', {$scope: scope, owner: {contactInternalName: 'Foo', contact: {displayName: 'Foo Bar'}}, DeleteModalService: mockDeleteService});
    });
    vm.removeContact();
    expect(vm.deleteMode).toBe(true);
    deleteServiceResourceDefer.reject();
    $timeout.flush();
    expect(vm.deleteMode).toBe(false);
  });

  it('Checks for dirty state', function() {
    inject(function($controller) {
      vm = $controller('select.application.contact.controller', {$scope: scope, owner: {}});
    });
    vm.owner.contact = null;
    vm.selected = undefined;
    expect(vm.isDirty()).toBe(false);
    vm.selected = {internalName: 'Foo'};
    expect(vm.isDirty()).toBe(true);
    vm.owner.contact = {internalName: 'Foo'};
    expect(vm.isDirty()).toBe(false);
    vm.owner.contact = {internalName: 'Bar'};
    expect(vm.isDirty()).toBe(true);

  });
})
