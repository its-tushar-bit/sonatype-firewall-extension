/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { IqPageFooter } from '../../react/IqPageFooter';
import { PaginationLink } from './PaginationLink';

export const ComponentDetailsFooter = ({ prev, next, currentPage, pageCount, ...props }) => (
  <IqPageFooter className="component-details-footer" {...props}>
    <PaginationLink href={prev} text="Previous Component" direction="prev" />
    {currentPage && pageCount && <PaginationCounter currentPage={currentPage} pageCount={pageCount} />}
    <PaginationLink href={next} text="Next Component" />
  </IqPageFooter>
);

export const propTypes = {
  next: PropTypes.string,
  prev: PropTypes.string,
  currentPage: PropTypes.number,
  pageCount: PropTypes.number,
};
ComponentDetailsFooter.propTypes = propTypes;

export const PaginationCounter = ({ currentPage, pageCount }) => (
  <div className="iq-page-counter">
    {currentPage.toLocaleString()} of {pageCount.toLocaleString()}
  </div>
);

PaginationCounter.propTypes = {
  currentPage: PropTypes.number.isRequired,
  pageCount: PropTypes.number.isRequired,
};
