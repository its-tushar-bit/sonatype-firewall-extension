function RuleController($scope, global) {
	$scope.state = global;
	
	$scope.addRule = function() {
		$scope.state.showAddRuleView = true;
	}
	
	$scope.reset = function() {
		delete $scope.state.showAddRuleView;
		delete $scope.state.addRuleName;
		delete $scope.state.addRuleOperand;
		delete $scope.state.addRuleOperator;
		delete $scope.state.addRuleValue;
		delete $scope.state.addRuleAction;
		delete $scope.state.secVulnCountSelected;
		delete $scope.state.licCatSelected;
		delete $scope.state.addRuleFormValid;
		delete $scope.state.addRuleConditionFormValid;
		
		$scope.state.ruleConditions = [];
		$scope.state.addRuleMatchType = 'any';
	}
	
	$scope.addRuleCondition = function() {
		$scope.state.ruleConditions.push({
			operand: $scope.state.addRuleOperand,
			operator: $scope.state.addRuleOperator,
			value: $scope.state.addRuleValue
		});
		
		$scope.validateRule();
	}
	
	$scope.enableRule = function() {
		$scope.updateStatus("enabled");
	}
	
	$scope.disableRule = function() {
		$scope.updateStatus("disabled");
	}
	
	$scope.updateStatus = function(status) {
        var grid = $scope.rulesTable;
		var rows = grid.getSelectedRows();
		
		for ( var i = 0 ; i < rows.length ; i++ ) {
			var item = grid.getDataItem(i);
			item.status = status;
		}
		
		$scope.state.rules = grid.getData();
	}
	
	$scope.removeRule = function() {
		var grid = $scope.rulesTable;
		$scope.removeSelectedItems(grid);
		$scope.state.rules = grid.getData();
	}
	
	$scope.removeRuleCondition = function() {
		var grid = $scope.ruleConditionsTable;
		$scope.removeSelectedItems(grid);
		$scope.state.ruleConditions = grid.getData();
		$scope.validateRule();
	}
	
	$scope.removeSelectedItems = function(grid) {		
		var rows = grid.getSelectedRows();
		var data = grid.getData();
		
		for ( var i = rows.length - 1 ; i >= 0 ; i-- ) {
			data.splice(rows[i], 1);
		}
		
		grid.setSelectedRows([]);
		grid.invalidate();
	}
	
	$scope.ruleInfo = function() {
		//TODO: show some info popup i imagine
	}
	
	$scope.saveRule = function() {
		$scope.state.rules.push({
		    name: $scope.state.addRuleName,
		    conditions: $scope.state.ruleConditions,
		    matchType: $scope.state.addRuleMatchType,
		    action: $scope.state.addRuleAction,
		    status: 'enabled'
		});
		
		$scope.reset();
	}
	
	$scope.validateRule = function() {
		delete $scope.state.addRuleFormValid;
		if ($scope.state.ruleConditions.length > 0
				&& $scope.state.addRuleAction
				&& $scope.state.addRuleName
				&& $scope.state.addRuleMatchType) {
			$scope.state.addRuleFormValid = true;
		}
	}
	
	$scope.validateRuleCondition = function() {
		delete $scope.state.addRuleConditionFormValid;
		if ($scope.state.addRuleOperand
				&& $scope.state.addRuleOperator
				&& $scope.state.addRuleValue) {
			$scope.state.addRuleConditionFormValid = true;
		}
	}
	
	$scope.ruleOperandChanged = function() {
		delete $scope.state.secVulnCountSelected;
		delete $scope.state.licCatSelected;
		
		if ($scope.state.addRuleOperand == 'secVuln') {
			$scope.state.secVulnCountSelected = true;
		} else if ($scope.state.addRuleOperand == 'licCat') {
			$scope.state.licCatSelected = true;
		}
		
		$scope.validateRuleCondition();
	}
	
	$scope.reset();
}