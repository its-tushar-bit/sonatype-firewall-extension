<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Nexus IQ CLI Native Binaries
This page describes what is all needed to produce and maintain native binaries for the Nexus IQ CLI.

*Note: Graal uses the term 'native image' and we are also using the term 'binary'. These are synonymous for an OS-dependant standalone executable.*

# Background
- A native image uses ahead-of-time (AOT) compilation instead of the usual Java just-in-time (JIT) compilation. It is important to understand that things that you normally think of happening at execution time in a JVM, now might be happening at build time. See [build time and run time initialization](#build-time-initialization-and-run-time-initialization) below.
- A native image must be built on its own OS. i.e. you cannot generate a Windows binary from Linux or any other combination. Windows binaries must be built on Windows, Mac on Mac, Linux on Linux.
- A native image can only be built with the GraalVM. You cannot build a native image with a regular JDK.
- The `native-image` application used to build images is installed via the `gu` application included in GraalVM. `gu` is the 'graal updater' which is effectively a mini package manager for Graal.
- We create our native images by specifying the `-jar` argument to the actual Nexus IQ CLI jar file. `native-image` processes the jar and bakes all the classes into the executable.
  - Since our jar is shaded we do not require the `native-image` `-cp` classpath argument to specify other dependencies.
  - Our jar is obfuscated and that is fine as it is all self-contained. That is, the obfuscated code is put into the image as-is.
- Dynamic class loading and proxies present a problem for `native-image`. Since it can't possibly know what might be loaded dynamically, there is opportunity for a class to *not* be baked into the image which would result in a `ClassNotFoundException` if accessed during execution. Therefore there is a configuration file `reflect-config.json` that can be provided to instruct `native-image` to add additional classes into the image that are not directly referenced.
- Resources also present a problem for `native-image` for similar reasons. By default resources are not included in images at all and must be specified by a `resource-config.json` configuration file.
- A very useful [tracing agent](https://www.graalvm.org/docs/reference-manual/native-image/#tracing-agent) is available to assist in configuration file generation. We use this to generate our configuration files by repeatedly executing the IQ CLI jar file with all known invocations to build-up the accessed classes and resources. We can then be more confident that our natives images include all required classes and resources.

## Build time initialization and run time initialization
When creating a native image it is important to understand build and run time initialization.
The important bits from the [docs](https://www.graalvm.org/docs/reference-manual/native-image/#runtime-vs-build-time-initialization) are:
> Building your application into a native image allows you to decide which parts of your application should be run at image build time and which parts have to run at image runtime.

> Sometimes it is beneficial to allow class initialization code to get executed at image build time for faster startup (e.g. if some static fields get initialized to runtime independent data).

The default as of GraalVM 19.0 is that all class-initialization code will be executed at image runtime.
A good example from the GraalVM [blog](https://medium.com/graalvm/updates-on-class-initialization-in-graalvm-native-image-generation-c61faca461f7) is a date field initialized in a static block. In a regular JVM process this would get initialized at JVM runtime. In GraalVM if you choose to initializate it at build time, you would actually bake the date into the binary when that binary was built. However, using build time initialization can result in performance benefits. So choices must be made of when certain classes should be initialized. 
For Nexus IQ CLI everything is runtime except for:
- TrueZip (de.schlichtherle.truezip.rof)
- Logback (ch.qos.logback)
- SLF4j (org.slf4j)
- JGit (org.eclipse.jgit)

# Requirements
- GraalVM. To build the image locally you will need to have the GraalVM installed and active.
  - Important to note that due to this requirement, the native image process is in a Maven module and gated by a profile that will not run by default.
  - [sdkman](https://sdkman.io/) is a great option to have a regular default JDK installed, and then activate GraalVM when necessary. GraalVM is based on OpenJDK so all regular features should work.
   ```
   sdk install 20.1.0.r11-grl
   sdk use java 20.1.0.r11-grl
   gu install native-image
   ```
- Read the docs. No joke. It is invaluable to understand the nuances of building a native image. There are some nuances that are not immediately apparent. Recommendation is to read all the docs in the [links](#important-links) section below.

# Process
## tl;dr
- entire process gated by a Maven profile so regular development will not be affected
- native image configuration file generation happens by running the regular CLI test class against the real CLI jar using the GraalVM native image tracing agent
- native image generation is done using the real jar and these configuration files 
- during CI, the configuration process will happen, then the build process will happen for each supported OS

## Details
- The `nexus-iq-cli-native-image` Maven module encapsulates the entire process for building our native images
- This module will be gated behind a `graal` Maven profile
  - `insight-brain` will be built 'as normal'. As in with a regular JDK/JVM, and no-impact to any developer or build tool.
  - GraalVM will be required to execute this new module.
  - Also, the native image generation takes some time that we don't want added to the regular build. Average during initial testing is TBD seconds.
  
The full process is a three stage process
1. generate the necessary configuration files for `native-image`
1. build the image
1. test the image.

### Configuration
There are four configuration files for `native-image`: `reflect-config.json`, `resource-config`, `jni-config.json`, and `proxy-config.json`.
Read more about these [here](https://www.graalvm.org/reference-manual/native-image/Configuration/). We only need to worry about the resource configuration and reflection configuration. These two files instruct `native-image` on which classes and resources to build into the image.

Graal provides the ability to automatically generate these configs through a Java agent that you attach to the execution of your jar. Since a jar can use/initiatize different classes and resources depending on how it is executed, this agent also has the ability to merge configs from successive runs together.

So what we do is repeatedly invoke the Java Nexus IQ CLI jar file with the native image tracing agent to build up our config files to contain all possible classes and resources used.
     
In order to ensure the greatest coverage, we use the same test class that is used to unit test the IQ CLI. These are run as unit tests (e.g. `surefire`) using the existing IQ Server test framework. This should invoke the IQ CLI with all known parameters and use cases and therefore ensure all classes and resources that can ever be used by it will end up in the configuration files.
     
TODO: If needed, we will/can also have hand-crafted configuration files that will be merged into the generated ones to cover any fringe cases of classes or resources not being picked up by the tracing agent.
     
Notes
- The full test suite for the CLI JAR is re-executed to get full coverage
- Instead of the test suite running against the class as in a normal junit test, we invoke a second full JVM using the [ZeroTurnaround zt-exec](https://github.com/zeroturnaround/zt-exec) library.
  - This is done because we need to use a Java agent to enable the native image tracing
  - Also, we need to run against the obfuscated JAR, not the real classes
- The output of the entire process is native-image configuration files
- The configuration files become the output artifact of this module, to be consumed downstream by the image generation itself
- This output is idempotent
  - That is, if the set of classes and resources used by the CLI throughout the entire test execution process is the same, the output will be the same
  - If it does change, that indicates that new functionality has entered the CLI
- (TBD) The configuration files are committed to the repository so that we can see any changes via git
     
### Generate Binaries
This is executing `native-image` against our CLI jar using the configuration files from the first step. 

Graal provides a [Maven plugin](https://www.graalvm.org/reference-manual/native-image/NativeImageMavenPlugin/) which lets us easily build the native images as part of our build.

The `native-image` options in use are explained below.

(TODO PLACEHOLDER - these will be filled out and fully documented as the work is complete. The following is the current list.)
- `-Dlogback.configurationFile=logback-graal.xml`
- `--verbose`
- `--no-server`
- `--no-fallback`
- `--report-unsupported-elements-at-runtime`
- `--enable-http --enable-https`
- `--allow-incomplete-classpath`
- `-H:+TraceClassInitialization`
- `-H:+ReportExceptionStackTraces`
- `-H:+PrintClassInitialization`
- `-H:+AddAllCharsets`
- `-H:Log=registerResource:`
- `--initialize-at-build-time=de.schlichtherle.truezip.rof,ch.qos.logback,org.slf4j,org.eclipse.jgit`
 
    
### Test the Binaries
After the binaries are built, we run the same test suite on them as are used in the regular IQ CLI unit tests, and 'tests' used to generate the configuration files.

This uses the [ZeroTurnaround zt-exec](https://github.com/zeroturnaround/zt-exec) library to execute the binary with the provided test parameters and assert the proper output.
  
# CI/Jenkins
## tl;dr
- Execute the native image process in parallel to the existing 'extra-tests'
- Execute the `config` module first to generate the configuration files
- Execute the `build` module **in parallel** across supported OSs
  - Supported OSs for initial release are Linux and Windows.
  - Mac is not as easily accomplished since AWS (our build infrastructure) does not support MacOS. We will measure need and implement a solution as necessary. 
- Gather binaries to include in `insight-brain` output artifacts

TODO: Document CI/Jenkins specifics as we build it out

# Maintenance
It is possible that as the IQ CLI application evolves over time that adjustments will be needed to our setup.

The expected use-case would be that a brand new piece of functionality is built, and due to the implementation details some new classes or resources are not included in the native images. This would likely only happen if there was some dynamic loading of some kind.

What is required in a case like this is most likely to add the new use case to the existing test suite of the CLI jar. This will automatically add it to the native image 'test' used for generating configuration files, as well as to the test of the binaries themselves.

# Important Links
- https://www.graalvm.org
- https://www.graalvm.org/reference-manual/native-image/
- http://www.christianwimmer.at/Publications/Wimmer19a/Wimmer19a.pdf