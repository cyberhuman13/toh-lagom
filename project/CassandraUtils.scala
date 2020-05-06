import scala.util.Try
import java.net.InetSocketAddress
import com.datastax.driver.core.{Cluster, PlainTextAuthProvider}

object CassandraUtils {
  lazy val contactPoint = s"""cassandra.${sys.env("AWS_REGION")}.amazonaws.com"""
  val keyspace = "toh_lagom"
  val port = 9142

  def initializeSchema(): Unit = {
    System.setProperty("javax.net.ssl.trustStore", "../cassandra_truststore.jsk")
    System.setProperty("javax.net.ssl.trustStorePassword", "amazon")
    val credentials = AmazonUtils.cassandraCredentials

    val authProvider = new PlainTextAuthProvider(credentials.username, credentials.password)
    val address = new InetSocketAddress(contactPoint, port)
    val cluster = Cluster.builder.addContactPoints(address.getAddress)
      .withPort(port).withAuthProvider(authProvider).withSSL.build
    val session = cluster.newSession

    Try {
      session.execute(
        s"""CREATE KEYSPACE IF NOT EXISTS $keyspace
           |  WITH replication = {
           |    'class': 'SingleRegionStrategy'
           |  };
           |""".stripMargin)

      Thread.sleep(10000)
      println(s"Successfully created Cassandra keyspace $keyspace.")

      session.execute(
        s"""CREATE TABLE IF NOT EXISTS $keyspace.messages (
           |  persistence_id text,
           |  partition_nr bigint,
           |  sequence_nr bigint,
           |  timestamp timeuuid,
           |  timebucket text,
           |  writer_uuid text,
           |  ser_id int,
           |  ser_manifest text,
           |  event_manifest text,
           |  event blob,
           |  message blob,
           |  meta_ser_id int,
           |  meta_ser_manifest text,
           |  meta blob,
           |  used boolean,
           |  tags set<text>,
           |  PRIMARY KEY ((persistence_id, partition_nr), sequence_nr, timestamp, timebucket)
           |) WITH gc_grace_seconds = 864000
           |  AND compaction = {
           |    'class': 'SizeTieredCompactionStrategy',
           |    'enabled': true,
           |    'tombstone_compaction_interval': 86400,
           |    'tombstone_threshold': 0.2,
           |    'unchecked_tombstone_compaction': false,
           |    'bucket_high': 1.5,
           |    'bucket_low': 0.5,
           |    'max_threshold': 32,
           |    'min_threshold': 4,
           |    'min_sstable_size': 50
           |  };
           |""".stripMargin)
      println(s"Successfully created Cassandra table $keyspace.messages.")

      session.execute(
        s"""CREATE TABLE IF NOT EXISTS $keyspace.tag_views (
           |  tag_name text,
           |  persistence_id text,
           |  sequence_nr bigint,
           |  timebucket bigint,
           |  timestamp timeuuid,
           |  tag_pid_sequence_nr bigint,
           |  writer_uuid text,
           |  ser_id int,
           |  ser_manifest text,
           |  event_manifest text,
           |  event blob,
           |  meta_ser_id int,
           |  meta_ser_manifest text,
           |  meta blob,
           |  PRIMARY KEY ((tag_name, timebucket), timestamp, persistence_id, tag_pid_sequence_nr)
           |) WITH gc_grace_seconds = 864000
           |  AND compaction = {
           |    'class': 'SizeTieredCompactionStrategy',
           |    'enabled': true,
           |    'tombstone_compaction_interval': 86400,
           |    'tombstone_threshold': 0.2,
           |    'unchecked_tombstone_compaction': false,
           |    'bucket_high': 1.5,
           |    'bucket_low': 0.5,
           |    'max_threshold': 32,
           |    'min_threshold': 4,
           |    'min_sstable_size': 50
           |  };
           |""".stripMargin)
      println(s"Successfully created Cassandra table $keyspace.tag_views.")

      session.execute(
        s"""CREATE TABLE IF NOT EXISTS $keyspace.tag_write_progress(
           |  persistence_id text,
           |  tag text,
           |  sequence_nr bigint,
           |  tag_pid_sequence_nr bigint,
           |  offset timeuuid,
           |  PRIMARY KEY (persistence_id, tag)
           |);
           |""".stripMargin)
      println(s"Successfully created Cassandra table $keyspace.tag_write_progress.")

      session.execute(
        s"""CREATE TABLE IF NOT EXISTS $keyspace.tag_scanning(
           |  persistence_id text,
           |  sequence_nr bigint,
           |  PRIMARY KEY (persistence_id)
           |);
           |""".stripMargin)
      println(s"Successfully created Cassandra table $keyspace.tag_scanning.")

      session.execute(
        s"""CREATE TABLE IF NOT EXISTS $keyspace.metadata(
           |  persistence_id text PRIMARY KEY,
           |  deleted_to bigint,
           |  properties map<text,text>
           |);
           |""".stripMargin)
      println(s"Successfully created Cassandra table $keyspace.metadata.")

      session.execute(
        s"""CREATE TABLE IF NOT EXISTS $keyspace.snapshots (
           |  persistence_id text,
           |  sequence_nr bigint,
           |  timestamp bigint,
           |  ser_id int,
           |  ser_manifest text,
           |  snapshot_data blob,
           |  snapshot blob,
           |  meta_ser_id int,
           |  meta_ser_manifest text,
           |  meta blob,
           |  PRIMARY KEY (persistence_id, sequence_nr)
           |) WITH CLUSTERING ORDER BY (sequence_nr DESC) AND gc_grace_seconds = 864000
           |  AND compaction = {
           |    'class': 'SizeTieredCompactionStrategy',
           |    'enabled': true,
           |    'tombstone_compaction_interval': 86400,
           |    'tombstone_threshold': 0.2,
           |    'unchecked_tombstone_compaction': false,
           |    'bucket_high': 1.5,
           |    'bucket_low': 0.5,
           |    'max_threshold': 32,
           |    'min_threshold': 4,
           |    'min_sstable_size': 50
           |  };
           |""".stripMargin)
      println(s"Successfully created Cassandra table $keyspace.snapshots.")
    } recover { case error =>
      error.printStackTrace()
      session.close()
      cluster.close()
      throw error
    }

    session.close()
    cluster.close()
  }
}
