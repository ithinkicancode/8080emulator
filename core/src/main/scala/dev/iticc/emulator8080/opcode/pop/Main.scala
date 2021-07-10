package dev.iticc.emulator8080.opcode.pop

import entities.Cpu
import entities.Exceptions._
import zhttp.http._
import zhttp.http.Method.{ HEAD, GET, POST }
import zhttp.service.{ ChannelFactory, Client, EventLoopGroup, Server }
import zio.{ App, ExitCode, Task, URIO, ZEnv, ZIO }
import zio.json._

object Main extends App {

  private val api = "api"
  private val apiVersion = "v1"
  private val status = "status"

  private val defaultPort = 8088
  private val memoryAddressQueryParam = "address"
  private val idQueryParam = "id"

  private lazy val healthy = Http.succeed(Response.text("Healthy"))

  // TODO: move this to config and inject config as an R
  private val remoteServiceUrl = Path("https://NOT-CONFIGURED!")

  private val requirements = ChannelFactory.auto ++ EventLoopGroup.auto()

  private val httpApp = Http.route[Request] {
    case HEAD -> Root / `status` =>
      Http.empty
    case GET -> Root / `status` =>
      healthy

    case request @ POST -> Root / `api` / `apiVersion` / "execute" =>
      val computation = for {
        cpu <- entityAs[Cpu](request).mapError { error =>
          BadClientRequest(s"Unable to obtain expected value from payload: <$error>.")
        }
        stackPointer = cpu.state.stackPointer
        lowByteUrl <- urlOf(
          cpu.id,
          s"$stackPointer"
        )
        highByteUrl <- urlOf(
          cpu.id,
          s"${(stackPointer + 1) & 0xffff}"
        )
        result <- ZIO
          .mapParN(
            fetchByteFrom(lowByteUrl),
            fetchByteFrom(highByteUrl)
          )(cpu.withNewState)
          .flatMap(
            ZIO
              .fromEither(_)
              .mapError { error =>
                ApplicationError(s"Error updating CPU state: <$error>.")
              }
          )
      } yield result

      val response = computation
        .bimap(
          {
            case error: BadClientRequest =>
              badRequestFor(s"${error.getLocalizedMessage}")
            case error =>
              internalErrorFor(s"${error.getLocalizedMessage}")
          },
          responseAsJson[Cpu]
        )
        .catchAll(ZIO.succeed(_))

      Http.fromEffect(response)

    case request @ GET -> Root / `api` / `apiVersion` / "debug" / "readMemory" =>
      val memoryAddress = for {
        values <- request.url.queryParams.get(memoryAddressQueryParam)
        value1 <- values.headOption
        int <- value1.toIntOption
      } yield int

      val response = memoryAddress
        .map(address => Response.text(s"${address & 0xff}"))
        .getOrElse(
          badRequestFor("Invalid memory address")
        )

      Http.succeed(response)
  }

  override def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    val port = args.headOption
      .flatMap(_.toIntOption)
      .getOrElse(defaultPort)

    Server
      .start(port, httpApp)
      .provideCustomLayer(requirements)
      .exitCode
  }

  private def urlOf(id: String, memoryAddress: String) = Task {
    URL(
      remoteServiceUrl,
      queryParams = Map(
        idQueryParam -> List(id),
        memoryAddressQueryParam -> List(memoryAddress)
      )
    )
  }

  private def fetchByteFrom(url: URL) = Client
    .request(GET -> url)
    .flatMap(_.content match {
      case HttpData.CompleteData(data) =>
        Task {
          data
            .map(_.toChar)
            .mkString
            .toInt
        }
      case HttpData.StreamData(_) =>
        ZIO.fail(
          RemoteHttpCallError("Received chunked/streaming data")
        )
      case HttpData.Empty =>
        ZIO.fail(
          RemoteHttpCallError("Received no data")
        )
    })

  private def entityAs[A: JsonCodec](request: Request) =
    for {
      payload <- ZIO
        .fromOption(request.getBodyAsString)
        .orElseFail("Empty payload")
      entity <- ZIO
        .fromEither(payload.fromJson[A])
    } yield entity

  private def responseAsJson[A: JsonCodec](entity: A) =
    Response.jsonString(entity.toJson)

  private def badRequestFor(reason: String) =
    Response.fromHttpError(
      HttpError.BadRequest(reason)
    )

  private def internalErrorFor(reason: String) =
    Response.fromHttpError(
      HttpError.InternalServerError(reason)
    )
}
