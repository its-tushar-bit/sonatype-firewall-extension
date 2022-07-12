/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';

import { selectPolicyViolations } from './firewallPolicyViolationsSelectors.js';
import FirewallPolicyViolationsTile from './policyViolationsTile/FirewallPolicyViolationsTile';

export default function FirewallPolicyViolations() {
  const violations = useSelector(selectPolicyViolations);
  return <FirewallPolicyViolationsTile violations={violations} title="Policy Violations" />;
}
