/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import applicationSecurityModule from '../../../main/frontend/policy/AppSecurityController';
import { httpInterceptors } from '../../../main/frontend/util/HttpInterceptors';
import clmContextLocationModule from '../../../main/frontend/util/CLMContextLocation';

describe('AppSecurityControllerSpec', function () {
  var mockCLMContextLocations = {
    getRoleMappingUrl: function () {
      //NOTE /rest/ is actually required even in this fake path as one of the http interceptors
      //uses its presence in a conditional
      return 'http://localhost/rest/test-path/';
    },
  };

  beforeEach(
    angular.mock.module(
      applicationSecurityModule.name,
      httpInterceptors.name,
      clmContextLocationModule.name,
      function ($provide) {
        $provide.value('CLMContextLocations', mockCLMContextLocations);
      }
    )
  );

  describe('AppSecurityController', function () {
    var scope = null,
      parentScope = null,
      role1 = null,
      role2 = null;

    beforeEach(inject(function ($rootScope, $httpBackend, CLMContextLocations, $controller) {
      parentScope = $rootScope.$new();
      scope = parentScope.$new();
      role1 = MockData.getRoleOneData();
      role2 = MockData.getRoleTwoData();

      $httpBackend.expectGET(SpecUtil.toRegExp(CLMContextLocations.getRoleMappingUrl())).respond({
        membersByRole: [role1, role2],
      });
      $controller('AppSecurityController', {
        $scope: scope,
        isAuthorized: true,
      });
      $httpBackend.flush();
      expect(scope.context.roles.length).toEqual(2);
    }));

    afterEach(inject(function ($httpBackend) {
      parentScope.$destroy();
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    }));

    it('data is loaded into scope', function () {
      expect(scope.context.roles.length).toBe(2);
      expect(scope.context.roles[0].roleId).toBe('1da70fae1fd54d6cb7999871ebdb9a36');
      expect(scope.context.roles[0].membersByOwner.length).toBe(2);
      expect(scope.context.roles[0].membersByOwner[0].ownerId).toBe('bom1-12345678');
      expect(scope.context.roles[0].membersByOwner[0].members.length).toBe(2);
      expect(scope.context.roles[0].membersByOwner[0].members[0].internalName).toBe('admin');
    });

    it('validate roleSaveComplete event is handled properly', inject(function ($rootScope) {
      $rootScope.$broadcast('roleSaveComplete', role1.roleId, MockData.getRoleSaveCompleteEventMemberList());

      var found;

      for (var i = 0; i < scope.context.roles.length; i++) {
        if (scope.context.roles[i].roleId === role1.roleId) {
          expect(scope.context.roles[i].membersByOwner[0].members).toEqual(
            MockData.getRoleSaveCompleteEventMemberList()
          );
          found = true;
          break;
        }
      }

      expect(found).toEqual(true);
    }));
  });

  describe('AppSecurityEditorController', function () {
    var scope,
      dialogClickHandler,
      role1Id = MockData.getRoleOneData().roleId;

    beforeEach(inject(function ($rootScope, $controller) {
      scope = $rootScope.$new();
      scope.role = MockData.getRoleOneData();

      $controller('AppSecurityEditorController', {
        $scope: scope,
        Dialog: {
          //mock impl of Dialog that exposes the click handler of its first `primary` button
          open: function (params) {
            params.buttons.some(function (btn) {
              if (btn.type === 'primary') {
                dialogClickHandler = btn.click;
                return true;
              }
            });
          },
        },
      });

      //allow watcher to populate scope.originalMembers
      scope.$digest();
    }));

    afterEach(inject(function ($httpBackend) {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();

      if (scope) {
        scope.$destroy();
        scope = null;
      }

      dialogClickHandler = null;
    }));

    it('initially sets the alerts on its scope to []', function () {
      expect(scope.alerts).toEqual([]);
    });

    it('sets the originalMembers on its scope from the members of the first owner within the role', function () {
      var originalMembers = scope.originalMembers;

      expect(originalMembers.length).toBe(2);
      expect(originalMembers[0].internalName).toBe('admin');
      expect(originalMembers[1].internalName).toBe('plynch');
    });

    it('updates the originalMembers scope prop whenever the role is changed', function () {
      var originalMembers;

      scope.role = MockData.getRoleTwoData();
      scope.$digest();

      originalMembers = scope.originalMembers;

      expect(originalMembers.length).toBe(4);
      expect(originalMembers[0].internalName).toBe('bfox');
      expect(originalMembers[1].internalName).toBe('dbradicich');
      expect(originalMembers[2].internalName).toBe('jduggan');
      expect(originalMembers[3].internalName).toBe('jorlina');
    });

    it('prevents the default action when it receives a pageChangeStarted event and its isDirty method returns true', inject(function (
      $rootScope
    ) {
      var evt, evt2;

      scope.isDirty = function () {
        return true;
      };
      evt = $rootScope.$broadcast('pageChangeStarted');
      expect(evt.defaultPrevented).toBe(true);

      scope.isDirty = function () {
        return false;
      };
      evt2 = $rootScope.$broadcast('pageChangeStarted');
      expect(evt2.defaultPrevented).toBe(false);
    }));

    it('calls scope.hide when cancelled and not dirty', function () {
      scope.isDirty = function () {
        return false;
      };
      var hide = (scope.hide = jasmine.createSpy());

      scope.cancel();

      expect(hide).toHaveBeenCalled();
    });

    it('opens a dialog and then calls scope.hide when cancelled and dirty', function () {
      scope.isDirty = function () {
        return true;
      };
      var hide = (scope.hide = jasmine.createSpy());

      scope.cancel();

      expect(dialogClickHandler).toBeDefined();
      expect(hide).not.toHaveBeenCalled();

      dialogClickHandler();

      expect(hide).toHaveBeenCalled();
    });

    it('hides if save is called when not dirty', function () {
      scope.isDirty = function () {
        return false;
      };
      var hide = (scope.hide = jasmine.createSpy());

      scope.save();

      expect(hide).toHaveBeenCalled();
    });

    it('saves the roles if save is called when dirty, and then emits roleSaveComplete and hides', inject(function (
      $rootScope,
      $httpBackend
    ) {
      var currentMembers = MockData.getRoleTwoData().membersByOwner[0].members;
      scope.getCurrentMembersToSave = function () {
        return currentMembers;
      };
      scope.isDirty = function () {
        return true;
      };
      var hide = (scope.hide = jasmine.createSpy());
      var eventSpy = jasmine.createSpy();

      $rootScope.$on('roleSaveComplete', eventSpy);

      scope.save();

      $httpBackend.expectPUT(SpecUtil.toRegExp(mockCLMContextLocations.getRoleMappingUrl(role1Id))).respond();
      expect(hide).not.toHaveBeenCalled();
      expect(eventSpy).not.toHaveBeenCalled();

      $httpBackend.flush();

      expect(hide).toHaveBeenCalled();
      expect(eventSpy).toHaveBeenCalled();
      expect(eventSpy.calls.mostRecent().args[1]).toBe(role1Id);
      expect(eventSpy.calls.mostRecent().args[2]).toBe(currentMembers);
    }));
  });
});
