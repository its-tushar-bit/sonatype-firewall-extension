/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxTableCell, NxTableRow, NxThreatIndicator, NxOverflowTooltip } from '@sonatype/react-shared-components';
import { useDispatch } from 'react-redux';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import FirewallWaiverRequestStatusBadge from './FirewallWaiverRequestStatusBadge';

export const waiverRequestPropType = PropTypes.shape({
  policyWaiverRequestId: PropTypes.string.isRequired,
  scopeOwnerType: PropTypes.string,
  scopeOwnerId: PropTypes.string,
  scopeOwnerName: PropTypes.string,
  requesterName: PropTypes.string,
  componentIdentifier: PropTypes.object,
  policyName: PropTypes.string,
  threatLevel: PropTypes.number,
  comment: PropTypes.string,
  requestTime: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Date)]),
  status: PropTypes.string.isRequired,
});

function formatDateRequested(value) {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  return date.toLocaleDateString('en-CA'); // YYYY-MM-DD format
}

function formatScopeOwnerType(type) {
  if (!type) {
    return '';
  }
  return type.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
}

function getComponentDisplayName(request) {
  // Use the displayName field (computed by backend from ComponentDisplayNameUtil)
  if (request.displayName?.parts?.length) {
    return request.displayName.parts.map((part) => part.value).join('');
  }
  // Fallback to componentIdentifier coordinates
  if (request.componentIdentifier) {
    const { coordinates } = request.componentIdentifier;
    if (coordinates) {
      const parts = [coordinates.groupId, coordinates.artifactId || coordinates.packageId, coordinates.version]
        .filter(Boolean);
      if (parts.length > 0) {
        return parts.join(':');
      }
    }
  }
  return '';
}

export default function FirewallRequestedWaiversTableRow({ request, repositoryFormat }) {
  const dispatch = useDispatch();
  const {
    policyWaiverRequestId,
    scopeOwnerType,
    scopeOwnerId,
    scopeOwnerName,
    requesterName,
    policyName,
    threatLevel,
    requestTime,
    status,
  } = request;

  const componentDisplayName = getComponentDisplayName(request);
  const scopeLabel = `${formatScopeOwnerType(scopeOwnerType)} - ${scopeOwnerName || scopeOwnerId}`;

  // The backend returns 'all_repositories' as the display alias for repository_container scope,
  // but the API path only accepts 'repository_container'. Map it back before navigating.
  // Also fall back to 'repository_container' when scopeOwnerId is REPOSITORY_CONTAINER_ID.
  const apiOwnerType =
    scopeOwnerType === 'all_repositories' || scopeOwnerId === 'REPOSITORY_CONTAINER_ID'
      ? 'repository_container'
      : scopeOwnerType;

  const origin = repositoryFormat === 'docker'
    ? 'firewall.waivers.containers.requested'
    : 'firewall.waivers.components.requested';

  const handleClick = () => {
    dispatch(
      stateGo('firewall.reviewWaiverRequest', {
        waiverRequestId: policyWaiverRequestId,
        ownerType: apiOwnerType,
        ownerId: scopeOwnerId,
        origin,
      })
    );
  };

  return (
    <NxTableRow isClickable onClick={handleClick} className="iq-requested-waivers-table-row">
      <NxTableCell className="iq-requested-waivers-table-row__threat">
        <NxThreatIndicator policyThreatLevel={threatLevel} />
        <span>{threatLevel}</span>
      </NxTableCell>
      <NxTableCell>{formatDateRequested(requestTime)}</NxTableCell>
      <NxTableCell>{requesterName}</NxTableCell>
      <NxTableCell>{policyName}</NxTableCell>
      <NxTableCell className="iq-requested-waivers-table-row__scope">
        <NxOverflowTooltip>
          <span>{scopeLabel}</span>
        </NxOverflowTooltip>
      </NxTableCell>
      <NxTableCell className="iq-requested-waivers-table-row__component">
        <NxOverflowTooltip>
          <span>{componentDisplayName}</span>
        </NxOverflowTooltip>
      </NxTableCell>
      <NxTableCell>
        <FirewallWaiverRequestStatusBadge status={status} />
      </NxTableCell>
      <NxTableCell chevron />
    </NxTableRow>
  );
}

FirewallRequestedWaiversTableRow.propTypes = {
  request: waiverRequestPropType.isRequired,
  repositoryFormat: PropTypes.string,
};
