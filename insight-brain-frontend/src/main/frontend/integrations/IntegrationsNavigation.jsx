/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxH2, NxTextLink } from '@sonatype/react-shared-components';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { useSelector } from 'react-redux';
import { selectCurrentRouteName } from 'MainRoot/reduxUiRouter/routerSelectors';
import { SECTIONS } from 'MainRoot/integrations/integrations.module';

export default function IntegrationsNavigation() {
  const { OVERVIEW, CICD, SCM, ISSUE_TRACKING, IDE, OTHERS } = SECTIONS;

  return (
    <>
      <NxH2>Integrations</NxH2>

      <ul className="nx-list">
        <li>
          <IntegrationsLink sectionName={OVERVIEW}>Overview</IntegrationsLink>
        </li>

        <li>
          <IntegrationsLink sectionName={CICD}>CI System Integrations</IntegrationsLink>
        </li>

        <li>
          <IntegrationsLink sectionName={SCM}>SCM</IntegrationsLink>
        </li>

        <li>
          <IntegrationsLink sectionName={ISSUE_TRACKING}>Issue Tracking</IntegrationsLink>
        </li>

        <li>
          <IntegrationsLink sectionName={IDE}>IDE</IntegrationsLink>
        </li>

        <li>
          <IntegrationsLink sectionName={OTHERS}>Others</IntegrationsLink>
        </li>
      </ul>
    </>
  );
}

function IntegrationsLink({ sectionName, displayName, children }) {
  const selectedSectionName = useSelector(selectCurrentRouteName);
  const uiRouterState = useRouterState();

  return (
    <NxTextLink href={getLink(sectionName)} className={getClassNames(sectionName)}>
      {children}
    </NxTextLink>
  );

  function getLink(sectionName) {
    return uiRouterState.href(`integrations.${sectionName}`);
  }

  function isSelected(sectionName) {
    return selectedSectionName === `integrations.${sectionName}`;
  }

  function getClassNames(sectionName) {
    if (isSelected(sectionName)) {
      return 'integrations-link integrations-link-selected';
    } else {
      return 'integrations-link';
    }
  }
}
