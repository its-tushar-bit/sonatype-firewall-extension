/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxTextLink } from '@sonatype/react-shared-components';
import { useSelector } from 'react-redux';
import { selectCurrentRouteName } from 'MainRoot/reduxUiRouter/routerSelectors';
import { SECTIONS } from 'MainRoot/integrations/integrations.module';
import useGetIntegrationsLink from 'MainRoot/integrations/useGetIntegrationsLink';

export default function IntegrationsNavigation() {
  const { OVERVIEW, CICD, SCM, ISSUE_TRACKING, IDE } = SECTIONS;

  return (
    <div className="nx-viewport-sized__container">
      <ul className="nx-list">
        <li>
          <IntegrationsLink
            id="integrations-sidebar__overview-link"
            sectionName={OVERVIEW}
            dataAnalyticsId="sonatype-developer-sidebar-overview"
          >
            Overview
          </IntegrationsLink>
        </li>

        <li>
          <IntegrationsLink
            id="integrations-sidebar__cicd-link"
            sectionName={CICD}
            dataAnalyticsId="sonatype-developer-sidebar-cicd"
          >
            CI System Integrations
          </IntegrationsLink>
        </li>

        <li>
          <IntegrationsLink
            id="integrations-sidebar__scm-link"
            sectionName={SCM}
            dataAnalyticsId="sonatype-developer-sidebar-scm"
          >
            SCM
          </IntegrationsLink>
        </li>

        <li>
          <IntegrationsLink
            id="integrations-sidebar__issue-tracking-link"
            sectionName={ISSUE_TRACKING}
            dataAnalyticsId="sonatype-developer-sidebar-issue-tracking"
          >
            Issue Tracking
          </IntegrationsLink>
        </li>

        <li>
          <IntegrationsLink
            id="integrations-sidebar__ide-link"
            sectionName={IDE}
            dataAnalyticsId="sonatype-developer-sidebar-ide"
          >
            IDE
          </IntegrationsLink>
        </li>
      </ul>
    </div>
  );
}

function IntegrationsLink({ sectionName, children, id, dataAnalyticsId }) {
  const selectedSectionName = useSelector(selectCurrentRouteName);

  const url = useGetIntegrationsLink(sectionName);

  return (
    <NxTextLink id={id} href={url} className={getClassNames(sectionName)} data-analytics-id={dataAnalyticsId}>
      {children}
    </NxTextLink>
  );

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
