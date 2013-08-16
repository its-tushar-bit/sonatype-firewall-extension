describe('Tests for the OrganizationController', function() {
  
  beforeEach(module('OrganizationModule', function($provide) {
    $provide.value('OrganizationId', {
      encoded: function() {
        return '1';
      }
    });
    $provide.value('hudson', ['$http', function($http) {
        return $http;
      }]
    );
  }));

  describe('OrganizationController', function() {
    var scope, httpBackend, rootScope, state, mockOrganization;

    beforeEach(inject(function($httpBackend, $rootScope, $controller, $state, CLMAppLocations) {
      httpBackend = $httpBackend;
      rootScope = $rootScope;

      $state.current.name = 'management.organization';

      var organizationsData = OrganizationMockData.getGETResponse();
      mockOrganization = organizationsData[0];
      httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getEntitiesUrl())).respond(organizationsData);

      scope = $rootScope.$new();
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

    it('switches organizations.', inject(function($timeout) {
      expect(scope.selectedOrganization).toEqual(null);
      scope.$apply(function() {
        state.params.organizationId = '2';
      });
      $timeout.flush();
      expect(scope.selectedOrganization).not.toBeUndefined();
      expect(scope.selectedOrganization.name).toEqual('org2');
    }));

    it('switch to new organization', inject(function($timeout) {
      expect(scope.selectedOrganization).toEqual(null);
      scope.$apply(function() {
        state.params.organizationId = '_new_';
      });
      $timeout.flush();
      expect(scope.selectedOrganization).not.toBeUndefined();
      expect(scope.selectedOrganization.name).toEqual(null);
    }));

    it('passes through alerts', inject(function($state, $httpBackend) {
      $httpBackend.expectGET('../assets/management.html?').respond('<div></div>');
      $httpBackend.expectGET('../organization-assets/components/organization-navigator.html?').respond('<div></div>');
      $httpBackend.expectGET('../organization-assets/components/organization-editor.html?').respond('<div></div>');

      $state.transitionTo('management.organization.view');

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
    var scope, httpBackend, rootScope, state, mockOrganization, originalMockOrganization;

    beforeEach(inject(function($httpBackend, $rootScope, $controller, $state, CLMLocations, CLMAppLocations,
                               OrganizationStore)
    {
      httpBackend = $httpBackend;
      rootScope = $rootScope;

      $state.current.name = 'management.organization';

      var organizationsData = OrganizationMockData.getGETResponse();
      httpBackend.whenGET(SpecUtil.toRegExp(CLMLocations.getOrganizationsUrl())).respond(organizationsData);
      originalMockOrganization = organizationsData[0];

      scope = $rootScope.$new();
      state = $state;

      scope.selectedOrganization = OrganizationStore.create();
      scope.selectedOrganization.$updateOriginal(originalMockOrganization);

      scope.organizations = [originalMockOrganization];

      $controller('OrganizationEditorController', {
        $scope: scope,
        $state: state
      });
    }));

    afterEach(function() {
      httpBackend.verifyNoOutstandingExpectation();
      httpBackend.verifyNoOutstandingRequest();
      scope.$destroy();
    });

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

      scope.cancelClick();

      expect(angular.equals(scope.selectedOrganization, originalMockOrganization)).toBeTruthy();
      expect(scope.hasRobotSource).not.toBeTruthy();
      expect(scope.iconChanged).not.toBeTruthy();
    });

    it('saves an organization', inject(function(CLMAppLocations) {
      scope.organizationEditor = {};
      scope.organizationEditor.$valid = true;

      scope.selectedOrganization.name = "newName";
      scope.selectedOrganization.id = null;
      scope.generateIcon();

      httpBackend.expectPOST(SpecUtil.toRegExp(CLMAppLocations.getEntitiesUrl())).respond(mockOrganization);

      var hasFormData = window.FormData;
      window.FormData = false;

      scope.saveClick();

      httpBackend.flush();

      window.FormData = hasFormData;
    }));

    it('validates organization name', inject(function() {
      scope.organizationEditor = {
        $valid: true
      };
      scope.selectedOrganization = {
        "id": "4",
        "name": "org4"
      };

      expect(scope.validateName('org1')).toEqual('Name is already in use');
      expect(scope.organizationEditor.$invalid).toBeTruthy();

      expect(scope.validateName('new name')).toBeUndefined();
      expect(scope.organizationEditor.$invalid).toBeFalsy();

      expect(scope.validateName('new  name')).toBeDefined();
      expect(scope.organizationEditor.$invalid).toBeTruthy();

      expect(scope.validateName(' new name')).toBeDefined();
      expect(scope.organizationEditor.$invalid).toBeTruthy();

      expect(scope.validateName('new name ')).toBeDefined();
      expect(scope.organizationEditor.$invalid).toBeTruthy();
    }));

    it('adds an organization', inject(function(CLMLocations, OrganizationStore) {
      scope.$state.params.organizationId = '_new_';
      //create a new Organization so that we're not updating the existing one
      scope.selectedOrganization = OrganizationStore.create();
      scope.selectedOrganization.name = 'name';
      scope.organizationEditor = {}

      httpBackend.expectPOST(SpecUtil.toRegExp(CLMLocations.getOrganizationsUrl()), {
        id: null,
        name: 'name'
      }).respond(OrganizationMockData.getPOSTResponse('name'));

      var hasFormData = window.FormData;
      window.FormData = false;

      scope.saveClick();

      httpBackend.expectGET('../assets/management.html?').respond('<div></div>');
      httpBackend.expectGET('../organization-assets/components/organization-navigator.html?').respond('<div></div>');
      httpBackend.expectGET('../organization-assets/components/organization-editor.html?').respond('<div></div>');
      httpBackend.expectGET('../policy-assets/components/policy/policy.html?').respond('<div></div>');

      httpBackend.flush();

      window.FormData = hasFormData;
    }));

    it('Can delete an organization and broadcast that it has happened', inject(function(CLMAppLocations, OrganizationStore) {
      var spy = spyOn(rootScope, '$broadcast').andReturn({defaultPrevented: false});

      httpBackend.expectDELETE(CLMAppLocations.getEntityUrl()).respond({});
      httpBackend.expectGET('../assets/management.html?').respond('<div></div>');
      httpBackend.expectGET('../organization-assets/components/organization-navigator.html?').respond('<div></div>');

      expect(angular.element('#deleteOrganizationModel').css('display')).toBeUndefined();

      scope.confirmDeleteOrganization(scope.selectedOrganization);

      expect(scope.deletedEnabled).toBeTruthy();
      expect(angular.element('#deleteOrganizationModel').css('display')).not.toBe('none');

      scope.deleteOrganization();

      httpBackend.flush();

      expect(angular.element('#deleteOrganizationModel').css('display')).toBeUndefined();
      expect(spy).toHaveBeenCalledWith('organizations.delete', originalMockOrganization.id);
      expect(scope.deletedEnabled).toBeFalsy();
    }));

    it('Can respond to errors when trying to delete an organization', inject(function(CLMAppLocations) {
      var spy = spyOn(rootScope, '$broadcast').andReturn({defaultPrevented: false});

      scope.organizations = [originalMockOrganization];
      httpBackend.expectDELETE(CLMAppLocations.getEntityUrl()).respond(400);

      expect(angular.element('#deleteOrganizationModel').css('display')).toBeUndefined();

      scope.confirmDeleteOrganization(scope.selectedOrganization);

      expect(scope.deletedEnabled).toBeTruthy();
      expect(angular.element('#deleteOrganizationModel').css('display')).not.toBe('none');

      scope.deleteOrganization();

      httpBackend.flush();

      expect(angular.element('#deleteOrganizationModel').css('display')).toBeUndefined();
      expect(spy).toHaveBeenCalledWith('showServerError', jasmine.any(Object));
      expect(scope.deletedEnabled).toBeFalsy();
    }));
  });
});
