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
		delete $scope.state.addRuleId;
		
		$scope.state.ruleConditions = [];
		$scope.state.addRuleMatchType = 'any';
	}
	
	$scope.addRuleCondition = function() {
		$scope.state.ruleConditions.push({
			operand: $scope.state.addRuleOperand,
			operator: $scope.state.addRuleOperator,
			value: $scope.state.addRuleValue,
			id: $scope.state.ruleConditions.length
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
		//edit
		if ($scope.state.addRuleId) {
			for ( var i = 0 ; i < $scope.state.rules.length ; i++ ) {
				if ( $scope.state.rules[i].id == $scope.state.addRuleId) {
					$scope.state.rules[i].name = $scope.state.addRuleName;
					$scope.state.rules[i].conditions = $scope.state.ruleConditions;
					$scope.state.rules[i].matchType = $scope.state.addRuleMatchType;
					$scope.state.rules[i].action = $scope.state.addRuleAction;
					break;
				}
			}
		} else {
			$scope.state.rules.push({
			    name: $scope.state.addRuleName,
			    conditions: $scope.state.ruleConditions,
			    matchType: $scope.state.addRuleMatchType,
			    action: $scope.state.addRuleAction,
			    status: 'enabled',
			    id: $scope.state.rules.length + 1
			});	
		}
		
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
	
    $('#rulesTable .slick-row').live('mouseover mouseout', function (event) {
        if (event.type == 'mouseover') {
            $(this).find(".btn").show(); 
        } else {

             $(this).find(".btn").hide();
        }
    });
    
    $('#rulesTable .slick-row .btn').live('click', function(){
        var me = $(this), id = me.attr('id');
        
		var rule = $scope.rulesTable.dataView.getItem($scope.rulesTable.dataView.getIdxById(id));
		
		$scope.state.addRuleName = rule.name;
		$scope.state.addRuleAction = rule.action;
		$scope.state.ruleConditions = rule.conditions;
		$scope.state.addRuleMatchType = rule.matchType;
		$scope.state.addRuleId = rule.id;
        
        $scope.addRule();
        $scope.validateRule();

        //since this event is called outside of angular, we need to force
        //an apply to get everything mapped up properly
        $scope.$apply();
    });
}