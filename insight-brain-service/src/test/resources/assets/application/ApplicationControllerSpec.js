var clmTimestamp = '';

describe('ApplicationController', function () {
	var scope, httpBackend, rootScope, state, mockApplication;

	function toRegExp(getUrl) {
		return new RegExp(getUrl + '\\?timestamp=[0-9]+');
	}

	beforeEach(module('ApplicationModule'));
	beforeEach(module(function($provide) {
		$provide.value('ApplicationId', {
				encoded : function () {
					return 'bom1-12345678';
				}
			}
		);
	}));

	beforeEach(inject(function ($httpBackend, $rootScope, $controller, $state, CLMAppLocations) {
		httpBackend = $httpBackend;
		rootScope = $rootScope;

		$state.current.name = 'management.application';

		var applicationsData = ApplicationMockData.getApplicationsData();
		mockApplication = applicationsData[0];
		httpBackend.whenGET(toRegExp(CLMAppLocations.getEntitiesUrl())).respond(applicationsData);

		scope = $rootScope.$new();
		state = $state;

		$controller('applicationController', { $scope: scope, $state: state });

		httpBackend.flush();
	}));

	it('loads applications.', function () {
		expect(scope.applications).not.toBeUndefined();
		expect(scope.applications.length).toEqual(1);
		expect(scope.applications[0].publicId).toEqual('bom1-12345678');
	});
	
	it('switches applications.', inject(function($timeout) {
		expect(scope.selectedApplication).toEqual(null);
		scope.$apply(function() {
			state.params.applicationPublicId = 'bom1-12345678';
		});
		$timeout.flush();
		expect(scope.selectedApplication).not.toBeUndefined();
		expect(scope.selectedApplication.publicId).toEqual('bom1-12345678');
	}));

  it('switch to new application', inject(function($timeout) {
    expect(scope.selectedApplication).toEqual(null);
    scope.$apply(function() {
      state.params.applicationPublicId = '_new_';
    });
    $timeout.flush();
    expect(scope.selectedApplication).not.toBeUndefined();
    expect(scope.selectedApplication.publicId).toEqual(null);
  }));
});

describe('ApplicationEditorController', function () {
	var scope, httpBackend, rootScope, state, mockApplication, originalMockApplication, mockOrganization;

	function toRegExp(getUrl) {
		return new RegExp(getUrl + '\\?timestamp=[0-9]+');
	}

	beforeEach(module('ApplicationModule', 'OrganizationModule'));
	beforeEach(inject(function ($httpBackend, $rootScope, $controller, $state, CLMLocations, CLMAppLocations) {
		httpBackend = $httpBackend;
		rootScope = $rootScope;

		$state.current.name = 'management.application';

		var applicationsData = ApplicationMockData.getApplicationsData();
		mockApplication = applicationsData[0];
		httpBackend.whenGET(toRegExp(CLMAppLocations.getEntitiesUrl())).respond(applicationsData);

		var organizationData = OrganizationMockData.getGETResponse();
		mockOrganization = organizationData[0];
                httpBackend.expectGET(toRegExp(CLMLocations.getOrganizationsUrl())).respond(organizationData);

		scope = $rootScope.$new();
		state = $state;
		
		originalMockApplication = angular.copy(mockApplication);
		scope.selectedApplication = mockApplication;
		scope.selectedApplication.$getOriginal = function() {
			return originalMockApplication;
		};
                scope.applications = [mockApplication];

		$controller('applicationEditorController', { $scope: scope, $state: state });

		httpBackend.flush();
	}));
	
	it('generates an icon', function () {
		scope.generateIcon();

		expect(scope.robotHash).not.toBeUndefined();
		expect(scope.robotHash).not.toEqual('');
		expect(scope.hasRobotSource).toBeTruthy();
	});
	
	it('gets org name from id', function() {
		var name = scope.getOrganizationName(mockOrganization.id);
		expect(name).toEqual(mockOrganization.name);
	});
	
	it('updates an applications organization', function() {
		scope.changeOrganization(mockOrganization);
		expect(scope.selectedApplication.organizationId).toEqual(mockOrganization.id);
	});
	
	it('checks if the form is dirty', function() {
		var isDirty = scope.isFormDirty();
		expect(isDirty).not.toBeTruthy();
		
		var originalOrgId = scope.selectedApplication.organizationId;
		scope.changeOrganization(mockOrganization);
		isDirty = scope.isFormDirty();
		expect(isDirty).toBeTruthy();
		
		scope.selectedApplication.organizationId = originalOrgId;
		isDirty = scope.isFormDirty();
		expect(isDirty).not.toBeTruthy();
		
		var originalName = scope.selectedApplication.name;
		scope.selectedApplication.name = "newName";
		isDirty = scope.isFormDirty();
		expect(isDirty).toBeTruthy();
		
		scope.selectedApplication.name = originalName;
		isDirty = scope.isFormDirty();
		expect(isDirty).not.toBeTruthy();
		
		scope.generateIcon();
		isDirty = scope.isFormDirty();
		expect(isDirty).toBeTruthy();
	});
	
	it('cancels edits', inject(function($httpBackend) {
		scope.changeOrganization(mockOrganization);
		scope.selectedApplication.name = "newName";
		scope.generateIcon();
		
		$httpBackend.expectGET('../assets/management.html').respond('<div></div>');
		$httpBackend.expectGET('../application-assets/components/application-navigator.html').respond('<div></div>');
		
		scope.cancel();
		
		expect(angular.equals(scope.selectedApplication, originalMockApplication)).toBeTruthy();
	}));
	
	it('saves an application', inject(function($httpBackend, CLMAppLocations) {
		scope.applicationEditor = {};
		scope.applicationEditor.$valid = true;
		
		scope.changeOrganization(mockOrganization);
		scope.selectedApplication.name = "newName";
		scope.generateIcon();
		
		$httpBackend.expectPUT(CLMAppLocations.getEntitiesUrl()).respond(mockApplication);
		
		var hasFormData = window.FormData;
		window.FormData = false;
		
		scope.save();
		
		$httpBackend.flush();
		
		window.FormData = hasFormData;
	}));

	it('deletes an application', inject(function(CLMAppLocations) {
		httpBackend.expectDELETE(CLMAppLocations.getEntityUrl(mockApplication.publicId)).respond({});

		scope.confirmDeleteApplication(mockApplication);
		scope.deleteApplication();
	}));

  it('validates application name', inject(function () {
    scope.applicationEditor = {
      $valid: true
    };
    scope.selectedApplication = {
      "id": undefined,
      "name": "applicationName",
      "publicId": "bom1-12345678",
      "organizationId": "organizationId",
    };

    expect(scope.validateApplicationName('applicationName')).toEqual('Name is already in use');
    expect(scope.applicationEditor.$invalid).toBeTruthy();

    expect(scope.validateApplicationName('new name')).toBeUndefined();
    expect(scope.applicationEditor.$invalid).toBeFalsy();

    expect(scope.validateApplicationName('new  name')).toBeDefined();
    expect(scope.applicationEditor.$invalid).toBeTruthy();

    expect(scope.validateApplicationName(' new name')).toBeDefined();
    expect(scope.applicationEditor.$invalid).toBeTruthy();

    expect(scope.validateApplicationName('new name ')).toBeDefined();
    expect(scope.applicationEditor.$invalid).toBeTruthy();
  }));

  it('validates application id', inject(function() {
    scope.applicationEditor = {
      $valid: true
    };
    scope.selectedApplication = {
      "id": undefined,
      "name": "applicationName",
      "publicId": "bom1-12345678",
      "organizationId": "organizationId",
    };

    expect(scope.validateApplicationId('bom1-12345678')).toEqual('Id is already in use');
    expect(scope.applicationEditor.$invalid).toBeTruthy();

    expect(scope.validateApplicationId('new id')).toBeUndefined();
    expect(scope.applicationEditor.$invalid).toBeFalsy();

    expect(scope.validateApplicationId('_new_')).toEqual('This is a reserved value');
    expect(scope.applicationEditor.$invalid).toBeTruthy();

  }));
});