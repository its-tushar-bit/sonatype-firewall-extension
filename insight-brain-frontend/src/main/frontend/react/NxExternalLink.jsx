/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import {NxFontAwesomeIcon} from '@sonatype/react-shared-components';
import {faExternalLink} from '@fortawesome/pro-regular-svg-icons';

export default function NxExternalLink({href, children}) {
  return (
    <a className="iq-external-link" target="_blank" rel="noopener noreferrer" href={href}>
      {children}&nbsp;<NxFontAwesomeIcon icon={faExternalLink}/>
    </a>
  );
}
NxExternalLink.propTypes = {
  href: PropTypes.string.isRequired,
  children: PropTypes.any.isRequired
};
