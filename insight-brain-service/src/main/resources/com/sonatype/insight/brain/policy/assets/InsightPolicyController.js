function InsightPolicyController($scope, global, $http, $location) {
	$scope.state = global;
	
	$scope.resetConstraint = function() {
		delete $scope.state.addConstraintFormValid;
		delete $scope.state.addConstraintConditionFormValid;
		$scope.state.currentConstraint = {
			conditions: [],
			operator: 'OR'
		};
		$scope.state.currentCondition = {};
	}
	
	$scope.resetActions = function() {
		$scope.state.actionData = {};
		
		if ($scope.state.currentPolicy) {
			angular.forEach($scope.state.currentPolicy.actions, function(value, key) {
				for ( var i = 0 ; i < value.length ; i++ ){
					switch (value[i].actionTypeId) {
					case 'fail':
						$scope.state.actionData[key + 'Action'] = 'fail';
						break;
					case 'warn':
						$scope.state.actionData[key + 'Action'] = 'warn';
						break;
					case 'notify':
						$scope.state.actionData[key + 'Notify'] = value[i].target;
						break;
					}
				}
			});
		}
		
		$scope.state.actionTableData = [];
		
		var handleActionForTable = function(id){
			return {
				id: id,
				fail: $scope.state.actionData[id + 'Action'] === 'fail',
				warn: $scope.state.actionData[id + 'Action'] === 'warn',
				notify: $scope.state.actionData[id + 'Notify']
			};
		}
		
		//this is for the table displayed in constraint screen
		angular.forEach($scope.state.actionContextList, function(value, key) {
			$scope.state.actionTableData.push(handleActionForTable(value.id));
		});
	}
	
	$scope.reset = function() {
		$scope.resetConstraint();
		delete $scope.state.currentPolicy;
		delete $scope.state.currentPolicyRef;
		delete $scope.state.showAddPolicyScreen;
		if ($scope.constraintGrid) {
			$scope.constraintGrid.setSelectedRows([]);
		}
		$scope.resetActions();
	}
	
	$scope.createPolicyClick = function(){
		$scope.state.currentPolicy = {
			constraints: [],
			actions: {}
		}
		$scope.state.showAddPolicyScreen = true;
		setTimeout(function(){
			$scope.constraintGrid.redraw();
		},50);
	}
	
	$scope.policyEditClick = function() {
		//do a copy so that we dont update the record in the list from server
		$scope.state.currentPolicy = angular.copy(this.policy);
		$scope.state.currentPolicyRef = this.policy;
		$scope.state.showAddPolicyScreen = true;
		$scope.state.policyValid = true;
		$scope.resetActions();
		setTimeout(function(){
			$scope.constraintGrid.redraw();
		},50);
	}
	
	$scope.savePolicyClick = function(){
		if ($scope.state.currentPolicyRef) {
			angular.copy($scope.state.currentPolicy, $scope.state.currentPolicyRef);
		} else {
			$scope.state.policyList.push($scope.state.currentPolicy);
		}
		$scope.reset();
	}
	
	$scope.cancelPolicyClick = function(){
		$scope.reset();
	}
	
	$scope.validatePolicy = function() {
		delete $scope.state.policyValid;
		if ($scope.state.currentPolicy.name
			&& $scope.state.currentPolicy.constraints.length > 0) {
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
		
		$scope.validatePolicy();
	}
	
	$scope.createConstraintClick = function() {
		$scope.resetConstraint();
		$('#editConstraintModal').modal('show');
	}
	
	$scope.addConstraintClick = function() {
		var constraintObj = {
			name: $scope.state.currentConstraint.name,
		    conditions: $scope.state.currentConstraint.conditions,
		    operator: $scope.state.currentConstraint.operator,
		    enabled: true,
		    //TODO: this will ultimately come from the server
		    id: $scope.state.currentConstraint.id ? $scope.state.currentConstraint.id : $scope.state.currentConstraint.name
		}
		
		var found = false;
		
		for ( var i = 0 ; i < $scope.state.currentPolicy.constraints.length ; i++) {
			if ( $scope.state.currentPolicy.constraints[i].id == constraintObj.id ) {
				$scope.state.currentPolicy.constraints[i] = constraintObj;
				found = true;
				break;
			}
		}
		
		if (!found) {
			$scope.state.currentPolicy.constraints.push(constraintObj);		
		}
		
		$scope.resetConstraint();
		
		//not a fan, but data-dismiss doesn't work when ng-click is also defined on an element
		$('#editConstraintModal').modal('hide');
		
		$scope.validatePolicy();
	}
	
	$scope.addConstraintCondition = function() {
		$scope.state.currentConstraint.conditions.push($scope.state.currentCondition);
		
		$scope.state.currentCondition = {};
		
		$scope.validateConstraint();
		$scope.validateConstraintCondition();
	}
	
	$scope.removeConstraintCondition = function() {
		var grid = $scope.constraintConditionsGrid;
		var rows = grid.getSelectedRows();
		for ( var i = rows.length - 1 ; i >= 0 ; i-- ) {
			grid.dataView.deleteItem(grid.dataView.getItemByIdx(rows[i]).id);
		}
		$scope.state.currentConstraint.conditions = grid.getData().getItems();
		$scope.validateConstraint();
	}
	
	$scope.validateConstraint = function() {
		delete $scope.state.addConstraintFormValid;
		if ($scope.state.currentConstraint.conditions.length > 0
				&& $scope.state.currentConstraint.name
				&& $scope.state.currentConstraint.operator) {
			$scope.state.addConstraintFormValid = true;
		}
	}
	
	$scope.validateConstraintCondition = function() {
		delete $scope.state.addConstraintConditionFormValid;
		if ($scope.state.currentCondition.operand
				&& $scope.state.currentCondition.operator
				&& (!$scope.state.currentCondition.operand.requiresValue || $scope.state.currentCondition.value)) {
			$scope.state.addConstraintConditionFormValid = true;
		}
	}
	
	$scope.constraintOperandChanged = function() {
		delete $scope.state.currentCondition.operator;
		delete $scope.state.currentCondition.value;
		$scope.validateConstraintCondition();
	}
	
	$scope.doneEditActionsClick = function() {
		//TODO: validation, at least on emails
		var handleAction = function(id){
			var result = [];
			
			if ($scope.state.actionData[id + 'Action'] === 'warn') {
				result.push({
					actionTypeId: 'warn'
				});
			} else if ($scope.state.actionData[id + 'Action'] === 'fail') {
				result.push({
					actionTypeId: 'fail'
				});
			}
			
			if ($scope.state.actionData[id + 'Notify']) {
				result.push({
					actionTypeId: 'notify',
					target: $scope.state.actionData[id + 'Notify']
				});
			}
			
			return result;
		};
		
		angular.forEach($scope.state.actionContextList, function(value, key) {
			$scope.state.currentPolicy.actions[value.id] = handleAction(value.id);
		});
			
		$scope.resetActions();
		
		//not a fan, but data-dismiss doesn't work when ng-click is also defined on an element
		$('#editActionsModal').modal('hide');
	}
	
	insightApp.appId = $location.search().appId;
	
	$http.get(insightApp.getConditionTypeUrl()).success(function(conditionTypeData, status, headers, config) {
    	$scope.state.conditionTypes = conditionTypeData;
    	$http.get(insightApp.getActionTypeUrl()).success(function(actionTypeData, status, headers, config) {
        	$scope.state.actionTypeList = actionTypeData;
        	$http.get(insightApp.getActionContextUrl()).success(function(actionContextData, status, headers, config) {
        		$scope.state.actionContextList = actionContextData;
        		$scope.state.policyList = [];
                $http.get(insightApp.getPolicyUrl()).success(function(data, status, headers, config) {
                	for ( var i = 0 ; i < data.length ; i++ ){
                		$scope.state.policyList.push(data[i]);
                		
                		//solely to setup the operand with the conditionType object
                		for ( var j = 0 ; j < data[i].constraints.length ; j++ ) {
                			for ( var k = 0 ; k < data[i].constraints[j].conditions.length ; k++ ) {
                				for ( var l = 0 ; l < $scope.state.conditionTypes.length ; l++ ){
                    				if ( $scope.state.conditionTypes[l].id == data[i].constraints[j].conditions[k].conditionTypeId ){
                    					data[i].constraints[j].conditions[k].operand = $scope.state.conditionTypes[l];				
                    					break;
                    				}
                    			}
                			}
                		}
                	}
                	
                	$scope.reset();
                });
        	});
    	});
    });
	
	$('#editConstraintModal').live('show', function (event) {
		setTimeout(function(){
			$scope.constraintConditionsGrid.redraw();
		},100);
    });
	
	$('#constraintGrid .slick-row').live('mouseover mouseout', function (event) {
        if (event.type == 'mouseover') {
            $(this).find(".btn").show(); 
        } else {

             $(this).find(".btn").hide();
        }
    });
	
    $('#constraintGrid .slick-row .btn-edit').live('click', function(){
    	var constraint = $scope.constraintGrid.dataView.getItem($scope.constraintGrid.dataView.getIdxById($(this).attr('id')));
    	
    	//copy so we dont update data in the current list
    	$scope.state.currentConstraint = angular.copy(constraint);
		$scope.validateConstraint();
		
		for ( var j = 0 ; j < $scope.state.currentConstraint.conditions.length ; j++ ){
			for ( var k = 0 ; k < $scope.state.conditionTypes.length ; k++ ){
				if ( $scope.state.conditionTypes[k].id == $scope.state.currentConstraint.conditions[j].conditionTypeId ){
					$scope.state.currentConstraint.conditions[j].operand = $scope.state.conditionTypes[k];
					break;
				}
			}
		}

        //since this event is called outside of angular, we need to force
        //an apply to get everything mapped up properly
        $scope.$apply();
        
        $('#editConstraintModal').modal('show');
    });
    
    $('#constraintGrid .slick-row .btn-enable').live('click', function(){
    	var constraint = $scope.constraintGrid.dataView.getItem($scope.constraintGrid.dataView.getIdxById($(this).attr('id')));
    	constraint.enabled = true;
    	
        //since this event is called outside of angular, we need to force
        //an apply to get everything mapped up properly
        $scope.$apply();
    });
    
    $('#constraintGrid .slick-row .btn-disable').live('click', function(){
    	var constraint = $scope.constraintGrid.dataView.getItem($scope.constraintGrid.dataView.getIdxById($(this).attr('id')));
    	constraint.enabled = false;
    	
        //since this event is called outside of angular, we need to force
        //an apply to get everything mapped up properly
        $scope.$apply();
    });
    
    $('#constraintGrid .slick-row .btn-delete').live('click', function(){
    	var constraint = $scope.constraintGrid.dataView.getItem($scope.constraintGrid.dataView.getIdxById($(this).attr('id')));
    	$scope.constraintGrid.dataView.deleteItem(constraint.id);
    	
        //since this event is called outside of angular, we need to force
        //an apply to get everything mapped up properly
        $scope.$apply();
    });
}