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
    }));
	
	it('initial state of the controller should be applied', function() {
		expect(scope.state.addConstraintFormValid).not.toBeDefined();
		expect(scope.state.addConstraintConditionFormValid).not.toBeDefined();
	});
});