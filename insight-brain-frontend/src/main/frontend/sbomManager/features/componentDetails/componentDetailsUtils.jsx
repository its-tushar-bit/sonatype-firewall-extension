/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { toUpper } from 'ramda';
import {
  NxErrorStatusIndicator,
  NxIntermediateStatusIndicator,
  NxNegativeStatusIndicator,
  NxPositiveStatusIndicator,
} from '@sonatype/react-shared-components';
import React from 'react';

export const transformJustification = (justification) =>
  justification ? justification.replace(/_/g, ' ').replace(/^\w/, toUpper) : '';

export const analysisStatusIndicator = (status, isCopy) => {
  switch (status) {
    case 'resolved':
      return (
        <NxPositiveStatusIndicator className={isCopy && 'sbom-manager-cdp-vulnerabilities-tile__copy-status'}>
          {transformAnalysisStatus(status)}
        </NxPositiveStatusIndicator>
      );
    case 'resolved_with_pedigree':
      return (
        <NxPositiveStatusIndicator className={isCopy && 'sbom-manager-cdp-vulnerabilities-tile__copy-status'}>
          {transformAnalysisStatus(status)}
        </NxPositiveStatusIndicator>
      );
    case 'exploitable':
      return (
        <NxErrorStatusIndicator className={isCopy && 'sbom-manager-cdp-vulnerabilities-tile__copy-status'}>
          {transformAnalysisStatus(status)}
        </NxErrorStatusIndicator>
      );
    case 'in_triage':
      return (
        <NxNegativeStatusIndicator
          className={
            'sbom-manager-cdp-vulnerabilities-tile__intriage-status' +
            (isCopy ? ' sbom-manager-cdp-vulnerabilities-tile__copy-status' : '')
          }
        >
          {transformAnalysisStatus(status)}
        </NxNegativeStatusIndicator>
      );
    case 'false_positive':
      return (
        <NxNegativeStatusIndicator className={isCopy && 'sbom-manager-cdp-vulnerabilities-tile__copy-status'}>
          {transformAnalysisStatus(status)}
        </NxNegativeStatusIndicator>
      );
    case 'not_affected':
      return (
        <NxIntermediateStatusIndicator className={isCopy && 'sbom-manager-cdp-vulnerabilities-tile__copy-status'}>
          {transformAnalysisStatus(status)}
        </NxIntermediateStatusIndicator>
      );
    default:
      return <span>Unannotated</span>;
  }
};

export const transformAnalysisStatus = (status) => {
  switch (status) {
    case 'resolved':
      return 'Resolved';
    case 'resolved_with_pedigree':
      return 'Resolved with Pedigree';
    case 'exploitable':
      return 'Exploitable';
    case 'in_triage':
      return 'In Triage';
    case 'false_positive':
      return 'False Positive';
    case 'not_affected':
      return 'Not Affected';
  }
};
