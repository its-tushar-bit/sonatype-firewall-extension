describe('delete.modal.controller.spec.js', function() {
  beforeEach(module('utility'));

  var vm,
      resource = ResourceUtils().createMockResource(),
      $timeout,
      scope;

  beforeEach(inject(function($controller, $rootScope, $q, _$timeout_) {
    scope = $rootScope.$new();
    scope.$close = jasmine.createSpy();


    vm = $controller('DeleteModalController', {
      $scope: scope,
      resourceType: 'foo',
      resourceName: 'bar',
      resource: resource,
      saveOnDelete: false
    });
    $timeout = _$timeout_;
  }));

  it('sets resource metadata', function() {
    expect(vm.resourceType).toBe('foo');
    expect(vm.resourceName).toBe('bar');
  });

  it('deletes a resource', function() {
    vm.deleteResource();
    expect(resource.$delete).toHaveBeenCalled();

    resource.resolveDelete();
    $timeout.flush();

    expect(vm.error).toBeUndefined();
  });

  it('handles a delete error', function() {
    vm.deleteResource();
    expect(resource.$delete).toHaveBeenCalled();

    resource.rejectDelete('qux');
    $timeout.flush();

    expect(vm.error).toBe('qux');
  });
});
