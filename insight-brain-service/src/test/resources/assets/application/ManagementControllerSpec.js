var clmTimestamp = '';

describe('ApplicationManagementController', function () {
	var scope, httpBackend, rootScope, clmLocations, compile, mockApplication, sniffer;

	function toRegExp(getUrl) {
		return new RegExp(getUrl + '\\?timestamp=[0-9]+');
	}

    angular.module('ApplicationId',[]).service('ApplicationId', function () {
		return {
			encoded : 'bom1-12345678'
		};
    });

	angular.module('Hudson', []).factory('hudson', ['$http', function ($http) {
		return $http;
	}]);

	beforeEach(module('ApplicationId', 'ApplicationManagement', 'AngularCommon', 'CLMLocation', 'Hudson'));
	beforeEach(inject(function ($httpBackend, $rootScope, $controller, hudson, CLMLocations, regexFactory, $compile, $sniffer) {
		httpBackend = $httpBackend;
		rootScope = $rootScope;
		clmLocations = CLMLocations;
		compile = $compile;
		sniffer = $sniffer;

		var applicationsData = ApplicationMockData.getApplicationsData();
		mockApplication = applicationsData[0];
		httpBackend.whenGET(toRegExp(CLMLocations.getApplicationsUrl())).respond(applicationsData);
		httpBackend.whenGET(toRegExp(CLMLocations.getActionStageUrl())).respond(MockData.getActionStageData());
		httpBackend.whenGET(toRegExp(CLMLocations.getProfilesUrl())).respond([]);

		scope = $rootScope.$new();

		$controller('ApplicationManagementController', {$scope: scope, hudson: hudson});

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
		expect(scope.stages.length).toEqual(MockData.getActionStageData().length);
	});

	it('generates an icon', function () {
		scope.generateIcon();

		expect(scope.robotHash).not.toBeUndefined();
		expect(scope.robotHash).not.toEqual('');
		expect(scope.hasRobotSource).toBeTruthy();
	});

	it('adds an application', function () {
		var setInput = function (element, val) {
			element.val(val);

			var inputEvent = document.createEvent('HTMLEvents');
			inputEvent.initEvent((sniffer.hasEvent('input')) ? 'input' : 'change', false, false);
			element[0].dispatchEvent(inputEvent);
			
			var blurEvent = document.createEvent('HTMLEvents');
			blurEvent.initEvent('blur', false, false);
			element[0].dispatchEvent(blurEvent);
		};
		
		var nameInput = angular.element("<input id='applicationName' name='applicationName' type='text' ng-model='selectedApplication.name' required alpha-Numeric is-Duplicate is-Duplicate-Array='applications' is-Duplicate-Id-Field='id' is-Duplicate-Case-Sensitive='true' has-Whitespace='suggestedApplicationName'/>");
		var idInput = angular.element("<input id='applicationPublicId' name='applicationPublicId' type='text' ng-model='selectedApplication.publicId' required is-Duplicate is-Duplicate-Array='applications' is-Duplicate-Id-Field='id'/>");
		var body = angular.element('body').append("<form id='applicationEditor' name='applicationEditor'></form>").find('#applicationEditor').append(nameInput).append(idInput);

		compile(body)(scope);

		scope.selectedApplication = { id: null, publicId: 'publicID', name: 'name' };

		httpBackend.expectPOST(clmLocations.getApplicationsUrl(), {
			id: null,
			publicId: "publicID",
			name: "name"
		}).respond(ApplicationMockData.getApplicationsData());

		scope.saveClick();

		// Test client side application rules
		setInput(nameInput, '!name');
		setInput(idInput, 'publicID');

		expect(scope.applicationEditor.applicationName.$valid).not.toBeTruthy();
		expect(scope.applicationEditor.applicationPublicId.$valid).toBeTruthy();

		setInput(nameInput, 'name');
		setInput(idInput, '');

		expect(scope.applicationEditor.applicationName.$valid).toBeTruthy();
		expect(scope.applicationEditor.applicationPublicId.$valid).not.toBeTruthy();

		setInput(nameInput, '  double  spaced');
		setInput(idInput, 'publicID');

		expect(scope.applicationEditor.applicationName.$valid).not.toBeTruthy();
		expect(scope.applicationEditor.applicationPublicId.$valid).toBeTruthy();

		angular.element('#applicationEditor').remove();
	});

	it('deletes an application', function () {
		httpBackend.expectDELETE(clmLocations.getApplicationUrl(mockApplication.publicId)).respond({});

		scope.confirmDeleteApplication(mockApplication);
		scope.deleteApplication();
	});
});