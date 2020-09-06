package com.virginiaprivacy.raydos

import com.virginiaprivacy.raydos.models.TextMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import rita.RiMarkov
import java.io.File
import java.io.InputStream
import kotlin.system.measureTimeMillis

class TextGenerator(private val asset: InputStream) {

    val data = File("raw/texts.csv")
    val messages = mutableListOf<String>()
    private val markov = RiMarkov(3)

    private fun convertDataToStrings() = flow {
        asset.bufferedReader().useLines {
            it.forEach { line ->
                if (line.split(",")[1].startsWith("\"")) {
                    this.emit(TextMessage(line
                        .split(",", limit = 2)[1]
                        .removePrefix("\"")
                        .split("\"")[0]))
                }
                else {
                    emit(TextMessage(line.split(",")[1]))
                }
            }
        }
    }

    suspend fun train(): Double {
        return measureTimeMillis {
            convertDataToStrings()
                .onEach { markov.loadText(it.content) }
                .buffer(1)
                .collect()
        } / 1000.0
    }

    fun getSentence(): String {
        val sentence = markov.generateSentence()
        return if (sentence.length <= 180 && !sentence.contains("[WARN]")) {
            sentence
        }
        else {
            getSentence()
        }
    }
}

@ExperimentalStdlibApi
fun main() {
    runBlocking(Dispatchers.IO) {
        TextGenerator(File("texts.csv").inputStream()).run {
            println("Trained in ${train()} seconds")
            File("/home/jesse/AndroidStudioProjects/RayDos2/app/src/main/assets/TextMessages.json")
                .writeText(
                    Json.encodeToString(ListSerializer(TextMessage.serializer()),
                        buildList {
                            repeat(600) {
                                add(TextMessage(getSentence()))
                            }
                        }))
        }

    }
}


