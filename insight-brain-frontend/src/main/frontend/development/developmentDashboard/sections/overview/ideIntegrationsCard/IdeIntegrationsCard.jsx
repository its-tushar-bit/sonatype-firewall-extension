/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { NxCard, NxTextLink, NxH3, NxLoadWrapper } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/development/developmentDashboard/slices/ideIntegrationsSlice';
import { selectIdeIntegrationsSlice } from 'MainRoot/development/developmentDashboard/selectors/integrationsSelectors';
import { SECTIONS } from 'MainRoot/development/developmentDashboard/sections';
import useGetIntegrationsLink from 'MainRoot/development/developmentDashboard/useGetIntegrationsLink';

export default function IdeIntegrationsCard() {
  const dispatch = useDispatch();
  const doLoad = () => {
    dispatch(actions.loadIdeIntegratedUserCount());
  };
  const { loading, loadError, ideIntegratedUserCount } = useSelector(selectIdeIntegrationsSlice);
  const ideUserCountMessage =
    ideIntegratedUserCount === 1 ? 'developer is using an IDE integration' : 'developers are using IDE integrations';
  const ideHref = useGetIntegrationsLink(SECTIONS.IDE);

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <NxCard className="iq-integrations-card-ide nx-card--equal" aria-label="Integrate using IDEs">
      <NxCard.Header>
        <NxH3>Sync with IDEs</NxH3>
      </NxCard.Header>
      <NxCard.Content className={loadError ? 'nx-card__content--row' : ''}>
        <div className="iq-integrations-card-callout">
          <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
            <div className="iq-integrations-card-callout--count">{ideIntegratedUserCount}</div>
            <div className="iq-integrations-card-callout--message">{ideUserCountMessage}</div>
          </NxLoadWrapper>
        </div>

        <NxCard.Text>
          Integrate with your IDE to monitor your codebase. Remediate policy violations faster by analyzing the root
          cause within the context of your IDE and upgrade to safer component versions.
        </NxCard.Text>
      </NxCard.Content>
      <NxCard.Footer>
        <NxTextLink href={ideHref} data-analytics-id="sonatype-developer-overview-ide-integration">
          See our list of IDE integrations
        </NxTextLink>
      </NxCard.Footer>
    </NxCard>
  );
}
