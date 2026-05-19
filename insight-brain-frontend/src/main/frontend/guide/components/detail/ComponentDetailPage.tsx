/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useEffect, useState } from 'react';
import { useParams, Outlet, Link } from 'react-router';
import { Flex } from '@radix-ui/themes';
import {
  PageLayout,
  ComponentDetailsHeader,
  ComponentTabsLayout,
  ComponentProvider,
  MalwareBanner,
} from '@guide/ui-core';
import { tokens } from '@guide/ui-core/utils';
import {
  getComponentDetail,
  getComponentVulnerabilities,
  getComponentVersions,
  getComponentDependencies,
} from 'GuideRoot/api/componentsBackend';
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
    return (
      <PageLayout>
        <Flex align="center" justify="center" style={{ minHeight: '60dvh' }}>
          <p>Error loading component: {error}</p>
        </Flex>
      </PageLayout>
    );
  }

  if (!state) {
    return (
      <PageLayout>
        <Flex
          direction="column"
          align="center"
          justify="center"
          gap={tokens.space.section}
          style={{ minHeight: '60dvh' }}
        >
          <p>Component not found.</p>
          <Link to="/components">Back to components</Link>
        </Flex>
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
