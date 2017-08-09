package controllers

import javax.inject.Inject

import play.api.libs.iteratee.Iteratee
import play.api.libs.oauth.{ConsumerKey, OAuthCalculator, RequestToken}
import play.api.libs.ws._
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Logger}

import scala.concurrent.{ExecutionContext, Future}


class Application @Inject()(wS: WSClient, config: Configuration, cc: ControllerComponents,
                            implicit val ec: ExecutionContext) extends AbstractController(cc) {

  def index = Action { implicit request =>
    Ok(views.html.index("Tweets"))
  }

  val credentials: Option[(ConsumerKey, RequestToken)] = for {
    apiKey <- config.getString("twitter.apiKey")
    apiSecret <- config.getString("twitter.apiSecret")
    token <- config.getString("twitter.token")
    tokenSecret <- config.getString("twitter.tokenSecret")
  } yield (
    ConsumerKey(apiKey, apiSecret), RequestToken(token, tokenSecret)
  )

  def tweets = Action.async {
    val loggingIteratee = Iteratee.foreach[Array[Byte]] {
      array => Logger.info(array.map(_.toChar).mkString)
    }
    credentials.map {
      case (consumerKey, requestToken) =>
        wS
          .url("https:stream.twitter.com/1.1/statuses/filter.json")
          .sign(OAuthCalculator(consumerKey, requestToken))
          .withQueryStringParameters("track" -> "reactive")
          .get.map { response =>
          Logger.info("Status: " + response.status)
          loggingIteratee
          Ok("Stream closed")
        }.map { _ =>
          Ok("Stream closed")
        }
    } getOrElse {
      Future.successful {
        InternalServerError("Twitter credentials missing")
      }
    }
  }

}
