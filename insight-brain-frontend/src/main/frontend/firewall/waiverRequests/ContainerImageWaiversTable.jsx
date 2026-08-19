/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import moment from 'moment';
import {
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxThreatIndicator,
  NxOverflowTooltip,
} from '@sonatype/react-shared-components';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { actions } from './containerImageWaiversSlice';

export default function ContainerImageWaiversTable() {
  const dispatch = useDispatch();
  const waivers = useSelector((state) => state.containerImageWaivers.waivers);
  const loading = useSelector((state) => state.containerImageWaivers.loading);
  const error = useSelector((state) => state.containerImageWaivers.error);

  const goToWaiverDetails = (waiver) => {
    dispatch(
      stateGo('firewall.waiver.details', {
        waiverId: waiver.policyWaiverId,
        ownerId: waiver.scopeOwnerId,
        ownerType: waiver.scopeOwnerType || 'application',
        type: 'waiver',
        sidebarReference: 'filter',
      })
    );
  };

  useEffect(() => {
    dispatch(actions.loadContainerImageWaivers());
  }, []);

  return (
    <NxTable className="iq-container-image-waivers-table">
      <NxTableHead>
        <NxTableRow>
          <NxTableCell className="iq-size-controlled-cell">Threat</NxTableCell>
          <NxTableCell>Date Created</NxTableCell>
          <NxTableCell>Expiration</NxTableCell>
          <NxTableCell>Policy</NxTableCell>
          <NxTableCell>Scope</NxTableCell>
          <NxTableCell>Components</NxTableCell>
          <NxTableCell chevron />
        </NxTableRow>
      </NxTableHead>
      <NxTableBody
        emptyMessage="No container image waivers found."
        error={error}
        isLoading={loading}
        retryHandler={() => dispatch(actions.loadContainerImageWaivers())}
      >
        {waivers.map((waiver) => (
          <NxTableRow key={waiver.policyWaiverId} isClickable onClick={() => goToWaiverDetails(waiver)}>
            <NxTableCell className="iq-threat-cell">
              <NxThreatIndicator policyThreatLevel={waiver.threatLevel} />
              <span className="nx-threat-number">{waiver.threatLevel}</span>
            </NxTableCell>
            <NxTableCell>{moment(waiver.createTime).format('YYYY-MM-DD')}</NxTableCell>
            <NxTableCell>{waiver.expiryTime ? moment(waiver.expiryTime).format('YYYY-MM-DD') : 'Never'}</NxTableCell>
            <NxTableCell>
              <NxOverflowTooltip>
                <div className="nx-truncate-ellipsis">{waiver.policyName}</div>
              </NxOverflowTooltip>
            </NxTableCell>
            <NxTableCell>
              <NxOverflowTooltip>
                <div className="nx-truncate-ellipsis">{waiver.scopeOwnerName || waiver.scopeOwnerId}</div>
              </NxOverflowTooltip>
            </NxTableCell>
            <NxTableCell>
              {waiver.matcherStrategy === 'ALL_COMPONENTS' ? 'All Components' : waiver.componentDisplayName}
            </NxTableCell>
            <NxTableCell chevron />
          </NxTableRow>
        ))}
      </NxTableBody>
    </NxTable>
  );
}
