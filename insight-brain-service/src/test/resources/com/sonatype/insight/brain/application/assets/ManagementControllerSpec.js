var clmTimestamp = '';

describe('ApplicationManagementController', function () {
	var scope;

	function toRegExp(getUrl) {
		return new RegExp(getUrl + '\\?timestamp=[0-9]+');
	}

	angular.module('Hudson', []).factory('hudson', ['$http', function ($http) {
		return $http;
	}]);

	beforeEach(module('Management', 'AngularCommon', 'CLMLocation', 'Hudson'));
	beforeEach(inject(function ($httpBackend, $rootScope, $controller, CLMLocations) {
		$httpBackend.whenGET(toRegExp(CLMLocations.getApplicationsUrl())).respond(ApplicationMockData.getApplicationsData());
		$httpBackend.whenGET(toRegExp(CLMLocations.getActionStageUrl())).respond(MockData.getActionStageData());
		$httpBackend.whenGET(toRegExp(CLMLocations.getCanGetHashIcon())).respond("true");

		scope = $rootScope.$new();

		$controller('ManagementController', {$scope: scope});

		$httpBackend.flush();
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

	it('adds an application.', inject(function ($httpBackend, $rootScope, $controller, hudson, CLMLocations) {
		$httpBackend.expectPOST(CLMLocations.getApplicationsUrl()).respond(ApplicationMockData.getApplicationsData());

		$controller('ManagementController', {$scope: scope, hudson: hudson});

		scope.applicationPublicId = 'mockApplicationId';

		scope.addApplication();
	}));
});