/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.uni.netty

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.{
  DefaultFullHttpResponse,
  FullHttpRequest,
  HttpHeaderNames,
  HttpHeaderValues,
  HttpResponseStatus,
  HttpUtil,
  HttpVersion
}
import wvlet.uni.http.{
  ContentType,
  HttpContent,
  HttpHeader,
  HttpMethod,
  HttpMultiMap,
  HttpStatus,
  Request,
  Response
}
import wvlet.uni.log.LogSupport
import wvlet.uni.rx.{OnCompletion, OnError, OnNext, Rx, RxRunner}

import scala.jdk.CollectionConverters.*

/**
  * Netty channel handler that processes HTTP requests using RxHttpHandler
  */
class NettyRequestHandler(handler: RxHttpHandler)
    extends SimpleChannelInboundHandler[FullHttpRequest]
    with LogSupport:

  override def channelRead0(ctx: ChannelHandlerContext, nettyRequest: FullHttpRequest): Unit =
    val keepAlive = HttpUtil.isKeepAlive(nettyRequest)
    try
      val request  = toUniRequest(nettyRequest)
      val response = handler.handle(request)
      runRxResponse(ctx, response, keepAlive)
    catch
      case e: Exception =>
        warn(s"Error handling request: ${e.getMessage}", e)
        sendResponse(ctx, Response.internalServerError(e.getMessage), keepAlive)

  private def runRxResponse(
      ctx: ChannelHandlerContext,
      rx: Rx[Response],
      keepAlive: Boolean
  ): Unit =
    RxRunner.runOnce(rx) {
      case OnNext(response) =>
        sendResponse(ctx, response.asInstanceOf[Response], keepAlive)
      case OnError(e) =>
        warn(s"Error in Rx handler: ${e.getMessage}", e)
        sendResponse(ctx, Response.internalServerError(e.getMessage), keepAlive)
      case OnCompletion =>
        sendResponse(ctx, Response.notFound, keepAlive)
    }

  private def sendResponse(
      ctx: ChannelHandlerContext,
      response: Response,
      keepAlive: Boolean
  ): Unit =
    val nettyResponse = toNettyResponse(response)

    if keepAlive then
      nettyResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
      ctx.writeAndFlush(nettyResponse)
    else
      nettyResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
      ctx.writeAndFlush(nettyResponse).addListener(ChannelFutureListener.CLOSE)

  private def toUniRequest(nettyRequest: FullHttpRequest): Request =
    val method = HttpMethod.of(nettyRequest.method().name()).getOrElse(HttpMethod.GET)
    val uri    = nettyRequest.uri()

    val headersBuilder = HttpMultiMap.newBuilder
    nettyRequest
      .headers()
      .entries()
      .asScala
      .foreach { entry =>
        headersBuilder.add(entry.getKey, entry.getValue)
      }

    val content =
      val buf = nettyRequest.content()
      if buf.readableBytes() > 0 then
        val bytes = new Array[Byte](buf.readableBytes())
        buf.readBytes(bytes)
        val ct = headersBuilder
          .result()
          .get(HttpHeader.ContentType)
          .flatMap(ContentType.parse)
          .getOrElse(ContentType.ApplicationOctetStream)
        HttpContent.bytes(bytes, ct)
      else
        HttpContent.Empty

    Request(method = method, uri = uri, headers = headersBuilder.result(), content = content)

  private def toNettyResponse(response: Response): DefaultFullHttpResponse =
    val status = HttpResponseStatus.valueOf(response.status.code)

    val content      = response.content
    val contentBytes = content.toContentBytes
    val buf          = Unpooled.wrappedBuffer(contentBytes)

    val nettyResponse = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buf)

    response
      .headers
      .entries
      .foreach { case (name, value) =>
        nettyResponse.headers().add(name, value)
      }

    content
      .contentType
      .foreach { ct =>
        nettyResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, ct.value)
      }

    nettyResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentBytes.length)

    nettyResponse

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit =
    warn(s"Channel exception: ${cause.getMessage}", cause)
    ctx.close()

end NettyRequestHandler

object NettyRequestHandler:
  def apply(handler: RxHttpHandler): NettyRequestHandler = new NettyRequestHandler(handler)
