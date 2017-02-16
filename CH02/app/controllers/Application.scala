package controllers

import actors.TwitterStreamer
import play.api.libs.json._
import play.api.libs.streams.ActorFlow
import play.api.mvc._

class Application extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.index("Tweets"))
  }

  def tweets = WebSocket.accept[String, JsValue] { request =>
    ActorFlow.actorRef(out => TwitterStreamer.props(out))

  }

  def replicateFeed = Action { implicit request =>
    Ok(TwitterStreamer.subscribeNode)
  }

}
