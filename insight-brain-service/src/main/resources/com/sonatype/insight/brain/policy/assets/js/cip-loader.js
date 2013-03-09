/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global $, window, CLM */
(function ()
{
    'use strict';

    $.extend( true, window, {
        'CLM': {
            'path': '../brain/'
        }
    } );

    var head = $( 'head' ), scripts = ['assets/angular/angular-1.0.5.min.js', 'policy-assets/js/cip-label-editor.js',
        'policy-assets/js/cip-policy-violations.js'];

    $.each( scripts, function ( key, script )
    {
        $( '<script></script>' ).attr( 'src', CLM.path + script ).appendTo( head );
    } );
}());