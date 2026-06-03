/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import SwaggerUI from 'swagger-ui-react';
import 'swagger-ui-react/swagger-ui.css';
import {
  NxH2,
  NxLoadError,
  NxLoadWrapper,
  NxPageMain,
  NxStatefulWarningAlert,
  NxTab,
  NxTabList,
  NxTabPanel,
  NxTabs,
  NxTile,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { selectApiPageSlice } from 'MainRoot/api/apiPageSelectors';
import { actions } from 'MainRoot/api/apiPageSlice';
import { BASE_URL } from 'MainRoot/util/urlUtil';
import { fetchUser } from 'MainRoot/user/userSessionUtils';
import { selectCurrentUser } from 'MainRoot/user/userSessionSelectors';

export default function ApiPage() {
  const dispatch = useDispatch();
  const { loading, loadError, publicOpenApi, experimentalOpenApi } = useSelector(selectApiPageSlice);
  const currentUser = useSelector(selectCurrentUser);
  const loadOpenApi = (endpoint) => dispatch(actions.loadOpenApi(endpoint));
  const [activeTabId, setActiveTabId] = useState(0);
  const load = () => {
    if (activeTabId === 0) {
      loadOpenApi('public');
    } else {
      loadOpenApi('experimental');
    }
  };
  const requestInterceptor = (request) => {
    request.headers['X-CSRF-TOKEN'] = window.document.cookie
      .split('; ')
      .find((row) => row.startsWith('CLM-CSRF-TOKEN='))
      ?.split('=')[1];
    const url = new URL(request.url);
    request.url = BASE_URL + url.pathname + url.search;

    // Schedule to send a request to keep any user session alive after the API request.
    // This is useful if somebody is working just within the API page
    // and doesn't want to be nagged or auto-logged out due to what looks like inactivity
    // but is actually just them sending raw API requests.
    if (currentUser) {
      setTimeout(() => fetchUser(false), 0);
    }

    return request;
  };

  useEffect(() => {
    load();
  }, [activeTabId]);

  return (
    <NxPageMain id="api-page">
      <NxTile>
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>API</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Header>
        <NxTile.Content>
          <NxLoadError error={loadError} retryHandler={load} />
          {!loadError && (
            <NxTabs activeTab={activeTabId} onTabSelect={setActiveTabId}>
              <NxTabList>
                <NxTab>Public</NxTab>
                <NxTab>Experimental</NxTab>
              </NxTabList>
              <NxTabPanel>
                <NxLoadWrapper loading={loading} retryHandler={() => {}}>
                  <SwaggerUI
                    spec={publicOpenApi}
                    defaultModelsExpandDepth={-1}
                    requestInterceptor={requestInterceptor}
                  />
                </NxLoadWrapper>
              </NxTabPanel>
              <NxTabPanel>
                <NxLoadWrapper loading={loading} retryHandler={() => {}}>
                  <NxStatefulWarningAlert>These REST APIs are liable to change.</NxStatefulWarningAlert>
                  <SwaggerUI
                    spec={experimentalOpenApi}
                    defaultModelsExpandDepth={-1}
                    requestInterceptor={requestInterceptor}
                  />
                </NxLoadWrapper>
              </NxTabPanel>
            </NxTabs>
          )}
        </NxTile.Content>
      </NxTile>
    </NxPageMain>
  );
}
