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
import { AppShell } from './layout/AppShell';
import { ComponentsSearchPage } from './components/ComponentsSearchPage';
import { VulnerabilitiesPage } from './vulnerabilities/VulnerabilitiesPage';
import { SearchPage } from './search/SearchPage';
import { LicenseProvider } from './license/LicenseProvider';
import { LicenseGate } from './license/LicenseGate';
import { GUIDE_PRODUCTS } from './license/licenseProducts';
import { ComponentDetailPage } from './components/detail/ComponentDetailPage';
import { OverviewTab } from './components/detail/OverviewTab';
import { VulnerabilitiesTab } from './components/detail/VulnerabilitiesTab';
import { VersionsTab } from './components/detail/VersionsTab';
import { DependenciesTab } from './components/detail/DependenciesTab';
import { McpPage } from './mcp/McpPage';
import { FeatureFlagProvider } from './feature-flags/FeatureFlagProvider';
import { FeatureGate } from './feature-flags/FeatureGate';
import { FEATURE_FLAGS } from './feature-flags/featureFlags';
import { Home } from './home/Home';

function AuthGate() {
  const { status } = useAuth();

  // 'unauthenticated' triggers the redirect-to-/ effect inside AuthProvider.
  // While that effect runs we keep showing the spinner so the user never
  // sees a flash of empty Guide UI.
  if (status === 'loading' || status === 'unauthenticated') {
    return (
      <Flex align="center" justify="center" style={{ minHeight: '100vh' }}>
        <Spinner size="3" />
      </Flex>
    );
  }

  return (
    <LicenseProvider>
      <LicenseGate requires={GUIDE_PRODUCTS}>
        <FeatureFlagProvider>
          <AppShell>
            <Routes>
              <Route path="/" element={<Home />} />
              <Route
                path="/components"
                element={
                  <FeatureGate flag={FEATURE_FLAGS.GUIDE_UI}>
                    <ComponentsSearchPage />
                  </FeatureGate>
                }
              />
              <Route
                path="/vulnerabilities"
                element={
                  <FeatureGate flag={FEATURE_FLAGS.GUIDE_UI}>
                    <VulnerabilitiesPage />
                  </FeatureGate>
                }
              />
              <Route
                path="/search"
                element={
                  <FeatureGate flag={FEATURE_FLAGS.GUIDE_UI}>
                    <SearchPage />
                  </FeatureGate>
                }
              />
              <Route path="/mcp" element={<McpPage />} />
              <Route
                path="/component/:ecosystem/:pkg/:version"
                element={
                  <FeatureGate flag={FEATURE_FLAGS.GUIDE_UI}>
                    <ComponentDetailPage />
                  </FeatureGate>
                }
              >
                <Route index element={<OverviewTab />} />
                <Route path="versions" element={<VersionsTab />} />
                <Route path="vulnerabilities" element={<VulnerabilitiesTab />} />
                <Route path="dependencies" element={<DependenciesTab />} />
              </Route>
            </Routes>
          </AppShell>
        </FeatureFlagProvider>
      </LicenseGate>
    </LicenseProvider>
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
