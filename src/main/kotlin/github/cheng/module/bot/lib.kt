package github.cheng.module.bot

import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.contact
import com.github.kotlintelegrambot.dispatcher.handlers.HandleCommand
import com.github.kotlintelegrambot.dispatcher.handlers.HandleMessage
import com.github.kotlintelegrambot.dispatcher.message
import kotlinx.coroutines.supervisorScope
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.pathString

/**
 *
 * @author caseycheng
 * @date 2023/10/15-21:45
 * @doc
 **/

/**
 * 解析参数，将参数解析为Map
 */
fun List<String>.toMap(): Map<String, String> {
    if (isEmpty()) return emptyMap()
    val result: MutableMap<String, String> = mutableMapOf()
    var key = ""
    this.forEachIndexed { index, arg ->
        if (index % 2 == 0) {
            key = arg
        } else {
            result[key] = arg
        }
    }
    return result
}

fun Path.basename() = this.fileName.name

fun String.suffix() = this.drop(this.lastIndexOf('.') + 1)

fun copyFile(
    sourceFile: Path,
    destFile: Path,
) {
    Files.createDirectories(destFile.parent)

    Files.copy(sourceFile, destFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
}

private fun currentPath() = Path("").toAbsolutePath().pathString

val currentPath by lazy { currentPath() }

@OptIn(ExperimentalContracts::class)
fun Dispatcher.privateMessage(handleMessage: HandleMessage) {
    contract {
        callsInPlace(handleMessage,InvocationKind.EXACTLY_ONCE)
    }
    message {
        if (message.chat.type != "private") return@message
        handleMessage()
    }
}

fun Dispatcher.privateCommand(command: String, handleCommand: HandleCommand) {
    command(command) {
        if (message.chat.type != "private") {
            return@command
        }
        handleCommand()
    }
}
