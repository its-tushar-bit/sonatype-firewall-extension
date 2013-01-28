var clmBuildTimestamp = '';

describe('InsightPolicyController tests', function() {
	var scope;

	angular.module('Hudson', []).factory('hudson', ['$http', function($http){
		return $http;
	}]);


	beforeEach(module('Policy', 'Hudson', 'CLMLocation'));
	// setup our http backend to return what we want
	beforeEach(inject(function($httpBackend, $rootScope, $controller, CLMLocations) {
	  CLMLocations.appId = 'myAppId';

      function toRegExp( getUrl ) {
          return new RegExp( getUrl + '\\?timestamp=[0-9]+' );
      }

      $httpBackend.expectGET(toRegExp(CLMLocations.getConditionTypeUrl())).respond(JSONData.getConditionTypeData());
      $httpBackend.expectGET(toRegExp(CLMLocations.getActionTypeUrl())).respond(JSONData.getActionTypeData());
      $httpBackend.expectGET(toRegExp(CLMLocations.getActionStageUrl())).respond(JSONData.getActionStageData());
      $httpBackend.expectGET(toRegExp(CLMLocations.getConditionValueTypeUrl())).respond(JSONData.getConditionValueTypeData());
      $httpBackend.expectGET(toRegExp(CLMLocations.getPolicyUrl())).respond(JSONData.getPolicyData());
      
      // inject the controller
      scope = $rootScope.$new();
	  
      $controller('InsightPolicyController', {$scope: scope, global: {}});
      $httpBackend.flush();
    }));
	
	it('Test initial data state', function() {
	    expect(scope.state.conditionTypeList.length).toEqual(11);
	    expect(scope.state.actionTypeList.length).toEqual(3);
	    expect(scope.state.actionStageList.length).toEqual(5);
	    expect(scope.state.conditionValueTypeList.length).toEqual(9);
	    expect(scope.state.policyList.length).toEqual(5);
	});
	
	it('Test apply UI data to json', function() {
	    expect(scope.state.policyList[0].summary.actionCount).toEqual(1);
	    expect(scope.state.policyList[0].summary.actions).toEqual('Build: Fail');
	    expect(scope.state.policyList[1].summary.actionCount).toEqual(1);
        expect(scope.state.policyList[1].summary.actions).toEqual('Build: Fail');
        expect(scope.state.policyList[2].summary.actionCount).toEqual(0);
        expect(scope.state.policyList[2].summary.actions).toEqual('');
        expect(scope.state.policyList[3].summary.actionCount).toEqual(0);
        expect(scope.state.policyList[3].summary.actions).toEqual('');
        expect(scope.state.policyList[4].summary.actionCount).toEqual(0);
        expect(scope.state.policyList[4].summary.actions).toEqual('');
        
        expect(scope.state.policyList[0].constraints[0].conditions[0].conditionType.id).toEqual('License');
        expect(scope.state.policyList[0].constraints[0].conditions[0].valueType.id).toEqual('LicenseValueType');
        expect(scope.state.policyList[0].constraints[0].conditions[0].value).toEqual('AAL');
        expect(scope.state.policyList[0].constraints[0].conditions[0].valueModifier).toBeUndefined();
        expect(scope.state.policyList[0].constraints[0].conditions[1].conditionType.id).toEqual('AgeInDays');
        expect(scope.state.policyList[0].constraints[0].conditions[1].valueType.id).toEqual('AgeInDaysValueType');
        expect(scope.state.policyList[0].constraints[0].conditions[1].value).toEqual(12);
        expect(scope.state.policyList[0].constraints[0].conditions[1].valueModifier).toEqual('m');
        expect(scope.state.policyList[0].constraints[0].conditions[2].conditionType.id).toEqual('SecurityVulnerability');
        expect(scope.state.policyList[0].constraints[0].conditions[2].valueType).toBeNull();
        expect(scope.state.policyList[0].constraints[0].conditions[2].value).toBeNull();
        expect(scope.state.policyList[0].constraints[0].conditions[2].valueModifier).toBeUndefined();
        expect(scope.state.policyList[0].constraints[0].conditions[3].conditionType.id).toEqual('SecurityVulnerabilitySeverity');
        expect(scope.state.policyList[0].constraints[0].conditions[3].valueType.id).toEqual('FloatValueType');
        expect(scope.state.policyList[0].constraints[0].conditions[3].value).toEqual('44');
        expect(scope.state.policyList[0].constraints[0].conditions[3].valueModifier).toBeUndefined();
        expect(scope.state.policyList[0].constraints[1].conditions[0].conditionType.id).toEqual('License');
        expect(scope.state.policyList[0].constraints[1].conditions[0].valueType.id).toEqual('LicenseValueType');
        expect(scope.state.policyList[0].constraints[1].conditions[0].value).toEqual('AAL');
        expect(scope.state.policyList[0].constraints[1].conditions[0].valueModifier).toBeUndefined();
        expect(scope.state.policyList[0].constraints[2].conditions[0].conditionType.id).toEqual('License');
        expect(scope.state.policyList[0].constraints[2].conditions[0].valueType.id).toEqual('LicenseValueType');
        expect(scope.state.policyList[0].constraints[2].conditions[0].value).toEqual('AAL');
        expect(scope.state.policyList[0].constraints[2].conditions[0].valueModifier).toBeUndefined();
        expect(scope.state.policyList[0].constraints[3].conditions[0].conditionType.id).toEqual('LicenseStatus');
        expect(scope.state.policyList[0].constraints[3].conditions[0].valueType.id).toEqual('LicenseStatusValueType');
        expect(scope.state.policyList[0].constraints[3].conditions[0].value).toEqual('OPEN');
        expect(scope.state.policyList[0].constraints[3].conditions[0].valueModifier).toBeUndefined();
        expect(scope.state.policyList[1].constraints[0].conditions[0].conditionType.id).toEqual('AgeInDays');
        expect(scope.state.policyList[1].constraints[0].conditions[0].valueType.id).toEqual('AgeInDaysValueType');
        expect(scope.state.policyList[1].constraints[0].conditions[0].value).toEqual(55);
        expect(scope.state.policyList[1].constraints[0].conditions[0].valueModifier).toEqual('y');
        expect(scope.state.policyList[2].constraints[0].conditions[0].conditionType.id).toEqual('SecurityVulnerability');
        expect(scope.state.policyList[2].constraints[0].conditions[0].valueType).toBeNull();
        expect(scope.state.policyList[2].constraints[0].conditions[0].value).toBeNull();
        expect(scope.state.policyList[2].constraints[0].conditions[0].valueModifier).toBeUndefined();
        expect(scope.state.policyList[3].constraints[0].conditions[0].conditionType.id).toEqual('SecurityVulnerability');
        expect(scope.state.policyList[3].constraints[0].conditions[0].valueType).toBeNull();
        expect(scope.state.policyList[3].constraints[0].conditions[0].value).toBeNull();
        expect(scope.state.policyList[3].constraints[0].conditions[0].valueModifier).toBeUndefined();
        expect(scope.state.policyList[4].constraints[0].conditions[0].conditionType.id).toEqual('SecurityVulnerability');
        expect(scope.state.policyList[4].constraints[0].conditions[0].valueType).toBeNull();
        expect(scope.state.policyList[4].constraints[0].conditions[0].value).toBeNull();
        expect(scope.state.policyList[4].constraints[0].conditions[0].valueModifier).toBeUndefined();
	});
	
	it('Test initial scope variables', function() {
	    expect(scope.state.policyChanged).toBeUndefined();
	    expect(scope.state.policyWatchStopFn).toBeUndefined();
	    expect(scope.state.currentPolicy).toBeUndefined();
	    expect(scope.state.showAddPolicyScreen).toBeUndefined();
	    expect(scope.state.showAddPolicyScreen).toBeUndefined();
	    expect(scope.state.actionEditMode).toBeUndefined();
	    expect(scope.state.currentConstraint).toEqual({conditions : [ { conditionTypeId : 'Label', conditionType : { name : 'Label', id : 'Label', supportedOperators : [ 'is', 'is not' ], valueTypeId : 'LabelValueType', valueHint : null }, operator : 'is', valueType : { id : 'LabelValueType', dataType : 'Label', allowMultiple : false, availableValues : [  ] }, valueModifier : 'y' } ], operator : 'OR'});
	    expect(scope.state.actionTableData).toEqual([]); 
	});
	
	it('Test create policy', inject(function(CLMLocations, $timeout, $httpBackend) {
		scope.viewCreatePolicy({ preventDefault : angular.noop });
		
		$timeout.flush();
		
		expect(scope.state.currentPolicy).toEqual({constraints: [], actions: {}, threatLevel: 5});
		expect(scope.state.showAddPolicyScreen).toBe(true);
		expect(scope.state.addPolicyTitle).toEqual('Create a New Policy');
		expect(scope.state.policyWatchStopFn).not.toBeUndefined();
		expect(scope.state.actionTableData).toEqual([ { id : 'procure', name : 'Procure', available : false, action : 'none' }, { id : 'develop', name : 'Develop', available : false, action : 'none' }, { id : 'build', name : 'Build', available : true, action : 'none' }, { id : 'release', name : 'Release', available : false, action : 'none' }, { id : 'operate', name : 'Operate', available : false, action : 'none' } ]); 
		
		scope.validatePolicy();
		expect(scope.state.policyValid).toBeUndefined();
		
		scope.state.currentPolicy.name = 'createPolicyTest';
		
        scope.viewAddConstraint();
        
        scope.validateConstraint();
        expect(scope.state.constraintValidationMsg).toBe('Please enter a name for this constraint');
        
        scope.state.currentConstraint.name = 'createPolicyTest_constraint';
        scope.state.currentConstraint.operator = 'AND';
        scope.state.currentConstraint.conditions[0].conditionTypeId = 'SecurityVulnerability';
        
        scope.$index = 0;
        scope.conditionTypeChanged();
        
        scope.state.currentConstraint.conditions[0].operator = 'present';
        delete scope.state.currentConstraint.conditions[0].value;
        
        scope.validateConstraint();
        expect(scope.state.constraintValidationMsg).toBeUndefined();
        
        scope.addConstraint();
        
        scope.validatePolicy();
        expect(scope.state.policyValid).toBe(true);
        
        scope.state.actionTableData[2].action = 'fail';
        
        $httpBackend.expectPOST(CLMLocations.getPolicyUrl(),JSONData.getCreateTestPolicy()).respond(JSONData.getCreateTestPolicy('newid'));
        
        scope.savePolicy();
        
        $httpBackend.flush();
        
        expect(scope.state.policyList[5].name).toEqual('createPolicyTest');
	}));
	
	it('Test edit policy', inject(function(CLMLocations, $timeout, $httpBackend) {
	    scope.$index = 4;
	    scope.viewEditPolicy();
	    $timeout.flush();
        
        expect(scope.state.currentPolicy).toEqual({ id : '03bf6717cbbf49b8a177c3004668875a', name : '4444', enabled : true, threatLevel : 5, constraints : [ { id : 'd68c0fda6269459ab81524079a4bc6a8', name : 'sd', enabled : true, operator : 'OR', conditions : [ { conditionTypeId : 'SecurityVulnerability', operator : 'present', value : null, conditionType : { name : 'Security Vulnerability', id : 'SecurityVulnerability', supportedOperators : [ 'present', 'absent' ], valueTypeId : null, valueHint : null }, valueType : null } ] } ], actions : {  }, summary : { constraints : '1 Constraint(s) to be evaluated', actionCount : 0, actions : '' } });
        expect(scope.state.showAddPolicyScreen).toBe(true);
        expect(scope.state.policyValid).toBe(true);
        expect(scope.state.addPolicyTitle).toEqual('Edit Policy');
        expect(scope.state.policyWatchStopFn).not.toBeUndefined();
        expect(scope.state.actionTableData).toEqual([ { id : 'procure', name : 'Procure', available : false, action : 'none' }, { id : 'develop', name : 'Develop', available : false, action : 'none' }, { id : 'build', name : 'Build', available : true, action : 'none' }, { id : 'release', name : 'Release', available : false, action : 'none' }, { id : 'operate', name : 'Operate', available : false, action : 'none' } ]); 
        
        scope.state.currentPolicy.name = '5555';
        
        $httpBackend.expectPUT(CLMLocations.getPolicyUrl(),JSONData.getEditTestPolicy()).respond(JSONData.getEditTestPolicy());
        
        scope.savePolicy();
        
        $httpBackend.flush();
        
        expect(scope.state.policyList[4].name).toEqual('5555');
	}));
	
	it('Test remove policy', inject(function(CLMLocations, $httpBackend) {
	    expect(scope.state.deletePolicyIndex).toBeUndefined();
	    
	    scope.$index = 0;
	    scope.viewRemovePolicy();
	    
	    expect(scope.state.deletePolicyIndex).toEqual(0);	    
	    expect(scope.state.policyList[0].id).toEqual('053e89a476b34d7dac5d97665d2d241e');
	    expect(scope.state.confirmationHeader).toEqual('Delete Policy?');
	    expect(scope.state.confirmationBody).toEqual('Are you sure you want to delete the Policy named \'asdffffrfff\'?  This action is not reversible.');
	    expect(scope.state.confirmationDeclineText).toEqual('Cancel');
	    expect(scope.state.confirmationAcceptText).toEqual('Delete');
	    expect(scope.state.confirmationAcceptFn).not.toBeNull();
	    expect(scope.state.confirmationDeclineFn).not.toBeNull();
	    
	    scope.confirmationDecline();

	    expect(scope.state.deletePolicyIndex).toBeUndefined();
	    expect(scope.state.policyList[0].id).toEqual('053e89a476b34d7dac5d97665d2d241e');
        
	    scope.viewRemovePolicy();
	    
	    expect(scope.state.deletePolicyIndex).toEqual(0);      
        expect(scope.state.policyList[0].id).toEqual('053e89a476b34d7dac5d97665d2d241e');
        expect(scope.state.confirmationHeader).toEqual('Delete Policy?');
        expect(scope.state.confirmationBody).toEqual('Are you sure you want to delete the Policy named \'asdffffrfff\'?  This action is not reversible.');
        expect(scope.state.confirmationDeclineText).toEqual('Cancel');
        expect(scope.state.confirmationAcceptText).toEqual('Delete');
        expect(scope.state.confirmationAcceptFn).not.toBeNull();
        expect(scope.state.confirmationDeclineFn).not.toBeNull();
        
        $httpBackend.expectDELETE(CLMLocations.getPolicyUrl() + '/' + scope.state.policyList[0].id).respond(200);
        
        scope.confirmationAccept();
        
        $httpBackend.flush();
        
        expect(scope.state.policyList[0].id).toEqual('ec21b3ee9f31447c9e40913d91776593');
	}));
	
	it('validate state and messaging when save policy is clicked', inject(function(CLMLocations, $httpBackend) {
		scope.state.currentPolicy = JSONData.getNewPolicy();
		var data = angular.copy(scope.state.currentPolicy);
		var dataWithId = angular.copy(data);
		dataWithId.id = 'anid';
		
		$httpBackend.expectPOST(CLMLocations.getPolicyUrl(), data).respond(dataWithId);
		
		scope.savePolicy();
		
		$httpBackend.flush();
		
		expect(scope.state.policyList[5].id).toBe('anid');
		expect(scope.state.policyList[5].name).toBe('policy3');
	}));
});