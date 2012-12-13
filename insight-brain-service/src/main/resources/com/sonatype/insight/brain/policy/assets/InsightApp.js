var insightApp = angular.module('insightApp', [], function($locationProvider){
	$locationProvider.html5Mode(true);
});

insightApp.factory('global', function($rootScope) {
    var checkboxSelector = new Slick.CheckboxSelectColumn({
        cssClass: "slick-cell-checkboxsel"
	}), state = {
    	actionTableDefinition : {
    		columns : [{
    			id : "id",
    			name : "Context",
    			field : "id",
    			width : 200,
    			formatter : function(row, cell, value, columnDef, dataContext) {
    				var text = '';
    				$.each(state.actionContextList, function(index,actionContext) {
    					if (actionContext.id === value) {
    						text = actionContext.name;
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
    			formatter : function(row, cell, value, columnDef, dataContext) {
    				if ( value ){
    					return '<div class="icon-check"></div>';
    				}
    				return '';
    			}
    		},{
    			id : "warn",
    			name : "Warn",
    			field : "warn",
    			width : 50,
    			formatter : function(row, cell, value, columnDef, dataContext) {
    				if ( value ){
    					return '<div class="icon-check"></div>';
    				}
    				return '';
    			}
    		},{
    			id : "notify",
    			name : "Notify",
    			field : "notify",
    			width : 800
    		}],
    		options : {
    			height : 200,
    			forceFitColumns : true,
    			fullWidthRows : true
    		},
    		selectionModel : new Slick.RowSelectionModel({selectActiveRow: false})
    	},
    	constraintTableDefinition : {
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
    	},
    	constraintConditionsTableDefinition : {
        	columns : [{
        		id : "operand",
        		name : "Operand",
        		field : "operand",
        		width : 100,
        		formatter : function(row, cell, value, columnDef, dataContext) {
        			return value.name;
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

insightApp.getActionTypeUrl = function(){
	return insightApp.getBaseUrl() + '/rest/policy/actionType';
}

insightApp.getActionContextUrl = function(){
	return insightApp.getBaseUrl() + '/rest/policy/contextType';
}

insightApp.getPolicyUrl = function(){
	return insightApp.getBaseUrl() + '/rest/policy/' + insightApp.appId;
}

insightApp.directive('slickgrid', SlickGridComponent);