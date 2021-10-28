/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import PropTypes from 'prop-types';
import { IqPageFooter } from '../../react/IqPageFooter';
import { PaginationLink } from './PaginationLink';
import { NxFontAwesomeIcon, NxTextLink } from '@sonatype/react-shared-components';
import { faChevronLeft } from '@fortawesome/free-solid-svg-icons';

export const ComponentDetailsFooter = ({
  prev,
  next,
  currentPage,
  pageCount,
  offspringComponentName,
  backToOffspringOnClick,
  ...props
}) => {
  const footerContent = offspringComponentName ? (
    <NxTextLink onClick={() => backToOffspringOnClick(prev)}>
      <NxFontAwesomeIcon icon={faChevronLeft} />
      <span className="component-details-footer__back-to-component">Back to </span>
      <span>{`${offspringComponentName} component`}</span>
    </NxTextLink>
  ) : (
    <Fragment>
      <PaginationLink href={prev} text="Previous Component" direction="prev" />
      {currentPage && pageCount && <PaginationCounter currentPage={currentPage} pageCount={pageCount} />}
      <PaginationLink href={next} text="Next Component" />
    </Fragment>
  );

  return (
    <IqPageFooter id="component-details-footer" {...props}>
      {footerContent}
    </IqPageFooter>
  );
};

export const ComponentDetailsFooterPropTypes = {
  next: PropTypes.string,
  prev: PropTypes.string,
  currentPage: PropTypes.number,
  pageCount: PropTypes.number,
  offspringComponentName: PropTypes.string,
  backToOffspringOnClick: PropTypes.func,
};
ComponentDetailsFooter.propTypes = {
  ...ComponentDetailsFooterPropTypes,
};

export const PaginationCounter = ({ currentPage, pageCount }) => (
  <div className="iq-page-counter">
    {currentPage.toLocaleString()} of {pageCount.toLocaleString()}
  </div>
);

PaginationCounter.propTypes = {
  currentPage: PropTypes.number.isRequired,
  pageCount: PropTypes.number.isRequired,
};
