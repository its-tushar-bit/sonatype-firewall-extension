describe('PolicyEditor.js', function() {
  var testScope = null,
      dialogScope = null;

  function getController(controllerName) {
    var controller = null,
        scope = null;

    inject(function($controller, $httpBackend) {
      scope = testScope.$new();
      controller = $controller(controllerName, {$scope: scope});
      $httpBackend.flush();
    });

    return { controller: controller, scope: scope };
  }

  function expectActionRequests() {
    inject(function($httpBackend, CLMLocations) {
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMLocations.getActionTypeUrl())).respond(PolicyMockData.getActionTypeData());
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMLocations.getActionStageUrl())).respond(MockData.getActionStageData());
    });
  }

  function createNewPolicy() {
    var policy = null;
    inject(function(PolicyStore, $httpBackend, CLMLocations, CLMAppLocations) {
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMLocations.getConditionTypeUrl())).respond(PolicyMockData.getConditionTypeData());
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getConditionValueTypeUrl())).respond(PolicyMockData.getConditionValueTypeData());
      policy = PolicyStore.get().create();
    });
    return policy;
  }

  beforeEach(module('PolicyEditor', 'HttpInterceptors', 'AngularCommon', 'CLMLocation', 'CLMAppLocation', function($provide) {
    $provide.value('$modal', {
      open: function(config) {
        dialogScope = testScope.$new();
        dialogScope.$close = function() {
        };
        inject(function($controller) {
          $controller(config.controller, {
            $scope: dialogScope
          });
        });
        return {
          result: {
            then: function(success, failure) {
              success();
            }
          }
        };
      }
    });

    $provide.value('ApplicationId', {
      encoded: function() {
        return 'bom1-12345678';
      }
    });
    $provide.value('OrganizationId', {
      encoded: function() {
        return null;
      }
    });
  }));

  beforeEach(inject(function($rootScope) {
    testScope = $rootScope.$new();
  }));

  afterEach(inject(function($httpBackend) {
    if (testScope) {
      testScope.$destroy();
    }
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  describe('inlinePolicyCreator', function() {
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

    beforeEach(inject(function($compile, $httpBackend) {
      getPolicyEditorController();
      createNewPolicy();
      var node = $("<div id='testInlinePolicyCreator' inline-policy-creator></div>");
      node.appendTo('body');
      scope = testScope.$new(); // testScope's destruction cascades
      $httpBackend.whenGET("policy-quick-add").respond('<div ng-if="policy">' + template + '</div>');
      $httpBackend.whenGET("../assets/components/policy-editor/condition-editor.html?").respond(conditionTemplate);
      $httpBackend.whenGET("../assets/components/policy-editor/constraint-editor.html?").respond(constraintEditorTemplate);
      $compile(node)(scope);
      $httpBackend.flush();
    }));

    afterEach(inject(function($httpBackend) {
      $('#testInlinePolicyCreator').remove();
    }));

    it('Create', inject(function($httpBackend) {
      var createScope = angular.element('#testInlinePolicyCreator').scope().$$childTail;

      createScope.click();
      expect(createScope.policy).not.toBeUndefined();

      expect(angular.element('#testInlinePolicyCreator').scope().$$childTail.policy).toBeDefined();
    }));

    it('Saving', inject(function($httpBackend, CLMAppLocations) {
      var createScope = angular.element('#testInlinePolicyCreator').scope().$$childTail;
      //creating a policy for the createScope should then trigger load of the child scope
      createScope.click();
      scope.$digest();

      var policyEditorScope = angular.element('.inline-policy-editor').scope();
      // short-circuit the validation in a way we can still confirm this was called
      spyOn(policyEditorScope, 'validate').andReturn(true);
      $httpBackend.whenGET("../assets/components/policy-editor/condition-editor.html?").respond(conditionTemplate);
      $httpBackend.whenGET('../assets/components/policy-editor/constraint-editor.html?').respond(constraintEditorTemplate);

      $httpBackend.expectPOST(SpecUtil.toRegExp(CLMAppLocations.getPolicyUrl())).respond({
        id: 'foo'
      });

      policyEditorScope.savePolicy();
      $httpBackend.flush();

      expect(angular.element('#testInlinePolicyCreator').scope().policy).toEqual(null);
      expect(policyEditorScope.validate).toHaveBeenCalled();
    }));

    it('Cancel', inject(function($httpBackend) {
      var createScope = angular.element('#testInlinePolicyCreator').scope().$$childTail;
      createScope.click();
      scope.$digest();

      var policyEditorScope = angular.element('.inline-policy-editor').scope();
      expect(policyEditorScope.policy).not.toBeNull();
      $httpBackend.whenGET("../assets/components/policy-editor/condition-editor.html?").respond(conditionTemplate);
      $httpBackend.whenGET('../assets/components/policy-editor/constraint-editor.html?').respond(constraintEditorTemplate);
      policyEditorScope.cancel();
      $httpBackend.flush();
      expect(policyEditorScope.policy).toBeNull();
    }));

    it('Operator hidden when one condition', inject(function($httpBackend) {
      var createScope = angular.element('#testInlinePolicyCreator').scope().$$childTail;

      $httpBackend.whenGET('../assets/components/policy-editor/constraint-editor.html?').respond(constraintEditorTemplate);
      createScope.$apply(function() {
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

    describe('isDirty', function() {
      it('Unchanged', function() {
        var createScope = angular.element('#testInlinePolicyCreator').scope().$$childTail;
        createScope.click();
        expect(testScope.$broadcast('pageChangeStarted').defaultPrevented).toEqual(false);
      });
      it('Policy Name', inject(function($httpBackend) {
        var createScope = angular.element('#testInlinePolicyCreator').scope().$$childTail;
        createScope.click();
        scope.$digest();

        var policyEditorScope = angular.element('.inline-policy-editor').scope();
        $httpBackend.whenGET("../assets/components/policy-editor/condition-editor.html?").respond(conditionTemplate);
        $httpBackend.whenGET('../assets/components/policy-editor/constraint-editor.html?').respond(constraintEditorTemplate);
        $httpBackend.flush();
        policyEditorScope.policy.name = 'foo';
        expect(testScope.$broadcast('pageChangeStarted').defaultPrevented).toEqual(true);
      }));
      it('Constraint Name', inject(function($httpBackend) {
        var createScope = angular.element('#testInlinePolicyCreator').scope().$$childTail;
        createScope.click();
        scope.$digest();

        var policyEditorScope = angular.element('.inline-policy-editor').scope();
        $httpBackend.whenGET("../assets/components/policy-editor/condition-editor.html?").respond(conditionTemplate);
        $httpBackend.whenGET('../assets/components/policy-editor/constraint-editor.html?').respond(constraintEditorTemplate);
        $httpBackend.flush();
        policyEditorScope.policy.constraints[0].name = 'foo'
        expect(testScope.$broadcast('pageChangeStarted').defaultPrevented).toEqual(true);
      }));
    });
  });

  describe('InlinePolicyEditor', function() {
    var template = SpecUtil.getTemplate("../assets/components/policy-editor/policy-inline-editor.html"),
        constraintEditorTemplate = SpecUtil.getTemplate("../assets/components/policy-editor/constraint-editor.html"),
        conditionEditorTemplate = SpecUtil.getTemplate("../assets/components/policy-editor/condition-editor.html"),
        parentScope = null,
        policyScope = null,
        scope = null;

    beforeEach(inject(function($compile, $httpBackend, CLMLocations, CLMAppLocations) {
      var node = $("<div><div ng-if='policyEditMap[policy.id]'><div id='testInlinePolicyEditor' inline-policy-editor '></div></div></div>");
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

    afterEach(function() {
      $('#testInlinePolicyEditor').remove();
    });

    it('Test policy validation', inject(function($httpBackend) {
      //policy name uses the form validation stuff
      var form = {
        name: {
          $error: {
            required: true,
            spaces: true,
            alphaNumeric: true
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
      parentScope.$digest();
      scope.policy.constraints = [];

      var policyEditorScope = angular.element('.inline-policy-editor').scope();
      policyEditorScope[scope.getFormName()] = form;
      validateValidation(policyEditorScope, 'Policy name is required.');
      form.name.$error.required = false;
      validateValidation(policyEditorScope, 'Policy name cannot contain leading, trailing or double spaces or tabs.');

      form.name.$error.spaces = false;
      validateValidation(policyEditorScope, 'Policy name must be alpha numeric.');

      form.name.$error.alphaNumeric = false;
      validateValidation(policyEditorScope, 'You must add at least one constraint to the policy.');

      scope.policy.constraints.push({});
      validateValidation(policyEditorScope, 'Enter a valid name for constraint #1');

      scope.policy.constraints[0].name = 'name';
      validateValidation(policyEditorScope, 'You must select any or all of the conditions for constraint "name"');

      scope.policy.constraints[0].operator = 'OR';
      validateValidation(policyEditorScope, 'You must add at least one condition to constraint "name"');

      scope.policy.constraints[0].conditions = [
        {}
      ];
      validateValidation(policyEditorScope,
          'Please select a valid condition type for condition #1 in constraint "name"');

      scope.policy.constraints[0].conditions[0].conditionTypeId = 'AgeInDays';
      validateValidation(policyEditorScope, 'Please enter a whole number for condition #1 in constraint "name"');

      scope.policy.constraints[0].conditions[0].conditionTypeId = 'SecurityVulnerabilitySeverity';
      validateValidation(policyEditorScope, 'Please enter a decimal number for condition #1 in constraint "name"');

      scope.policy.constraints[0].conditions[0].conditionTypeId = 'SecurityVulnerabilityStatus';
      validateValidation(policyEditorScope, 'Please enter a value for condition #1 in constraint "name"');

      scope.policy.constraints[0].conditions[0].value = '300';
      scope.policy.constraints.push({});
      validateValidation(policyEditorScope, 'Enter a valid name for constraint #2');

      scope.policy.constraints[1].name = 'name';
      validateValidation(policyEditorScope, 'You must select any or all of the conditions for constraint "name"');

      scope.policy.constraints[1].operator = 'OR';
      validateValidation(policyEditorScope, 'You must add at least one condition to constraint "name"');

      scope.policy.constraints[1].conditions = [
        {}
      ];
      validateValidation(policyEditorScope,
          'Please select a valid condition type for condition #1 in constraint "name"');

      scope.policy.constraints[1].conditions[0].conditionTypeId = 'AgeInDays';
      validateValidation(policyEditorScope, 'Please enter a whole number for condition #1 in constraint "name"');

      scope.policy.constraints[1].conditions[0].value = '300';
      scope.policy.constraints[1].conditions.push({});
      validateValidation(policyEditorScope,
          'Please select a valid condition type for condition #2 in constraint "name"');

      scope.policy.constraints[1].conditions[1].conditionTypeId = 'AgeInDays';
      validateValidation(policyEditorScope, 'Please enter a whole number for condition #2 in constraint "name"');

      scope.policy.constraints[1].conditions[1].value = '300';
      policyEditorScope.validate();
      expect(policyEditorScope.alerts.length).toEqual(0);
      $httpBackend.flush();
    }));

    it('Test update policy', inject(function(PolicyStore, CLMAppLocations, $httpBackend) {
      var policyStoreContents;
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getPolicyUrl())).respond(PolicyMockData.getPolicyData());
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getApplicablePolicies())).respond(ApplicationMockData.getApplicablePolicies());
      PolicyStore.get().get().then(function() {
        policyStoreContents = arguments[0];
        policyScope.policy = policyStoreContents[0];
      });
      $httpBackend.flush();
      parentScope.$apply(function() {
        parentScope.policyEditMap[policyScope.policy.id] = true;
      });

      policyScope.policy.name = 'asdflkasdfkljasfdklj';
      expect(policyScope.policy.isDirty()).toEqual(true);

      $httpBackend.expectPUT(SpecUtil.toRegExp(CLMAppLocations.getPolicyUrl())).respond(angular.extend(angular.copy(policyScope.policy.$getOriginal()),
          { name: policyScope.policy.name }));
      angular.element('.inline-policy-editor').scope().savePolicy();
      $httpBackend.flush();

      expect(policyStoreContents[0].isDirty()).toEqual(false);
    }));

    it('Test cancel update policy', inject(function(PolicyStore, CLMAppLocations, $httpBackend) {
      var policyStoreContents;
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getPolicyUrl())).respond(PolicyMockData.getPolicyData());
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getApplicablePolicies())).respond(ApplicationMockData.getApplicablePolicies());
      PolicyStore.get().get().then(function() {
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
      $httpBackend.flush();
    }));
  });

  describe('Constraints', function() {
    function getConstraintEditorController() {
      inject(function($httpBackend, CLMLocations, CLMAppLocations) {
        $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getConditionTypeUrl())).respond(PolicyMockData.getConditionTypeData());
        $httpBackend.expectGET(SpecUtil.toRegExp(CLMAppLocations.getConditionValueTypeUrl())).respond(PolicyMockData.getConditionValueTypeData());
      });

      return getController('ConstraintEditorController');
    }

    describe('ConstraintEditor', function() {
      it('New Constraint - Dirty Checks', inject(function($httpBackend) {
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
        $httpBackend.flush();
      }));

      xit('figures dirty state of existing constraint', inject(function() {
        var controller = getConstraintEditorController(),
            constraint = {
              name: 'Name',
              conditions: [
                { conditionTypeId: 'Label', operator: 'is', value: 'red' }
              ],
              operator: 'OR'
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

  describe('PolicyStore', function() {
    it('Default Values', inject(function($httpBackend) {
      var newPolicy = createNewPolicy();
      $httpBackend.flush();
      expect(newPolicy.threatLevel).toEqual(5);
      expect(newPolicy.constraints).toEqual([
        { conditions: [], operator: 'OR', id: jasmine.any(String) }
      ]);
    }));
  });

  describe('ageInDays', function() {
    var scope = null;

    beforeEach(inject(function($compile) {
      var node = $("<div id='testAgeInDays' age-in-days ng-model='age'></div>");
      node.appendTo('body');
      scope = testScope.$new();
      $compile(node)(scope);
      scope.$digest();
    }));

    afterEach(function() {
      $('#testAgeInDays').remove();
    });

    it('Simple Number', function() {
      SpecUtil.setInput($('#testAgeInDays input:first'), '1');
      expect(scope.age).toEqual('365'); // year is default
    });

    it('Null Value', function() {
      scope.age = null;
      scope.$digest();
      expect(scope.age).toEqual(null);
      expect($('#testAgeInDays input:first').val()).toEqual('');
    });

    it('Remove Value', function() {
      scope.age = null;
      scope.$digest();
      expect(scope.age).toEqual(null);
      SpecUtil.setInput($('#testAgeInDays input:first'), '1');
      expect(scope.age).toEqual('365');
      SpecUtil.setInput($('#testAgeInDays input:first'), '');
      expect(scope.age).toEqual(null);
    });

    it('Zero Value (edge case)', function() {
      SpecUtil.setInput($('#testAgeInDays input:first'), '0');
      expect(scope.age).toEqual('0');
    });

    // TODO The select event doesn't fire need to investigate
    xit('Change Modifier', function() {
      SpecUtil.setInput($('#testAgeInDays input:first'), '1');
      expect(scope.age).toEqual('365');
      SpecUtil.setInput($('#testAgeInDays select'), 30);
      expect(scope.age).toEqual('30');
    });
  });

  describe('Email notification display formatter', function() {
    beforeEach(inject(function() {
      expectActionRequests();
      testScope.policy = createNewPolicy();
      editorScope = getController('PolicyEditorController').scope;
    }));

    it('No format when no emails', function() {
      testScope.policy.actions.foo = [];

      var formatted = editorScope.getFormattedEmailList({ id: 'foo' });
      expect(formatted).toEqual('');
    });

    it('Formats single email', function() {
      testScope.policy.actions.foo = [
        { actionTypeId: 'notify', target: 'single@email.org' }
      ];

      var formatted = editorScope.getFormattedEmailList({ id: 'foo' });
      expect(formatted).toEqual('single@email.org');
    });

    it('Formats multiple emails', function() {
      testScope.policy.actions.foo = [
        { actionTypeId: 'notify', target: 'any@email.org' },
        { actionTypeId: 'notify', target: 'another@email.org' }
      ];

      var formatted = editorScope.getFormattedEmailList({ id: 'foo' });
      expect(formatted).toEqual('any@email.org, another@email.org');
    });
  });

  describe('Edit Notifications', function() {
    var editorScope;

    beforeEach(inject(function() {
      expectActionRequests();
      testScope.policy = createNewPolicy();
      editorScope = getController('PolicyEditorController').scope;
    }));

    it('Save no addresses', function() {
      editorScope.editNotification({ id: 'foo' });
      dialogScope.save();
      expect(testScope.policy.actions.foo).toEqual([]);
    });

    it('Save One New Address', function() {
      editorScope.editNotification({ id: 'foo' });
      dialogScope.notificationEmailList.push('single@example.org');
      dialogScope.save();
      expect(testScope.policy.actions.foo).toEqual([
        { actionTypeId: 'notify', target: 'single@example.org' }
      ]);
    });

    it('Save Multiple New Addresses', function() {
      editorScope.editNotification({ id: 'foo' });
      dialogScope.notificationEmailList.push('one@example.org', 'two@example.org');
      dialogScope.save();
      expect(testScope.policy.actions.foo).toEqual([
        { actionTypeId: 'notify', target: 'one@example.org' },
        { actionTypeId: 'notify', target: 'two@example.org' }
      ]);
    });

    it('Cancel Does Not Update', function() {
      editorScope.editNotification({ id: 'foo' });
      dialogScope.notificationEmailList.push('cancelled@example.org');
      dialogScope.$close();
      expect(testScope.policy.actions.foo).toBeUndefined();
    });

    it('Adds to Existing Addresses', function () {
      testScope.policy.actions.foo = [
        { actionTypeId: 'notify', target: 'existing@example.org' },
        { actionTypeId: 'notify', target: 'another.existing@example.org' }
      ];

      editorScope.editNotification({ id: 'foo' });
      dialogScope.notificationEmailList.push('new@example.org');
      dialogScope.save();

      expect(testScope.policy.actions.foo.length).toEqual(3);
      expect(testScope.policy.actions.foo).toEqual([
        { actionTypeId: 'notify', target: 'existing@example.org' },
        { actionTypeId: 'notify', target: 'another.existing@example.org' },
        { actionTypeId: 'notify', target: 'new@example.org' }
      ]);
    });

    it('Removes Existing Address', function () {
      testScope.policy.actions.foo = [
        { actionTypeId: 'notify', target: 'existing@example.org' },
        { actionTypeId: 'notify', target: 'another.existing@example.org' }
      ];

      editorScope.editNotification({ id: 'foo' });
      dialogScope.notificationEmailList.push('new@example.org', 'gnu@example.org');
      dialogScope.notificationEmailList.splice(0,1);
      dialogScope.save();

      expect(testScope.policy.actions.foo.length).toEqual(3);
      expect(testScope.policy.actions.foo).toEqual([
        { actionTypeId: 'notify', target: 'another.existing@example.org' },
        { actionTypeId: 'notify', target: 'new@example.org' },
        { actionTypeId: 'notify', target: 'gnu@example.org' }
      ]);
    });

    it('Accepts brain 1.6 concatenated emails', function () {
      testScope.policy.actions.foo = [
        { actionTypeId: 'notify', target: 'chang.bunker@siam.th,eng.bunker@siam.th' }
      ];

      editorScope.editNotification({ id: 'foo' });
      dialogScope.notificationEmailList.push('abby.hensel@mn.us', 'brittany.hensel@mn.us');
      dialogScope.save();

      expect(testScope.policy.actions.foo).toEqual([
        { actionTypeId: 'notify', target: 'chang.bunker@siam.th,eng.bunker@siam.th' },
        { actionTypeId: 'notify', target: 'abby.hensel@mn.us' },
        { actionTypeId: 'notify', target: 'brittany.hensel@mn.us' }
      ]);
    });
  });

  /**
   * Testing is not exhaustive as under the hood this shares the same code path as
   * all conditions previously tested in 'Editing Notifications'.
   */
  describe('Edit Monitoring Notifications', function() {
    var editorScope;

    beforeEach(inject(function() {
      expectActionRequests();
      testScope.policy = createNewPolicy();
      editorScope = getController('PolicyEditorController').scope;
    }));

    it('Save Multiple New Addresses', function() {
      editorScope.editMonitoringNotificationActions();
      dialogScope.notificationEmailList.push('one@example.org', 'two@example.org');
      dialogScope.save();
      expect(testScope.policy.monitorNotifyActions).toEqual([
        { actionTypeId: 'notify', target: 'one@example.org' },
        { actionTypeId: 'notify', target: 'two@example.org' }
      ]);
    });
  });
});
