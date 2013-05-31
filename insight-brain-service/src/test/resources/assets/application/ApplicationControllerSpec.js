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