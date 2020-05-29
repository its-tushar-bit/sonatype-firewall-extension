<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# Performance Automation
The performance automation scripts are for Linux hosts like those in our CI environment, but they
also work for macOS and Windows.

# Environment configuration
To configure the environment, do the following:
* Install the command line tool `psql` that is included with PostgreSQL
* Install Python 3.7
* Install PIP (Python package installer)
* Install [Pipenv](https://pipenv.pypa.io/en/latest/) (dependencies management)
* Navigate in the terminal to the folder `nexus-iq-tools/automation`
* Run the following command to install the dependencies: `pipenv sync`
* Activate the virtual environment with: `pipenv shell`

# Test environment configuration
Generate the JARs for Insight-Brain-Service and Nexus-IQ-Tools by building the project. Then run the
following to test the configuration:
```bash
python run_performance_eval.py \
    -p sample-profile.json \
    -iq ../../insight-brain-service/target/insight-brain-service-*-SNAPSHOT-server.jar \
    -tools ../target/nexus-iq-tools-*-SNAPSHOT.jar \
    -lic /path/to/license.lic
```
The previous command will run the performance test and at the end store the results in a file inside
this folder that begins with the name `perf_results`.

# Run parameters
To see the list of available parameters, run `python run_performance_eval.py` without any
parameters. To see more details run `python run_performance_eval.py --help`.

## Test profile
The profile has the following structure:
```json
{
    "iq_data": {
        "dataset": "small",
        "data_path": "S3://iq-perf-datasets"
    },
    "iq_server": {
        "run_server" : {
            "params" : [],
            "java_opts" : ["-Xms16g", "-Xmx16g", "-XX:+UseG1GC", "-Ddw.dbCacheSizePercent=50"]
        }
    },
    "iq_tools": {
        "shift_db" : {
            "params" : [],
            "java_opts" : ["-Xms16g", "-Xmx16g"]
        },
        "generate_urls" : {
            "params" : ["-u", "target_urls.json"],
            "java_opts" : ["-Xms16g", "-Xmx16g"]
        },
        "run_test" : {
            "params" : [],
            "java_opts" : ["-Xms1024m", "-Xmx2048m", "-Dlogback.configurationFile=../../logback.xml"]
        }
    },
    "postgres": {
        "hostname": "localhost",
        "port": 5432,
        "username": "postgres",
        "password": "postgres",
        "database": "postgres"
    }
}
```
Here is an explanation of some values:  

### iq_data.dataset
This is the size of the database used. One of the following values should be used: `small`,
`medium`, and `large`.

### iq_data.data_path
This is the path to where the datasets reside. This value can be a local path like
`/home/some-user/datasets` or an S3 path like `S3://iq-perf-datasets`. When using a local path the
following file structure should be followed:
```
datasets
+-- small
|   +-- h2*.zip
|   +-- postgres*.zip
+-- medium
|   +-- h2*.zip
|   +-- postgres*.zip
+-- large
|   +-- h2*.zip
|   +-- postgres*.zip
```
The folders inside `datasets` should be named `small`, `medium`, and `large`. The files inside these
folders should follow the patterns `h2*.zip` or `postgres*.zip`. For the case of H2 the uncompressed
content is the IQ Server data folder with the ODS database. For the case of PostgreSQL the
uncompressed content is an SQL dump containing all schemas
(ODS, aggregation, DM, third_party_scans).

**Note**: If the dataset targeted is on S3, environment variables or credentials file must be
configured. For more information check
https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-configure.html.

### iq_server.run_server.params
These are parameters passed to IQ Server. For example `server` and `config.yml`.

### iq_server.run_server.java_opts
These are parameters for the Java Virtual Machine. Follow the examples in 
`nexus-iq-tools/testsuite` and adjust for your environment.

### iq_tools.shift_db
These are parameters passed to IQ Tools when running a date shifting operation.

### iq_tools.generate_urls
These are parameters passed to IQ Tools when generating the URLs for testing. Here the `params`
field allows to set the parameter `-u` for custom URLs to test. Follow the example here:
`nexus-iq-tools/testsuite/target_urls.json`. To have consistent results set the values `minRuns` and
`maxRuns` equal.

### iq_tools.run_test
These are parameters passed to IQ Tools when running the performance test. Here is useful to set a
custom configuration for Logback to log more exceptions. This need for a custom configuration is
going to be addressed in a future version of IQ Tools. Follow the example here:
`nexus-iq-tools/testsuite/logback.xml`.

### postgres
This sets the necessary connection parameters to PostgreSQL for this script. This field is not
necessary when using H2.

### More
For more examples open the `nexus-iq-tools/testsuite` folder and look at the profiles used in
Jenkins.

## H2 parameters
To run a test with an H2 database the following template can be used:
```bash
python run_performance_eval.py \
    -p <your-profile.json> \
    -iq ../../insight-brain-service/target/insight-brain-service-*-SNAPSHOT-server.jar \
    -tools ../target/nexus-iq-tools-*-SNAPSHOT.jar \
    -lic </path/to/the/license> \
    -k
```

## PostgreSQL parameters
To run a test with a PostgreSQL database the following template can be used:
```bash
python run_performance_eval.py \
    -p <your-profile.json> \
    -iq ../../insight-brain-service/target/insight-brain-service-*-SNAPSHOT-server.jar \
    -tools ../target/nexus-iq-tools-*-SNAPSHOT.jar \
    -lic </path/to/the/license> \
    --use-postgres \
    -k
```
An H2 dataset can also be used for PostgreSQL by migrating it. Use the `--migrate-h2-to-postgres`
parameter for that:
```bash
python run_performance_eval.py \
    -p <your-profile.json> \
    -iq ../../insight-brain-service/target/insight-brain-service-*-SNAPSHOT-server.jar \
    -tools ../target/nexus-iq-tools-*-SNAPSHOT.jar \
    -lic </path/to/the/license> \
    --use-postgres \
    --migrate-h2-to-postgres \
    -k
```