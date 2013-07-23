var clmBuildTimestamp = '';

//TODO: validate data loaded properly on edit
//TODO: validate validations on policy name, constraint name, constraint operator, and constraint conditions
//TODO: validate cancel prompts and reverts any changes on OK
//TODO: validate action editing works properly

describe('PolicyEditor.js', function() {
	var sampleData = [{"id":"c2e1bf404e6d4f5d9458069a04a5cf11","name":"Policy1","enabled":true,"threatLevel":8,"constraints":[{"id":"f52c8ce2958743d5b5e10b176bfce67b","name":"Constraint1","enabled":true,"operator":"OR","conditions":[{"conditionTypeId":"AgeInDays","operator":"older than","value":"365"}]}],"actions":{"procure":[{"actionTypeId":"fail","target":null}],"develop":[{"actionTypeId":"warn","target":null}],"build":[],"stage-release":[{"actionTypeId":"fail","target":null}],"release":[],"operate":[]}}],
		sampleActions = { build : [ { actionTypeId : 'notify', target : 'test@example.org' } ], develop : [], operate : [], procure : [{ actionTypeId : 'fail' }], release : [], 'stage-release' : []},
		testScope = null;

	function getController(controllerName) {
		var controller = null,
			scope = null,
			compile = null,
			sniffer = null;

		inject(function ($controller, $httpBackend, $compile, $sniffer) {
			scope = testScope.$new();
			controller = $controller(controllerName, {$scope: scope});
			compile = $compile;
			sniffer = $sniffer;
			$httpBackend.flush();
		});

		return { controller : controller, scope : scope, compile : compile, sniffer: sniffer };
	}

	function expectActionRequests() {
		inject(function($httpBackend, CLMLocations) {
			$httpBackend.expectGET(CLMLocations.getActionTypeUrl()).respond(PolicyMockData.getActionTypeData());
			$httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(MockData.getActionStageData());
		});
	}

	function createNewPolicy() {
		var policy = null;
		inject(function (PolicyStore, $httpBackend, CLMLocations, CLMAppLocations) {
			$httpBackend.whenGET(toRegExp(CLMLocations.getConditionTypeUrl())).respond(PolicyMockData.getConditionTypeData());
			$httpBackend.whenGET(toRegExp(CLMAppLocations.getConditionValueTypeUrl())).respond(PolicyMockData.getConditionValueTypeData());
			policy = PolicyStore.get().create();
			$httpBackend.flush();
		});
		return policy;
	}

	function toRegExp(url) {
		return new RegExp(url + '\\?timestamp=[0-9]+')
	}

	beforeEach(module('PolicyEditor', 'AngularCommon', 'CLMLocation', 'CLMAppLocation', function($provide) {
		$provide.value('Hudson', ['$http', function($http) {
				return $http;
			}]
		);
	}));

	beforeEach(inject(function ($rootScope) {
		testScope = $rootScope.$new();
	}));

	afterEach(function() {
		if (testScope) {
			testScope.$destroy();
		}
	});

	describe('inlinePolicyCreator', function () {
		function getPolicyEditorController() {
			expectActionRequests();

			return getController('PolicyEditorController');
		}

		var template = SpecUtil.getTemplate("../assets/components/policy-editor/policy-quick-add.html"),
			scope = null;

		beforeEach(inject(function ($compile, $httpBackend, PolicyStore) {
		    getPolicyEditorController();
			var node = $("<div id='testInlinePolicyCreator' inline-policy-creator='createPolicy()'></div>");
			node.appendTo('body');
			scope = testScope.$new(); // testScope's destruction cascades
			$httpBackend.whenGET("../assets/components/policy-editor/policy-quick-add.html").respond(template);
			$compile(node)(scope);
			$httpBackend.flush();
		}));

		afterEach(function () {
			$('#testInlinePolicyCreator').remove();
		});

		it('Create', function () {
			var createScope = angular.element('#testInlinePolicyCreator').scope(),
				spy = jasmine.createSpy('createPolicy');

			spy.andReturn(createNewPolicy());
			scope.createPolicy = spy;
			createScope.click();
			expect(spy).toHaveBeenCalled();

			expect(angular.element('#testInlinePolicyCreator > div').scope().policy).toBeDefined();
		});

		//TODO: check validation
		it('Saving', inject(function ($httpBackend, CLMAppLocations) {
			var createScope = angular.element('#testInlinePolicyCreator').scope();

			scope.createPolicy = function () {
			    var policy = createNewPolicy();
			    policy.name = 'testname';
			    policy.constraints[0].name = 'constraintname';
			    policy.constraints[0].operator = 'any';
			    return policy;
			};
			createScope.click();

			$httpBackend.expectPOST(toRegExp(CLMAppLocations.getPolicyUrl())).respond({
				id : 'foo',
			});
			createScope.savePolicy();
			$httpBackend.flush();

			expect(angular.element('#testInlinePolicyCreator > div').scope().policy).toEqual(null);
		}));

		it('Cancel', function () {
			var createScope = angular.element('#testInlinePolicyCreator').scope();
			scope.createPolicy = function () {
				return createNewPolicy();
			};
			createScope.click();
			createScope.cancel();

			expect(angular.element('#testInlinePolicyCreator > div').scope().policy).toEqual(null);
		});
	});

	describe('InlinePolicyEditor', function () {
		function expectPolicyRequest(responseData) {
			inject(function($httpBackend, CLMAppLocations) {
				$httpBackend.expectGET(toRegExp(CLMAppLocations.getPolicyUrl())).respond(angular.copy(responseData));
			});
		}

		function getPolicyEditorController() {
			expectActionRequests();

			return getController('PolicyEditorController');
		}
		
        var template = SpecUtil.getTemplate("../assets/components/policy-editor/policy-inline-editor.html"),
            notificationTemplate = SpecUtil.getTemplate("../assets/components/notification-manager/notification-manager.html"),
            scope = null, controller = null;

        beforeEach(inject(function ($compile, $httpBackend, PolicyStore) {
            var obj = getPolicyEditorController();
            controller = obj.controller;
            scope = obj.scope;
            scope.policy = {
                constraints: [],
                actions: {
                }
            };
            var node = $("<div id='testInlinePolicyEditor' inline-policy-editor></div>");
            node.appendTo('body');
            $httpBackend.whenGET("../assets/components/policy-editor/policy-inline-editor.html").respond(template);
            $httpBackend.whenGET("../assets/components/notification-manager/notification-manager.html?").respond(notificationTemplate);
            $compile(node)(scope);
            $httpBackend.flush();
        }));

        afterEach(function () {
            $('#testInlinePolicyEditor').remove();
        });
		
		it('Test policy validation', inject(function(){
		    //policy name uses the form validation stuff
		    var form = {
                name : {
                    $error : {
                        required : true,
                        spaces : true,
                        alphaNumeric : true
                    }   
                }
            };
		    var validateValidation = function(scope, msg) {
		        scope.validate();
	            expect(scope.alerts.length).toEqual(1);
	            expect(scope.alerts[0].msg).toEqual(msg);
	            expect(scope.alerts[0].type).toEqual('error');
		    };
		    
		    scope[scope.getFormName()] = form;
		    validateValidation(scope,'Policy name is required.');
		    
		    form.name.$error.required = false;
		    validateValidation(scope,'Policy name cannot contain leading, trailing or double spaces or tabs.');
		    
            form.name.$error.spaces = false;
            validateValidation(scope,'Policy name must be alpha numeric.');
            
            form.name.$error.alphaNumeric = false;
            validateValidation(scope,'You must add at least one constraint to the policy.');
            
            scope.policy.constraints.push({});
            validateValidation(scope,'Enter a valid name for constraint #1');
            
            scope.policy.constraints[0].name = 'name';
            validateValidation(scope,'You must select any or all of the conditions for constraint #1');
            
            scope.policy.constraints[0].operator = 'any';
            validateValidation(scope,'You must add at least one condition to constraint #1');
            
            /*
             * TODO need to wrap up this test, have to run for kids first
            scope.policy.constraints[0].conditions = [{}];
            validateValidation(scope,'Please select a valid condition type for condition #1 in constraint #1');
            scope.policy.constraints[0].conditions[0].conditionTypeId = 'AgeInDays';
            validateValidation(scope,'Please enter a value for condition #1 in constraint #1');
		    */
		}));
		
		it('Test update policy', inject(function(){
		    
		}));
		
		it('Test cancel update policy', inject(function(){
		    
		}));

		/*
		//old stuff left here for idea purposes
		xit('Test Edit Actions', inject(function ($httpBackend, $state) {
			function expectUpdatePolicy(response) {
				inject(function($httpBackend, CLMAppLocations) {
					$httpBackend.expectPUT(new RegExp(CLMAppLocations.getPolicyUrl() + '\\?timestamp=[0-9]+')).respond(response);
				});
			}

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

		xit('Store Not Modified', inject(function ($state) {
			// Ensures that the store is not modified prior to saving
			$state.params.policyId = 'c2e1bf404e6d4f5d9458069a04a5cf11';
			var controller = getPolicyEditorController(),
			policy = controller.scope.state.currentPolicy;

			expect(policy).not.toBeUndefined();
			expect(controller.isSaveDisabled()).toEqual(false);
			policy.name = 'An Entirely New Policy Name';

			expect(controller.scope.policies[0].name).toEqual('Policy1');
		}));
		*/
	});

	describe('Constraints', function () {
		function getConstraintEditorController() {
			inject(function($httpBackend, CLMLocations, CLMAppLocations) {
				$httpBackend.expectGET(toRegExp(CLMLocations.getConditionTypeUrl())).respond(PolicyMockData.getConditionTypeData());
				$httpBackend.expectGET(toRegExp(CLMAppLocations.getConditionValueTypeUrl())).respond(PolicyMockData.getConditionValueTypeData());
			});

			return getController('ConstraintEditorController');
		}

		describe('ConstraintEditor', function () {
			it('New Constraint - Dirty Checks', inject(function (PolicyStore) {
				var controller = getConstraintEditorController(),
					policy = createNewPolicy(),
					e;

				// pristine constraint
				testScope.constraint = angular.copy(policy.constraints[0]);
				testScope.$digest();
				e = testScope.$broadcast('pageChangeStarted', null);
				expect(e.defaultPrevented).toEqual(false);

				// changed name
				controller.scope.constraint.name = 'A Constraint Name';
				e = testScope.$broadcast('pageChangeStarted', null);
				expect(e.defaultPrevented).toEqual(true);

				// changed operator
				testScope.constraint = angular.copy(policy.constraints[0]);
				testScope.$digest();
				controller.scope.constraint.operator = 'ALL';
				e = testScope.$broadcast('pageChangeStarted', null);
				expect(e.defaultPrevented).toEqual(true);

				// changed condition value
				testScope.constraint = angular.copy(policy.constraints[0]);
				testScope.$digest();
				controller.scope.constraint.conditions[0].value = 1;
				e = testScope.$broadcast('pageChangeStarted', null);
				expect(e.defaultPrevented).toEqual(true);

				// new condition
				testScope.constraint = angular.copy(policy.constraints[0]);
				testScope.$digest();
				controller.scope.constraint.conditions.push({});
				e = testScope.$broadcast('pageChangeStarted', null);
				expect(e.defaultPrevented).toEqual(true);
			}));

			xit('figures dirty state of existing constraint', inject(function () {
				var controller = getConstraintEditorController(),
					constraint = {
						name : 'Name',
						conditions : [{ conditionTypeId : 'Label', operator : 'is', value : 'red' }],
						operator : 'OR'
					}, 
					e;

				// pristine constraint
				testScope.$broadcast('policy.editConstraint', constraint);
				e = testScope.$broadcast('pageChangeStarted', null);
				expect(e.defaultPrevented).toEqual(false);

				// changed name
				testScope.$broadcast('policy.editConstraint', constraint);
				controller.scope.constraint.name = 'A Constraint Name';
				e = testScope.$broadcast('pageChangeStarted', null);
				expect(e.defaultPrevented).toEqual(true);

				// changed operator
				testScope.$broadcast('policy.editConstraint', constraint);
				controller.scope.constraint.operator = 'ALL';
				e = testScope.$broadcast('pageChangeStarted', null);
				expect(e.defaultPrevented).toEqual(true);

				// changed condition value
				testScope.$broadcast('policy.editConstraint', constraint);
				controller.scope.constraint.conditions[0].value = 'black';
				e = testScope.$broadcast('pageChangeStarted', null);
				expect(e.defaultPrevented).toEqual(true);

				// changed condition operator
				testScope.$broadcast('policy.editConstraint', constraint);
				controller.scope.constraint.conditions[0].operator = 'is not';
				e = testScope.$broadcast('pageChangeStarted', null);
				expect(e.defaultPrevented).toEqual(true);

				// changed condition type
				testScope.$broadcast('policy.editConstraint', constraint);
				controller.scope.constraint.conditions[0].conditionTypeId = 'License';
				e = testScope.$broadcast('pageChangeStarted', null);
				expect(e.defaultPrevented).toEqual(true);

				// new condition
				testScope.$broadcast('policy.editConstraint', constraint);
				controller.scope.constraint.conditions.push({});
				e = testScope.$broadcast('pageChangeStarted', null);
				expect(e.defaultPrevented).toEqual(true);
			}));
		});

		describe('ConstraintEditorController', function () {

		    /*
		    //old tests left here for idea purposes
			xit('Test Create New Constraint', inject(function (PolicyStore) {
				var controller = getConstraintEditorController(),
					policy = createNewPolicy();

				// pristine constraint
				testScope.constraint = angular.copy(policy.constraints[0]);
				testScope.$digest();

				// Initial State should be in error
				expect(controller.scope.constraintValidationMsg).not.toBeUndefined();

				// A name alone should not be enough to validate
				controller.scope.constraint.name = 'A Constraint Name';
				controller.scope.validateConstraint();
				expect(controller.scope.constraintValidationMsg).not.toBeUndefined();

				// Set the condition
				controller.scope.constraint.conditions[0].value = 365;
				controller.scope.validateConstraint();
				expect(controller.scope.constraintValidationMsg).not.toBeUndefined();

				//Pick any/all(names are mapped to values OR/AND)
				controller.scope.constraint.operator = 'OR';
				controller.scope.validateConstraint();
				expect(controller.scope.constraintValidationMsg).toBeUndefined();
			}));

			xit('Test Constraint Name Validation', inject(function () {
				var controller = getConstraintEditorController();

				testScope.constraint = {
					name : '',
					conditions : [{ conditionTypeId : 'AgeInDays', operator : 'older than', value : 365 }],
					operator : 'OR'
				};
				testScope.$digest();
				expect(controller.scope.constraintValidationMsg).toEqual('Please enter a name for this constraint');
				// condition validation
			}));

			xit('Test Constraint Condition Validation', inject(function () {
				var controller = getConstraintEditorController();

				testScope.constraint = {
					name : 'ConstraintName',
					conditions : [{ conditionTypeId : '', operator : 'older than', value : 365 }],
					operator : 'OR'
				};
				testScope.$digest();
				expect(controller.scope.constraintValidationMsg).toEqual('Please select a valid condition type for condition #1');

				testScope.constraint = {
					name : 'ConstraintName',
					conditions : [{ conditionTypeId : 'AgeInDays', operator : 'older than', value : null }],
					operator : 'OR'
				};
				testScope.$digest();
				expect(controller.scope.constraintValidationMsg).toEqual('Please enter a value for condition #1');
			}));

			// Test the manipulation of the DOM due to changing models. This test should be moved to a browser based tester such as Selenium at one point
			xit('Updates Contraint DOM appropriately', inject(function($httpBackend, $compile, CLMAppLocations) {	
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
			}));*/
		});
	});

	describe('PolicyStore', function () {
		it('Default Values', inject(function (PolicyStore) {
			var newPolicy = createNewPolicy();
			expect(newPolicy.threatLevel).toEqual(5);
			expect(newPolicy.constraints).toEqual([{ conditions : [ ], operator : null }]);
		}));
	});
});
