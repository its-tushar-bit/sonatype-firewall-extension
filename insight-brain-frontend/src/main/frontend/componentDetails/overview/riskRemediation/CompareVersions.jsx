/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { NxTable } from '@sonatype/react-shared-components';

export const CompareVersions = () => {
  return (
    <section className="iq-compare-versions nx-tile">
      <header className="nx-tile-header">
        <h3 className="nx-h3 nx-tile-header__title">Compare Versions TODO</h3>
      </header>
      <div className="nx-tile-content">
        <NxTable>
          <NxTable.Head>
            <NxTable.Row>
              <NxTable.Cell></NxTable.Cell>
              <NxTable.Cell>CURRENT</NxTable.Cell>
              <NxTable.Cell>SELECTED</NxTable.Cell>
            </NxTable.Row>
          </NxTable.Head>
          <NxTable.Body>
            <NxTable.Row>
              <NxTable.Cell>Version</NxTable.Cell>
              <NxTable.Cell>2.1.2</NxTable.Cell>
              <NxTable.Cell>--</NxTable.Cell>
            </NxTable.Row>
            <NxTable.Row>
              <NxTable.Cell>Highest Policy Threat</NxTable.Cell>
              <NxTable.Cell>10</NxTable.Cell>
              <NxTable.Cell></NxTable.Cell>
            </NxTable.Row>
            <NxTable.Row className="iq-compare-versions__secutiry-category">
              <NxTable.Cell>Security Violation Threat</NxTable.Cell>
              <NxTable.Cell>10</NxTable.Cell>
              <NxTable.Cell></NxTable.Cell>
            </NxTable.Row>
            <NxTable.Row className="iq-compare-versions__secutiry-category">
              <NxTable.Cell>Highest CVSS Score</NxTable.Cell>
              <NxTable.Cell>9</NxTable.Cell>
              <NxTable.Cell></NxTable.Cell>
            </NxTable.Row>
            <NxTable.Row className="iq-compare-versions__legal-category">
              <NxTable.Cell>Legal Violation Threat</NxTable.Cell>
              <NxTable.Cell>10</NxTable.Cell>
              <NxTable.Cell></NxTable.Cell>
            </NxTable.Row>
            <NxTable.Row className="iq-compare-versions__legal-category">
              <NxTable.Cell>Effective License</NxTable.Cell>
              <NxTable.Cell>CDDL-1.1, Generic-Open-Source-Clause, CDDL-2.1 or GPL-2.0-CPE</NxTable.Cell>
              <NxTable.Cell>CDDL-1.1, Generic-Open-Source-Clause, CDDL-2.1 or GPL-2.0-CPE</NxTable.Cell>
            </NxTable.Row>
            <NxTable.Row className="iq-compare-versions__quality-category">
              <NxTable.Cell>Quality Violation Threat</NxTable.Cell>
              <NxTable.Cell>8</NxTable.Cell>
              <NxTable.Cell></NxTable.Cell>
            </NxTable.Row>
            <NxTable.Row className="iq-compare-versions__quality-category">
              <NxTable.Cell>Hygiene Rating</NxTable.Cell>
              <NxTable.Cell>Laggard</NxTable.Cell>
              <NxTable.Cell></NxTable.Cell>
            </NxTable.Row>
            <NxTable.Row>
              <NxTable.Cell>Other Violation Threat</NxTable.Cell>
              <NxTable.Cell>none</NxTable.Cell>
              <NxTable.Cell></NxTable.Cell>
            </NxTable.Row>
          </NxTable.Body>
        </NxTable>
      </div>
    </section>
  );
};
