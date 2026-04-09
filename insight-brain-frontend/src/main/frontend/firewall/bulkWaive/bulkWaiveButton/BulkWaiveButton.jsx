/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useDispatch } from 'react-redux';
import { NxButton } from '@sonatype/react-shared-components';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { actions as firewallBulkWaiverActions } from '../firewallBulkWaiverSlice';
import { actions as repositoryResultsActions } from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSlice';
import { checkPermissions } from 'MainRoot/util/authorizationUtil';

export default function BulkWaiveButton({
  repositoryId,
  disabled,
  className = '',
  source = 'repository-report',
  componentIdentifier = null,
  componentHash = null,
  matchState = null,
  tabId = null,
  pathname = null,
  componentDisplayName = null,
}) {
  const dispatch = useDispatch();
  const [hasPermission, setHasPermission] = useState(true);

  useEffect(() => {
    if (repositoryId) {
      checkPermissions(['WAIVE_POLICY_VIOLATIONS'], 'repository', repositoryId)
        .then(() => setHasPermission(true))
        .catch(() => setHasPermission(false));
    }
  }, [repositoryId]);

  const handleClick = () => {
    dispatch(
      firewallBulkWaiverActions.setSourceContext({
        source,
        repositoryId,
        componentIdentifier,
        componentHash,
        matchState,
        tabId,
        pathname,
        componentDisplayName,
      })
    );

    dispatch(repositoryResultsActions.clearFilters());

    dispatch(stateGo('firewall.bulkWaive', { repositoryId }));
  };

  if (!hasPermission) {
    return null;
  }

  return (
    <NxButton variant="tertiary" id="fw-bulk-waive" disabled={disabled} className={className} onClick={handleClick}>
      <span>Bulk Waive</span>
    </NxButton>
  );
}

BulkWaiveButton.propTypes = {
  repositoryId: PropTypes.string.isRequired,
  disabled: PropTypes.bool.isRequired,
  className: PropTypes.string,
  source: PropTypes.oneOf(['repository-report', 'component-details']),
  componentIdentifier: PropTypes.string,
  componentHash: PropTypes.string,
  matchState: PropTypes.string,
  tabId: PropTypes.string,
  pathname: PropTypes.string,
  componentDisplayName: PropTypes.string,
};
