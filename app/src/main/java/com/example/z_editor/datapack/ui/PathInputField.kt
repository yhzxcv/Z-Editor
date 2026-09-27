package com.example.z_editor.datapack.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.z_editor.data.repository.LevelRepository
import com.example.z_editor.views.components.OpenDocumentTreeFixed

/**
 * 数据包层输入框的统一配色。
 *
 * 本来是各页各写一份，正巧六处写的是同一个方案（`onSurfaceVariant` 描边 + 主题色聚焦态），
 * 于是抽出来给 [PathInputField] 和预览页的搜索框共用 —— 这两处是最容易各自走样的，
 * 一个不带 `unfocusedBorderColor` 就会退回 M3 默认的 `outline`，摆在别的框旁边一眼看出不同。
 */
@Composable
internal fun datapackFieldColors(themeColor: Color) = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedBorderColor = themeColor,
    focusedLabelColor = themeColor,
    cursorColor = themeColor,
)

/** [PathInputField] 的按钮能选什么。 */
enum class PathPick {
    /** 只能选文件，点按钮直接开文件选择器。 */
    File,

    /** 只能选目录，点按钮直接开目录选择器。 */
    Directory,

    /** 文件或目录都行（本页两种输入都接受时用），点按钮弹一个小菜单让用户挑。 */
    FileOrDirectory,
}

/**
 * 路径输入框：既能手打/粘贴，也能点右边的按钮从系统选择器里挑。
 *
 * 数据包层这一批页面全都只认**真实路径字符串**（`java.io.File`），所以选完要换算：
 * [LevelRepository.realPathStringOf] 认得 tree uri 和 document uri 两种形态，
 * 换算不出来（云盘、下载项、`Android/data` 等没有真实路径的位置）时**只弹提示、不动输入框** ——
 * 静默填进去一个空串或半截路径，用户会以为选上了。
 *
 * 换算成功后存下来的是**字符串**，之后不再碰这个 uri，所以**不 take 持久化权限**
 * （与 `realPathStringOf` 的说明一致：未持久化的 uri 重启后就是死的，存它反而是陷阱）。
 *
 * 本组件只管"选一个路径填进去"，**不校验**它是不是文件/是不是目录、存不存在 ——
 * 那是各页「解析路径」按钮的活，各页的报错文案也比这里更具体。
 */
@Composable
fun PathInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    themeColor: Color,
    pick: PathPick = PathPick.FileOrDirectory,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    val applyPicked: (Uri?) -> Unit = { uri ->
        when (val picked = uri?.let { LevelRepository.realPathStringOf(it) }) {
            null -> if (uri != null) {
                Toast.makeText(
                    context,
                    "这个位置换算不出本地路径（云盘、下载项或 Android/data），请手动输入",
                    Toast.LENGTH_LONG
                ).show()
            }

            else -> onValueChange(picked)
        }
    }

    val pickFile = rememberLauncherForActivityResult(OpenDocumentAtStorage(), applyPicked)
    val pickDir = rememberLauncherForActivityResult(OpenDocumentTreeFixed(), applyPicked)

    // `*/*` 而不是 .pam/.json 之类的过滤器：PAM/RTON 没有注册 mime type，
    // 按类型过滤会把它们直接从列表里藏掉，用户根本看不见要选的文件。
    fun launchFile() = pickFile.launch(arrayOf("*/*"))

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        trailingIcon = {
            Box {
                IconButton(
                    onClick = {
                        when (pick) {
                            PathPick.File -> launchFile()
                            PathPick.Directory -> pickDir.launch(null)
                            PathPick.FileOrDirectory -> menuOpen = true
                        }
                    },
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = if (pick == PathPick.File) {
                            Icons.AutoMirrored.Filled.InsertDriveFile
                        } else {
                            Icons.Default.FolderOpen
                        },
                        contentDescription = when (pick) {
                            PathPick.File -> "选择文件"
                            PathPick.Directory -> "选择目录"
                            PathPick.FileOrDirectory -> "选择文件或目录"
                        },
                        tint = if (enabled) themeColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("选择文件") },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null)
                        },
                        onClick = {
                            menuOpen = false
                            launchFile()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("选择文件夹") },
                        leadingIcon = { Icon(Icons.Default.FolderOpen, null) },
                        onClick = {
                            menuOpen = false
                            pickDir.launch(null)
                        }
                    )
                }
            }
        },
        colors = datapackFieldColors(themeColor)
    )
}

/**
 * `ACTION_OPEN_DOCUMENT`，但让它从**内部存储根**开始，而不是系统默认的「最近」——
 * 数据包层要选的文件在解包目录里，「最近」页面对这个场景基本没用。
 *
 * 这条 `EXTRA_INITIAL_URI` 只是个 hint：非 AOSP ROM 上认不出这个 authority 时系统会忽略它，
 * 退回默认行为，不会报错。真在某些机型上出怪问题，把 [createIntent] 整个删掉、
 * 直接用 `ActivityResultContracts.OpenDocument()` 即可。
 */
private class OpenDocumentAtStorage : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent =
        super.createIntent(context, input).putExtra(
            DocumentsContract.EXTRA_INITIAL_URI,
            Uri.parse("content://com.android.externalstorage.documents/document/primary%3A")
        )
}
