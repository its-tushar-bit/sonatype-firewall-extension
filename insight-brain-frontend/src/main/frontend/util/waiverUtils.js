/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { getFutureDate } from './jsUtil';
import * as PropTypes from 'prop-types';

export const waiverExpirations = [
  { name: 'Never', value: 'never' }, // <select> doesn't handle null values, so use string instead
  { name: '7 Days', value: '7' },
  { name: '14 Days', value: '14' },
  { name: '30 Days', value: '30' },
  { name: '60 Days', value: '60' },
  { name: '90 Days', value: '90' },
  { name: '120 Days', value: '120' },
  { name: 'Custom', value: 'custom' },
];

export const getExpiryTime = (expiration) => {
  if (!expiration) {
    return null;
  }
  return getFutureDate(expiration);
};

export const displayWaiverScope = (waiver) => {
  switch (waiver.scopeOwnerType) {
    case 'root_organization': {
      return 'Root Organization';
    }
    case 'organization': {
      return `Organization - ${waiver.scopeOwnerName}`;
    }
    case 'application': {
      return `Application - ${waiver.scopeOwnerName}`;
    }
  }
  return null;
};

export const waiverType = {
  policyId: PropTypes.string,
  policyName: PropTypes.string,
  policyWaiverId: PropTypes.string,
  scopeOwnerId: PropTypes.string,
  scopeOwnerName: PropTypes.string,
  scopeOwnerType: PropTypes.string,
  hash: PropTypes.string,
  createTime: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  creatorName: PropTypes.string,
  comment: PropTypes.string,
  constraintFacts: PropTypes.array,
  constraintFactsJson: PropTypes.string,
  componentName: PropTypes.string,
};
