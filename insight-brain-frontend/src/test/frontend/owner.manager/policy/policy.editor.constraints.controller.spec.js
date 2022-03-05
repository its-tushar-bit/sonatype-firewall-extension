/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import ConditionTypeValueResourceMockData from '../mock.data/conditionTypeValue.mock.data';
import PolicyResourceMockData from '../mock.data/policy.resource.mock.data';
import { getConditionTypeUrl, getConditionValueTypeUrl } from 'MainRoot/util/CLMLocation';
import * as constraintSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesConstraintSelectors';
import { mapStateToThis } from 'MainRoot/owner.manager/policy/policy.editor.constraints.controller';

describe('policy.editor.constraints.controller', () => {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, ($provide) => {
      SpecUtil.mockNgRedux($provide);
    })
  );

  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  let vm;
  beforeEach(inject(($controller) => {
    mockAxiosCalls({
      get: {
        [getConditionTypeUrl()]: Promise.resolve({
          data: PolicyResourceMockData.getConditionTypeUrl(),
        }),
        [getConditionValueTypeUrl('application', 'ownerId')]: Promise.resolve({
          data: ConditionTypeValueResourceMockData.getConditionValueTypeUrl(),
        }),
      },
    });

    vm = $controller('policy.editor.constraints.controller', {}, { constraints: [] });
    vm.isNewPolicy = true;
    vm.$onInit();
  }));

  describe('mapStateToThis', () => {
    it('maps redux properties to component', () => {
      spyOn(constraintSelectors, 'selectLoadError').and.returnValue(null);
      spyOn(constraintSelectors, 'selectIsLoading').and.returnValue(false);
      spyOn(constraintSelectors, 'selectEditConstraintMap').and.returnValue({ 1646123499604: true });
      spyOn(constraintSelectors, 'selectConditionTypesMap').and.returnValue({
        AgeInDays: {
          enabled: true,
          valueTypeId: 'AgeInDaysValueType',
          valueHint: 'Enter term',
          autoUnquarantineSupported: false,
          threatCategory: 'QUALITY',
          name: 'Age',
          id: 'AgeInDays',
          valueTypes: {},
        },
      });
      spyOn(constraintSelectors, 'selectConditionTypes').and.returnValue([
        {
          enabled: true,
          valueTypeId: 'AgeInDaysValueType',
          valueHint: 'Enter term',
          autoUnquarantineSupported: false,
          threatCategory: 'QUALITY',
          name: 'Age',
          id: 'AgeInDays',
          valueTypes: {},
        },
      ]);

      const output = mapStateToThis({});

      expect(output.loadError).toBeNull();
      expect(output.loading).toBeFalse();
      expect(output.editConstraintMap).toEqual({ 1646123499604: true });
      expect(output.conditionTypesMap).toEqual({
        AgeInDays: {
          enabled: true,
          valueTypeId: 'AgeInDaysValueType',
          valueHint: 'Enter term',
          autoUnquarantineSupported: false,
          threatCategory: 'QUALITY',
          name: 'Age',
          id: 'AgeInDays',
          valueTypes: {},
        },
      });
      expect(output.conditionTypes).toEqual([
        {
          enabled: true,
          valueTypeId: 'AgeInDaysValueType',
          valueHint: 'Enter term',
          autoUnquarantineSupported: false,
          threatCategory: 'QUALITY',
          name: 'Age',
          id: 'AgeInDays',
          valueTypes: {},
        },
      ]);
    });
  });

  describe('$onInit()', () => {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    it('calls loadConstraint', () => {
      expect(vm.loadConstraint).toHaveBeenCalledOnceWith({ isNewPolicy: true, constraints: [] });
    });
  });

  describe('$onDestroy()', () => {
    it('unsubscribes from redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  it('properly constructs condition string', () => {
    expect(
      vm.conditionString(
        {
          conditionTypeId: 'Label',
          operator: 'is',
          value: '6be0f524314245c7aded40b3d4ac8112',
        },
        {
          Label: {
            name: 'Label',
            valueType: {
              availableValues: [
                {
                  id: '2438cdfe428141c8b8a06fac9bc699c3',
                  label: 'App Component Label222',
                },
                {
                  id: '6be0f524314245c7aded40b3d4ac8112',
                  label: 'App Component Label',
                },
              ],
            },
          },
        }
      )
    ).toMatch('Label is App Component Label');

    expect(
      vm.conditionString(
        {
          conditionTypeId: 'License Threat Group',
          operator: 'is not',
          value: 'd341ca90a4ea4971aa84376148892c7d',
        },
        {
          'License Threat Group': {
            name: 'License Threat Group',
            valueType: {
              availableValues: [
                {
                  id: '2438cdfe428141c8b8a06fac9bc699c3',
                  name: 'Glory',
                },
                {
                  id: 'd341ca90a4ea4971aa84376148892c7d',
                  name: 'Liberal',
                },
              ],
            },
          },
        }
      )
    ).toMatch('License Threat Group is not Liberal');

    expect(
      vm.conditionString(
        {
          conditionTypeId: 'AgeInDays',
          operator: 'older than',
          value: '730',
        },
        {
          AgeInDays: {
            name: 'Age',
          },
        }
      )
    ).toMatch('Age older than 2 Years');

    expect(
      vm.conditionString(
        {
          conditionTypeId: 'SecurityVulnerabilityStatus',
          operator: 'is',
          value: 'ACKNOWLEDGED',
        },
        {
          SecurityVulnerabilityStatus: {
            name: 'Security Vulnerability Status',
            valueType: {
              availableValues: [
                {
                  id: 'NOT_ACKNOWLEDGED',
                  name: 'Not Acknowledged',
                },
                {
                  id: 'ACKNOWLEDGED',
                  name: 'Acknowledged',
                },
              ],
            },
          },
        }
      )
    ).toMatch('Security Vulnerability Status is Acknowledged');
  });

  it('properly adds conditions', () => {
    const constraint = {
      id: 'beCarefulWithKnives',
      operator: 'OR',
      conditions: [
        {
          conditionTypeId: 'AgeInDays',
          operator: 'older than',
          value: '730',
        },
        {
          conditionTypeId: 'SecurityVulnerabilityStatus',
          operator: 'is',
          value: 'ACKNOWLEDGED',
        },
      ],
    };

    vm.addCondition(constraint);

    expect(constraint.conditions.length).toBe(3);
    expect(constraint.conditions[2]).toEqual({
      conditionTypeId: 'AgeInDays',
      operator: 'older than',
      value: null,
    });
  });

  it('properly deletes conditions', () => {
    const constraint = {
      id: 'beCarefulWithKnives',
      operator: 'OR',
      conditions: [
        {
          conditionTypeId: 'AgeInDays',
          operator: 'older than',
          value: '730',
        },
        {
          conditionTypeId: 'SecurityVulnerabilityStatus',
          operator: 'is',
          value: 'ACKNOWLEDGED',
        },
        {
          conditionTypeId: 'AgeInDays',
          operator: 'older than',
          value: null,
        },
      ],
    };

    vm.deleteCondition(constraint, 2);

    expect(constraint.conditions.length).toBe(2);
    expect(constraint.conditions[2]).toBeUndefined();
  });

  it('properly adds constraints', () => {
    vm.constraints = [
      {
        id: 'knife1',
        operator: 'OR',
        conditions: [],
      },
      {
        id: 'knife2',
        operator: 'OR',
        conditions: [],
      },
    ];

    vm.addConstraint();

    expect(vm.constraints.length).toBe(3);
    expect(vm.constraints[2].operator).toEqual('OR');
    expect(vm.constraints[2].conditions.length).toBe(1);
    expect(vm.constraints[2].conditions[0]).toEqual({
      conditionTypeId: 'AgeInDays',
      operator: 'older than',
    });
    expect(vm.updateEditConstraintId).toHaveBeenCalledTimes(1);
  });

  it('properly deletes constraints', () => {
    vm.constraints = [
      {
        id: 'knife1',
        operator: 'OR',
        conditions: [],
      },
      {
        id: 'knife2',
        operator: 'OR',
        conditions: [],
      },
    ];

    vm.deleteConstraint(1);
    expect(vm.constraints.length).toBe(1);
    expect(vm.constraints[1]).toBeUndefined();
  });
});
