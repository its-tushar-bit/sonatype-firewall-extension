/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Routes, Route } from 'react-router';
import { NavigationProvider } from '@guide/ui-core';
import { useReactRouterAdapter } from './reactRouterAdapter';

export default function App() {
  const adapter = useReactRouterAdapter();
  return (
    <NavigationProvider adapter={adapter}>
      <Routes>
        <Route path="/" element={<h1>Sonatype Guide</h1>} />
      </Routes>
    </NavigationProvider>
  );
}
