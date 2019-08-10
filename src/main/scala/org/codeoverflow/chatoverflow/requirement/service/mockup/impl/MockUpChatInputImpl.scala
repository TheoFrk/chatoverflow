package org.codeoverflow.chatoverflow.requirement.service.mockup.impl

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.dto.chat.{ChatEmoticon, ChatMessage, ChatMessageAuthor, TextChannel}
import org.codeoverflow.chatoverflow.api.io.event.chat.mockup.{MockupChatMessageReceiveEvent, MockupEvent}
import org.codeoverflow.chatoverflow.api.io.input.chat.MockUpChatInput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.impl.EventInputImpl
import org.codeoverflow.chatoverflow.requirement.service.mockup.MockUpChatConnector

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

@Deprecated
@Impl(impl = classOf[MockUpChatInput], connector = classOf[MockUpChatConnector])
class MockUpChatInputImpl extends EventInputImpl[MockupEvent, MockUpChatConnector] with MockUpChatInput with WithLogger {

  private val messages: ListBuffer[ChatMessage[ChatMessageAuthor, TextChannel, ChatEmoticon]] = ListBuffer[ChatMessage[ChatMessageAuthor, TextChannel, ChatEmoticon]]()

  override def getLastMessages(lastMilliseconds: Long): java.util.List[ChatMessage[ChatMessageAuthor, TextChannel, ChatEmoticon]] = {
    val currentTime = OffsetDateTime.now

    messages.filter(_.getTime.isAfter(currentTime.minus(lastMilliseconds, ChronoUnit.MILLIS))).toList.asJava
  }


  override def getLastPrivateMessages(lastMilliseconds: Long): java.util.List[ChatMessage[ChatMessageAuthor, TextChannel, ChatEmoticon]] = {
    new util.ArrayList() // Not yet implemented
  }

  override def serialize(): String = getSourceIdentifier

  override def deserialize(value: String): Unit = setSourceConnector(value)

  /**
    * Start the input, called after source connector did init
    *
    * @return true if starting the input was successful, false if some problems occurred
    */
  override def start(): Boolean = {
    sourceConnector.get.registerEventHandler[ChatMessage[ChatMessageAuthor, TextChannel, ChatEmoticon]](onMessage)
    true
  }

  /**
    * Stops the input, called before source connector will shutdown
    *
    * @return true if stopping was successful
    */
  override def stop(): Boolean = true

  private def onMessage(msg: ChatMessage[ChatMessageAuthor, TextChannel, ChatEmoticon]): Unit = {
    call(new MockupChatMessageReceiveEvent(msg))
    messages += msg
  }
}
