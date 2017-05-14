package example

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

private val TEST_COUNT = 16

fun main(args: Array<String>) = runBlocking<Unit> {
    println("Started!")
    List(TEST_COUNT) {
        launch(CommonPool) {
            test()
        }
    }.forEach { it.join() }
    println("Done.")
}
