var ruleApp = angular.module('ruleApp', []);

ruleApp.factory('global', function($rootScope) {
    var state = {};
    state.rules = [];
    
    var checkboxRulesSelector = new Slick.CheckboxSelectColumn({
        cssClass: "slick-cell-checkboxsel"
	});
    
    var checkboxRuleConditionsSelector = new Slick.CheckboxSelectColumn({
        cssClass: "slick-cell-checkboxsel"
	});
    
    state.rulesTableDef = {
		columns : [ checkboxRulesSelector.getColumnDefinition(), {
			id : "name",
			name : "Rule Name",
			field : "name",
			width : 500
		}, {
			id : "status",
			name : "Status",
			field : "status",
			width : 100,
			formatter : function(row, cell, value, columnDef, dataContext) {
				return '<table><tr><td style="padding: 0px;width: 99%;">' + value + '</td><td style="padding: 0px;"><button id="' + dataContext.id + '" style="height:18px;padding-top:1px;display:none" class="btn btn-small" title="Edit Rule">Edit</button></td></tr></table>';
			}
		} ],
		options : {
			height : 200,
			forceFitColumns : true,
			fullWidthRows : true
		},
		plugins : [checkboxRulesSelector],
		selectionModel : new Slick.RowSelectionModel({selectActiveRow: false})
	};
    
    state.ruleConditionsTableDef = {
    	columns : [ checkboxRuleConditionsSelector.getColumnDefinition(), {
    		id : "operand",
    		name : "Operand",
    		field : "operand",
    		formatter : function(row, cell, value, columnDef, dataContext) {
    			return ruleApp.getRuleOperandText(value);
			}
    	},{
    		id : "operator",
    		name : "Operator",
    		field : "operator",
    		formatter : function(row, cell, value, columnDef, dataContext) {
    			return ruleApp.getRuleOperatorText(value);
			}
    	},{
    		id : "value",
    		name : "Value",
    		field : "value",
    		formatter : function(row, cell, value, columnDef, dataContext) {
    			return ruleApp.getRuleValueText(value);
			}
    	}],
    	options : {
    		height : 100,
    		forceFitColumns : true,
			fullWidthRows : true
		},
		plugins : [checkboxRuleConditionsSelector],
		selectionModel : new Slick.RowSelectionModel({selectActiveRow: false})
    };
    
    return state;
});

ruleApp.directive('slickgrid', SlickGridComponent);

ruleApp.getRuleOperandText = function(val) {
	switch (val) {
	case 'secVuln':
		return 'Security Vulnerability Count';
	case 'licCat':
		return 'License Category';
	}
}

ruleApp.getRuleOperatorText = function(val) {
	switch (val) {
	case 'equal':
		return 'is equal to';
	case 'notEqual':
		return 'is not equal to';
	case 'lessThan':
		return 'is less than';
	case 'lessThanEqual':
		return 'is less than or equal to';
	case 'greaterThan':
		return 'is greater than';
	case 'greaterThanEqual':
		return 'is greater than or equal to';
	}
}

ruleApp.getRuleValueText = function(val) {
	switch (val) {
	case 'copyLeft':
		return 'Copyleft';
	case 'nonStandard':
		return 'Non-Standard';
	case 'weakCopyLeft':
		return 'Weak Copyleft';
	case 'liberal':
		return 'Liberal';
	}
	
	//must be numeric value
	return val;
}