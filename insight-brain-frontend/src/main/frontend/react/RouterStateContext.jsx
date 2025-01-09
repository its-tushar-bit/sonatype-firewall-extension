/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useContext } from 'react';
import * as PropTypes from 'prop-types';

/*
 * Context to propagate the angular ui router inside deep nested components, without propagating this as props
 */
const RouterStateContext = React.createContext({ href: () => null, includes: () => false, get: () => null });
export default RouterStateContext;

export const routerPropType = PropTypes.shape({
  href: PropTypes.func.isRequired,
  includes: PropTypes.func.isRequired,
  get: PropTypes.func.isRequired,
});

export function RouterStateProvider({ value, children }) {
  return <RouterStateContext.Provider value={value}>{children}</RouterStateContext.Provider>;
}

RouterStateProvider.propTypes = {
  value: routerPropType,
  children: PropTypes.node,
};

export const useRouterState = () => {
  const routerState = useContext(RouterStateContext);

  return {
    ...routerState,
    href: (params, others) => {
      if (routerState.includes('firewall') && typeof params === 'string' && !params.startsWith('firewall')) {
        return routerState.href('firewall.' + params, others);
      }

      return routerState.href(params, others);
    },
  };
};
