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
			id: ruleApp.getNextId($scope.state.ruleConditions)
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
		
		$scope.state.rules = grid.getData().getItems();
	}
	
	$scope.removeRule = function() {
		var grid = $scope.rulesTable;
		$scope.removeSelectedItems(grid);
		$scope.state.rules = grid.getData().getItems();
	}
	
	$scope.removeRuleCondition = function() {
		var grid = $scope.ruleConditionsTable;
		$scope.removeSelectedItems(grid);
		$scope.state.ruleConditions = grid.getData().getItems();
		$scope.validateRule();
	}
	
	$scope.removeSelectedItems = function(grid) {		
		var rows = grid.getSelectedRows();
		
		for ( var i = rows.length - 1 ; i >= 0 ; i-- ) {
			$scope.rulesTable.dataView.deleteItem($scope.rulesTable.dataView.getItemByIdx(rows).id);
		}
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
			    id: ruleApp.getNextId($scope.state.rules)
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
	
	$scope.populateForEdit = function(rule) {
		$scope.state.addRuleName = rule.name;
		$scope.state.addRuleAction = rule.action;
		$scope.state.ruleConditions = rule.conditions;
		$scope.state.addRuleMatchType = rule.matchType;
		$scope.state.addRuleId = rule.id;
        
        $scope.addRule();
        $scope.validateRule();
	}
	
	$scope.reset();
	
    $('#rulesTable .slick-row').live('mouseover mouseout', function (event) {
        if (event.type == 'mouseover') {
            $(this).find(".btn").show(); 
        } else {

             $(this).find(".btn").hide();
        }
    });
    
    $('#rulesTable .edit-click').live('click', function(){
    	$scope.populateForEdit($scope.rulesTable.dataView.getItem($scope.rulesTable.getCellFromEvent(arguments[0]).row));
		
        //since this event is called outside of angular, we need to force
        //an apply to get everything mapped up properly
        $scope.$apply();
    });
    
    $('#rulesTable .slick-row .btn-edit').live('click', function(){
    	$scope.populateForEdit($scope.rulesTable.dataView.getItem($scope.rulesTable.dataView.getIdxById($(this).attr('id'))));

        //since this event is called outside of angular, we need to force
        //an apply to get everything mapped up properly
        $scope.$apply();
    });
    
    $('#rulesTable .slick-row .btn-enable').live('click', function(){
		$scope.rulesTable.dataView.getItem($scope.rulesTable.dataView.getIdxById($(this).attr('id'))).status = 'enabled';
		
        //since this event is called outside of angular, we need to force
        //an apply to get everything mapped up properly
        $scope.$apply();
    });
    
    $('#rulesTable .slick-row .btn-disable').live('click', function(){
		$scope.rulesTable.dataView.getItem($scope.rulesTable.dataView.getIdxById($(this).attr('id'))).status = 'disabled';
		
        //since this event is called outside of angular, we need to force
        //an apply to get everything mapped up properly
        $scope.$apply();
    });
    
    $('#rulesTable .slick-row .btn-delete').live('click', function(){
		$scope.rulesTable.dataView.deleteItem($(this).attr('id'));
		
        //since this event is called outside of angular, we need to force
        //an apply to get everything mapped up properly
        $scope.$apply();
    });
    
    $('#ruleConditionsTable .slick-row').live('mouseover mouseout', function (event) {
        if (event.type == 'mouseover') {
            $(this).find(".btn").show(); 
        } else {

             $(this).find(".btn").hide();
        }
    });
    
    $('#ruleConditionsTable .slick-row .btn-delete').live('click', function(){
		$scope.ruleConditionsTable.dataView.deleteItem($(this).attr('id'));
		
        //since this event is called outside of angular, we need to force
        //an apply to get everything mapped up properly
        $scope.$apply();
    });
}