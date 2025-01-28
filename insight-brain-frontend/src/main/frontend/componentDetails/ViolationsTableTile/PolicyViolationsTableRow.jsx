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
  NxFontAwesomeIcon,
  NxSmallTag,
  NxTableCell,
  NxTableRow,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';
import { faHistory } from '@fortawesome/pro-solid-svg-icons';
import classnames from 'classnames';
import ActiveWaiversIndicator from '../../violation/ActiveWaiversIndicator';
import ReachabilityStatus from 'MainRoot/componentDetails/ReachabilityStatus/ReachabilityStatus';

const ACTION_ICON_CATEGORY = {
  fail: 'critical',
  warn: 'severe',
};

export default function PolicyViolationsTableRow({
  violation,
  toggleShowViolationsDetailPopover,
  setSelectedPolicyViolationId,
  isAutoWaiversEnabled,
}) {
  const {
    policyThreatLevel,
    policyName,
    constraints,
    actions,
    legacyViolation,
    waived,
    policyViolationId,
    reachabilityStatus,
  } = violation;
  const [firstConstraint] = constraints;
  const reasons = flatten(
    constraints.map((constraint) => constraint.conditions.map((condition) => condition.conditionReason))
  );
  const isRemediated = legacyViolation || waived;
  const rowClassNames = classnames('iq-policy-violation-row', {
    'iq-policy-violation-row--remediated': isRemediated,
  });

  const setPolicyViolationIdToShow = () => {
    setSelectedPolicyViolationId(policyViolationId);
    toggleShowViolationsDetailPopover();
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
        <div className="iq-policy-violation-row__policy-name-and-reachability">
          <span>{policyName}</span>
          <ReachabilityStatus reachabilityStatus={reachabilityStatus} />
        </div>
        {renderActionsAsList(actions)}
      </NxTableCell>
      <NxTableCell>{firstConstraint ? firstConstraint.constraintName : null}</NxTableCell>
      <NxTableCell>
        {reasons?.map((reason, index) => {
          return <p key={index}>{reason}</p>;
        })}
      </NxTableCell>
      <NxTableCell className="iq-policy-violation-row__actions-and-indicators-cell">
        <LegacyViolationsAndWaiverIndicators violation={violation} isAutoWaiversEnabled={isAutoWaiversEnabled} />
      </NxTableCell>
      <NxTableCell chevron />
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
  legacyViolation: PropTypes.bool,
  waived: PropTypes.bool,
  applicableWaivers: PropTypes.arrayOf(PropTypes.string),
};

PolicyViolationsTableRow.propTypes = {
  violation: PropTypes.shape(violationPropTypes),
  toggleShowViolationsDetailPopover: PropTypes.func,
  setSelectedPolicyViolationId: PropTypes.func.isRequired,
};

/* Helper component for legacy violations and waiver indicators. */
const LegacyViolationsAndWaiverIndicators = ({ violation, isAutoWaiversEnabled }) => {
  const { waived, legacyViolation, applicableWaivers, waivedWithAutoWaiver = [] } = violation;
  const numberOfWaivers = applicableWaivers?.length || 0;

  const legacyViolationIndicator = legacyViolation ? (
    <div className="iq-waiver-indicator">
      <NxFontAwesomeIcon icon={faHistory} />
      <span>Legacy</span>
    </div>
  ) : null;

  return (
    <Fragment>
      {waivedWithAutoWaiver && isAutoWaiversEnabled && (
        <div className="iq-waiver-indicator">
          <NxSmallTag color="green" className="iq-waiver-indicator-auto-tag">
            Auto
          </NxSmallTag>
        </div>
      )}
      {numberOfWaivers > 0 && (
        <ActiveWaiversIndicator activeWaiverCount={numberOfWaivers} waived={waived} showUnapplied />
      )}
      {legacyViolationIndicator}
    </Fragment>
  );
};

PolicyViolationsTableRow.indicators = LegacyViolationsAndWaiverIndicators;
LegacyViolationsAndWaiverIndicators.propTypes = { violation: PropTypes.shape(violationPropTypes) };
