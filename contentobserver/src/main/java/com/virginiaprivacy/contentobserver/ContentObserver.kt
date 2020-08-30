package com.virginiaprivacy.contentobserver

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler

inline fun contentObserver(handler: Handler, crossinline action: ContentObserver.() -> Unit): ContentObserver {

    return object : ContentObserver(handler) {

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            handler.post {
                this.action()
            }
        }
    }
}