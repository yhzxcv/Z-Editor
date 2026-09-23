package com.example.z_editor.datapack.ui

internal fun withTargetExtension(baseName: String, extension: String): String = when {
    baseName.endsWith(".$extension", ignoreCase = true) -> baseName
    baseName.endsWith(".") -> "$baseName$extension"
    else -> "$baseName.$extension"
}
