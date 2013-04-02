var clmTimestamp = '';

describe('ApplicationManagementController', function () {
	var scope, httpBackend, rootScope, clmLocations, compile, mockApplication;

	function toRegExp(getUrl) {
		return new RegExp(getUrl + '\\?timestamp=[0-9]+');
	}

	angular.module('Hudson', []).factory('hudson', ['$http', function ($http) {
		return $http;
	}]);

	beforeEach(module('Management', 'AngularCommon', 'CLMLocation', 'Hudson'));
	beforeEach(inject(function ($httpBackend, $rootScope, $controller, hudson, CLMLocations, regexFactory, $compile) {
		httpBackend = $httpBackend;
		rootScope = $rootScope;
		clmLocations = CLMLocations;
		compile = $compile;

		var applicationsData = ApplicationMockData.getApplicationsData();
		mockApplication = applicationsData[0];
		httpBackend.whenGET(toRegExp(CLMLocations.getApplicationsUrl())).respond(applicationsData);
		httpBackend.whenGET(toRegExp(CLMLocations.getActionStageUrl())).respond(MockData.getActionStageData());
		httpBackend.whenGET(toRegExp(CLMLocations.getCanGetHashIcon())).respond("true");

		scope = $rootScope.$new();

		$controller('ManagementController', {$scope: scope, hudson: hudson});

		// Edit Controller needs an application to be selected
		scope.selectedApplication = { id: null, publicId: null, name: null };
		$controller('EditApplicationController', { $scope: scope, hudson: hudson, regexFactory: regexFactory});

		httpBackend.flush();
	}));

	it('loads applications.', function () {
		expect(scope.applications).not.toBeUndefined();
		expect(scope.applications.length).toEqual(1);
		expect(scope.applications[0].publicId).toEqual('bom1-12345678');
	});

	it('loads stages', function () {
		expect(scope.stages).not.toBeUndefined();
		expect(scope.stages.length).toEqual(5);
	});

	it('adds an application from the SaaS.', function () {
		httpBackend.expectPOST(clmLocations.getApplicationsUrl()).respond(ApplicationMockData.getApplicationsData());

		scope.applicationPublicId = 'mockApplicationId';

		scope.addApplication();
	});

	it('generates an icon', function () {
		scope.generateIcon();

		expect(scope.robotHash).not.toBeUndefined();
		expect(scope.robotHash).not.toEqual('');
		expect(scope.hasRobotSource).toBeTruthy();
	});

	it('adds an application', function () {
		var nameInput = angular.element("<input id='applicationName' name='applicationName' type='text' ng-model='selectedApplication.name' />");
		var idInput = angular.element("<input id='applicationPublicId' name='applicationPublicId' type='text' ng-model='selectedApplication.publicId' />");
		var body = angular.element('body').html("<form name='applicationEditor'></form>").find('form').append(nameInput).append(idInput);

		compile(body)(scope);

		scope.selectedApplication = { id: null, publicId: 'publicID', name: 'name' };

		httpBackend.expectPOST(clmLocations.getApplicationsUrl(), {
			applicationName: 'name',
			applicationPublicId: 'publicID'
		}).respond(ApplicationMockData.getApplicationsData());

		scope.saveClick();

		// Test client side application rules
		nameInput.val('!name');
		idInput.val('publicID');

		scope.saveClick();

		expect(scope.applicationEditor.applicationName.$valid).not.toBeTruthy();
		expect(scope.applicationEditor.applicationPublicId.$valid).toBeTruthy();

		nameInput.val('name');
		idInput.val('');

		scope.saveClick();

		expect(scope.applicationEditor.applicationName.$valid).toBeTruthy();
		expect(scope.applicationEditor.applicationPublicId.$valid).not.toBeTruthy();

		nameInput.val('  double  spaced');
		idInput.val('publicID');

		scope.saveClick();
		expect(scope.applicationEditor.applicationName.$valid).not.toBeTruthy();
		expect(scope.applicationEditor.applicationPublicId.$valid).toBeTruthy();
	});

	it('deletes an application', function () {
		httpBackend.expectDELETE(clmLocations.getApplicationUrl(mockApplication.publicId)).respond({});

		scope.confirmDeleteApplication(mockApplication);
		scope.deleteApplication();
	});
});