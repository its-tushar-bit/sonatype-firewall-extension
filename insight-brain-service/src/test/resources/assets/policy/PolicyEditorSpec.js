describe('PolicyEditor.js', function() {
	var testScope = null;

	function getController(controllerName) {
		var controller = null,
			scope = null;

		inject(function ($controller, $httpBackend, $compile, $sniffer) {
			scope = testScope.$new();
			controller = $controller(controllerName, {$scope: scope});
			$httpBackend.flush();
		});

		return { controller : controller, scope : scope };
	}

	function expectActionRequests() {
		inject(function($httpBackend, CLMLocations) {
			$httpBackend.whenGET(CLMLocations.getActionTypeUrl()).respond(PolicyMockData.getActionTypeData());
			$httpBackend.whenGET(CLMLocations.getActionStageUrl()).respond(MockData.getActionStageData());
		});
	}

	function createNewPolicy() {
		var policy = null;
		inject(function (PolicyStore, $httpBackend, CLMLocations, CLMAppLocations) {
			$httpBackend.whenGET(SpecUtil.toRegExp(CLMLocations.getConditionTypeUrl())).respond(PolicyMockData.getConditionTypeData());
			$httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getConditionValueTypeUrl())).respond(PolicyMockData.getConditionValueTypeData());
			policy = PolicyStore.get().create();
		});
		return policy;
	}

	beforeEach(module('PolicyEditor', 'AngularCommon', 'CLMLocation', 'CLMAppLocation', function($provide) {
	  $provide.factory('hudson', ['$http', function($http){
      return $http;
    }]);
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
		
		function getConstraintEditorController() {
		    return getController('ConstraintEditorController');
		}

		var conditionTemplate = SpecUtil.getTemplate("../assets/components/policy-editor/condition-editor.html"),
			template = SpecUtil.getTemplate("../assets/components/policy-editor/policy-inline-editor.html"),
			constraintEditorTemplate = SpecUtil.getTemplate("../assets/components/policy-editor/constraint-editor.html"),
			scope = null;

		beforeEach(inject(function ($compile, $httpBackend, PolicyStore) {
			getPolicyEditorController();
			createNewPolicy();
			var node = $("<div id='testInlinePolicyCreator' inline-policy-creator='createPolicy()'></div>");
			node.appendTo('body');
			scope = testScope.$new(); // testScope's destruction cascades
			$httpBackend.whenGET("policy-quick-add").respond('<div show-if="policy">' + template + '</div>');
			$httpBackend.whenGET("../assets/components/policy-editor/condition-editor.html?").respond(conditionTemplate);
			$compile(node)(scope);
			$httpBackend.flush();
		}));

		afterEach(function () {
			$('#testInlinePolicyCreator').remove();
		});

		it('Create', function () {
			var createScope = angular.element('#testInlinePolicyCreator').scope();

			createScope.click();
			expect(createScope.policy).not.toBeUndefined();

			expect(angular.element('#testInlinePolicyCreator').scope().policy).toBeDefined();
		});

		//TODO: check validation
		it('Saving', inject(function ($httpBackend, CLMAppLocations) {
			var createScope = angular.element('#testInlinePolicyCreator').scope();

			scope.createPolicy = function () {
			    var policy = createNewPolicy();
			    policy.name = 'testname';
			    policy.constraints[0].name = 'constraintname';
			    policy.constraints[0].operator = 'OR';
			    return policy;
			};
			createScope.click();

			$httpBackend.expectPOST(SpecUtil.toRegExp(CLMAppLocations.getPolicyUrl())).respond({
				id : 'foo',
			});
			createScope.savePolicy();
			$httpBackend.flush();

			expect(angular.element('#testInlinePolicyCreator').scope().policy).toEqual(null);
		}));

		it('Cancel', function () {
			var createScope = angular.element('#testInlinePolicyCreator').scope();
			scope.createPolicy = function () {
				return createNewPolicy();
			};
			createScope.click();
			createScope.cancel();
		});
		
		it('Operator hidden when one condition', inject(function ($httpBackend) {
		    var createScope = angular.element('#testInlinePolicyCreator').scope();
            scope.createPolicy = function () {
                return createNewPolicy();
            };

            $httpBackend.whenGET('../assets/components/policy-editor/constraint-editor.html?').respond(constraintEditorTemplate);
            createScope.$apply(function () {
                createScope.click();
            });
            $httpBackend.flush();
            expect(createScope.policy).toBeDefined();

            //by default the operator field should be hidden, as there is only 1 condition initially
            var operator = $('#testInlinePolicyCreator').find('select[ng-model="constraint.operator"]')[0];
            expect($(operator).is(":visible")).toEqual(false);
            
            constraintScope = getConstraintEditorController().scope;
            constraintScope.constraint = createScope.policy.constraints[0];
            
            constraintScope.addCondition();
            
            createScope.$digest();
            
            //now we should be add 2 conditions, so the field should show
            operator = $('#testInlinePolicyCreator').find('select[ng-model="constraint.operator"]')[0];
            expect($(operator).is(":visible")).toEqual(true);
            
            constraintScope.removeCondition(1);
            
            createScope.$digest();
            
            operator = $('#testInlinePolicyCreator').find('select[ng-model="constraint.operator"]')[0];
            expect($(operator).is(":visible")).toEqual(false);
		}));

		describe('isDirty', function () {
			it('Unchanged', function () {
				var createScope = angular.element('#testInlinePolicyCreator').scope();
				scope.createPolicy = function () {
					return createNewPolicy();
				};
				createScope.click();
				expect(testScope.$broadcast('pageChangeStarted').defaultPrevented).toEqual(false);
			});
			it('Policy Name', function () {
				var createScope = angular.element('#testInlinePolicyCreator').scope();
				scope.createPolicy = function () {
					return createNewPolicy();
				};
				createScope.click();
				createScope.policy.name = 'foo';
				expect(testScope.$broadcast('pageChangeStarted').defaultPrevented).toEqual(true);
			});
			it('Constraint Name', function () {
				var createScope = angular.element('#testInlinePolicyCreator').scope();
				scope.createPolicy = function () {
					return createNewPolicy();
				};
				createScope.click();
				createScope.policy.constraints[0].name = 'foo'
				expect(testScope.$broadcast('pageChangeStarted').defaultPrevented).toEqual(true);
			});
		});
	});

	describe('InlinePolicyEditor', function () {
		function expectPolicyRequest(responseData) {
			inject(function($httpBackend, CLMAppLocations) {
				$httpBackend.expectGET(SpecUtil.toRegExp(CLMAppLocations.getPolicyUrl())).respond(angular.copy(responseData));
			});
		}

		function getPolicyEditorController() {
			expectActionRequests();

			return getController('PolicyEditorController');
		}
		function getPolicyController() {
			inject(function ($httpBackend, CLMAppLocations) {
				$httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getPolicyUrl())).respond(PolicyMockData.getPolicyData());
				$httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getApplicablePolicies())).respond(ApplicationMockData.getApplicablePolicies());
			});

			return getController('PolicyController');
		}
		
        var template = SpecUtil.getTemplate("../assets/components/policy-editor/policy-inline-editor.html"),
            constraintEditorTemplate = SpecUtil.getTemplate("../assets/components/policy-editor/constraint-editor.html"),
            conditionEditorTemplate = SpecUtil.getTemplate("../assets/components/policy-editor/condition-editor.html"),
            parentScope = null,
            policyScope = null,
            scope = null;

        beforeEach(inject(function ($compile, $httpBackend, CLMLocations, CLMAppLocations) {
            var node = $("<div><div show-if='policyEditMap[policy.id]'><div id='testInlinePolicyEditor' inline-policy-editor '></div></div></div>");
            node.appendTo('body');
            expectActionRequests()
            $httpBackend.whenGET(SpecUtil.toRegExp(CLMLocations.getConditionTypeUrl())).respond(PolicyMockData.getConditionTypeData());
            $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getConditionValueTypeUrl())).respond(PolicyMockData.getConditionValueTypeData());
            $httpBackend.whenGET("../assets/components/policy-editor/policy-inline-editor.html?").respond(template);
            $httpBackend.whenGET("../assets/components/policy-editor/constraint-editor.html?").respond(constraintEditorTemplate);
            $httpBackend.whenGET("../assets/components/policy-editor/condition-editor.html?").respond(conditionEditorTemplate);

            parentScope = testScope.$new();
            parentScope.policyEditMap = {};
            policyScope = parentScope.$new();
            policyScope.policy = createNewPolicy();

            $compile(node)(policyScope.$new());
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

		    parentScope.policyEditMap[policyScope.policy.id] = true;
			parentScope.$digest();
			scope = angular.element('#testInlinePolicyEditor').scope();

            scope.policy.constraints = [];
		    
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
            validateValidation(scope,'You must select any or all of the conditions for constraint "name"');
            
            scope.policy.constraints[0].operator = 'OR';
            validateValidation(scope,'You must add at least one condition to constraint "name"');
            
            scope.policy.constraints[0].conditions = [{}];
            validateValidation(scope,'Please select a valid condition type for condition #1 in constraint "name"');
            
            scope.policy.constraints[0].conditions[0].conditionTypeId = 'AgeInDays';
            validateValidation(scope,'Please enter a whole number for condition #1 in constraint "name"');

            scope.policy.constraints[0].conditions[0].conditionTypeId = 'SecurityVulnerabilitySeverity';
            validateValidation(scope,'Please enter a decimal number for condition #1 in constraint "name"');

            scope.policy.constraints[0].conditions[0].conditionTypeId = 'SecurityVulnerabilityStatus';
            validateValidation(scope,'Please enter a value for condition #1 in constraint "name"');

            scope.policy.constraints[0].conditions[0].value = '300';
            scope.policy.constraints.push({});
            validateValidation(scope,'Enter a valid name for constraint #2');
            
            scope.policy.constraints[1].name = 'name';
            validateValidation(scope,'You must select any or all of the conditions for constraint "name"');
            
            scope.policy.constraints[1].operator = 'OR';
            validateValidation(scope,'You must add at least one condition to constraint "name"');
            
            scope.policy.constraints[1].conditions = [{}];
            validateValidation(scope,'Please select a valid condition type for condition #1 in constraint "name"');
            
            scope.policy.constraints[1].conditions[0].conditionTypeId = 'AgeInDays';
            validateValidation(scope,'Please enter a whole number for condition #1 in constraint "name"');
            
            scope.policy.constraints[1].conditions[0].value = '300';
            scope.policy.constraints[1].conditions.push({});
            validateValidation(scope,'Please select a valid condition type for condition #2 in constraint "name"');
            
            scope.policy.constraints[1].conditions[1].conditionTypeId = 'AgeInDays';
            validateValidation(scope,'Please enter a whole number for condition #2 in constraint "name"');
            
            scope.policy.constraints[1].conditions[1].value = '300';
            scope.validate();
            expect(scope.alerts.length).toEqual(0);
		}));
		
		it('Test update policy', inject(function(PolicyStore, CLMAppLocations, $httpBackend) {
			var policyStoreContents;
			$httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getPolicyUrl())).respond(PolicyMockData.getPolicyData());
			$httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getApplicablePolicies())).respond(ApplicationMockData.getApplicablePolicies());
			PolicyStore.get().get().then(function () {
				policyStoreContents = arguments[0];
				policyScope.policy = policyStoreContents[0];
			});
			$httpBackend.flush();
			parentScope.$apply(function () {
				parentScope.policyEditMap[policyScope.policy.id] = true;
			});

			policyScope.policy.name = 'asdflkasdfkljasfdklj';
			expect(policyScope.policy.isDirty()).toEqual(true);

			$httpBackend.expectPUT(SpecUtil.toRegExp(CLMAppLocations.getPolicyUrl())).respond(angular.extend(angular.copy(policyScope.policy.$getOriginal()), { name : policyScope.policy.name }));
			angular.element('#testInlinePolicyEditor').scope().savePolicy()
			$httpBackend.flush();

			expect(policyStoreContents[0].isDirty()).toEqual(false);
		}));

		it('Test cancel update policy', inject(function(PolicyStore, CLMAppLocations, $httpBackend) {
			var policyStoreContents;
			$httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getPolicyUrl())).respond(PolicyMockData.getPolicyData());
			$httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getApplicablePolicies())).respond(ApplicationMockData.getApplicablePolicies());
			PolicyStore.get().get().then(function () {
				policyStoreContents = arguments[0];
				policyScope.policy = policyStoreContents[0];
			});
			$httpBackend.flush();
			parentScope.policyEditMap[policyScope.policy.id] = true;
			parentScope.$digest();

			policyScope.policy.name = 'asdflkasdfkljasfdklj';
			expect(policyScope.policy.isDirty()).toEqual(true);
			policyScope.$destroy();
			parentScope.$digest();
			expect(policyStoreContents[0].isDirty()).toEqual(false);
		}));
	});

	describe('Constraints', function () {
		function getConstraintEditorController() {
			inject(function($httpBackend, CLMLocations, CLMAppLocations) {
				$httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getConditionTypeUrl())).respond(PolicyMockData.getConditionTypeData());
				$httpBackend.expectGET(SpecUtil.toRegExp(CLMAppLocations.getConditionValueTypeUrl())).respond(PolicyMockData.getConditionValueTypeData());
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
	});

	describe('PolicyStore', function () {
		it('Default Values', inject(function (PolicyStore) {
			var newPolicy = createNewPolicy();
			expect(newPolicy.threatLevel).toEqual(5);
			expect(newPolicy.constraints).toEqual([{ conditions : [ ], operator : 'OR', id : jasmine.any(String) }]);
		}));
	});

	describe('ageInDays', function () {
		var scope = null;

		beforeEach(inject(function ($compile) {
            var node = $("<div id='testAgeInDays' age-in-days ng-model='age'></div>");
            node.appendTo('body');
            scope = testScope.$new();
            $compile(node)(scope);
            scope.$digest();
		}));

		afterEach(function () {
			$('#testAgeInDays').remove();
		});

		it('Simple Number', function () {
			SpecUtil.setInput($('#testAgeInDays input:first'), '1');
			expect(scope.age).toEqual('365'); // year is default
		});

		it('Null Value', function () {
			scope.age = null;
			scope.$digest();
			expect(scope.age).toEqual(null);
			expect($('#testAgeInDays input:first').val()).toEqual('');
		});

		it('Remove Value', function () {
			scope.age = null;
			scope.$digest();
			expect(scope.age).toEqual(null);
			SpecUtil.setInput($('#testAgeInDays input:first'), '1');
			expect(scope.age).toEqual('365');
			SpecUtil.setInput($('#testAgeInDays input:first'), '');
			expect(scope.age).toEqual(null);
		});

		it('Zero Value (edge case)', function () {
			SpecUtil.setInput($('#testAgeInDays input:first'), '0');
			expect(scope.age).toEqual('0');
		});

		// TODO The select event doesn't fire need to investigate
		xit('Change Modifier', function () {
			SpecUtil.setInput($('#testAgeInDays input:first'), '1');
			expect(scope.age).toEqual('365');
			SpecUtil.setInput($('#testAgeInDays select'), 30);
			expect(scope.age).toEqual('30');
		});
	});
});
