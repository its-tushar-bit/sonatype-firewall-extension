/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';
import { NxTile, NxH2, NxLoadWrapper } from '@sonatype/react-shared-components';
import FirewallPolicyViolationsTable from './FirewallPolicyViolationsTable';
import { loadComponentPolicyViolations } from 'MainRoot/firewall/firewallActions';
import {
  selectFirewallComponentDetailsPage,
  selectFirewallComponentDetailsPageRouteParams,
} from 'MainRoot/firewall/firewallSelectors';

export default function FirewallPolicyViolationsTile({ title, violations, showProxyState = false }) {
  const { isLoadingPolicyViolations, policyViolationsError } = useSelector(selectFirewallComponentDetailsPage);
  const dispatch = useDispatch();
  const { pathname, repositoryId } = useSelector(selectFirewallComponentDetailsPageRouteParams);

  return (
    <NxTile>
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>{title}</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <NxLoadWrapper
          loading={isLoadingPolicyViolations}
          error={policyViolationsError}
          retryHandler={() => dispatch(loadComponentPolicyViolations(pathname, repositoryId))}
        >
          <FirewallPolicyViolationsTable {...{ violations }} showProxyState={showProxyState} />
        </NxLoadWrapper>
      </NxTile.Content>
    </NxTile>
  );
}

FirewallPolicyViolationsTile.propTypes = {
  title: PropTypes.string.isRequired,
  violations: FirewallPolicyViolationsTable.propTypes.violations,
  showProxyState: FirewallPolicyViolationsTable.propTypes.showProxyState,
};
