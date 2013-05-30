describe('dashboardApp', function () {
	var scope, state;

	beforeEach(module('dashboardApp'));
	beforeEach(inject(function ($rootScope, $state, $controller) {
		scope = $rootScope.$new();
		state = $state;
		
		$controller('dashboardController', { $scope: scope, $state: state });
	}));

	it('Adjusts the dashboard dropdown', inject(function($httpBackend) {
		$httpBackend.expectGET('../assets/management.html').respond('<div></div>');
		$httpBackend.expectGET('../application-assets/components/application-navigator.html').respond('<div></div>')
		
		scope.$apply(function() {
			state.current.name = 'management.application';
		});
		
		expect(scope.selectedDashboard).not.toBeUndefined();
		expect(scope.selectedDashboard.name).toEqual('Management');
	}));
});