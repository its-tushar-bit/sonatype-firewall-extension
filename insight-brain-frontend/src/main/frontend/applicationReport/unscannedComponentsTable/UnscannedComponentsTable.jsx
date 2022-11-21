/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, Fragment } from 'react';
import { useSelector } from 'react-redux';
import { NxP, NxTable } from '@sonatype/react-shared-components';
import { selectUnscannedComponents } from 'MainRoot/applicationReport/applicationReportSelectors';
import ComponentDisplay from 'MainRoot/ComponentDisplay/ReactComponentDisplay';
import { parseOccurrencePathname } from 'MainRoot/componentDetails/componentDetailsUtils';

export default function UnscannedComponentsTable() {
  const unscannedComponents = useSelector(selectUnscannedComponents);
  const [rows, setRows] = useState(unscannedComponents);
  const [sortDir, setSortDir] = useState('desc');

  // use state to sort instead of redux because
  // of the simplicity of the table
  const sortByName = () => {
    if (sortDir === 'asc') {
      setSortDir('desc');
      setRows(rows.slice().sort((a, b) => (a.pathnames[0] > b.pathnames[0] ? 1 : -1)));
    } else {
      setSortDir('asc');
      setRows(rows.slice().sort((a, b) => (a.pathnames[0] > b.pathnames[0] ? -1 : 1)));
    }
  };

  const componentRows = rows.map((component) => {
    const { pathnames } = component;
    const parsedPathNames = pathnames.map(parseOccurrencePathname);
    return parsedPathNames.map(({ dirname, basename }) => {
      return (
        <NxTable.Row key={basename}>
          <NxTable.Cell>
            <ComponentDisplay component={component} />
          </NxTable.Cell>
          <NxTable.Cell>{dirname}</NxTable.Cell>
        </NxTable.Row>
      );
    });
  });

  return (
    <Fragment>
      <NxP>These components were not able to be scanned.</NxP>
      <NxTable id="iq-unscanned-components-table">
        <NxTable.Head>
          <NxTable.Row>
            <NxTable.Cell isSortable sortDir={sortDir} onClick={() => sortByName()}>
              Component
            </NxTable.Cell>
            <NxTable.Cell>Occurrences</NxTable.Cell>
          </NxTable.Row>
        </NxTable.Head>
        <NxTable.Body>{componentRows}</NxTable.Body>
      </NxTable>
    </Fragment>
  );
}
