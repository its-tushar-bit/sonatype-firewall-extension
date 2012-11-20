function RuleController($scope, global, $http, $location) {
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
		delete $scope.state.addRuleFormValid;
		delete $scope.state.addRuleConditionFormValid;
		delete $scope.state.addRuleId;
		delete $scope.state.conditionOperators;
		delete $scope.state.conditionValues;
		
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
		
		delete $scope.state.addRuleOperand;
		delete $scope.state.addRuleOperator;
		delete $scope.state.addRuleValue;
		
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
			var item = grid.getDataItem(rows[i]);
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
			grid.dataView.deleteItem(grid.dataView.getItemByIdx(rows[i]).id);
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
					$scope.state.rules[i].conditions = $scope.state.ruleConditions.slice(0);
					$scope.state.rules[i].matchType = $scope.state.addRuleMatchType;
					$scope.state.rules[i].action = $scope.state.addRuleAction;
					break;
				}
			}
		} else {
			$scope.state.rules.push({
			    name: $scope.state.addRuleName,
			    conditions: $scope.state.ruleConditions.slice(0),
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
		delete $scope.state.conditionValues;
		
		var condition = $scope.state.addRuleOperand;
		
		$scope.state.conditionOperators = condition.operators.slice(0);
		
		if (condition.values) {
			$scope.state.conditionValues = condition.values.slice(0);
		}
		
		$scope.validateRuleCondition();
	}
	
	$scope.populateForEdit = function(rule) {
		$scope.state.addRuleName = rule.name;
		$scope.state.addRuleAction = rule.action;
		$scope.state.ruleConditions = rule.conditions.slice(0);
		$scope.state.addRuleMatchType = rule.matchType;
		$scope.state.addRuleId = rule.id;
        
        $scope.addRule();
        $scope.validateRule();
	}
	
	$scope.reset();
	
	//chaining these requests together as the rule list requires requires the responses for condition/alert types to have already been received
    $http.get('/rest/policy/conditionType').success(function(data, status, headers, config) {
    	$scope.state.conditionTypes = [];
    	for ( var i = 0 ; i < data.length ; i++ ){
    		$scope.state.conditionTypes.push({
    			id: data[i].id,
    			name: data[i].operandName,
    			operators: data[i].supportedOperators,
    			values: data[i].availableValues
    		});
    	}
    	
    	$http.get('/rest/policy/actionType').success(function(data, status, headers, config) {
        	$scope.state.actionTypes = [];
        	for ( var i = 0 ; i < data.length ; i++ ){
        		$scope.state.actionTypes.push({
        			id: data[i].id,
        			name: data[i].name,
        			values: data[i].availableValues
        		});
        	}
        	
        	$scope.state.rules = [];
            $http.get('rest/policy/rule/' + $location.search().appId).success(function(data, status, headers, config) {
            	$scope.state.rules = [];

            	for ( var i = 0 ; i < data.length ; i++ ){
            		var newRule = {
        				name: data[i].name,
            			conditions: [],
            			matchType: (data[i].operator == 'AND' ? 'all' : 'any'),
            			status: (data[i].enabled ? 'enabled' : 'disabled'),
            			id: data[i].id
            		};
            		
            		for ( var j = 0 ; j < data[i].conditions.length ; j++ ){
            			for ( var k = 0 ; k < $scope.state.conditionTypes.length ; k++ ){
            				if ( $scope.state.conditionTypes[k].id == data[i].conditions[j].conditionTypeId ){
            					newRule.conditions.push({
            	    				operand: $scope.state.conditionTypes[k],
            	    				operator: data[i].conditions[j].operator,
            	    				value: data[i].conditions[j].value,
            	    				id: data[i].conditions[j].id
            	    			});
            					break;
            				}
            			}
            		}
            		
            		//TODO: server currently supports multiple actions, UI only a single action
            		//this will be remedied at some point
            		for ( var j = 0 ; j < data[i].actions.length && j < 1; j++ ){
            			for ( var k = 0 ; k < $scope.state.actionTypes.length ; k++ ){
            				if ( $scope.state.actionTypes[k].id == data[i].actions[j].actionTypeId ){
            					newRule.action = $scope.state.actionTypes[k]
            					break;
            				}
            			} 
            		}
            		
            		$scope.state.rules.push(newRule);
            	}
            });
        });
    });
	
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
		$scope.validateRule();
		
        //since this event is called outside of angular, we need to force
        //an apply to get everything mapped up properly
        $scope.$apply();
    });
    
    $('.btn-add').live('click', function(){
    	$scope.addRule();
    	
    	//since this event is called outside of angular, we need to force
        //an apply to get everything mapped up properly
    	$scope.$apply();
    });
}