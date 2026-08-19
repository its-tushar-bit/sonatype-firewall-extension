/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import PropTypes from 'prop-types';
import { IqPageFooter } from '../../react/IqPageFooter';
import { PaginationLink } from './PaginationLink';

export const ComponentDetailsFooter = ({ prev, next, currentPage, pageCount, ...props }) => {
  const footerContent = (
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
