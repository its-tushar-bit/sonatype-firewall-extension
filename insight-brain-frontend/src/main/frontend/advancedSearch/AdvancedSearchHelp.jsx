/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useSelector } from 'react-redux';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';
import { NxCollapsibleItems, NxTextLink, useToggle } from '@sonatype/react-shared-components';

export default function AdvancedSearchHelp() {
  const [isOpen, toggleIsOpen] = useToggle(false);
  const isSbomManager = useSelector(selectIsSbomManager);
  const prodUrl = 'https://links.sonatype.com/products';
  const searchDocUrl = prodUrl + (isSbomManager ? '/sbom/docs/search' : '/nxiq/doc/advanced-search');

  const helpDiv = (
    <div id="advanced-search-help-container">
      {helpRow('CVE-2019-7619', 'Find a specific vulnerability')}
      {helpRow(
        'CVE-2019-*',
        'Search by vulnerability (starts with a specific value. * here means any number of any characters)'
      )}
      {helpRow('applicationName:nexus*', 'Search by application name')}
      {helpRow('policyViolationPolicyName:"License-Copyleft"', 'Find components violating a specific policy')}
      {helpRow('componentLicenseThreatLevel:[8 TO 10]', 'Find components with high-threat licenses')}
      {helpRow(
        'policyViolationThreatCategory:license AND policyViolationWaiverStatus:"Active"',
        'Find license policy violations with active waivers'
      )}
      {!isSbomManager && (
        <>
          {helpRow(
            'applicationName:nexus* AND itemType:APPLICATION',
            'Search by application name focused on application documents'
          )}
          {helpRow(
            'applicationName:nexus* AND itemType:SECURITY_VULNERABILITY',
            'Search by application name focused on security vulnerabilities'
          )}
          {helpRow(
            'applicationName:Nexus* AND vulnerabilityStatus:Open',
            'Search by application name focused on *open* security vulnerabilities'
          )}
          <div className="iq-adv-search-help__note">
            Note: Do not need <i>itemType:SECURITY_VULNERABILITY</i> anymore, since vulnerabilityStatus:Open will
            already return search results that are of item type <i>SECURITY_VULNERABILITY</i>.
          </div>
          {helpRow(
            'itemType:SECURITY_VULNERABILITY AND componentFormat:(a-name OR npm) AND' +
              ' vulnerabilityStatus:(Acknowledged OR "Not Applicable")',
            'Search specific components with specific state'
          )}
          {helpRow(
            'componentFormat:(a-name npm) AND vulnerabilityStatus:(Acknowledged "Not Applicable")',
            'Search specific components with specific state (alternative version)'
          )}
          <div className="iq-adv-search-help__note">
            Tip: Both queries above are equivalent, they return the same result. It is a good example of not actually
            requiring the itemType. OR is the default operator so it can be omitted.
          </div>
        </>
      )}
      <p className="nx-text--advanced-search-help-row">
        <span className="iq-adv-search-help__explanation">
          Also watch out for special characters that need escaping e.g. ( ) and “ “
        </span>
      </p>
      <p>
        <span className="iq-adv-search-help__explanation">
          Read additional{' '}
          <NxTextLink external href={searchDocUrl}>
            documentation
          </NxTextLink>
        </span>
        <span className="iq-adv-search-help__explanation">
          Send feedback to the{' '}
          <NxTextLink external href="https://links.sonatype.com/products/nxiq/feedback/advanced-search">
            community board
          </NxTextLink>
        </span>
      </p>
    </div>
  );

  return (
    <div className="nx-container--advanced-search-help-container">
      <NxCollapsibleItems isOpen={isOpen} onToggleCollapse={toggleIsOpen} triggerContent="Search query examples">
        <NxCollapsibleItems.Child>{helpDiv}</NxCollapsibleItems.Child>
      </NxCollapsibleItems>
    </div>
  );

  function helpRow(example, explanation) {
    return (
      <p>
        <span className="iq-adv-search-help__example">{example}</span>
        <span className="iq-adv-search-help__explanation">{explanation}</span>
      </p>
    );
  }
}
