describe('RuleController tests', function() {	
	var scope, $httpBackend, $location;
	
	//setup our http backend to return what we want
	beforeEach(inject(function(_$httpBackend_, $rootScope, $controller, _$location_) {
	  $location = _$location_;
	  $location.search('appId','myAppId');
	  ruleApp.appId = 'myAppId';
      $httpBackend = _$httpBackend_;
      $httpBackend.expectGET(ruleApp.getConditionTypeUrl()).
          respond([{
        	  id: 'LicenseInList',
        	  availableValues: ['Apache-2.0','EPL-1.0','GPL-2.0','Not Provided','Non-Standard'],
        	  operandName: 'License',
        	  supportedOperators: ['in', 'not in'],
        	  requiresValue: true
          }, {
        	  id: 'SecurityVulnerabilityCount',
        	  operandName: 'Security Vulnerability Count',
        	  supportedOperators: ['<','<=','>','>='] 
          }]);
      $httpBackend.expectGET(ruleApp.getActionTypeUrl()).
          respond([{
        	  id: 'AddLabel',
        	  name: 'Add label',
        	  availableValues: ['Whitelist','Blacklist','Big no-no','Must have']
          },{
        	  id: 'MarkAsFailed',
        	  name: 'Mark as failed'
          }]);
      $httpBackend.expectGET(ruleApp.getRuleUrl()).
          respond([{
        	  id: 'ruleId1',
        	  name: 'ruleId1',
        	  operator: 'AND',
        	  enabled: true,
        	  actions: [{
        		  actionTypeId: 'MarkAsFailed'
        	  }],
        	  conditions: [{
        		  conditionTypeId: 'LicenseInList',
        		  operator: 'in',
        		  value: 'Not Provided'
        	  }]
          },{
        	  id: 'ruleId2',
        	  name: 'ruleId2',
        	  operator: 'OR',
        	  enabled: false,
        	  actions: [{
        		  actionTypeId: 'AddLabel'
        	  }],
        	  conditions: [{
        		  conditionTypeId: 'SecurityVulnerabilityCount',
        		  operator: '<',
        		  value: '25'
        	  }]
          }]);

      //inject the controller
      scope = $rootScope.$new();
      $controller(RuleController, {$scope: scope, global: {}});
      $httpBackend.flush();
    }));
	
	it('initial state of the controller should be applied', function() {
		expect(scope.state.conditionTypes.length).toEqual(2);
		expect(scope.state.conditionTypes[0].id).toEqual('LicenseInList');
		expect(scope.state.conditionTypes[0].operandName).toEqual('License');
		expect(scope.state.conditionTypes[0].supportedOperators).toEqual(['in', 'not in']);
		expect(scope.state.conditionTypes[0].availableValues).toEqual(['Apache-2.0','EPL-1.0','GPL-2.0','Not Provided','Non-Standard']);
		expect(scope.state.conditionTypes[0].requiresValue).toEqual(true);
		expect(scope.state.conditionTypes[1].id).toEqual('SecurityVulnerabilityCount');
		expect(scope.state.conditionTypes[1].operandName).toEqual('Security Vulnerability Count');
		expect(scope.state.conditionTypes[1].supportedOperators).toEqual(['<','<=','>','>=']);
		expect(scope.state.conditionTypes[1].availableValues).toEqual(null);
		
		expect(scope.state.actionTypes.length).toEqual(2);
		expect(scope.state.actionTypes[0].id).toEqual('AddLabel');
		expect(scope.state.actionTypes[0].name).toEqual('Add label');
		expect(scope.state.actionTypes[0].availableValues).toEqual(['Whitelist','Blacklist','Big no-no','Must have']);
		expect(scope.state.actionTypes[1].id).toEqual('MarkAsFailed');
		expect(scope.state.actionTypes[1].name).toEqual('Mark as failed');
		expect(scope.state.actionTypes[1].availableValues).toEqual(null);
		
		expect(scope.state.showAddRuleView).toEqual(undefined);
		expect(scope.state.addRuleName).toEqual(undefined);
		expect(scope.state.addRuleOperand).toEqual(undefined);
		expect(scope.state.addRuleOperator).toEqual(undefined);
		expect(scope.state.addRuleValue).toEqual(undefined);
		expect(scope.state.addRuleAction).toEqual(undefined);
		expect(scope.state.addRuleFormValid).toEqual(undefined);
		expect(scope.state.addRuleConditionFormValid).toEqual(undefined);
		expect(scope.state.currentRule).toEqual(undefined);
		expect(scope.state.ruleConditions).toEqual([]);
		expect(scope.state.addRuleMatchType).toEqual('OR');
		expect(scope.state.rules.length).toEqual(2);
		expect(scope.state.rules[0].id).toEqual('ruleId1');
		expect(scope.state.rules[0].name).toEqual('ruleId1');
		expect(scope.state.rules[0].matchType).toEqual('AND');
		expect(scope.state.rules[0].enabled).toEqual(true);
		expect(scope.state.rules[0].action).toEqual(scope.state.actionTypes[1]);
		expect(scope.state.rules[0].conditions.length).toEqual(1);
		expect(scope.state.rules[0].conditions[0].operand).toEqual(scope.state.conditionTypes[0]);
		expect(scope.state.rules[0].conditions[0].operator).toEqual('in');
		expect(scope.state.rules[0].conditions[0].value).toEqual('Not Provided');
		expect(scope.state.rules[1].id).toEqual('ruleId2');
		expect(scope.state.rules[1].name).toEqual('ruleId2');
		expect(scope.state.rules[1].matchType).toEqual('OR');
		expect(scope.state.rules[1].enabled).toEqual(false);
		expect(scope.state.rules[1].action).toEqual(scope.state.actionTypes[0]);
		expect(scope.state.rules[1].conditions.length).toEqual(1);
		expect(scope.state.rules[1].conditions[0].operand).toEqual(scope.state.conditionTypes[1]);
		expect(scope.state.rules[1].conditions[0].operator).toEqual('<');
		expect(scope.state.rules[1].conditions[0].value).toEqual('25');
	});
	
	it('validate adding a rule', function(){		
		expect(scope.state.showAddRuleView).toEqual(undefined);
		
		scope.addRule();
		
		expect(scope.state.showAddRuleView).toEqual(true);
		
		//note cancel click simply calls reset() function
		scope.reset();
		
		expect(scope.state.showAddRuleView).toEqual(undefined);
	});
	
	it('validate the rule validation', function(){		
		//clear the var, shouldn't be able to get in this state, but we'll validate anyway
		delete scope.state.addRuleMatchType;
		
		scope.validateRule();
		
		expect(scope.state.addRuleFormValid).toEqual(undefined);
		
		scope.state.ruleConditions = ['something'];
		scope.validateRule();
		
		expect(scope.state.addRuleFormValid).toEqual(undefined);
		
		scope.state.addRuleAction = 'something';
        scope.validateRule();
		
		expect(scope.state.addRuleFormValid).toEqual(undefined);
		
		scope.state.addRuleName = 'something';
        scope.validateRule();
		
		expect(scope.state.addRuleFormValid).toEqual(undefined);
		
		scope.state.addRuleMatchType = 'something';
        scope.validateRule();
		
		expect(scope.state.addRuleFormValid).toEqual(true);
	});
	
	it('validate the rule condition validation', function(){
		scope.validateRuleCondition();
		
		expect(scope.state.addRuleConditionFormValid).toEqual(undefined);
		
		scope.state.addRuleOperand = scope.state.conditionTypes[0];
		scope.validateRuleCondition();
		
		expect(scope.state.addRuleConditionFormValid).toEqual(undefined);
		
		scope.state.addRuleOperator = 'something';
        scope.validateRuleCondition();
		
		expect(scope.state.addRuleConditionFormValid).toEqual(undefined);
		
		scope.state.addRuleValue = 'something';
        scope.validateRuleCondition();
		
		expect(scope.state.addRuleConditionFormValid).toEqual(true);
		
		delete scope.state.addRuleValue;
		scope.state.addRuleOperand = scope.state.conditionTypes[1];
		scope.validateRuleCondition();
		
		expect(scope.state.addRuleConditionFormValid).toEqual(true);
	});
	
	it('validate loading of rule data for edit', function(){
		scope.populateForEdit({
			name: 'name',
			action: 'action',
			matchType: 'matchType',
			id: 'id',
			conditions: ['condition']
		});
		
		expect(scope.state.addRuleFormValid).toEqual(true);
		expect(scope.state.showAddRuleView).toEqual(true);
		expect(scope.state.addRuleName).toEqual('name');
		expect(scope.state.addRuleAction).toEqual('action');
		expect(scope.state.ruleConditions).toEqual(['condition']);
		expect(scope.state.addRuleMatchType).toEqual('matchType');
		expect(scope.state.currentRule.id).toEqual('id');
	});
	
	it('validate save new rule', function(){
		$httpBackend.expectPOST(ruleApp.getRuleUrl(), {
	    	  name: 'ruleId3',
	    	  operator: 'AND',
	    	  actions: [{
	    		  actionTypeId: 'MarkAsFailed'
	    	  }],
	    	  conditions: [{
	    		  conditionTypeId: 'LicenseInList',
	    		  operator: 'in',
	    		  value: 'Not Provided'
	    	  }],
	    	  enabled: true
	      }).respond({
	    	  id: 'ruleId3',
	    	  name: 'ruleId3',
	    	  operator: 'AND',
	    	  actions: [{
	    		  actionTypeId: 'MarkAsFailed'
	    	  }],
	    	  conditions: [{
	    		  conditionTypeId: 'LicenseInList',
	    		  operator: 'in',
	    		  value: 'Not Provided'
	    	  }],
	    	  enabled: true
	      });
		
		scope.state.rules = [];
		
		expect(scope.state.rules.length).toEqual(0);
		
		scope.state.addRuleName = 'ruleId3';
		scope.state.addRuleAction = scope.state.actionTypes[1];
		scope.state.addRuleMatchType = 'AND';
		scope.state.ruleConditions = [{
			operand: scope.state.conditionTypes[0],
			operator: 'in',
			value: 'Not Provided'
		}];
		
		scope.saveRule();
		
		$httpBackend.flush();
		
		expect(scope.state.rules.length).toEqual(1);
		expect(scope.state.rules[0].name).toEqual('ruleId3');
		expect(scope.state.rules[0].action).toEqual(scope.state.actionTypes[1]);
		expect(scope.state.rules[0].matchType).toEqual('AND');
		expect(scope.state.rules[0].conditions).toEqual([{
			operand: scope.state.conditionTypes[0],
			operator: 'in',
			value: 'Not Provided'
		}]);
		expect(scope.state.rules[0].id).toEqual('ruleId3');
		expect(scope.state.addRuleName).toEqual(undefined);
		expect(scope.state.addRuleAction).toEqual(undefined);
		//any is the default, doesn't get into undefined state
		expect(scope.state.addRuleMatchType).toEqual('OR');
		expect(scope.state.ruleConditions).toEqual([]);
		expect(scope.state.currentRule).toEqual(undefined);
	});
	
	it('validate save existing rule', function(){
		$httpBackend.expectPUT(ruleApp.getRuleUrl(), {
			  name: 'ruleId3',
	    	  operator: 'AND',
	    	  actions: [{
	    		  actionTypeId: 'MarkAsFailed'
	    	  }],
	    	  conditions: [{
	    		  conditionTypeId: 'LicenseInList',
	    		  operator: 'in',
	    		  value: 'Not Provided'
	    	  }],
	    	  enabled: true,
	    	  id: 'ruleId3'
	      }).respond({
	    	  id: 'ruleId3',
	    	  name: 'ruleId3',
	    	  operator: 'AND',
	    	  actions: [{
	    		  actionTypeId: 'MarkAsFailed'
	    	  }],
	    	  conditions: [{
	    		  conditionTypeId: 'LicenseInList',
	    		  operator: 'in',
	    		  value: 'Not Provided'
	    	  }],
	    	  enabled: true
	      });
		
		scope.state.rules = [{
			id: 'ruleId3',
			name: 'name',
			conditions: [{
				operand: scope.state.conditionTypes[0],
				operator: 'in',
				value: 'Not Provided'
			}],
			matchType: 'AND',
			action: scope.state.actionTypes[1],
			enabled: true
		}];
		
		scope.state.currentRule = scope.state.rules[0],
		scope.state.addRuleName = 'ruleId3';
		scope.state.addRuleAction = scope.state.actionTypes[1]
		scope.state.addRuleMatchType = 'AND';
		scope.state.ruleConditions = [{
			operand: scope.state.conditionTypes[0],
			operator: 'in',
			value: 'Not Provided'
		}];
		
		scope.saveRule();
		
		$httpBackend.flush();
		
		expect(scope.state.rules.length).toEqual(1);
		expect(scope.state.rules[0].name).toEqual('ruleId3');
		expect(scope.state.rules[0].action).toEqual(scope.state.actionTypes[1]);
		expect(scope.state.rules[0].matchType).toEqual('AND');
		expect(scope.state.rules[0].conditions).toEqual([{
			operand: scope.state.conditionTypes[0],
			operator: 'in',
			value: 'Not Provided'
		}]);
		expect(scope.state.rules[0].id).toEqual('ruleId3');
		expect(scope.state.addRuleName).toEqual(undefined);
		expect(scope.state.addRuleAction).toEqual(undefined);
		//any is the default, doesn't get into undefined state
		expect(scope.state.addRuleMatchType).toEqual('OR');
		expect(scope.state.ruleConditions).toEqual([]);
		expect(scope.state.currentRule).toEqual(undefined);
	});
	
	it('validate rule operand change behavior', function(){
		scope.state.addRuleOperand = scope.state.conditionTypes[0];
		
		scope.state.addRuleOperator = 'something';
		scope.state.addRuleValue = 'something';
		
		scope.ruleOperandChanged();
		
		expect(scope.state.addRuleOperator).toEqual(undefined);
		expect(scope.state.addRuleValue).toEqual(undefined);
		expect(scope.state.addRuleConditionFormValid).toEqual(undefined);
	});
	
	it('validate enable and disable of rules', function(){
		$httpBackend.expectPUT(ruleApp.getRuleUrl(), {
			  name: 'ruleId3',
	    	  operator: 'AND',
	    	  actions: [{
	    		  actionTypeId: 'MarkAsFailed'
	    	  }],
	    	  conditions: [{
	    		  conditionTypeId: 'LicenseInList',
	    		  operator: 'in',
	    		  value: 'Not Provided'
	    	  }],
	    	  enabled: true,
	    	  id: 'ruleId3'
	      }).respond({
	    	  id: 'ruleId3',
	    	  name: 'ruleId3',
	    	  operator: 'AND',
	    	  actions: [{
	    		  actionTypeId: 'MarkAsFailed'
	    	  }],
	    	  conditions: [{
	    		  conditionTypeId: 'LicenseInList',
	    		  operator: 'in',
	    		  value: 'Not Provided'
	    	  }],
	    	  enabled: true
	      });
		
		var item = {
			id: 'ruleId3',
			name: 'ruleId3',
			conditions: [{
				operand: scope.state.conditionTypes[0],
				operator: 'in',
				value: 'Not Provided'
			}],
			matchType: 'AND',
			action: scope.state.actionTypes[1]
		};
		
		//slickgrid mock
        scope.rulesTable = {
			getSelectedRows: function() {
				return [1];
			},
			getDataItem: function(index) {
				return item;
			},
			getData: function() {
				return {
					getItems: function() {
						return [item];
					}
				}
			}
		};
		
		scope.enableRule();
		
		$httpBackend.flush();
		
		expect(item.enabled).toEqual(true);
		
		$httpBackend.expectPUT(ruleApp.getRuleUrl(), {
			  name: 'ruleId3',
	    	  operator: 'AND',
	    	  actions: [{
	    		  actionTypeId: 'MarkAsFailed'
	    	  }],
	    	  conditions: [{
	    		  conditionTypeId: 'LicenseInList',
	    		  operator: 'in',
	    		  value: 'Not Provided'
	    	  }],
	    	  enabled: false,
	    	  id: 'ruleId3'
	      }).respond({
	    	  id: 'ruleId3',
	    	  name: 'ruleId3',
	    	  operator: 'AND',
	    	  actions: [{
	    		  actionTypeId: 'MarkAsFailed'
	    	  }],
	    	  conditions: [{
	    		  conditionTypeId: 'LicenseInList',
	    		  operator: 'in',
	    		  value: 'Not Provided'
	    	  }],
	    	  enabled: false
	      });
		
		scope.disableRule();
		
		$httpBackend.flush();
		
		expect(item.enabled).toEqual(false);
	});
	
	//TODO: need to test the functions that access the grid, but first need to mock the grid object
});