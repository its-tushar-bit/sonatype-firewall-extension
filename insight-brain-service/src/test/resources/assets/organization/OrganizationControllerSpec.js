describe('Tests for the OrganizationController', function() {

  beforeEach(module('OrganizationModule', 'HttpInterceptors', function($provide) {
    $provide.value('OrganizationId', {
      encoded: function() {
        return '1';
      }
    });
    $provide.value('ApplicationId', {
      encoded: function() {
        return;
      }
    });
  }));

  describe('OrganizationController', function() {
    var scope, httpBackend, rootScope, state, mockOrganization;

    beforeEach(inject(function($httpBackend, $rootScope, $controller, $state, CLMAppLocations) {
      httpBackend = $httpBackend;
      rootScope = $rootScope;

      $state.current.name = 'management.organization';

      var organizationsData = OrganizationMockData.getGETResponse();
      mockOrganization = organizationsData[0];
      httpBackend.expectGET(SpecUtil.toRegExp(CLMAppLocations.getEntitiesUrl())).respond(organizationsData);

      scope = $rootScope.$new();

      scope.aoEditorName = {
        $save : angular.noop
      };
      state = $state;

      $controller('OrganizationController', {
        $scope: scope,
        $state: state
      });

      httpBackend.flush();
    }));

    afterEach(function() {
      httpBackend.verifyNoOutstandingExpectation();
      httpBackend.verifyNoOutstandingRequest();
      scope.$destroy();
    });

    it('loads organizations.', function() {
      expect(scope.organizations).not.toBeUndefined();
      expect(scope.organizations.length).toEqual(3);
      expect(scope.organizations[0].id).toEqual('1');
      expect(scope.organizations[0].name).toEqual('org1');
      expect(scope.organizations[1].id).toEqual('2');
      expect(scope.organizations[1].name).toEqual('org2');
      expect(scope.organizations[2].id).toEqual('3');
      expect(scope.organizations[2].name).toEqual('org3');
    });

    it('passes through alerts', inject(function($state, $httpBackend) {
      $httpBackend.expectGET('../assets/management.html?').respond('<div></div>');
      $httpBackend.expectGET('../organization-assets/components/organization-navigator.html?').respond('<div></div>');
      $httpBackend.expectGET('../application-assets/components/aoeditor.html?').respond('<div></div>');

      $state.transitionTo('management.organization.view', {
        organizationId : '_new_'
      });

      $httpBackend.flush();

      expect($state.current.data.passThroughAlerts).not.toBeUndefined();
      expect($state.current.data.passThroughAlerts.length).toEqual(0);
      $state.current.data.passThroughAlerts.push({ type: 'error', msg: 'orgtest'});

      $httpBackend.expectGET('../policy-assets/components/policy/policy.html?').respond('<div></div>');

      $state.transitionTo('management.organization.view.policies', { organizationId: 'ID' });

      $httpBackend.flush();

      expect($state.current.data.passThroughAlerts).not.toBeUndefined();
      expect($state.current.data.passThroughAlerts.length).toEqual(1);
      expect($state.current.data.passThroughAlerts[0].msg).toEqual('orgtest');
      expect($state.current.data.passThroughAlerts[0].type).toEqual('error');
    }));
  });

  describe('OrganizationEditorController', function() {
    var scope, parentScope, httpBackend, rootScope, state, mockOrganization, originalMockOrganization;

    afterEach(inject(function($httpBackend) {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
      if (parentScope) {
        parentScope.$destroy();
      } else if (scope) {
        scope.$destroy();
      }
    }));

    describe('Missing Organization', function () {
      beforeEach(inject(function ($controller, $rootScope) {
        var state = {
          params : {
            organizationId : 'foo'
          },
          current : {}
        };

        parentScope = $rootScope.$new();
        parentScope.organizations = [];
        scope = parentScope.$new();

        $controller('OrganizationEditorController', {
          $scope: scope,
          $state: state,
          selectedOrganization : null
        });
      }));

      it('Simple', function () {
        expect(scope.ao).toBeDefined();
        expect(scope.ao.selected).toBeFalsy();
        expect(scope.ao.getPublicId()).toEqual('foo');
      });
    });

    describe('', function () {
      beforeEach(inject(function($httpBackend, $rootScope, $controller, $state, CLMLocations, CLMAppLocations, OrganizationStore) {
        httpBackend = $httpBackend;
        rootScope = $rootScope;

        $state.current.name = 'management.organization';

        var organizationsData = OrganizationMockData.getGETResponse();
        httpBackend.whenGET(SpecUtil.toRegExp(CLMLocations.getOrganizationsUrl())).respond(organizationsData);
        originalMockOrganization = organizationsData[0];

        parentScope = $rootScope.$new();
        scope = parentScope.$new();
        scope.aoEditorName = {
          $save : angular.noop
        };
        state = $state;

        var selectedOrganization = OrganizationStore.create();
        selectedOrganization.$updateOriginal(originalMockOrganization);

        parentScope.organizations = [originalMockOrganization];
        parentScope.organizationIconTimestamp = {}

        $controller('OrganizationEditorController', {
          $scope: scope,
          $state: state,
          selectedOrganization : selectedOrganization
        });
      }));

      it('generates an icon', function() {
        scope.generateIcon();

        expect(scope.robotHash).not.toBeUndefined();
        expect(scope.robotHash).not.toEqual('');
        expect(scope.hasRobotSource).toBeTruthy();

        // After first robohash is generated using the name, a random should be created next
        var robotHash = scope.robotHash;
        scope.generateIcon();
        expect(scope.robotHash).not.toEqual(robotHash);
      });

      it('checks if the form is dirty', function() {
        var isDirty = scope.isFormDirty();
        expect(isDirty).not.toBeTruthy();

        var originalName = scope.selectedOrganization.name;
        scope.selectedOrganization.name = "newName";
        isDirty = scope.isFormDirty();
        expect(isDirty).toBeTruthy();

        scope.selectedOrganization.name = originalName;
        isDirty = scope.isFormDirty();
        expect(isDirty).not.toBeTruthy();

        scope.generateIcon();
        isDirty = scope.isFormDirty();
        expect(isDirty).toBeTruthy();
      });

      it('cancels edits', function() {
        scope.selectedOrganization.name = "newName";
        scope.generateIcon();

        expect(scope.hasRobotSource).toBeTruthy();
        expect(scope.iconChanged).toBeTruthy();

        scope.cancel();

        expect(angular.equals(scope.selectedOrganization, originalMockOrganization)).toBeTruthy();
        expect(scope.hasRobotSource).not.toBeTruthy();
        expect(scope.iconChanged).not.toBeTruthy();
      });

      it('saves an organization', inject(function(CLMAppLocations) {
        scope.aoEditor = {
                $valid : true
        };

        scope.selectedOrganization.name = "newName";
        scope.selectedOrganization.id = null;
        scope.generateIcon();

        httpBackend.expectPOST(SpecUtil.toRegExp(CLMAppLocations.getEntitiesUrl())).respond(mockOrganization);

        var hasFormData = window.FormData;
        window.FormData = false;

        scope.save();

        httpBackend.flush();

        window.FormData = hasFormData;
      }));

      it('adds an organization', inject(function(CLMLocations, OrganizationStore) {
        scope.$state.params.organizationId = '_new_';
        //create a new Organization so that we're not updating the existing one
        scope.selectedOrganization = OrganizationStore.create();
        scope.selectedOrganization.name = 'name';
        scope.aoEditor = {};

        httpBackend.expectPOST(SpecUtil.toRegExp(CLMLocations.getOrganizationsUrl()), {
          id: null,
          name: 'name'
        }).respond(OrganizationMockData.getPOSTResponse('name'));

        var hasFormData = window.FormData;
        window.FormData = false;

        scope.save();

        httpBackend.expectGET('../assets/management.html?').respond('<div></div>');
        httpBackend.expectGET('../organization-assets/components/organization-navigator.html?').respond('<div></div>');
        httpBackend.expectGET('../application-assets/components/aoeditor.html?').respond('<div></div>');
        httpBackend.expectGET('../policy-assets/components/policy/policy.html?').respond('<div></div>');

        httpBackend.flush();

        window.FormData = hasFormData;
      }));

      it('Can delete an organization and broadcast that it has happened', inject(function(CLMAppLocations, OrganizationStore, $modal, $q) {
        var spy = spyOn(rootScope, '$broadcast').andReturn({defaultPrevented: false}),
        modalDeferred = $q.defer(),
        modalSpy = spyOn($modal, 'open').andReturn({
          result : modalDeferred.promise
        });

        httpBackend.expectGET('../assets/management.html?').respond('<div></div>');
        httpBackend.expectGET('../organization-assets/components/organization-navigator.html?').respond('<div></div>');

        scope.confirmDelete();

        expect(modalSpy).toHaveBeenCalled();
        expect(modalSpy.mostRecentCall.args[0].resolve.selected()).toEqual(scope.selectedOrganization);
        expect(modalSpy.mostRecentCall.args[0].templateUrl).toEqual('delete-org-modal');

        scope.$apply(function () {
          modalDeferred.resolve();
        });
        httpBackend.flush();

        expect(spy).toHaveBeenCalledWith('organizations.delete', originalMockOrganization.id);
      }));

      it('Can respond to errors when trying to delete an organization', inject(function(CLMAppLocations, $modal, $q) {
        var spy = spyOn(scope, '$broadcast').andReturn({defaultPrevented: false}),
        modalDeferred = $q.defer(),
        modalSpy = spyOn($modal, 'open').andReturn({
          result : modalDeferred.promise
        });
        scope.confirmDelete();

        expect(modalSpy).toHaveBeenCalled();
        expect(modalSpy.mostRecentCall.args[0].resolve.selected()).toEqual(scope.selectedOrganization);
        expect(modalSpy.mostRecentCall.args[0].templateUrl).toEqual('delete-org-modal');

        scope.$apply(function () {
          modalDeferred.reject(['foo', 400, null, null]);
        });
        expect(spy).toHaveBeenCalledWith('showServerError', ['foo', 400, null, null]);
      }));

      it('displays confirmation dialog when navigating away from edited data', function() {
        scope.selectedOrganization.name = 'new_name';
        var e = scope.$broadcast('pageChangeStarted');
        expect(e.defaultPrevented).toBeTruthy();

        scope.selectedOrganization.name = originalMockOrganization.name;
        e = scope.$broadcast('pageChangeStarted');
        expect(e.defaultPrevented).not.toBeTruthy();

        scope.generateIcon();
        var e = scope.$broadcast('pageChangeStarted');
        expect(e.defaultPrevented).toBeTruthy();

        scope.cancel();
        e = scope.$broadcast('pageChangeStarted');
        expect(e.defaultPrevented).not.toBeTruthy();

        scope.selectedOrganization.name = 'new_name';
        e = scope.$broadcast('pageChangeStarted', 'organization/' + scope.selectedOrganization.id);
        expect(e.defaultPrevented).not.toBeTruthy();
      });

      it('does not cancel edits when changing between tabs', function() {
        scope.selectedOrganization.name = 'new_name';
        e = scope.$broadcast('pageChangeAccepted', 'organization/' + scope.selectedOrganization.id);
        expect(scope.selectedOrganization.name).toEqual('new_name');

        e = scope.$broadcast('pageChangeAccepted', 'organization/');
        expect(scope.selectedOrganization.name).toEqual(originalMockOrganization.name);
      });
    });
  });
});
