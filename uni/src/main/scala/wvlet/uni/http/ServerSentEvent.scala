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

/**
  * Represents a Server-Sent Event (SSE) as defined in the HTML5 specification.
  *
  * SSE is a server push technology enabling a client to receive automatic updates from a server via
  * an HTTP connection. Each event can have an optional id, event type, retry interval, and data
  * payload.
  *
  * @param id
  *   Optional event identifier. The client can use this to resume from a specific event after
  *   reconnection using the Last-Event-ID header.
  * @param event
  *   Optional event type. Defaults to "message" if not specified. Clients can listen for specific
  *   event types.
  * @param retry
  *   Optional reconnection time in milliseconds. Tells the client how long to wait before
  *   reconnecting if the connection is lost.
  * @param data
  *   The event payload. If the data contains multiple lines, they will be split and sent as
  *   separate data fields in the wire format.
  */
case class ServerSentEvent(
    id: Option[String] = None,
    event: Option[String] = None,
    retry: Option[Long] = None,
    data: String = ""
):
  /**
    * Converts this event to the SSE wire format.
    *
    * The format follows the SSE specification:
    * {{{
    * id: <id>
    * event: <event-type>
    * retry: <milliseconds>
    * data: <line1>
    * data: <line2>
    *
    * }}}
    *
    * Each event ends with a double newline to signal the end of the event.
    *
    * @return
    *   The event in SSE wire format
    */
  def toContent: String =
    val b = Seq.newBuilder[String]
    id.foreach(x => b += s"id: ${x}")
    event.foreach(x => b += s"event: ${x}")
    retry.foreach(x => b += s"retry: ${x}")
    if data.nonEmpty then
      data.split("\n", -1).foreach(x => b += s"data: ${x}")
    else
      b += "data: "
    s"${b.result().mkString("\n")}\n\n"

  /**
    * Creates a new event with the specified id
    */
  def withId(newId: String): ServerSentEvent = copy(id = Some(newId))

  /**
    * Creates a new event without an id
    */
  def noId: ServerSentEvent = copy(id = None)

  /**
    * Creates a new event with the specified event type
    */
  def withEvent(newEvent: String): ServerSentEvent = copy(event = Some(newEvent))

  /**
    * Creates a new event without an event type
    */
  def noEvent: ServerSentEvent = copy(event = None)

  /**
    * Creates a new event with the specified retry interval
    */
  def withRetry(newRetry: Long): ServerSentEvent = copy(retry = Some(newRetry))

  /**
    * Creates a new event without a retry interval
    */
  def noRetry: ServerSentEvent = copy(retry = None)

  /**
    * Creates a new event with the specified data
    */
  def withData(newData: String): ServerSentEvent = copy(data = newData)

end ServerSentEvent

object ServerSentEvent:
  /**
    * Creates an event with just data
    */
  def data(dataContent: String): ServerSentEvent = ServerSentEvent(data = dataContent)

  /**
    * Creates an event with data and event type
    */
  def withEventType(dataContent: String, eventType: String): ServerSentEvent = ServerSentEvent(
    event = Some(eventType),
    data = dataContent
  )

end ServerSentEvent
