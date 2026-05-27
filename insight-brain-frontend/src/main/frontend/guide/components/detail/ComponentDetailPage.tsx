/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useEffect, useState } from 'react';
import { useParams, Outlet } from 'react-router';
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
} from '@guide/ui-core';
import {
  getComponentDetail,
  getComponentVulnerabilities,
  getComponentVersions,
  getComponentDependencies,
} from 'GuideRoot/api/componentsBackend';
import { ErrorPage } from 'GuideRoot/layout/ErrorPage';
import { ComponentDetailSkeleton } from './ComponentDetailSkeleton';
import type { ComponentDetails } from '@guide/ui-core/types';

interface DetailState {
  component: ComponentDetails;
  vulnCount: number;
  versionsCount: number;
  depsCount: number;
}

export function ComponentDetailPage() {
  const { ecosystem = '', pkg = '', version = '' } = useParams<{
    ecosystem: string;
    pkg: string;
    version: string;
  }>();

  const [state, setState] = useState<DetailState | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    Promise.all([
      getComponentDetail(ecosystem, pkg, version),
      getComponentVulnerabilities(ecosystem, pkg, version, undefined, {}, { offset: 0, limit: 1 }),
      getComponentVersions(ecosystem, pkg, version, undefined, {}, { offset: 0, limit: 1 }),
      getComponentDependencies(ecosystem, pkg, version, undefined, {}, { offset: 0, limit: 1 }),
    ])
      .then(([component, vulnRes, versionsRes, depsRes]) => {
        if (!cancelled) {
          if (!component) {
            setState(null);
          } else {
            setState({
              component,
              vulnCount: vulnRes.total,
              versionsCount: versionsRes.total,
              depsCount: depsRes.total,
            });
          }
        }
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : String(e));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => { cancelled = true; };
  }, [ecosystem, pkg, version]);

  if (loading) return <ComponentDetailSkeleton />;

  if (error) {
    return <ErrorPage retryHref="/components" />;
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

  const { component, vulnCount, versionsCount, depsCount } = state;

  return (
    <PageLayout>
      <ComponentDetailsHeader
        component={component}
        ecosystem={ecosystem}
        packageName={pkg}
        version={version}
        recommendationsResponse={null}
      />
      <MalwareBanner
        isMalware={component.isMalware}
        name={component.name}
        version={component.version}
      />
      <ComponentProvider
        component={component}
        vulnerabilityCount={vulnCount}
        versionsCount={versionsCount}
        dependencyCount={depsCount}
      >
        <ComponentTabsLayout>
          <Outlet />
        </ComponentTabsLayout>
      </ComponentProvider>
    </PageLayout>
  );
}
