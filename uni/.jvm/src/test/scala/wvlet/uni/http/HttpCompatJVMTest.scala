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
package wvlet.uni.http

import wvlet.uni.control.ResultClass
import wvlet.uni.test.UniTest

import java.io.IOException
import java.net.*
import java.nio.channels.ClosedChannelException
import java.util.concurrent.ExecutionException
import javax.net.ssl.*

class HttpCompatJVMTest extends UniTest:

  // SSL exception tests
  test("classify SSLHandshakeException as non-retryable") {
    val ex = SSLHandshakeException("Certificate error")
    HttpCompat.sslExceptionClassifier(ex) shouldMatch {
      case ResultClass.Failed(isRetryable, _, _) =>
        isRetryable shouldBe false
    }
  }

  test("classify SSLKeyException as non-retryable") {
    val ex = SSLKeyException("Key error")
    HttpCompat.sslExceptionClassifier(ex) shouldMatch {
      case ResultClass.Failed(isRetryable, _, _) =>
        isRetryable shouldBe false
    }
  }

  test("classify SSLPeerUnverifiedException as non-retryable") {
    val ex = SSLPeerUnverifiedException("Peer unverified")
    HttpCompat.sslExceptionClassifier(ex) shouldMatch {
      case ResultClass.Failed(isRetryable, _, _) =>
        isRetryable shouldBe false
    }
  }

  test("classify generic SSLException as retryable") {
    val ex = SSLException("Protocol error")
    HttpCompat.sslExceptionClassifier(ex) shouldMatch {
      case ResultClass.Failed(isRetryable, _, _) =>
        isRetryable shouldBe true
    }
  }

  // Connection exception tests
  test("classify ConnectException as retryable") {
    val ex = ConnectException("Connection refused")
    HttpCompat.connectionExceptionClassifier(ex) shouldMatch {
      case ResultClass.Failed(isRetryable, _, _) =>
        isRetryable shouldBe true
    }
  }

  test("classify SocketTimeoutException as retryable") {
    val ex = SocketTimeoutException("Read timeout")
    HttpCompat.connectionExceptionClassifier(ex) shouldMatch {
      case ResultClass.Failed(isRetryable, _, _) =>
        isRetryable shouldBe true
    }
  }

  test("classify ClosedChannelException as retryable") {
    val ex = ClosedChannelException()
    HttpCompat.connectionExceptionClassifier(ex) shouldMatch {
      case ResultClass.Failed(isRetryable, _, _) =>
        isRetryable shouldBe true
    }
  }

  test("classify ProtocolException as retryable") {
    val ex = ProtocolException("Protocol error")
    HttpCompat.connectionExceptionClassifier(ex) shouldMatch {
      case ResultClass.Failed(isRetryable, _, _) =>
        isRetryable shouldBe true
    }
  }

  test("classify InterruptedException as retryable") {
    val ex = InterruptedException("Interrupted")
    HttpCompat.connectionExceptionClassifier(ex) shouldMatch {
      case ResultClass.Failed(isRetryable, _, _) =>
        isRetryable shouldBe true
    }
  }

  test("classify BindException as retryable") {
    val ex = BindException("Address in use")
    HttpCompat.connectionExceptionClassifier(ex) shouldMatch {
      case ResultClass.Failed(isRetryable, _, _) =>
        isRetryable shouldBe true
    }
  }

  test("classify NoRouteToHostException as retryable") {
    val ex = NoRouteToHostException("No route")
    HttpCompat.connectionExceptionClassifier(ex) shouldMatch {
      case ResultClass.Failed(isRetryable, _, _) =>
        isRetryable shouldBe true
    }
  }

  test("classify IOException with GOAWAY as retryable") {
    val ex = IOException("GOAWAY received")
    HttpCompat.connectionExceptionClassifier(ex) shouldMatch {
      case ResultClass.Failed(isRetryable, _, _) =>
        isRetryable shouldBe true
    }
  }

  test("classify generic SocketException as non-retryable") {
    val ex = SocketException("Unknown error")
    HttpCompat.connectionExceptionClassifier(ex) shouldMatch {
      case ResultClass.Failed(isRetryable, _, _) =>
        isRetryable shouldBe false
    }
  }

  test("classify SocketException with 'Socket closed' message as retryable") {
    val ex = SocketException("Socket closed")
    HttpCompat.connectionExceptionClassifier(ex) shouldMatch {
      case ResultClass.Failed(isRetryable, _, _) =>
        isRetryable shouldBe true
    }
  }

  // Root cause exception tests
  test("unwrap ExecutionException and classify cause") {
    val cause = java.io.EOFException("EOF")
    val ex    = ExecutionException(cause)
    HttpCompat.rootCauseExceptionClassifier(ex) shouldMatch {
      case ResultClass.Failed(isRetryable, _, _) =>
        isRetryable shouldBe true
    }
  }

  test("unwrap nested exception and classify cause") {
    val cause   = java.io.EOFException("EOF")
    val wrapper = RuntimeException("Wrapped", cause)
    HttpCompat.rootCauseExceptionClassifier(wrapper) shouldMatch {
      case ResultClass.Failed(isRetryable, _, _) =>
        isRetryable shouldBe true
    }
  }

end HttpCompatJVMTest
