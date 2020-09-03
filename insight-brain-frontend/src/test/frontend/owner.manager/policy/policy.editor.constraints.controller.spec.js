/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';
import ConditionTypeValueResourceMockData from '../mock.data/conditionTypeValue.mock.data';
import PolicyResourceMockData from '../mock.data/policy.resource.mock.data';

describe('policy.editor.constraints.controller.spec.js', function() {

  beforeEach(angular.mock.module(ownerManagerModule.name, function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  var vm,
      $timeout,
      constraintStoreDefer,
      ConstraintStore;

  beforeEach(inject(function($q, _$timeout_, $controller, _ConstraintStore_) {
    $timeout = _$timeout_;

    ConstraintStore = _ConstraintStore_;
    constraintStoreDefer = $q.defer();

    spyOn(constraintStoreDefer.promise, 'then').and.callThrough();
    spyOn(ConstraintStore, 'get').and.returnValue(constraintStoreDefer.promise);

    vm = $controller('policy.editor.constraints.controller', {}, {constraints: []});
  }));

  it('Properly loads conditions', function() {
    var conditionTypeValues = {};
    ConditionTypeValueResourceMockData.getConditionValueTypeUrl().forEach(function(typeValue) {
      conditionTypeValues[typeValue.id] = typeValue;
    });

    resolveLoadData();

    expect(ConstraintStore.get).toHaveBeenCalled();
    Object.keys(vm.conditionTypes).forEach(function(type) {
      expect(vm.conditionTypes[type].valueType).toEqual(conditionTypeValues[vm.conditionTypes[type].valueTypeId]);
    });
  });

  it('Properly constructs condition string', function() {

    resolveLoadData();

    expect(vm.conditionString({
      conditionTypeId: 'Label',
      operator: 'is',
      value: '6be0f524314245c7aded40b3d4ac8112'
    })).toMatch('Label is App Component Label');

    expect(vm.conditionString({
      conditionTypeId: 'License Threat Group',
      operator: 'is not',
      value: 'd341ca90a4ea4971aa84376148892c7d'
    })).toMatch('License Threat Group is not Liberal');

    expect(vm.conditionString({
      conditionTypeId: 'AgeInDays',
      operator: 'older than',
      value: '730'
    })).toMatch('Age older than 2 Years');

    expect(vm.conditionString({
      conditionTypeId: 'SecurityVulnerabilityStatus',
      operator: 'is',
      value: 'ACKNOWLEDGED'
    })).toMatch('Security Vulnerability Status is Acknowledged');
  });

  it('Properly add/deletes conditions', function() {
    resolveLoadData();

    var constraint = {
      id: 'beCarefulWithKnives',
      operator: 'OR',
      conditions: [
        {
          conditionTypeId: 'AgeInDays',
          operator: 'older than',
          value: '730'
        },
        {
          conditionTypeId: 'SecurityVulnerabilityStatus',
          operator: 'is',
          value: 'ACKNOWLEDGED'
        }
      ]
    };

    vm.addCondition(constraint);
    expect(constraint.conditions.length).toBe(3);
    expect(constraint.conditions[2]).toEqual({
      conditionTypeId: 'AgeInDays',
      operator: 'older than',
      value: null
    });

    vm.deleteCondition(constraint, 2);
    expect(constraint.conditions.length).toBe(2);
    expect(constraint.conditions[2]).toBeUndefined();
  });

  it('Properly add/deletes constraints', function() {
    resolveLoadData();
    vm.constraints = [
      {
        id: 'knife1',
        operator: 'OR',
        conditions: []
      }, {
        id: 'knife2',
        operator: 'OR',
        conditions: []
      }
    ];

    vm.addConstraint();
    expect(vm.constraints.length).toBe(3);
    expect(vm.constraints[2].operator).toEqual('OR');
    expect(vm.constraints[2].conditions.length).toBe(1);
    expect(vm.constraints[2].conditions[0]).toEqual({
      conditionTypeId: 'AgeInDays',
      operator: 'older than'
    });
    expect(vm.editConstraintMap[vm.constraints[2].id]).toBeTruthy();

    vm.deleteConstraint(2);
    expect(vm.constraints.length).toBe(2);
    expect(vm.constraints[2]).toBeUndefined();
  });

  function resolveLoadData() {
    constraintStoreDefer.resolve([
      PolicyResourceMockData.getConditionTypeUrl(), ConditionTypeValueResourceMockData.getConditionValueTypeUrl()
    ]);

    $timeout.flush();
  }
});
