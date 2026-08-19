/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as PropTypes from 'prop-types';

export default PropTypes.shape({
  id: PropTypes.string.isRequired,
  url: PropTypes.string.isRequired,
  secretKey: PropTypes.string,
  description: PropTypes.string,
  eventTypes: PropTypes.arrayOf(PropTypes.string),
});
