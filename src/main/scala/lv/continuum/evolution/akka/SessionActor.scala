package lv.continuum.evolution.akka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import io.circe.Error
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol.UserType._
import lv.continuum.evolution.protocol.Protocol._

object SessionActor {

  case class Command(in: Either[Error, In], replyTo: ActorRef[Out])

  def apply(): Behavior[Command] = unauthenticated

  private def unauthenticated: Behavior[Command] =
    Behaviors.receive { (context, command) =>
      command.in match {
        case Right(LoginIn(username, password)) =>
          (username, password) match {
            case ("admin", "admin") =>
              command.replyTo ! LoginSuccessfulOut(userType = Admin)
              authenticated(Admin)

            case ("user", "user") =>
              command.replyTo ! LoginSuccessfulOut(userType = User)
              authenticated(User)

            case _ =>
              command.replyTo ! ErrorOut(OutType.LoginFailed)
              Behaviors.same
          }

        case Right(_) =>
          command.replyTo ! ErrorOut(OutType.NotAuthenticated)
          Behaviors.same

        case Left(error) =>
          context.log.warn(s"Issue while parsing JSON: ${ error.getMessage }")
          command.replyTo ! ErrorOut(OutType.InvalidMessage)
          Behaviors.same
      }
    }

  private def authenticated(userType: UserType): Behavior[Command] =
    Behaviors.receive { (context, command) =>
      (userType, command.in) match {
        case (_, Right(_: LoginIn)) =>
          command.replyTo ! ErrorOut(OutType.UnknownError)
          Behaviors.same

        case (_, Right(PingIn(seq))) =>
          command.replyTo ! PongOut(seq = seq)
          Behaviors.same

        case (_, Right(SubscribeTablesIn)) |
             (_, Right(UnsubscribeTablesIn)) =>
          // TODO Respond with ack and pass to TableActor
          Behaviors.same

        case (Admin, Right(_: AdminIn)) =>
          // TODO Respond with ack and pass to TableActor
          Behaviors.same

        case (User, Right(_: AdminIn)) =>
          command.replyTo ! ErrorOut(OutType.NotAuthorized)
          Behaviors.same

        case (_, Left(error)) =>
          context.log.warn(s"Issue while parsing JSON: ${ error.getMessage }")
          command.replyTo ! ErrorOut(OutType.InvalidMessage)
          Behaviors.same
      }
    }
}
