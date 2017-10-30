/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/*
 * ZeroClipboard doesn't work unless it's been attached to the expected global variable
 * (https://stackoverflow.com/a/21408664).
 *
 * This file does that and also configures the swf file location
 */
import ZeroClipboard from 'zeroclipboard';
import zeroClipboardSwfPath from 'zeroclipboard/ZeroClipboard.swf';

window.ZeroClipboard = ZeroClipboard;

ZeroClipboard.config({ moviePath: zeroClipboardSwfPath });

export default ZeroClipboard;
