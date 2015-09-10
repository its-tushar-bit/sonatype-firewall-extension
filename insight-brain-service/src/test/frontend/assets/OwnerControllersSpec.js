describe('OwnerControllers', function () {
  beforeEach(module('owner.manager.module', function ($provide) {
    $provide.value('$cookies', {});
  }));

  function createTests(type, storeName, owner) {
    var deferred,
        refreshDeferred,
        controllerScope;

    function flushTimeouts() {
      inject(function ($timeout) {
        $timeout.flush();
      });
    }

    function callPromiseError() {
      deferred.reject.apply(deferred, arguments);
      flushTimeouts();
    }

    function callPromiseSuccess() {
      deferred.resolve.apply(deferred, arguments);
      flushTimeouts();
    }

    beforeEach(inject(function ($q) {
      deferred = $q.defer();
      spyOn(deferred.promise, 'then').andCallThrough();
      refreshDeferred = $q.defer();
      spyOn(refreshDeferred.promise, 'then').andCallThrough();
    }));

    afterEach(function () {
      controllerScope.$destroy();
    });

    describe('OwnerSummaryController', function () {
      beforeEach(inject(['$controller', '$rootScope', storeName, function ($controller, $rootScope, store) {
        spyOn(store, 'get').andReturn(deferred.promise);
        spyOn(store, 'refresh').andReturn(refreshDeferred.promise);

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

      it('Typical', inject(function () {
        expect(deferred.promise.then).toHaveBeenCalled();

        callPromiseSuccess([owner]);
        expect(controllerScope.owner).toEqual(owner);
        expect(controllerScope.type).toEqual(type);
      }));

      it('Missing', inject(function () {
        expect(deferred.promise.then).toHaveBeenCalled();

        callPromiseSuccess([{},{}]);
        expect(controllerScope.error).toEqual('Unable to locate ' + type);
        expect(controllerScope.type).toEqual(type);
      }));

      it('Error', inject(function () {
        expect(deferred.promise.then).toHaveBeenCalled();

        callPromiseError('error');
        expect(controllerScope.owner).toBeUndefined();
        expect(controllerScope.error).toEqual(['error']);
        expect(controllerScope.type).toEqual(type);

        // reload successfully
        controllerScope.doLoad();
        expect(refreshDeferred.promise.then).toHaveBeenCalled();
        refreshDeferred.promise.then.mostRecentCall.args[0]([owner]);
        expect(controllerScope.owner).toEqual(owner);
        expect(controllerScope.type).toEqual(type);
        expect(controllerScope.error).toBeUndefined();
      }));
    });
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
