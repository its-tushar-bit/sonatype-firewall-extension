/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxLoadError } from '@sonatype/react-shared-components';
import { Messages } from '../utilAngular/CommonServices';

/**
 * A wrapper component that renders standardized DOM for error messages, optionally with a retry button.
 */
export default function LoadError(props) {
  return <NxLoadError {...props} error={Messages.getHttpErrorMessage(props.error)} />;
}

LoadError.propTypes = {
  ...NxLoadError.propTypes,
  error: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Error), PropTypes.object]),
};
