function InsightPolicyController($scope, global, $http, $location) {
	$scope.state = global;
	
	$scope.resetConstraint = function() {
		delete $scope.state.addConstraintFormValid;
		delete $scope.state.addConstraintConditionFormValid;
		$scope.state.currentConstraint = {
			conditions: [],
			actions: [],
			operator: 'OR'
		};
		$scope.state.currentCondition = {};
	}
	
	$scope.resetActions = function() {
		delete $scope.state.procureAction;
		delete $scope.state.procureNotify;
		delete $scope.state.developAction;
		delete $scope.state.developNotify;
		delete $scope.state.buildAction;
		delete $scope.state.buildNotify;
		delete $scope.state.releaseAction;
		delete $scope.state.releaseNotify;
		delete $scope.state.operateAction;
		delete $scope.state.operateNotify;
		
		if ($scope.state.currentPolicy) {
			angular.forEach($scope.state.currentPolicy.actions, function(value, key) {
				$scope.state[value.id + 'Action'] = value.warn ? 'warn' : (value.fail ? 'fail' : '');
				$scope.state[value.id + 'Notify'] = value.notify;
			});
		}
	}
	
	$scope.reset = function() {
		$scope.resetConstraint();
		delete $scope.state.currentPolicy;
		delete $scope.state.currentPolicyRef;
		delete $scope.state.showAddPolicyScreen;
		if ($scope.constraintGrid) {
			$scope.constraintGrid.setSelectedRows([]);
		}
	}
	
	$scope.createPolicyClick = function(){
		$scope.state.currentPolicy = {
			constraints: [],
			actions: [{
				id: 'procure'
			},{
				id: 'develop'
			},{
				id: 'build'
			},{
				id: 'release'
			},{
				id: 'operate'
			}]
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
		angular.forEach($scope.state.currentPolicy.actions, function(value, key) {
			$scope.state[value.id + 'Action'] = value.warn ? 'warn' : (value.fail ? 'fail' : '');
			$scope.state[value.id + 'Notify'] = value.notify;
		});
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
		
		$scope.state.currentPolicy.actions = [{
			id: 'procure',
			warn: $scope.state.procureAction === 'warn',
			fail: $scope.state.procureAction === 'fail',
			notify: $scope.state.procureNotify
		},{
			id: 'develop',
			warn: $scope.state.developAction === 'warn',
			fail: $scope.state.developAction === 'fail',
			notify: $scope.state.developNotify
		},{
			id: 'build',
			warn: $scope.state.buildAction === 'warn',
			fail: $scope.state.buildAction === 'fail',
			notify: $scope.state.buildNotify
		},{
			id: 'release',
			warn: $scope.state.releaseAction === 'warn',
			fail: $scope.state.releaseAction === 'fail',
			notify: $scope.state.releaseNotify
		},{
			id: 'operate',
			warn: $scope.state.operateAction === 'warn',
			fail: $scope.state.operateAction === 'fail',
			notify: $scope.state.operateNotify
		}];
		
		$scope.resetActions();
		
		//not a fan, but data-dismiss doesn't work when ng-click is also defined on an element
		$('#editActionsModal').modal('hide');
	}
	
	$scope.reset();
	
	$http.get(insightApp.getConditionTypeUrl()).success(function(conditionTypeData, status, headers, config) {
    	$scope.state.conditionTypes = conditionTypeData;
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