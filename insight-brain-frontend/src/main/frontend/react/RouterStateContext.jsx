/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

/*
 * Context to propagate the angular ui router inside deep nested components, without propagating this as props
 */
const RouterStateContext = React.createContext({ href: () => null });
export default RouterStateContext;

export const routerPropType = PropTypes.shape({
  href: PropTypes.func.isRequired
});

