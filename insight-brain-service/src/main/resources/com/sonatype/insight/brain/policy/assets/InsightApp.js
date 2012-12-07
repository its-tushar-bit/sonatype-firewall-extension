var insightApp = angular.module('insightApp', [], function($locationProvider){
	$locationProvider.html5Mode(true);
});

insightApp.factory('global', function($rootScope) {
    var checkboxRulesSelector = new Slick.CheckboxSelectColumn({
        cssClass: "slick-cell-checkboxsel"
	}), state = {
    	//TODO: this will be removed when REST request for this data is in place
    	policyConstraintCount : 1,
    	//TODO: this will be removed when REST request for this data is in place
    	policyConstraintList : [{
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
    	policyConstraintTableDefinition : {
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
    	}
    };
        
    return state;
});

insightApp.directive('slickgrid', SlickGridComponent);