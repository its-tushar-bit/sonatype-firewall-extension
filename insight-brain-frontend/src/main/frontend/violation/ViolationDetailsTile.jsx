/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { compose, keys, map, max, prop, reduce, values } from 'ramda';
import classnames from 'classnames';
import { categoryByPolicyThreatLevel } from '@sonatype/react-shared-components/util/threatLevels';

import ViolationExclamation from '../react/ViolationExclamation';
import { timeAgo } from '../util/CommonServices';
import { capitalize } from '../util/jsUtil';
import { getOwnerImageUrl } from '../util/CLMContextLocation';
import ViolationDetailsSubtitle from './ViolationDetailsSubtitle';
import StageDisplay from './StageDisplay';

const ownerIdTypeMap = {
  application: 'applicationPublicId',
  organization: 'organizationId'
};

export default function ViolationDetailsTile({ $state, violationDetails, stageTypes }) {
  const { applicationPublicId, policyName, threatLevel, policyOwner, stageData } = violationDetails,
      { ownerName, ownerType } = policyOwner,
      ownerId = policyOwner.ownerPublicId || policyOwner.ownerId,

      threatLevelCategory = categoryByPolicyThreatLevel[threatLevel],
      threatLevelClassName = classnames('iq-threat-level', `iq-threat-level--${threatLevelCategory}`),

      parseISODate = (time) => new Date(time),
      openTime = timeAgo(parseISODate(violationDetails.openTime)),

      parseRecentEvaluationTimes = compose(parseISODate, prop('mostRecentEvaluationTime')),
      mostRecentEvaluationTimes = map(parseRecentEvaluationTimes, values(violationDetails.stageData)),
      mostRecentEvaluationTimestamp = reduce(max, 0, mostRecentEvaluationTimes),
      mostRecentEvaluationTime = timeAgo(mostRecentEvaluationTimestamp),

      ownerIdType = ownerIdTypeMap[ownerType],
      ownerHref = $state.href($state.get(`management.view.${ownerType}`), { [ownerIdType]: ownerId }),
      ownerImageUrl = getOwnerImageUrl({
        publicId: policyOwner.ownerPublicId,
        id: policyOwner.ownerId
      }),

      // pair each possible stage type with its respective (optional) data from the backend
      stageDisplayData = map(stageType => [stageType, stageData[stageType.stageTypeId]], stageTypes),
      createStageDisplay = ([stageType, stageData]) => (
        <dd key={stageType.stageTypeId}>
          <StageDisplay { ...({ $state, stageType, stageData, applicationPublicId }) } />
        </dd>
      );

  return (
    <div id="violation-details-tile" className="nx-tile iq-violation-details">
      <div className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">
            <ViolationExclamation threatLevelCategory={threatLevelCategory} />
            <span>Violation of <em>{policyName}</em></span>
          </h2>
        </div>
        <ViolationDetailsSubtitle { ...violationDetails } />
      </div>
      <div className="nx-tile-content nx-grid-row">
        <dl className="iq-read-only nx-grid-col nx-grid-col--33 iq-violation-details__left-details">
          <div className="iq-violation-details__threat-level">
            <dt>Threat Level</dt>
            <dd className={threatLevelClassName}>{threatLevel}</dd>
          </div>
          <div className="iq-violation-details__policy-type">
            <dt>Policy Type</dt>
            <dd>{capitalize(violationDetails.policyThreatCategory)}</dd>
          </div>
          <div className="iq-violation-details__first-reported">
            <dt>First Reported</dt>
            <dd>{openTime.age} {openTime.qualifier}</dd>
          </div>
          <div className="iq-violation-details__last-reported">
            <dt>Last Reported</dt>
            <dd>{mostRecentEvaluationTime.age} {mostRecentEvaluationTime.qualifier}</dd>
          </div>
        </dl>
        <dl className="iq-read-only iq-read-only-data--horizontal nx-grid-col iq-violation-details__right-details">
          <div className="iq-violation-details__stages">
            <dt>Stages</dt>
            { map(createStageDisplay, stageDisplayData) }
          </div>
          <div className="iq-violation-details__policy-owner">
            <dt>Policy Owner</dt>
            <dd>
              <img className="iq-violation-details__policy-owner-icon" src={ownerImageUrl} />
              <a href={ownerHref}>{ownerName}</a>
            </dd>
          </div>
        </dl>
      </div>
    </div>
  );
}

export const violationDetailsPropTypes = {
  policyName: PropTypes.string.isRequired,
  policyThreatCategory: PropTypes.string.isRequired,
  policyOwner: PropTypes.shape({
    ownerName: PropTypes.string.isRequired,
    ownerType: PropTypes.oneOf(keys(ownerIdTypeMap)).isRequired,
    ownerId: PropTypes.string.isRequired,
    ownerPublicId: PropTypes.string
  }).isRequired,
  threatLevel: PropTypes.number.isRequired,
  openTime: PropTypes.string.isRequired,
  stageData: PropTypes.objectOf(StageDisplay.propTypes.stageData.isRequired).isRequired,
  applicationPublicId: PropTypes.string.isRequired,
  organizationName: PropTypes.string.isRequired,
  applicationName: PropTypes.string.isRequired,
  displayName: PropTypes.object,
  filenames: PropTypes.array
};

ViolationDetailsTile.propTypes = {
  $state: PropTypes.shape({
    get: PropTypes.func.isRequired,
    href: PropTypes.func.isRequired
  }).isRequired,
  violationDetails: PropTypes.shape(violationDetailsPropTypes),
  stageTypes: PropTypes.arrayOf(PropTypes.shape({
    stageTypeId: PropTypes.string.isRequired,
    shortName: PropTypes.string.isRequired
  }).isRequired)
};
