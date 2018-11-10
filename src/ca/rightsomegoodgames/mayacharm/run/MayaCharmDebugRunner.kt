package ca.rightsomegoodgames.mayacharm.run

import ca.rightsomegoodgames.mayacharm.settings.ProjectSettings
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.impl.ProcessListUtil
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.jetbrains.python.debugger.PyDebugRunner
import com.jetbrains.python.debugger.PyLocalPositionConverter
import com.jetbrains.python.debugger.attach.PyAttachToProcessCommandLineState
import com.jetbrains.python.sdk.PythonSdkType
import java.net.ServerSocket

class MayaCharmDebugRunner : PyDebugRunner() {
    override fun getRunnerId(): String {
        return "MayaCharmDebugRunner"
    }

    override fun canRun(s: String, runProfile: RunProfile): Boolean {
        return runProfile is MayaCharmRunConfiguration && s == "Debug"
    }

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        // todo figure out windows vs mac pathing here
        val process = ProcessListUtil.getProcessList().firstOrNull{it.executableName == "maya.exe"} ?: return null

        val sdk = PythonSdkType.getAllLocalCPythons().firstOrNull(fun(x: Sdk): Boolean {
            val homePath = x.homePath ?: return false
            val newPath = ProjectSettings.MayaFromMayaPy(homePath)
            return newPath == process.commandLine.removeSurrounding("\"")
        }) ?: return null

        val runConfig = environment.runProfile as MayaCharmRunConfiguration

        val serverSocket = ServerSocket(0)
        val cliState = PyAttachToProcessCommandLineState.create(environment.project, sdk.homePath!!, serverSocket.localPort, process.pid)

        val executionResult = cliState.execute(environment.executor, this)

        val session = XDebuggerManager.getInstance(environment.project)
            .startSession(
                environment,
                object : XDebugProcessStarter() {
                    override fun start(session: XDebugSession): XDebugProcess {
                        val debugProcess = MayaCharmDebugProcess(
                                session,
                                serverSocket,
                                executionResult.executionConsole,
                                executionResult.processHandler,
                                false,
                                environment.project,
                                runConfig,
                                process
                        )
                        debugProcess.positionConverter = PyLocalPositionConverter()
                        createConsoleCommunicationAndSetupActions(environment.project, executionResult, debugProcess, session)
                        return debugProcess
                    }
                }
            )
        return session.runContentDescriptor
    }
}
