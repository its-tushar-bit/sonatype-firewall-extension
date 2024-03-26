/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { compose, keys, map, max, prop, reduce, values } from 'ramda';
import classnames from 'classnames';
import { categoryByPolicyThreatLevel } from '@sonatype/react-shared-components/util/threatLevels';
import { NxButton, NxTextLink } from '@sonatype/react-shared-components';

import ViolationDetailsTileHeaderMainTitle from './ViolationDetailsTileHeaderMainTitle';
import ActiveWaiversIndicator from 'MainRoot/violation/ActiveWaiversIndicator';
import { timeAgo } from '../utilAngular/CommonServices';
import { capitalizeFirstLetter } from '../util/jsUtil';
import ViolationDetailsSubtitle from './ViolationDetailsSubtitle';
import StageDisplay from './StageDisplay';
import PolicyViolationConstraintInfo, { constraintViolationsPropType } from './PolicyViolationConstraintInfo';

const ownerIdTypeMap = {
  application: 'applicationPublicId',
  organization: 'organizationId',
};

export default function ViolationDetailsTile(props) {
  const {
      $state,
      violationDetails,
      stageTypes,
      stateGo,
      activeWaivers,
      selectedViolationId,
      isFromPolicyViolations,
      isFirewallContext,
      policyDetail,
      hasPermissionForAppWaivers,
      constraintViolations,
      isSbomManager,
    } = props,
    applicationPublicId = isFirewallContext ? null : violationDetails.applicationPublicId,
    policyName = isFirewallContext ? policyDetail.policyName : violationDetails.policyName,
    policyExists = isFirewallContext ? policyDetail.policyOwner.ownerId : !!violationDetails.policyOwner.ownerId,
    threatLevel = isFirewallContext ? policyDetail.policyThreatLevel : violationDetails.threatLevel,
    stageData = isFirewallContext
      ? { release: { mostRecentEvaluationTime: policyDetail.lastReported } }
      : violationDetails.stageData,
    policyOwner = isFirewallContext ? policyDetail.policyOwner : violationDetails.policyOwner,
    threatLevelCategory =
      categoryByPolicyThreatLevel[isFirewallContext ? policyDetail.policyThreatLevel : violationDetails.threatLevel],
    threatLevelClassName = classnames(
      'iq-read-only-data',
      'iq-threat-level',
      `iq-threat-level--${threatLevelCategory}`
    ),
    parseISODate = (time) => new Date(time),
    openTime = timeAgo(parseISODate(isFirewallContext ? null : violationDetails.openTime)),
    parseRecentEvaluationTimes = compose(parseISODate, prop('mostRecentEvaluationTime')),
    mostRecentEvaluationTimes = map(
      parseRecentEvaluationTimes,
      values(isFirewallContext ? stageData : violationDetails.stageData)
    ),
    mostRecentEvaluationTimestamp = reduce(max, 0, mostRecentEvaluationTimes),
    mostRecentEvaluationTime = timeAgo(mostRecentEvaluationTimestamp),
    // pair each possible stage type with its respective (optional) data from the backend
    stageDisplayData = map(
      (stageType) => [stageType, stageData[stageType.stageTypeId]],
      isFirewallContext ? '' : stageTypes
    ),
    createStageDisplay = ([stageType, stageData]) => (
      <dd className="iq-read-only-data" key={stageType.stageTypeId}>
        <StageDisplay {...{ $state, stageType, stageData, applicationPublicId }} />
      </dd>
    ),
    // Manage waivers buttons
    redirectToAddWaiverPage = () => {
      if (hasPermissionForAppWaivers) {
        stateGo('addWaiver', { violationId: selectedViolationId });
      }
    },
    redirectToRequestWaiverPage = () => stateGo('requestWaiver', { violationId: selectedViolationId }),
    waiversButton = hasPermissionForAppWaivers ? (
      <NxButton
        id="violation-page-add-waiver"
        variant={activeWaivers.length === 0 ? 'primary' : 'secondary'}
        onClick={redirectToAddWaiverPage}
      >
        <span>Add Waiver</span>
      </NxButton>
    ) : (
      <NxButton
        id="violation-page-request-waiver"
        variant={activeWaivers.length === 0 ? 'primary' : 'secondary'}
        onClick={redirectToRequestWaiverPage}
      >
        <span>Request Waiver</span>
      </NxButton>
    );

  function getOwnerHref(owner) {
    const ownerIdType = ownerIdTypeMap[owner.ownerType],
      ownerId = owner.ownerPublicId || owner.ownerId;
    return $state.href($state.get(`${isSbomManager ? 'sbomManager.' : ''}management.view.${owner.ownerType}`), {
      [ownerIdType]: ownerId,
    });
  }

  const secondFormGroupClasses =
    'nx-form-group iq-read-only iq-read-only-data--horizontal nx-grid-col iq-violation-details__right-details';

  const sectionClasses = classnames('iq-violation-details', {
    'nx-tile': !isFromPolicyViolations,
    'iq-violation-details-popover-section': isFromPolicyViolations,
  });

  const bottomFormGroupClasses = classnames('iq-violation-details__bottom-details', {
    'iq-violation-details__bottom-from-firewall': isFirewallContext,
  });
  return (
    <section id="violation-details-tile" className={sectionClasses}>
      {isFromPolicyViolations ? (
        ''
      ) : (
        <header
          className={classnames({
            'nx-tile-header': !isFromPolicyViolations,
            'header--disabled': !policyExists,
          })}
        >
          <ViolationDetailsTileHeaderMainTitle
            policyExists={policyExists}
            policyName={policyName}
            threatLevelCategory={threatLevelCategory}
          />
          {!isFirewallContext && <ViolationDetailsSubtitle {...violationDetails} />}
          {policyExists && (
            <Fragment>
              <div className="nx-tile__actions">{waiversButton}</div>
              {activeWaivers?.length && hasPermissionForAppWaivers ? (
                <ActiveWaiversIndicator
                  activeWaiverCount={activeWaivers.length}
                  waived={isFirewallContext ? policyDetail?.waived : violationDetails?.waived}
                  showUnapplied={isFromPolicyViolations}
                />
              ) : null}
            </Fragment>
          )}
        </header>
      )}
      <div
        className={classnames('nx-grid-row', {
          'nx-tile-content': !isFromPolicyViolations,
          'iq-violations-details-info': isFromPolicyViolations,
        })}
      >
        <dl className="nx-form-group iq-read-only nx-grid-col iq-violation-details__left-details">
          <div className="iq-violation-details__threat-level">
            <dt>Threat Level</dt>
            <dd className={threatLevelClassName}>{threatLevel}</dd>
          </div>
          <div className="iq-violation-details__policy-type">
            <dt>Policy Type</dt>
            <dd className="iq-read-only-data">
              {capitalizeFirstLetter(
                isFirewallContext ? policyDetail.policyThreatCategory : violationDetails.policyThreatCategory
              )}
            </dd>
          </div>
        </dl>
        <dl className={secondFormGroupClasses}>
          {isFirewallContext ? null : (
            <div className="iq-violation-details__stages">
              <dt>Stages</dt>
              {map(createStageDisplay, stageDisplayData)}
            </div>
          )}
          <div className={bottomFormGroupClasses}>
            <div className="iq-violation-details__policy-owner">
              <dt>Policy Owner</dt>
              <dd className="iq-read-only-data">
                {policyExists ? (
                  <NxTextLink href={getOwnerHref(policyOwner)}>{policyOwner.ownerName}</NxTextLink>
                ) : (
                  'Policy no longer exists'
                )}
              </dd>
            </div>
            <div className={'iq-violation-details__reported'}>
              {isFirewallContext ? null : (
                <div className="iq-violation-details__first-reported">
                  <dt>First Reported</dt>
                  <dd className="iq-read-only-data">
                    {openTime.age} {openTime.qualifier}
                  </dd>
                </div>
              )}
              <div className="iq-violation-details__last-reported">
                <dt>Last Reported</dt>
                <dd className="iq-read-only-data">
                  {mostRecentEvaluationTime.age} {mostRecentEvaluationTime.qualifier}
                </dd>
              </div>
            </div>
          </div>
        </dl>
      </div>
      <PolicyViolationConstraintInfo
        isFirewallContext={isFirewallContext}
        constraintViolations={constraintViolations}
        isFromPolicyViolations={isFromPolicyViolations}
      />
    </section>
  );
}

export const violationDetailsPropTypes = {
  policyViolationId: PropTypes.string.isRequired,
  policyName: PropTypes.string.isRequired,
  policyThreatCategory: PropTypes.string.isRequired,
  policyOwner: PropTypes.shape({
    ownerName: PropTypes.string,
    ownerType: PropTypes.oneOf(keys(ownerIdTypeMap)),
    ownerId: PropTypes.string,
    ownerPublicId: PropTypes.string,
  }).isRequired,
  threatLevel: PropTypes.number.isRequired,
  openTime: PropTypes.string.isRequired,
  stageData: PropTypes.objectOf(StageDisplay.propTypes.stageData.isRequired).isRequired,
  applicationPublicId: PropTypes.string.isRequired,
  organizationName: PropTypes.string.isRequired,
  applicationName: PropTypes.string.isRequired,
  displayName: PropTypes.object,
  filenames: PropTypes.array,
  waived: PropTypes.bool,
};

export const applicableWaiverPropTypes = {
  policyWaiverId: PropTypes.string.isRequired,
  comment: PropTypes.string,
  scopeOwnerType: PropTypes.string.isRequired,
  scopeOwnerId: PropTypes.string.isRequired,
  scopeOwnerName: PropTypes.string.isRequired,
  hash: PropTypes.string,
  policyId: PropTypes.string.isRequired,
};

ViolationDetailsTile.propTypes = {
  $state: PropTypes.shape({
    get: PropTypes.func.isRequired,
    href: PropTypes.func.isRequired,
    params: PropTypes.shape({
      type: PropTypes.string,
      sidebarReference: PropTypes.string,
    }),
  }).isRequired,
  selectedViolationId: PropTypes.string,
  violationDetails: PropTypes.shape(violationDetailsPropTypes),
  stageTypes: PropTypes.arrayOf(
    PropTypes.shape({
      stageTypeId: PropTypes.string.isRequired,
      shortName: PropTypes.string.isRequired,
    }).isRequired
  ),
  stateGo: PropTypes.func.isRequired,
  activeWaivers: PropTypes.arrayOf(PropTypes.shape(applicableWaiverPropTypes)),
  isFromPolicyViolations: PropTypes.bool,
  isFirewallContext: PropTypes.bool,
  policyDetail: PropTypes.object,
  hasPermissionForAppWaivers: PropTypes.bool,
  isSbomManager: PropTypes.bool,
  constraintViolations: constraintViolationsPropType,
};
