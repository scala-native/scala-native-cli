resolvers ++= Resolver.sonatypeOssRepos("snapshots")

val pluginVersion = System.getProperty("plugin.version")
if (pluginVersion == null)
  throw new RuntimeException(
    """|The system property 'plugin.version' is not defined.
        |Specify this property using the scriptedLaunchOpts -D.""".stripMargin
  )
else {
  addSbtPlugin("org.scala-native" % "sbt-scala-native" % pluginVersion)
}
resolvers ++= sys.props
  .get("scalanative.build.staging.resolvers")
  .toList
  .flatMap(_.split(","))
  .flatMap(Resolver.sonatypeOssRepos(_))
