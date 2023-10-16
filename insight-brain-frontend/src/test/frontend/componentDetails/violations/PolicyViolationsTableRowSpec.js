/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxFontAwesomeIcon, NxTableCell, NxThreatIndicator } from '@sonatype/react-shared-components';

import * as enzymeUtils from 'TestRoot/enzymeUtils';
import PolicyViolationsTableRow from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationsTableRow';
import ViolationExclamation from 'MainRoot/react/ViolationExclamation';
import ActiveWaiversIndicator from 'MainRoot/violation/ActiveWaiversIndicator';

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
        legacyViolation: false,
        waived: false,
        applicableWaivers: [],
      },
      toggleShowViolationsDetailPopover: jasmine.createSpy('toggleShowViolationsDetailPopover'),
      hasPermissionToAddWaivers: true,
      setSelectedPolicyViolationId: jasmine.createSpy('setSelectedPolicyViolationId'),
    };

    getShallow = enzymeUtils.getShallowComponent(PolicyViolationsTableRow, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(PolicyViolationsTableRow, minimalProps);
  });

  describe('clicking on a row', () => {
    it('calls toggleShowViolationsDetailPopover action', () => {
      const component = getMounted();
      component.simulate('click');
      expect(minimalProps.toggleShowViolationsDetailPopover).toHaveBeenCalledTimes(1);
    });

    it('calls setSelectedPolicyViolationId action', () => {
      const component = getMounted();
      component.simulate('click');
      expect(minimalProps.setSelectedPolicyViolationId).toHaveBeenCalled();
    });
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

    it('renders the threat level of the actions with as disabled when the row is waived', () => {
      const component = getShallow({
          violation: {
            ...minimalProps.violation,
            actions: [
              {
                actionType: 'fail',
                actionSummary: 'Build Failure',
              },
            ],
            waived: true,
          },
        }),
        rowCells = component.find(NxTableCell),
        policyNameAndActionsCell = rowCells.at(1);

      const actionElement = policyNameAndActionsCell.dive().find('li');
      expect(actionElement.find(ViolationExclamation)).toHaveProp('threatLevelCategory', 'disabled');
    });

    it('renders the threat level of the actions with as disabled when the row has legacy violation', () => {
      const component = getShallow({
          violation: {
            ...minimalProps.violation,
            actions: [
              {
                actionType: 'fail',
                actionSummary: 'Build Failure',
              },
            ],
            legacyViolation: true,
          },
        }),
        rowCells = component.find(NxTableCell),
        policyNameAndActionsCell = rowCells.at(1);

      const actionElement = policyNameAndActionsCell.dive().find('li');
      expect(actionElement.find(ViolationExclamation)).toHaveProp('threatLevelCategory', 'disabled');
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

  describe('renders a cell for the waivers actions buttons and relevant indicators', () => {
    describe('renders indicators according to legacy violations and waivers', () => {
      const getMountedIndicators = (additionalProps) => {
        const component = getMounted(additionalProps),
          rowCells = component.find(NxTableCell),
          waiversAndLegacyCell = rowCells.at(4);
        return waiversAndLegacyCell.find(PolicyViolationsTableRow.indicators);
      };

      it('renders a legacy violations indicator if it is a legacy violation', () => {
        const indicators = getMountedIndicators({ violation: { ...minimalProps.violation, legacyViolation: true } });
        expect(indicators).toExist();

        const legacyIcon = indicators.find(NxFontAwesomeIcon);
        expect(legacyIcon).toExist();

        expect(indicators.find('span')).toHaveText('Legacy');
      });

      it('does not render a legacy violations indicator if it is a legacy violation', () => {
        const indicators = getMountedIndicators({ violation: { ...minimalProps.violation, legacyViolation: false } });
        expect(indicators.find('span')).not.toExist();
      });

      it('renders an information indicator when there are unapplied waivers', () => {
        const indicators = getMountedIndicators({
          violation: { ...minimalProps.violation, applicableWaivers: ['waiver1'] },
        });
        const unnappliedIcon = indicators.find(NxFontAwesomeIcon);
        expect(unnappliedIcon).toExist();

        expect(indicators.find('span')).toHaveText('Unapplied Waiver');
      });

      it('does not render an information indicator when there are no unapplied waivers', () => {
        const indicators = getMountedIndicators({ violation: { ...minimalProps.violation, applicableWaivers: [] } });
        expect(indicators.find('span')).not.toExist();
      });

      it('does not render an information indicator when the violation has been waived', () => {
        const indicators = getMountedIndicators({
          violation: { ...minimalProps.violation, applicableWaivers: ['waiver1'], waived: true },
        });

        expect(indicators).toHaveText('1Active Waiver');
      });

      it('renders an ActiveWaiversIndicator when the violation has been waived and has applicableWaivers', () => {
        const indicators = getMountedIndicators({
          violation: { ...minimalProps.violation, applicableWaivers: ['waiver1', 'waiver2'], waived: true },
        });

        const activeWaiversIndicator = indicators.find(ActiveWaiversIndicator);
        expect(activeWaiversIndicator).toExist();
        expect(activeWaiversIndicator).toHaveProp('activeWaiverCount', 2);
        expect(indicators).toHaveText('2Active Waivers');
      });
    });
  });
});
