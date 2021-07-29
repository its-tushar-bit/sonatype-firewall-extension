/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import { faFlag } from '@fortawesome/pro-solid-svg-icons';
import { NxButton, NxFontAwesomeIcon, NxTableCell, NxThreatIndicator } from '@sonatype/react-shared-components';

import PolicyViolationsTableRow from '../../../../main/frontend/componentDetails/violations/PolicyViolationsTableRow';
import ViolationExclamation from '../../../../main/frontend/react/ViolationExclamation';
import ActiveWaiversIndicator from '../../../../main/frontend/violation/ActiveWaiversIndicator';

describe('PolicyViolationsTableRow', () => {
  let minimalProps, getShallow, goToWaiversSpy, getMounted, setShowViolationsDetailSpy;

  beforeEach(function () {
    goToWaiversSpy = jasmine.createSpy('goToWaiversForViolation');
    setShowViolationsDetailSpy = jasmine.createSpy('setShowViolationsDetail');
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
      goToWaivers: goToWaiversSpy,
      setShowViolationsDetail: setShowViolationsDetailSpy,
    };

    getShallow = enzymeUtils.getShallowComponent(PolicyViolationsTableRow, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(PolicyViolationsTableRow, minimalProps);
  });

  describe('clicks on a row and makes sure the waiver button still works', () => {
    it('clicks on a row outside of the button and calls the setShowViolationsDetail action', () => {
      const component = getMounted();
      component.simulate('click');
      expect(setShowViolationsDetailSpy).toHaveBeenCalledTimes(1);
    });

    it('clicks on a button inside of a row and the setShowViolationsDetail action is not called', () => {
      const component = getShallow(),
        btn = component.find(NxButton);
      btn.simulate('click');
      expect(goToWaiversSpy).toHaveBeenCalledTimes(1);
      expect(setShowViolationsDetailSpy).toHaveBeenCalledTimes(0);
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

  describe('renders a cell for the manage waivers trigger and relevant indicators', () => {
    const getIndicators = (additionalProps) => {
      const component = getShallow(additionalProps),
        rowCells = component.find(NxTableCell),
        waiversAndGrandfatheringCell = rowCells.at(4);

      return waiversAndGrandfatheringCell.find(PolicyViolationsTableRow.indicators);
    };

    it('renders a manage waivers btn if the violation is not grandfathered', () => {
      const component = getShallow({ violation: { ...minimalProps.violation, grandfathered: false } }),
        rowCells = component.find(NxTableCell),
        waiversAndGrandfatheringCell = rowCells.at(4),
        btn = waiversAndGrandfatheringCell.find(NxButton);

      expect(btn).toExist();
      expect(btn.find(NxFontAwesomeIcon)).toHaveProp('icon', faFlag);
      expect(btn.find('span')).toHaveText('Manage Waivers');

      btn.simulate('click');
      expect(goToWaiversSpy).toHaveBeenCalledWith('policyViolationId');
    });

    it('does not render a manage waivers btn if the violation is grandfathered', () => {
      const component = getShallow({ violation: { ...minimalProps.violation, grandfathered: true } }),
        rowCells = component.find(NxTableCell),
        waiversAndGrandfatheringCell = rowCells.at(4),
        btn = waiversAndGrandfatheringCell.find(NxButton);

      expect(btn).not.toExist();
    });

    it('renders a grandfathering indicator if the violation has been grandfathered', () => {
      const indicators = getIndicators({ violation: { ...minimalProps.violation, grandfathered: true } });
      expect(indicators).toExist();

      const grandfatheringIcon = indicators.dive().find(NxFontAwesomeIcon);
      expect(grandfatheringIcon).toExist();

      expect(indicators.dive().find('span')).toHaveText('Grandfathered');
    });

    it('does not render a grandfathering indicator if the violation has not been grandfathered', () => {
      const indicators = getIndicators({ violation: { ...minimalProps.violation, grandfathered: false } });
      expect(indicators.dive().find('span')).not.toExist();
    });

    it('renders an information indicator when there are unapplied waivers', () => {
      const indicators = getIndicators({ violation: { ...minimalProps.violation, applicableWaivers: ['waiver1'] } });

      const unnappliedIcon = indicators.dive().find(NxFontAwesomeIcon);
      expect(unnappliedIcon).toExist();

      expect(indicators.dive().find('span')).toHaveText('Unapplied Waiver');
    });

    it('does not render an information indicator when there are no unapplied waivers', () => {
      const indicators = getIndicators({ violation: { ...minimalProps.violation, applicableWaivers: [] } });
      expect(indicators.dive().find('span')).not.toExist();
    });

    it('does not render an information indicator when the violation has been waived', () => {
      const indicators = getIndicators({
        violation: { ...minimalProps.violation, applicableWaivers: ['waiver1'], waived: true },
      });

      expect(indicators.dive().find('span')).not.toExist();
    });

    it('renders an ActiveWaiversIndicator when the violation has been waived and has applicableWaivers', () => {
      const indicators = getIndicators({
        violation: { ...minimalProps.violation, applicableWaivers: ['waiver1', 'waiver2'], waived: true },
      });

      const activeWaiversIndicator = indicators.dive().find(ActiveWaiversIndicator);
      expect(activeWaiversIndicator).toExist();
      expect(activeWaiversIndicator).toHaveProp('noOfWaivers', 2);
    });
  });
});
