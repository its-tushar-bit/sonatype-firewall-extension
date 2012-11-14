function RuleController($scope, global) {
	$scope.state = global;
	$scope.showAddRuleView = false;
	$scope.showAllRulesView = true;
	$scope.state.ruleConditions = [];
	$scope.addRuleConditionFormInvalid = true;
	$scope.addRuleFormInvalid = true;
	$scope.addRuleMatchType = 'any';
	
	$scope.viewAddRule = function() {
		//TODO: not a fan of this, but i want the panels to be same height, will put alternative in later
		$('#newRuleContainer').height($('#rulesTableContainer').height());
		$scope.showAddRuleView = true;
		$scope.showAllRulesView = false;
	}
	
	$scope.viewAllRules = function() {
		$scope.showAddRuleView = false;
		$scope.showAllRulesView = true;
		
		delete $scope.addRuleName;
		delete $scope.addRuleOperand;
		delete $scope.addRuleOperator;
		delete $scope.addRuleValue;
		delete $scope.addRuleAction;
		delete $scope.secVulnCountSelected;
		delete $scope.licCatSelected;
		
		$scope.state.ruleConditions = [];
		$scope.addRuleMatchType = 'any';
		$scope.addRuleFormInvalid = true;
		$scope.addRuleConditionFormInvalid = true;
	}
	
	$scope.addRuleCondition = function() {
		$scope.state.ruleConditions.push({
			operand: $scope.addRuleOperand,
			operator: $scope.addRuleOperator,
			value: $scope.addRuleValue
		});
		
		$scope.validateRule();
	}
	
	$scope.enableRule = function() {
		var grid = $scope.rulesTable;
		
		var rows = grid.getSelectedRows();
		
		for ( var i = 0 ; i < rows.length ; i++ ) {
			var item = grid.getDataItem(i);
			item.status = "enabled";
		}
		
		$scope.state.rows = grid.getData();
	}
	
	$scope.disableRule = function() {
        var grid = $scope.rulesTable;
		
		var rows = grid.getSelectedRows();
		
		for ( var i = 0 ; i < rows.length ; i++ ) {
			var item = grid.getDataItem(i);
			item.status = "disabled";
		}
		
		$scope.state.rows = grid.getData();
	}
	
	$scope.removeRule = function() {
		var grid = $scope.rulesTable;
		
		var rows = grid.getSelectedRows();
		
		var data = grid.getData();
		
		for ( var i = rows.length - 1 ; i >= 0 ; i-- ) {
			data.splice(rows[i], 1);
		}
		
		grid.setSelectedRows([]);
		
		grid.invalidate();
		
		$scope.state.rows = grid.getData();
	}
	
	$scope.removeRuleCondition = function() {
		var grid = $scope.ruleConditionsTable;
		
		var rows = grid.getSelectedRows();
		
		var data = grid.getData();
		
		for ( var i = rows.length - 1 ; i >= 0 ; i-- ) {
			data.splice(rows[i], 1);
		}
		
		grid.setSelectedRows([]);
		
		grid.invalidate();
		
		$scope.state.ruleConditions = grid.getData();
		
		$scope.validateRule();
	}
	
	$scope.ruleInfo = function() {
		//TODO: show some info popup i imagine
	}
	
	$scope.saveRule = function() {
		$scope.state.rows.push({
		    name: $scope.addRuleName,
		    status: "enabled"
		});
		
		$scope.viewAllRules();
	}
	
	$scope.validateRule = function() {
		$scope.addRuleFormInvalid = true;
		if ($scope.state.ruleConditions.length > 0
				&& $scope.addRuleAction
				&& $scope.addRuleName
				&& $scope.addRuleMatchType) {
			$scope.addRuleFormInvalid = false;
		}
	}
	
	$scope.validateRuleCondition = function() {
		$scope.addRuleConditionFormInvalid = true;
		if ($scope.addRuleOperand
				&& $scope.addRuleOperator
				&& $scope.addRuleValue) {
			$scope.addRuleConditionFormInvalid = false;
		}
	}
	
	$scope.ruleOperandChanged = function() {
		$scope.secVulnCountSelected = false;
		$scope.licCatSelected = false;
		
		if ($scope.addRuleOperand == 'secVuln') {
			$scope.secVulnCountSelected = true;
		} else if ($scope.addRuleOperand == 'licCat') {
			$scope.licCatSelected = true;
		}
		
		$scope.validateRuleCondition();
	}
}