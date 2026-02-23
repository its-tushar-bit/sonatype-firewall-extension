/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from '../router/routerInstance';

/**
 * @deprecated Use `router` from 'MainRoot/router/routerInstance' directly instead.
 * This hook is kept for backwards compatibility with existing components.
 * For tests, mock router.stateService methods directly.
 */
export const useRouterState = () => {
  return router.stateService;
};
