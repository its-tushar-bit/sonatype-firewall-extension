/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useEffect, useRef, useState } from 'react';
import { useParams, Outlet, useSearchParams } from 'react-router';
import {
  PageLayout,
  PageHeading,
  BodyText,
  Button,
  HeroSection,
  ComponentDetailsHeader,
  ComponentTabsLayout,
  ComponentProvider,
  MalwareBanner,
  ArtifactPendingProvider,
  useSetArtifactPending,
} from '@guide/ui-core';
import {
  getComponentDetail,
  getComponentVulnerabilities,
  getComponentVersions,
  getComponentDependencies,
  getRecommendations,
} from 'GuideRoot/api/componentsBackend';
import { ErrorPage } from 'GuideRoot/layout/ErrorPage';
import { reloadPage, clearErrorRetries } from 'GuideRoot/utils/navigation';
import { PolicyContextBar } from 'GuideRoot/components/navigation/context-picker/PolicyContextBar';
import { ComponentDetailSkeleton } from './ComponentDetailSkeleton';
import type { ComponentDetails, RecommendationResponse } from '@guide/ui-core/types';

interface DetailState {
  component: ComponentDetails;
  vulnCount: number;
  versionsCount: number;
  depsCount: number;
  recommendations: RecommendationResponse | null;
}

export interface ArtifactOutletContext {
  extension?: string;
  classifier?: string;
}

/**
 * Inner component that has access to ArtifactPendingProvider context.
 * This allows it to set the pending state during artifact transitions.
 */
function ComponentDetailContent() {
  const { ecosystem = '', pkg = '', version = '' } = useParams<{
    ecosystem: string;
    pkg: string;
    version: string;
  }>();

  const [searchParams] = useSearchParams();
  const extension = searchParams.get('extension') || undefined;
  const classifier = searchParams.get('classifier') || undefined;

  const [state, setState] = useState<DetailState | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  // Tracks the last (ecosystem, pkg, version) tuple we loaded data for. React Router
  // reuses this component instance across navigations that match the same route
  // pattern, so a plain "hasLoadedOnce" boolean would incorrectly treat a jump to
  // a different component as an artifact-only transition. Comparing the tuple
  // scopes "already loaded" to the current component identity.
  const loadedComponentKey = useRef<string | null>(null);

  const setArtifactPending = useSetArtifactPending();

  useEffect(() => {
    let cancelled = false;

    // Show full-page skeleton whenever the component identity changes (or on first
    // load). For artifact-only transitions on the same component, keep existing
    // data on screen and let ArtifactPendingProvider drive skeletons in
    // TrustScoreCard and TabTriggerWithBadge.
    const currentKey = `${ecosystem}/${pkg}/${version}`;
    const isInitialLoad = loadedComponentKey.current !== currentKey;
    if (isInitialLoad) {
      setLoading(true);
      // Clear stale data from the previous component so the skeleton renders
      // instead of the previous name/version/malware banner while loading.
      setState(null);
    } else {
      // For artifact transitions, set pending state to show skeletons
      // in TrustScoreCard and TabTriggerWithBadge
      setArtifactPending(true);
    }
    setError(null);

    const artifact = { extension, classifier };
    Promise.all([
      getComponentDetail(ecosystem, pkg, version, artifact),
      getComponentVulnerabilities(ecosystem, pkg, version, undefined, {}, { offset: 0, limit: 1 }, artifact),
      getComponentVersions(ecosystem, pkg, version, undefined, {}, { offset: 0, limit: 1 }, artifact),
      getComponentDependencies(ecosystem, pkg, version, undefined, {}, { offset: 0, limit: 1 }, artifact),
      getRecommendations(ecosystem, pkg, version, artifact),
    ])
      .then(([component, vulnRes, versionsRes, depsRes, recommendations]) => {
        if (!cancelled) {
          clearErrorRetries();
          if (!component) {
            setState(null);
          } else {
            setState({
              component,
              vulnCount: vulnRes.total,
              versionsCount: versionsRes.total,
              depsCount: depsRes.total,
              recommendations,
            });
          }
        }
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : String(e));
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
          // Record the component identity we just finished loading data for
          // (regardless of outcome — found/404/error). Subsequent effect runs
          // for the SAME component skip the full-page skeleton and treat the
          // change as an artifact-only transition.
          loadedComponentKey.current = currentKey;
          // Only clear pending state if we set it in this run (artifact transitions),
          // mirroring the guarded setArtifactPending(true) above.
          if (!isInitialLoad) setArtifactPending(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [ecosystem, pkg, version, extension, classifier]);

  if (loading && state === null) return <ComponentDetailSkeleton />;

  // Only replace the page with the error view when there is no prior data to
  // preserve. During an artifact-only transition, `state` still holds the
  // previously loaded artifact — keep it on screen rather than dropping the
  // user into a full-page error for a transient fetch failure.
  if (error && state === null) {
    return <ErrorPage onRetry={reloadPage} />;
  }

  if (!state) {
    return (
      <PageLayout>
        <HeroSection>
          <PageHeading>Component Not Found</PageHeading>
          <BodyText align="center">
            Could not find component data for &ldquo;{ecosystem}/{pkg}/{version}&rdquo;.
          </BodyText>
          <BodyText align="center">Please check the URL and try again.</BodyText>
          <Button variant="primary" href="/">Go to home</Button>
        </HeroSection>
      </PageLayout>
    );
  }

  const { component, vulnCount, versionsCount, depsCount, recommendations } = state;

  return (
    <PageLayout>
      <PolicyContextBar />
      <ComponentDetailsHeader
        component={component}
        ecosystem={ecosystem}
        packageName={pkg}
        version={version}
        recommendationsResponse={recommendations}
        artifacts={component.components ?? []}
        artifactFilter={{ extension, classifier }}
      />
      <MalwareBanner
        isMalware={component.isMalware}
        name={component.name}
        version={component.version}
        extension={extension}
        classifier={classifier}
      />
      <ComponentProvider
        component={component}
        vulnerabilityCount={vulnCount}
        versionsCount={versionsCount}
        dependencyCount={depsCount}
      >
        <ComponentTabsLayout>
          <Outlet context={{ extension, classifier }} />
        </ComponentTabsLayout>
      </ComponentProvider>
    </PageLayout>
  );
}

export function ComponentDetailPage() {
  return (
    <ArtifactPendingProvider>
      <ComponentDetailContent />
    </ArtifactPendingProvider>
  );
}
