/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { faChevronRight, faChevronLeft } from '@fortawesome/free-solid-svg-icons';
import { NxFontAwesomeIcon, NxTextLink } from '@sonatype/react-shared-components';

export const PaginationLink = ({ text, href, direction = 'next' }) => (
  <div className={classnames('iq-pagination-link', { 'iq-pagination-link--disabled': !href })}>
    <NxTextLink className={classnames('nx-text-link', `iq-pagination-link__${direction}`)} href={href}>
      {direction === 'prev' && <NxFontAwesomeIcon icon={faChevronLeft} />}
      <span>{text || direction}</span>
      {direction === 'next' && <NxFontAwesomeIcon icon={faChevronRight} />}
    </NxTextLink>
  </div>
);

PaginationLink.propTypes = {
  href: PropTypes.string,
  text: PropTypes.string,
  direction: PropTypes.oneOf(['next', 'prev']),
};
