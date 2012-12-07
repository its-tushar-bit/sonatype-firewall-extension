function InsightPolicyController($scope, global, $http, $location) {
	$scope.state = global;
	
	$scope.createPolicyClick = function(){
		$scope.state.showAddPolicyScreen = true;
	}
	
	$scope.savePolicyClick = function(){
		//TODO: save the policy
	}
	
	$scope.cancelPolicyClick = function(){
		//TODO: process the cancel
		delete $scope.state.showAddPolicyScreen;
	}
	
	$scope.validatePolicy = function() {
		delete $scope.state.policyValid;
		//TODO: put validations in place in place
		if ($scope.state.policyName) {
			$scope.state.policyValid = true;
		}
	}
	
	$scope.enableConstraint = function() {
		$scope.updateStatus(true);
	}
	
	$scope.disableConstraint = function() {
		$scope.updateStatus(false);
	}
	
	$scope.updateStatus = function(enabled) {
		var rows = $scope.policyConstraintGrid.getSelectedRows();
		
		if (!rows.length > 0){
			delete rows;
			return;
		}
		
		angular.forEach(rows, function(value, key){
			$scope.policyConstraintGrid.getDataItem(value).enabled = enabled;
		});
	}
	
	$scope.removeConstraint = function() {		
		var rows = $scope.policyConstraintGrid.getSelectedRows();
		
		if (!rows.length > 0){
			return;
		}
		
		rows.reverse();
		
		angular.forEach(rows, function(value, key){
			$scope.policyConstraintGrid.dataView.deleteItem($scope.policyConstraintGrid.getDataItem(value).id);
		});
	}
}