/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import * as constraintSelectors from 'MainRoot/OrgsAndPolicies/constraintSelectors';
import { mapStateToThis } from 'MainRoot/owner.manager/policy/policy.editor.constraints.controller';

describe('policy.editor.constraints.controller', () => {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, ($provide) => {
      SpecUtil.mockNgRedux($provide);
    })
  );

  let vm;
  beforeEach(inject(($controller) => {
    vm = $controller('policy.editor.constraints.controller', {}, { constraints: [] });
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

  describe('addConstraint', () => {
    it('calls updateEditConstraintId and addConstraintAction', () => {
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

      expect(vm.addConstraintAction).toHaveBeenCalledOnceWith([
        ...vm.constraints,
        jasmine.objectContaining({
          conditions: [
            {
              conditionTypeId: 'AgeInDays',
              operator: 'older than',
            },
          ],
          operator: 'OR',
        }),
      ]);
      expect(vm.updateEditConstraintId).toHaveBeenCalledTimes(1);
    });
  });

  describe('deleteConstraint', () => {
    it('calls deleteConstraintAction', () => {
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

      const constraintIndex = 1;
      vm.deleteConstraint(constraintIndex);

      expect(vm.deleteConstraintAction).toHaveBeenCalledOnceWith([vm.constraints[0]]);
    });
  });

  describe('addCondition', () => {
    it('calls deleteConditionAction', () => {
      vm.constraints = [
        {
          id: 'knife1',
          operator: 'OR',
          conditions: [],
        },
        {
          id: 'knife1',
          operator: 'OR',
          conditions: [],
        },
      ];
      const defaultNewCondition = {
        conditionTypeId: 'AgeInDays',
        operator: 'older than',
        value: null,
      };

      const constraintIndex = 1;
      vm.addCondition(constraintIndex);

      expect(vm.addConditionAction).toHaveBeenCalledOnceWith({
        constraintIndex,
        value: [...vm.constraints[constraintIndex].conditions, defaultNewCondition],
      });
    });
  });

  describe('deleteCondition', () => {
    it('calls deleteConditionAction', () => {
      vm.constraints = [
        {
          id: 'knife1',
          operator: 'OR',
          conditions: [],
        },
        {
          id: 'knife1',
          operator: 'OR',
          conditions: [
            {
              conditionTypeId: 'AgeInDays',
              operator: 'older than',
              value: null,
            },
            {
              conditionTypeId: 'relativePercentage',
              operator: 'moreThan',
              value: 3,
            },
          ],
        },
      ];

      const constraintIndex = 1,
        conditionIndex = 1;
      const updatedArray = vm.constraints[constraintIndex].conditions.filter((_, index) => index !== conditionIndex);

      vm.deleteCondition(constraintIndex, conditionIndex);

      expect(vm.deleteConditionAction).toHaveBeenCalledOnceWith({
        constraintIndex,
        conditionIndex,
        value: updatedArray,
      });
    });
  });

  describe('onConditionTypeIdChange', () => {
    it('calls actions to set default values for associated fields', () => {
      vm.conditionTypesMap = {
        AgeInDays: {
          enabled: true,
          autoUnquarantineSupported: false,
          supportedOperators: ['older than', 'younger than'],
          valueTypeId: 'AgeInDaysValueType',
          valueHint: 'Enter term',
          threatCategory: 'QUALITY',
          name: 'Age',
          id: 'AgeInDays',
          valueType: {
            availableValues: null,
            allowMultiple: false,
            dataType: 'Integer',
            id: 'AgeInDaysValueType',
          },
        },
      };
      vm.constraints = [
        {
          id: 'knife1',
          operator: 'OR',
          conditions: [],
        },
        {
          id: 'knife1',
          operator: 'OR',
          conditions: [
            {
              conditionTypeId: 'relativePercentage',
              operator: 'older than',
              value: null,
            },
            {
              conditionTypeId: 'AgeInDays',
              operator: 'moreThan',
              value: 3,
            },
          ],
        },
      ];

      const constraintIndex = 1,
        conditionIndex = 1;

      vm.onConditionTypeIdChange(constraintIndex, conditionIndex);

      expect(vm.setConstraintCondition).toHaveBeenCalledOnceWith({
        constraintIndex,
        conditionIndex,
        value: { conditionTypeId: 'AgeInDays', operator: 'older than', value: null },
      });
    });
  });

  describe('onConstraintNameChange', () => {
    it('calls setConstraintName', () => {
      vm.constraints = [
        {
          id: 'knife1',
          operator: 'OR',
          conditions: [],
        },
        {
          id: 'knife1',
          operator: 'OR',
          name: 'somebody',
          conditions: [],
        },
      ];

      const constraintIndex = 1;

      vm.onConstraintNameChange(constraintIndex);

      expect(vm.setConstraintName).toHaveBeenCalledOnceWith({
        constraintIndex,
        value: 'somebody',
      });
    });
  });

  describe('onConstraintOperatorChange', () => {
    it('calls setConstraintOperator', () => {
      vm.constraints = [
        {
          id: 'knife1',
          operator: 'OR',
          conditions: [],
        },
        {
          id: 'knife1',
          operator: 'AND',
          conditions: [],
        },
      ];

      const constraintIndex = 1;

      vm.onConstraintOperatorChange(constraintIndex);

      expect(vm.setConstraintOperator).toHaveBeenCalledOnceWith({
        constraintIndex,
        value: 'AND',
      });
    });
  });

  describe('onConditionOperatorChange', () => {
    it('calls setConditionOperator', () => {
      vm.constraints = [
        {
          id: 'knife1',
          operator: 'OR',
          conditions: [],
        },
        {
          id: 'knife1',
          operator: 'AND',
          conditions: [
            {
              conditionTypeId: 'AgeInDays',
              operator: 'older than',
              value: null,
            },
            {
              conditionTypeId: 'relativePercentage',
              operator: 'moreThan',
              value: 3,
            },
          ],
        },
      ];

      const constraintIndex = 1,
        conditionIndex = 1;

      vm.onConditionOperatorChange(constraintIndex, conditionIndex);

      expect(vm.setConditionOperator).toHaveBeenCalledOnceWith({
        constraintIndex,
        conditionIndex,
        value: 'moreThan',
      });
    });
  });

  describe('onConditionValueChange', () => {
    it('calls setConditionValue', () => {
      vm.constraints = [
        {
          id: 'knife1',
          operator: 'OR',
          conditions: [],
        },
        {
          id: 'knife1',
          operator: 'AND',
          conditions: [
            {
              conditionTypeId: 'AgeInDays',
              operator: 'older than',
              value: null,
            },
            {
              conditionTypeId: 'relativePercentage',
              operator: 'moreThan',
              value: 3,
            },
          ],
        },
      ];

      const constraintIndex = 1,
        conditionIndex = 1;

      vm.onConditionValueChange(constraintIndex, conditionIndex);

      expect(vm.setConditionValue).toHaveBeenCalledOnceWith({
        constraintIndex,
        conditionIndex,
        value: 3,
      });
    });
  });
});
