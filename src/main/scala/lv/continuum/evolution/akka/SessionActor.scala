package lv.continuum.evolution.akka

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Terminated}
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
  ): Behavior[SessionCommand] = {

    def unauthenticated: Behavior[SessionCommand] =
      Behaviors
        .receive[SessionCommand] { (context, command) =>
          command.in match {
            case Right(LoginIn(username, password)) =>
              login(username, password, command.replyTo)

            case Right(_) =>
              command.replyTo ! Some(ErrorOut(OutType.NotAuthenticated))
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
            case (_, Right(LoginIn(username, password))) =>
              login(username, password, command.replyTo)

            case (_, Right(PingIn(seq))) =>
              command.replyTo ! Some(PongOut(seq = seq))
              Behaviors.same

            case (User, Right(_: AdminTableIn)) =>
              command.replyTo ! Some(ErrorOut(OutType.NotAuthorized))
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
    ): Behavior[SessionCommand] = (username, password) match {
      case (Username("admin"), Password("admin")) =>
        replyTo ! Some(LoginSuccessfulOut(userType = Admin))
        authenticated(Admin)

      case (Username("user"), Password("user")) =>
        replyTo ! Some(LoginSuccessfulOut(userType = User))
        authenticated(User)

      case _ =>
        replyTo ! Some(ErrorOut(OutType.LoginFailed))
        tableActor ! TableCommand(UnsubscribeTablesIn, pushActor)
        unauthenticated
    }

    def error(
      context: ActorContext[SessionCommand],
      error: Error,
      replyTo: ActorRef[Option[Out]],
    ): Behavior[SessionCommand] = {
      context.log.warn(s"Issue while parsing JSON: ${ error.getMessage }")
      replyTo ! Some(ErrorOut(OutType.InvalidMessage))
      Behaviors.same
    }

    Behaviors.setup { context =>
      context.watch(pushActor)
      unauthenticated
    }
  }

  private implicit class Stopper(behavior: Behaviors.Receive[SessionCommand]) {
    def stopWhenWatchedActorTerminates: Behavior[SessionCommand] =
      behavior.receiveSignal {
        case (context, Terminated(_)) =>
          context.log.info("Watched actor terminated, will stop now")
          Behaviors.stopped
      }
  }
}
