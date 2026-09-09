"""训练期间针对危险类的监控回调。"""

import numpy as np
import tensorflow as tf
from sklearn.metrics import recall_score
from tensorflow.keras.callbacks import Callback


class DangerFocusCallback(Callback):
    """记录危险类召回率，召回率过低时调低学习率以促进探索。"""

    def __init__(self, danger_class=0, min_recall=0.2):
        super().__init__()
        self.danger_class = danger_class
        self.min_recall = min_recall  # 危险类最低可接受召回率
        self._X_val = None
        self._y_val = None

    def on_epoch_end(self, epoch, logs=None):
        pred = np.argmax(self.model.predict(self._X_val, verbose=0), axis=1)
        recall = recall_score(
            self._y_val, pred, labels=[self.danger_class], average=None
        )[0]
        logs["val_danger_recall"] = recall  # 供 EarlyStopping 等后续回调监控
        print(f" - val_danger_recall: {recall:.4f}")

        if recall < self.min_recall and epoch > 5:
            current_lr = float(tf.keras.backend.get_value(self.model.optimizer.lr))
            new_lr = current_lr * 0.7
            tf.keras.backend.set_value(self.model.optimizer.lr, new_lr)
            print(f"  危险类召回率过低，调整学习率至: {new_lr:.6f}")