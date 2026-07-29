/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Build-time constant injected by esbuild defineConfig
declare const CLM_SERVER_VERSION: string;

// CSS side-effect imports
declare module '*.css';
declare module '@radix-ui/themes/styles.css';

// JS modules without TypeScript declarations
declare module 'MainRoot/react/Footer/Footer' {
  import React from 'react';
  interface FooterProps {
    clmServerVersion?: string;
  }
  const Footer: React.ComponentType<FooterProps>;
  export default Footer;
}

declare module 'MainRoot/reduxConfig/store' {
  import { Store, UnknownAction, ThunkDispatch } from '@reduxjs/toolkit';
  interface AppStore extends Store<unknown, UnknownAction> {
    dispatch: ThunkDispatch<unknown, unknown, UnknownAction>;
  }
  const store: AppStore;
  export default store;
}

declare module 'MainRoot/router/routerInstance' {
  import { UIRouterReact } from '@uirouter/react';
  const router: UIRouterReact;
  export default router;
}

declare module 'MainRoot/utility/axiosConfig' {
  export function attachAxiosInterceptors(): void;
}
