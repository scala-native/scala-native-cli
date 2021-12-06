scalaVersion := "2.12.15"
val nativeVersion = "0.4.1"
val cliVersion = nativeVersion

inThisBuild(
  Def.settings(
    organization := "org.scala-native",
    version := cliVersion
  )
)

lazy val cli = project
  .in(file(s"cli/version_${nativeVersion}"))
  .settings(
    moduleName := "scala-native-cli",
    scalacOptions += "-Ywarn-unused:imports",
    libraryDependencies += "org.scala-native" %% "tools" % nativeVersion,
    libraryDependencies += "com.github.alexarchambault" %% "case-app" % "2.1.0-M9",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % Test,
    assembly / assemblyJarName := "scala-native-cli.jar", // Used for integration tests.
    Compile / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "shared/src/main/scala",
    Test / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "shared/src/test/scala"
  )

// Meant to resolve classpath dependencies, provide compiled nir
// and a seperate environment for integration tests
lazy val cliIntegration = project
  .in(file("scala-native-cli-integration-test-runner"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "scala-native-cli-integration-test-runner",
    scriptedLaunchOpts := {
      val cliPath =
        (cli / Compile / packageBin / artifactPath).value.toPath.getParent.toString + "/scala-native-cli.jar"
      scriptedLaunchOpts.value ++
        Seq(
          "-Xmx1024M",
          "-Dplugin.version=" + version.value,
          "-Dscala-native-cli=" + cliPath,
          "-Dscala-native-cli-native-version=" + nativeVersion
        )
    },
    addSbtPlugin("org.portable-scala" % "sbt-platform-deps" % "1.0.0"),
    sbtTestDirectory := (ThisProject / baseDirectory).value / "src/test",
    // publish the other projects before running scripted tests.
    scriptedDependencies := {
      scriptedDependencies
        .dependsOn(
          cli / assembly
        )
        .value
    },
    scriptedBufferLog := false
  )
  .dependsOn(cli)
