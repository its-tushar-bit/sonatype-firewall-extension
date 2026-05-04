/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Routes, Route } from 'react-router';
import { NavigationProvider } from '@guide/ui-core';
import { Spinner, Flex } from '@radix-ui/themes';
import { useReactRouterAdapter } from './reactRouterAdapter';
import { AuthProvider, useAuth } from './auth/AuthProvider';
import { LoginPage } from './auth/LoginPage';

function AuthGate() {
  const { status, ssoConfig, login } = useAuth();

  if (status === 'loading') {
    return (
      <Flex align="center" justify="center" style={{ minHeight: '100vh' }}>
        <Spinner size="3" />
      </Flex>
    );
  }

  if (status === 'unauthenticated') {
    return <LoginPage login={login} ssoConfig={ssoConfig} />;
  }

  return (
    <Routes>
      <Route path="/" element={<h1>Sonatype Guide</h1>} />
    </Routes>
  );
}

export default function App() {
  const adapter = useReactRouterAdapter();
  return (
    <AuthProvider>
      <NavigationProvider adapter={adapter}>
        <AuthGate />
      </NavigationProvider>
    </AuthProvider>
  );
}
