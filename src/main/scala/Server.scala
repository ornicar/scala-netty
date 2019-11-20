import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import com.typesafe.scalalogging.Logger
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.epoll.{ EpollChannelOption, EpollEventLoopGroup, EpollServerSocketChannel }
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate
import io.netty.util.AttributeKey

object ScalaNetty extends App {

  private val logger = Logger(getClass)

  val version = System.getProperty("java.version")
  val memory = Runtime.getRuntime().maxMemory() / 1024 / 1024
  logger.info("lila-ws stream-less play 2.7")
  logger.info(s"Java version: $version, memory: ${memory}MB")

  val SSL = false
  val PORT = 8080

  val clients: ActorSystem[Clients.Control] = ActorSystem(Clients.start, "clients")

  val sslCtx = if (SSL) Some {
    val ssc = new SelfSignedCertificate
    SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build()
  }
  else None

  val bossGroup = new EpollEventLoopGroup()
  val workerGroup = new EpollEventLoopGroup()
  try {
    val b = new ServerBootstrap
    b.group(bossGroup, workerGroup)
      .channel(classOf[EpollServerSocketChannel])
      .childHandler(new WebSocketServerInitializer(clients, sslCtx))

    val ch = b.bind(PORT).sync().channel()

    logger.info(s"Listening to $PORT")

    ch.closeFuture().sync()

    logger.info(s"Closed $PORT")
  }
  finally {
    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
  }
}

object Client {

  sealed trait Msg
  final case class Out(msg: String) extends Msg

  def start(channel: Channel): Behavior[Msg] = Behaviors.setup { ctx =>

    apply(msg => channel.writeAndFlush(new TextWebSocketFrame(msg)))
  }

  def apply(clientIn: String => Unit): Behavior[Msg] = Behaviors.receive { (ctx, msg) =>

    msg match {
      case Out(msg) =>
        clientIn(msg)
        Behaviors.same
    }
  }
}

object Clients {

  type ChannelId = String

  sealed trait Control
  final case class Start(channel: Channel) extends Control
  final case class Stop(id: ChannelId) extends Control
  final case class Out(id: ChannelId, msg: String) extends Control

  def start: Behavior[Control] = Behaviors.setup { ctx =>
    apply(Map.empty[ChannelId, ActorRef[Client.Msg]])
  }

  def apply(clients: Map[ChannelId, ActorRef[Client.Msg]]): Behavior[Control] =
    Behaviors.receive[Control] { (ctx, msg) =>
      msg match {
        case Out(id, msg) =>
          clients get id match {
            case None => ctx.log.info(s"Message sent to missing client: $msg")
            case Some(client) => client ! Client.Out(msg)
          }
          Behaviors.same
        case Start(channel) =>
          val id = channel.id.asShortText
          val client = ctx.spawn(Client.start(channel), id)
          channel.attr(attrKey).set(client)
          apply(clients + (id -> client))
        case Stop(id) => clients get id match {
          case None => Behaviors.same
          case Some(client) =>
            ctx.stop(client)
            apply(clients - id)
        }
      }
    }

  val attrKey = AttributeKey.valueOf[ActorRef[Client.Msg]]("client")
}
