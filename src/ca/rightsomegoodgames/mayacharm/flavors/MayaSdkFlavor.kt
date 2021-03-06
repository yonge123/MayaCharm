package ca.rightsomegoodgames.mayacharm.flavors

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.python.sdk.flavors.PythonFlavorProvider
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import icons.PythonIcons
import java.io.File
import javax.swing.Icon

class MayaSdkFlavor private constructor() : PythonSdkFlavor() {
    override fun isValidSdkHome(path: String?): Boolean {
        val file = File(path)
        return file.isFile && isValidSdkPath(file) || isMayaFolder(file)
    }

    override fun isValidSdkPath(file: File): Boolean {
        val name = FileUtil.getNameWithoutExtension(file).toLowerCase()
        return name.startsWith("mayapy")
    }

    override fun getVersionOption(): String {
        return "--version"
    }

    override fun getName(): String {
        return "MayaPy"
    }

    override fun getIcon(): Icon {
        return PythonIcons.Python.Python
    }

    override fun getSdkPath(path: VirtualFile): VirtualFile? {
        if (isMayaFolder(File(path.path))) {
            return path.findFileByRelativePath("Contents/bin/mayapy")
        }
        return path
    }

    companion object {
        val INSTANCE: MayaSdkFlavor = MayaSdkFlavor()

        private fun isMayaFolder(file: File) : Boolean {
            return file.isDirectory && file.name == "Maya.app"
        }
    }
}

class MayaFlavorProvider : PythonFlavorProvider {
    override fun getFlavor(platformIndependent: Boolean): PythonSdkFlavor? = MayaSdkFlavor.INSTANCE
}
