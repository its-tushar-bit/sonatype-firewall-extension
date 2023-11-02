/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { faExclamationCircle, faExclamationTriangle, faSquare } from '@fortawesome/free-solid-svg-icons';
import { NxFontAwesomeIcon, NxTextLink } from '@sonatype/react-shared-components';

import { terseAgo } from '../utilAngular/CommonServices';

const iconByActionTypeId = {
  fail: faExclamationCircle,
  warn: faExclamationTriangle,
};

export default function StageDisplay({ $state, stageType, stageData, applicationPublicId }) {
  const displayName = stageType.shortName;

  if (stageData) {
    const { mostRecentEvaluationTime, mostRecentScanId, actionTypeId } = stageData,
      icon = iconByActionTypeId[actionTypeId],
      iconClassName = actionTypeId
        ? `iq-violation-details__stage-action iq-violation-details__stage-action--${actionTypeId}`
        : null,
      href = $state.href($state.get('applicationReport'), {
        publicId: applicationPublicId,
        scanId: mostRecentScanId,
      }),
      displayTime = terseAgo(new Date(mostRecentEvaluationTime));

    return (
      <span className="iq-violation-details__stage">
        {icon && <NxFontAwesomeIcon icon={icon} className={iconClassName} />}
        <NxTextLink href={href}>
          {displayName} {displayTime}
        </NxTextLink>
      </span>
    );
  } else {
    return (
      <span className="iq-violation-details__stage iq-violation-details__stage--unused">
        <NxFontAwesomeIcon icon={faSquare} />
        <span>{displayName}</span>
      </span>
    );
  }
}

StageDisplay.propTypes = {
  $state: PropTypes.shape({
    get: PropTypes.func.isRequired,
    href: PropTypes.func.isRequired,
  }).isRequired,
  stageType: PropTypes.shape({
    shortName: PropTypes.string.isRequired,
  }).isRequired,
  stageData: PropTypes.shape({
    mostRecentEvaluationTime: PropTypes.string.isRequired,
    mostRecentScanId: PropTypes.string.isRequired,
    actionTypeId: PropTypes.oneOf(['fail', 'warn', null]),
  }),
  applicationPublicId: PropTypes.string.isRequired,
};
