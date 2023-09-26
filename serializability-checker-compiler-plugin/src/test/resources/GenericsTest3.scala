package org.random.project

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.grpc.scaladsl.WebHandler
import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

object GenericsTest3 {

  def invokeGrpcWebHandler(handlers: List[PartialFunction[HttpRequest, Future[HttpResponse]]])(implicit
      as: ActorSystem[_]): HttpRequest => Future[HttpResponse] = {
    // For some reason type parameter on below method call for implicitly passed ActorSystem[_] is not `scala.Any`
    // but `org.random.project.GenericsTest3._$1`. That's why we use `WebHandler.grpcWebHandler` method in test.
    WebHandler.grpcWebHandler(handlers: _*)
  }

}
