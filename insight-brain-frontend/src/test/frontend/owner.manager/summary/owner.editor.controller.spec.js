/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';
import OwnerUtils from '../owner.utils';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesRootSlice';

describe('owner.editor.controller.js', function () {
  var controllerScope, vm, originalFormData, form, setSelectedOwnerSpy;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
      $provide.value('$cookies', {
        get: angular.noop,
      });
    })
  );

  beforeEach(inject(function ($window) {
    originalFormData = $window.FormData;
    $window.FormData = angular.noop;
    form = angular.element('<form id="custom-icon-form"></form>');
    angular.element('body').append(form);
    setSelectedOwnerSpy = spyOn(rootActions, 'setSelectedOwner');
  }));

  afterEach(inject(function ($window) {
    controllerScope.$destroy();
    $window.FormData = originalFormData;
    form.remove();
  }));

  function createTests(type) {
    describe('New Owner: ' + type, function () {
      var ownerResource;

      beforeEach(inject(function ($controller, $rootScope, $q) {
        ownerResource = {
          $new: true,
          $save: angular.noop,
          isDirty: angular.noop,
          $clone: angular.noop,
        };

        controllerScope = $rootScope.$new();
        controllerScope.$dismiss = jasmine.createSpy('dismiss');
        controllerScope.$close = jasmine.createSpy('close');

        vm = $controller('owner.editor.controller', {
          $scope: controllerScope,
          owner: ownerResource,
          ownerType: type,
          siblings: [],
        });

        vm.ownerEditorMask = { wrap: SpecUtil.promiseWrapper($q) };
        vm.ownerEditor = {
          name: { $setPristine: jasmine.createSpy() },
        };
      }));

      describe('$onInit()', () => {
        it('subscribes to the redux store', () => {
          expect(vm.unsubscribe).toBeDefined();
        });
      });

      describe('$destroy()', () => {
        it('unsubscribes from the redux store', () => {
          expect(vm.unsubscribe).not.toHaveBeenCalled();

          controllerScope.$destroy();

          expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
        });
      });

      describe('Page Changes', function () {
        it('clean', function () {
          spyOn(vm.dirtyOwner, 'isDirty').and.returnValue(false);

          SpecUtil.expectStateChangeNotPrevented(controllerScope);
          expect(vm.unsavedModalVisible).toBeFalsy();
          expect(vm.dirtyOwner.isDirty).toHaveBeenCalled();
        });

        it('dirty', function () {
          spyOn(vm.dirtyOwner, 'isDirty').and.returnValue(true);

          SpecUtil.expectStateChangePrevented(controllerScope);
          expect(vm.unsavedModalVisible).toBeTruthy();
          expect(vm.dirtyOwner.isDirty).toHaveBeenCalled();
        });

        it('Closes', inject(function ($rootScope) {
          $rootScope.$broadcast('pageChangeAccepted');
          expect(controllerScope.$dismiss).toHaveBeenCalled();
        }));
      });

      describe('Save', function () {
        var saveDeferred,
          $timeout,
          publicId = 'publicId',
          id = 'id';

        beforeEach(inject(function ($q, _$timeout_) {
          $timeout = _$timeout_;
          saveDeferred = $q.defer();

          spyOn(vm.dirtyOwner, '$save').and.returnValue(saveDeferred.promise);

          controllerScope.$apply(function () {
            vm.dirtyOwner.name = 'My new ' + type;
            if (type === 'application') {
              vm.dirtyOwner.publicId = publicId;
            }
          });
          expect(ownerResource.name).toEqual('My new ' + type); // new objects work with the original

          vm.save();
        }));

        it('Error on Owner', function () {
          saveDeferred.reject('foobar');
          $timeout.flush();
          expect(vm.error).toEqual('foobar');

          // retry clears error
          vm.save();
          expect(vm.error).toBeFalsy();
        });

        it('Error on Icon', inject(function ($state, $httpBackend) {
          spyOn($state, 'go');
          vm.dirtyOwner.publicId = publicId;
          vm.dirtyOwner.id = id;
          $httpBackend.expectPOST('/rest/' + type + '/icon/' + id).respond(500, 'Server Error');
          saveDeferred.resolve(angular.extend({ id }, angular.copy(vm.dirtyOwner)));
          $httpBackend.flush();
          $timeout.flush();

          expect(setSelectedOwnerSpy).toHaveBeenCalledOnceWith(vm.dirtyOwner);
          expect(vm.error).toBeUndefined();
          expect(vm.iconWarning).toEqual('Server Error');

          expect($state.go).toHaveBeenCalledWith(
            'management.view.' + type,
            type === 'application'
              ? {
                  applicationPublicId: publicId,
                }
              : {
                  organizationId: id,
                }
          );

          // retry clears the error
          vm.save();
          expect(vm.iconWarning).toBeFalsy();
        }));

        it('Success', inject(function ($state, $httpBackend) {
          spyOn($state, 'go');

          $httpBackend.expectPOST('/rest/' + type + '/icon/' + id).respond('');
          const updatedOwner = angular.extend({ id }, angular.copy(vm.dirtyOwner));
          saveDeferred.resolve(updatedOwner);
          $httpBackend.flush();
          $timeout.flush();

          expect(setSelectedOwnerSpy).toHaveBeenCalledOnceWith(updatedOwner);
          expect(vm.ownerEditor.name.$setPristine).toHaveBeenCalled();
          expect($state.go).toHaveBeenCalledWith(
            'management.view.' + type,
            type === 'application'
              ? {
                  applicationPublicId: publicId,
                }
              : {
                  organizationId: id,
                }
          );
          expect(controllerScope.$close).toHaveBeenCalled();
        }));

        it('ContactInternalName properly set', inject(function () {
          expect(vm.dirtyOwner.contactInternalName).toBeUndefined();

          saveDeferred.reject('retry with contact');
          $timeout.flush();

          if (type === 'application') {
            vm.dirtyOwner.contact = { internalName: 'internalName' };

            vm.save();
            expect(vm.dirtyOwner.contactInternalName).toEqual('internalName');
          }
        }));
      });
    });
  }

  OwnerUtils.runTestsForOwnerTypes(createTests);
});
