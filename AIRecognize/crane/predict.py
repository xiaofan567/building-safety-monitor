"""已有塔吊安全预测模型的评估与单条预测。"""

import os
from collections import Counter

import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.preprocessing import StandardScaler

from .config import DATA_FILE, FEATURE_COLS, LABEL_FILE, MODEL_FILE, SEQUENCE_LENGTH
from .model import weighted_focal_loss


def load_labeled_model():
    """加载模型及对应的类别名称列表。"""
    class_names = list(np.load(LABEL_FILE, allow_pickle=True))
    model = tf.keras.models.load_model(
        MODEL_FILE, custom_objects={"weighted_focal_loss": weighted_focal_loss}
    )
    return model, class_names


def _build_features(df):
    scaler = StandardScaler()
    return scaler.fit_transform(df[FEATURE_COLS].values)


def _make_sequences(scaled):
    """按时序窗口切分数据，返回形状为 (样本数, 窗口长度, 特征数) 的矩阵。"""
    n = len(scaled) - SEQUENCE_LENGTH
    return np.stack([scaled[i : i + SEQUENCE_LENGTH] for i in range(n)])


def evaluate():
    """滑动窗口批量评估整体准确率与各类别召回率。"""
    model, class_names = load_labeled_model()
    df = pd.read_csv(DATA_FILE)
    scaled = _build_features(df)
    actual = df["安全状态"].values

    X = _make_sequences(scaled)  # 一次性批量预测，避免逐条 predict 过慢
    probs = model.predict(X, batch_size=256, verbose=1)
    preds = [class_names[int(np.argmax(p))] for p in probs]

    correct, counts = Counter(), Counter()
    for true, pred in zip(actual[SEQUENCE_LENGTH:], preds):
        counts[true] += 1
        if pred == true:
            correct[true] += 1

    total = sum(counts.values())
    hits = sum(correct.values())
    print(f"\n真正例/总数   -> {hits}/{total}  整体准确率: {hits / max(total, 1):.4f}")
    for cls in class_names:
        c, ok = counts[cls], correct[cls]
        print(f"  {cls}: 正确 {ok}/{c}  召回率 {ok / max(c, 1):.4f}")


def predict_last():
    """用数据末尾一个窗口预测最新一条的安全状态。"""
    model, class_names = load_labeled_model()
    df = pd.read_csv(DATA_FILE)
    scaled = _build_features(df)
    seq = scaled[-SEQUENCE_LENGTH:][None, ...]
    prob = model.predict(seq, verbose=0)[0]
    pred = class_names[int(np.argmax(prob))]
    true = df["安全状态"].iloc[-1]
    probs = {class_names[i]: round(float(p), 4) for i, p in enumerate(prob)}
    print(f"最后一条记录真实状态: {true}")
    print(f"模型预测状态:        {pred}  (置信度 {float(np.max(prob)):.4f})")
    print("各类概率:", probs)