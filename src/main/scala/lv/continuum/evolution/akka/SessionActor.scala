package lv.continuum.evolution.akka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import io.circe.Error
import lv.continuum.evolution.akka.TableActor.TableCommand
import lv.continuum.evolution.protocol.Protocol.In._
import lv.continuum.evolution.protocol.Protocol.Out._
import lv.continuum.evolution.protocol.Protocol.UserType._
import lv.continuum.evolution.protocol.Protocol._

object SessionActor {

  case class SessionCommand(in: Either[Error, In], replyTo: ActorRef[Option[Out]])

  def apply(
    tableActor: ActorRef[TableCommand],
    pushActor: ActorRef[PushOut],
  ): Behavior[SessionCommand] = unauthenticated(tableActor, pushActor)

  private def unauthenticated(
    tableActor: ActorRef[TableCommand],
    pushActor: ActorRef[PushOut],
  ): Behavior[SessionCommand] =
    Behaviors.receive { (context, command) =>
      command.in match {
        case Right(LoginIn(username, password)) =>
          (username, password) match {
            case ("admin", "admin") =>
              command.replyTo ! Some(LoginSuccessfulOut(userType = Admin))
              authenticated(tableActor, pushActor, Admin)

            case ("user", "user") =>
              command.replyTo ! Some(LoginSuccessfulOut(userType = User))
              authenticated(tableActor, pushActor, User)

            case _ =>
              command.replyTo ! Some(ErrorOut(OutType.LoginFailed))
              Behaviors.same
          }

        case Right(_) =>
          command.replyTo ! Some(ErrorOut(OutType.NotAuthenticated))
          Behaviors.same

        case Left(error) =>
          context.log.warn(s"Issue while parsing JSON: ${ error.getMessage }")
          command.replyTo ! Some(ErrorOut(OutType.InvalidMessage))
          Behaviors.same
      }
    }

  private def authenticated(
    tableActor: ActorRef[TableCommand],
    pushActor: ActorRef[PushOut],
    userType: UserType,
  ): Behavior[SessionCommand] =
    Behaviors.receive { (context, command) =>
      (userType, command.in) match {
        case (_, Right(_: LoginIn)) =>
          command.replyTo ! Some(ErrorOut(OutType.UnknownError))
          Behaviors.same

        case (_, Right(PingIn(seq))) =>
          command.replyTo ! Some(PongOut(seq = seq))
          Behaviors.same

        case (_, in @ Right(SubscribeTablesIn | UnsubscribeTablesIn)) =>
          command.replyTo ! None
          tableActor ! TableCommand(in.value, pushActor)
          Behaviors.same

        case (Admin, Right(in: AdminIn)) =>
          command.replyTo ! None
          tableActor ! TableCommand(in, pushActor)
          Behaviors.same

        case (User, Right(_: AdminIn)) =>
          command.replyTo ! Some(ErrorOut(OutType.NotAuthorized))
          Behaviors.same

        case (_, Left(error)) =>
          context.log.warn(s"Issue while parsing JSON: ${ error.getMessage }")
          command.replyTo ! Some(ErrorOut(OutType.InvalidMessage))
          Behaviors.same
      }
    }
}
