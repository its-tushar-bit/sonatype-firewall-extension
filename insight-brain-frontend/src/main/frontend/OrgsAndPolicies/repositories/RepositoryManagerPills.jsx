/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import NavPills from 'MainRoot/navPills/NavPills';

const navList = [
  {
    label: 'Configuration',
    target: 'repositories-pill-configuration',
    isDisplayed: true,
  },
  {
    label: 'Policies',
    target: 'owner-pill-policy',
    isDisplayed: true,
  },
  {
    label: 'Namespace Confusion Protection',
    target: 'namespace-confusion-protection-pill-configuration',
    isDisplayed: true,
  },
  {
    label: 'Access',
    target: 'access-tile-pill-access',
    isDisplayed: true,
  },
];

export default function RepositoryManagerPills() {
  return <NavPills list={navList} root="#repositories-summary-sections" />;
}
