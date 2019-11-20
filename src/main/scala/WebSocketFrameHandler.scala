import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame

import java.util.Locale

class WebSocketFrameHandler extends SimpleChannelInboundHandler[WebSocketFrame] {

  override protected def channelRead0(ctx: ChannelHandlerContext, frame: WebSocketFrame) = frame match {
    case frame: TextWebSocketFrame =>
      ctx.channel.writeAndFlush(new TextWebSocketFrame(frame.text().reverse))
    case frame =>
      throw new UnsupportedOperationException("unsupported frame type: " + frame.getClass().getName())
  }
}
