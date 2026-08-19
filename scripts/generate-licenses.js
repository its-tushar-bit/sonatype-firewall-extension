/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
const fs = require('fs/promises');
const path = require('path');

function escapeSqlString(str) {
  return str.replace(/'/g, "''");
}

(async () => {
  try {
    const response = await fetch('https://clm-staging.sonatype.com/rest/license');
    const json = await response.json();

    const licenses = [...json.licenses].sort((a, b) => a.id.toLowerCase().localeCompare(b.id.toLowerCase()));
    const multiLicenses = [...json.multiLicenses].sort((a, b) => a.id.toLowerCase().localeCompare(b.id.toLowerCase()));

    const multiLicenseMappings = Object.fromEntries(
        Object.entries(json.multiLicenseMappings).sort((a, b) => a[0].toLowerCase().localeCompare(b[0].toLowerCase()))
    );

    let licenseSql = '';
    let multiLicenseSql = '';
    let multiLicenseLicenseSql = '';

    for (const license of licenses) {
      licenseSql += `INSERT INTO license (license_id,shortDisplayName,longDisplayName) VALUES ('${escapeSqlString(license.id)}','${escapeSqlString(license.shortDisplayName)}','${escapeSqlString(license.longDisplayName)}');\n`;
    }

    for (const license of multiLicenses) {
      multiLicenseSql += `INSERT INTO multi_license (multi_license_id,shortDisplayName,longDisplayName) VALUES ('${escapeSqlString(license.id)}','${escapeSqlString(license.shortDisplayName)}','${escapeSqlString(license.longDisplayName)}');\n`;
    }

    for (const [multiId, licenseSet] of Object.entries(multiLicenseMappings)) {
      const sorted = [...licenseSet].sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase()));
      for (const licenseId of sorted) {
        multiLicenseLicenseSql += `INSERT INTO multi_license_license (multi_license_id,license_id) VALUES ('${escapeSqlString(multiId)}','${escapeSqlString(licenseId)}');\n`;
      }
    }

    const brainDm = path.join('..', 'insight-brain-db', 'src', 'main', 'resources', 'db', 'insight_brain_dm');

    await fs.mkdir(brainDm, { recursive: true });

    await fs.writeFile(path.join(brainDm, 'license.sql'), licenseSql, 'utf8');
    await fs.writeFile(path.join(brainDm, 'multi_license.sql'), multiLicenseSql, 'utf8');
    await fs.writeFile(path.join(brainDm, 'multi_license_license.sql'), multiLicenseLicenseSql, 'utf8');

    console.log('SQL files generated successfully.');
  } catch (err) {
    console.error('An error occurred:', err);
  }
})();
