/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { NxH3, NxP, NxTable, NxTextLink, NxTile } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from './appsWithoutScmIntegrationsSlice';
import { appsWithoutScmIntegrationsSliceSelector } from './appsWithoutScmIntegrationsSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { SECTIONS } from 'MainRoot/integrations/integrations.module';

export default function AppsWithoutScmIntegrations() {
  const uiRouterState = useRouterState();
  const scmIntegrationsHref = uiRouterState.href(`integrations.${SECTIONS.SCM}`);
  const howToEnableScmIntegrationsHref =
    'https://links.sonatype.com/products/nxiq/doc/integrations/scm/automatic-source-control-feedback';

  const appsWithoutScmIntegrationsSlice = useSelector(appsWithoutScmIntegrationsSliceSelector);
  const { dashboardResults, loading, loadError } = appsWithoutScmIntegrationsSlice;

  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(actions.loadAppsWithoutScmIntegrations());
  }, []);

  return (
    <div id="iq-integrations-apps-without-scm-integrations-section">
      <NxTile>
        <NxTile.Content>
          <NxTile.Subsection>
            <NxTile.SubsectionHeader>
              <NxH3>Automated Source Control Feedback</NxH3>
            </NxTile.SubsectionHeader>
            <NxP>
              Identify and remediate open source issues earlier in development where they have the least impact. With
              automated source control feedback via SCM Integration, developers are only notified on policy violations
              and how to remediate them within a pull request.
            </NxP>
          </NxTile.Subsection>
          <NxTile.Subsection>
            <NxTile.SubsectionHeader>
              <NxH3>High risk apps not set up with Automated Source Control Feedback</NxH3>
            </NxTile.SubsectionHeader>
            <NxTable>
              <NxTable.Head>
                <NxTable.Row>
                  <NxTable.Cell>Apps</NxTable.Cell>
                  <NxTable.Cell>Total Risk</NxTable.Cell>
                </NxTable.Row>
              </NxTable.Head>
              <NxTable.Body
                isLoading={loading}
                error={loadError}
                emptyMessage="All of your apps are set up with Automatic Source Control Feedback."
              >
                {dashboardResults.map(({ applicationName, totalRisk }) => {
                  return (
                    <NxTable.Row key={applicationName.concat(totalRisk)}>
                      <NxTable.Cell>{applicationName}</NxTable.Cell>
                      <NxTable.Cell>{totalRisk}</NxTable.Cell>
                    </NxTable.Row>
                  );
                })}
              </NxTable.Body>
            </NxTable>
          </NxTile.Subsection>
          <NxP>
            Read more <NxTextLink href={scmIntegrationsHref}>about our SCM integrations</NxTextLink> or{' '}
            <NxTextLink href={howToEnableScmIntegrationsHref} external>
              how to enable them
            </NxTextLink>
            .
          </NxP>
        </NxTile.Content>
      </NxTile>
    </div>
  );
}
