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

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{Channel, ChannelFuture, ChannelInitializer, ChannelOption, EventLoopGroup}
import io.netty.channel.epoll.{Epoll, EpollEventLoopGroup, EpollServerSocketChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.{HttpContentCompressor, HttpObjectAggregator, HttpServerCodec}
import wvlet.uni.log.LogSupport
import wvlet.uni.util.ThreadUtil

import java.net.InetSocketAddress

/**
  * Netty-based HTTP server implementation
  */
class NettyHttpServer(config: NettyServerConfig) extends LogSupport:
  private var bossGroup: EventLoopGroup   = null
  private var workerGroup: EventLoopGroup = null
  private var channel: Channel            = null
  @volatile
  private var running: Boolean = false

  private def effectiveHandler: RxHttpHandler =
    if config.filters.isEmpty then
      config.handler
    else
      val chained = RxHttpFilter.chain(config.filters)
      RxHttpHandler { request =>
        chained.apply(request, config.handler)
      }

  def start(): Unit = synchronized {
    if running then
      throw IllegalStateException("Server is already running")

    val useEpoll = config.useNativeTransport && Epoll.isAvailable

    if useEpoll then
      bossGroup = EpollEventLoopGroup(1, ThreadUtil.newDaemonThreadFactory(s"${config.name}-boss"))
      workerGroup = EpollEventLoopGroup(
        0,
        ThreadUtil.newDaemonThreadFactory(s"${config.name}-worker")
      )
    else
      bossGroup = NioEventLoopGroup(1, ThreadUtil.newDaemonThreadFactory(s"${config.name}-boss"))
      workerGroup = NioEventLoopGroup(
        0,
        ThreadUtil.newDaemonThreadFactory(s"${config.name}-worker")
      )

    val handler = effectiveHandler

    val bootstrap = ServerBootstrap()
    bootstrap
      .group(bossGroup, workerGroup)
      .channel(
        if useEpoll then
          classOf[EpollServerSocketChannel]
        else
          classOf[NioServerSocketChannel]
      )
      .childHandler(
        new ChannelInitializer[SocketChannel]:
          override def initChannel(ch: SocketChannel): Unit =
            val pipeline = ch.pipeline()
            pipeline.addLast(
              "codec",
              HttpServerCodec(
                config.maxInitialLineLength,
                config.maxHeaderSize,
                config.maxContentLength
              )
            )
            pipeline.addLast("aggregator", HttpObjectAggregator(config.maxContentLength))
            pipeline.addLast("compressor", HttpContentCompressor())
            pipeline.addLast("handler", NettyRequestHandler(handler))
      )
      .option(ChannelOption.SO_BACKLOG, Integer.valueOf(128))
      .childOption(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.TRUE)

    val bindFuture = bootstrap.bind(config.host, config.port).sync()
    channel = bindFuture.channel()
    running = true

    info(s"Netty server started at ${localAddress}")
  }

  def stop(): Unit = synchronized {
    if running then
      info(s"Stopping Netty server at ${localAddress}")
      try
        if channel != null then
          channel.close().sync()
      finally
        try
          if workerGroup != null then
            workerGroup.shutdownGracefully().sync()
        finally
          try
            if bossGroup != null then
              bossGroup.shutdownGracefully().sync()
          finally running = false
  }

  def awaitTermination(): Unit =
    if channel != null then
      channel.closeFuture().sync()

  def isRunning: Boolean = running

  def localAddress: InetSocketAddress =
    if channel != null then
      channel.localAddress().asInstanceOf[InetSocketAddress]
    else
      InetSocketAddress(config.host, config.port)

  def localPort: Int = localAddress.getPort

end NettyHttpServer

object NettyHttpServer:
  def apply(config: NettyServerConfig): NettyHttpServer = new NettyHttpServer(config)
