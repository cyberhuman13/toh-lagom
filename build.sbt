name in ThisBuild := "toh-lagom"
organization in ThisBuild := "com.chariotsolutions"
scalaVersion in ThisBuild := "2.13.1"
version in ThisBuild := "0.1.0"

scalacOptions in ThisBuild ++= Seq(
  "-language:implicitConversions",
  "-language:postfixOps"
)

lagomKafkaEnabled in ThisBuild := false

val lagomTestKit = "com.lightbend.lagom" %% "lagom-scaladsl-testkit" % "1.6.1"
val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.3"
val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1"

lazy val `toh-lagom` = (project in file("."))
  .aggregate(`toh-lagom-api`, `toh-lagom-impl`)

lazy val `toh-lagom-api` = (project in file("toh-lagom-api"))
  .settings(libraryDependencies ++= Seq(lagomScaladslApi))

lazy val `toh-lagom-impl` = (project in file("toh-lagom-impl"))
  .enablePlugins(LagomScala)
  .dependsOn(`toh-lagom-api`)
  .configs(IntegrationTest)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      macwire % Provided
    )
  )
  .settings(
    Defaults.itSettings,
    TestSettings.forked(IntegrationTest),
    libraryDependencies ++= Seq(
      scalaTest % Test,
      scalaTest % IntegrationTest,
      lagomTestKit % IntegrationTest
    )
  )
