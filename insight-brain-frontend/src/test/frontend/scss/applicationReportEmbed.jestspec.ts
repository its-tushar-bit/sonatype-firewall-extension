/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import fs from 'fs';
import path from 'path';

const FRONTEND = path.resolve(__dirname, '../../../main/frontend');

function useTargets(scssPath: string): Set<string> {
  const content = fs.readFileSync(path.join(FRONTEND, scssPath), 'utf8');
  return new Set([...content.matchAll(/@use\s+'([^']+)'/g)].map((m) => m[1]));
}

const scssUses = useTargets('scss/scss.scss');
const aggregateUses = useTargets('scss/applicationReportEmbed.scss');

describe('applicationReportEmbed.scss', () => {
  // Nexus One embeds applicationReport.* / Component Details without loading scss.scss.
  // Guard the shared partials so License Detections (and siblings) do not silently lose CSS.
  it('includes the application-report / component-details partials scss.scss loads', () => {
    [
      '../applicationReport/applicationReport',
      '../componentDetails/ComponentDetails',
      '../componentDetails/ViolationsTableTile/componentWaivers/componentWaiversPopover',
      '../componentDetails/TransferList/TransferList',
      '../componentDetails/TransferList/TransferListHalf',
      '../componentDetails/overview/occurrencesPopover/occurrencesPopover',
    ].forEach((partial) => {
      expect(scssUses).toContain(partial);
      expect(aggregateUses).toContain(partial);
    });
  });
});
