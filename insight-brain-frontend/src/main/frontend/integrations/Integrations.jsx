/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect, useMemo } from 'react';
import IntegrationsNavigation from './IntegrationsNavigation';
import Overview from './sections/overview/Overview';
import CiCd from './sections/CiCd';
import Scm from './sections/Scm';
import IssueTracking from './sections/IssueTracking';
import Ide from './sections/Ide';
import { useDispatch, useSelector } from 'react-redux';
import { selectRouterStateUrl } from 'MainRoot/reduxUiRouter/routerSelectors';
import { NxH1, NxPageMain, NxPageTitle } from '@sonatype/react-shared-components';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { SECTIONS } from './module';

const componentMap = {
  '/overview': Overview,
  '/ci-cd': CiCd,
  '/scm': Scm,
  '/issue-tracking': IssueTracking,
  '/ide': Ide,
};

export default function Integrations() {
  const dispatch = useDispatch();
  const currentPath = useSelector(selectRouterStateUrl);

  useEffect(() => {
    if (currentPath === '/integrations') {
      dispatch(stateGo(`integrations.${SECTIONS.OVERVIEW}`));
    }
  }, [currentPath]);

  const renderComponent = useMemo(() => {
    const Component = componentMap[currentPath];
    return Component ? <Component /> : null;
  }, [currentPath]);

  return (
    <>
      <div id="integrations-sidebar" className="nx-page-sidebar nx-viewport-sized">
        <IntegrationsNavigation />
      </div>

      <NxPageMain>
        <NxPageTitle>
          <NxH1>Integrations</NxH1>
        </NxPageTitle>

        {renderComponent}
      </NxPageMain>
    </>
  );
}
