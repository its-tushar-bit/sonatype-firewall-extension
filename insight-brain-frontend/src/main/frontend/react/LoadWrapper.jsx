import React from 'react';
import * as PropTypes from 'prop-types';
import { NxLoadWrapper } from '@sonatype/react-shared-components';

import { Messages } from '../util/CommonServices';

/**
 * A wrapper component that renders either a loading spinner, an error message, or if neither of those apply,
 * the specified children.  The children may optionally be specified as a function in order to compute their VDOM
 * lazily
 */
export default function LoadWrapper(props) {
  return <NxLoadWrapper { ...props } error={Messages.getHttpErrorMessage(props.error)} />;
}

LoadWrapper.propTypes = {
  ...NxLoadWrapper.propTypes,
  error: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Error), PropTypes.object])
};
