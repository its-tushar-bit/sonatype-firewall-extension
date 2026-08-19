/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
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
import Reachability from 'MainRoot/components/reachability/Reachability';
import moment from 'moment/moment';

const ACTION_ICON_CATEGORY = {
  fail: 'critical',
  warn: 'severe',
};

export default function PolicyViolationsTableRow({
  violation,
  toggleShowViolationsDetailPopover,
  setSelectedPolicyViolationId,
  isAutoWaiversEnabled,
  waivers,
  isLegalTab,
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
  const [telemetryClass, setTelemetryClass] = React.useState('');

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
    <NxTableRow
      className={classnames('iq-policy-violation-row', telemetryClass)}
      isClickable
      onClick={setPolicyViolationIdToShow}
    >
      <NxTableCell className={classnames({ disabled: isRemediated })}>
        <NxThreatIndicator policyThreatLevel={policyThreatLevel} />
        <span className="nx-threat-number">{policyThreatLevel}</span>
      </NxTableCell>
      <NxTableCell className="iq-policy-violation-row__policy-name-and-action-cell">
        <div className="iq-policy-violation-row__policy-name-and-reachability">
          <span>{policyName}</span>
          {isLegalTab && <ReachabilityStatus reachabilityStatus={reachabilityStatus} />}
        </div>
        {renderActionsAsList(actions)}
      </NxTableCell>
      <NxTableCell>{firstConstraint ? firstConstraint.constraintName : null}</NxTableCell>
      <NxTableCell>
        {reasons?.map((reason, index) => {
          return <p key={index}>{reason}</p>;
        })}
      </NxTableCell>
      {!isLegalTab && (
        <NxTableCell className="iq-policy-violation-row__actions-and-indicators-cell iq-policy-violation-cell">
          <Reachability reachable={reachabilityStatus} />
        </NxTableCell>
      )}
      <NxTableCell className="iq-policy-violation-row__actions-and-indicators-cell iq-policy-violation-cell">
        <WaiverStatus
          violation={violation}
          isAutoWaiversEnabled={isAutoWaiversEnabled}
          waivers={waivers}
          setTelemetryClass={setTelemetryClass}
        />
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
  waivedWithAutoWaiver: PropTypes.bool,
  expiredWaivers: PropTypes.arrayOf(
    PropTypes.shape({
      expiryTime: PropTypes.number,
    })
  ),
};

PolicyViolationsTableRow.propTypes = {
  violation: PropTypes.shape(violationPropTypes),
  toggleShowViolationsDetailPopover: PropTypes.func,
  setSelectedPolicyViolationId: PropTypes.func.isRequired,
  isAutoWaiversEnabled: PropTypes.bool,
  waivers: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      expiryTime: PropTypes.number,
    })
  ),
  isLegalTab: PropTypes.bool,
};

const WaiverStatus = ({ violation, isAutoWaiversEnabled, waivers, setTelemetryClass }) => {
  const { waived, legacyViolation, applicableWaivers, waivedWithAutoWaiver = false } = violation;
  const activeWaivers = applicableWaivers?.length || 0;

  useEffect(() => {
    const determineTelemetryClass = () => {
      if (waivedWithAutoWaiver && isAutoWaiversEnabled) {
        return 'iq-policy-violation-row--auto';
      }
      if (legacyViolation) {
        return 'iq-policy-violation-row--legacy';
      }
      if (activeWaivers === 0) {
        return isEarliestWaiverExpirationInRange() ? 'iq-policy-violation-row--expired' : '';
      }
      if (activeWaivers > 0 && waived) {
        const furthestExpiringWaiverDays = getFurthestExpiringWaiverDays(waivers);
        return 0 <= furthestExpiringWaiverDays && furthestExpiringWaiverDays < 10
          ? 'iq-policy-violation-row--expiring'
          : 'iq-policy-violation-row--remediated';
      }
      return '';
    };

    setTelemetryClass(determineTelemetryClass());
  }, [waivedWithAutoWaiver, isAutoWaiversEnabled, legacyViolation, activeWaivers, waived, waivers]);

  /**
   * Calculate the days between today and the given timestamp.
   * Values less than 1 are returned as decimal for a more precise calculation.
   * Values greater than 1 are rounded up to the next whole number.
   * Negative results indicate that the given timestamp is in the past.
   * @param timestamp Given timestamp
   * @returns {number|number}
   */
  const getDaysBetweenTodayAndTimestamp = (timestamp) => {
    const today = moment().startOf('day'); // today at 00:00:00
    const target = moment(timestamp).startOf('day'); // timestamp date at 00:00:00

    return target.diff(today, 'days');
  };

  /**
   * Check if any of the expired waivers in the last 9 days range.
   * @returns {boolean}
   */
  const isEarliestWaiverExpirationInRange = () => {
    if (!violation.expiredWaivers) {
      return false;
    }

    for (const expiredWaiver of violation.expiredWaivers) {
      const daysBetween = getDaysBetweenTodayAndTimestamp(expiredWaiver.expiryTime);

      if (-10 < daysBetween && daysBetween <= 0) {
        return true;
      }
    }

    return false;
  };

  /**
   * Get the furthest expiring waiver days.
   * If any expiring waiver is above the range then that value is returned.
   * @param waivers
   * @returns {number}
   */
  const getFurthestExpiringWaiverDays = (waivers) => {
    if (!violation.applicableWaivers || !waivers) {
      return -1;
    }

    let furthestExpiringWaiverDays = -1;

    for (const applicableWaiverId of violation.applicableWaivers) {
      const foundWaiver = waivers.find((waiver) => waiver.id === applicableWaiverId);

      if (!foundWaiver) {
        continue;
      } else if (!foundWaiver.expiryTime) {
        return -1;
      }

      const daysBetween = getDaysBetweenTodayAndTimestamp(foundWaiver.expiryTime);

      if (daysBetween >= 10) {
        return -1;
      } else if (daysBetween > furthestExpiringWaiverDays) {
        furthestExpiringWaiverDays = daysBetween;
      }
    }

    return furthestExpiringWaiverDays;
  };

  if (waivedWithAutoWaiver && isAutoWaiversEnabled) {
    return (
      <div className="iq-waiver-indicator iq-policy-violation-status">
        <NxSmallTag color="green" className="iq-waiver-indicator-auto-tag">
          Auto
        </NxSmallTag>
      </div>
    );
  } else if (legacyViolation) {
    return (
      <div className="iq-waiver-indicator iq-policy-violation-status">
        <NxFontAwesomeIcon icon={faHistory} />
        <span>Legacy</span>
      </div>
    );
  } else if (activeWaivers === 0) {
    const isWaiverExpirationInRange = isEarliestWaiverExpirationInRange();

    return (
      <div>
        <span>Open</span>
        {isWaiverExpirationInRange && <div className="expires-in">Waiver expired</div>}
      </div>
    );
  } else if (activeWaivers > 0 && waived) {
    const furthestExpiringWaiverDays = getFurthestExpiringWaiverDays(waivers);

    return (
      <div>
        Waived
        <div className="expires-in">
          {0 === furthestExpiringWaiverDays && 'Expires today'}
          {furthestExpiringWaiverDays === 1 && 'Expires in 1 day'}
          {1 < furthestExpiringWaiverDays &&
            furthestExpiringWaiverDays < 10 &&
            `Expires in ${furthestExpiringWaiverDays} days`}
        </div>
      </div>
    );
  } else if (activeWaivers > 0 && !waived) {
    return (
      <ActiveWaiversIndicator activeWaiverCount={activeWaivers} waived={waived} showUnapplied isPolicyViolationStatus />
    );
  }

  return <></>;
};

WaiverStatus.propTypes = {
  violation: PropTypes.shape(violationPropTypes),
  isAutoWaiversEnabled: PropTypes.bool,
  waivers: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      expiryTime: PropTypes.number,
    })
  ),
  setTelemetryClass: PropTypes.func,
};
