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
import {
  NxStatefulSegmentedButton,
  NxButton,
  NxFontAwesomeIcon,
  NxTooltip,
  NxTextLink,
} from '@sonatype/react-shared-components';
import { faEye } from '@fortawesome/pro-solid-svg-icons';

import ViolationExclamation from '../react/ViolationExclamation';
import { timeAgo } from '../utilAngular/CommonServices';
import { capitalizeFirstLetter } from '../util/jsUtil';
import { getOwnerImageUrl } from '../utilAngular/CLMContextLocation';
import ViolationDetailsSubtitle from './ViolationDetailsSubtitle';
import StageDisplay from './StageDisplay';
import ActiveWaiversIndicator from './ActiveWaiversIndicator';

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
      goToWaivers,
      selectedViolationId,
      isFromPolicyViolations,
      isFirewallContext,
      policyDetail,
      selectPolicyId,
      onGoToRepositoryComponentWaiversPage,
      hasPermissionForAppWaivers,
    } = props,
    applicationPublicId = isFirewallContext ? null : violationDetails.applicationPublicId,
    policyName = isFirewallContext ? policyDetail.policyName : violationDetails.policyName,
    policyExists = isFirewallContext ? policyDetail.policyOwner.ownerId : !!violationDetails.policyOwner.ownerId,
    threatLevel = isFirewallContext ? policyDetail.policyThreatLevel : violationDetails.threatLevel,
    selectedId = isFirewallContext ? selectPolicyId : selectedViolationId,
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
    onManageWaiversClick = () => {
      if (isFromPolicyViolations) {
        isFirewallContext ? onGoToRepositoryComponentWaiversPage(selectedId) : goToWaivers(selectedId);
      } else {
        stateGo('listWaivers', {
          violationId: selectedId,
          type: $state.params.type,
          sidebarReference: $state.params.sidebarReference,
        });
      }
    },
    // Manage waivers buttons
    redirectToAddWaiverPage = () => {
      if (hasPermissionForAppWaivers) {
        stateGo('addWaiver', { violationId: selectedViolationId });
      }
    },
    redirectToRequestWaiverPage = () => stateGo('requestWaiver', { violationId: selectedViolationId }),
    manageWaiversButton = isFirewallContext ? (
      <NxButton id="violation-page-add-waiver" variant="tertiary" onClick={onManageWaiversClick}>
        <NxFontAwesomeIcon icon={faEye} />
        <span>Manage Waivers</span>
      </NxButton>
    ) : (
      <NxStatefulSegmentedButton
        id="violation-page-manage-waivers"
        variant="tertiary"
        onClick={onManageWaiversClick}
        buttonContent="Manage Waivers"
      >
        <div>
          <NxTooltip title={hasPermissionForAppWaivers ? '' : 'Insufficient permissions to Add Waiver'} placement="top">
            <button
              id="violation-page-add-waiver"
              className={classnames('nx-dropdown-button', { disabled: !hasPermissionForAppWaivers })}
              onClick={redirectToAddWaiverPage}
            >
              Add Waiver
            </button>
          </NxTooltip>
        </div>
        <button id="violation-page-request-waiver" className="nx-dropdown-button" onClick={redirectToRequestWaiverPage}>
          Request Waiver
        </button>
      </NxStatefulSegmentedButton>
    );

  function getOwnerHref(owner) {
    const ownerIdType = ownerIdTypeMap[owner.ownerType],
      ownerId = owner.ownerPublicId || owner.ownerId;
    return $state.href($state.get(`management.view.${owner.ownerType}`), {
      [ownerIdType]: ownerId,
    });
  }

  const secondFormGroupClasses =
    'nx-form-group iq-read-only iq-read-only-data--horizontal nx-grid-col iq-violation-details__right-details';

  const headerMainTitle = () => {
    const titleClassnames = classnames('nx-tile-header__title', {
      'nx-tile-header__title--disabled': !policyExists,
    });
    let titleThreatLevelCategory,
      nonExistingPolicyText,
      violationNameText = (
        <span>
          Violation of <em>{policyName}</em>
        </span>
      );

    if (policyExists) {
      titleThreatLevelCategory = threatLevelCategory;
      nonExistingPolicyText = null;
    } else {
      titleThreatLevelCategory = 'disabled';
      violationNameText = <strike>{violationNameText}</strike>;
      nonExistingPolicyText = <span>Policy no longer exists</span>;
    }

    return (
      <div className={titleClassnames}>
        <h2 className="nx-h2">
          <ViolationExclamation threatLevelCategory={titleThreatLevelCategory} />
          {violationNameText}
        </h2>
        {nonExistingPolicyText}
      </div>
    );
  };

  return (
    <section id="violation-details-tile" className="nx-tile iq-violation-details">
      <header
        className={classnames('nx-tile-header', {
          'nx-tile-header--disabled': !policyExists,
        })}
      >
        {headerMainTitle()}
        {!isFirewallContext && <ViolationDetailsSubtitle {...violationDetails} />}
        {policyExists && (
          <Fragment>
            <div className="nx-tile__actions">{manageWaiversButton}</div>
            <ActiveWaiversIndicator
              activeWaiverCount={activeWaivers.length}
              waived={isFirewallContext ? policyDetail.waived : violationDetails.waived}
              showUnapplied={isFromPolicyViolations}
            />
          </Fragment>
        )}
      </header>
      <div className="nx-tile-content nx-grid-row">
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
        </dl>
        <dl className={secondFormGroupClasses}>
          {isFirewallContext ? null : (
            <div className="iq-violation-details__stages">
              <dt>Stages</dt>
              {map(createStageDisplay, stageDisplayData)}
            </div>
          )}
          <div className="iq-violation-details__policy-owner">
            <dt>Policy Owner</dt>
            <dd className="iq-read-only-data">
              {policyExists ? (
                <Fragment>
                  <img
                    className="iq-violation-details__policy-owner-icon"
                    src={getOwnerImageUrl({
                      publicId: policyOwner.ownerPublicId,
                      id: policyOwner.ownerId,
                    })}
                  />
                  <NxTextLink href={getOwnerHref(policyOwner)}>{policyOwner.ownerName}</NxTextLink>
                </Fragment>
              ) : (
                'Policy no longer exists'
              )}
            </dd>
          </div>
        </dl>
      </div>
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
  goToWaivers: PropTypes.func.isRequired,
  isFromPolicyViolations: PropTypes.bool,
  isFirewallContext: PropTypes.bool,
  policyDetail: PropTypes.object,
  selectPolicyId: PropTypes.string,
  onGoToRepositoryComponentWaiversPage: PropTypes.func.isRequired,
  hasPermissionForAppWaivers: PropTypes.bool,
};
