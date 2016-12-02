describe('owner.editor.controller.spec.js', function() {
  var controllerScope,
      vm;

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  afterEach(function() {
    controllerScope.$destroy();
  });

  function createTests(type) {
    describe('New Owner: ' + type, function() {
      var ownerResource;

      beforeEach(inject(function($controller, $rootScope, $q) {
        ownerResource = {
          $new: true,
          $save: angular.noop,
          isDirty: angular.noop,
          $clone: angular.noop
        };

        controllerScope = $rootScope.$new();
        controllerScope.$dismiss = jasmine.createSpy('dismiss');
        controllerScope.$close = jasmine.createSpy('close');

        vm = $controller('owner.editor.controller', {
          $scope: controllerScope,
          owner: ownerResource,
          ownerType: type,
          siblings: []
        });

        vm.ownerEditorMask = {wrap: SpecUtil.promiseWrapper($q)};
        vm.ownerEditor = {
          name: { $setPristine: jasmine.createSpy() }
        };
      }));

      describe('Page Changes', function() {
        it('clean', function() {
          spyOn(vm.dirtyOwner, 'isDirty').and.returnValue(false);

          SpecUtil.expectStateChangeNotPrevented(controllerScope);
          expect(vm.unsavedModalVisible).toBeFalsy();
          expect(vm.dirtyOwner.isDirty).toHaveBeenCalled();
        });

        it('dirty', function() {
          spyOn(vm.dirtyOwner, 'isDirty').and.returnValue(true);

          SpecUtil.expectStateChangePrevented(controllerScope);
          expect(vm.unsavedModalVisible).toBeTruthy();
          expect(vm.dirtyOwner.isDirty).toHaveBeenCalled();
        });

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

          spyOn(vm.dirtyOwner, '$save').and.returnValue(saveDeferred.promise);

          controllerScope.$apply(function() {
            vm.dirtyOwner.name = 'My new ' + type;
            if (type === 'application') {
              vm.dirtyOwner.publicId = 'my-new';
            }
          });
          expect(ownerResource.name).toEqual('My new ' + type); // new objects work with the original

          vm.save();
        }));

        it('Error on Owner', function() {
          saveDeferred.reject('foobar');
          $timeout.flush();
          expect(vm.error).toEqual('foobar');

          // retry clears error
          vm.save();
          expect(vm.error).toBeFalsy();
        });

        it('Error on Icon', inject(function($state, $httpBackend) {
          spyOn($state, 'go');
          vm.dirtyOwner.publicId = 'abcd';
          vm.dirtyOwner.id = 'abcd';
          $httpBackend.expectPOST('/rest/' + type + '/icon').respond(500, 'Server Error');
          saveDeferred.resolve(angular.extend({id: 'abcd'}, angular.copy(vm.dirtyOwner)));
          $httpBackend.flush();
          $timeout.flush();

          expect(vm.error).toBeUndefined();
          expect(vm.iconWarning).toEqual('Server Error');

          expect($state.go).toHaveBeenCalledWith('management.view.' + type, type === 'application' ? {
            applicationPublicId: vm.dirtyOwner.publicId
          } : {
            organizationId: 'abcd'
          });

          // retry clears the error
          vm.save();
          expect(vm.iconWarning).toBeFalsy();
        }));

        it('Success', inject(function($state, $httpBackend) {
          spyOn($state, 'go');

          $httpBackend.expectPOST('/rest/' + type + '/icon').respond('');
          saveDeferred.resolve(angular.extend({id: 'abcd'}, angular.copy(vm.dirtyOwner)));
          $httpBackend.flush();
          $timeout.flush();

          expect(vm.ownerEditor.name.$setPristine).toHaveBeenCalled();
          expect($state.go).toHaveBeenCalledWith('management.view.' + type, type === 'application' ? {
            applicationPublicId: vm.dirtyOwner.publicId
          } : {
            organizationId: 'abcd'
          });
          expect(controllerScope.$close).toHaveBeenCalled();
        }));

        it('ContactInternalName properly set', inject(function() {
          expect(vm.dirtyOwner.contactInternalName).toBeUndefined();

          saveDeferred.reject('retry with contact');
          $timeout.flush();

          if (type === 'application') {
            vm.dirtyOwner.contact = {internalName: 'internalName'};

            vm.save();
            expect(vm.dirtyOwner.contactInternalName).toEqual('internalName');
          }
        }));
      });
    });
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
