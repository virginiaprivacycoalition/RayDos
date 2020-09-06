package com.virginiaprivacy.contentobserver


import android.content.ContentResolver
import android.os.Handler
import android.provider.Telephony
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import junit.framework.Assert.assertNotNull
import net.bytebuddy.utility.RandomString
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.same
import org.robolectric.RobolectricTestRunner
import org.robolectric.fakes.RoboCursor
import kotlin.random.Random


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(RobolectricTestRunner::class)
class ExampleInstrumentedTest {

    private val messageCursor = RoboCursor()


    @Test
    fun useAppContext() {

        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation()
        val contentResolver: ContentResolver = mock {
            on {
                query(same(Telephony.Sms.Outbox.CONTENT_URI),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull())

            } doReturn messageCursor.apply { setResults(getMessageResults()) }


        }
        val providerClient = contentResolver.acquireContentProviderClient(Telephony.Sms.Outbox.CONTENT_URI)
//        assertNotNull(providerClient)

        var changes = 0
        val observer =  contentObserver(Handler(appContext.context.mainLooper)) {
            Log.i("Test", changes++.toString())
        }
        contentResolver.registerContentObserver(Telephony.Sms.Outbox.CONTENT_URI, false, observer)
        assertNotNull(observer)
        contentResolver.query(Telephony.Sms.Outbox.CONTENT_URI, null, null, arrayOf<String>(), null)

        Log.d(
            "", changes.toString()
        )    }

    private fun getMessageResults() =
        arrayOf<Array<String>>().apply {
            repeat(Random(0).nextInt(25)) {
                this[it] = arrayOf(RandomString.make(11).repeat(15))
            }
        }
    }
