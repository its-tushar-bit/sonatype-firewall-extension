describe('RuleController tests', function() {	
	var scope;
	
	//setup our http backend to return what we want
	beforeEach(inject(function(_$httpBackend_, $rootScope, $controller) {
      $httpBackend = _$httpBackend_;
      $httpBackend.expectGET('/rest/policy/conditionType').
          respond([{
        	  id: 'LicenseInList',
        	  availableValues: ['Apache-2.0','EPL-1.0','GPL-2.0','Not Provided','Non-Standard'],
        	  operandName: 'License',
        	  supportedOperators: ['in', 'not in'] 
          }, {
        	  id: 'SecurityVulnerabilityCount',
        	  operandName: 'Security Vulnerability Count',
        	  supportedOperators: ['<','<=','>','>='] 
          }]);

      //inject the controller
      scope = $rootScope.$new();
      $controller(RuleController, {$scope: scope, global: {}});
      $httpBackend.flush();
    }));
	
	it('initial state of the controller should be applied', function() {
		expect(scope.state.conditions.length).toEqual(2);
		expect(scope.state.conditions[0].id).toEqual('LicenseInList');
		expect(scope.state.conditions[0].name).toEqual('License');
		expect(scope.state.conditions[0].operators).toEqual(['in', 'not in']);
		expect(scope.state.conditions[0].values).toEqual(['Apache-2.0','EPL-1.0','GPL-2.0','Not Provided','Non-Standard']);
		expect(scope.state.conditions[1].id).toEqual('SecurityVulnerabilityCount');
		expect(scope.state.conditions[1].name).toEqual('Security Vulnerability Count');
		expect(scope.state.conditions[1].operators).toEqual(['<','<=','>','>=']);
		expect(scope.state.conditions[1].values).toEqual(null);
		
		expect(scope.state.showAddRuleView).toEqual(undefined);
		expect(scope.state.addRuleName).toEqual(undefined);
		expect(scope.state.addRuleOperand).toEqual(undefined);
		expect(scope.state.addRuleOperator).toEqual(undefined);
		expect(scope.state.addRuleValue).toEqual(undefined);
		expect(scope.state.addRuleAction).toEqual(undefined);
		expect(scope.state.secVulnCountSelected).toEqual(undefined);
		expect(scope.state.licCatSelected).toEqual(undefined);
		expect(scope.state.addRuleFormValid).toEqual(undefined);
		expect(scope.state.addRuleConditionFormValid).toEqual(undefined);
		expect(scope.state.addRuleId).toEqual(undefined);
		expect(scope.state.ruleConditions).toEqual([]);
		expect(scope.state.addRuleMatchType).toEqual('any');
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
		
		scope.state.addRuleOperand = 'something';
		scope.validateRuleCondition();
		
		expect(scope.state.addRuleConditionFormValid).toEqual(undefined);
		
		scope.state.addRuleOperator = 'something';
        scope.validateRuleCondition();
		
		expect(scope.state.addRuleConditionFormValid).toEqual(undefined);
		
		scope.state.addRuleValue = 'something';
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
		expect(scope.state.addRuleId).toEqual('id');
	});
	
	it('validate save new rule', function(){
		scope.state.rules = [];
		
		expect(scope.state.rules.length).toEqual(0);
		
		scope.state.addRuleName = 'name';
		scope.state.addRuleAction = 'action';
		scope.state.addRuleMatchType = 'matchType';
		scope.state.ruleConditions = ['condition'];
		
		scope.saveRule();
		
		expect(scope.state.rules.length).toEqual(1);
		expect(scope.state.rules[0].name).toEqual('name');
		expect(scope.state.rules[0].action).toEqual('action');
		expect(scope.state.rules[0].matchType).toEqual('matchType');
		expect(scope.state.rules[0].conditions).toEqual(['condition']);
		expect(scope.state.rules[0].id).toEqual('1');
		expect(scope.state.addRuleName).toEqual(undefined);
		expect(scope.state.addRuleAction).toEqual(undefined);
		//any is the default, doesn't get into undefined state
		expect(scope.state.addRuleMatchType).toEqual('any');
		expect(scope.state.ruleConditions).toEqual([]);
		expect(scope.state.addRuleId).toEqual(undefined);
	});
	
	it('validate save existing rule', function(){
		scope.state.rules = [{
			id: 'id',
			name: 'name',
			conditions: ['condition'],
			matchType: 'matchType',
			action: 'action'
		}];
		
		scope.state.addRuleId = 'id',
		scope.state.addRuleName = 'name2';
		scope.state.addRuleAction = 'action2';
		scope.state.addRuleMatchType = 'matchType2';
		scope.state.ruleConditions = ['condition2'];
		
		scope.saveRule();
		
		expect(scope.state.rules.length).toEqual(1);
		expect(scope.state.rules[0].name).toEqual('name2');
		expect(scope.state.rules[0].action).toEqual('action2');
		expect(scope.state.rules[0].matchType).toEqual('matchType2');
		expect(scope.state.rules[0].conditions).toEqual(['condition2']);
		expect(scope.state.rules[0].id).toEqual('id');
		expect(scope.state.addRuleName).toEqual(undefined);
		expect(scope.state.addRuleAction).toEqual(undefined);
		//any is the default, doesn't get into undefined state
		expect(scope.state.addRuleMatchType).toEqual('any');
		expect(scope.state.ruleConditions).toEqual([]);
		expect(scope.state.addRuleId).toEqual(undefined);
	});
	
	it('validate rule operand change behavior', function(){
		scope.state.addRuleOperand = {
    	    id: 'LicenseInList',
    	    values: ['Apache-2.0','EPL-1.0','GPL-2.0','Not Provided','Non-Standard'],
    	    name: 'License',
    	    operators: ['in', 'not in'] 
        };
		
		scope.ruleOperandChanged();
		
		expect(scope.state.conditionOperators).toEqual(['in', 'not in'])
		expect(scope.state.conditionValues).toEqual(['Apache-2.0','EPL-1.0','GPL-2.0','Not Provided','Non-Standard']);
		
		scope.state.addRuleOperand = {
			id: 'SecurityVulnerabilityCount',
	        name: 'Security Vulnerability Count',
	        operators: ['<','<=','>','>='] 	
		}
		
        scope.ruleOperandChanged();
		
		expect(scope.state.conditionOperators).toEqual(['<','<=','>','>='])
		expect(scope.state.conditionValues).toEqual(undefined);
	});
	
	it('validate enable and disable of rules', function(){
		var item = {
			id: 'id',
			name: 'name',
			conditions: ['condition'],
			matchType: 'matchType',
			action: 'action'
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
		
		expect(item.status).toEqual('enabled');
		
		scope.disableRule();
		
		expect(item.status).toEqual('disabled');
	});
	
	//TODO: need to test the functions that access the grid, but first need to mock the grid object
});