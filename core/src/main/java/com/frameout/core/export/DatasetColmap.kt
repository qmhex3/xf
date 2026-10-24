package com.frameout.core.export

object DatasetColmap {
    const val SH_NAME = "run_colmap.sh"
    const val BAT_NAME = "run_colmap.bat"

    fun shell(): String =
        "#!/usr/bin/env bash\nset -e\ncd \"\$(dirname \"\$0\")\"\nmkdir -p sparse\n" +
            "colmap automatic_reconstructor --workspace_path . --image_path images --data_type video --single_camera 1 --quality high --dense 0\n"

    fun batch(): String =
        "@echo off\r\ncd /d \"%~dp0\"\r\nif not exist sparse mkdir sparse\r\n" +
            "colmap automatic_reconstructor --workspace_path . --image_path images --data_type video --single_camera 1 --quality high --dense 0\r\n"
}
