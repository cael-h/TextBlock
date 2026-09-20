package dev.octoshrimpy.quik.model

import android.provider.Telephony.Mms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageReactionTargetTest {

    @Test
    fun `image only MMS is reactable`() {
        val message = incomingMms("image/jpeg")

        assertTrue(message.hasReactableContent())
        assertEquals("an image", message.getReactionTargetText())
    }

    @Test
    fun `media reaction labels describe their attachment`() {
        assertEquals("a video", incomingMms("video/mp4").getReactionTargetText())
        assertEquals("an audio message", incomingMms("audio/aac").getReactionTargetText())
        assertEquals("an attachment", incomingMms("application/pdf").getReactionTargetText())
    }

    @Test
    fun `gif and webp sticker payloads are reactable images`() {
        assertEquals("an image", incomingMms("image/gif").getReactionTargetText())
        assertEquals("an image", incomingMms("image/webp").getReactionTargetText())
    }

    @Test
    fun `caption is preferred over generic media reaction label`() {
        val message = incomingMms("image/jpeg").apply {
            parts.add(MmsPart().apply {
                type = "text/plain"
                text = "Look at this"
            })
        }

        assertEquals("Look at this", message.getReactionTargetText())
    }

    @Test
    fun `empty SMS is not reactable`() {
        val message = Message().apply {
            type = Message.TYPE_SMS
            body = "   "
        }

        assertFalse(message.hasReactableContent())
    }

    private fun incomingMms(contentType: String) = Message().apply {
        type = Message.TYPE_MMS
        boxId = Mms.MESSAGE_BOX_INBOX
        parts.add(MmsPart().apply { type = contentType })
    }
}
