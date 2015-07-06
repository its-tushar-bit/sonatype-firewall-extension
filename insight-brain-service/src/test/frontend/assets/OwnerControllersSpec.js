describe('OwnerControllers', function () {
  beforeEach(module('OwnerModule'));

  function createTests(type, storeName, owner) {
    var promise,
        controllerScope;

    beforeEach(inject(['$controller', '$rootScope', storeName, function ($controller, $rootScope, store) {
      promise = { then : jasmine.createSpy('storeSpy') };
      spyOn(store, 'get').andReturn(promise);

      controllerScope = $rootScope.$new();
      $controller('OwnerSummaryController', {
        $scope : controllerScope,
        $state : {
          current : {
            name : 'management.' + type +  '-view'
          },
          params : type === 'application' ? { applicationPublicId : 'abcd' } : { organizationId : 'abcd' }
        }
      });
    }]));

    afterEach(function () {
      controllerScope.$destroy();
    });

    describe('OwnerSummaryController', function () {
      it('Typical', inject(function () {
        expect(promise.then).toHaveBeenCalled();

        promise.then.mostRecentCall.args[0]([owner]);
        expect(controllerScope.owner).toEqual(owner);
        expect(controllerScope.type).toEqual(type);
      }));

      it('Missing', inject(function () {
        expect(promise.then).toHaveBeenCalled();

        promise.then.mostRecentCall.args[0]([{},{}]);
        expect(controllerScope.error).toEqual('Unable to locate ' + type);
        expect(controllerScope.type).toEqual(type);
      }));

      it('Error', inject(function () {
        expect(promise.then).toHaveBeenCalled();

        promise.then.mostRecentCall.args[1]('error', 'error', 'error');
        expect(controllerScope.owner).toBeUndefined();
        expect(controllerScope.error).toEqual(['error', 'error', 'error']);
        expect(controllerScope.type).toEqual(type);

        // reload successfully
        controllerScope.doLoad();
        expect(promise.then.calls.length).toEqual(2);
        promise.then.mostRecentCall.args[0]([owner]);
        expect(controllerScope.owner).toEqual(owner);
        expect(controllerScope.type).toEqual(type);
        expect(controllerScope.error).toBeUndefined();
      }));
    });
  }

  describe('Organization', function () {
    createTests('organization', 'OrganizationStore', { id : 'abcd', name : 'My Org' });
  });

  describe('Application', function () {
    createTests('application', 'ApplicationStore', { publicId : 'abcd', id : '0000abcd', name : 'My App' })
  });

});
