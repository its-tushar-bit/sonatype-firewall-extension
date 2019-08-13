/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
export default function getThreatColor(threatLevel) {
    return threatLevel > 7 ? 'red' :
        threatLevel > 3 ? 'orange' :
        threatLevel > 1 ? 'yellow' :
        threatLevel > 0 ? 'darkblue' :
        'blue';
};
