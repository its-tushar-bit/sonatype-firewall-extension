var insightApp = angular.module('insightApp', [], function($locationProvider){
	$locationProvider.html5Mode(true);
});

insightApp.factory('global', function($rootScope) {
    var checkboxRulesSelector = new Slick.CheckboxSelectColumn({
        cssClass: "slick-cell-checkboxsel"
	}), state = {
    	//TODO: this will be removed when REST request for this data is in place
    	policyList : [{
    		name: 'policy1',
    		id: 'policy1',
    		constraints: [{
    			id: 'constraint1',
    			name: 'constraint1',
    			conditions: [],
    			operator: 'AND',
    			enabled: true
    		}],
    		actions: []
    	},{
    		name: 'policy2',
    		id: 'policy2',
    		constraints: [{
    			id: 'constraint2',
    			name: 'constraint2',
    			conditions: [],
    			operator: 'AND',
    			enabled: true
    		}],
    		actions: []
    	},{
    		name: 'policy3',
    		id: 'policy3',
    		constraints: [{
    			id: 'constraint3',
    			name: 'constraint3',
    			conditions: [],
    			operator: 'AND',
    			enabled: true
    		}],
    		actions: []
    	},{
    		name: 'policy4',
    		id: 'policy4',
    		constraints: [{
    			id: 'constraint4',
    			name: 'constraint4',
    			conditions: [],
    			operator: 'AND',
    			enabled: true
    		}],
    		actions: []
    	},{
    		name: 'policy5',
    		id: 'policy5',
    		constraints: [{
    			id: 'constraint5',
    			name: 'constraint5',
    			conditions: [],
    			operator: 'AND',
    			enabled: true
    		}],
    		actions: []
    	},{
    		name: 'policy6',
    		id: 'policy6',
    		constraints: [{
    			id: 'constraint6',
    			name: 'constraint6',
    			conditions: [],
    			operator: 'AND',
    			enabled: true
    		}],
    		actions: []
    	},{
    		name: 'policy7',
    		id: 'policy7',
    		constraints: [{
    			id: 'constraint7',
    			name: 'constraint7',
    			conditions: [],
    			operator: 'AND',
    			enabled: true
    		}],
    		actions: []
    	},{
    		name: 'policy8',
    		id: 'policy8',
    		constraints: [{
    			id: 'constraint8',
    			name: 'constraint8',
    			conditions: [],
    			operator: 'AND',
    			enabled: true
    		}],
    		actions: []
    	}],
    	//TODO: this will be removed when REST request for this data is in place
    	actionTypeList : [{
    		context: 'Procure',
    		id: 'procure'
    	},{
    		context: 'Develop',
    		id: 'develop'
    	},{
    		context: 'Build',
    		id: 'build'
    	},{
    		context: 'Release',
    		id: 'release'
    	},{
    		context: 'Operate',
    		id: 'operate'
    	}],
    	//TODO: this will be removed when REST request for this data is in place
    	constraintList : [{
    		name: 'myname',
    		status: 'status'
    	},{
    		name: 'myname2',
    		status: 'status2'
    	},{
    		name: 'myname3',
    		status: 'status3'
    	},{
    		name: 'myname4',
    		status: 'status4'
    	},{
    		name: 'myname5',
    		status: 'status5'
    	},{
    		name: 'myname6',
    		status: 'status6'
    	},{
    		name: 'myname7',
    		status: 'status7'
    	},{
    		name: 'myname8',
    		status: 'status8'
    	}],
    	actionTableDefinition : {
    		columns : [{
    			id : "context",
    			name : "Context",
    			field : "context"
    		},{
    			id : "fail",
    			name : "Fail",
    			field : "fail"
    		},{
    			id : "warn",
    			name : "Warn",
    			field : "warn"
    		},{
    			id : "notify",
    			name : "Notify",
    			field : "notify"
    		}],
    		options : {
    			height : 200,
    			forceFitColumns : true,
    			fullWidthRows : true
    		},
    		selectionModel : new Slick.RowSelectionModel({selectActiveRow: false})
    	},
    	constraintTableDefinition : {
    		columns : [ checkboxRulesSelector.getColumnDefinition(), {
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
    		emptyMessage : "No Constraints have been defined.<br><a href='#newConstraintModal' data-toggle='modal'>Create</a> a new Constraint?"
    	},
    	constraintConditionsTableDefinition : {
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
        		height : 125,
        		forceFitColumns : true,
    			fullWidthRows : true
    		},
    		plugins : [],
    		selectionModel : new Slick.RowSelectionModel({selectActiveRow: false}),
    		emptyMessage : "Add one or more Conditions to define the Rule."
        }
    };
        
    return state;
});

insightApp.getBaseUrl = function(){
	var idx = location.href.indexOf('/policy-assets/');
	
	if (idx > -1) {
		return location.href.substring(0,idx);
	}
	
	return '';
}

insightApp.getConditionTypeUrl = function(){
	return insightApp.getBaseUrl() + '/rest/policy/conditionType';
}

insightApp.directive('slickgrid', SlickGridComponent);