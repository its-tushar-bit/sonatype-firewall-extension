/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function useGetIntegrationsLink(sectionName) {
  const uiRouterState = useRouterState();

  return uiRouterState.href(`developer.dashboard.${sectionName}`);
}
