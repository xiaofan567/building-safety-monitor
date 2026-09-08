"""检测模型加载管理器。

Flask 服务启动时加载以下检测模型：
- 安全帽：DAMO-YOLO（modelscope pipeline）
- 香烟/吸烟：DAMO-YOLO（modelscope pipeline）
- 明火/烟火：DAMO-YOLO（modelscope pipeline）
- 安全衣：本地 ultralytics YOLO 权重（model/last.pt）
"""
import logging
import os

from modelscope.pipelines import pipeline
from modelscope.utils.constant import Tasks
from ultralytics import YOLO

logger = logging.getLogger(__name__)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
# 安全衣本地模型路径（model 目录下的 last.pt）
SAFETY_CLOTHING_MODEL_PATH = os.path.join(BASE_DIR, "model", "last.pt")

# DAMO-YOLO 垂类检测模型 ID
DAMOYOLO_HELMET_MODEL_ID = "iic/cv_tinynas_object-detection_damoyolo_safety-helmet"
DAMOYOLO_CIGARETTE_MODEL_ID = "damo/cv_tinynas_object-detection_damoyolo_cigarette"
DAMOYOLO_SMOKEFIRE_MODEL_ID = "iic/cv_tinynas_object-detection_damoyolo_smokefire"

# 各模型类别标签映射
HELMET_CLASSES = {1: "safety_hat", 2: "no_safety_hat"}   # 1=戴安全帽 2=未戴安全帽
CIGARETTE_CLASSES = {1: "cigarette"}                      # 1=香烟
SMOKEFIRE_CLASSES = {1: "smoke", 2: "fire"}               # 1=烟 2=火

TYPE_HELMET = "helmet"
TYPE_CIGARETTE = "cigarette"
TYPE_SMOKEFIRE = "smokefire"
TYPE_SAFETY_CLOTHING = "safety_clothing"


class ModelManager:
    """统一管理所有检测模型，服务启动时一次性加载。"""

    def __init__(self):
        self._models = {}

    def load_all(self):
        """加载全部检测模型，任一模型加载失败则抛出异常终止启动。"""

        # 1. 安全帽检测（DAMO-YOLO）
        logger.info("正在加载安全帽检测模型：%s", DAMOYOLO_HELMET_MODEL_ID)
        self._models[TYPE_HELMET] = pipeline(
            Tasks.domain_specific_object_detection,
            model=DAMOYOLO_HELMET_MODEL_ID,
            trust_remote_code=True,
        )
        logger.info("安全帽检测模型加载完成")

        # 2. 香烟/吸烟检测（DAMO-YOLO）
        logger.info("正在加载香烟检测模型：%s", DAMOYOLO_CIGARETTE_MODEL_ID)
        self._models[TYPE_CIGARETTE] = pipeline(
            Tasks.domain_specific_object_detection,
            model=DAMOYOLO_CIGARETTE_MODEL_ID,
            trust_remote_code=True,
        )
        logger.info("香烟检测模型加载完成")

        # 3. 明火/烟火检测（DAMO-YOLO）
        logger.info("正在加载烟火检测模型：%s", DAMOYOLO_SMOKEFIRE_MODEL_ID)
        self._models[TYPE_SMOKEFIRE] = pipeline(
            Tasks.domain_specific_object_detection,
            model=DAMOYOLO_SMOKEFIRE_MODEL_ID,
            trust_remote_code=True,
        )
        logger.info("烟火检测模型加载完成")

        # 4. 安全衣检测（本地 ultralytics YOLO）
        if not os.path.exists(SAFETY_CLOTHING_MODEL_PATH):
            raise FileNotFoundError(
                f"安全衣模型不存在：{SAFETY_CLOTHING_MODEL_PATH}"
            )
        logger.info("正在加载安全衣检测模型：%s", SAFETY_CLOTHING_MODEL_PATH)
        self._models[TYPE_SAFETY_CLOTHING] = YOLO(SAFETY_CLOTHING_MODEL_PATH)
        logger.info("安全衣检测模型加载完成")

        logger.info("全部检测模型加载完成")

    def get(self, model_type):
        """获取指定类型的检测模型。"""
        return self._models[model_type]

    def is_loaded(self):
        """判断模型是否已全部加载。"""
        return len(self._models) >= 4