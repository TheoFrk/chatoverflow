package org.codeoverflow.chatoverflow.requirement.service.tipeeestream

import java.util.Calendar
import java.util.function.Consumer

import io.socket.client.{IO, Socket}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.dto.event.tipeeestream.{TipeeeStreamDonation, TipeeeStreamEvent, TipeeeStreamFollow, TipeeeStreamSubscription}
import org.codeoverflow.chatoverflow.connector.Connector
import org.json.{JSONException, JSONObject}

import scala.collection.mutable.ListBuffer

/**
  * The tipeeestream connector connects to the socket.io service to work with incoming events.
  *
  * @param sourceIdentifier the name of the tipeeestream account
  */
class TipeeeStreamConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {
  private val eventHandler = ListBuffer[Consumer[TipeeeStreamEvent]]()
  private val apiKey = "apiKey"
  private val username = "username"
  override protected var requiredCredentialKeys: List[String] = List(apiKey, username)
  override protected var optionalCredentialKeys: List[String] = List()
  private var socket: Option[Socket] = None
  private val TIMEOUT = 10000

  /**
    * Starts the connector, e.g. creates a connection with its platform.
    */
  override def start(): Boolean = {
    //TODO test error handling
    var connected: Option[Boolean] = None
    val thread = Thread.currentThread
    socket.get.on(Socket.EVENT_CONNECT, (_: Any) => {
      logger info "Connected to TipeeStream Socket.io"
      socket.get.emit("join-room", getAuthenticationObject)
      logger info "emitted credentials to TipeeSetream Socket.io api"
      socket.get.on("new-event", (objects: Array[AnyRef]) => {
        serializeObjectToObject(objects)
      })
      connected = Some(true)
      connected.notifyAll()
    })
    socket.get.on(Socket.EVENT_CONNECT_ERROR, (e: Any) => {
      logger warn s"Could not connect to TipeeeStream socket:"
      logger warn e.asInstanceOf[Array[Object]].mkString(",")
      connected = Some(false)
      connected.notifyAll()
    })
    socket.get.on(Socket.EVENT_CONNECT_TIMEOUT, (_: Any) => {
      logger warn s"$sourceIdentifier socket timed out"
    })
    socket.get.on(Socket.EVENT_ERROR, (e: Any) => {
      logger warn s"$sourceIdentifier socket error:"
      logger warn e.asInstanceOf[Array[Object]].mkString(",")
    })
    val start = Calendar.getInstance.getTimeInMillis
    while (connected.isEmpty && start + TIMEOUT > Calendar.getInstance.getTimeInMillis) connected.wait(TIMEOUT)
    connected.getOrElse({
      logger warn "Could not connect to TipeeeStream socket: Timed out!"
      false
    })
  }

  def addIncomingEventHandler(handler: Consumer[TipeeeStreamEvent]): Unit = {
    eventHandler += handler
  }

  private def serializeObjectToObject(objects : Array[AnyRef]) : Unit = {
    val json: JSONObject = objects(0).asInstanceOf[JSONObject]
    val event: JSONObject = json.getJSONObject("event")
    val eventType: String = event.getString("type")
    eventType match {
      case "subscription" =>
        Subscription(event)
      case "donation" =>
        Donation(event)
      case "follow" =>
        Follow(event)
      case _ =>
    }
  }

  @throws[JSONException]
  private def Donation(event: JSONObject): Unit = {
    val parameter = event.getJSONObject("parameters")
    val user = parameter.getString("username")
    val message = parameter.getString("formattedMessage")
    val amount = parameter.getDouble("amount")
    val donation: TipeeeStreamDonation = new TipeeeStreamDonation(null, user, message, amount, null, null)
    eventHandler.foreach(_.accept(donation))
  }

  @throws[JSONException]
  private def Subscription(event: JSONObject): Unit = {
    val parameter = event.getJSONObject("parameters")
    val user = parameter.getString("username")
    val message = parameter.getString("formattedMessage")
    val resub = parameter.getInt("resub")
    val subscription: TipeeeStreamSubscription = new TipeeeStreamSubscription(null, user, message, resub)
    eventHandler.foreach(_.accept(subscription))
  }

  @throws[JSONException]
  private def Follow(event: JSONObject): Unit = {
    val parameter = event.getJSONObject("parameters")
    val user = parameter.getString("username")
    val message = parameter.getString("message")
    val follow: TipeeeStreamFollow = new TipeeeStreamFollow(null, user, message)
    eventHandler.foreach(_.accept(follow))
  }

  /**
    * This stops the activity of the connector, e.g. by closing the platform connection.
    */
  override def stop(): Boolean = {
    socket.foreach(_.close())
    true
  }

  private def getAuthenticationObject: JSONObject = {
    val obj = new JSONObject()
    obj.put("room", credentials.get.getValue(apiKey).get)
    obj.put("username", credentials.get.getValue(username).get)
    obj
  }
}
