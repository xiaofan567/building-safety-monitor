"""建筑施工安全 AI 识别服务。

基于 Flask 搭建，服务启动时加载安全帽、安全衣、吸烟、明火等检测模型。
"""
import logging
import os
import tempfile

from flask import Flask, jsonify, request

from detector import detect_image
from model_loader import (
    ModelManager,
    TYPE_CIGARETTE,
    TYPE_HELMET,
    TYPE_SAFETY_CLOTHING,
    TYPE_SMOKEFIRE,
)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)


def create_app():
    """创建 Flask 应用并在启动时加载检测模型。"""
    app = Flask(__name__)

    # 模型管理器：服务启动时加载全部检测模型
    app.model_manager = ModelManager()
    app.model_manager.load_all()

    @app.get("/api/health")
    def health():
        """健康检查：返回服务状态及各模型加载情况。"""
        return jsonify(
            {
                "status": "ok",
                "models": {
                    "helmet": TYPE_HELMET in app.model_manager._models,
                    "cigarette": TYPE_CIGARETTE in app.model_manager._models,
                    "smokefire": TYPE_SMOKEFIRE in app.model_manager._models,
                    "safety_clothing": TYPE_SAFETY_CLOTHING in app.model_manager._models,
                },
            }
        )

    @app.post("/detect")
    def detect():
        """AI 危险源检测（multipart/form-data）。

        入参：
            image: 待检测图片文件（必填）
            type : 需检测的危险源类型数组（必填，如 ["1","3"]）
        返回：
            JSON 数组，每项包含 boxes/detection_name/total_objects/type/valid_count。
        """
        image_file = request.files.get("image")
        if image_file is None:
            return jsonify({"message": "缺少参数 image"}), 400
        if "type" not in request.form:
            return jsonify({"message": "缺少参数 type"}), 400

        types = request.form.getlist("type")

        suffix = "." + image_file.filename.rsplit(".", 1)[-1] if "." in image_file.filename else ""
        with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as tmp:
            image_file.save(tmp.name)
            tmp_path = tmp.name
        try:
            results = detect_image(app.model_manager, tmp_path, types)
        finally:
            try:
                os.remove(tmp_path)
            except OSError:
                pass

        return jsonify(results)

    return app


app = create_app()


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False)