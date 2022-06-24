package lv.continuum.lobby.akka

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import lv.continuum.lobby.akka.TableActor.TableCommand
import lv.continuum.lobby.auth.Authenticator
import lv.continuum.lobby.model.ParsingError
import lv.continuum.lobby.protocol.Protocol.In._
import lv.continuum.lobby.protocol.Protocol.Out._
import lv.continuum.lobby.protocol.Protocol.UserType._
import lv.continuum.lobby.protocol.Protocol._

object SessionActor {

  case class SessionCommand(in: Either[ParsingError, In], replyTo: ActorRef[Option[Out]])

  def apply(
    authenticator: Authenticator,
    tableActor: ActorRef[TableCommand],
    pushActor: ActorRef[PushOut],
  ): Behavior[SessionCommand] = {

    def unauthenticated: Behavior[SessionCommand] =
      Behaviors
        .receive[SessionCommand] { (context, command) =>
          command.in match {
            case Right(Login(username, password)) =>
              login(username, password, command.replyTo)

            case Right(_) =>
              command.replyTo ! Some(NotAuthenticated)
              Behaviors.same

            case Left(e) =>
              error(context, e, command.replyTo)
          }
        }
        .stopWhenWatchedActorTerminates

    def authenticated(userType: UserType): Behavior[SessionCommand] =
      Behaviors
        .receive[SessionCommand] { (context, command) =>
          (userType, command.in) match {
            case (_, Right(Login(username, password))) =>
              login(username, password, command.replyTo)

            case (_, Right(Ping(seq))) =>
              command.replyTo ! Some(Pong(seq = seq))
              Behaviors.same

            case (User, Right(_: AdminTableIn)) =>
              command.replyTo ! Some(NotAuthorized)
              Behaviors.same

            case (_, Right(tableIn: TableIn)) =>
              command.replyTo ! None
              tableActor ! TableCommand(tableIn, pushActor)
              Behaviors.same

            case (_, Left(e)) =>
              error(context, e, command.replyTo)
          }
        }
        .stopWhenWatchedActorTerminates

    def login(
      username: Username,
      password: Password,
      replyTo: ActorRef[Option[Out]],
    ): Behavior[SessionCommand] =
      authenticator.authenticate(username, password) match {
        case Some(userType) =>
          replyTo ! Some(LoginSuccessful(userType))
          authenticated(userType)

        case None =>
          replyTo ! Some(LoginFailed)
          tableActor ! TableCommand(UnsubscribeTables, pushActor)
          unauthenticated
      }

    def error(
      context: ActorContext[SessionCommand],
      error: ParsingError,
      replyTo: ActorRef[Option[Out]],
    ): Behavior[SessionCommand] = {
      context.log.warn(s"Issue while parsing JSON: $error")
      replyTo ! Some(InvalidMessage)
      Behaviors.same
    }

    Behaviors.setup { context =>
      context.watch(pushActor)
      unauthenticated
    }
  }

  implicit private class Stopper(behavior: Behaviors.Receive[SessionCommand]) {
    def stopWhenWatchedActorTerminates: Behavior[SessionCommand] =
      behavior.receiveSignal { case (_, Terminated(_)) => Behaviors.stopped }
  }
}
