/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';

import { selectPolicyViolations } from './firewallPolicyViolationsSelectors.js';
import FirewallPolicyViolationsTile from './policyViolationsTile/FirewallPolicyViolationsTile';
import { loadComponentPolicyViolations, loadExistingWaiversData } from 'MainRoot/firewall/firewallActions';
import { selectFirewallComponentDetailsPageRouteParams } from 'MainRoot/firewall/firewallSelectors';

export default function FirewallPolicyViolations() {
  const dispatch = useDispatch();
  const violations = useSelector(selectPolicyViolations);
  const { pathname, repositoryId, componentHash } = useSelector(selectFirewallComponentDetailsPageRouteParams);

  React.useEffect(() => {
    dispatch(loadComponentPolicyViolations(pathname, repositoryId));
    dispatch(loadExistingWaiversData('repository', repositoryId, componentHash));
  }, [pathname, repositoryId, componentHash]);

  return <FirewallPolicyViolationsTile violations={violations} title="Policy Violations" />;
}
