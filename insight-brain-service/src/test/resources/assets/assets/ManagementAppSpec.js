describe('dashboardApp', function () {
	var scope, state;

	beforeEach(module('dashboardApp'));
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
});