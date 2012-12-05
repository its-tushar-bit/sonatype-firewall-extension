function InsightNavController($scope, global, $http, $location) {
	$scope.state = global;
	
	$scope.policyTabClick = function() {
		$scope.state.policyTabCls = 'active';
		$scope.state.labelTabCls = '';
		$scope.state.licenseGroupTabCls = '';
	}
	
	$scope.labelTabClick = function() {
		$scope.state.policyTabCls = '';
		$scope.state.labelTabCls = 'active';
		$scope.state.licenseGroupTabCls = '';
	}
	
	$scope.licenseGroupTabClick = function() {
		$scope.state.policyTabCls = '';
		$scope.state.labelTabCls = '';
		$scope.state.licenseGroupTabCls = 'active';
	}
}