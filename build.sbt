import scala.util.Random

name in ThisBuild := "toh-lagom"
scalaVersion in ThisBuild := "2.13.1"
organization in ThisBuild := "com.chariotsolutions"
maintainer in ThisBuild := "lkorogodski@chariotsolutions.com"
version in ThisBuild := sys.env.getOrElse("TOH_VERSION", "1.0.0")

scalacOptions in ThisBuild ++= Seq(
  "-language:implicitConversions",
  "-language:postfixOps"
)

// These settings will only be used when running in the Dev mode.
// Disable the embedded Cassandra service and use AWS MCS instead.
// Disable the embedded Kafka server, too.
lagomKafkaEnabled in ThisBuild := false
lagomCassandraEnabled in ThisBuild := false
lagomUnmanagedServices in ThisBuild := Map(
  "cas_native" -> s"https://${CassandraUtils.contactPoint(AmazonUtils.awsRegion)}:${CassandraUtils.port}"
)

val kubernetesApi = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.5"
val lagomTestKit = "com.lightbend.lagom" %% "lagom-scaladsl-testkit" % "1.6.1"
val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.3"
val postgresql = "org.postgresql" % "postgresql" % "42.2.10"
val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1"

lazy val `toh-lagom` = (project in file("."))
  .aggregate(`toh-lagom-api`, `toh-lagom-impl`)

lazy val `toh-lagom-api` = (project in file("toh-lagom-api"))
  .settings(libraryDependencies ++= Seq(lagomScaladslApi))

lazy val `toh-lagom-impl` = (project in file("toh-lagom-impl"))
  .enablePlugins(LagomScala, EcrPlugin)
  .dependsOn(`toh-lagom-api`)
  .configs(IntegrationTest)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslPersistenceJdbc,
      lagomScaladslAkkaDiscovery,
      kubernetesApi % Runtime,
      postgresql % Runtime,
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
  .settings(
    packageName in Docker := (name in ThisBuild).value,
    dockerExposedPorts in Docker := Seq(9000, 9008, 8558, 2552, 25520),
    mappings in Universal += file("cassandra_truststore.jks") -> "cassandra_truststore.jks",
    javaOptions in Universal ++= Seq(
      "-Dpidfile.path=/dev/null",
      "-Dconfig.resource=production.conf",
      s"-Dplay.http.secret.key=${Random.alphanumeric.take(40).mkString}",
      s"-Dcassandra.default.authentication.username=${AmazonUtils.cassandraCredentials.username}",
      s"-Dcassandra.default.authentication.password=${AmazonUtils.cassandraCredentials.password}",
      s"""-Ddb.default.url=${sys.env.getOrElse("POSTGRESQL_URL", "jdbc:postgresql://localhost/toh_lagom")}""",
      s"-Ddb.default.username=${AmazonUtils.postgresqlCredentials.username}",
      s"-Ddb.default.password=${AmazonUtils.postgresqlCredentials.password}"
    )
  )
  .settings(
    region in Ecr := AmazonUtils.awsRegion,
    repositoryTags in Ecr ++= Seq(version.value),
    repositoryName in Ecr := (packageName in Docker).value,
    login in Ecr := ((login in Ecr) dependsOn (createRepository in Ecr)).value,
    push in Ecr := ((push in Ecr) dependsOn (publishLocal in Docker, login in Ecr)).value,
    localDockerImage in Ecr := s"${(packageName in Docker).value}:${(version in Docker).value}"
  )

lazy val initializeSchema = taskKey[Unit]("Initializes Cassandra schema")
initializeSchema := CassandraUtils.initializeSchema()
