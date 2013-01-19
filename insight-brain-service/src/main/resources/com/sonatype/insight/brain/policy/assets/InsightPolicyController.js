(function () {
	'use strict';

	var policyModule = angular.module('Policy', ['Hudson']);

	policyModule.controller('InsightPolicyController', ['$scope', 'global', '$http', 'hudson', '$timeout', function ($scope, global, $http, hudson, $timeout) {

	function updatePolicySummary(data) {
		data.summary = {
			constraints: data.constraints.length + ' Constraint(s) to be evaluated',
		}
		
		var actionCount = 0;
		var actionNames = '';
		angular.forEach(data.actions, function(value, key){
			if ( value.length > 0 ) {
    			actionCount++;
    			if ( actionNames.length > 0 ) {
					actionNames += ', ';
				}
    			
    			for ( var j = 0 ; j < $scope.state.actionStageList.length ; j++ ){
    				if ( $scope.state.actionStageList[j].id == key ){
    					actionNames += $scope.state.actionStageList[j].name + ': ';				
    					break;
    				}
    			}
    			
    			for ( var j = 0 ; j < value.length ; j++ ) {
    				if (j > 0) {
						actionNames += '/';
					}
    				if ( value[j].actionTypeId === 'warn') {
    					actionNames += 'Warn';
    				} else if ( value[j].actionTypeId === 'fail') {
    					actionNames += 'Fail';
    				} else if ( value[j].actionTypeId === 'notify') {
    					actionNames += 'Notify';
    				}
    			}
			}
		});
		
		data.summary.actionCount = actionCount;
		data.summary.actions = actionNames;
	}
	
	function addUIConditionData(data) {
		angular.forEach(data.constraints, function(constraint,constraintIndex){
			angular.forEach(constraint.conditions, function(condition, conditionIndex){
				condition.conditionType = getConditionType(condition.conditionTypeId);
				condition.valueType = getConditionValueType(condition.conditionType.valueTypeId);
				if ( condition.value ){
					var parts = condition.value.split(',');
					if ( parts.length > 1 ){
						condition.value = parts;
					} else if (condition.valueType && condition.valueType.id === 'AgeInDaysValueType'){			
						if (condition.value > 365 && condition.value % 365 === 0) {
							condition.value = condition.value / 365;
							condition.valueModifier = 'y';
						} else if (condition.value > 30 && condition.value & 30 === 0) {
							condition.value = condition.value / 30;
							condition.valueModifier = 'm';
						} else {
							condition.valueModifier = 'd';
						}
					}
				}
			});
		});
	}
	
	function removeUIConditionData(data) {
		angular.forEach(data.constraints, function(constraint,constraintIndex){
			angular.forEach(constraint.conditions, function(condition, conditionIndex){
				if ( angular.isArray(condition.value) ){
					condition.value = condition.value.join();
				} else if (condition.valueType && condition.valueType.id === 'AgeInDaysValueType'){
					if (condition.valueModifier === 'y'){
						condition.value = condition.value * 365;
					} else if (condition.valueModifier === 'm'){
						condition.value = condition.value * 30;
					}
				}
				
				delete condition.valueModifier;
				delete condition.conditionType;
				delete condition.valueType;
			});
		});
	}
	
	function getConditionType (id) {
		for ( var i = 0 ; i < $scope.state.conditionTypeList.length ; i++ ){
			if ( $scope.state.conditionTypeList[i].id == id ){
				return $scope.state.conditionTypeList[i];
			}
		}
		
		return null;
	}
	
	function getConditionValueType( id ) {
		for ( var i = 0 ; i < $scope.state.conditionValueTypeList.length ; i++ ){
			if ( $scope.state.conditionValueTypeList[i].id == id ){
				return $scope.state.conditionValueTypeList[i];
			}
		}
		
		return null;
	}
	
	function isAvailableStage(id) {
		return id === 'build';
	}
	
	function showHttpMask(bodyText){
		$scope.state.httpMaskBody = bodyText;
		$('#httpMaskModal').modal('show');
	}
	
	function hideHttpMask(){
		$('#httpMaskModal').modal('hide');
	}
	
	function handleHttpError(headerText, bodyText, status) {
		hideHttpMask();
		$scope.state.httpErrorBody = status === 0 ? 'Unable to connect to server.' : bodyText;
		$scope.state.httpErrorHeader = headerText;
		$('#httpErrorModal').modal('show');
	}
	
	function isDoneLoading(){
		return $scope.state.conditionTypeList !== undefined
			&& $scope.state.actionTypeList !== undefined
			&& $scope.state.actionStageList !== undefined
			&& $scope.state.conditionValueTypeList !== undefined
			&& $scope.state.policyList !== undefined;
	}
	
	function waitForLoad(){
		if (!isDoneLoading()) {
			$timeout(waitForLoad, 250);
		} else {
			postLoad();
		}
	}
	
	function postLoad(){
		angular.forEach($scope.state.policyList, function(policy, key) {
			updatePolicySummary(policy);
    		addUIConditionData(policy);
		});
    	
    	$scope.reset();
    	hideHttpMask();
	}
	
	$scope.state = global;
	
	$scope.resetConstraint = function() {
		$scope.state.currentConstraint = {
			conditions: [],
			operator: 'OR'
		};
		$scope.addCondition();
		delete $scope.state.actionEditMode;
	}
	
	$scope.resetActions = function() {
		$scope.state.actionTableData = [];
		
		if ($scope.state.currentPolicy) {
			for ( var i = 0 ; i < $scope.state.actionStageList.length ; i++ ) {
				var item = {
					id: $scope.state.actionStageList[i].id,
					name: $scope.state.actionStageList[i].name,
					available: isAvailableStage($scope.state.actionStageList[i].id),
					action: 'none'
				};
				
				if ($scope.state.currentPolicy.actions[item.id]) {
					for ( var j = 0 ; j < $scope.state.currentPolicy.actions[item.id].length ; j++ ){
						switch ($scope.state.currentPolicy.actions[item.id][j].actionTypeId) {
						case 'fail':
							item.action = 'fail';
							break;
						case 'warn':
							item.action = 'warn';
							break;
						}
					}
				}
				
				$scope.state.actionTableData.push(item);
			}
		}
	}
	
	$scope.reset = function() {
		$scope.resetConstraint();
		delete $scope.state.currentPolicy;
		delete $scope.state.showAddPolicyScreen;
		$scope.resetActions();
	}
	
	$scope.editPolicy = function(){
		$scope.state.currentPolicy = angular.copy($scope.state.policyList[this.$index]);
		$scope.state.showAddPolicyScreen = true;
		$scope.resetActions();
		$scope.validatePolicy();
	}
	
	$scope.removePolicy = function(){
		$scope.state.deletePolicyIndex = this.$index;
		$('#deletePolicyConfirmationModal').modal('show');
	}
	
	$scope.createPolicyClick = function($event){
		$event.preventDefault();
		$scope.state.currentPolicy = {
			constraints: [],
			actions: {},
			threatLevel: 5
		}
		$scope.state.showAddPolicyScreen = true;
		$scope.resetActions();
	}
	
	$scope.savePolicyClick = function() {		
		//I copy the item here as I don't want to dirty the UI data with changes needed for the server
		var item = angular.copy($scope.state.currentPolicy);
		removeUIConditionData(item);
		showHttpMask('Saving policy...');
		//edit
		if ($scope.state.currentPolicy.id) {		    
			$http.put(insightApp.getPolicyUrl(),item).success(function(data, status, headers, config){
				updatePolicySummary(data);
				addUIConditionData(data);
				for ( var i = 0 ; i < $scope.state.policyList.length ; i++ ){
					if ($scope.state.policyList[i].id === data.id){
						angular.copy(data, $scope.state.policyList[i]);
						break;
					}
            	}
				$scope.reset();
				hideHttpMask();
			}).error(function(data, status, headers, config){
				handleHttpError('Policy Save Error', data, status);
			});
		} else {		   			
			hudson.post(insightApp.getPolicyUrl(),item).success(function(data, status, headers, config){
				updatePolicySummary(data);
				addUIConditionData(data);
				$scope.state.policyList.push(data);
				$scope.reset();
				hideHttpMask();
			}).error(function(data, status, headers, config){
				handleHttpError('Policy Save Error', data, status);
			});
		}
	}
	
	$scope.deletePolicyClick = function(){
		$('#deletePolicyConfirmationModal').modal('hide');
		showHttpMask('Deleting policy...');
		$http.delete(insightApp.getPolicyUrl() + '/' + $scope.state.policyList[$scope.state.deletePolicyIndex].id).success(function(data, status, headers, config){
			$scope.state.policyList.splice($scope.state.deletePolicyIndex,1);
			hideHttpMask();
		}).error(function(data, status, headers, config){
			handleHttpError('Policy Delete Error', data, status);
		});
	}
	
	$scope.cancelPolicyClick = function(){
		$('#cancelPolicyConfirmationModal').modal('hide');
		$scope.reset();
	}

	$scope.validatePolicy = function() {
		delete $scope.state.policyValid;
		if ($scope.state.currentPolicy.name
			&& $scope.state.currentPolicy.threatLevel >= 0
			&& $scope.state.currentPolicy.constraints.length > 0) {
			$scope.state.policyValid = true;
		}
	}
	
	$scope.removeConstraint = function() {		
		$scope.state.deleteConstraintIndex = this.$index;
        $('#deleteConstraintConfirmationModal').modal('show');
	}
	
	$scope.addConstraint = function() {
		$scope.resetConstraint();        
        $('#editConstraintModal').modal('show');
	}
	
	$scope.editConstraint = function() {
    	//copy so we dont update data in the current list
		$scope.state.currentConstraint = angular.copy($scope.state.currentPolicy.constraints[this.$index]);
    	$scope.validateConstraint();
        $('#editConstraintModal').modal('show');
	}
	
	$scope.cancelConstraintClick = function() {
		$('#editConstraintModal').modal('hide');
		$scope.resetConstraint();
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
	
	$scope.deleteConstraintClick = function(){
		$scope.state.currentPolicy.constraints.splice($scope.state.deleteConstraintIndex,1);
    	$scope.validatePolicy();
    	$('#deleteConstraintConfirmationModal').modal('hide');
	}
	
	$scope.validateConstraint = function() {
		delete $scope.state.constraintValidationMsg;
		
		if (!$scope.state.currentConstraint.name){
			$scope.state.constraintValidationMsg = 'Please enter a name for this constraint';
			return;
		}
		
		for (var i = 0 ; i < $scope.state.currentConstraint.conditions.length ; i++){
			if ($scope.state.currentConstraint.conditions[i].valueType && !$scope.state.currentConstraint.conditions[i].value){
				$scope.state.constraintValidationMsg = 'There is an invalid condition, please correct!';
				return;
			}
		}
	}
	
	$scope.conditionTypeChanged = function() {
		var condition = $scope.state.currentConstraint.conditions[this.$index];
		condition.conditionType = getConditionType(condition.conditionTypeId);
		condition.valueType = getConditionValueType(condition.conditionType.valueTypeId);
		
		condition.operator = condition.conditionType.supportedOperators[0];
		
		delete condition.value;
		
		condition.valueModifier = 'y';
		
		$scope.validateConstraint();
	}
	
	$scope.addCondition = function() {
		var conditionType = getConditionType($scope.state.conditionTypeList[0].id);
		var valueType = getConditionValueType(conditionType.valueTypeId);
		
		$scope.state.currentConstraint.conditions.push({
			conditionTypeId: conditionType.id,
			conditionType: conditionType,
			operator: conditionType.supportedOperators[0],
			valueType: valueType,
			valueModifier: 'y'
		});
		
		$scope.validateConstraint();
	}
	
	$scope.removeCondition = function() {
		$scope.state.currentConstraint.conditions.splice(this.$index, 1);
		$scope.validateConstraint();
	}
	
	$scope.editActionsClick = function() {
		if ($scope.state.actionEditMode){
			$scope.pushActionDataToModel();
		}
		$scope.state.actionEditMode = !$scope.state.actionEditMode;
	}
	
	$scope.pushActionDataToModel = function() {
		if ($scope.state.actionEditMode){			
			if ($scope.state.currentPolicy) {
				var handleAction = function(id){
					var result = [];
					
					for ( var i = 0 ; i < $scope.state.actionTableData.length ; i++ ) {
						if ($scope.state.actionTableData[i].id === id) {
							switch ($scope.state.actionTableData[i].action){
							case 'fail':
								result.push({
									actionTypeId: 'fail'
								});
								break;
							case 'warn':
								result.push({
									actionTypeId: 'warn'
								});
								break;
							}
							break;
						}
					}
					
					return result;
				};
				
				angular.forEach($scope.state.actionStageList, function(value, key) {
					$scope.state.currentPolicy.actions[value.id] = handleAction(value.id);
				});
			}
		}
	}
	
	$scope.$watch('state.actionTableData',function(newScopeData){
		$scope.pushActionDataToModel();
	},true);
	
	showHttpMask('Loading data from server...');
	
	delete $scope.state.conditionTypeList;
	delete $scope.state.actionTypeList;
	delete $scope.state.actionStageList;
	delete $scope.state.conditionValueTypeList;
	delete $scope.state.policyList;
	
	$http.get(insightApp.getConditionTypeUrl()).success(function(data, status, headers, config) {
    	$scope.state.conditionTypeList = data;
    }).error(function(data, status, headers, config){
    	handleHttpError('Condition Type Initialization Error', data, status);
	});
	
	$http.get(insightApp.getActionTypeUrl()).success(function(data, status, headers, config) {
    	$scope.state.actionTypeList = data;
	}).error(function(data, status, headers, config){
		handleHttpError('Action Type Initialization Error', data, status);
	});
	
	$http.get(insightApp.getActionStageUrl()).success(function(data, status, headers, config) {
		$scope.state.actionStageList = data;
	}).error(function(data, status, headers, config){
		handleHttpError('Action Stage Initialization Error', data, status);
	});
	
	$http.get(insightApp.getConditionValueTypeUrl()).success(function(data, status, headers, config) {
		$scope.state.conditionValueTypeList = data;
	}).error(function(data, status, headers, config){
		handleHttpError('Condition Value Type Initialization Error', data, status);
	});
	
	$http.get(insightApp.getPolicyUrl()).success(function(data, status, headers, config) {
    	$scope.state.policyList = data;
    }).error(function(data, status, headers, config){
    	handleHttpError('Policy Initialization Error', data, status);
	});
	
	waitForLoad();
	
	}]);
}());