function InsightPolicyController($scope, global, $http, $location) {
	$scope.state = global;
	
	$scope.reset = function() {
		delete $scope.state.addConstraintName;
		delete $scope.state.addConstraintOperand;
		delete $scope.state.addConstraintOperator;
		delete $scope.state.addConstraintValue;
		delete $scope.state.addConstraintFormValid;
		delete $scope.state.addConstraintConditionFormValid;
		delete $scope.state.currentConstraint;
		
		$scope.state.constraintConditionList = [];
		$scope.state.addConstraintMatchType = 'OR';
	}
	
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
		var rows = $scope.constraintGrid.getSelectedRows();
		
		if (!rows.length > 0){
			delete rows;
			return;
		}
		
		angular.forEach(rows, function(value, key){
			$scope.constraintGrid.getDataItem(value).enabled = enabled;
		});
	}
	
	$scope.removeConstraint = function() {		
		var rows = $scope.constraintGrid.getSelectedRows();
		
		if (!rows.length > 0){
			return;
		}
		
		rows.reverse();
		
		angular.forEach(rows, function(value, key){
			$scope.constraintGrid.dataView.deleteItem($scope.constraintGrid.getDataItem(value).id);
		});
	}
	
	$scope.addConstraintClick = function() {
		$scope.state.constraintList.push({
		    name: $scope.state.addConstraintName,
		    conditions: $scope.state.constraintConditionList.slice(0),
		    matchType: $scope.state.addConstraintMatchType,
		    enabled: true,
		    //TODO: this will ultimately come from the server
		    id: $scope.state.addConstraintName
		});	
		
		$('#newConstraintModal').modal('hide');
		$scope.reset();
	}
	
	$scope.addConstraintCondition = function() {
		$scope.state.constraintConditionList.push({
			operand: $scope.state.addConstraintOperand,
			operator: $scope.state.addConstraintOperator,
			value: $scope.state.addConstraintValue
		});
		
		delete $scope.state.addConstraintOperand;
		delete $scope.state.addConstraintOperator;
		delete $scope.state.addConstraintValue;
		
		$scope.validateConstraint();
		$scope.validateConstraintCondition();
	}
	
	$scope.removeConstraintCondition = function() {
		var grid = $scope.constraintConditionsGrid;
		var rows = grid.getSelectedRows();
		for ( var i = rows.length - 1 ; i >= 0 ; i-- ) {
			grid.dataView.deleteItem(grid.dataView.getItemByIdx(rows[i]).id);
		}
		$scope.state.constraintConditionList = grid.getData().getItems();
		$scope.validateConstraint();
	}
	
	$scope.validateConstraint = function() {
		delete $scope.state.addConstraintFormValid;
		if ($scope.state.constraintConditionList.length > 0
				&& $scope.state.addConstraintName
				&& $scope.state.addConstraintMatchType) {
			$scope.state.addConstraintFormValid = true;
		}
	}
	
	$scope.validateConstraintCondition = function() {
		delete $scope.state.addConstraintConditionFormValid;
		if ($scope.state.addConstraintOperand
				&& $scope.state.addConstraintOperator
				&& (!$scope.state.addConstraintOperand.requiresValue || $scope.state.addConstraintValue)) {
			$scope.state.addConstraintConditionFormValid = true;
		}
	}
	
	$scope.constraintOperandChanged = function() {
		delete $scope.state.addConstraintOperator;
		delete $scope.state.addConstraintValue;
		$scope.validateConstraintCondition();
	}
	
	$scope.reset();
	
	$http.get(insightApp.getConditionTypeUrl()).success(function(conditionTypeData, status, headers, config) {
    	$scope.state.conditionTypes = conditionTypeData;
    });
	
	$('#newConstraintModal').live('show', function (event) {
		setTimeout(function(){
			$scope.constraintConditionsGrid.redraw();
		},100);
    });
}