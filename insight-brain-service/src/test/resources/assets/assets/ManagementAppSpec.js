describe('dashboardApp', function () {
	var scope, state;

	beforeEach(module('dashboardApp'));
	beforeEach(module(function($provide) {
		$provide.value('licenseChecker', { check : function(){} });
	}));
	beforeEach(inject(function ($rootScope, $state, $controller, $httpBackend) {
		scope = $rootScope.$new();
		state = $state;
		
		$controller('dashboardController', { $scope: scope, $state: state });
		
		$httpBackend.expectGET('../assets/management.html').respond('<div></div>');
		$httpBackend.expectGET('../application-assets/components/application-navigator.html').respond('<div></div>');
	
	}));

	it('Adjusts the dashboard dropdown', function() {	
		scope.$apply(function() {
			state.current.name = 'management.application';
		});
		
		expect(scope.selectedDashboard).not.toBeUndefined();
		expect(scope.selectedDashboard.name).toEqual('Management');
	});
	
	it('Creates states for different panes', inject(function($httpBackend) {
		// transitionTo returns a promise. scope.$apply does not flush this promise and therefore the transition will not occur without creating
		// a mock $q framework. transitionTo will throw an error if the state does not exist, however, so the lack of an error indicates test success
		state.transitionTo('management.application', {});
		
		$httpBackend.expectGET('../organization-assets/components/organization-navigator.html').respond('<div></div>');
		state.transitionTo('management.organization', {});
	}));
	
	it('Validate location change event is broadcast properly', inject(function($rootScope) {
        var successStart = false, successAccept = false;
        $rootScope.$on('pageChangeStarted', function(event, destination){
            successStart = true;
        });
        $rootScope.$on('pageChangeAccepted', function(event, destination){
            successAccept = true;
        });
        
        $rootScope.$broadcast('$locationChangeStart', 'http://www.cnn.com', 'http://www.google.com');
        
        waitsFor(function() {
            return successStart;
        }, "pageChangeStarted event not properly retrieved", 1000);
        waitsFor(function() {
            return successAccept;
        }, "pageChangeAccepted event not properly retrieved", 1000);
    }));
});

describe('ManagementModule', function () {
	var scope;

	beforeEach(module('ManagementModule', 'AngularCommon'));
	beforeEach(inject(function ($rootScope, $state, $controller, commonCodeFactory) {
		scope = $rootScope.$new();
		
		$controller('ManagementController', { $scope: scope, $state: $state, commonCodeFactory: commonCodeFactory });
	}));

	it('Lists Org before App', function() {	
		expect(scope.managementPanes).not.toBeUndefined();
		expect(scope.managementPanes.length).toEqual(3);
		expect(scope.managementPanes[0].name).toEqual('Organizations');
		expect(scope.managementPanes[1].name).toEqual('Applications');
		expect(scope.managementPanes[2].name).toEqual('Configuration');
	});
});
