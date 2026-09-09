"""塔吊安全预测模型相关配置与路径常量。"""

import os

# 包所在目录的上级（即 AIRecognize 根目录）
BASE_DIR = os.path.dirname(os.path.abspath(os.path.dirname(__file__)))

# 数据集与产物路径
DATA_FILE = os.path.join(BASE_DIR, "data", "tower_crane_data.csv")
OUTPUT_DIR = os.path.join(BASE_DIR, "outputs")
MODEL_FILE = os.path.join(OUTPUT_DIR, "enhanced_crane_model_v4.h5")
LABEL_FILE = os.path.join(OUTPUT_DIR, "label_classes.npy")
SCALER_FILE = os.path.join(OUTPUT_DIR, "scaler_params.npy")

# 模型与数据相关常量
SEQUENCE_LENGTH = 20  # 时序窗口长度
NOISE_LEVEL = 0.03  # 基础噪声水平
DANGER_NAME = "危险"  # 危险类别名称
WARN_NAME = "预警"  # 预警类别名称
FEATURE_COLS = [
    "weight",
    "amplitude",
    "obliguity",
    "windSpeed",
    "moment",
    "height",
    "rotation",
]