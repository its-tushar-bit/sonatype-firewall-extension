/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';
import { NxTile, NxH2, NxLoadWrapper } from '@sonatype/react-shared-components';
import FirewallPolicyViolationsTable from './FirewallPolicyViolationsTable';
import { loadComponentPolicyViolations } from 'MainRoot/firewall/firewallActions';
import {
  selectFirewallComponentDetailsPage,
  selectFirewallComponentDetailsPageRouteParams,
} from 'MainRoot/firewall/firewallSelectors';
import {
  selectWaivers,
  selectComponentName,
  selectComponentNameWithoutVersion,
} from '../firewallPolicyViolationsSelectors';
import { selectWaiverToDelete } from 'MainRoot/waivers/deleteWaiverModal/deleteWaiverSelector.js';
import ViewAllPoliciesWaiversButton from './ViewAllPoliciesWaiversButton';

export default function FirewallPolicyViolationsTile({ title, violations }) {
  const [showPolicyWaiversPopover, setShowComponentWaiversPopover] = useState(false);
  const { isLoadingPolicyViolations, policyViolationsError } = useSelector(selectFirewallComponentDetailsPage);
  const dispatch = useDispatch();
  const { pathname, repositoryId, componentDisplayName } = useSelector(selectFirewallComponentDetailsPageRouteParams);
  const componentName = useSelector(selectComponentName);
  const componentNameWithoutVersion = useSelector(selectComponentNameWithoutVersion);
  const waivers = useSelector(selectWaivers);
  const waiverToDelete = useSelector(selectWaiverToDelete);

  return (
    <NxTile>
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>{title}</NxH2>
        </NxTile.HeaderTitle>
        <div className="nx-tile__actions">
          <ViewAllPoliciesWaiversButton setShowComponentWaiversPopover={setShowComponentWaiversPopover} />
        </div>
      </NxTile.Header>
      <NxTile.Content>
        <NxLoadWrapper
          loading={isLoadingPolicyViolations}
          error={policyViolationsError}
          retryHandler={() => dispatch(loadComponentPolicyViolations(pathname, repositoryId))}
        >
          <FirewallPolicyViolationsTable
            setShowComponentWaiversPopover={setShowComponentWaiversPopover}
            showPolicyWaiversPopover={showPolicyWaiversPopover}
            loadPolicyViolationsInformation={loadComponentPolicyViolations}
            {...{ violations }}
            showProxyState
            componentName={componentName || componentDisplayName}
            componentNameWithoutVersion={componentNameWithoutVersion}
            waivers={waivers}
            waiverToDelete={waiverToDelete}
          />
        </NxLoadWrapper>
      </NxTile.Content>
    </NxTile>
  );
}

FirewallPolicyViolationsTile.propTypes = {
  title: PropTypes.string.isRequired,
  violations: FirewallPolicyViolationsTable.propTypes.violations,
  showPolicyWaiversPopover: PropTypes.func,
  componentName: PropTypes.string,
  componentNameWithoutVersion: PropTypes.string,
  waivers: FirewallPolicyViolationsTable.propTypes.waivers,
  waiverToDelete: FirewallPolicyViolationsTable.propTypes.waiverToDelete,
};
