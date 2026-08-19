/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-disable react/prop-types */
import React, { useMemo } from 'react';
import { useSelector } from 'react-redux';
import NavPills from 'MainRoot/navPills/NavPills';
import { selectShowRepositoryConfiguration } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';

const hostedNavList = [
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

export default function RepositorySummaryPills({ isHosted = false }) {
  const showRepositoryConfiguration = useSelector(selectShowRepositoryConfiguration);

  const proxyNavList = useMemo(
    () => [
      {
        label: 'Proxy Repository',
        target: 'proxy-repository-pill-configuration',
        isDisplayed: showRepositoryConfiguration,
      },
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
    ],
    [showRepositoryConfiguration]
  );

  const navList = isHosted ? hostedNavList : proxyNavList;
  return <NavPills list={navList} root="#repositories-summary-sections" />;
}
