package doobie

import doobie._
import doobie.implicits._

import cats._
import cats.effect._
import cats.implicits._
import doobie.hikari._
import scala.concurrent.ExecutionContext.global

object DoobieApp extends IOApp {
  implicit val ctx: ContextShift[IO] = IO.contextShift(global)

  val transactor: Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32)
      be <- Blocker[IO]
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.h2.Driver",
        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        "sa",
        "",
        ce,
        be
      )
    } yield xa

  def run(args: List[String]): IO[ExitCode] =
    transactor.use { xa =>
      val create =  sql"CREATE TABLE bla (id INT NOT NULL, des VARCHAR(30) )".update
      val update =  sql"INSERT INTO bla(id,des) VALUES (1, 'Hi' )".update
      val select = sql"SELECT id, des FROM bla".query[Greeting].unique
      (create.run *> update.run *> select).transact(xa).map(i => println(s"Done $i")).map(_ =>ExitCode.Success)
    }
}

case class Greeting(id: Int,message: String)
