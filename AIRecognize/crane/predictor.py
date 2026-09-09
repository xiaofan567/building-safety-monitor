"""塔吊安全预测器：供 Flask 服务在启动时加载模型并对外提供 /crane_safety 预测。

接口契约（依据 Apifox《塔机AI报警预测》）：
    请求  POST /crane_safety  application/json
          {"crane_data": [{weight, amplitude, obliguity, windSpeed, moment,
                           height, rotation, monitorTime, assetId}, ...]}
    响应  {"status": "success",
           "last_10_safety_results": [ {data_index, safety_status,
                                         confidence, prob_distribution}, ... ],
           "request_info": {danger_count, safety_count, warning_count}}
"""

import os

import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.preprocessing import StandardScaler

from .config import (
    DANGER_NAME,
    DATA_FILE,
    FEATURE_COLS,
    LABEL_FILE,
    MODEL_FILE,
    SCALER_FILE,
    SEQUENCE_LENGTH,
    WARN_NAME,
)
from .model import weighted_focal_loss


class TowerSafetyPredictor:
    """塔吊安全预测器：负责加载模型、标准化与滑动窗口预测。"""

    def __init__(self):
        self.model = tf.keras.models.load_model(
            MODEL_FILE,
            custom_objects={"weighted_focal_loss": weighted_focal_loss},
        )
        self.class_names = list(np.load(LABEL_FILE, allow_pickle=True))
        self._mean, self._scale = self._load_scaler()

    def _load_scaler(self):
        """加载标准化参数（训练时保存的 mean/scale）；缺失则用训练数据拟合。"""
        if os.path.exists(SCALER_FILE):
            params = np.load(SCALER_FILE, allow_pickle=True).item()
            mean = np.asarray(params["mean"], dtype=np.float64)
            scale = np.asarray(params["scale"], dtype=np.float64)
        else:
            df = pd.read_csv(DATA_FILE)
            scaler = StandardScaler().fit(df[FEATURE_COLS].values)
            mean, scale = scaler.mean_, scaler.scale_
        # 防止某些特征方差为 0 导致除零
        scale = np.where(np.isfinite(scale) & (scale > 1e-8), scale, 1.0)
        return mean, scale

    def _standardize(self, rows):
        """将一条窗口（20×7）按训练时的 mean/scale 标准化。"""
        return (rows - self._mean) / self._scale

    def predict(self, crane_data):
        """对最近至多 10 个滑窗各预测一次，返回接口约定的响应体。"""
        if not crane_data:
            raise ValueError("crane_data 不能为空")

        try:
            features = np.asarray(
                [[float(r[col]) for col in FEATURE_COLS] for r in crane_data],
                dtype=np.float64,
            )
        except (KeyError, TypeError, ValueError):
            raise ValueError("crane_data 每项需包含 weight/amplitude/obliguity/windSpeed/moment/height/rotation")

        n = len(features)
        # 取最近至多 10 个窗口的结束下标
        last_indices = list(range(max(0, n - 10), n))

        windows = []
        for k in last_indices:
            window = features[max(0, k - SEQUENCE_LENGTH + 1): k + 1]
            pad = SEQUENCE_LENGTH - len(window)
            if pad > 0:  # 样本不足时用首行前向补齐到固定窗口长度
                window = np.concatenate([np.repeat(window[:1], pad, axis=0), window], axis=0)
            windows.append(self._standardize(window))
        X = np.asarray(windows, dtype=np.float32)  # (K, 20, 7)

        probs = self.model.predict(X, batch_size=256, verbose=0)  # (K, 3)

        last_10 = []
        danger = warning = safety = 0
        for k, prob in zip(last_indices, probs):
            cls = self.class_names[int(np.argmax(prob))]
            if cls == DANGER_NAME:
                danger += 1
            elif cls == WARN_NAME:
                warning += 1
            else:
                safety += 1
            last_10.append(
                {
                    "data_index": k,
                    "safety_status": cls,
                    "confidence": round(float(np.max(prob)), 4),
                    "prob_distribution": {
                        self.class_names[i]: round(float(p), 4)
                        for i, p in enumerate(prob)
                    },
                }
            )

        return {
            "status": "success",
            "last_10_safety_results": last_10,
            "request_info": {
                "danger_count": danger,
                "warning_count": warning,
                "safety_count": safety,
            },
        }