# Sonatype Licensing and IQ

Sonatype products use cryptographically-signed license files which control whether the product is allowed to be used at
all, what features are enabled, how many users are authorized, etc. This document will give a brief overview of the
major license-related codebases and then dive into specifics about how to generate and manage fake licenses for
automated tests.

The core licensing code resides in a separate repository, sonatype-licensing. The maven modules built by this repo are
relied upon by all non-free Sonatype products.

In IQ, there are a few classes which wrap and modify the behavior of sonatype-licensing, most importantly
CLMLicenseManager. CLMLicenseManager is responsible for calling into sonatype-licensing as well as confirming up-to-date
license details with HDS. Additionally, IQ includes `DatabasePreferencesFactory` which, via dependency injection, gets
injected into the sonatype-licensing flow and changes where the license file is stored and retrieved from (putting it in
the database rather than the on-disk java user preferences store.

HDS too has licensing-related code, both of its own and in libraries that are shared with IQ.

## Key Stores and Signatures
There are multiple cryptographic signatures involved in Sonatype's licensing. First, the license files themselves
contain a signature proving that they were created by Sonatype and have not been tampered with. Additionally, the HDS
REST API that provides license "details" for a given fingerprint signs those details. These two signatures are computed
using different private keys and in fact different algorithms entirely – the code that signs the license files is only
compatible with DSA while the HDS code will only use RSA. Thus, two separate public/private keypairs, in two separate
keystore files, are involved in the whole process. Creating and using test licenses requires managing both keystores.

### The license-signing keys
A DSA public/private keypair suitable for generating license files may be generated using the following command:
```
keytool -genkey -keystore license-signing-keystore.p12 -storepass <password> -alias Nexus -validity 4096 -dname "CN=localhost, OU=Engineering, O=Sonatype, L=Fulton, ST=Maryland, C=MD"
```
Replace `<password>` with a password or your choosing. Note that this will store the keys in a file named
`license-signing-keystore.p12`. The `alias` value is not crucial for this step, but it is for later steps so we might as
well include it for consistency. This keystore will be used to generate licenses, but does not need to be made available
to IQ and HDS.

The IQ and HDS code expects to have access to a keystore containing the _public_ key from the keypair used to sign
licenses, and expects it to be set up a particular way. After generating the keypair using the command above, the
necessary public key store can be generated using the following commands:
```
keytool -export -keystore license-signing-keystore.p12 -alias Nexus -file certificate.cer -storepass <password>
keytool -import -keystore publicKeyStore -storepass <specific-password> -storetype jks -alias Nexus -file certificate.cer
```
These commands will create an intermediate file called `certificate.cer` which contains the public key. That file may be
delete after the second command. The second command will create the file needed by IQ: a keystore containing the public
key, under the alias "Nexus" (it must be that value), stored in a _JKS_ file named "publicKeyStore" (the file must be
located on the classpath at /productlicense/publicKeyStore).

Note that another password is needed here, referred to above as "specific-password". IQ is hardcocded with a particular
password, which one must extract and deobfuscate from
[`com.sonatype.insight.license.model.CLMLicenseBuilder.publicKeyStorePassword`, found in the HDS source
repo](https://github.com/sonatype/hosted-data-services/blob/main/insight-license-model/src/main/java/com/sonatype/insight/license/model/CLMLicenseBuilder.java#L19).
Out of an abundance of caution it must be left as an exercise for the reader to obtain that actual password value.

### The HDS license details keys
In an automated test that tests IQ license handling with an HdsMockServer, the code under test will attempt to verify a
signature on the license details returned by the mocked HDS, which means that the mocked HDS must sign them. The
location in which IQ will look for the certificate for this signature verification is partially
[hardcoded](https://github.com/sonatype/insight-brain/blob/main/insight-brain-service/src/main/java/com/sonatype/insight/brain/product/license/CLMLicenseManager.java#L303)
– it will look in /com/sonatype/insight/brain/product/license/licensing-keystore.p12, and will use the hardcoded key
found in `CLMLicenseManager` to open that keystore. However, the alias of the certificate to use varies depending on the
response from HDS. At the same time, the keystore that HDS uses to sign license details is fully configurable by setting
properties on the `ProductLicenseConfig` bean. Both the keystore path, and the key alias within the keystore, can be
configured on that object. The alias configured here is also the one that will be passed down to IQ, so IQ's certificate
keystore must have an alias that matches.

All that is to say, there are multiple parts here that must be updated in lockstep and which do currently match, so it's
best to avoid touching them. Specifically, when running this sort of automated test, you should set up the mock HDS to
use the private key stored under the "licensing-key-test" alias in classpath:/productlicense/licensing-keystore-hds.p12,
which will cause IQ to use the certificate stored under that same "licensing-key-test" alias in
classpath:/com/sonatype/insight/brain/product/license/licensing-keystore.p12. This key and certificate form a matching
pair.

In case you need to inspect the contents of licensing-keystore-hds.p12, it too uses an obfuscated, hardcoded key, which
can be found in [com.sonatype.insight.productlicense.ProductLicenseSigner.OBFUSCATED_KEYSTORE_PASSWORD](https://github.com/sonatype/hosted-data-services/blob/c1c27fab41150ee0578336fa25150bd9b53f8121/insight-license-util/src/main/java/com/sonatype/insight/productlicense/ProductLicenseSigner.java#L24).

## Generating test licenses
With the license-signing-keystore.p12 and publicKeyStore files set up as described above, one may generate licenses
using the license-creator jar and a `descriptor.xml` file. Several example `descriptor.xml` files can be found in this
directory. These files define the features, user counts, and expiration date of the generated license, as well as
specifying the filename, alias, and passwords to the keystore containing the private key with which to sign the license.
The `<path>` should be that of the keystore containing the private key (`license-signing-keystore.p12` if the
instructions above were follow), and the `<password>` element must be filled in with the password for that keystore.
`<keyPassword>` should also be set to that value, though it is unclear if it is actually used (in the past, JKS
keystores has separate passwords for the store itself and individual keys, but P12 keystores have no such capability).
At any rate, the `<keyPassword>` may not be omitted or left blank without causing errors.

Once your descriptor file is ready, you need a copy of the `license-creator` jar file. This may be obtained by building
the `sonatype-licensing` repo. Then, the license may be generated by running a command like the following in this
directory:
```
java -jar license-creator-cli.jar -alias Nexus -f descriptor.xml -o .
```
This will create a `license.lic` file in the specified directory (the current directory with `-o .`) based on the
information in the specified descriptor file. Note that this command appears to require Java 8 (and no higher) in order
to run successfully.
