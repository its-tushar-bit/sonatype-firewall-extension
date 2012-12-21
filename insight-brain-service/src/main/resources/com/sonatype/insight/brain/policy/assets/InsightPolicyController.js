function InsightPolicyController($scope, global, $http, $location) {
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
	
	function parseConditionValues(data) {
		angular.forEach(data.constraints, function(constraint,constraintIndex){
			angular.forEach(constraint.conditions, function(condition, conditionIndex){
				if ( condition.value ){
					var parts = condition.value.split(',');
					if ( parts.length > 1 ){
						condition.value = parts;
					}
				}
			});
		});
	}
	
	function composeConditionValues(data) {
		angular.forEach(data.constraints, function(constraint,constraintIndex){
			angular.forEach(constraint.conditions, function(condition, conditionIndex){
				if ( angular.isArray(condition.value) ){
					condition.value = condition.value.join();
				}
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
	
	$scope.state = global;
	
	var checkboxSelector = new Slick.CheckboxSelectColumn({
        cssClass: "slick-cell-checkboxsel"
	})
	
	$scope.state.actionTableDefinition = {
		columns : [{
			id : "id",
			name : "Stage",
			field : "id",
			width : 200,
			formatter : function(row, cell, value, columnDef, dataContext) {
				var text = '';
				$.each($scope.state.actionStageList, function(index,actionStage) {
					if (actionStage.id === value) {
						text = actionStage.name;
						return false;
					}
					return true;
				});
				return text;
			}
		},{
			id : "fail",
			name : "Fail",
			field : "fail",
			width : 50,
			cssClass: "checkbox-edit-cell",
			formatter: function(row,cell,value,columnDef,dataContext){
				if ($scope.state.actionEditMode){
					return "<input id='actionField-" + dataContext.id + "-fail' class='editor-checkbox' type='checkbox'" + (value === true ? ' checked ' : '') + "'></input>";
				} else {
					return value ? "<img src='img/tick.png'>" : "";
				}
			}
		},{
			id : "warn",
			name : "Warn",
			field : "warn",
			width : 50,
			cssClass: "checkbox-edit-cell",
			formatter: function(row,cell,value,columnDef,dataContext){
				if ($scope.state.actionEditMode){
					return "<input id='actionField-" + dataContext.id + "-warn' class='editor-checkbox' type='checkbox'" + (value === true ? ' checked ' : '') + "'></input>";
				} else {
					return value ? "<img src='img/tick.png'>" : "";
				}
			}
		},{
			id : "notify",
			name : "Notify",
			field : "notify",
			width : 800,
			formatter: function(row,cell,value,columnDef,dataContext){
				if ($scope.state.actionEditMode){
					return "<input id='actionField-" + dataContext.id + "-notify' class='editor-text' type='text' value='" + (value ? value : "") + "'></input>";
				} else {
					return value;
				}
			}
		}],
		options : {
			height : 200,
			forceFitColumns : true,
			fullWidthRows : true
		},
		selectionModel : {
			destroy: function(){},
			init: function(){},
			setSelectedRanges: function(){},
			onSelectedRangesChanged: {
				unsubscribe: function(){},
				subscribe: function(){}
			}
		}
	};
	$scope.state.constraintTableDefinition = {
		columns : [ checkboxSelector.getColumnDefinition(), {
			id : "name",
			name : "Constraint Name",
			field : "name",
			width : 400,
			cssClass : 'edit-click'
		}, {
			id : "enabled",
			name : "Status",
			field : "enabled",
			width : 100,
			formatter : function(row, cell, value, columnDef, dataContext) {
				return '<table><tr><td style="padding: 0px;width: 99%;">' + (value ? 'enabled' : 'disabled') + '</td>'
				    + '<td style="padding: 0px;padding-right:2px;"><button id="' + dataContext.id + '" class="btn btn-mini btn-edit slick-row-hover-button" title="Edit Constraint"><i class="icon-pencil" style="margin-top:0px;"></i></button></td>'
				    + '<td style="padding: 0px;padding-right:2px;"><button id="' + dataContext.id + '" class="btn btn-mini btn-enable slick-row-hover-button" title="Enable Constraint"><i class="icon-ok-circle" style="margin-top:0px;"></i></button></td>'
				    + '<td style="padding: 0px;padding-right:2px;"><button id="' + dataContext.id + '" class="btn btn-mini btn-disable slick-row-hover-button" title="Disable Constraint"><i class="icon-remove-circle" style="margin-top:0px;"></i></button></td>'
				    + '<td style="padding: 0px;padding-right:2px;"><button id="' + dataContext.id + '" class="btn btn-mini btn-delete slick-row-hover-button" title="Delete Constraint"><i class="icon-trash" style="margin-top:0px;"></i></button></td>'
				    + '</tr></table>';
			}
		} ],
		options : {
			height : 200,
			forceFitColumns : true,
			fullWidthRows : true
		},
		plugins : [checkboxSelector],
		selectionModel : new Slick.RowSelectionModel({selectActiveRow: false}),
		emptyMessage : "No Constraints have been defined.<br><a href='#editConstraintModal' data-toggle='modal'>Create</a> a new Constraint?"
	};
	$scope.state.constraintConditionsTableDefinition = {
    	columns : [{
    		id : "conditionTypeId",
    		name : "Operand",
    		field : "conditionTypeId",
    		width : 100,
    		formatter : function(row, cell, value, columnDef, dataContext) {
    			return getConditionType(value).name;
    		}
    	},{
    		id : "operator",
    		name : "Operator",
    		field : "operator",
    		width : 100
    	},{
    		id : "value",
    		name : "Value",
    		field : "value",
    		width : 100,
			formatter : function(row, cell, value, columnDef, dataContext) {
				return '<table><tr><td style="padding: 0px;width: 99%;">' + (value ? value : '') + '</td>'
				    + '<td style="padding: 0px;padding-right:2px;"><button id="' + dataContext.id + '" class="btn btn-mini btn-delete slick-row-hover-button" title="Delete Condition"><i class="icon-trash"></i></button></td>'
				    + '</tr></table>';
			}
    	}],
    	options : {
    		height : 125,
    		forceFitColumns : true,
			fullWidthRows : true
		},
		plugins : [],
		selectionModel : new Slick.RowSelectionModel({selectActiveRow: false}),
		emptyMessage : "Add one or more Conditions to define the Constraint."
    };
	
	$scope.resetConstraint = function() {
		delete $scope.state.addConstraintFormValid;
		delete $scope.state.addConstraintConditionFormValid;
		$scope.state.currentConstraint = {
			conditions: [],
			operator: 'OR'
		};
		$scope.state.currentCondition = {};
		delete $scope.state.currentConditionType;
		delete $scope.state.currentConditionValueType;
		delete $scope.state.actionEditMode;
	}
	
	$scope.resetActions = function() {
		$scope.state.actionTableData = [];
		
		if ($scope.state.currentPolicy) {
			for ( var i = 0 ; i < $scope.state.actionStageList.length ; i++ ) {
				var item = {
					id: $scope.state.actionStageList[i].id
				};
				
				if ($scope.state.currentPolicy.actions[item.id]) {
					for ( var j = 0 ; j < $scope.state.currentPolicy.actions[item.id].length ; j++ ){
						switch ($scope.state.currentPolicy.actions[item.id][j].actionTypeId) {
						case 'fail':
							item.fail = true;
							break;
						case 'warn':
							item.warn = true;
							break;
						case 'notify':
							item.notify = $scope.state.currentPolicy.actions[item.id][j].target;
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
			actions: {},
			threatLevel: 5
		}
		$scope.state.showAddPolicyScreen = true;
		$scope.resetActions();
		setTimeout(function(){
			$scope.constraintGrid.redraw();
		},50);
	}
	
	$scope.savePolicyClick = function() {
		$scope.pushActionDataToModel();
		
		//I copy the item here as I don't want to dirty the UI data with changes needed for the server
		var item = angular.copy($scope.state.currentPolicy);
		composeConditionValues(item);
		//edit
		if ($scope.state.currentPolicyRef) {		    
			$http.put(insightApp.getPolicyUrl(),item).success(function(data, status, headers, config){
				angular.copy($scope.state.currentPolicy, $scope.state.currentPolicyRef);
				updatePolicySummary($scope.state.currentPolicyRef);
				$scope.reset();
			});
		} else {		   			
			$http.post(insightApp.getPolicyUrl(),item).success(function(data, status, headers, config){
				$scope.state.currentPolicy.id = data.id;
				$scope.state.policyList.push($scope.state.currentPolicy);
				updatePolicySummary($scope.state.currentPolicy);
				$scope.reset();
			});
		}
	}
	
	$scope.deletePolicyClick = function(){
		$http.delete(insightApp.getPolicyUrl() + '/' + $scope.state.policyToDelete.id).success(function(data, status, headers, config){
			var idx = $scope.state.policyList.indexOf($scope.state.policyToDelete);
			if (idx >= 0){
				$scope.state.policyList.splice(idx,1);
				$('#deletePolicyConfirmationModal').modal('hide');
			}
		});
	}
	
	$scope.cancelPolicyClick = function(){
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
		if ($scope.state.currentCondition.conditionTypeId
				&& $scope.state.currentCondition.operator
				&& (!getConditionType($scope.state.currentCondition.conditionTypeId).valueTypeId || $scope.state.currentCondition.value)) {
			$scope.state.addConstraintConditionFormValid = true;
		}
	}
	
	$scope.constraintOperandChanged = function() {
		delete $scope.state.currentCondition.operator;
		delete $scope.state.currentCondition.value;
		$scope.state.currentConditionType = getConditionType($scope.state.currentCondition.conditionTypeId);
		$scope.state.currentConditionValueType = getConditionValueType($scope.state.currentConditionType.valueTypeId);
		$scope.validateConstraintCondition();
	}
	
	$scope.editActionsClick = function() {
		$scope.pushActionDataToModel();
		$scope.state.actionEditMode = !$scope.state.actionEditMode;
		$scope.actionGrid.invalidate();
	}
	
	$scope.pushActionDataToModel = function() {
		if ($scope.state.actionEditMode){
			$scope.state.actionTableData = [];
			
			angular.forEach($scope.state.actionStageList, function(value,key){
				$scope.state.actionTableData.push({
					id: value.id,
					fail: $('#actionField-' + value.id + '-fail').is(":checked"),
					warn: $('#actionField-' + value.id + '-warn').is(":checked"),
					notify: $('#actionField-' + value.id + '-notify').val()
				});
			});
		}
	}
	
	$scope.$watch('state.actionTableData',function(newScopeData){
		if ($scope.state.currentPolicy) {
			var handleAction = function(id){
				var result = [];
				
				for ( var i = 0 ; i < newScopeData.length ; i++ ) {
					if (newScopeData[i].id === id) {
						if (newScopeData[i].warn) {
							result.push({
								actionTypeId: 'warn'
							});
						}
						if (newScopeData[i].fail) {
							result.push({
								actionTypeId: 'fail'
							});
						}
						if (newScopeData[i].notify) {
							result.push({
								actionTypeId: 'notify',
								target: newScopeData[i].notify
							});
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
	},true);
	
	insightApp.appId = $location.search().appId;
	
	$http.get(insightApp.getConditionTypeUrl()).success(function(conditionTypeData, status, headers, config) {
    	$scope.state.conditionTypeList = conditionTypeData;
    	$http.get(insightApp.getActionTypeUrl()).success(function(actionTypeData, status, headers, config) {
        	$scope.state.actionTypeList = actionTypeData;
        	$http.get(insightApp.getActionStageUrl()).success(function(actionStageData, status, headers, config) {
        		$scope.state.actionStageList = actionStageData;
        		$http.get(insightApp.getConditionValueTypeUrl()).success(function(conditionValueTypeData, status, headers, config) {
        			$scope.state.conditionValueTypeList = conditionValueTypeData;
	        		$scope.state.policyList = [];
	                $http.get(insightApp.getPolicyUrl()).success(function(data, status, headers, config) {
	                	for ( var i = 0 ; i < data.length ; i++ ){
	                		$scope.state.policyList.push(data[i]);
	                		updatePolicySummary(data[i]);
	                		parseConditionValues(data[i]);
	                	}
	                	
	                	$scope.reset();
	                });
        		});
        	});
    	});
    });
	
	$('#editConstraintModal').live('show', function (event) {
		setTimeout(function(){
			$scope.constraintConditionsGrid.redraw();
		},100);
    });
	
	$('.policy-item').live('mouseover mouseout', function (event) {
        if (event.type == 'mouseover') {
            $(this).find(".btn").show(); 
        } else {

             $(this).find(".btn").hide();
        }
    });
	
	$('.policy-item .btn-edit').live('click', function(){
		for ( var i = 0 ; i < $scope.state.policyList.length ; i++ ) {
			if ( $scope.state.policyList[i].id === $(this).attr('id')) {
				$scope.state.currentPolicy = angular.copy($scope.state.policyList[i]);
				$scope.state.currentPolicyRef = $scope.state.policyList[i];
				$scope.state.showAddPolicyScreen = true;
				$scope.resetActions();
				$scope.validatePolicy();
				setTimeout(function(){
					$scope.actionGrid.invalidate();
					$scope.constraintGrid.redraw();
				},50);

		        //since this event is called outside of angular, we need to force
		        //an apply to get everything mapped up properly
		        $scope.$apply();
			}
		}
    });
	
	$('.policy-item .btn-delete').live('click', function(){
		for ( var i = 0 ; i < $scope.state.policyList.length ; i++ ) {
			if ( $scope.state.policyList[i].id === $(this).attr('id')) {
				$scope.state.policyToDelete = $scope.state.policyList[i];
				$scope.$apply();
				$('#deletePolicyConfirmationModal').modal('show');
				break;
			}
		}
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
    	$scope.validatePolicy();
    	
        //since this event is called outside of angular, we need to force
        //an apply to get everything mapped up properly
        $scope.$apply();
    });
    
    $('#constraintConditionsGrid .slick-row').live('mouseover mouseout', function (event) {
        if (event.type == 'mouseover') {
            $(this).find(".btn").show(); 
        } else {

             $(this).find(".btn").hide();
        }
    });
    
    $('#constraintConditionsGrid .slick-row .btn-delete').live('click', function(){
		$scope.constraintConditionsGrid.dataView.deleteItem($(this).attr('id'));
		$scope.validateConstraint();
		
        //since this event is called outside of angular, we need to force
        //an apply to get everything mapped up properly
        $scope.$apply();
    });
}