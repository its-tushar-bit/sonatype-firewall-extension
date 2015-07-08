describe('OwnerControllers', function () {
  beforeEach(module('OwnerModule'));

  function createTests(type, storeName, owner) {
    var promise,
        refreshPromise,
        controllerScope;

    function callPromiseError() {
      return promise.then.mostRecentCall.args[1].apply(null, arguments);
    }

    function callPromiseSuccess() {
      return promise.then.mostRecentCall.args[0].apply(null, arguments);
    }

    beforeEach(function () {
      promise = { then : jasmine.createSpy('promiseThen') };
      refreshPromise = { then : jasmine.createSpy('refreshPromiseThen') };
    });

    afterEach(function () {
      controllerScope.$destroy();
    });

    describe('OwnerSummaryController', function () {
      beforeEach(inject(['$controller', '$rootScope', storeName, function ($controller, $rootScope, store) {
        spyOn(store, 'get').andReturn(promise);
        spyOn(store, 'refresh').andReturn(refreshPromise);

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
        expect(promise.then).toHaveBeenCalled();

        callPromiseSuccess([owner]);
        expect(controllerScope.owner).toEqual(owner);
        expect(controllerScope.type).toEqual(type);
      }));

      it('Missing', inject(function () {
        expect(promise.then).toHaveBeenCalled();

        callPromiseSuccess([{},{}]);
        expect(controllerScope.error).toEqual('Unable to locate ' + type);
        expect(controllerScope.type).toEqual(type);
      }));

      it('Error', inject(function () {
        expect(promise.then).toHaveBeenCalled();

        callPromiseError('error', 'error', 'error');
        expect(controllerScope.owner).toBeUndefined();
        expect(controllerScope.error).toEqual(['error', 'error', 'error']);
        expect(controllerScope.type).toEqual(type);

        // reload successfully
        controllerScope.doLoad();
        expect(refreshPromise.then).toHaveBeenCalled();
        refreshPromise.then.mostRecentCall.args[0]([owner]);
        expect(controllerScope.owner).toEqual(owner);
        expect(controllerScope.type).toEqual(type);
        expect(controllerScope.error).toBeUndefined();
      }));
    });

    describe('OwnerEditorController', function () {
      describe('New Owner', function () {
        var ownerResource;
        beforeEach(inject(['$controller', '$rootScope', function ($controller, $rootScope) {
          ownerResource = {
            $new : true,
            $save : angular.noop,
            isDirty : angular.noop,
            $clone : angular.noop
          };

          controllerScope = $rootScope.$new();
          controllerScope.$dismiss = jasmine.createSpy('dismiss');
          controllerScope.$close = jasmine.createSpy('close');

          $controller('OwnerEditorController', {
            $scope : controllerScope,
            owner : ownerResource,
            ownerType : type,
            siblings : []
          });
        }]));

        describe('Page Changes', function () {
          it('clean', inject(function ($rootScope) {
            spyOn(controllerScope.dirtyOwner, 'isDirty').andReturn(false);
            var event = $rootScope.$broadcast('pageChangeStarted');

            expect(controllerScope.dirtyOwner.isDirty).toHaveBeenCalled();
            expect(event.defaultPrevented).toBeFalsy();
          }));

          it('dirty', inject(function ($rootScope) {
            spyOn(controllerScope.dirtyOwner, 'isDirty').andReturn(true);
            var event = $rootScope.$broadcast('pageChangeStarted');

            expect(controllerScope.dirtyOwner.isDirty).toHaveBeenCalled();
            expect(event.defaultPrevented).toBeTruthy();
          }));

          it('Closes', inject(function ($rootScope) {
            $rootScope.$broadcast('pageChangeAccepted');
            expect(controllerScope.$dismiss).toHaveBeenCalled();
          }));
        });

        describe('Save', function () {
          beforeEach(function () {
            spyOn(controllerScope.dirtyOwner, '$save').andReturn(promise);

            controllerScope.$apply(function () {
              controllerScope.dirtyOwner.name = 'My new ' + type;
              if (type === 'application') {
                controllerScope.dirtyOwner.publicId = 'my-new';
              }
            });
            expect(ownerResource.name).toEqual('My new ' + type); // new objects work with the original

            controllerScope.save();
          });

          it('Error', function () {
            callPromiseError('foobar');
            expect(controllerScope.error).toEqual('foobar');

            // retry successfully
            controllerScope.save();
            expect(controllerScope.error).toBeFalsy();
          });

          it('Success', inject(function ($state) {
            spyOn($state, 'go');
            callPromiseSuccess(angular.extend({ id : 'abcd' }, angular.copy(controllerScope.dirtyOwner)));

            expect($state.go).toHaveBeenCalledWith('management.' + type + '-view', type === 'application' ? {
              applicationPublicId: controllerScope.dirtyOwner.publicId
            } : {
              organizationId: 'abcd'
            });
            expect(controllerScope.$close).toHaveBeenCalled();
          }));
        });
      });
    });
  }

  describe('Organization', function () {
    createTests('organization', 'OrganizationStore', { id : 'abcd', name : 'My Org' });
  });

  describe('Application', function () {
    createTests('application', 'ApplicationStore', { publicId : 'abcd', id : '0000abcd', name : 'My App' })
  });

  describe('OwnerEditor', function () {
    beforeEach(inject(function ($modal) {
      spyOn($modal, 'open');
    }));

    it('open', inject(function (OwnerEditor, $modal) {
      var owner = {
         id : 'foo',
         name : 'bar'
      };

      OwnerEditor.open(owner, 'organization');
      expect($modal.open).toHaveBeenCalled();

      expect($modal.open.mostRecentCall.args[0].resolve.owner()).toEqual(owner);
      expect($modal.open.mostRecentCall.args[0].resolve.ownerType()).toEqual('organization');
    }));
  });
});
