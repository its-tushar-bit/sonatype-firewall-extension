describe('RuleController tests', function() {	
	it('initial state of the controller should be applied', function() {
		var scope = {};
		var global = {};
		
		var controller = new RuleController(scope, global);
		
		expect(scope.state).toEqual(global);
		expect(scope.state.showAddRuleView).toBe(undefined);
		expect(scope.state.addRuleName).toBe(undefined);
		expect(scope.state.addRuleOperand).toBe(undefined);
		expect(scope.state.addRuleOperator).toBe(undefined);
		expect(scope.state.addRuleValue).toBe(undefined);
		expect(scope.state.addRuleAction).toBe(undefined);
		expect(scope.state.secVulnCountSelected).toBe(undefined);
		expect(scope.state.licCatSelected).toBe(undefined);
		expect(scope.state.addRuleFormValid).toBe(undefined);
		expect(scope.state.addRuleConditionFormValid).toBe(undefined);
		expect(scope.state.addRuleId).toBe(undefined);
		expect(scope.state.ruleConditions).toEqual([]);
		expect(scope.state.addRuleMatchType).toEqual('any');
	});
	
	it('validate adding a rule', function(){
		var scope = {};
		
		var controller = new RuleController(scope, {});
		
		expect(scope.state.showAddRuleView).toEqual(undefined);
		
		scope.addRule();
		
		expect(scope.state.showAddRuleView).toEqual(true);
		
		//note cancel click simply calls reset() function
		scope.reset();
		
		expect(scope.state.showAddRuleView).toEqual(undefined);
	});
	
	it('validate the rule validation', function(){
		var scope = {};
		
		var controller = new RuleController(scope, {});
		
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
        var scope = {};
		
		var controller = new RuleController(scope, {});
		
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
        var scope = {};
		
		var controller = new RuleController(scope, {});
		
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
        var scope = {};
		
		var controller = new RuleController(scope, {});
		
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
        var scope = {};
		
		var controller = new RuleController(scope, {});
		
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
        var scope = {};
		
		var controller = new RuleController(scope, {});
		
		expect(scope.state.secVulnCountSelected).toEqual(undefined);
		expect(scope.state.licCatSelected).toEqual(undefined);
		
		scope.state.addRuleOperand = 'secVuln';
		scope.ruleOperandChanged();
		
		expect(scope.state.secVulnCountSelected).toEqual(true);
		expect(scope.state.licCatSelected).toEqual(undefined);
		
		scope.state.addRuleOperand = 'licCat';
		scope.ruleOperandChanged();
		
		expect(scope.state.secVulnCountSelected).toEqual(undefined);
		expect(scope.state.licCatSelected).toEqual(true);
	});
	
	//TODO: need to test the functions that access the grid, but first need to mock the grid object
});