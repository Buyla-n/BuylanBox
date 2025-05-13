package com.buyla.application.data

import net.lingala.zip4j.model.FileHeader
import java.nio.file.Path

data class FileStateData(
    val path: Path,
    val filesInside: List<FileHeader>,
    val isInside : Boolean,
    val pathInside : String
)
