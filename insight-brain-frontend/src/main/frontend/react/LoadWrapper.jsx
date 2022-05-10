/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxLoadWrapper } from '@sonatype/react-shared-components';

import { Messages } from '../utilAngular/CommonServices';

/**
 * A wrapper component that renders either a loading spinner, an error message, or if neither of those apply,
 * the specified children.  The children may optionally be specified as a function in order to compute their VDOM
 * lazily
 */
export default function LoadWrapper({ error, ...otherProps }) {
  const errorIsReactNode = React.isValidElement(error),
    processedError = errorIsReactNode ? error : Messages.getHttpErrorMessage(error);

  return <NxLoadWrapper {...otherProps} error={processedError} />;
}

LoadWrapper.propTypes = {
  ...NxLoadWrapper.propTypes,
  error: PropTypes.oneOfType([PropTypes.string, PropTypes.node, PropTypes.instanceOf(Error), PropTypes.object]),
};
