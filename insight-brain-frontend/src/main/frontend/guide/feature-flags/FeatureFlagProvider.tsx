/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { fetchFeatureFlags } from './featureFlagsApi';
import type { FeatureFlag } from './featureFlags';

interface FeatureFlagsContextValue {
  isLoading: boolean;
  isFeatureEnabled: (flag: FeatureFlag) => boolean;
}

const FeatureFlagsContext = createContext<FeatureFlagsContextValue | null>(null);

export function FeatureFlagProvider({ children }: { children: ReactNode }) {
  const [flags, setFlags] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    fetchFeatureFlags()
      .then((result) => {
        if (!cancelled) {
          setFlags(result);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setFlags([]);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const isFeatureEnabled = useCallback(
    (flag: FeatureFlag) => flags.includes(flag),
    [flags]
  );

  const value = useMemo<FeatureFlagsContextValue>(
    () => ({ isLoading, isFeatureEnabled }),
    [isLoading, isFeatureEnabled]
  );

  return (
    <FeatureFlagsContext.Provider value={value}>{children}</FeatureFlagsContext.Provider>
  );
}

export function useFeatureFlags(): FeatureFlagsContextValue {
  const context = useContext(FeatureFlagsContext);
  if (!context) {
    throw new Error('useFeatureFlags must be used within a FeatureFlagProvider');
  }
  return context;
}
