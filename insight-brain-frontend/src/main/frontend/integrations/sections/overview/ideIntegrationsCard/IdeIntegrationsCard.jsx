/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { NxCard, NxTextLink, NxH3, NxLoadWrapper } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from './ideIntegrationsSlice';
import { selectIdeIntegrationsSlice } from './integrationsSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { SECTIONS } from 'MainRoot/integrations/module';

export default function IdeIntegrationsCard() {
  const uiRouterState = useRouterState();
  const dispatch = useDispatch();
  const doLoad = () => dispatch(actions.loadIdeIntegratedUserCount());
  const { loading, loadError, ideIntegratedUserCount } = useSelector(selectIdeIntegrationsSlice);
  const ideUserCountMessage =
    ideIntegratedUserCount === 1
      ? 'member of your team uses an IDE integration'
      : 'members of your team use an IDE integration';
  const ideHref = uiRouterState.href(`integrations.${SECTIONS.IDE}`);

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <NxCard className="iq-integrations-card-ide nx-card--equal" aria-label="Integrate using IDEs">
      <NxCard.Header>
        <NxH3>Integrate using IDEs</NxH3>
      </NxCard.Header>
      <NxCard.Content className={`nx-card__content--columns ${loadError ? 'nx-card__content--row' : ''}`}>
        <div className="iq-integrations-card-callout">
          <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
            <div className="nx-card__call-out">{ideIntegratedUserCount}</div>
            <div>{ideUserCountMessage}</div>
          </NxLoadWrapper>
        </div>

        <NxCard.Text>
          IDE integrations give you immediate visibility into open-source issues that do not meet AppSec requirements.
          Prevent frustrating rework by discovering issues before committing any code.
        </NxCard.Text>
      </NxCard.Content>
      <NxCard.Footer>
        <NxTextLink href={ideHref}>See our list of IDE integrations</NxTextLink>
      </NxCard.Footer>
    </NxCard>
  );
}
