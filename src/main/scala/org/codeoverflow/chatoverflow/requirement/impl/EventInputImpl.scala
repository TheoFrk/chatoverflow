package org.codeoverflow.chatoverflow.requirement.impl

import java.util.function.Consumer

import org.codeoverflow.chatoverflow.api.io.event.Event
import org.codeoverflow.chatoverflow.api.io.input.event.EventInput
import org.codeoverflow.chatoverflow.connector.Connector

import scala.reflect.ClassTag

/**
  * Default implementation for all inputs that provide events.
  *
  * The integrated event registry allows registering new Event handlers at any time.
  * @tparam T the event interface that all events for this EventInput share
  * @tparam C the connector to which this input belongs
  */
abstract class EventInputImpl[T <: Event, C <: Connector](implicit ctc: ClassTag[C]) extends InputImpl[C] with EventInput[T] with EventManager {

  /**
   * Register a new event handler that listens for a specific event
   *
   * @param eventHandler consumer for which `accept()` is called if the event is fired
   * @param eventClass   class of the events for which this listener should listen
   */
  override def registerEventHandler[S <: T](eventHandler: Consumer[S], eventClass: Class[S]): Unit = {
    registerEventHandler[S](x => eventHandler.accept(x))(ClassTag(eventClass), identifier)
  }

  override def shutdown(): Boolean = {
    if (sourceConnector.isDefined) {
      val stopped = stop()
      unregisterAllEventListeners
      stopped & sourceConnector.get.shutdown()
    } else {
      logger warn "Source connector not set."
      false
    }
  }
}
