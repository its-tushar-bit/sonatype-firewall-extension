function InsightPolicyController($scope, global, $http) {
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
				
				if (isAvailableStage(value)){
					return text;
				} else {
					return "<div class='masked-cell' title='This stage is under development'>" + text + "</div>";
				}
			}
		},{
			id : "fail",
			name : "Fail",
			field : "fail",
			width : 60,
			cssClass: "checkbox-edit-cell",
			headerCssClass: "header-centered",
			formatter: function(row,cell,value,columnDef,dataContext){
				var prefix = '';
				var suffix = '';
				
				if (!isAvailableStage(dataContext.id)){
					prefix = "<div class='masked-cell' title='This stage is under development'>";
					suffix = "</div>";
				}
				
				if ($scope.state.actionEditMode){
					return prefix + "<input id='actionField-" + dataContext.id + "-fail' name='actionField-" + dataContext.id + "' " + (prefix.length ? 'disabled' : '') + " type='radio'" + (value === true ? ' checked ' : '') + "'></input>" + suffix;
				} else {
					return prefix + (value ? "<img src='img/tick.png'>" : "") + suffix;
				}
			}
		},{
			id : "warn",
			name : "Warn",
			field : "warn",
			width : 60,
			cssClass: "checkbox-edit-cell",
			headerCssClass: "header-centered",
			formatter: function(row,cell,value,columnDef,dataContext){
				var prefix = '';
				var suffix = '';
				
				if (!isAvailableStage(dataContext.id)){
					prefix = "<div class='masked-cell' title='This stage is under development'>";
					suffix = "</div>";
				}
				
				if ($scope.state.actionEditMode){
					return prefix + "<input id='actionField-" + dataContext.id + "-warn' name='actionField-" + dataContext.id + "' " + (prefix.length ? 'disabled' : '') + " type='radio'" + (value === true ? ' checked ' : '') + "'></input>" + suffix;
				} else {
					return prefix + (value ? "<img src='img/tick.png'>" : "") + suffix;
				}
			}
		},{
			id : "none",
			name : "Do Nothing",
			field : "none",
			width : 75,
			cssClass: "checkbox-edit-cell",
			headerCssClass: "header-centered",
			formatter: function(row,cell,value,columnDef,dataContext){
				var prefix = '';
				var suffix = '';
				
				if (!isAvailableStage(dataContext.id)){
					prefix = "<div class='masked-cell' title='This stage is under development'>";
					suffix = "</div>";
				}
				
				if ($scope.state.actionEditMode){
					return prefix + "<input id='actionField-" + dataContext.id + "-none' name='actionField-" + dataContext.id + "' " + (prefix.length ? 'disabled' : '') + " type='radio'" + (!dataContext.warn && !dataContext.fail ? ' checked ' : '') + "'></input>" + suffix;
				} else {
					return prefix + (!dataContext.warn && !dataContext.fail ? "<img src='img/tick.png'>" : "") + suffix;
				}
			}
		}],
		options : {
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
		columns : [ {
			id : "name",
			name : "Constraint Name",
			field : "name",
			width : 400,
			cssClass : 'edit-click',
			formatter : function(row, cell, value, columnDef, dataContext) {
				return '<table><tr><td style="padding: 0px;width: 99%;">' + value + '</td>'
					+ '<td style="padding: 0px;padding-right:2px;"><button id="' + dataContext.id + '" class="btn btn-mini btn-add slick-row-hover-button" title="Add Constraint"><i class="icon-plus-sign" style="margin-top:0px;"></i></button></td>'
				    + '<td style="padding: 0px;padding-right:2px;"><button id="' + dataContext.id + '" class="btn btn-mini btn-edit slick-row-hover-button" title="Edit Constraint"><i class="icon-pencil" style="margin-top:0px;"></i></button></td>'
				    + '<td style="padding: 0px;padding-right:2px;"><button id="' + dataContext.id + '" class="btn btn-mini btn-delete slick-row-hover-button" title="Delete Constraint"><i class="icon-trash" style="margin-top:0px;"></i></button></td>'
				    + '</tr></table>';
			}
		} ],
		options : {
			height : 200,
			forceFitColumns : true,
			fullWidthRows : true
		},
		selectionModel : new Slick.RowSelectionModel({selectActiveRow: false}),
		emptyMessage : "No Constraints have been defined.<br><a href='#editConstraintModal' data-toggle='modal'>Create</a> a new Constraint?"
	};
	
	$scope.resetConstraint = function() {
		delete $scope.state.addConstraintFormValid;
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
		delete $scope.state.showAddPolicyScreen;
		if ($scope.constraintGrid) {
			$scope.constraintGrid.setSelectedRows([]);
		}
		$scope.resetActions();
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
		setTimeout(function(){
			$scope.constraintGrid.redraw();
		},50);
	}
	
	$scope.savePolicyClick = function() {
		$scope.pushActionDataToModel();
		
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
			$http.post(insightApp.getPolicyUrl(),item).success(function(data, status, headers, config){
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
		$http.delete(insightApp.getPolicyUrl() + '/' + $scope.state.policyToDelete.id).success(function(data, status, headers, config){
			var idx = $scope.state.policyList.indexOf($scope.state.policyToDelete);
			if (idx >= 0){
				$scope.state.policyList.splice(idx,1);
			}
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
		$scope.constraintGrid.dataView.deleteItem($scope.state.constraintToDelete.id);
    	$scope.validatePolicy();
    	$('#deleteConstraintConfirmationModal').modal('hide');
	}
	
	$scope.validateConstraint = function() {
		delete $scope.state.addConstraintFormValid;
		delete $scope.state.conditionsValid;
		
		for (var i = 0 ; i < $scope.state.currentConstraint.conditions.length ; i++){
			if ($scope.state.currentConstraint.conditions[i].valueType && !$scope.state.currentConstraint.conditions[i].value){
				return;
			}
		}
		
		$scope.state.conditionsValid = true;
		
		if ($scope.state.currentConstraint.name
			&& $scope.state.currentConstraint.operator) {
			$scope.state.addConstraintFormValid = true;
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
		$scope.pushActionDataToModel();
		$scope.state.actionEditMode = !$scope.state.actionEditMode;
		$scope.actionGrid.invalidate();
	}
	
	$scope.pushActionDataToModel = function(newData) {
		if ($scope.state.actionEditMode){			
			if (newData) {
				$scope.state.actionTableData = newData;
			} else {
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
			
			if ($scope.state.currentPolicy) {
				var handleAction = function(id){
					var result = [];
					
					for ( var i = 0 ; i < $scope.state.actionTableData.length ; i++ ) {
						if ($scope.state.actionTableData[i].id === id) {
							if ($scope.state.actionTableData[i].warn) {
								result.push({
									actionTypeId: 'warn'
								});
							}
							if ($scope.state.actionTableData[i].fail) {
								result.push({
									actionTypeId: 'fail'
								});
							}
							if ($scope.state.actionTableData[i].notify) {
								result.push({
									actionTypeId: 'notify',
									target: $scope.state.actionTableData[i].notify
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
		}
	}
	
	$scope.$watch('state.actionTableData',function(newScopeData){
		$scope.pushActionDataToModel(newScopeData);
	},true);
	
	showHttpMask('Loading data from server...');
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
	                		addUIConditionData(data[i]);
	                	}
	                	
	                	$scope.reset();
	                	hideHttpMask();
	                }).error(function(data, status, headers, config){
	                	handleHttpError('Policy Initialization Error', data, status);
	            	});
        		}).error(function(data, status, headers, config){
        			handleHttpError('Condition Value Type Initialization Error', data, status);
        		});
        	}).error(function(data, status, headers, config){
        		handleHttpError('Action Stage Initialization Error', data, status);
        	});
    	}).error(function(data, status, headers, config){
    		handleHttpError('Action Type Initialization Error', data, status);
    	});
    }).error(function(data, status, headers, config){
    	handleHttpError('Condition Type Initialization Error', data, status);
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
	
	$('#constraintGrid .slick-row .btn-add').live('click', function(){
		$scope.resetConstraint();
    	
        //since this event is called outside of angular, we need to force
        //an apply to get everything mapped up properly
        $scope.$apply();
        
        $('#editConstraintModal').modal('show');
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
    
    $('#constraintGrid .slick-row .btn-delete').live('click', function(){
    	$scope.state.constraintToDelete = $scope.constraintGrid.dataView.getItem($scope.constraintGrid.dataView.getIdxById($(this).attr('id')));
    	
        //since this event is called outside of angular, we need to force
        //an apply to get everything mapped up properly
        $scope.$apply();
        
        $('#deleteConstraintConfirmationModal').modal('show');
    });
}