/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { categoryByPolicyThreatLevel } from '@sonatype/react-shared-components/util/threatLevels';
import { NxH3, NxTextLink } from '@sonatype/react-shared-components';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { timeAgo } from 'MainRoot/util/CommonServices';
import { capitalizeFirstLetter } from 'MainRoot/util/jsUtil';
import PolicyViolationConstraintInfo from 'MainRoot/violation/PolicyViolationConstraintInfo';
import ReachabilityStatus from 'MainRoot/componentDetails/ReachabilityStatus/ReachabilityStatus';

const ownerIdTypeMap = {
  application: 'applicationPublicId',
  organization: 'organizationId',
};

export default function FirewallViolationDetailsTile({
  policyDetail,
  violationDetails,
  isFromPolicyViolations = true,
  isSbomManager = false,
}) {
  const $state = useRouterState();

  if (!policyDetail) {
    return null;
  }

  const policyOwner = policyDetail?.policyOwner || violationDetails?.policyOwner;
  const policyExists = !!policyOwner?.ownerId;
  const threatLevel = policyDetail?.policyThreatLevel ?? violationDetails?.threatLevel;
  const policyThreatCategory = policyDetail?.policyThreatCategory || violationDetails?.policyThreatCategory;
  const threatLevelCategory = categoryByPolicyThreatLevel[threatLevel];
  const threatLevelClassName = classnames(
    'iq-read-only-data',
    'iq-threat-level',
    `iq-threat-level--${threatLevelCategory}`
  );
  const sectionClasses = classnames('iq-violation-details', {
    'nx-tile': !isFromPolicyViolations,
    'iq-violation-details-popover-section': isFromPolicyViolations,
  });
  const bottomFormGroupClasses = classnames('iq-violation-details__bottom-details', {
    'iq-violation-details__bottom-from-firewall': true,
  });
  const lastReportedValue = policyDetail?.lastReported || violationDetails?.openTime;
  const lastReported = lastReportedValue ? timeAgo(new Date(lastReportedValue)) : null;
  const reachabilityStatus = violationDetails?.reachabilityStatus;

  function getOwnerHref(owner) {
    const ownerIdType = ownerIdTypeMap[owner.ownerType];
    const ownerId = owner.ownerPublicId || owner.ownerId;

    return $state.href($state.get(`${isSbomManager ? 'sbomManager.' : ''}management.view.${owner.ownerType}`), {
      [ownerIdType]: ownerId,
    });
  }

  return (
    <section id="firewall-violation-details-tile" className={sectionClasses}>
      <div
        className={classnames('nx-grid-row', {
          'nx-tile-content': !isFromPolicyViolations,
          'iq-violations-details-info': isFromPolicyViolations,
        })}
      >
        <dl className="nx-form-group iq-read-only nx-grid-col iq-violation-details__left-details">
          <div className="iq-violation-details__threat-level">
            <dt id="iq-violation-details__threat-level">Threat Level</dt>
            <dd aria-labelledby="iq-violation-details__threat-level" className={threatLevelClassName}>
              {threatLevel}
            </dd>
          </div>
          <div className="iq-violation-details__policy-type">
            <dt id="iq-violation-details__policy-type">Policy Type</dt>
            <dd aria-labelledby="iq-violation-details__policy-type" className="iq-read-only-data">
              {capitalizeFirstLetter(policyThreatCategory)}
            </dd>
          </div>
        </dl>
        <dl className="nx-form-group iq-read-only iq-read-only-data--horizontal nx-grid-col iq-violation-details__right-details">
          <div className={bottomFormGroupClasses}>
            <div className="iq-violation-details__policy-owner">
              <dt id="iq-violation-details__policy-owner">Policy Owner</dt>
              <dd aria-labelledby="iq-violation-details__policy-owner" className="iq-read-only-data">
                {policyExists ? (
                  <NxTextLink href={getOwnerHref(policyOwner)}>{policyOwner.ownerName}</NxTextLink>
                ) : (
                  'Policy no longer exists'
                )}
              </dd>
            </div>
            <div className="iq-violation-details__reported">
              <div className="iq-violation-details__last-reported">
                <dt id="iq-violation-details__last-reported">Last Reported</dt>
                <dd aria-labelledby="iq-violation-details__last-reported" className="iq-read-only-data">
                  {lastReported ? `${lastReported.age} ${lastReported.qualifier}` : '--'}
                </dd>
              </div>
            </div>
          </div>
        </dl>
      </div>
      <PolicyViolationConstraintInfo
        isFirewallContext
        constraintViolations={policyDetail?.constraints || []}
        isFromPolicyViolations={isFromPolicyViolations}
      />
      {reachabilityStatus && (
        <div className="iq-violation-details__reachability">
          <NxH3>Reachability Analysis</NxH3>
          <ReachabilityStatus reachabilityStatus={reachabilityStatus} />
        </div>
      )}
    </section>
  );
}

FirewallViolationDetailsTile.propTypes = {
  policyDetail: PropTypes.object,
  violationDetails: PropTypes.object,
  isFromPolicyViolations: PropTypes.bool,
  isSbomManager: PropTypes.bool,
};
