describe('owner.editor.controller.spec.js', function () {
  var controllerScope;

  beforeEach(module('owner.manager.module', function ($provide) {
    $provide.value('$cookies', {});
  }));

  afterEach(function() {
    controllerScope.$destroy();
  });

  function createTests(type) {
    describe('New Owner: ' + type, function() {
      var ownerResource;

      beforeEach(inject(function($controller, $rootScope) {
          ownerResource = {
            $new: true,
            $save: angular.noop,
            isDirty: angular.noop,
            $clone: angular.noop
          };

          controllerScope = $rootScope.$new();
          controllerScope.$dismiss = jasmine.createSpy('dismiss');
          controllerScope.$close = jasmine.createSpy('close');

          $controller('OwnerEditorController', {
            $scope: controllerScope,
            owner: ownerResource,
            ownerType: type,
            siblings: []
          });
        }
      ));

      describe('Page Changes', function() {
        it('clean', inject(function($rootScope) {
          spyOn(controllerScope.dirtyOwner, 'isDirty').andReturn(false);
          var event = $rootScope.$broadcast('pageChangeStarted');

          expect(controllerScope.dirtyOwner.isDirty).toHaveBeenCalled();
          expect(event.defaultPrevented).toBeFalsy();
        }));

        it('dirty', inject(function($rootScope) {
          spyOn(controllerScope.dirtyOwner, 'isDirty').andReturn(true);
          var event = $rootScope.$broadcast('pageChangeStarted');

          expect(controllerScope.dirtyOwner.isDirty).toHaveBeenCalled();
          expect(event.defaultPrevented).toBeTruthy();
        }));

        it('Closes', inject(function($rootScope) {
          $rootScope.$broadcast('pageChangeAccepted');
          expect(controllerScope.$dismiss).toHaveBeenCalled();
        }));
      });

      describe('Save', function() {
        var saveDeferred, $timeout;

        beforeEach(inject(function($q, _$timeout_) {
          $timeout = _$timeout_;
          saveDeferred = $q.defer();

          spyOn(controllerScope.dirtyOwner, '$save').andReturn(saveDeferred.promise);

          controllerScope.$apply(function() {
            controllerScope.dirtyOwner.name = 'My new ' + type;
            if (type === 'application') {
              controllerScope.dirtyOwner.publicId = 'my-new';
            }
          });
          expect(ownerResource.name).toEqual('My new ' + type); // new objects work with the original

          controllerScope.save();
        }));

        it('Error on Owner', function() {
          saveDeferred.reject('foobar');
          $timeout.flush();
          expect(controllerScope.error).toEqual('foobar');

          // retry clears error
          controllerScope.save();
          expect(controllerScope.error).toBeFalsy();
        });

        it('Error on Icon', inject(function($state, $httpBackend) {
          $httpBackend.expectPOST('/rest/' + type + '/icon').respond(500, 'Server Error');
          saveDeferred.resolve(angular.extend({id: 'abcd'}, angular.copy(controllerScope.dirtyOwner)));
          $httpBackend.flush();
          expect(controllerScope.error).toEqual('Server Error');

          // retry clears error
          controllerScope.save();
          expect(controllerScope.error).toBeFalsy();
        }));

        it('Success', inject(function($state, $httpBackend) {
          spyOn($state, 'go');

          $httpBackend.expectPOST('/rest/' + type + '/icon').respond('');
          saveDeferred.resolve(angular.extend({id: 'abcd'}, angular.copy(controllerScope.dirtyOwner)));
          $httpBackend.flush();
          $timeout.flush();

          expect($state.go).toHaveBeenCalledWith('management.' + type + '-view', type === 'application' ? {
            applicationPublicId: controllerScope.dirtyOwner.publicId
          } : {
            organizationId: 'abcd'
          });
          expect(controllerScope.$close).toHaveBeenCalled();
        }));
      });
    });
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
