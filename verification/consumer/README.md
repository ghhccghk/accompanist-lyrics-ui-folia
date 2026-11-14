# Independent artifact consumer

This directory is a separate Gradle root. It consumes Maven coordinates and has
no source/project/includeBuild dependency on the library. It can be copied to a
different directory and built using a compatible Gradle 9.5.0 installation/wrapper.

From the repository root, with JDK 21 installed and ANDROID_HOME set, run:

```powershell
./scripts/verify-consumer.ps1
```

The script builds a file Maven repository under root build/local-maven, then
runs the independent JVM Compose rendering/callback test and Android compilation.
It checks LICENSE, NOTICE, third-party inventory and source ledger in the current
snapshot JARs, AAR and source archives. It never pushes or publishes remotely.

Standalone command (supply your artifact repository URI):

```powershell
gradle -PfoliaRepository=file:///path/to/local-maven jvmTest compileAndroidMain
```

The tested coordinate is `com.mocharealm.accompanist:lyrics-ui-folia:0.1.0-SNAPSHOT`.
Android compilation is not device execution. External binary distribution and a
complete transitive notice audit remain separate delivery requirements.
