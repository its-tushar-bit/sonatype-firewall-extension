/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createContext, useContext } from 'react';
import type { ReactNode } from 'react';
import type { EstateComponentDetails } from './estateComponentDetailsApi';

export type EstateComponentHdsStatus = 'loading' | 'ready' | 'error' | 'empty';

export type EstateComponentBlastRadiusCounts = {
  readonly applications?: number;
  readonly organizations?: number;
  readonly violations?: number;
};

export interface EstateComponentDetailShellContextValue {
  readonly componentHash: string;
  readonly hdsStatus: EstateComponentHdsStatus;
  readonly details: EstateComponentDetails | null;
  readonly displayName: string;
  readonly blastRadiusCounts: EstateComponentBlastRadiusCounts;
  readonly retryHds: () => void;
}

const EstateComponentDetailShellContext = createContext<EstateComponentDetailShellContextValue | null>(null);

export function EstateComponentDetailShellProvider({
  value,
  children,
}: {
  readonly value: EstateComponentDetailShellContextValue;
  readonly children: ReactNode;
}): JSX.Element {
  return (
    <EstateComponentDetailShellContext.Provider value={value}>{children}</EstateComponentDetailShellContext.Provider>
  );
}

export function useEstateComponentDetailShellContext(): EstateComponentDetailShellContextValue {
  const ctx = useContext(EstateComponentDetailShellContext);
  if (!ctx) {
    throw new Error('useEstateComponentDetailShellContext requires EstateComponentDetailShellProvider');
  }
  return ctx;
}
