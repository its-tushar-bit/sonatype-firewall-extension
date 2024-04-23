/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  NxH2,
  NxLoadWrapper,
  NxSmallThreatCounter,
  NxTable,
  NxTextLink,
  NxTile,
} from '@sonatype/react-shared-components';

import './RecentlyImportedSbomsTile.scss';

export default function RecentlyImportedSboms() {
  const doLoad = () => {};
  const sortDir = 'asc';

  const sboms = [
    {
      name: 'MuseDev',
      version: '0.0.2',
      fileFormat: 'SPDX',
      importDate: '2024-07-07 19-05-33',
      vulnerabilitiesInfo: {
        criticalCount: 12,
        severeCount: 5,
        moderateCount: 5,
        lowCount: 6,
        noneCount: 9,
        unspecifiedCount: 9,
      },
    },
  ];

  const sortByName = () => {};

  const sbomsRows = sboms.map((sbom, index) => (
    <NxTable.Row key={index}>
      <NxTable.Cell>{sbom.name}</NxTable.Cell>
      <NxTable.Cell>
        <NxTextLink>{sbom.version}</NxTextLink>
      </NxTable.Cell>
      <NxTable.Cell>{sbom.fileFormat}</NxTable.Cell>
      <NxTable.Cell>{sbom.importDate}</NxTable.Cell>
      <NxTable.Cell>
        <NxSmallThreatCounter {...sbom.vulnerabilitiesInfo} />
      </NxTable.Cell>
    </NxTable.Row>
  ));

  return (
    <NxLoadWrapper retryHandler={doLoad}>
      <NxTile id="recently-imported-sboms-tile" className="sbom-manager-recently-imported-sboms-tile">
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Recently Imported SBOMs</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Header>
        <NxTile.Content>
          <NxTable id="recently-imported-sboms-tile-table">
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell isSortable sortDir={sortDir} onClick={() => sortByName()}>
                  Application Name
                </NxTable.Cell>
                <NxTable.Cell>Version</NxTable.Cell>
                <NxTable.Cell>File Format</NxTable.Cell>
                <NxTable.Cell>Import Date</NxTable.Cell>
                <NxTable.Cell>Vulnerabilities</NxTable.Cell>
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body>{sbomsRows}</NxTable.Body>
          </NxTable>
        </NxTile.Content>
      </NxTile>
    </NxLoadWrapper>
  );
}
