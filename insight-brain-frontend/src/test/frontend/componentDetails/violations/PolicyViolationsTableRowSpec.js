/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NxButton,
  NxFontAwesomeIcon,
  NxStatefulSegmentedButton,
  NxTableCell,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';
import { omit } from 'ramda';

import * as enzymeUtils from '../../enzymeUtils';
import PolicyViolationsTableRow from '../../../../main/frontend/componentDetails/ViolationsTableTile/PolicyViolationsTableRow';
import ViolationExclamation from '../../../../main/frontend/react/ViolationExclamation';
import ActiveWaiversIndicator from '../../../../main/frontend/violation/ActiveWaiversIndicator';

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
      toggleAddWaiverPopover: jasmine.createSpy('toggleAddWaiverPopover'),
      toggleRequestWaiverPopover: jasmine.createSpy('toggleRequestWaiverPopover'),
      hasPermissionToAddWaivers: true,
      setSelectedPolicyViolationId: jasmine.createSpy('setSelectedPolicyViolationId'),
    };

    getShallow = enzymeUtils.getShallowComponent(PolicyViolationsTableRow, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(PolicyViolationsTableRow, minimalProps);
  });

  describe('clicking on a row', () => {
    describe('makes sure the waiver button still works', () => {
      it('clicks on a row outside of the button and calls the toggleShowViolationsDetailPopover action', () => {
        const component = getMounted();
        component.simulate('click');
        expect(minimalProps.toggleShowViolationsDetailPopover).toHaveBeenCalledTimes(1);
      });

      it('clicks on the request waiver button inside of a row and the toggleShowViolationsDetailPopover action is not called', () => {
        const component = getShallow({ hasPermissionToAddWaivers: false }),
          buttonComponent = component.find(PolicyViolationsTableRow.waiverButtons),
          btn = buttonComponent.dive().find(NxButton);
        btn.simulate('click');
        expect(minimalProps.toggleShowViolationsDetailPopover).not.toHaveBeenCalled();
      });

      it('clicks on the add waiver combo button inside of a row and the toggleShowViolationsDetailPopover action is not called', () => {
        const component = getShallow(),
          buttonComponent = component.find(PolicyViolationsTableRow.waiverButtons),
          btn = buttonComponent.dive().find(NxStatefulSegmentedButton);
        btn.simulate('click');
        expect(minimalProps.toggleShowViolationsDetailPopover).not.toHaveBeenCalled();
      });
    });

    describe('calls setSelectedPolicyViolationId action', () => {
      it('when clicking outside a button', () => {
        const component = getMounted();
        component.simulate('click');
        expect(minimalProps.setSelectedPolicyViolationId).toHaveBeenCalled();
      });

      it('when clicking on the add waiver combo button', () => {
        const component = getMounted(),
          buttonComponent = component.find(PolicyViolationsTableRow.waiverButtons),
          btn = buttonComponent.find(NxStatefulSegmentedButton);
        btn.simulate('click');
        expect(minimalProps.setSelectedPolicyViolationId).toHaveBeenCalled();
      });

      it('when clicking on the request waiver button', () => {
        const component = getMounted({ hasPermissionToAddWaivers: false }),
          buttonComponent = component.find(PolicyViolationsTableRow.waiverButtons),
          btn = buttonComponent.find(NxButton);
        btn.simulate('click');
        expect(minimalProps.setSelectedPolicyViolationId).toHaveBeenCalled();
      });

      it('when clicking on the trigger button of the actions combo', () => {
        let component = getMounted(),
          buttonComponent = component.find(PolicyViolationsTableRow.waiverButtons),
          btn = buttonComponent.find(NxStatefulSegmentedButton),
          dropdownTrigger = btn.find(NxFontAwesomeIcon); // trigger for the segmented button
        dropdownTrigger.simulate('click');

        component.update();
        buttonComponent = component.find(PolicyViolationsTableRow.waiverButtons);
        const requestWaiverOption = buttonComponent.find('.nx-dropdown-button');
        requestWaiverOption.simulate('click');
        expect(minimalProps.setSelectedPolicyViolationId).toHaveBeenCalled();
      });
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
    describe('renders buttons to apply waiver actions', () => {
      const getShallowButtons = (additionalProps) => {
        const component = getShallow(additionalProps),
          rowCells = component.find(NxTableCell),
          waiversAndGrandfatheringCell = rowCells.at(4);

        return waiversAndGrandfatheringCell.find(PolicyViolationsTableRow.waiverButtons);
      };

      it('does not render an add waiver btn if the violation is grandfathered', () => {
        const shallowButtonsComponent = getShallowButtons({
          violation: { ...minimalProps.violation, grandfathered: true },
        });
        expect(shallowButtonsComponent.dive().children().length).toEqual(0);
      });

      it('does not render an add waiver btn if the violation is waived', () => {
        const shallowButtonsComponent = getShallowButtons({ violation: { ...minimalProps.violation, waived: true } });
        expect(shallowButtonsComponent.dive().children().length).toEqual(0);
      });

      it('does not render an add waiver btn if the violation has unapplied waivers', () => {
        const shallowButtonsComponent = getShallowButtons({
          violation: { ...minimalProps.violation, waived: false, applicableWaivers: ['waiver1'] },
        });
        expect(shallowButtonsComponent.dive().children().length).toEqual(0);
      });

      it('renders a request waiver button if the violation is not remediated but the user has no permission to add waivers', () => {
        const shallowButtonsComponent = getShallowButtons({
          hasPermissionToAddWaivers: false,
        });
        const requestWaiverButton = shallowButtonsComponent.dive().find(NxButton);
        expect(requestWaiverButton).toExist();
        expect(requestWaiverButton).toHaveText('Request Waiver');
        requestWaiverButton.simulate('click');
        expect(minimalProps.toggleRequestWaiverPopover).toHaveBeenCalled();
      });

      it('renders an add waiver segmented button if the violation is not remediated and the user has permission to add waivers', () => {
        const shallowButtonsComponent = getShallowButtons();
        const addWaiverSegmentedButton = shallowButtonsComponent.dive().find(NxStatefulSegmentedButton);
        expect(addWaiverSegmentedButton).toExist();
        expect(addWaiverSegmentedButton).toHaveProp('buttonContent', 'Add Waiver');
        expect(addWaiverSegmentedButton).toHaveProp('disabled', false);

        addWaiverSegmentedButton.simulate('click');
        expect(minimalProps.toggleAddWaiverPopover).toHaveBeenCalled();

        const secondaryOption = addWaiverSegmentedButton.find('button');
        expect(secondaryOption).toHaveText('Request Waiver');
        secondaryOption.simulate('click');
        expect(minimalProps.toggleRequestWaiverPopover).toHaveBeenCalled();
      });

      it('renders a disabled add waiver segmented button if the violation is missing policyViolationId', () => {
        const shallowButtonsComponent = getShallowButtons({
            violation: omit(['policyViolationId'], minimalProps.violation),
          }),
          addWaiverSegmentedButton = shallowButtonsComponent.dive().find(NxStatefulSegmentedButton);

        expect(addWaiverSegmentedButton).toExist();
        expect(addWaiverSegmentedButton).toHaveProp('disabled', true);
      });

      it('renders a tooltip when hovering over the disabled add waivers button if the violation is missing policyViolationId', () => {
        const shallowButtonsComponent = getShallowButtons({
            violation: omit(['policyViolationId'], minimalProps.violation),
          }),
          addWaiverSegmentedButton = shallowButtonsComponent.dive().find(NxStatefulSegmentedButton);

        // Since we have a wrapper div to help display the tooltip with need the parent above it
        const upperTooltip = addWaiverSegmentedButton.parents().at(1);
        expect(upperTooltip).toHaveProp('title', 'Re-evaluate this report to enable waivers functionality.');
      });
    });

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
