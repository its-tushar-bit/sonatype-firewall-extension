/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createContext, useContext } from 'react';
import type { ReactNode } from 'react';
import type { OwnerAdapter } from './OwnerAdapter';

/**
 * React context carrying the active {@link OwnerAdapter}. Modelled on
 * `@guide/ui-core`'s `NavigationProvider` / `GatingProvider`: the host wires a
 * concrete adapter once near the app root, and the picker consumes it via
 * {@link useOwnerAdapter} without ever instantiating one itself. Keeping the
 * adapter out of the component is what lets the picker be lifted to
 * `@guide/ui-core` later with only an import-path change.
 */
const OwnerAdapterContext = createContext<OwnerAdapter | null>(null);

export function OwnerAdapterProvider({
  adapter,
  children,
}: {
  adapter: OwnerAdapter;
  children: ReactNode;
}) {
  return <OwnerAdapterContext.Provider value={adapter}>{children}</OwnerAdapterContext.Provider>;
}

export function useOwnerAdapter(): OwnerAdapter {
  const adapter = useContext(OwnerAdapterContext);
  if (!adapter) {
    throw new Error('useOwnerAdapter must be used within an OwnerAdapterProvider');
  }
  return adapter;
}
