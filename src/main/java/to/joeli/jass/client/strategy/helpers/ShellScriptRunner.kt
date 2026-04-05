package to.joeli.jass.client.strategy.helpers

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException

object ShellScriptRunner {

    val logger: Logger = LoggerFactory.getLogger(ShellScriptRunner::class.java)

    /**
     * Gets the directory where the deep learning with keras is done
     *
     * @return
     */
    val pythonDirectory: String
        get() = System.getProperty("user.dir") + "/src/main/java/to/joeli/jass/client/strategy/training/python"


    /**
     * This can be used to start independent services we want to use. The started process is saved into the passed list.
     * This list can then be used to access running processes and kill them if needed.
     *
     * @param processes
     * @param directory
     * @param command
     */
    fun startShellProcessInThread(processes: MutableList<Process>, directory: String, command: String) {
        val thread = Thread {
            val builder = buildShellCommand(directory, command)
            try {
                processes.add(builder.start())
            } catch (e: IOException) {
                logger.error("Failed to start shell process: $command", e)
            }
        }
        thread.start()
    }

    /**
     * Runs a shell process in the calling thread and blocks until it is finished.
     *
     * @param directory
     * @param command
     * @return
     */
    fun runShellProcess(directory: String, command: String): Boolean {
        val builder = buildShellCommand(directory, command)

        try {
            val process = builder.start()
            val exitCode = process.waitFor()
            logger.info("Shell process '{}' finished with exit code: {}", command, exitCode)
            return exitCode == 0
        } catch (e: IOException) {
            logger.error("Failed to run shell process: $command", e)
        } catch (e: InterruptedException) {
            logger.warn("Shell process interrupted: $command", e)
            Thread.currentThread().interrupt()
        }

        return false
    }

    /**
     * Uses the ProcessBuilder to assemble a shell command out of the directory and command strings
     *
     * @param directory
     * @param command
     * @return
     */
    private fun buildShellCommand(directory: String, command: String): ProcessBuilder {
        val builder = ProcessBuilder()
        builder.inheritIO()
        builder.directory(File(directory))
        builder.command(*command.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        return builder
    }

    /**
     * Can be used to shutdown a process which cannot be stopped gracefully
     *
     * @param process
     * @param signal
     * @throws InterruptedException
     * @throws IOException
     * @throws IllegalStateException
     */
    @Throws(InterruptedException::class, IOException::class, IllegalStateException::class)
    fun killProcess(process: Process, signal: Int) {
        val builder = buildShellCommand("/", "kill " + signal + " " + getPidOfProcess(process))
        val exitCode = builder.start().waitFor()
        if (exitCode != 0) {
            throw IllegalStateException("<kill $signal> failed, exit code: $exitCode")
        }
    }

    /**
     * Retrieves the pid of a running process using the ProcessHandle API.
     *
     * @param process
     * @return
     */
    @Synchronized
    private fun getPidOfProcess(process: Process): Long {
        val pid = process.pid()
        logger.debug("PID of process is {}", pid)
        return pid
    }
}
