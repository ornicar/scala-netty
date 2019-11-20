import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame

import java.util.Locale

class WebSocketFrameHandler(clients: ActorRef[Clients.Control]) extends SimpleChannelInboundHandler[WebSocketFrame] {

  override protected def channelRead0(ctx: ChannelHandlerContext, frame: WebSocketFrame) = frame match {
    case frame: TextWebSocketFrame =>
      ctx.channel.attr(Clients.attrKey).get ! Client.Out(frame.text())
      // ctx.channel.writeAndFlush(new TextWebSocketFrame(frame.text().reverse))
    case frame =>
      throw new UnsupportedOperationException("unsupported frame type: " + frame.getClass().getName())
  }
}
