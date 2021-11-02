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
        grandfathered: false,
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

    it('renders the threat level of the actions with as disabled when the row is grandfathered', () => {
      const component = getShallow({
          violation: {
            ...minimalProps.violation,
            actions: [
              {
                actionType: 'fail',
                actionSummary: 'Build Failure',
              },
            ],
            grandfathered: true,
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
    describe('renders indicators according to grandfathering and waivers', () => {
      const getShallowIndicators = (additionalProps) => {
        const component = getShallow(additionalProps),
          rowCells = component.find(NxTableCell),
          waiversAndGrandfatheringCell = rowCells.at(4);

        return waiversAndGrandfatheringCell.find(PolicyViolationsTableRow.indicators);
      };

      it('renders a grandfathering indicator if the violation has been grandfathered', () => {
        const indicators = getShallowIndicators({ violation: { ...minimalProps.violation, grandfathered: true } });
        expect(indicators).toExist();

        const grandfatheringIcon = indicators.dive().find(NxFontAwesomeIcon);
        expect(grandfatheringIcon).toExist();

        expect(indicators.dive().find('span')).toHaveText('Grandfathered');
      });

      it('does not render a grandfathering indicator if the violation has not been grandfathered', () => {
        const indicators = getShallowIndicators({ violation: { ...minimalProps.violation, grandfathered: false } });
        expect(indicators.dive().find('span')).not.toExist();
      });

      it('renders an information indicator when there are unapplied waivers', () => {
        const indicators = getShallowIndicators({
          violation: { ...minimalProps.violation, applicableWaivers: ['waiver1'] },
        });

        const unnappliedIcon = indicators.dive().find(NxFontAwesomeIcon);
        expect(unnappliedIcon).toExist();

        expect(indicators.dive().find('span')).toHaveText('Unapplied Waiver');
      });

      it('does not render an information indicator when there are no unapplied waivers', () => {
        const indicators = getShallowIndicators({ violation: { ...minimalProps.violation, applicableWaivers: [] } });
        expect(indicators.dive().find('span')).not.toExist();
      });

      it('does not render an information indicator when the violation has been waived', () => {
        const indicators = getShallowIndicators({
          violation: { ...minimalProps.violation, applicableWaivers: ['waiver1'], waived: true },
        });

        expect(indicators.dive().find('span')).not.toExist();
      });

      it('renders an ActiveWaiversIndicator when the violation has been waived and has applicableWaivers', () => {
        const indicators = getShallowIndicators({
          violation: { ...minimalProps.violation, applicableWaivers: ['waiver1', 'waiver2'], waived: true },
        });

        const activeWaiversIndicator = indicators.dive().find(ActiveWaiversIndicator);
        expect(activeWaiversIndicator).toExist();
        expect(activeWaiversIndicator).toHaveProp('noOfWaivers', 2);
      });
    });
  });
});
