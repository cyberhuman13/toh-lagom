import com.amazonaws.util.Base64
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import play.api.libs.json.{Format, Json}

object AmazonUtils {
  case class Credentials(username: String, password: String)
  implicit val format: Format[Credentials] = Json.format

  lazy val awsRegionName = sys.env("AWS_REGION")
  lazy val awsRegion = Region.getRegion(Regions.fromName(awsRegionName))
  private lazy val awsSecretsClient = AWSSecretsManagerClientBuilder.standard
    .withRegion(awsRegionName).build

  lazy val cassandraCredentials = getCredentials("toh-lagom-cassandra")
  lazy val postgresqlCredentials = getCredentials("toh-lagom-postgresql")

  private def getCredentials(secretId: String): Credentials = {
    val request = new GetSecretValueRequest().withSecretId(secretId)
    val result = awsSecretsClient.getSecretValue(request)
    val secret = Option(result.getSecretString)
      .getOrElse(new String(Base64.decode(result.getSecretBinary.array)))
    Json.parse(secret).as[Credentials]
  }
}
