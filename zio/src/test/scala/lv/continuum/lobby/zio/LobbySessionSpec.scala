package lv.continuum.lobby.zio

import lv.continuum.lobby.protocol.Protocol.{Out, Password, PushOut, UserType, Username}
import lv.continuum.lobby.protocol.Protocol.UserType.{Admin, User}
import lv.continuum.lobby.protocol.TestData
import lv.continuum.lobby.zio.LobbySession.Env
import lv.continuum.lobby.zio.layer.{
  Authenticator,
  LobbyHolderLive,
  SessionHolderLive,
  Subscriber,
  SubscribersHolder,
  SubscribersHolderLive
}
import zio.*
import zio.test.*
import zio.test.Assertion.*

object LobbySessionSpec extends ZIOSpecDefault with TestData {

  override def spec: Spec[TestEnvironment with Scope, Nothing] =
    suite("LobbySessionSpec")(
      suite("when not authenticated should")(
        test("decline authentication upon invalid credentials")(
          for {
            out <- LobbySession.process(Right(login._2), stubSubscriber)
          } yield assert(out)(isSome(equalTo(loginFailed._2)))
        ),
        test("decline responding to pings")(
          for {
            out <- LobbySession.process(Right(ping._2), stubSubscriber)
          } yield assert(out)(isSome(equalTo(notAuthenticated._2)))
        ),
        test("decline processing TableIn messages")(
          for {
            out <- LobbySession.process(Right(subscribeTables._2), stubSubscriber)
          } yield assert(out)(isSome(equalTo(notAuthenticated._2)))
        ),
        test("decline processing AdminTableIn messages")(
          for {
            out <- LobbySession.process(Right(addTable._2), stubSubscriber)
          } yield assert(out)(isSome(equalTo(notAuthenticated._2)))
        ),
        test("report invalid messages")(
          for {
            out <- LobbySession.process(Left(parsingError), stubSubscriber)
          } yield assert(out)(isSome(equalTo(invalidMessage._2)))
        ),
      ).provide(
        stubAuthenticator(None),
        LobbyHolderLive.layer,
        SessionHolderLive.layer,
        SubscribersHolderLive.layer,
      ),
      suite("when authenticated as User should")(
        test("respond to pings")(processPingWhenLoggedIn),
        test("subscribe and unsubscribe via TableIn messages")(
          processSubscribeAndUnsubscribeWhenLoggedIn()
        ),
        test("decline processing AdminTableIn messages")(
          for {
            _   <- processLogin
            out <- LobbySession.process(Right(addTable._2), stubSubscriber)
          } yield assert(out)(isSome(equalTo(notAuthorized._2)))
        ),
        test("report invalid messages")(processParsingErrorWhenLoggedIn),
      ).provide(
        stubAuthenticator(Some(User)),
        LobbyHolderLive.layer,
        SessionHolderLive.layer,
        SubscribersHolderLive.layer,
      ),
      suite("when authenticated as Admin should")(
        test("respond to pings")(processPingWhenLoggedIn),
        test("subscribe and unsubscribe via TableIn messages")(
          processSubscribeAndUnsubscribeWhenLoggedIn()
        ),
        test("handle AddTable, UpdateTable and RemoveTable messages and notify subscribers")(
          processSubscribeAndUnsubscribeWhenLoggedIn(mockSubscriber =>
            for {
              outAddTable      <- LobbySession.process(Right(addTable._2), mockSubscriber)
              pushOutsAddTable <- mockSubscriber.pushOuts

              outUpdateTable      <- LobbySession.process(Right(updateTable._2), mockSubscriber)
              pushOutsUpdateTable <- mockSubscriber.pushOuts

              outRemoveTable      <- LobbySession.process(Right(removeTable._2), mockSubscriber)
              pushOutsRemoveTable <- mockSubscriber.pushOuts
            } yield Some(
              List(
                assert(outAddTable)(isNone),
                assert(pushOutsAddTable)(equalTo(Vector(tableAdded._2))),
                assert(outUpdateTable)(isNone),
                assert(pushOutsUpdateTable)(hasLast(equalTo(tableUpdated._2))),
                assert(outRemoveTable)(isNone),
                assert(pushOutsRemoveTable)(hasLast(equalTo(tableRemoved._2))),
              ).reduce(_ && _)
            ),
          )
        ),
        test("handle failure upon AddTable message")(
          processSubscribeAndUnsubscribeWhenLoggedIn(_ =>
            for {
              out <- LobbySession.process(Right(addTableInvalid), stubSubscriber)
            } yield Some(assert(out)(isSome(equalTo(tableAddFailed._2))))
          )
        ),
        test("handle failure upon UpdateTable message")(
          processSubscribeAndUnsubscribeWhenLoggedIn(_ =>
            for {
              out <- LobbySession.process(Right(updateTableInvalid), stubSubscriber)
            } yield Some(assert(out)(isSome(equalTo(tableUpdateFailed._2))))
          )
        ),
        test("handle failure upon RemoveTable message")(
          processSubscribeAndUnsubscribeWhenLoggedIn(_ =>
            for {
              out <- LobbySession.process(Right(removeTableInvalid), stubSubscriber)
            } yield Some(assert(out)(isSome(equalTo(tableRemoveFailed._2))))
          )
        ),
        test("report invalid messages")(processParsingErrorWhenLoggedIn),
      ).provide(
        stubAuthenticator(Some(Admin)),
        LobbyHolderLive.layer,
        SessionHolderLive.layer,
        SubscribersHolderLive.layer,
      ),
    )

  private val stubSubscriber: Subscriber = (pushOut: PushOut) => ZIO.unit

  class MockSubscriber(
    pushOutsRef: Ref[Vector[PushOut]]
  ) extends Subscriber {

    override def send(pushOut: PushOut): Task[Unit] =
      pushOutsRef.update(_ :+ pushOut)

    def pushOuts: UIO[Vector[PushOut]] = pushOutsRef.get
  }
  object MockSubscriber {

    def make: UIO[MockSubscriber] =
      for {
        pushOutsRef <- Ref.make(Vector.empty[PushOut])
      } yield MockSubscriber(pushOutsRef)
  }

  private def stubAuthenticator(userType: Option[UserType]): ULayer[Authenticator] =
    ZLayer.succeed { (username: Username, password: Password) => ZIO.succeed(userType) }

  private val processLogin: ZIO[Env, Nothing, Option[Out]] =
    LobbySession.process(Right(login._2), stubSubscriber)

  private val processPingWhenLoggedIn: ZIO[Env, Nothing, TestResult] =
    for {
      _   <- processLogin
      out <- LobbySession.process(Right(ping._2), stubSubscriber)
    } yield assert(out)(isSome(equalTo(pong._2)))

  private def processSubscribeAndUnsubscribeWhenLoggedIn(
    assertWhenSubscribed: MockSubscriber => ZIO[Env, Nothing, Option[TestResult]] = _ => ZIO.succeed(None),
  ): ZIO[Env, Nothing, TestResult] =
    for {
      _                           <- processLogin
      mockSubscriber              <- MockSubscriber.make
      subscribeOut                <- LobbySession.process(Right(subscribeTables._2), mockSubscriber)
      subscribersWhenSubscribed   <- ZIO.serviceWithZIO[SubscribersHolder](_.subscribers)
      testResultWhenSubscribed    <- assertWhenSubscribed(mockSubscriber)
      unsubscribeOut              <- LobbySession.process(Right(unsubscribeTables._2), mockSubscriber)
      subscribersWhenUnsubscribed <- ZIO.serviceWithZIO[SubscribersHolder](_.subscribers)
    } yield {
      (List(
        assert(subscribeOut)(isSome(equalTo(tableList._2))),
        assert(subscribersWhenSubscribed)(equalTo(Set(mockSubscriber))),
        assert(unsubscribeOut)(isNone),
        assert(subscribersWhenUnsubscribed)(isEmpty),
      ) ++ testResultWhenSubscribed).reduce(_ && _)
    }

  private val processParsingErrorWhenLoggedIn: ZIO[Env, Nothing, TestResult] =
    for {
      _   <- processLogin
      out <- LobbySession.process(Left(parsingError), stubSubscriber)
    } yield assert(out)(isSome(equalTo(invalidMessage._2)))
}
