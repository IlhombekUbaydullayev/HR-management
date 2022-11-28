package com.example.hrmanagement

fun Boolean.throwIfFalse(function: () -> Throwable) {
    if (!this) throw function()
}

fun Boolean.throwIfTrue(function: () -> Throwable) {
    if (this) throw function()
}

fun getFileExtension(fileName: String): String? {
    val dot = fileName.lastIndexOf(".")
    if (dot > 0 && dot <= fileName.length-2){
        return fileName.substring(dot+1)
    }
    return null
}
