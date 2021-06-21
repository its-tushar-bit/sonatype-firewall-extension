/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import { NxTableCell, NxThreatIndicator } from '@sonatype/react-shared-components';

import PolicyViolationsTableRow from '../../../../main/frontend/componentDetails/violations/PolicyViolationsTableRow';
import ViolationExclamation from '../../../../main/frontend/react/ViolationExclamation';

describe('PolicyViolationsTableRow', () => {
  let minimalProps, getShallow, getMounted;

  beforeEach(function () {
    minimalProps = {
      violation: {
        policyViolationId: 'policyViolationId',
        policyThreatLevel: 10,
        policyName: 'Security-Blocker',
        actions: [],
        constraints: [
          {
            constraintName: 'Critical score',
            conditions: [
              { conditionReason: 'first reason from first constraint' },
              { conditionReason: 'second reason from first constraint' },
            ],
          },
          { conditions: [{ conditionReason: 'first reason from second constraint' }] },
        ],
      },
    };

    getShallow = enzymeUtils.getShallowComponent(PolicyViolationsTableRow, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(PolicyViolationsTableRow, minimalProps);
    console.log(getMounted);
  });

  it('renders a Threat cell with the policyThreatLevel and an indicator related to the threat level', () => {
    const component = getShallow(),
      rowCells = component.find(NxTableCell),
      threatLevelCell = rowCells.at(0);

    const threatIndicator = threatLevelCell.find(NxThreatIndicator);
    expect(threatIndicator).toExist();
    expect(threatIndicator).toHaveProp('policyThreatLevel', 10);
  });

  describe('renders a Policy name and action cell', () => {
    it('renders the policy name in the cell', () => {
      const component = getShallow(),
        rowCells = component.find(NxTableCell),
        policyNameAndActionsCell = rowCells.at(1);

      expect(policyNameAndActionsCell.find('span')).toHaveText('Security-Blocker');
    });

    it('renders the policy name and the actions with threat level', () => {
      const component = getShallow({
          violation: {
            ...minimalProps.violation,
            actions: [
              {
                actionType: 'fail',
                actionSummary: 'Build Failure',
              },
            ],
          },
        }),
        rowCells = component.find(NxTableCell),
        policyNameAndActionsCell = rowCells.at(1);

      expect(policyNameAndActionsCell.find('span').at(0)).toHaveText('Security-Blocker');

      const actionElement = policyNameAndActionsCell.dive().find('li');
      expect(actionElement.find(ViolationExclamation)).toHaveProp('threatLevelCategory', 'critical');
      expect(actionElement.find('span')).toHaveText('Build Failure');
    });
  });

  it('renders a Constraint name cell with the constraint name of the first constraint of the violation', () => {
    const component = getShallow(),
      rowCells = component.find(NxTableCell),
      constraintNameCell = rowCells.at(2);

    expect(constraintNameCell).toHaveProp('children', 'Critical score');
  });

  it('renders a Condition cell with all the conditions from the constraints of the violation', () => {
    const component = getShallow(),
      rowCells = component.find(NxTableCell),
      conditionCell = rowCells.at(3);

    const reasons = conditionCell.find('p');

    expect(reasons.length).toBe(3);
    expect(reasons.at(0)).toHaveText('first reason from first constraint');
    expect(reasons.at(1)).toHaveText('second reason from first constraint');
    expect(reasons.at(2)).toHaveText('first reason from second constraint');
  });
});
