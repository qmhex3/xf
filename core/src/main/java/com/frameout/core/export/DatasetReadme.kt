package com.frameout.core.export

object DatasetReadme {
    fun text(format: DatasetFormat, count: Int, project: String, rejected: Int = 0): String {
        val head = "$project\n$count sharp frames. frames.csv lists time, sharpness (0-100) and raw score per frame.\n" + rejectedNote(rejected) + "\n"
        return head + when (format) {
            DatasetFormat.Colmap -> colmap()
            DatasetFormat.Yolo -> yolo()
            DatasetFormat.Coco -> coco()
        }
    }

    private fun rejectedNote(rejected: Int): String =
        if (rejected == 0) "" else "$rejected frames were rejected as blurry and are kept in rejected/ for review (split column is rejected in frames.csv). They are not part of the dataset: move any you want back into images/.\n"

    private fun colmap(): String =
        "COLMAP / 3D reconstruction\n" +
            "1. Install COLMAP and make sure the colmap command works.\n" +
            "2. Run run_colmap.sh (Linux, macOS) or run_colmap.bat (Windows) from this folder.\n" +
            "3. The sparse model is written to sparse/ and the database to database.db.\n" +
            "Frames are in video order and share one camera (single_camera 1), which suits a phone video.\n" +
            "For Gaussian splatting or NeRF tools, point them at images/ and this folder.\n"

    private fun yolo(): String =
        "YOLO dataset (Ultralytics layout)\n" +
            "images/train and images/val hold the frames, labels/train and labels/val hold one empty .txt per frame.\n" +
            "The frames are not annotated. Label them in CVAT, Label Studio, Roboflow or LabelImg and export YOLO text files into labels/.\n" +
            "Edit data.yaml (nc and names) to match your classes, then train with: yolo detect train data=data.yaml\n"

    private fun coco(): String =
        "COCO dataset\n" +
            "images/train and images/val hold the frames, annotations/instances_train.json and annotations/instances_val.json list them.\n" +
            "The frames are not annotated: annotations is empty and categories holds one placeholder class named object.\n" +
            "Open the JSON files in CVAT, Label Studio or Roboflow, add your classes and boxes, and export again.\n"
}
