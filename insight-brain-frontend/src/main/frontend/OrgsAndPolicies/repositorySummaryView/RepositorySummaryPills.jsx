/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import NavPills from 'MainRoot/navPills/NavPills';

const navList = [
  {
    label: 'Policies',
    target: 'owner-pill-policy',
    isDisplayed: true,
  },
  {
    label: 'Access',
    target: 'access-tile-pill-access',
    isDisplayed: true,
  },
];

export default function RepositorySummaryPills() {
  return <NavPills list={navList} root="#repositories-summary-sections" />;
}
