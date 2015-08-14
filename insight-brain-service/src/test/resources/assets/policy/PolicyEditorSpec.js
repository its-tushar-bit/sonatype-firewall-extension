describe('PolicyEditor.js', function() {
  var testScope = null,
    bomId = 'bom1-12345678',
    tags = [{
      id: 'tagId1',
      ownerId: bomId,
      name: 'foo',
      description: 'foo'
    }, {
      id: 'tagId2',
      ownerId: bomId,
      name: 'bar',
      description: 'bar'
    }, {
      id: 'tagId3',
      ownerId: bomId,
      name: 'baz',
      description: 'baz'
    }],
    roles = {
      membersByRole: [
        { roleId: 'foo', roleName: 'bar' },
        { roleId: 'baz', roleName: 'qux' }
      ]
    };

  function getPolicyEditorScope(){
    var scope = angular.element('.inline-policy-editor').scope();
    return  scope;
  }
  function getController(controllerName, parentScope, noFlush) {
    var controller = null,
        scope = null;

    inject(function($controller, $httpBackend) {
      if (!parentScope) {
        scope = testScope.$new();
      } else {
        scope = parentScope.$new();
      }

      controller = $controller(controllerName, {$scope: scope});

      if (!noFlush) {
        $httpBackend.flush();
      }
    });

    return { controller: controller, scope: scope };
  }

  function expectActionRequests() {
    inject(function($httpBackend, CLMLocations, CLMAppLocations) {
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMLocations.getActionStageUrl())).respond(MockData.getActionStageData());
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getRoleMappingUrl())).respond(roles);
    });
  }

  function createNewPolicy() {
    var policy = null;
    inject(function(PolicyStore, $httpBackend, CLMLocations, CLMAppLocations) {
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMLocations.getConditionTypeUrl())).respond(PolicyMockData.getConditionTypeData());
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getConditionValueTypeUrl())).respond(PolicyMockData.getConditionValueTypeData());
      PolicyStore.get().then(function(store) {
        policy = store.create();
      });
      try {
        $httpBackend.flush();
      } catch (e) {
        // condition types were already loaded by policy store, that's fine
      }
    });
    return policy;
  }

  beforeEach(module('PolicyEditor', 'HttpInterceptors', 'AngularCommon', 'CLMLocation', 'CLMAppLocation', function($provide) {
    $provide.value('ApplicationId', {
      encoded: function() {
        return bomId;
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
      testScope.policy = createNewPolicy();
      testScope.tags = [];
      getPolicyEditorController();
      var node = $("<div id='testInlinePolicyCreator' inline-policy-creator tags='tags' policy-tag-map='policyTagMap'></div>");
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
      $httpBackend.flush();
      expect(createScope.policy).not.toBeUndefined();

      expect(angular.element('#testInlinePolicyCreator').scope().$$childTail.policy).toBeDefined();
    }));

    it('Saving', inject(function($httpBackend, CLMAppLocations) {
      var createScope = angular.element('#testInlinePolicyCreator').scope().$$childTail;
      //creating a policy for the createScope should then trigger load of the child scope
      createScope.click();
      scope.$digest();

      var policyEditorScope = getPolicyEditorScope();
      policyEditorScope.policy = testScope.policy;
      // short-circuit the validation in a way we can still confirm this was called
      spyOn(policyEditorScope, 'validate').andReturn(true);
      $httpBackend.whenGET("../assets/components/policy-editor/condition-editor.html?").respond(conditionTemplate);
      $httpBackend.whenGET('../assets/components/policy-editor/constraint-editor.html?').respond(constraintEditorTemplate);

      $httpBackend.expectPOST(SpecUtil.toRegExp(CLMAppLocations.getPolicyUrl())).respond({
        id: 'foo'
      });
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMAppLocations.getPolicyTagUrl('foo'))).respond([]);

      policyEditorScope.savePolicy();
      $httpBackend.flush();

      expect(angular.element('#testInlinePolicyCreator').scope().policy).toEqual(testScope.policy);
      expect(policyEditorScope.validate).toHaveBeenCalled();
    }));

    it('Cancel', inject(function($httpBackend) {
      var createScope = angular.element('#testInlinePolicyCreator').scope().$$childTail;
      createScope.click();
      scope.$digest();

      var policyEditorScope = getPolicyEditorScope();
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

        var policyEditorScope = getPolicyEditorScope();
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

        var policyEditorScope = getPolicyEditorScope();
        $httpBackend.whenGET("../assets/components/policy-editor/condition-editor.html?").respond(conditionTemplate);
        $httpBackend.whenGET('../assets/components/policy-editor/constraint-editor.html?').respond(constraintEditorTemplate);
        $httpBackend.flush();
        policyEditorScope.policy.constraints[0].name = 'foo';
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
      testScope.policy = createNewPolicy();
      testScope.tags = [];
      var node = $("<div><div ng-if='policyEditMap[policy.id]'><div id='testInlinePolicyEditor' inline-policy-editor tags='tags'></div></div></div>");
      node.appendTo('body');
      expectActionRequests();
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
            validNameCharacters: true
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

      var policyEditorScope = getPolicyEditorScope();
      policyEditorScope[scope.getFormName()] = form;
      validateValidation(policyEditorScope, 'Policy name is required.');
      form.name.$error.required = false;
      validateValidation(policyEditorScope, 'Policy name cannot contain leading, trailing or double spaces or tabs.');

      form.name.$error.spaces = false;
      validateValidation(policyEditorScope, 'Policy name must use valid characters: alphanumeric, "_", ".", "-", or spaces.');

      form.name.$error.validNameCharacters = false;
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

      policyEditorScope.hasPolicyTags = true;
      validateValidation(policyEditorScope, 'Must select tags to associate with the policy.');
      policyEditorScope.appliedTagIds.push('foo');

      policyEditorScope.validate();
      expect(policyEditorScope.alerts.length).toEqual(0);
      $httpBackend.flush();
    }));

    it('Test update policy', inject(function(PolicyStore, CLMAppLocations, $httpBackend) {
      var policyStoreContents;
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getPolicyUrl())).respond(PolicyMockData.getPolicyData());
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getApplicablePolicies())).respond(ApplicationMockData.getApplicablePolicies());
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getPolicyTagUrl(PolicyMockData.getPolicyData()[0].id))).respond(tags);
      PolicyStore.get().then(function(store) {
        return store.get();
      }).then(function() {
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
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getPolicyTagUrl(PolicyMockData.getPolicyData()[0].id))).respond(tags);
      PolicyStore.get().then(function(store) {
        return store.get();
      }).then(function() {
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

  describe('Policy Tags', function() {
    // OrgId null due to $provider set to return org id null in test set up
    var scope,
        orgId = null,
        appliedTags = [{
          'id':'tagId1',
          'organizationId':orgId,
          'name':'Tag One Name',
          'description':'Tag One Description',
          'color':'orange'
        },{
          'id':'tagId2',
          'organizationId':orgId,
          'name':'Tag Two Name',
          'description':'Tag Two Description',
          'color':'red'
        }];

    beforeEach(inject(function($state, $httpBackend, CLMAppLocations) {
      $state.current.name = 'management.organization';

      testScope.policy = createNewPolicy();
      testScope.policy.$new = false;
      testScope.tags = tags;
      expectActionRequests();

      $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getPolicyTagUrl(undefined))).respond(appliedTags);
      scope = getController('PolicyEditorController').scope;
    }));

    it('loads applied policy tags', function() {
      expect(scope.appliedTagIds.length).toBe(2);
      expect(scope.appliedTagIds).toEqual([ 'tagId1', 'tagId2' ]);
    });

    it('marks editor dirty when applied policy tag changes', function() {
      scope.appliedTagIds.splice(0, 1);
      var e = scope.$broadcast('pageChangeStarted');
      expect(e.defaultPrevented).toBeTruthy();
    });

    it('adjusts hasPolicyTags', function() {
      expect(scope.hasPolicyTags).toBe(true);

      scope.appliedTagIds.splice(0, 2);
      scope.$digest();
      expect(scope.hasPolicyTags).toBe(false);

      scope.appliedTagIds.push('tagId1');
      scope.$digest();
      expect(scope.hasPolicyTags).toBe(true);
    });

    it('saves updated policy tag information', inject(function($httpBackend, CLMAppLocations) {
      scope.getFormName = function() { return null; };
      scope.hide = jasmine.createSpy('hide');

      scope.appliedTagIds.splice(0, 1);
      scope.appliedTagIds.push('tagId3');

      var policyId, policyTags;
      scope.$on('policySaveComplete', function(event, id, tags){
        policyId = id;
        policyTags = tags;
      });

      $httpBackend.expectPUT(SpecUtil.toRegExp(CLMAppLocations.getPolicyUrl())).respond(testScope.policy);
      $httpBackend.expectDELETE(SpecUtil.toRegExp(CLMAppLocations.getPolicyTagUrl(undefined).substring(0, CLMAppLocations.getPolicyTagUrl(undefined).indexOf('?')) + '/tagId1?orgId=null')).respond(204);
      $httpBackend.expectPOST(SpecUtil.toRegExp(CLMAppLocations.getPolicyTagUrl(undefined))).respond(200);
      scope.savePolicy();
      $httpBackend.flush();
      expect(scope.hide).toHaveBeenCalled();
      expect(policyId).toBeUndefined(); //we didn't actually
      expect(policyTags).toBeDefined();
      expect(policyTags.length).toBe(2);
      expect(policyTags[0].id).toBe('tagId2');
      expect(policyTags[1].id).toBe('tagId3');
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
      expect(newPolicy.threatLevel).toEqual(5);
      expect(newPolicy.constraints).toEqual([
        { conditions: [ { conditionTypeId: 'AgeInDays', operator: 'older than', value: null } ], operator: 'OR', id: jasmine.any(String) }
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

    afterEach(inject(function($httpBackend) {
      $('#testAgeInDays').remove();
    }));

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

  describe('Notification target aggregators', function() {
    beforeEach(inject(function() {
      expectActionRequests();
      testScope.policy = createNewPolicy();
      editorScope = getController('PolicyEditorController').scope;
    }));

    it('No email when no emails', function() {
      testScope.policy.actions.foo = [];

      var emails = editorScope.getEmailList({ id: 'foo' });
      expect(emails).toEqual([]);
    });

    it('No role when no roles', function() {
      testScope.policy.actions.foo = [];

      var roleList = editorScope.getRolesList({ id: 'foo' });
      expect(roleList).toEqual([]);
    });

    it('Returns single email', function() {
      testScope.policy.actions.foo = [
        { actionTypeId: 'notify', target: 'single@email.org' }
      ];

      var emails = editorScope.getEmailList({ id: 'foo' });
      expect(emails).toEqual(['single@email.org']);
    });

    it('Returns single role', function() {
      testScope.policy.actions.foo = [
        { actionTypeId: 'notify', target: roles.membersByRole[0].roleId, targetType: 'role' }
      ];

      var roleList = editorScope.getRolesList({ id: 'foo' });
      expect(roleList).toEqual([roles.membersByRole[0]]);
    });

    it('Returns multiple emails', function() {
      testScope.policy.actions.foo = [
        { actionTypeId: 'notify', target: 'any@email.org' },
        { actionTypeId: 'notify', target: 'another@email.org' }
      ];

      var emails = editorScope.getEmailList({ id: 'foo' });
      expect(emails).toEqual(['any@email.org', 'another@email.org']);
    });

    it('Returns multiple roles', function() {
      testScope.policy.actions.foo = [
        { actionTypeId: 'notify', target: roles.membersByRole[0].roleId, targetType: 'role' },
        { actionTypeId: 'notify', target: roles.membersByRole[1].roleId, targetType: 'role' }
      ];

      var roleList = editorScope.getRolesList({ id: 'foo' });
      expect(roleList).toEqual([roles.membersByRole[0], roles.membersByRole[1]]);
    });

    it('Ignores roles for emails list', function() {
      testScope.policy.actions.foo = [
        { actionTypeId: 'notify', target: roles.membersByRole[0].roleId, targetType: 'role' }
      ];

      var emails = editorScope.getEmailList({ id: 'foo' });
      expect(emails).toEqual([]);
    });

    it('Ignores emails for roles list', function() {
      testScope.policy.actions.foo = [
        { actionTypeId: 'notify', target: 'any@email.org' }
      ];

      var roleList = editorScope.getRolesList({ id: 'foo' });
      expect(roleList).toEqual([]);
    });
  });

  describe('Notification Modal Controller', function() {
    var editorScope, originalActions = [
      { actionTypeId: 'notify', target: 'any@email.org' },
      { actionTypeId: 'notify', target: roles.membersByRole[0].roleId, targetType: 'role' }
    ];

    beforeEach(inject(function() {
      expectActionRequests();
      testScope.policy = createNewPolicy();
      editorScope = getController('PolicyEditorController').scope;
      editorScope.actions = angular.copy(originalActions);
      dialogScope = getController('NotificationModalController', editorScope, true).scope;
      dialogScope.$close = jasmine.createSpy();
    }));

    it('Populates emails', function() {
      expect(dialogScope.notifications.emails).toEqual([originalActions[0].target]);
    });

    it('Populates roles', function() {
      expect(dialogScope.notifications.roles).toEqual([roles.membersByRole[0]]);
    });

    it('Populates available roles', function() {
      expect(dialogScope.availableRoles).toEqual([roles.membersByRole[1]]);
    });

    it('Closes modal on save', function() {
      dialogScope.save();
      expect(dialogScope.$close).toHaveBeenCalled();
    })

    it('Save no changes', function() {
      dialogScope.save();
      expect(editorScope.actions).toEqual(originalActions);
    });

    it('Save New Address', function() {
      dialogScope.entries.email = 'single@example.org';
      dialogScope.addEmail();
      dialogScope.save();
      expect(editorScope.actions).toEqual([
        originalActions[0],
        { actionTypeId: 'notify', target: 'single@example.org' },
        originalActions[1]
      ]);
    });

    it('Clears address after adding', function() {
      dialogScope.entries.email = 'single@example.org';
      dialogScope.addEmail();
      expect(dialogScope.entries.email).toEqual('');
    });

    it('Saves New Role', function() {
      dialogScope.entries.role = { roleId: 'baz'};
      dialogScope.addRole();
      dialogScope.save();
      expect(editorScope.actions).toEqual([
        originalActions[0],
        originalActions[1],
        { actionTypeId: 'notify', target: 'baz', targetType: 'role' }
      ]);
    });

    it('Removes role after adding', function() {
      dialogScope.entries.role = { roleId: 'baz'};
      dialogScope.addRole();
      expect(dialogScope.availableRoles).toEqual([]);
    });

    it('Saves New Address and Role', function() {
      dialogScope.entries.email = 'single@example.org';
      dialogScope.addEmail();
      dialogScope.entries.role = { roleId: 'bar'};
      dialogScope.addRole();
      dialogScope.save();
      expect(editorScope.actions).toEqual([
        originalActions[0],
        { actionTypeId: 'notify', target: 'single@example.org' },
        originalActions[1],
        { actionTypeId: 'notify', target: 'bar', targetType: 'role' }
      ]);
    });

    it('Allows comma separated Emails from Brain < 1.6', function() {
      dialogScope.entries.email = 'one@foo.com,two@bar.com';
      var validation = dialogScope.validateEmail(dialogScope.entries.email);
      expect(validation.email).toBe(true);
      dialogScope.addEmail();
      dialogScope.save();
      expect(editorScope.actions).toEqual([
        originalActions[0],
        { actionTypeId: 'notify', target: 'one@foo.com,two@bar.com' },
        originalActions[1]
      ]);
    });

    it('Removes Role', function () {
      dialogScope.remove(dialogScope.notifications.roles[0], dialogScope.notifications.roles);
      dialogScope.save();
      expect(editorScope.actions).toEqual([
        originalActions[0]
      ]);
    });
  });
});
