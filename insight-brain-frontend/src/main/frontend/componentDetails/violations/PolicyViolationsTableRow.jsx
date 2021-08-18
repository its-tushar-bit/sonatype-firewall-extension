/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { flatten } from 'ramda';

import ViolationExclamation from '../../react/ViolationExclamation';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxStatefulSegmentedButton,
  NxTableCell,
  NxTableRow,
  NxThreatIndicator,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faChevronRight, faHistory, faInfoCircle } from '@fortawesome/pro-solid-svg-icons';
import classnames from 'classnames';
import ActiveWaiversIndicator from '../../violation/ActiveWaiversIndicator';

const ACTION_ICON_CATEGORY = {
  fail: 'critical',
  warn: 'severe',
};

export default function PolicyViolationsTableRow({
  violation,
  toggleShowViolationsDetailPopover,
  toggleAddWaiverPopover,
  toggleRequestWaiverPopover,
  hasPermissionToAddWaivers,
  setSelectedPolicyViolationId,
}) {
  const { policyThreatLevel, policyName, constraints, actions, grandfathered, waived, policyViolationId } = violation;
  const [firstConstraint] = constraints;
  const reasons = flatten(
    constraints.map((constraint) => constraint.conditions.map((condition) => condition.conditionReason))
  );
  const isRemediated = grandfathered || waived;
  const rowClassNames = classnames('iq-policy-violation-row', {
    'iq-policy-violation-row--remediated': isRemediated,
  });

  const setPolicyViolationIdToShow = (e) => {
    if (!e.target.closest('button') && policyViolationId) {
      setSelectedPolicyViolationId(policyViolationId);
      toggleShowViolationsDetailPopover();
    }
  };

  const renderActionsAsList = (actions = []) => {
    if (actions.length === 0) {
      return null;
    }

    return (
      <ul>
        {actions.map((action) => {
          return (
            <li key={action.actionType}>
              <ViolationExclamation
                threatLevelCategory={isRemediated ? 'disabled' : ACTION_ICON_CATEGORY[action.actionType]}
              />
              <span>{action.actionSummary}</span>
            </li>
          );
        })}
      </ul>
    );
  };

  return (
    <NxTableRow className={rowClassNames} isClickable onClick={setPolicyViolationIdToShow}>
      <NxTableCell className={classnames({ disabled: isRemediated })}>
        <NxThreatIndicator policyThreatLevel={policyThreatLevel} />
        <span className="nx-threat-number">{policyThreatLevel}</span>
      </NxTableCell>
      <NxTableCell className="iq-policy-violation-row__policy-name-and-action-cell">
        <span>{policyName}</span>
        {renderActionsAsList(actions)}
      </NxTableCell>
      <NxTableCell>{firstConstraint ? firstConstraint.constraintName : null}</NxTableCell>
      <NxTableCell>
        {reasons &&
          reasons.map((reason, index) => {
            return <p key={index}>{reason}</p>;
          })}
      </NxTableCell>
      <NxTableCell className="iq-policy-violation-row__actions-and-indicators-cell">
        <PolicyViolationsWaiverButtons
          violation={violation}
          openAddWaiverPopover={toggleAddWaiverPopover}
          openRequestWaiverPopover={toggleRequestWaiverPopover}
          hasPermissionToAddWaivers={hasPermissionToAddWaivers}
          setSelectedPolicyViolationId={setSelectedPolicyViolationId}
        />
        <PolicyViolationsGrandfatheringAndWaiverIndicators violation={violation} />
      </NxTableCell>
      <NxTableCell>
        <NxFontAwesomeIcon icon={faChevronRight} />
      </NxTableCell>
    </NxTableRow>
  );
}

export const violationPropTypes = {
  policyViolationId: PropTypes.string.isRequired,
  policyThreatLevel: PropTypes.number.isRequired,
  policyName: PropTypes.string.isRequired,
  actions: PropTypes.arrayOf(
    PropTypes.shape({
      actionType: PropTypes.string.isRequired,
      actionSummary: PropTypes.string.isRequired,
    })
  ),
  constraints: PropTypes.arrayOf(
    PropTypes.shape({
      constraintName: PropTypes.string,
      conditions: PropTypes.arrayOf(
        PropTypes.shape({
          conditionReason: PropTypes.string,
        })
      ),
    })
  ),
  grandfathered: PropTypes.bool,
  waived: PropTypes.bool,
  applicableWaivers: PropTypes.arrayOf(PropTypes.string),
};

PolicyViolationsTableRow.propTypes = {
  violation: PropTypes.shape(violationPropTypes),
  toggleShowViolationsDetailPopover: PropTypes.func,
  toggleAddWaiverPopover: PropTypes.func.isRequired,
  toggleRequestWaiverPopover: PropTypes.func.isRequired,
  hasPermissionToAddWaivers: PropTypes.bool.isRequired,
  setSelectedPolicyViolationId: PropTypes.func.isRequired,
};

/* Helper component for grandfathering and waiver indicators. */
const PolicyViolationsGrandfatheringAndWaiverIndicators = ({ violation }) => {
  const { waived, grandfathered, applicableWaivers = [] } = violation;
  const numberOfWaivers = applicableWaivers.length;

  const pendingWaiversIndicator =
    !waived && numberOfWaivers > 0 ? (
      <div>
        <NxFontAwesomeIcon icon={faInfoCircle} />
        <span>Unapplied Waiver</span>
      </div>
    ) : null;

  const appliedWaiversIndicator =
    waived && numberOfWaivers > 0 ? <ActiveWaiversIndicator noOfWaivers={numberOfWaivers} /> : null;

  const grandfatheredIndicator = grandfathered ? (
    <div>
      <NxFontAwesomeIcon icon={faHistory} />
      <span>Grandfathered</span>
    </div>
  ) : null;

  return (
    <Fragment>
      {pendingWaiversIndicator}
      {appliedWaiversIndicator}
      {grandfatheredIndicator}
    </Fragment>
  );
};

PolicyViolationsTableRow.indicators = PolicyViolationsGrandfatheringAndWaiverIndicators;
PolicyViolationsGrandfatheringAndWaiverIndicators.propTypes = { violation: PropTypes.shape(violationPropTypes) };

const PolicyViolationsWaiverButtons = ({
  violation,
  openAddWaiverPopover,
  openRequestWaiverPopover,
  hasPermissionToAddWaivers,
  setSelectedPolicyViolationId,
}) => {
  const { policyViolationId, grandfathered, waived, applicableWaivers = [] } = violation;
  const isGrandfatheredOrWaived = grandfathered || waived || applicableWaivers.length > 0;
  if (isGrandfatheredOrWaived) {
    return null;
  }

  const openRequestWaiverPopoverHandler = () => {
    setSelectedPolicyViolationId(policyViolationId);
    openRequestWaiverPopover();
  };
  const openAddWaiverPopoverHandler = () => {
    setSelectedPolicyViolationId(policyViolationId);
    openAddWaiverPopover();
  };
  const unavailableWaiverActionsTooltip = !policyViolationId
    ? 'Re-evaluate this report to enable waivers functionality.'
    : '';

  if (!hasPermissionToAddWaivers) {
    const requestButtonClassnames = classnames('iq-policy-violation__request-waivers-btn', {
      disabled: !policyViolationId,
    });
    return (
      <NxTooltip title={unavailableWaiverActionsTooltip}>
        <div>
          <NxButton variant="tertiary" className={requestButtonClassnames} onClick={openRequestWaiverPopoverHandler}>
            <span>Request Waiver</span>
          </NxButton>
        </div>
      </NxTooltip>
    );
  }

  const segmentedButtonClassnames = classnames('iq-policy-violation__waivers-dropdown-btn', {
    disabled: !policyViolationId,
  });
  return (
    <NxTooltip title={unavailableWaiverActionsTooltip}>
      <div>
        <NxStatefulSegmentedButton
          className={segmentedButtonClassnames}
          variant="tertiary"
          onClick={openAddWaiverPopoverHandler}
          buttonContent="Add Waiver"
          disabled={!policyViolationId}
        >
          <button className="nx-dropdown-button" onClick={openRequestWaiverPopoverHandler}>
            <span>Request Waiver</span>
          </button>
        </NxStatefulSegmentedButton>
      </div>
    </NxTooltip>
  );
};

PolicyViolationsTableRow.waiverButtons = PolicyViolationsWaiverButtons;
PolicyViolationsWaiverButtons.propTypes = {
  violation: PropTypes.shape(violationPropTypes),
  openAddWaiverPopover: PropTypes.func.isRequired,
  openRequestWaiverPopover: PropTypes.func.isRequired,
  hasPermissionToAddWaivers: PropTypes.bool.isRequired,
  setSelectedPolicyViolationId: PropTypes.func.isRequired,
};
