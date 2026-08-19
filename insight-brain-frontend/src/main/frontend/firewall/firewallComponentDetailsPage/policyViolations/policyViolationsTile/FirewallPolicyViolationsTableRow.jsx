/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { NxTableCell, NxTableRow, NxThreatIndicator, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faExclamationCircle, faExclamationTriangle } from '@fortawesome/free-solid-svg-icons';
import FirewallPolicyViolationsWaiverIndicators from './FirewallPolicyViolationsWaiverIndicators';
import { actions } from 'MainRoot/componentDetails/ViolationsTableTile/policyViolationsSlice';
import { useDispatch } from 'react-redux';

export default function FirewallPolicyViolationsTableRow({ violation, showProxyState = false }) {
  const dispatch = useDispatch();
  const { policyThreatLevel, policyName, constraints, policyViolationId, waived } = violation;
  const [constraintFacts] = constraints;
  const conditionFacts = [...constraintFacts.conditions];
  const PROXY_FAILING_FLAG = 'fail';
  const PROXY_WARNING_FLAG = 'warn';

  const isRemediated = waived;
  const rowClassNames = classnames('iq-policy-violation-row', {
    'iq-policy-violation-row--remediated': isRemediated,
  });
  const setViolationsDetailRowClicked = actions.setViolationsDetailRowClicked;
  const toggleShowViolationsDetailPopover = actions.toggleShowViolationsDetailPopover();
  const setSelectPolicyViolation = actions.setSelectedPolicyViolation;
  const setPolicyViolationIdToShow = () => {
    dispatch(setViolationsDetailRowClicked());
    dispatch(toggleShowViolationsDetailPopover);
    dispatch(setSelectPolicyViolation({ policyViolationId, policyName, policyThreatLevel }));
  };

  return (
    <NxTableRow className={rowClassNames} isClickable onClick={setPolicyViolationIdToShow}>
      <NxTableCell className={classnames({ disabled: isRemediated })}>
        <NxThreatIndicator policyThreatLevel={policyThreatLevel} />
        <span className="nx-threat-number">{policyThreatLevel}</span>
      </NxTableCell>
      <NxTableCell className="iq-policy-violation-row__policy-name-and-action-cell">
        <span>{policyName}</span>
        {showProxyState && violation.policyActionTypeId === PROXY_WARNING_FLAG && (
          <span className="iq-policy-violation-row__proxy-state-flag">
            <NxFontAwesomeIcon
              icon={faExclamationTriangle}
              className="iq-policy-violation-row__proxy-state-warning-icon"
            />
            Proxy Warning
          </span>
        )}
        {showProxyState && violation.policyActionTypeId === PROXY_FAILING_FLAG && (
          <span className="iq-policy-violation-row__proxy-state-flag">
            <NxFontAwesomeIcon
              icon={faExclamationCircle}
              className="iq-policy-violation-row__proxy-state-failing-icon"
            />
            Proxy Failing
          </span>
        )}
      </NxTableCell>
      <NxTableCell>{constraintFacts.constraintName}</NxTableCell>
      <NxTableCell>
        {conditionFacts.map((conditionFact, index) => (
          <p key={index}>{conditionFact.conditionReason}</p>
        ))}
      </NxTableCell>
      <NxTableCell className="iq-policy-violation-row__actions-and-indicators-cell">
        <FirewallPolicyViolationsWaiverIndicators violation={violation} />
      </NxTableCell>
      <NxTableCell chevron />
    </NxTableRow>
  );
}

FirewallPolicyViolationsTableRow.propTypes = {
  violation: PropTypes.shape({
    policyViolationId: PropTypes.string.isRequired,
    policyId: PropTypes.string.isRequired,
    policyName: PropTypes.string.isRequired,
    policyThreatLevel: PropTypes.number.isRequired,
    policyThreatCategory: PropTypes.string.isRequired,
    waived: PropTypes.bool.isRequired,
    constraints: PropTypes.arrayOf(
      PropTypes.shape({
        constraintId: PropTypes.string.isRequired,
        constraintName: PropTypes.string.isRequired,
        constraintOperator: PropTypes.string.isRequired,
        conditions: PropTypes.arrayOf(
          PropTypes.shape({
            conditionType: PropTypes.string.isRequired,
            conditionSummary: PropTypes.string.isRequired,
            conditionReason: PropTypes.string.isRequired,
            conditionTriggerReference: PropTypes.object,
          })
        ),
      })
    ),
    constraintFactsJson: PropTypes.string.isRequired,
    policyActionTypeId: PropTypes.string,
  }),
  showProxyState: PropTypes.bool,
};
