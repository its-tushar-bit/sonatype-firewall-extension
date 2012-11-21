var ruleApp = angular.module('ruleApp', [], function($locationProvider){
	$locationProvider.html5Mode(true);
});

ruleApp.factory('global', function($rootScope) {
    var state = {};
    
    var checkboxRulesSelector = new Slick.CheckboxSelectColumn({
        cssClass: "slick-cell-checkboxsel"
	});
    
    state.rulesTableDef = {
		columns : [ checkboxRulesSelector.getColumnDefinition(), {
			id : "name",
			name : "Rule Name",
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
				    + '<td style="padding: 0px;padding-right:2px;"><button id="' + dataContext.id + '" class="btn btn-mini btn-edit slick-row-hover-button" title="Edit Rule"><i class="icon-pencil"></i></button></td>'
				    + '<td style="padding: 0px;padding-right:2px;"><button id="' + dataContext.id + '" class="btn btn-mini btn-enable slick-row-hover-button" title="Enable Rule"><i class="icon-ok-circle"></i></button></td>'
				    + '<td style="padding: 0px;padding-right:2px;"><button id="' + dataContext.id + '" class="btn btn-mini btn-disable slick-row-hover-button" title="Disable Rule"><i class="icon-remove-circle"></i></button></td>'
				    + '<td style="padding: 0px;padding-right:2px;"><button id="' + dataContext.id + '" class="btn btn-mini btn-delete slick-row-hover-button" title="Delete Rule"><i class="icon-trash"></i></button></td>'
				    + '</tr></table>';
			}
		} ],
		options : {
			height : 200,
			forceFitColumns : true,
			fullWidthRows : true
		},
		plugins : [checkboxRulesSelector],
		selectionModel : new Slick.RowSelectionModel({selectActiveRow: false}),
		emptyMessage : "There are no rules, why don't you <a class='btn-add'>create one</a>?"
	};
    
    state.ruleConditionsTableDef = {
    	columns : [{
    		id : "operand",
    		name : "Operand",
    		field : "operand",
    		formatter : function(row, cell, value, columnDef, dataContext) {
    			return value.operandName;
    		}
    	},{
    		id : "operator",
    		name : "Operator",
    		field : "operator"
    	},{
    		id : "value",
    		name : "Value",
    		field : "value",
			formatter : function(row, cell, value, columnDef, dataContext) {
				return '<table><tr><td style="padding: 0px;width: 99%;">' + (value ? value : '') + '</td>'
				    + '<td style="padding: 0px;padding-right:2px;"><button id="' + dataContext.id + '" class="btn btn-mini btn-delete slick-row-hover-button" title="Delete Condition"><i class="icon-trash"></i></button></td>'
				    + '</tr></table>';
			}
    	}],
    	options : {
    		height : 100,
    		forceFitColumns : true,
			fullWidthRows : true
		},
		plugins : [],
		selectionModel : new Slick.RowSelectionModel({selectActiveRow: false}),
		emptyMessage : "Add one or more Conditions to define the Rule."
    };
    
    return state;
});

ruleApp.createRuleDTO = function(data){
	var dto = {
		name: data.name,
		operator: data.operator,
		actions: [{
			actionTypeId: data.actionTypeId
		}],
		conditions: [],
		enabled: data.enabled,
		id: data.id
    };
	
	for ( var i = 0 ; i < data.conditions.length ; i++ ){
    	dto.conditions.push({
    		conditionTypeId: data.conditions[i].operand.id,
    		operator: data.conditions[i].operator,
    		value: data.conditions[i].value
    	});
    }
	
	return dto;
}

ruleApp.getBaseUrl = function(){
	var idx = location.href.indexOf('/rule-assets/');
	
	if (idx > -1) {
		return location.href.substring(0,idx);
	}
	
	return '';
}

ruleApp.getRuleUrl = function(){
	return ruleApp.getBaseUrl() + '/rest/policy/rule/' + ruleApp.appId;
}

ruleApp.getConditionTypeUrl = function(){
	return ruleApp.getBaseUrl() + '/rest/policy/conditionType';
}

ruleApp.getActionTypeUrl = function(){
	return ruleApp.getBaseUrl() + '/rest/policy/actionType';
}

ruleApp.directive('slickgrid', SlickGridComponent);