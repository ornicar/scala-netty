import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;

object EchoServer {

  val SSL = false
  val PORT = 8080

  def main(args: Array[String]): Unit = {

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
        .childHandler(new WebSocketServerInitializer(sslCtx))

      val ch = b.bind(PORT).sync().channel()

      println(s"Listening to $PORT")

      ch.closeFuture().sync()

      println(s"Closed $PORT")
    }
    finally {
      bossGroup.shutdownGracefully()
      workerGroup.shutdownGracefully()
    }
  }
}
