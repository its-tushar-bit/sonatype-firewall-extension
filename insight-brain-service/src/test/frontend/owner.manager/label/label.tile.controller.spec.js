describe('LabelTileController', function () {
  var vm;

  beforeEach(module('owner.manager.module', function ($provide) {
    $provide.value('$cookies', {});
  }));

  function createTests(type, storeName, owner) {
    var getDeferred, $timeout;

    beforeEach(inject([
      '$controller', storeName, '$q', '$timeout', function($controller, store, $q, _$timeout_) {
        $timeout = _$timeout_;
        getDeferred = $q.defer();

        spyOn(getDeferred.promise, 'then').andCallThrough();
        spyOn(store, 'get').andReturn(getDeferred.promise);

        vm = $controller('LabelTileController', {
          $state: {
            current: {
              name: 'management.' + type + '-view'
            },
            params: type === 'application' ? {applicationPublicId: owner.publicId} : {organizationId: owner.id}
          }
        });
      }
    ]));

    it('Typical', inject(function() {
      expect(getDeferred.promise.then).toHaveBeenCalled();

      getDeferred.resolve([owner]);
      $timeout.flush();
      expect(vm.owner).toEqual(owner);
    }));

    it('Missing', inject(function() {
      expect(getDeferred.promise.then).toHaveBeenCalled();

      getDeferred.resolve([{}, {}]);
      $timeout.flush();
      expect(vm.error).toEqual('Unable to locate ' + type);
    }));
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
