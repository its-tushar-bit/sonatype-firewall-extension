describe('owner.summary.controller.js', function() {
  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  function createTests(type, storeName, owner) {
    var mockOwnerStore = StoreUtils().createMockStore(storeName),
        controllerScope,
        $timeout;

    beforeEach(inject(function($controller, $rootScope, _$timeout_) {
      $timeout = _$timeout_;
      controllerScope = $rootScope.$new();

      $controller('OwnerSummaryController', {
        $scope: controllerScope,
        $state: {
          current: {
            name: 'management.' + type + '-view'
          },
          params: type === 'application' ? {applicationPublicId: 'abcd'} : {organizationId: 'abcd'}
        }
      });
    }));

    afterEach(function() {
      controllerScope.$destroy();
    });

    it('Properly Loading Owner', function() {
      mockOwnerStore.resolveGet([owner]);
      $timeout.flush();

      expect(controllerScope.owner).toEqual(owner);
      expect(controllerScope.type).toEqual(type);
    });

    it('Properly Displaying Error', function() {
      mockOwnerStore.resolveGet([{}, {}]);
      $timeout.flush();

      expect(controllerScope.owner).toBeUndefined();
      expect(controllerScope.error).toEqual('Unable to locate ' + type);
      expect(controllerScope.type).toEqual(type);
    });

    it('Refreshing Owner After Error', function() {
      mockOwnerStore.rejectGet('Error');
      $timeout.flush();

      expect(controllerScope.owner).toBeUndefined();
      expect(controllerScope.error).toBeDefined();

      // reload successfully
      controllerScope.doLoad();
      mockOwnerStore.resolveRefresh([owner]);
      $timeout.flush();

      expect(controllerScope.owner).toEqual(owner);
      expect(controllerScope.type).toEqual(type);
      expect(controllerScope.error).toBeUndefined();
    });
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
