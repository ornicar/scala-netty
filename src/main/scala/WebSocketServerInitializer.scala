import io.netty.channel._
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.{ HttpObjectAggregator, HttpServerCodec }
import io.netty.handler.codec.http._
import io.netty.handler.codec.TooLongFrameException
import java.io.IOException
// import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler
import com.typesafe.scalalogging.Logger
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.ssl.SslContext

class WebSocketServerInitializer(sslCtx: Option[SslContext]) extends ChannelInitializer[SocketChannel] {

  private val logger = Logger(getClass)

  override def initChannel(ch: SocketChannel): Unit = {
    val pipeline = ch.pipeline()
    sslCtx foreach { ssl =>
      pipeline.addLast(ssl.newHandler(ch.alloc()))
    }
    pipeline.addLast(new HttpServerCodec())
    pipeline.addLast(new HttpObjectAggregator(4096)) // 8192?
    // pipeline.addLast(new WebSocketServerCompressionHandler())
    pipeline.addLast(new WebSocketServerProtocolHandler(
      "/", // path
      null, // subprotocols (?)
      true, // allowExtensions (?)
      2048, // max frame size
      false, // allowMaskMismatch (?)
      true, // checkStartsWith
      true // dropPongFrames
    ) {
      override def userEventTriggered(ctx: ChannelHandlerContext, evt: java.lang.Object): Unit = evt match {
        case hs: WebSocketServerProtocolHandler.HandshakeComplete =>
          println(s"${hs.requestUri} ${hs.requestHeaders}")
        case evt => ctx fireUserEventTriggered evt
      }
      override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
        cause match {
          // IO exceptions happen all the time, it usually just means that the client has closed the connection before fully
          // sending/receiving the response.
          case e: IOException =>
            logger.trace("Benign IO exception caught in Netty", e)
            ctx.channel().close()
          case e: TooLongFrameException =>
            logger.warn("Handling TooLongFrameException", e)
            sendSimpleErrorResponse(ctx, HttpResponseStatus.REQUEST_URI_TOO_LONG)
          case e: IllegalArgumentException if Option(e.getMessage).exists(_.contains("Header value contains a prohibited character")) =>
            sendSimpleErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST)
          case e =>
            logger.error("Exception in Netty", e)
            super.exceptionCaught(ctx, cause)
        }
      }
      private def sendSimpleErrorResponse(ctx: ChannelHandlerContext, status: HttpResponseStatus): ChannelFuture = {
        val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status)
        response.headers.set(HttpHeaderNames.CONNECTION, "close")
        response.headers.set(HttpHeaderNames.CONTENT_LENGTH, "0")
        val f = ctx.channel.write(response)
        f.addListener(ChannelFutureListener.CLOSE)
        f
      }
    })
    pipeline.addLast(new WebSocketFrameHandler())
  }
}
