describe('InsightPolicyController tests', function() {
	var scope, $httpBackend, $location;
	
	//setup our http backend to return what we want
	beforeEach(inject(function(_$httpBackend_, $rootScope, $controller, _$location_) {
	  $location = _$location_;
	  $location.search('appId','myAppId');
	  insightApp.appId = 'myAppId';
      $httpBackend = _$httpBackend_;
      
      $httpBackend.expectGET(insightApp.getConditionTypeUrl()).
      respond([{
    	  name: 'License Category',
    	  id: 'LicenseCategory',
    	  supportedOperators: ['is','is not'],
    	  availableValues: ['Copyleft','Non-Standard','Not Provided','Weak Copyleft','Liberal'],
    	  requiresValue: true
      },{
    	  name: 'Security Vulnerability',
    	  id: 'SecurityVulnerability',
    	  supportedOperators: ['present','absent'],
    	  availableValues: null,
    	  requiresValue: false
      }]);
      
      $httpBackend.expectGET(insightApp.getActionTypeUrl()).
      respond([{
    	  name: 'Fail',
    	  id: 'fail',
    	  availableTargets: null,
    	  requiresTarget: false
      },{
    	  name: 'Warn',
    	  id: 'warn',
    	  availableTargets: null,
    	  requiresTarget: false
      },{
    	  name: 'Notify',
    	  id: 'notify',
    	  availableTargets: null,
    	  requiresTarget: true
      }]);
      
      $httpBackend.expectGET(insightApp.getActionContextUrl()).
      respond([{
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
      }]);
      
      
      
      $httpBackend.expectGET(insightApp.getPolicyUrl()).
      respond([{
    	  id: '1de8469ae4604d67a86a676c0819d109',
    	  name: 'policy1',
    	  enabled: true,
    	  threatLevel: 0,
    	  constraints: [{
    		  id: 'constraint1',
    		  name: 'constraint1',
    		  enabled: true,
    		  operator: 'OR',
    		  conditions: [{
    			  conditionTypeId: 'LicenseCategory',
    			  operator: 'is',
    			  value: 'CopyLeft'
    		  },{
    			  conditionTypeId: 'SecurityVulnerability',
    			  operator: 'absent',
    			  value: null
    		  },{
    			  conditionTypeId: 'LicenseCategory',
    			  operator: 'is not',
    			  value: 'Weak Copyleft'
    		  },{
    			  conditionTypeId: 'SecurityVulnerability',
    			  operator: 'present',
    			  value: null
    		  }]
    	  },{
    		  id: 'constraint2',
    		  name: 'constraint2',
    		  enabled: true,
    		  operator: 'AND',
    		  conditions: [{
    			  conditionTypeId: 'SecurityVulnerability',
    			  operator: 'present',
    			  value: null
    		  }]
    	  }],
    	  actions: {
    		  procure: [{
    			  actionTypeId: 'warn',
    			  target: null
    		  },{
    			  actionTypeId: 'notify',
    			  target: 'mail1'
    		  }],
    		  develop: [{
    			  actionTypeId: 'fail',
    			  target: null
    		  },{
    			  actionTypeId: 'notify',
    			  target: 'mail2'
    		  }],
    		  build: [{
    			  actionTypeId: 'warn',
    			  target: null
    		  },{
    			  actionTypeId: 'notify',
    			  target: 'mail3'
    		  }],
    		  release: [{
    			  actionTypeId: 'fail',
    			  target: null
    		  },{
    			  actionTypeId: 'notify',
    			  target: 'mail4'
    		  }],
    		  operate: [{
    			  actionTypeId: 'warn',
    			  target: null
    		  },{
    			  actionTypeId: 'notify',
    			  target: 'mail5'
    		  }]
    	  }
      },{
    	  id: '8add99ae020443708d9514804774f455',
    	  name: 'policy2',
    	  enabled: true,
    	  threatLevel: 10,
    	  constraints: [{
    		  id: 'constraint3',
    		  name: 'constraint3',
    		  enabled: true,
    		  operator: 'AND',
    		  conditions: [{
    			  conditionTypeId: 'LicenseCategory',
    			  operator: 'is not',
    			  value: 'CopyLeft'
    		  }]
    	  }],
    	  actions: {
    		  procure: [{
    			  actionTypeId: 'fail',
    			  target: null
    		  },{
    			  actionTypeId: 'notify',
    			  target: 'mail6'
    		  }],
    		  develop: [{
    			  actionTypeId: 'warn',
    			  target: null
    		  },{
    			  actionTypeId: 'notify',
    			  target: 'mail7'
    		  }],
    		  build: [{
    			  actionTypeId: 'fail',
    			  target: null
    		  },{
    			  actionTypeId: 'notify',
    			  target: 'mail8'
    		  }],
    		  release: [{
    			  actionTypeId: 'warn',
    			  target: null
    		  },{
    			  actionTypeId: 'notify',
    			  target: 'mail9'
    		  }],
    		  operate: [{
    			  actionTypeId: 'fail',
    			  target: null
    		  },{
    			  actionTypeId: 'notify',
    			  target: 'mail10'
    		  }]
    	  }
      }]);
      
      //inject the controller
      scope = $rootScope.$new();
      $controller(InsightPolicyController, {$scope: scope, global: {}});
      $httpBackend.flush();
      
	  //slickgrid mock
	  scope.constraintGrid = {
	  	  redraw: function(){},
	  	  setSelectedRows: function(){}
	  };
    }));
	
	it('initial state of the controller should be applied', function() {
		expect(scope.state.currentPolicy).not.toBeDefined();
		expect(scope.state.currentPolicyRef).not.toBeDefined();
		expect(scope.state.showAddPolicyScreen).not.toBeDefined();
		expect(scope.state.addConstraintFormValid).not.toBeDefined();
		expect(scope.state.addConstraintConditionFormValid).not.toBeDefined();
		expect(scope.state.currentConstraint.conditions.length).toBe(0);
		expect(scope.state.currentConstraint.operator).toBe('OR');
		expect(scope.state.currentCondition).toEqual({});
		expect(scope.state.actionData).toEqual({});
		expect(scope.state.actionTableData[0].id).toBe('procure');
		expect(scope.state.actionTableData[0].fail).toBe(false);
		expect(scope.state.actionTableData[0].warn).toBe(false);
		expect(scope.state.actionTableData[0].notify).not.toBeDefined();
		expect(scope.state.actionTableData[1].id).toBe('develop');
		expect(scope.state.actionTableData[1].fail).toBe(false);
		expect(scope.state.actionTableData[1].warn).toBe(false);
		expect(scope.state.actionTableData[1].notify).not.toBeDefined();
		expect(scope.state.actionTableData[2].id).toBe('build');
		expect(scope.state.actionTableData[2].fail).toBe(false);
		expect(scope.state.actionTableData[2].warn).toBe(false);
		expect(scope.state.actionTableData[2].notify).not.toBeDefined();
		expect(scope.state.actionTableData[3].id).toBe('release');
		expect(scope.state.actionTableData[3].fail).toBe(false);
		expect(scope.state.actionTableData[3].warn).toBe(false);
		expect(scope.state.actionTableData[3].notify).not.toBeDefined();
		expect(scope.state.actionTableData[4].id).toBe('operate');
		expect(scope.state.actionTableData[4].fail).toBe(false);
		expect(scope.state.actionTableData[4].warn).toBe(false);
		expect(scope.state.actionTableData[4].notify).not.toBeDefined();
		expect(scope.state.conditionTypeList[0].name).toBe('License Category');
		expect(scope.state.conditionTypeList[0].id).toBe('LicenseCategory');
		expect(scope.state.conditionTypeList[0].supportedOperators).toEqual(['is','is not']);
		expect(scope.state.conditionTypeList[0].availableValues).toEqual(['Copyleft','Non-Standard','Not Provided','Weak Copyleft','Liberal']);
		expect(scope.state.conditionTypeList[0].requiresValue).toBe(true);
		expect(scope.state.conditionTypeList[1].name).toBe('Security Vulnerability');
		expect(scope.state.conditionTypeList[1].id).toBe('SecurityVulnerability');
		expect(scope.state.conditionTypeList[1].supportedOperators).toEqual(['present','absent']);
		expect(scope.state.conditionTypeList[1].availableValues).toBe(null);
		expect(scope.state.conditionTypeList[1].requiresValue).toBe(false);
		expect(scope.state.actionTypeList[0].name).toBe('Fail');
		expect(scope.state.actionTypeList[0].id).toBe('fail');
		expect(scope.state.actionTypeList[0].availableTargets).toBe(null);
		expect(scope.state.actionTypeList[0].requiresTarget).toBe(false);
		expect(scope.state.actionTypeList[1].name).toBe('Warn');
		expect(scope.state.actionTypeList[1].id).toBe('warn');
		expect(scope.state.actionTypeList[1].availableTargets).toBe(null);
		expect(scope.state.actionTypeList[1].requiresTarget).toBe(false);
		expect(scope.state.actionTypeList[2].name).toBe('Notify');
		expect(scope.state.actionTypeList[2].id).toBe('notify');
		expect(scope.state.actionTypeList[2].availableTargets).toBe(null);
		expect(scope.state.actionTypeList[2].requiresTarget).toBe(true);
		expect(scope.state.actionContextList[0].name).toBe('Procure');
		expect(scope.state.actionContextList[0].id).toBe('procure');
		expect(scope.state.actionContextList[1].name).toBe('Develop');
		expect(scope.state.actionContextList[1].id).toBe('develop');
		expect(scope.state.actionContextList[2].name).toBe('Build');
		expect(scope.state.actionContextList[2].id).toBe('build');
		expect(scope.state.actionContextList[3].name).toBe('Release');
		expect(scope.state.actionContextList[3].id).toBe('release');
		expect(scope.state.actionContextList[4].name).toBe('Operate');
		expect(scope.state.actionContextList[4].id).toBe('operate');
		expect(scope.state.policyList[0].id).toBe('1de8469ae4604d67a86a676c0819d109');
		expect(scope.state.policyList[0].name).toBe('policy1');
		expect(scope.state.policyList[0].enabled).toBe(true);
		expect(scope.state.policyList[0].threatLevel).toBe(0);
		expect(scope.state.policyList[0].constraints[0].id).toBe('constraint1');
		expect(scope.state.policyList[0].constraints[0].name).toBe('constraint1');
		expect(scope.state.policyList[0].constraints[0].enabled).toBe(true);
		expect(scope.state.policyList[0].constraints[0].operator).toBe('OR');
		expect(scope.state.policyList[0].constraints[0].conditions[0].conditionTypeId).toBe('LicenseCategory');
		expect(scope.state.policyList[0].constraints[0].conditions[0].operator).toBe('is');
		expect(scope.state.policyList[0].constraints[0].conditions[0].value).toBe('CopyLeft');
		expect(scope.state.policyList[0].constraints[0].conditions[1].conditionTypeId).toBe('SecurityVulnerability');
		expect(scope.state.policyList[0].constraints[0].conditions[1].operator).toBe('absent');
		expect(scope.state.policyList[0].constraints[0].conditions[1].value).toBe(null);
		expect(scope.state.policyList[0].constraints[0].conditions[2].conditionTypeId).toBe('LicenseCategory');
		expect(scope.state.policyList[0].constraints[0].conditions[2].operator).toBe('is not');
		expect(scope.state.policyList[0].constraints[0].conditions[2].value).toBe('Weak Copyleft');
		expect(scope.state.policyList[0].constraints[0].conditions[3].conditionTypeId).toBe('SecurityVulnerability');
		expect(scope.state.policyList[0].constraints[0].conditions[3].operator).toBe('present');
		expect(scope.state.policyList[0].constraints[0].conditions[3].value).toBe(null);
		expect(scope.state.policyList[0].constraints[1].id).toBe('constraint2');
		expect(scope.state.policyList[0].constraints[1].name).toBe('constraint2');
		expect(scope.state.policyList[0].constraints[1].enabled).toBe(true);
		expect(scope.state.policyList[0].constraints[1].operator).toBe('AND');
		expect(scope.state.policyList[0].constraints[1].conditions[0].conditionTypeId).toBe('SecurityVulnerability');
		expect(scope.state.policyList[0].constraints[1].conditions[0].operator).toBe('present');
		expect(scope.state.policyList[0].constraints[1].conditions[0].value).toBe(null);
		expect(scope.state.policyList[0].actions.procure[0].actionTypeId).toBe('warn');
		expect(scope.state.policyList[0].actions.procure[0].target).toBe(null);
		expect(scope.state.policyList[0].actions.procure[1].actionTypeId).toBe('notify');
		expect(scope.state.policyList[0].actions.procure[1].target).toBe('mail1');
		expect(scope.state.policyList[0].actions.develop[0].actionTypeId).toBe('fail');
		expect(scope.state.policyList[0].actions.develop[0].target).toBe(null);
		expect(scope.state.policyList[0].actions.develop[1].actionTypeId).toBe('notify');
		expect(scope.state.policyList[0].actions.develop[1].target).toBe('mail2');
		expect(scope.state.policyList[0].actions.build[0].actionTypeId).toBe('warn');
		expect(scope.state.policyList[0].actions.build[0].target).toBe(null);
		expect(scope.state.policyList[0].actions.build[1].actionTypeId).toBe('notify');
		expect(scope.state.policyList[0].actions.build[1].target).toBe('mail3');
		expect(scope.state.policyList[0].actions.release[0].actionTypeId).toBe('fail');
		expect(scope.state.policyList[0].actions.release[0].target).toBe(null);
		expect(scope.state.policyList[0].actions.release[1].actionTypeId).toBe('notify');
		expect(scope.state.policyList[0].actions.release[1].target).toBe('mail4');
		expect(scope.state.policyList[0].actions.operate[0].actionTypeId).toBe('warn');
		expect(scope.state.policyList[0].actions.operate[0].target).toBe(null);
		expect(scope.state.policyList[0].actions.operate[1].actionTypeId).toBe('notify');
		expect(scope.state.policyList[0].actions.operate[1].target).toBe('mail5');
		expect(scope.state.policyList[1].id).toBe('8add99ae020443708d9514804774f455');
		expect(scope.state.policyList[1].name).toBe('policy2');
		expect(scope.state.policyList[1].enabled).toBe(true);
		expect(scope.state.policyList[1].threatLevel).toBe(10);
		expect(scope.state.policyList[1].constraints[0].id).toBe('constraint3');
		expect(scope.state.policyList[1].constraints[0].name).toBe('constraint3');
		expect(scope.state.policyList[1].constraints[0].enabled).toBe(true);
		expect(scope.state.policyList[1].constraints[0].operator).toBe('AND');
		expect(scope.state.policyList[1].constraints[0].conditions[0].conditionTypeId).toBe('LicenseCategory');
		expect(scope.state.policyList[1].constraints[0].conditions[0].operator).toBe('is not');
		expect(scope.state.policyList[1].constraints[0].conditions[0].value).toBe('CopyLeft');
		expect(scope.state.policyList[1].actions.procure[0].actionTypeId).toBe('fail');
		expect(scope.state.policyList[1].actions.procure[0].target).toBe(null);
		expect(scope.state.policyList[1].actions.procure[1].actionTypeId).toBe('notify');
		expect(scope.state.policyList[1].actions.procure[1].target).toBe('mail6');
		expect(scope.state.policyList[1].actions.develop[0].actionTypeId).toBe('warn');
		expect(scope.state.policyList[1].actions.develop[0].target).toBe(null);
		expect(scope.state.policyList[1].actions.develop[1].actionTypeId).toBe('notify');
		expect(scope.state.policyList[1].actions.develop[1].target).toBe('mail7');
		expect(scope.state.policyList[1].actions.build[0].actionTypeId).toBe('fail');
		expect(scope.state.policyList[1].actions.build[0].target).toBe(null);
		expect(scope.state.policyList[1].actions.build[1].actionTypeId).toBe('notify');
		expect(scope.state.policyList[1].actions.build[1].target).toBe('mail8');
		expect(scope.state.policyList[1].actions.release[0].actionTypeId).toBe('warn');
		expect(scope.state.policyList[1].actions.release[0].target).toBe(null);
		expect(scope.state.policyList[1].actions.release[1].actionTypeId).toBe('notify');
		expect(scope.state.policyList[1].actions.release[1].target).toBe('mail9');
		expect(scope.state.policyList[1].actions.operate[0].actionTypeId).toBe('fail');
		expect(scope.state.policyList[1].actions.operate[0].target).toBe(null);
		expect(scope.state.policyList[1].actions.operate[1].actionTypeId).toBe('notify');
		expect(scope.state.policyList[1].actions.operate[1].target).toBe('mail10');
	});
	
	it('validate state when create policy is clicked', function() {
		scope.createPolicyClick();
		
		expect(scope.state.currentPolicy.constraints).toEqual([]);
		expect(scope.state.currentPolicy.actions).toEqual({});
		expect(scope.state.showAddPolicyScreen).toBe(true);
	});
	
	it('validate state and messaging when save policy is clicked', function() {
		scope.state.currentPolicy = {
    	    name: 'policy3',
    	    enabled: true,
    	    threatLevel: 5,
    	    constraints: [{
    		    id: 'constraint4',
    		    name: 'constraint4',
    		    enabled: true,
    		    operator: 'AND',
    		    conditions: [{
    			    conditionTypeId: 'LicenseCategory',
    			    operator: 'is not',
    			    value: 'CopyLeft'
    		    }]
    	    }],
    	    actions: {
    		    procure: [{
    			    actionTypeId: 'fail',
    			    target: null
    		    },{
    			    actionTypeId: 'notify',
    			    target: 'mail11'
    		    }],
    		    develop: [{
    			    actionTypeId: 'warn',
    			    target: null
    		    },{
    			    actionTypeId: 'notify',
    			    target: 'mail12'
    		    }],
    		    build: [{
    			    actionTypeId: 'fail',
    			    target: null
    		    },{
    			    actionTypeId: 'notify',
    			    target: 'mail13'
    		    }],
    		    release: [{
    			    actionTypeId: 'warn',
    			    target: null
    		    },{
    			    actionTypeId: 'notify',
    			    target: 'mail14'
    		    }],
    		    operate: [{
    			    actionTypeId: 'fail',
    			    target: null
    		    },{
    			    actionTypeId: 'notify',
    			    target: 'mail15'
    		    }]
    	    }
        };
		
		var data = angular.copy(scope.state.currentPolicy);
		var dataWithId = angular.copy(data);
		dataWithId.id = 'anid';
		
		$httpBackend.expectPOST(insightApp.getPolicyUrl(), data).respond(dataWithId);
		
		scope.savePolicyClick();
		
		$httpBackend.flush();
		
		expect(scope.state.policyList[2].id).toBe('anid');
		expect(scope.state.policyList[2].name).toBe('policy3');
		expect(scope.state.policyList[2].enabled).toBe(true);
		expect(scope.state.policyList[2].threatLevel).toBe(5);
		expect(scope.state.policyList[2].constraints[0].id).toBe('constraint4');
		expect(scope.state.policyList[2].constraints[0].name).toBe('constraint4');
		expect(scope.state.policyList[2].constraints[0].enabled).toBe(true);
		expect(scope.state.policyList[2].constraints[0].operator).toBe('AND');
		expect(scope.state.policyList[2].constraints[0].conditions[0].conditionTypeId).toBe('LicenseCategory');
		expect(scope.state.policyList[2].constraints[0].conditions[0].operator).toBe('is not');
		expect(scope.state.policyList[2].constraints[0].conditions[0].value).toBe('CopyLeft');
		expect(scope.state.policyList[2].actions.procure[0].actionTypeId).toBe('fail');
		expect(scope.state.policyList[2].actions.procure[0].target).toBe(null);
		expect(scope.state.policyList[2].actions.procure[1].actionTypeId).toBe('notify');
		expect(scope.state.policyList[2].actions.procure[1].target).toBe('mail11');
		expect(scope.state.policyList[2].actions.develop[0].actionTypeId).toBe('warn');
		expect(scope.state.policyList[2].actions.develop[0].target).toBe(null);
		expect(scope.state.policyList[2].actions.develop[1].actionTypeId).toBe('notify');
		expect(scope.state.policyList[2].actions.develop[1].target).toBe('mail12');
		expect(scope.state.policyList[2].actions.build[0].actionTypeId).toBe('fail');
		expect(scope.state.policyList[2].actions.build[0].target).toBe(null);
		expect(scope.state.policyList[2].actions.build[1].actionTypeId).toBe('notify');
		expect(scope.state.policyList[2].actions.build[1].target).toBe('mail13');
		expect(scope.state.policyList[2].actions.release[0].actionTypeId).toBe('warn');
		expect(scope.state.policyList[2].actions.release[0].target).toBe(null);
		expect(scope.state.policyList[2].actions.release[1].actionTypeId).toBe('notify');
		expect(scope.state.policyList[2].actions.release[1].target).toBe('mail14');
		expect(scope.state.policyList[2].actions.operate[0].actionTypeId).toBe('fail');
		expect(scope.state.policyList[2].actions.operate[0].target).toBe(null);
		expect(scope.state.policyList[2].actions.operate[1].actionTypeId).toBe('notify');
		expect(scope.state.policyList[2].actions.operate[1].target).toBe('mail15');
	});
});