var ruleApp = angular.module('ruleApp', []);

ruleApp.factory('global', function($rootScope) {
    var state = {};
    state.rules = [];
    
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
			id : "status",
			name : "Status",
			field : "status",
			width : 100,
			formatter : function(row, cell, value, columnDef, dataContext) {
				return '<table><tr><td style="padding: 0px;width: 99%;">' + value + '</td>'
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
				return '<table><tr><td style="padding: 0px;width: 99%;">' + ruleApp.getRuleValueText(value) + '</td>'
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

ruleApp.getNextId = function(data) {
	if (!data || data.length < 1) {
		return 1;
	}
	
	return data[data.length - 1].id + 1;
}