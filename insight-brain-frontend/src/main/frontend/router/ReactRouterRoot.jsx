/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { UIRouter, UIView } from '@uirouter/react';
import { Provider as ReduxProvider } from 'react-redux';
import store from 'MainRoot/reduxConfig/store';
import router from 'MainRoot/router/routerInstance';

export default function ReactRouterRoot() {
  return (
    <ReduxProvider store={store}>
      <UIRouter router={router}>
        <UIView />
      </UIRouter>
    </ReduxProvider>
  );
}
