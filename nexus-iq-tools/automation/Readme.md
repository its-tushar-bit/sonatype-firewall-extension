<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# Performance Automation #

  * The performance automation scripts are intended to be run on linux hosts like those in our CI env.
  * The scripts _may_ work on MacOS and Windows but have had limited testing.

# Requirements #
  * To run locally, you'll need to be running python 3.7 and have pip installed
  * Pipenv is used to share a portable environment
    * 1) ```sudo pip install pipenv```
        - macos: ```brew install pipenv```
        - windows: <in admin terminal> ```pip install pipenv```
    * 2) ```pipenv sync``` - this should install all dependencies

  * To execute the local test you'll need paths to jars for IQ server and the Tools jar
  * If the dataset targeted is on S3, environment variables or credentials files must be configured
  * Execute like this from within the automation folder:
    * ```pipenv run python run_performance_eval.py -p sample-profile.json -iq ../../insight-brain-service/target/insight-brain-service-*-SNAPSHOT-server.jar -tools ../target/nexus-iq-tools-*-SNAPSHOT.jar```
  * To use Postgres use something like this:
    * ```--use-postgres -p /path/to/test-profile.json -iq /path/to/insight-brain-service-server.jar -tools /path/to/nexus-iq-tools.jar -lic /path/to/license.lic```
    * The test profile must contain the connection details for Postgres in the following form:
        ```json
        {
            "iq_data": {},
            "iq_server": {},
            "iq_tools": {},
            "postgres": {
                "hostname": "localhost",
                "port": 5432,
                "username": "postgres",
                "password": "postgres",
                "database": "postgres"
            }
        }
        ```
