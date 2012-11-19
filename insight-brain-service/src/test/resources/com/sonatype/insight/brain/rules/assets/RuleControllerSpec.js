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
});