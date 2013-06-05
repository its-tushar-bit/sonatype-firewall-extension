var clmTimestamp = '';

describe('ApplicationController', function () {
	var scope, httpBackend, rootScope, state, mockApplication;

	function toRegExp(getUrl) {
		return new RegExp(getUrl + '\\?timestamp=[0-9]+');
	}

	beforeEach(module('ApplicationModule'));
	beforeEach(inject(function ($httpBackend, $rootScope, $controller, $state, CLMLocations, CLMAppLocations) {
		httpBackend = $httpBackend;
		rootScope = $rootScope;
		clmLocations = CLMLocations;

		var applicationsData = ApplicationMockData.getApplicationsData();
		mockApplication = applicationsData[0];
		httpBackend.whenGET(toRegExp(CLMLocations.getApplicationsUrl())).respond(applicationsData);

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
	}));
});

describe('ApplicationEditorController', function () {
	var scope, httpBackend, rootScope, state, mockApplication, originalMockApplication, mockOrganization;

	function toRegExp(getUrl) {
		return new RegExp(getUrl + '\\?timestamp=[0-9]+');
	}

	beforeEach(module('ApplicationModule', 'Organization'));
	beforeEach(inject(function ($httpBackend, $rootScope, $controller, $state, CLMLocations, CLMAppLocations) {
		httpBackend = $httpBackend;
		rootScope = $rootScope;
		clmLocations = CLMLocations;

		var applicationsData = ApplicationMockData.getApplicationsData();
		mockApplication = applicationsData[0];
		httpBackend.whenGET(toRegExp(CLMLocations.getApplicationsUrl())).respond(applicationsData);
		
		var organizationData = OrganizationMockData.getGETResponse();
		mockOrganization = organizationData[0];
        httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(organizationData);

		scope = $rootScope.$new();
		state = $state;
		
		originalMockApplication = angular.copy(mockApplication);
		scope.selectedApplication = mockApplication;
		scope.selectedApplication.$getOriginal = function() {
			return originalMockApplication;
		};

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
	
	it('saves an application', inject(function($httpBackend, CLMLocations) {
		scope.applicationEditor = {};
		scope.applicationEditor.$valid = true;
		
		scope.changeOrganization(mockOrganization);
		scope.selectedApplication.name = "newName";
		scope.generateIcon();
		
		$httpBackend.expectPUT(CLMLocations.getApplicationsUrl()).respond(mockApplication);
		
		var hasFormData = window.FormData;
		window.FormData = false;
		
		scope.save();
		
		$httpBackend.flush();
		
		window.FormData = hasFormData;
	}));
});