var clmTimestamp = '';

describe('ApplicationManagementController', function () {
	var scope, scopeEdit, httpBackend, rootScope, clmLocations;

	function toRegExp(getUrl) {
		return new RegExp(getUrl + '\\?timestamp=[0-9]+');
	}

	angular.module('Hudson', []).factory('hudson', ['$http', function ($http) {
		return $http;
	}]);

	beforeEach(module('Management', 'AngularCommon', 'CLMLocation', 'Hudson'));
	beforeEach(inject(function ($httpBackend, $rootScope, $controller, hudson, CLMLocations, regexFactory) {
		httpBackend = $httpBackend;
		rootScope = $rootScope;
		clmLocations = CLMLocations;

		httpBackend.whenGET(toRegExp(CLMLocations.getApplicationsUrl())).respond(ApplicationMockData.getApplicationsData());
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
});