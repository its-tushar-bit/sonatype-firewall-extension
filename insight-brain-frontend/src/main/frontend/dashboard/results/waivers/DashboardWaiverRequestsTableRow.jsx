/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import moment from 'moment';
import { capitalizeFirstLetter } from 'MainRoot/util/jsUtil';
import ComponentDisplay from 'MainRoot/ComponentDisplay/ReactComponentDisplay';
import { NxTable, NxThreatIndicator, NxOverflowTooltip } from '@sonatype/react-shared-components';
import { isWaiverAllVersionsOrExact } from 'MainRoot/util/waiverUtils';
import { useDispatch } from 'react-redux';
import { checkPermissions } from 'MainRoot/util/authorizationUtil';

export default function DashboardWaiverRequestsTableRow({ stateGo, waiverRequest }) {
  const {
    id: waiverRequestId,
    threatLevel,
    requestTime,
    requesterName,
    policyName,
    ownerId,
    ownerType,
    scope,
    componentMatchStrategy,
    status,
  } = waiverRequest;

  const dispatch = useDispatch();

  const reviewWaiverRequest = () => {
    // Create common params object once to avoid duplication
    const commonParams = {
      //If response's ownerType is 'root_organization', change it to a supported 'organization' path param
      ownerType: ownerType === 'root_organization' ? 'organization' : ownerType,
      ownerId,
      policyWaiverRequestId: waiverRequestId,
    };

    const isFirewallRequest =
      ownerType === 'repository' ||
      ownerType === 'repository_manager' ||
      ownerType === 'repository_container' ||
      ownerType === 'all_repositories';

    if (isFirewallRequest) {
      // 'all_repositories' is the display alias used in responses, but the API path requires 'repository_container'
      const apiOwnerType =
        ownerType === 'all_repositories' || ownerId === 'REPOSITORY_CONTAINER_ID'
          ? 'repository_container'
          : ownerType;
      dispatch(
        stateGo('dashboardFirewallWaiverRequestReview', {
          ownerType: apiOwnerType,
          ownerId,
          waiverRequestId: waiverRequestId,
          origin: 'dashboard.overview.waiverRequests',
        })
      );
      return;
    }

    const permissionCheck =
      ownerType === 'root_organization'
        ? checkPermissions(['WAIVE_POLICY_VIOLATIONS'])
        : checkPermissions(['WAIVE_POLICY_VIOLATIONS'], ownerType, ownerId);

    permissionCheck
      .then(() => {
        dispatch(stateGo('requestWaiverReview', commonParams));
      })
      .catch(() => {
        dispatch(stateGo('requestWaiverUpdate', commonParams));
      });
  };

  const requestTimeFormated = moment(requestTime).format('YYYY-MM-DD');

  return (
    <NxTable.Row
      key={waiverRequestId}
      onClick={reviewWaiverRequest}
      className="iq-dashboard-waiver-request"
      isClickable
    >
      <NxTable.Cell className="iq-threat-cell">
        <NxThreatIndicator policyThreatLevel={threatLevel} />
        <span className="nx-threat-number">{threatLevel}</span>
      </NxTable.Cell>
      <NxTable.Cell>
        <NxOverflowTooltip>
          <div className="nx-truncate-ellipsis">{requestTimeFormated}</div>
        </NxOverflowTooltip>
      </NxTable.Cell>
      <NxTable.Cell>
        <NxOverflowTooltip>
          <div>{requesterName}</div>
        </NxOverflowTooltip>
      </NxTable.Cell>
      <NxTable.Cell>
        <NxOverflowTooltip>
          <div>{policyName || <span>{'—'}</span>}</div>
        </NxOverflowTooltip>
      </NxTable.Cell>
      <NxTable.Cell>
        <NxOverflowTooltip>
          <div className="nx-truncate-ellipsis">{scope}</div>
        </NxOverflowTooltip>
      </NxTable.Cell>
      <NxTable.Cell>
        {waiverRequest.componentIdentifier === null ? (
          <span>{'—'}</span>
        ) : isWaiverAllVersionsOrExact(waiverRequest) ? (
          <ComponentDisplay component={waiverRequest} truncate={true} matcherStrategy={componentMatchStrategy} />
        ) : (
          'All Components'
        )}
      </NxTable.Cell>
      <NxTable.Cell>
        <div>{capitalizeFirstLetter(status)}</div>
      </NxTable.Cell>
      <NxTable.Cell chevron />
    </NxTable.Row>
  );
}

export const waiverRequestPropTypes = PropTypes.shape({
  id: PropTypes.string.isRequired,
  threatLevel: PropTypes.number.isRequired,
  requestTime: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  requesterName: PropTypes.string,
  policyName: PropTypes.string,
  ownerId: PropTypes.string.isRequired,
  ownerName: PropTypes.string,
  ownerType: PropTypes.string.isRequired,
  scope: PropTypes.string.isRequired,
  componentMatchStrategy: PropTypes.string,
  componentIdentifier: PropTypes.object,
  status: PropTypes.string.isRequired,
});

DashboardWaiverRequestsTableRow.propTypes = {
  stateGo: PropTypes.func.isRequired,
  waiverRequest: waiverRequestPropTypes,
};
