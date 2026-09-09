"""塔吊时序数据的读取、标准化与序列化。"""

import numpy as np
import pandas as pd
from sklearn.preprocessing import LabelEncoder, StandardScaler

from .config import DATA_FILE, FEATURE_COLS, NOISE_LEVEL, SEQUENCE_LENGTH


def load_data():
    """读取运行数据并做安全状态标签编码，返回 (df, LabelEncoder)。"""
    df = pd.read_csv(DATA_FILE)
    le = LabelEncoder()
    df["安全状态编码"] = le.fit_transform(df["安全状态"])
    return df, le


def create_enhanced_sequences(
    data, seq_length=SEQUENCE_LENGTH, noise_level=NOISE_LEVEL, danger_class=2
):
    """生成带噪声增强的时序数据，增加危险样本的特征区分度。"""
    scaler = StandardScaler()
    scaled = scaler.fit_transform(data[FEATURE_COLS].values)

    labels = data["安全状态编码"].values
    X, y = [], []
    for i in range(len(scaled) - seq_length):
        seq = scaled[i : i + seq_length]
        current_label = labels[i + seq_length]
        # 危险样本使用更高噪声以提升泛化能力
        lvl = max(noise_level, 0.05) if current_label == danger_class else noise_level
        if current_label == danger_class or np.random.rand() < 0.5:
            seq = np.clip(seq + np.random.normal(0, lvl, seq.shape), -3, 3)
        X.append(seq)
        y.append(current_label)
    return np.array(X), np.array(y).flatten()