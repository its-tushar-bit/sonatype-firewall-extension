/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState, useMemo } from 'react';
import * as PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { NxTable, NxTableBody, NxTableCell, NxTableHead, NxTableRow } from '@sonatype/react-shared-components';
import {
  selectWaiverRequests,
  selectWaiverRequestsLoading,
  selectWaiverRequestsError,
} from './firewallWaiverRequestsSelectors';
import { actions } from './firewallWaiverRequestsSlice';
import FirewallRequestedWaiversTableRow from './FirewallRequestedWaiversTableRow';

function getNextSortDir(currentDir) {
  return currentDir === null ? 'asc' : currentDir === 'asc' ? 'desc' : null;
}

function compareValues(a, b) {
  if (a == null && b == null) return 0;
  if (a == null) return -1;
  if (b == null) return 1;
  if (typeof a === 'number' && typeof b === 'number') return a - b;
  return String(a).localeCompare(String(b));
}

function getSortValue(request, field) {
  switch (field) {
    case 'threatLevel':
      return request.threatLevel;
    case 'requestTime':
      return request.requestTime;
    case 'requesterName':
      return request.requesterName;
    case 'policyName':
      return request.policyName;
    case 'scope':
      return request.scopeOwnerName || request.scopeOwnerId;
    case 'status':
      return request.status;
    default:
      return null;
  }
}

export default function FirewallRequestedWaiversTable({ repositoryFormat }) {
  const dispatch = useDispatch();
  const waiverRequests = useSelector(selectWaiverRequests);
  const loading = useSelector(selectWaiverRequestsLoading);
  const error = useSelector(selectWaiverRequestsError);

  const [sortField, setSortField] = useState(null);
  const [sortDir, setSortDir] = useState(null);

  useEffect(() => {
    dispatch(actions.loadWaiverRequests());
  }, []);

  const handleSort = (field) => {
    const nextDir = sortField === field ? getNextSortDir(sortDir) : getNextSortDir(null);
    setSortField(nextDir === null ? null : field);
    setSortDir(nextDir);
  };

  const sortedRequests = useMemo(() => {
    // For the containers tab, only show waiver requests scoped to REPOSITORY_CONTAINER_ID.
    // For the components tab, exclude those requests (they are container-only).
    const isContainerRequest = (r) =>
      r.scopeOwnerType === 'all_repositories' || r.scopeOwnerId === 'REPOSITORY_CONTAINER_ID';
    const filtered = waiverRequests.filter((r) => {
      if (repositoryFormat === 'docker') {
        return isContainerRequest(r);
      }
      return !isContainerRequest(r);
    });
    if (!sortField || !sortDir) {
      return filtered;
    }
    return [...filtered].sort((a, b) => {
      const valA = getSortValue(a, sortField);
      const valB = getSortValue(b, sortField);
      const cmp = compareValues(valA, valB);
      return sortDir === 'asc' ? cmp : -cmp;
    });
  }, [waiverRequests, sortField, sortDir, repositoryFormat]);

  return (
    <NxTable className="iq-requested-waivers-table">
      <NxTableHead>
        <NxTableRow>
          <NxTableCell
            isSortable
            sortDir={sortField === 'threatLevel' ? sortDir : null}
            onClick={() => handleSort('threatLevel')}
          >
            Threat
          </NxTableCell>
          <NxTableCell
            isSortable
            sortDir={sortField === 'requestTime' ? sortDir : null}
            onClick={() => handleSort('requestTime')}
          >
            Date Requested
          </NxTableCell>
          <NxTableCell
            isSortable
            sortDir={sortField === 'requesterName' ? sortDir : null}
            onClick={() => handleSort('requesterName')}
          >
            Requester
          </NxTableCell>
          <NxTableCell
            isSortable
            sortDir={sortField === 'policyName' ? sortDir : null}
            onClick={() => handleSort('policyName')}
          >
            Policy
          </NxTableCell>
          <NxTableCell
            isSortable
            sortDir={sortField === 'scope' ? sortDir : null}
            onClick={() => handleSort('scope')}
          >
            Scope
          </NxTableCell>
          <NxTableCell>Components</NxTableCell>
          <NxTableCell
            isSortable
            sortDir={sortField === 'status' ? sortDir : null}
            onClick={() => handleSort('status')}
          >
            Status
          </NxTableCell>
          <NxTableCell chevron />
        </NxTableRow>
      </NxTableHead>
      <NxTableBody
        emptyMessage="No waiver requests found."
        error={error}
        isLoading={loading}
        retryHandler={() => dispatch(actions.loadWaiverRequests())}
      >
        {sortedRequests.map((request) => (
          <FirewallRequestedWaiversTableRow key={request.policyWaiverRequestId} request={request} repositoryFormat={repositoryFormat} />
        ))}
      </NxTableBody>
    </NxTable>
  );
}

FirewallRequestedWaiversTable.propTypes = {
  repositoryFormat: PropTypes.string,
};
