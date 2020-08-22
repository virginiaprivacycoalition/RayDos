package com.virginiaprivacy.contentobserver

import android.content.ContentProviderClient
import android.content.ContentResolver
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.mockito.Mockito.*

fun setUp() {

}
/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {


    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation()
        val providerClient = mock(ContentResolver::class.java)
        providerClient.localContentProvider.
        assertEquals("com.virginiaprivacy.contentobserver.test", appContext.packageName)
    }
}