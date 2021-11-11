/*
 * Copyright (c) 2011-2021 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.core.http;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.lang.time.StopWatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.test.core.TestUtils;

@RunWith(Parameterized.class)
public class HttpBandwidthLimitingTest extends Http2TestBase {
  private static final int OUTBOUND_LIMIT = 64 * 1024;  // 64KB/s
  private static final int INBOUND_LIMIT = 64 * 1024;   // 64KB/s
  private static final int TEST_CONTENT_SIZE = 64 * 1024 * 4;   // 64 * 4 = 256KB

  private final File sampleF = new File("src/test/resources/test_traffic.txt");
  private final Handlers HANDLERS = new Handlers();

  @Parameters(name = "HTTP {0}")
  public static Iterable<Object[]> data() {

    Function<Vertx, HttpServer> http1ServerFactory = (v) -> Providers.http1Server(v, INBOUND_LIMIT, OUTBOUND_LIMIT);
    Function<Vertx, HttpServer> http2ServerFactory = (v) -> Providers.http2Server(v, INBOUND_LIMIT, OUTBOUND_LIMIT);
    Function<Vertx, HttpClient> http1ClientFactory = (v) -> v.createHttpClient();
    Function<Vertx, HttpClient> http2ClientFactory = (v) -> v.createHttpClient(createHttp2ClientOptions());

    return Arrays.asList(new Object[][] {
      { 1.1, http1ServerFactory, http1ClientFactory },
      { 2.0, http2ServerFactory, http2ClientFactory }
    });
  }

  private HttpServer server = null;
  private HttpClient client = null;
  private Function<Vertx, HttpServer> serverFactory;
  private Function<Vertx, HttpClient> clientFactory;

  public HttpBandwidthLimitingTest(double protoVersion, Function<Vertx, HttpServer> serverFactory,
                                   Function<Vertx, HttpClient> clientFactory) {
    this.serverFactory = serverFactory;
    this.clientFactory = clientFactory;
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
    server = serverFactory.apply(vertx);
    client = clientFactory.apply(vertx);
  }

  @After
  public void after() {
    client.close();
    server.close();
    vertx.close();
  }

  @Test
  public void sendBufferThrottled() throws Exception {
    Buffer expectedBuffer = TestUtils.randomBuffer(TEST_CONTENT_SIZE);

    StopWatch watch = new StopWatch();
    server.requestHandler(HANDLERS.bufferRead(expectedBuffer));
    startServer(server);

    watch.start();
    read(expectedBuffer, server, client);
    await();
    watch.stop();

    assertTrue(watch.getTime() > expectedTimeMillis(TEST_CONTENT_SIZE, INBOUND_LIMIT));
  }

  @Test
  public void sendFileIsThrottled() throws Exception {
    StopWatch watch = new StopWatch();
    server.requestHandler(HANDLERS.getFile(sampleF));
    startServer(server);

    watch.start();
    client.request(HttpMethod.GET, server.actualPort(), DEFAULT_HTTP_HOST,"/get-file",
                   req -> req.result().send(resp -> {
                     resp.result().bodyHandler(r -> testComplete());
                   }));
    await();
    watch.stop();

    assertTrue(watch.getTime() > 3000);
  }

  @Test
  public void dataUploadIsThrottled() throws Exception {
    Buffer expectedBuffer = TestUtils.randomBuffer((TEST_CONTENT_SIZE));

    StopWatch watch = new StopWatch();
    server.requestHandler(HANDLERS.bufferWrite(expectedBuffer));
    startServer(server);

    watch.start();
    write(expectedBuffer, server, client);
    await();
    watch.stop();

    assertTrue(watch.getTime() > expectedTimeMillis(TEST_CONTENT_SIZE, INBOUND_LIMIT));
  }

  @Test
  public void fileUploadIsThrottled() throws Exception {
    StopWatch watch = new StopWatch();
    server.requestHandler(HANDLERS.uploadFile(sampleF));
    startServer(server);

    watch.start();
    upload(server, client, sampleF);
    await();
    watch.stop();

    assertTrue( watch.getTime() > 2000);
  }

  /**
   * The throttling takes a while to kick in so the expected time cannot be strict especially
   * for small data sizes in these tests.
   *
   * @param size
   * @param rate
   * @return
   */
  private long expectedTimeMillis(int size, int rate) {
    return (long) (TimeUnit.MILLISECONDS.convert((size / rate), TimeUnit.SECONDS) * 0.5);
  }

  private void read(Buffer expected, HttpServer server, HttpClient client) {
    client.request(HttpMethod.GET, server.actualPort(), DEFAULT_HTTP_HOST,"/buffer-read",
      req -> {
        req.result().send(resp -> resp.result().bodyHandler(r -> {
          assertEquals(expected.getByteBuf(), r.getByteBuf());
          testComplete();
        }));
      });
  }

  private void write(Buffer buffer, HttpServer server, HttpClient client) {
    client.request(HttpMethod.POST, server.actualPort(), DEFAULT_HTTP_HOST, "/buffer-write",
                   req -> req.result()
                             .putHeader(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(buffer.length()))
                             .end(buffer)
                   );
  }

  private void upload(HttpServer server, HttpClient client, File expected) {
    Buffer b = vertx.fileSystem().readFileBlocking(expected.getAbsolutePath());
    client.request(HttpMethod.PUT, server.actualPort(), DEFAULT_HTTP_HOST, "/upload-file", req -> {
      req.result()
         .putHeader(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(expected.length()))
         .putHeader(HttpHeaderNames.CONTENT_TYPE, "application/binary")
         .end(b);
    });
  }

  class Handlers {
    public Handler<HttpServerRequest> bufferRead(Buffer expectedBuffer) {
      return (req) -> {
        req.response().setChunked(true);

        int start = 0;
        int size = expectedBuffer.length();
        int chunkSize = OUTBOUND_LIMIT / 2;
        while (size > 0) {
          int len = Math.min(chunkSize, size);
          req.response().write(expectedBuffer.getBuffer(start, start + len));
          start += len;
          size -= len;
        }
        req.response().end();
      };
    }

    public Handler<HttpServerRequest> getFile(File expected) {
      return req -> req.response().sendFile(expected.getAbsolutePath());
    }

    public Handler<HttpServerRequest> bufferWrite(Buffer expected) {
      return req -> {
        req.bodyHandler(buffer -> {
          assertEquals(expected.getByteBuf(), buffer.getByteBuf());
          testComplete();
        });
      };
    }

    public Handler<HttpServerRequest> uploadFile(File expected) {
      return req -> {
        req.endHandler((r) -> {
          assertEquals(expected.length(), req.bytesRead());
          testComplete();
        });
      };
    }
  }

  static class Providers {
    private static HttpServer http1Server(Vertx vertx, int inboundLimit, int outboundLimit) {
      int openPort = findFreePort();
      HttpServerOptions options = new HttpServerOptions()
                                    .setHost(DEFAULT_HTTP_HOST)
                                    .setPort(openPort)
                                    .setInboundGlobalBandwidth(inboundLimit)
                                    .setOutboundGlobalBandwidth(outboundLimit);

      return vertx.createHttpServer(options);
    }

    private static HttpServer http2Server(Vertx vertx, int inboundLimit, int outboundLimit) {
      int openPort = findFreePort();
      HttpServerOptions options = createHttp2ServerOptions(openPort, DEFAULT_HTTP_HOST)
                                    .setInboundGlobalBandwidth(inboundLimit)
                                    .setOutboundGlobalBandwidth(outboundLimit);

      return vertx.createHttpServer(options);
    }

    private static int findFreePort() {
      try (ServerSocket socket = new ServerSocket(0)) {
        return socket.getLocalPort();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return -1;
    }
  }
}
