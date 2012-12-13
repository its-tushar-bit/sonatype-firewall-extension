var insightApp = angular.module('insightApp', [], function($locationProvider){
	$locationProvider.html5Mode(true);
});

insightApp.factory('global', function($rootScope) {
    var checkboxSelector = new Slick.CheckboxSelectColumn({
        cssClass: "slick-cell-checkboxsel"
	}), state = {
    	//TODO: this will be removed when REST request for this data is in place
    	policyList : [{
    		name: 'policy1',
    		id: 'policy1',
    		constraints: [{
    			id: 'constraint1',
    			name: 'constraint1',
    			conditions: [{
    				conditionTypeId: 'LicenseCategory',
    				operator: 'is',
    				value: 'COPYLEFT'
    			},{
    				conditionTypeId: 'SecurityVulnerability',
    				operator: 'present'
    			}],
    			operator: 'AND',
    			enabled: true
    		}],
    		actions : {
		        procure : [{
		            actionTypeId : 'fail',
		            target : null
		        }, {
		            actionTypeId : 'notify',
		            target : 'notify@notify.com'
		        }],
		        develop : [{
		            actionTypeId : 'fail',
		            target : null
		        }],
		        release : [{
		            actionTypeId : 'warn',
		            target : null
		        },{
		            actionTypeId : 'notify',
		            target : 'anotheremail@mail.com'
		        }],
		        operate : [{
		            actionTypeId : 'warn',
		            target : null
		        }]
		    }
    	},{
    		name: 'policy2',
    		id: 'policy2',
    		constraints: [{
    			id: 'constraint2',
    			name: 'constraint2',
    			conditions: [{
    				conditionTypeId: 'LicenseCategory',
    				operator: 'is',
    				value: 'COPYLEFT'
    			},{
    				conditionTypeId: 'SecurityVulnerability',
    				operator: 'present'
    			}],
    			operator: 'AND',
    			enabled: true
    		}],
    		actions : {
		        procure : [{
		            actionTypeId : 'fail',
		            target : null
		        }, {
		            actionTypeId : 'notify',
		            target : 'notify@notify.com'
		        }],
		        develop : [{
		            actionTypeId : 'fail',
		            target : null
		        }],
		        release : [{
		            actionTypeId : 'warn',
		            target : null
		        },{
		            actionTypeId : 'notify',
		            target : 'anotheremail@mail.com'
		        }],
		        operate : [{
		            actionTypeId : 'warn',
		            target : null
		        }]
		    }
    	},{
    		name: 'policy3',
    		id: 'policy3',
    		constraints: [{
    			id: 'constraint3',
    			name: 'constraint3',
    			conditions: [{
    				conditionTypeId: 'LicenseCategory',
    				operator: 'is',
    				value: 'COPYLEFT'
    			},{
    				conditionTypeId: 'SecurityVulnerability',
    				operator: 'present'
    			}],
    			operator: 'AND',
    			enabled: true
    		}],
    		actions : {
		        procure : [{
		            actionTypeId : 'fail',
		            target : null
		        }, {
		            actionTypeId : 'notify',
		            target : 'notify@notify.com'
		        }],
		        develop : [{
		            actionTypeId : 'fail',
		            target : null
		        }],
		        release : [{
		            actionTypeId : 'warn',
		            target : null
		        },{
		            actionTypeId : 'notify',
		            target : 'anotheremail@mail.com'
		        }],
		        operate : [{
		            actionTypeId : 'warn',
		            target : null
		        }]
		    }
    	},{
    		name: 'policy4',
    		id: 'policy4',
    		constraints: [{
    			id: 'constraint4',
    			name: 'constraint4',
    			conditions: [{
    				conditionTypeId: 'LicenseCategory',
    				operator: 'is',
    				value: 'COPYLEFT'
    			},{
    				conditionTypeId: 'SecurityVulnerability',
    				operator: 'present'
    			}],
    			operator: 'AND',
    			enabled: true
    		}],
    		actions : {
		        procure : [{
		            actionTypeId : 'fail',
		            target : null
		        }, {
		            actionTypeId : 'notify',
		            target : 'notify@notify.com'
		        }],
		        develop : [{
		            actionTypeId : 'fail',
		            target : null
		        }],
		        release : [{
		            actionTypeId : 'warn',
		            target : null
		        },{
		            actionTypeId : 'notify',
		            target : 'anotheremail@mail.com'
		        }],
		        operate : [{
		            actionTypeId : 'warn',
		            target : null
		        }]
		    }
    	},{
    		name: 'policy5',
    		id: 'policy5',
    		constraints: [{
    			id: 'constraint5',
    			name: 'constraint5',
    			conditions: [{
    				conditionTypeId: 'LicenseCategory',
    				operator: 'is',
    				value: 'COPYLEFT'
    			},{
    				conditionTypeId: 'SecurityVulnerability',
    				operator: 'present'
    			}],
    			operator: 'AND',
    			enabled: true
    		}],
    		actions : {
		        procure : [{
		            actionTypeId : 'fail',
		            target : null
		        }, {
		            actionTypeId : 'notify',
		            target : 'notify@notify.com'
		        }],
		        develop : [{
		            actionTypeId : 'fail',
		            target : null
		        }],
		        release : [{
		            actionTypeId : 'warn',
		            target : null
		        },{
		            actionTypeId : 'notify',
		            target : 'anotheremail@mail.com'
		        }],
		        operate : [{
		            actionTypeId : 'warn',
		            target : null
		        }]
		    }
    	},{
    		name: 'policy6',
    		id: 'policy6',
    		constraints: [{
    			id: 'constraint6',
    			name: 'constraint6',
    			conditions: [{
    				conditionTypeId: 'LicenseCategory',
    				operator: 'is',
    				value: 'COPYLEFT'
    			},{
    				conditionTypeId: 'SecurityVulnerability',
    				operator: 'present'
    			}],
    			operator: 'AND',
    			enabled: true
    		}],
    		actions : {
		        procure : [{
		            actionTypeId : 'fail',
		            target : null
		        }, {
		            actionTypeId : 'notify',
		            target : 'notify@notify.com'
		        }],
		        develop : [{
		            actionTypeId : 'fail',
		            target : null
		        }],
		        release : [{
		            actionTypeId : 'warn',
		            target : null
		        },{
		            actionTypeId : 'notify',
		            target : 'anotheremail@mail.com'
		        }],
		        operate : [{
		            actionTypeId : 'warn',
		            target : null
		        }]
		    }
    	},{
    		name: 'policy7',
    		id: 'policy7',
    		constraints: [{
    			id: 'constraint7',
    			name: 'constraint7',
    			conditions: [{
    				conditionTypeId: 'LicenseCategory',
    				operator: 'is',
    				value: 'COPYLEFT'
    			},{
    				conditionTypeId: 'SecurityVulnerability',
    				operator: 'present'
    			}],
    			operator: 'AND',
    			enabled: true
    		}],
    		actions : {
		        procure : [{
		            actionTypeId : 'fail',
		            target : null
		        }, {
		            actionTypeId : 'notify',
		            target : 'notify@notify.com'
		        }],
		        develop : [{
		            actionTypeId : 'fail',
		            target : null
		        }],
		        release : [{
		            actionTypeId : 'warn',
		            target : null
		        },{
		            actionTypeId : 'notify',
		            target : 'anotheremail@mail.com'
		        }],
		        operate : [{
		            actionTypeId : 'warn',
		            target : null
		        }]
		    }
    	},{
    		name: 'policy8',
    		id: 'policy8',
    		constraints: [{
    			id: 'constraint8',
    			name: 'constraint8',
    			conditions: [{
    				conditionTypeId: 'LicenseCategory',
    				operator: 'is',
    				value: 'COPYLEFT'
    			},{
    				conditionTypeId: 'SecurityVulnerability',
    				operator: 'present'
    			}],
    			operator: 'AND',
    			enabled: true
    		}],
    		actions : {
		        procure : [{
		            actionTypeId : 'fail',
		            target : null
		        }, {
		            actionTypeId : 'notify',
		            target : 'notify@notify.com'
		        }],
		        develop : [{
		            actionTypeId : 'fail',
		            target : null
		        }],
		        release : [{
		            actionTypeId : 'warn',
		            target : null
		        },{
		            actionTypeId : 'notify',
		            target : 'anotheremail@mail.com'
		        }],
		        operate : [{
		            actionTypeId : 'warn',
		            target : null
		        }]
		    }
    	}],
    	actionContextList : [{
    		name: 'Procure',
    		id: 'procure'
    	},{
    		name: 'Develop',
    		id: 'develop'
    	},{
    		name: 'Build',
    		id: 'build'
    	},{
    		name: 'Release',
    		id: 'release'
    	},{
    		name: 'Operate',
    		id: 'operate'
    	}],
    	//TODO: this will be removed when REST request for this data is in place
    	actionTypeList : [{
    		id: 'fail',
    		name: 'fail',
    	},{
    		id: 'warn',
    		name: 'Warn'
    	},{
    		id: 'notify',
    		name: 'Notify',
    		requiresTarget: true
    	}],
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

insightApp.directive('slickgrid', SlickGridComponent);