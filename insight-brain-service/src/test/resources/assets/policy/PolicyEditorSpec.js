var clmBuildTimestamp = '';

describe('InsightPolicyController tests', function() {
	var sampleData = [{"id":"c2e1bf404e6d4f5d9458069a04a5cf11","name":"Policy1","enabled":true,"threatLevel":8,"constraints":[{"id":"f52c8ce2958743d5b5e10b176bfce67b","name":"Constraint1","enabled":true,"operator":"OR","conditions":[{"conditionTypeId":"AgeInDays","operator":"older than","value":"365"}]}],"actions":{"procure":[{"actionTypeId":"fail","target":null}],"develop":[{"actionTypeId":"warn","target":null}],"build":[],"stage-release":[{"actionTypeId":"fail","target":null}],"release":[],"operate":[]}}],
		sampleActions = { build : [ { actionTypeId : 'notify', target : 'test@example.org' } ], develop : [], operate : [], procure : [{ actionTypeId : 'fail' }], release : [], 'stage-release' : []};

	function getController(controllerName) {
		var controller = null,
			scope = null;

		inject(function ($rootScope, $controller, $httpBackend) {
			scope = $rootScope.$new();
			controller = $controller(controllerName, {$scope: scope});
			$httpBackend.flush();
		});
		return { controller : controller, scope : scope };
	}

	function getPolicyEditorController(policyResponseData) {
		expectActionRequests();
		expectPolicyRequest(policyResponseData || sampleData);

		return getController('PolicyEditorController');
	}

	function getConstraintEditorController() {
		inject(function($httpBackend, CLMLocations) {
			$httpBackend.expectGET(CLMLocations.getConditionTypeUrl()).respond(PolicyMockData.getConditionTypeData());
			$httpBackend.expectGET(CLMLocations.getConditionValueTypeUrl()).respond(PolicyMockData.getConditionValueTypeData());
		});
		return getController('ConstraintEditorController');
	}

	function expectActionRequests() {
		inject(function($httpBackend, CLMLocations) {
			$httpBackend.expectGET(CLMLocations.getActionTypeUrl()).respond(PolicyMockData.getActionTypeData());
			$httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(MockData.getActionStageData());
		});
	}

	function expectPolicyRequest(responseData) {
		inject(function($httpBackend, CLMLocations) {
			$httpBackend.expectGET(new RegExp(CLMLocations.getPolicyUrl() + '\\?timestamp=[0-9]+')).respond(angular.copy(responseData));
		});
	}

	function expectNewPolicy(response) {
		inject(function($httpBackend, CLMLocations) {
			$httpBackend.expectPOST(new RegExp(CLMLocations.getPolicyUrl() + '\\?timestamp=[0-9]+')).respond(response);
		});
	}

	function expectUpdatePolicy(response) {
		inject(function($httpBackend, CLMLocations) {
			$httpBackend.expectPUT(new RegExp(CLMLocations.getPolicyUrl() + '\\?timestamp=[0-9]+')).respond(response);
		});
	}

	angular.module('Hudson', []).factory('hudson', [ '$http', function($http) {
		return $http;
	} ]);

	beforeEach(module('PolicyEditor'));

	it('Test Create New Policy', inject(function ($httpBackend, $routeParams) {
		$routeParams.policyId = 'new';
		var controller = getPolicyEditorController(),
			policy = controller.scope.state.currentPolicy,
			asyncRan = false;

		// Initial State
		expect(policy).not.toBeUndefined();
		expect(policy.name).toBeUndefined();
		expect(policy.constraints).toEqual([]);
		expect(controller.scope.isPolicyValid()).toEqual(false);

		// Validation Test
		policy.name = 'Sample Policy';
		expect(controller.scope.isPolicyValid()).toEqual(false);
		policy.constraints.push({}); // Invalid constraint but PolicyEditor doesn't handle that validation
		expect(controller.scope.isPolicyValid()).toEqual(true);

		// Add an action
		controller.scope.state.actions.procure.action = 'fail';
		controller.scope.state.actions.build.notify.push('test@example.org');

		expectNewPolicy(function (method, url, objJson) {
			var obj = angular.fromJson(objJson);
			expect(obj.name).toEqual('Sample Policy');
			expect(obj.actions).toEqual(sampleActions);
			expect(obj.id).toBeUndefined();
			obj.id = 'generated-id';
			asyncRan = true;
			return [200, obj, {}];
		});
		controller.scope.savePolicy();
		$httpBackend.flush();
		expect(asyncRan).toEqual(true);

		expect(controller.scope.policies.length).toEqual(2);
		expect(controller.scope.policies[1].id).toEqual('generated-id');
	}));

	it('Test Duplicate Policy Names', inject(function ($routeParams) {
		$routeParams.policyId = 'new';
		var controller = getPolicyEditorController(),
			policy = controller.scope.state.currentPolicy;

		expect(policy).not.toBeUndefined();
		expect(controller.scope.isPolicyValid()).toEqual(false);

		policy.constraints.push({}); // Invalid constraint but PolicyEditor doesn't handle that validation
		policy.name = 'Policy1';
		expect(controller.scope.isPolicyValid()).toEqual(false);
	}));

	it('Test Store Not Modified', inject(function ($routeParams) {
		// Ensures that the store is not modified prior to saving
		$routeParams.policyId = 'c2e1bf404e6d4f5d9458069a04a5cf11';
		var controller = getPolicyEditorController(),
			policy = controller.scope.state.currentPolicy;

		expect(policy).not.toBeUndefined();
		expect(controller.scope.isPolicyValid()).toEqual(true);
		policy.name = 'An Entirely New Policy Name';

		expect(controller.scope.policies[0].name).toEqual('Policy1');
	}));

	it('Test Edit Actions', inject(function ($httpBackend, $routeParams) {
		$routeParams.policyId = 'c2e1bf404e6d4f5d9458069a04a5cf11';
		var controller = getPolicyEditorController(),
			asyncRan = false,
			policy = controller.scope.state.currentPolicy;

		// Add an action
		controller.scope.state.actions.procure.action = 'fail';
		controller.scope.state.actions.develop.action = null;
		controller.scope.state.actions['stage-release'].action = null;
		controller.scope.state.actions.build.notify.push('test@example.org');

		expectUpdatePolicy(function (method, url, objJson) {
			var obj = angular.fromJson(objJson);
			expect(obj.actions).toEqual(sampleActions);
			asyncRan = true;
			return [200, obj, {}];
		});
		controller.scope.savePolicy();
		$httpBackend.flush();

		expect(asyncRan).toEqual(true);
	}));

	it('Test Create New Constraint', inject(function ($rootScope) {
		var controller = getConstraintEditorController(),
			asyncRan = false;
		controller.scope.$broadcast('policy.editConstraint', null);

		// Initial State should be in error
		expect(controller.scope.constraintValidationMsg).not.toBeUndefined();

		// A name alone should not be enough to validate
		controller.scope.currentConstraint.name = 'A Constraint Name';
		controller.scope.validateConstraint();
		expect(controller.scope.constraintValidationMsg).not.toBeUndefined();

		// Set the condition
		controller.scope.currentConstraint.conditions[0].valueModifier = 365;
		controller.scope.currentConstraint.conditions[0].v = 1;
		controller.scope.updateAge(controller.scope.currentConstraint.conditions[0]);
		controller.scope.validateConstraint();
		expect(controller.scope.constraintValidationMsg).toBeUndefined();

		$rootScope.$on('policy.constraintSaved', function (event, constraint) {
			expect(constraint.id).toBeUndefined();
			expect(constraint.name).toEqual('A Constraint Name');
			expect(constraint.conditions).toEqual([{ conditionTypeId : 'AgeInDays', operator : 'older than', value : 365 }]);
			asyncRan = true;
		});

		controller.scope.saveConstraint();

		expect(asyncRan).toEqual(true);
	}));

	it('Test Constraint Name Validation', inject(function () {
		var controller = getConstraintEditorController();

		controller.scope.$broadcast('policy.editConstraint', {
			name : '',
			conditions : [{ conditionTypeId : 'AgeInDays', operator : 'older than', value : 365 }],
			operator : 'OR'
		});
		expect(controller.scope.constraintValidationMsg).toEqual('Please enter a name for this constraint');
		// condition validation
	}));

	it('Test Constraint Condition Validation', inject(function () {
		var controller = getConstraintEditorController();

		controller.scope.$broadcast('policy.editConstraint', {
			name : 'ConstraintName',
			conditions : [{ conditionTypeId : '', operator : 'older than', value : 365 }],
			operator : 'OR'
		});
		expect(controller.scope.constraintValidationMsg).toEqual('Please select a valid condition type for condition #1');
		controller.scope.cancelConstraint();

		controller.scope.$broadcast('policy.editConstraint', {
			name : 'ConstraintName',
			conditions : [{ conditionTypeId : 'AgeInDays', operator : 'older than', value : null }],
			operator : 'OR'
		});
		expect(controller.scope.constraintValidationMsg).toEqual('Please enter a value for condition #1');
		controller.scope.cancelConstraint();
	}));

	// TODO Test Response of PolicyEditorController to events from Constraint Controller
});