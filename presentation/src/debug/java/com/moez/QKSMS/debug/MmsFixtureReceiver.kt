package dev.octoshrimpy.quik.debug

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.provider.Telephony
import android.util.Base64
import com.google.android.mms.ContentType
import com.google.android.mms.pdu_alt.CharacterSets
import com.google.android.mms.pdu_alt.EncodedStringValue
import com.google.android.mms.pdu_alt.PduBody
import com.google.android.mms.pdu_alt.PduHeaders
import com.google.android.mms.pdu_alt.PduPart
import com.google.android.mms.pdu_alt.PduPersister
import com.google.android.mms.pdu_alt.RetrieveConf
import dev.octoshrimpy.quik.injection.appComponent
import dev.octoshrimpy.quik.interactor.SyncMessage
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale

/** Debug-only entry point used by scripts/test-emulator-mms-media.ps1. */
class MmsFixtureReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val kind = intent.getStringExtra(EXTRA_KIND)?.lowercase(Locale.US) ?: KIND_JPEG
        val sender = intent.getStringExtra(EXTRA_SENDER) ?: DEFAULT_SENDER
        val runId = intent.getStringExtra(EXTRA_RUN_ID) ?: System.currentTimeMillis().toString()

        runCatching { persistFixture(context, sender, kind, runId) }
            .onSuccess { uri ->
                appComponent.syncMessage().execute(SyncMessage.Params(uri)) {
                    pendingResult.finish()
                }
            }
            .onFailure { error ->
                android.util.Log.e("MmsFixtureReceiver", "Could not persist $kind fixture", error)
                pendingResult.finish()
            }
    }

    private fun persistFixture(context: Context, sender: String, kind: String, runId: String) =
        PduPersister.getPduPersister(context).persist(
            buildPdu(sender, kind, runId),
            Telephony.Mms.Inbox.CONTENT_URI,
            true,
            true,
            null,
            -1
        ).also { uri ->
            val values = ContentValues().apply {
                put(Telephony.Mms.READ, 0)
                put(Telephony.Mms.SEEN, 0)
                put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000L)
            }
            context.contentResolver.update(uri, values, null, null)
        }

    private fun buildPdu(sender: String, kind: String, runId: String): RetrieveConf {
        val fixtureId = "textblock-fixture-$runId-$kind"
        val body = PduBody().apply {
            addPart(textPart("TextBlock $kind MMS fixture"))
            addPart(mediaPart(kind))
        }

        return RetrieveConf().apply {
            mmsVersion = PduHeaders.CURRENT_MMS_VERSION
            from = EncodedStringValue(sender)
            addTo(EncodedStringValue(EMULATOR_NUMBER))
            date = System.currentTimeMillis() / 1000L
            messageId = fixtureId.asciiBytes()
            transactionId = fixtureId.asciiBytes()
            contentType = ContentType.MULTIPART_RELATED.asciiBytes()
            retrieveStatus = PduHeaders.RETRIEVE_STATUS_OK
            this.body = body
        }
    }

    private fun textPart(text: String) = PduPart().apply {
        charset = CharacterSets.UTF_8
        contentType = ContentType.TEXT_PLAIN.asciiBytes()
        contentId = "<text>".asciiBytes()
        contentLocation = "text.txt".asciiBytes()
        name = "text.txt".asciiBytes()
        data = text.toByteArray(StandardCharsets.UTF_8)
    }

    private fun mediaPart(kind: String): PduPart {
        val (mimeType, filename, data) = when (kind) {
            KIND_PNG -> Triple(ContentType.IMAGE_PNG, "fixture.png", bitmapBytes(Bitmap.CompressFormat.PNG))
            KIND_GIF -> Triple(ContentType.IMAGE_GIF, "fixture.gif", animatedGifBytes())
            KIND_WEBP -> Triple("image/webp", "fixture.webp", bitmapBytes(Bitmap.CompressFormat.WEBP_LOSSLESS))
            KIND_MALFORMED -> Triple(ContentType.IMAGE_JPEG, "broken.jpg", byteArrayOf(0x00, 0x11, 0x22, 0x33))
            else -> Triple(ContentType.IMAGE_JPEG, "fixture.jpg", bitmapBytes(Bitmap.CompressFormat.JPEG))
        }

        return PduPart().apply {
            contentType = mimeType.asciiBytes()
            contentId = "<$filename>".asciiBytes()
            contentLocation = filename.asciiBytes()
            name = filename.asciiBytes()
            this.filename = filename.asciiBytes()
            contentDisposition = "inline".asciiBytes()
            this.data = data
        }
    }

    private fun bitmapBytes(format: Bitmap.CompressFormat): ByteArray {
        val bitmap = Bitmap.createBitmap(480, 320, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(21, 117, 92))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("TextBlock MMS", bitmap.width / 2f, bitmap.height / 2f, paint)
        return ByteArrayOutputStream().use { output ->
            check(bitmap.compress(format, 90, output)) { "Could not encode $format fixture" }
            bitmap.recycle()
            output.toByteArray()
        }
    }

    private fun animatedGifBytes(): ByteArray = Base64.decode(ANIMATED_GIF_BASE64, Base64.DEFAULT)

    private fun String.asciiBytes() = toByteArray(StandardCharsets.US_ASCII)

    companion object {
        const val ACTION_INJECT = "com.caelh.textblock.debug.action.INJECT_MMS_FIXTURE"
        const val EXTRA_KIND = "kind"
        const val EXTRA_SENDER = "sender"
        const val EXTRA_RUN_ID = "runId"

        private const val DEFAULT_SENDER = "5558675309"
        private const val EMULATOR_NUMBER = "15555215554"
        private const val KIND_JPEG = "jpeg"
        private const val KIND_PNG = "png"
        private const val KIND_GIF = "gif"
        private const val KIND_WEBP = "webp"
        private const val KIND_MALFORMED = "malformed"

        // Two small GIF frames. The visual is intentionally tiny; this fixture validates GIF decoding.
        private const val ANIMATED_GIF_BASE64 =
            "R0lGODlhAgACAPAAAP8AAAAAACH5BAAAAAAALAAAAAACAAIAAAIDhI9WADs="
    }
}
