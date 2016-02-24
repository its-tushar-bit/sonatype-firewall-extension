describe('release.quarantine.controller', function() {
  var scope, dereg, eventSpy;

  beforeEach(module('cip.policy.violations', function($provide) {
    $provide.value('SelectedComponent', {
      get: function() {
        return {
          componentIdentifier: {},
          pathname: '/foo/bar',
          hash: 'abcd'
        }
      }
    });
    $provide.value('OwnerContext', {
      ownerId: 'some-repo-id',
      ownerType: 'repository'
    });
  }));

  beforeEach(inject(function ($rootScope, $controller) {
    eventSpy = jasmine.createSpy('eventListener');
    dereg = $rootScope.$on('reload.component', eventSpy);

    scope = $rootScope.$new();
    scope.$close = jasmine.createSpy('close');

    $controller('release.quarantine.controller as vm', {
      $scope: scope
    });
  }));

  afterEach(function() {
    scope.$destroy();
    dereg();
  });

  it('error to success', inject(function($httpBackend) {
    $httpBackend.expectPOST(SpecUtil.toRegExp('../brain/rest/repositories/some-repo-id/unquarantine/%2Ffoo%2Fbar')).respond(500, 'random error');
    scope.vm.release();

    expect(scope.vm.activeRequest).toBeTruthy();
    $httpBackend.flush();

    expect(scope.vm.activeRequest).toBeFalsy();
    expect(scope.vm.error).toEqual('random error');

    $httpBackend.expectPOST(SpecUtil.toRegExp('../brain/rest/repositories/some-repo-id/unquarantine/%2Ffoo%2Fbar')).respond(204);
    scope.vm.release();

    expect(scope.vm.activeRequest).toBeTruthy();
    expect(scope.vm.error).toBeFalsy();
    $httpBackend.flush();

    expect(scope.vm.activeRequest).toBeFalsy();
    expect(scope.$close).toHaveBeenCalled();
    expect(eventSpy).toHaveBeenCalledWith(jasmine.any(Object), { pathname: '/foo/bar' });
  }));
});
