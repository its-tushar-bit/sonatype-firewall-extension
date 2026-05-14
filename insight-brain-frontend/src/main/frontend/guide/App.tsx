/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { Routes, Route, Navigate } from 'react-router';
import { NavigationProvider } from '@guide/ui-core';
import { Spinner, Flex } from '@radix-ui/themes';
import { useReactRouterAdapter } from './reactRouterAdapter';
import { AuthProvider, useAuth } from './auth/AuthProvider';
import { LoginPage } from './auth/LoginPage';
import { AppShell } from './layout/AppShell';
import { ComponentsTestPage } from './components/ComponentsTestPage';
import { VulnerabilitiesPage } from './vulnerabilities/VulnerabilitiesPage';
import { LicenseProvider } from './license/LicenseProvider';
import { LicenseGate } from './license/LicenseGate';
import { GUIDE_PRODUCTS } from './license/licenseProducts';

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
    <LicenseProvider>
      <LicenseGate requires={GUIDE_PRODUCTS}>
        <AppShell>
          <Routes>
            <Route path="/" element={<h1>Sonatype Guide</h1>} />
            <Route path="/components" element={<ComponentsTestPage />} />
            <Route path="/vulnerabilities" element={<VulnerabilitiesPage />} />
          </Routes>
        </AppShell>
      </LicenseGate>
    </LicenseProvider>
  );
}

function BackupLogin() {
  const { status, ssoConfig, login } = useAuth();

  if (status === 'loading') {
    return (
      <Flex align="center" justify="center" style={{ minHeight: '100vh' }}>
        <Spinner size="3" />
      </Flex>
    );
  }

  if (status === 'authenticated') {
    return <Navigate to="/" replace />;
  }

  return <LoginPage login={login} ssoConfig={ssoConfig} />;
}

export default function App() {
  const adapter = useReactRouterAdapter();
  return (
    <AuthProvider>
      <NavigationProvider adapter={adapter}>
        <Routes>
          <Route path="/backupLogin" element={<BackupLogin />} />
          <Route path="*" element={<AuthGate />} />
        </Routes>
      </NavigationProvider>
    </AuthProvider>
  );
}
