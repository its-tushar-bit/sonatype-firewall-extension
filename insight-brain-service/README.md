<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
Insight Brain Service
=============

A Dropwizard web application used by Clients to manage risk in their own software.
 
## Development

The server can be run for development by running the from the insight-brain-service directory and the InsightBrainService class with the following arguments:

    server src/test/resources/config-dev.yml
    
Reload of CSS/JS resources is supplied by wro4j and the m2e plugin in Eclipse, and by grunt tooling for IDEA users. This is automatic in Eclipse,
but for IDEA installing Node.js and the grunt-cli package is required (example uses brew):

    brew install node
    npm install -g grunt-cli

and you will have to execute the following commands in the insight-brain-service folder:

    npm install
    grunt develop

In both of these cases changes to asset files will be detected and any required massaging will take place automatically. In addition the grunt server
works as a proxy for the running web application and will automatically reload any pages it is serving when changes are ready.
 
 
## Testing

In addition to the standard maven test abilities, jasmine tests can be executed like so:

     #runs jasmine tests for a specific profile in a browser, handy for writing tests
     mvn phantomjs:install jasmine:bdd -P(test-cip|test-brain|test-version-graph) 
     
     #run jasmine tests as a suite
     mvn phantomjs:install jasmine:test -P(test-cip|test-brain|test-version-graph)
     
Each profile represents a logical unit of javascript in a particular execution environment, where different scripts are expected to be delivered to a client. 
Failure to specify a profile will result in fairly ambiguous error messages about missing objects.          
     
## Style Guide

The grunt tooling also provides a style guide that showcases the expected styling for common constructs in the UI. Execute the following command to launch a server hosting the styleguide:

    grunt livingstyle

As new common constructs are added to the UI, they should be codified in the style guide for reference.

## Additional tools

The grunt tooling provides a variety of other tasks to allow for testing and profiling the application, which can be queried by:

    grunt --help

## Misc

* Disable javascript minification using the system property: `-Dwro4j.minimize=false`. 
  
    This property can also set by activating the profiles: `m2e` or `idea`.
