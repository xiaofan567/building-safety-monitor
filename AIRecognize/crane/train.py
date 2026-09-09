"""塔吊安全预测模型训练流程：python -m crane train。"""

import os

import numpy as np
import tensorflow as tf
from sklearn.metrics import classification_report
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau

from .callbacks import DangerFocusCallback
from .config import DANGER_NAME, MODEL_FILE, OUTPUT_DIR, SEQUENCE_LENGTH, WARN_NAME
from .data_prep import create_enhanced_sequences, load_data
from .model import build_model, set_class_indices

# 设置全局随机种子，保证结果可复现
np.random.seed(42)
tf.random.set_seed(42)


def get_class_weights(class_names):
    """根据类别名生成固定权重：危险8倍、预警4倍、其余1倍。"""
    return {
        i: (8.0 if name == DANGER_NAME else 4.0 if name == WARN_NAME else 1.0)
        for i, name in enumerate(class_names)
    }


def main():
    df, le = load_data()
    danger_idx = le.transform([DANGER_NAME])[0]
    warn_idx = le.transform([WARN_NAME])[0]
    set_class_indices(danger_idx, warn_idx)

    print("类别编码映射:", dict(zip(le.classes_, le.transform(le.classes_))))
    X, y = create_enhanced_sequences(df, danger_class=danger_idx)
    print("输入数据形状:", X.shape, "标签形状:", y.shape)

    # 按时间顺序划分训练/验证集
    split_idx = int(len(X) * 0.8)
    X_train, X_val = X[:split_idx], X[split_idx:]
    y_train, y_val = y[:split_idx], y[split_idx:]

    model = build_model((SEQUENCE_LENGTH, X.shape[2]), len(le.classes_))
    model.summary()

    class_weights = get_class_weights(list(le.classes_))
    print("\n类别权重分配:", class_weights)

    # 回调需在训练前绑定验证数据；DangerFocusCallback 放最前以先写入召回率
    danger_cb = DangerFocusCallback(danger_class=danger_idx)
    danger_cb._X_val, danger_cb._y_val = X_val, y_val

    model.fit(
        X_train, y_train,
        epochs=30,
        batch_size=256,
        validation_data=(X_val, y_val),
        class_weight=class_weights,
        callbacks=[
            danger_cb,
            EarlyStopping(
                patience=15, monitor="val_danger_recall", mode="max",
                restore_best_weights=True,
            ),
            ReduceLROnPlateau(monitor="val_loss", factor=0.5, patience=5, verbose=1),
        ],
        verbose=2,
    )

    # 最终评估
    y_pred = np.argmax(model.predict(X_val, verbose=0), axis=1)
    print("\n测试集详细评估:")
    print(classification_report(y_val, y_pred, target_names=le.classes_))

    # 保存模型与编码信息
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    model.save(MODEL_FILE)
    np.save(os.path.join(OUTPUT_DIR, "label_classes.npy"), le.classes_)
    print("\n模型已保存为", MODEL_FILE)


if __name__ == "__main__":
    main()