/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import './_TierTag.scss';

/**
 * Reusable tier tag component for displaying tier badges (e.g., "Pro", "Enterprise").
 *
 * @param {Object} props
 * @param {string} props.children - The text content of the tag (e.g., "Pro", "Enterprise")
 * @param {string} [props.className] - Optional additional CSS class
 */
export default function TierTag({ children, className = '' }) {
  return <span className={`iq-tier-tag ${className}`.trim()}>{children}</span>;
}

TierTag.propTypes = {
  children: PropTypes.node.isRequired,
  className: PropTypes.string,
};
