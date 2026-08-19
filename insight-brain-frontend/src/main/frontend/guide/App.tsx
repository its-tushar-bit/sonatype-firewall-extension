/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useMemo } from 'react';
import { Routes, Route } from 'react-router';
import { NavigationProvider } from '@guide/ui-core';
import { Spinner, Flex } from '@radix-ui/themes';
import { useReactRouterAdapter } from './reactRouterAdapter';
import { OwnerAdapterProvider } from './components/navigation/context-picker/OwnerAdapterProvider';
import { PolicyContextProvider } from './components/navigation/context-picker/PolicyContext';
import { SelfHostedOwnerAdapter } from './api/context-picker/SelfHostedOwnerAdapter';
import { AuthProvider, useAuth } from './auth/AuthProvider';
import { AppShell } from './layout/AppShell';
import { PolicyScopeBoundary } from './layout/PolicyScopeBoundary';
import { NotFoundPage } from './layout/NotFoundPage';
import { ErrorBoundary } from './layout/ErrorBoundary';
import { ComponentsSearchPage } from './components/ComponentsSearchPage';
import { VulnerabilitiesPage } from './vulnerabilities/VulnerabilitiesPage';
import { SecurityEventsPage } from './security-events/SecurityEventsPage';
import { SecurityEventDetailLayout } from './security-events/SecurityEventDetailLayout';
import { SecurityEventOverviewTab } from './security-events/detail/SecurityEventOverviewTab';
import { SecurityEventComponentsImpactedTab } from './security-events/detail/SecurityEventComponentsImpactedTab';
import { VulnerabilityDetailLayout } from './vulnerabilities/VulnerabilityDetailLayout';
import { SecurityDetailsTab } from './vulnerabilities/detail/SecurityDetailsTab';
import { ComponentsImpactedTab } from './vulnerabilities/detail/ComponentsImpactedTab';
import { SonatypeResearchTab } from './vulnerabilities/detail/SonatypeResearchTab';
import { SearchPage } from './search/SearchPage';
import { LicenseProvider } from './license/LicenseProvider';
import { LicenseGate } from './license/LicenseGate';
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
import { OnboardingProvider, OnboardingModal } from './onboarding';

function AuthGate() {
  const { status } = useAuth();
  // Owner adapter for the policy-context picker, backed by the /api/v2/policy-context/owners/*
  // endpoints. A future Guide SaaS host swaps in its own OwnerAdapter implementation.
  const ownerAdapter = useMemo(() => new SelfHostedOwnerAdapter(), []);

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
      <LicenseGate>
        <FeatureFlagProvider>
          <OwnerAdapterProvider adapter={ownerAdapter}>
            <PolicyContextProvider>
              <OnboardingProvider>
                <AppShell>
                  <OnboardingModal />
                  <PolicyScopeBoundary>
                    <ErrorBoundary>
                      <Routes>
                        <Route path="/" element={<Home />} />
                        <Route path="/components" element={<ComponentsSearchPage />} />
                        <Route path="/vulnerabilities" element={<VulnerabilitiesPage />} />
                        <Route path="/vulnerability/:vulnId" element={<VulnerabilityDetailLayout />}>
                          <Route index element={<SecurityDetailsTab />} />
                          <Route path="components-impacted" element={<ComponentsImpactedTab />} />
                          <Route path="sonatype-research" element={<SonatypeResearchTab />} />
                        </Route>
                        <Route path="/security-events" element={<SecurityEventsPage />} />
                        <Route path="/security-event/:eventId" element={<SecurityEventDetailLayout />}>
                          <Route index element={<SecurityEventOverviewTab />} />
                          <Route path="affected-components" element={<SecurityEventComponentsImpactedTab />} />
                        </Route>
                        <Route
                          path="/search"
                          element={
                            <FeatureGate flag={FEATURE_FLAGS.AI_DEVELOPER}>
                              <SearchPage />
                            </FeatureGate>
                          }
                        />
                        <Route path="/mcp" element={<McpPage />} />
                        <Route path="/component/:ecosystem/:pkg/:version" element={<ComponentDetailPage />}>
                          <Route index element={<OverviewTab />} />
                          <Route path="versions" element={<VersionsTab />} />
                          <Route path="vulnerabilities" element={<VulnerabilitiesTab />} />
                          <Route path="dependencies" element={<DependenciesTab />} />
                        </Route>
                        <Route path="*" element={<NotFoundPage />} />
                      </Routes>
                    </ErrorBoundary>
                  </PolicyScopeBoundary>
                </AppShell>
              </OnboardingProvider>
            </PolicyContextProvider>
          </OwnerAdapterProvider>
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
