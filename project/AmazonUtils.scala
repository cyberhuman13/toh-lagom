import scala.util.Try
import com.amazonaws.util.Base64
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import play.api.libs.json.{JsObject, JsString, Json}

object AmazonUtils {
  case class Credentials(username: String, password: String)

  lazy val awsRegion = Try(Regions.getCurrentRegion).toOption.flatMap(Option.apply)
    .getOrElse(Region.getRegion(Regions.US_EAST_1))

  private lazy val awsSecretsClient = AWSSecretsManagerClientBuilder.standard
    .withRegion(awsRegion.getName).build

  lazy val cassandraCredentials = getCredentials("toh-lagom-cassandra")
  lazy val postgresqlCredentials = getCredentials("toh-lagom-postgresql")

  private def getCredentials(secretId: String): Credentials = {
    val request = new GetSecretValueRequest().withSecretId(secretId)
    val result = awsSecretsClient.getSecretValue(request)
    val secret = Option(result.getSecretString)
      .getOrElse(new String(Base64.decode(result.getSecretBinary.array)))

    Json.parse(secret) match {
      case JsObject(json) if json.size == 1 => json.head match {
        case (username, JsString(password)) => Credentials(username, password)
        case _ => throw new RuntimeException(s"Malformed AWS secret with id '$secretId'.")
      }
      case _ => throw new RuntimeException(s"Malformed AWS secret with id '$secretId'.")
    }
  }
}
