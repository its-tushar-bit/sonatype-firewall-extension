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
	
	function parseConditionValue(conditionTypeId, valueId, conditionValueType, valueModifier){
		var conditionValueObject = getConditionValue(conditionTypeId, valueId);
		if (conditionValueObject){
			switch (conditionValueType.dataType){
			case 'License':
				return conditionValueObject.shortDisplayName;
			case 'MatchState':
			case 'LicenseStatus':
			case 'SecurityVulnerabilityStatus':
				return conditionValueObject.name;
			}			
		}
		
		if (conditionValueType.id === 'AgeInDaysValueType'){			
			if ( valueId > 365 ) {
				return (valueId / 365).toFixed( valueId % 365 ? 1 : 0 ) + ' years';
			} else if ( valueId > 30 ) {
				return (valueId / 30).toFixed(0) + ' months';
			} else {
				return valueId + ' days';
			}
		}
		
		return valueId;
	}
	
	function parseConditionValues(data) {
		angular.forEach(data.constraints, function(constraint,constraintIndex){
			angular.forEach(constraint.conditions, function(condition, conditionIndex){
				if ( condition.value ){
					var conditionValueType = getConditionValueType(getConditionType(condition.conditionTypeId).valueTypeId);
					condition.valueText = '';
					var parts = condition.value.split(',');
					if ( parts.length > 1 ){
						condition.value = parts;
						for ( var i = 0 ; i < condition.value.length ; i++ ){
							var valueText = parseConditionValue(condition.conditionTypeId, condition.value[i], conditionValueType, condition.valueModifier);
							if (condition.valueText){
								condition.valueText += ', ';
							}
							condition.valueText += valueText;
						}
					} else {
						condition.valueText = parseConditionValue(condition.conditionTypeId, condition.value, conditionValueType, condition.valueModifier);
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
	
	function getConditionValue(conditionTypeId, valueId){
		var conditionValueType = getConditionValueType(getConditionType(conditionTypeId).valueTypeId);
		
		if (!conditionValueType.availableValues){
			return valueId;
		}
		
		for ( var i = 0 ; i < conditionValueType.availableValues.length ; i++ ){
			if ( conditionValueType.availableValues[i].id === valueId ){
				return conditionValueType.availableValues[i];
			}
		}
		
		return null;
	}
	
	function applyConditionValueModifier(valueTypeId, value, modifier) {
		if ( valueTypeId === 'AgeInDaysValueType' ){
			switch(modifier){
			case 'y':
				return value * 365;
			case 'm':
				return value * 30;
			}
		}
		
		return value;
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
				return '<table><tr><td style="padding: 0px;width: 99%;">' + (dataContext.valueText ? dataContext.valueText : (value ? value : '')) + '</td>'
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
		$scope.state.currentCondition = {
			valueModifier: 'y'
		};
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
		showHttpMask('Saving policy...');
		//edit
		if ($scope.state.currentPolicy.id) {		    
			$http.put(insightApp.getPolicyUrl(),item).success(function(data, status, headers, config){
				updatePolicySummary(data);
        		parseConditionValues(data);
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
        		parseConditionValues(data);
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
	
	$scope.addConstraintCondition = function() {		
		if ( $scope.state.currentCondition.value ){
			var conditionValueType = getConditionValueType(getConditionType($scope.state.currentCondition.conditionTypeId).valueTypeId);
			$scope.state.currentCondition.valueText = '';
			var parts = $scope.state.currentCondition.value.split(',');
			if ( parts.length > 1 ){
				$scope.state.currentCondition.value = parts;
				for ( var i = 0 ; i < $scope.state.currentCondition.value.length ; i++ ){
					$scope.state.currentCondition.value[i] = applyConditionValueModifier(conditionValueType.id, $scope.state.currentCondition.value[i], $scope.state.currentCondition.valueModifier);
					var valueText = parseConditionValue($scope.state.currentCondition.conditionTypeId, $scope.state.currentCondition.value[i], conditionValueType, $scope.state.currentCondition.valueModifier);
					if ($scope.state.currentCondition.valueText){
						$scope.state.currentCondition.valueText += ', ';
					}
					$scope.state.currentCondition.valueText += valueText;
				}
			} else {
				$scope.state.currentCondition.value = applyConditionValueModifier(conditionValueType.id, $scope.state.currentCondition.value, $scope.state.currentCondition.valueModifier);
				$scope.state.currentCondition.valueText = parseConditionValue($scope.state.currentCondition.conditionTypeId, $scope.state.currentCondition.value, conditionValueType, $scope.state.currentCondition.valueModifier);
			}
		}
		
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
	                		parseConditionValues(data[i]);
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