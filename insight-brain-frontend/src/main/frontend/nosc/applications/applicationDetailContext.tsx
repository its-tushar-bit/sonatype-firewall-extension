/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { createContext, useContext } from 'react';
import type { ApplicationDTO } from './applicationDetailTypes';
import type { TileStatus } from 'MainRoot/nosc/dashboard/useTile';

/** Shell-level data not stored in the applicationDetail Redux slice (CLM-40901). */
export interface ApplicationDetailShellContextValue {
  readonly publicId: string;
  readonly appData: ApplicationDTO | undefined;
  readonly appStatus: TileStatus;
  readonly appRetry: () => void;
  readonly applicationInternalId: string | undefined;
  readonly retryReports: () => void;
  readonly retryPolicy: () => void;
  readonly retryRaw: () => void;
}

const ApplicationDetailShellContext = createContext<ApplicationDetailShellContextValue | null>(
  null,
);

export function ApplicationDetailShellProvider({
  value,
  children,
}: {
  readonly value: ApplicationDetailShellContextValue;
  readonly children: React.ReactNode;
}): JSX.Element {
  return (
    <ApplicationDetailShellContext.Provider value={value}>
      {children}
    </ApplicationDetailShellContext.Provider>
  );
}

export function useApplicationDetailShellContext(): ApplicationDetailShellContextValue {
  const ctx = useContext(ApplicationDetailShellContext);
  if (!ctx) {
    throw new Error('useApplicationDetailShellContext must be used within ApplicationDetailShellProvider');
  }
  return ctx;
}
