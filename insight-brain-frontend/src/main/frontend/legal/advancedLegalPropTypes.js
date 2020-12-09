/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as PropTypes from 'prop-types';

export const componentPropType = PropTypes.shape({
  displayName: PropTypes.string.isRequired,
  licenseLegalData: PropTypes.shape({
    copyrights: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired
  }).isRequired
});
