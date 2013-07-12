var clmBuildTimestamp = '';

xdescribe('PolicyEditor', function() {
	var sampleData = [{"id":"c2e1bf404e6d4f5d9458069a04a5cf11","name":"Policy1","enabled":true,"threatLevel":8,"constraints":[{"id":"f52c8ce2958743d5b5e10b176bfce67b","name":"Constraint1","enabled":true,"operator":"OR","conditions":[{"conditionTypeId":"AgeInDays","operator":"older than","value":"365"}]}],"actions":{"procure":[{"actionTypeId":"fail","target":null}],"develop":[{"actionTypeId":"warn","target":null}],"build":[],"stage-release":[{"actionTypeId":"fail","target":null}],"release":[],"operate":[]}}],
		sampleActions = { build : [ { actionTypeId : 'notify', target : 'test@example.org' } ], develop : [], operate : [], procure : [{ actionTypeId : 'fail' }], release : [], 'stage-release' : []};

	function getTemplate(url) {
		url = url.split('/');
		if (url[0] === '..') {
			url.splice(0, 1);
		}
		if (url[0] === 'policy-assets') {
			url[0] = 'policy';
		} else if (url[0] === 'organization-assets') {
			url[0] = 'organization';
		} else if (url[0] === 'application-assets') {
			url[0] = 'application';
		}

		if (location.hostname) {
			url = 'src/main/resources/assets/' + url.join('/');
		} else {
			url = 'src/' + url.join('/');
		}

		var data = null;
		$.ajax({
			async: false,
			dataType: 'html',
			url: url,
			success: function(responseData) {
				data = responseData;
			}
		});
		return data;
	}

	function getController(controllerName) {
		var controller = null,
			scope = null,
			compile = null,
			sniffer = null;

		inject(function ($rootScope, $controller, $httpBackend, $compile, $sniffer) {
			scope = $rootScope.$new();
			controller = $controller(controllerName, {$scope: scope});
			compile = $compile;
			sniffer = $sniffer;
			$httpBackend.flush();
		});

		return { controller : controller, scope : scope, compile : compile, sniffer: sniffer };
	}

	function getPolicyEditorController(policyResponseData) {
		expectActionRequests();
		expectPolicyRequest(policyResponseData || sampleData);

		var modulePackage = getController('PolicyEditorController');

		modulePackage.isSaveDisabled = function () { return modulePackage.scope.policyEditor.$invalid || !(modulePackage.scope.state.currentPolicy && modulePackage.scope.state.currentPolicy.constraints.length > 0); };

		var nameInput = angular.element("<input type='text' id='policyName' name='policyName' placeholder='Enter Policy Name...' ng-model='state.currentPolicy.name' required is-Duplicate is-Duplicate-Array='policies' is-Duplicate-Id-Field='id' is-Duplicate-Case-Sensitive='false'>");
		var body = angular.element('body').append("<form id='policyEditor' name='policyEditor'></form>").find('#policyEditor').append(nameInput);

		modulePackage.setNameInput = function (val) {
			nameInput.val(val);

			var inputEvent = document.createEvent('HTMLEvents');
			inputEvent.initEvent((modulePackage.sniffer.hasEvent('input')) ? 'input' : 'change', false, false);
			nameInput[0].dispatchEvent(inputEvent);
		};

		modulePackage.compile(body)(modulePackage.scope);

		return modulePackage;
	}

	function getConstraintEditorController() {
		inject(function($httpBackend, CLMLocations, CLMAppLocations) {
			$httpBackend.expectGET(toRegExp(CLMLocations.getConditionTypeUrl())).respond(PolicyMockData.getConditionTypeData());
			$httpBackend.expectGET(toRegExp(CLMAppLocations.getConditionValueTypeUrl())).respond(PolicyMockData.getConditionValueTypeData());
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
		inject(function($httpBackend, CLMAppLocations) {
			$httpBackend.expectGET(toRegExp(CLMAppLocations.getPolicyUrl())).respond(angular.copy(responseData));
		});
	}

	function expectNewPolicy(response) {
		inject(function($httpBackend, CLMAppLocations) {
			$httpBackend.expectPOST(toRegExp(CLMAppLocations.getPolicyUrl())).respond(response);
		});
	}

	function expectUpdatePolicy(response) {
		inject(function($httpBackend, CLMAppLocations) {
			$httpBackend.expectPUT(new RegExp(CLMAppLocations.getPolicyUrl() + '\\?timestamp=[0-9]+')).respond(response);
		});
	}

	beforeEach(module('PolicyEditor', 'AngularCommon', 'CLMLocation'));
	angular.module('Hudson', []).factory('hudson', ['$http', function($http){
		return $http;
	}]);

        function toRegExp(url) {
          return new RegExp(url + '\\?timestamp=[0-9]+')
        }

	beforeEach(module('PolicyEditor', 'AngularCommon', 'CLMAppLocation'));
	beforeEach(module(function($provide) {
		$provide.value('ApplicationId', {
				encoded : function () {
					return 'bom1-12345678';
				}
			}
		);
	}));
	beforeEach(module(function($provide) {
		$provide.value('Hudson', ['$http', function($http) {
				return $http;
			}]
		);
	}));

	afterEach(function() {
		angular.element('#policyEditor').remove();
	});

	it('Test Create New Policy', inject(function ($httpBackend, $routeParams) {
		$routeParams.policyId = 'new';
		var controller = getPolicyEditorController(),
			policy = controller.scope.state.currentPolicy,
			asyncRan = false;

		// Initial State
		expect(policy).not.toBeUndefined();
		expect(policy.name).toBeUndefined();
		expect(policy.constraints).toEqual([]);
		expect(controller.isSaveDisabled()).toEqual(true);

		// Validation Test
		controller.setNameInput('Sample Policy');
		expect(policy.name).toEqual('Sample Policy');

		expect(controller.isSaveDisabled()).toEqual(true);
		policy.constraints.push({}); // Invalid constraint but PolicyEditor doesn't handle that validation
		expect(controller.isSaveDisabled()).toEqual(false);

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
		expect(controller.isSaveDisabled()).toEqual(true);

		policy.constraints.push({}); // Invalid constraint but PolicyEditor doesn't handle that validation
		controller.setNameInput('Policy1');
		expect(controller.isSaveDisabled()).toEqual(true);
	}));

	it('Test Store Not Modified', inject(function ($state) {
		// Ensures that the store is not modified prior to saving
		$state.params.policyId = 'c2e1bf404e6d4f5d9458069a04a5cf11';
		var controller = getPolicyEditorController(),
			policy = controller.scope.state.currentPolicy;

		expect(policy).not.toBeUndefined();
		expect(controller.isSaveDisabled()).toEqual(false);
		policy.name = 'An Entirely New Policy Name';

		expect(controller.scope.policies[0].name).toEqual('Policy1');
	}));

	it('Test Edit Actions', inject(function ($httpBackend, $state) {
		$state.params.policyId = 'c2e1bf404e6d4f5d9458069a04a5cf11';
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
                expect(controller.scope.constraintValidationMsg).not.toBeUndefined();

                //Pick any/all(names are mapped to values OR/AND)
                controller.scope.currentConstraint.operator = 'OR';
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

	// Test the manipulation of the DOM due to changing models. This test should be moved to a browser based tester such as Selenium at one point
	it('Updates Contraint DOM appropriately', inject(function($httpBackend, $compile, CLMAppLocations) {	
		var modulePackage = getConstraintEditorController();
		var scope = modulePackage.scope;

		$httpBackend.expectGET('../assets/components/notification-manager/notification-manager.html?').respond('<div></div>');

		var editor = angular.element(getTemplate('../assets/components/policy-editor/policy-editor.html'));
		var body = angular.element('body').append("<div id='policyEditor'></div>").find('#policyEditor').append(editor);

		var constraint = {
			name : 'ConstraintName',
			conditions : [{ name : "Age", id: 'AgeInDays', conditionTypeId : 'AgeInDays', valueTypeId : 'AgeInDaysValueType', operator : 'older than', value : 365, supportedOperators : [ 'older than' ] }],
			operator : 'OR'
		};

		$httpBackend.expectGET(toRegExp(CLMAppLocations.getConditionValueTypeUrl())).respond(PolicyMockData.getConditionValueTypeData());
		$httpBackend.expectGET('../assets/components/policy-editor/condition-editor.html').respond(getTemplate('../assets/components/policy-editor/condition-editor.html'));

		$compile(body)(scope);
		scope.$broadcast('policy.editConstraint', constraint);

		scope.$digest();
		$httpBackend.flush();

		var ageInDaysInput = angular.element('[ng-switch-when="AgeInDaysValueType"]');
		expect(ageInDaysInput.length).toEqual(1);
		expect(ageInDaysInput.attr('class')).toEqual('ng-scope');
		expect(angular.element('[ng-model="condition.v"]').val()).toEqual('1');

		var conditionTypeSelector = angular.element('[ng-model="condition.conditionTypeId"]');
		conditionTypeSelector.val('Coordinates');
		expect(conditionTypeSelector.find('option:selected').val()).toEqual('Coordinates');

		var changeEvent = document.createEvent('HTMLEvents');
		changeEvent.initEvent('change', false, false);
		conditionTypeSelector[0].dispatchEvent(changeEvent);

		var coordinatesOperator = angular.element('[ng-model="condition.operator"]').filter(function() { return $(this).css("display") !== "none"; });
		expect(coordinatesOperator.length).toEqual(1);
		var coordinateOptions = coordinatesOperator.find('option');
		expect(coordinateOptions.length).toEqual(2);
		expect(coordinateOptions[0].text).toEqual('match');
		expect(coordinateOptions[1].text).toEqual('do not match');

		conditionTypeSelector.val('SecurityVulnerabilityStatus');
		expect(conditionTypeSelector.find('option:selected').val()).toEqual('SecurityVulnerabilityStatus');

		changeEvent = document.createEvent('HTMLEvents');
		changeEvent.initEvent('change', false, false);
		conditionTypeSelector[0].dispatchEvent(changeEvent);

		coordinatesOperator = angular.element('[ng-model="condition.operator"]').filter(function() { return $(this).css("display") !== "none"; });
		expect(coordinatesOperator.length).toEqual(1);
		coordinateOptions = coordinatesOperator.find('option');
		expect(coordinateOptions.length).toEqual(2);
		expect(coordinateOptions[0].text).toEqual('is');
		expect(coordinateOptions[1].text).toEqual('is not');

		var conditionValue = angular.element('[ng-model="condition.value"]');
		expect(conditionValue.length).toEqual(1);
		var conditionOptions = conditionValue.find('option');
		expect(conditionOptions.length).toEqual(4);
		expect(conditionOptions[0].text).toEqual('Open');
		expect(conditionOptions[0].value).toEqual('0');

		angular.element('#policyEditor').remove();
	}));

	// TODO Test Response of PolicyEditorController to events from Constraint Controller
	describe('PolicyStore', function () {
		it('Default Values', inject(function (PolicyStore) {
			var newPolicy = PolicyStore.get().create();
			expect(newPolicy.threatLevel).toEqual(5);
			expect(newPolicy.constraints).toEqual([]);
		}));

		it('isActionDirty', inject(function (PolicyStore) {
			var policy = {
					actions : {
						procure: [],
						develop: [],
						build: [],
						"stage-release": [],
						release: [],
						operate: []
					}
				},
				deserializedActions = PolicyStore.deserializeActions(policy.actions);

			// Empty, unchanged
			expect(PolicyStore.isActionDirty(policy, deserializedActions)).toEqual(false);

			// Action has been removed
			policy.actions.procure.push({
				actionTypeId: "fail",
				target: null
			});
			expect(PolicyStore.isActionDirty(policy, deserializedActions)).toEqual(true);

			// One action, unchanged
			deserializedActions = PolicyStore.deserializeActions(policy.actions);
			expect(PolicyStore.isActionDirty(policy, deserializedActions)).toEqual(false);

			// Notification has been removed
			policy.actions.procure.push({
				actionTypeId: "notify",
				target: "foo@bar.com"
			});
			expect(PolicyStore.isActionDirty(policy, deserializedActions)).toEqual(true);

			// One action, one notification, unchanged
			deserializedActions = PolicyStore.deserializeActions(policy.actions);
			expect(PolicyStore.isActionDirty(policy, deserializedActions)).toEqual(false);

			// One action, one notification, action removed
			policy.actions.procure.splice(0, 1);
			expect(PolicyStore.isActionDirty(policy, deserializedActions)).toEqual(true);

			// one notification unchanged
			deserializedActions = PolicyStore.deserializeActions(policy.actions);
			expect(PolicyStore.isActionDirty(policy, deserializedActions)).toEqual(false);

			// one notification removed
			policy.actions.procure.pop();
			expect(PolicyStore.isActionDirty(policy, deserializedActions)).toEqual(true);
		}));
	});

	describe('ConstraintEditor', function () {
		it('figures dirty state of new constraint', inject(function () {
			var controller = getConstraintEditorController(), 
				e;

			// pristine constraint
			controller.scope.$broadcast('policy.editConstraint', null);
			e = controller.scope.$broadcast('pageChangeStarted', null);
			expect(e.defaultPrevented).toEqual(false);

			// changed name
			controller.scope.$broadcast('policy.editConstraint', null);
			controller.scope.currentConstraint.name = 'A Constraint Name';
			e = controller.scope.$broadcast('pageChangeStarted', null);
			expect(e.defaultPrevented).toEqual(true);

			// changed operator
			controller.scope.$broadcast('policy.editConstraint', null);
			controller.scope.currentConstraint.operator = 'ALL';
			e = controller.scope.$broadcast('pageChangeStarted', null);
			expect(e.defaultPrevented).toEqual(true);

			// changed condition value
			controller.scope.$broadcast('policy.editConstraint', null);
			controller.scope.currentConstraint.conditions[0].value = 1;
			e = controller.scope.$broadcast('pageChangeStarted', null);
			expect(e.defaultPrevented).toEqual(true);

			// new condition
			controller.scope.$broadcast('policy.editConstraint', null);
			controller.scope.currentConstraint.conditions.push({});
			e = controller.scope.$broadcast('pageChangeStarted', null);
			expect(e.defaultPrevented).toEqual(true);
		}));

		it('figures dirty state of existing constraint', inject(function () {
			var controller = getConstraintEditorController(),
				constraint = {
					name : 'Name',
					conditions : [{ conditionTypeId : 'Label', operator : 'is', value : 'red' }],
					operator : 'OR'
				}, 
				e;

			// pristine constraint
			controller.scope.$broadcast('policy.editConstraint', constraint);
			e = controller.scope.$broadcast('pageChangeStarted', null);
			expect(e.defaultPrevented).toEqual(false);

			// changed name
			controller.scope.$broadcast('policy.editConstraint', constraint);
			controller.scope.currentConstraint.name = 'A Constraint Name';
			e = controller.scope.$broadcast('pageChangeStarted', null);
			expect(e.defaultPrevented).toEqual(true);

			// changed operator
			controller.scope.$broadcast('policy.editConstraint', constraint);
			controller.scope.currentConstraint.operator = 'ALL';
			e = controller.scope.$broadcast('pageChangeStarted', null);
			expect(e.defaultPrevented).toEqual(true);

			// changed condition value
			controller.scope.$broadcast('policy.editConstraint', constraint);
			controller.scope.currentConstraint.conditions[0].value = 'black';
			e = controller.scope.$broadcast('pageChangeStarted', null);
			expect(e.defaultPrevented).toEqual(true);

			// changed condition operator
			controller.scope.$broadcast('policy.editConstraint', constraint);
			controller.scope.currentConstraint.conditions[0].operator = 'is not';
			e = controller.scope.$broadcast('pageChangeStarted', null);
			expect(e.defaultPrevented).toEqual(true);

			// changed condition type
			controller.scope.$broadcast('policy.editConstraint', constraint);
			controller.scope.currentConstraint.conditions[0].conditionTypeId = 'License';
			e = controller.scope.$broadcast('pageChangeStarted', null);
			expect(e.defaultPrevented).toEqual(true);

			// new condition
			controller.scope.$broadcast('policy.editConstraint', constraint);
			controller.scope.currentConstraint.conditions.push({});
			e = controller.scope.$broadcast('pageChangeStarted', null);
			expect(e.defaultPrevented).toEqual(true);
		}));
	});
});
