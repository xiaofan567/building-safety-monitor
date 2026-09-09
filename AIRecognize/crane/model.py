"""CNN + 双向LSTM + 注意力 混合塔吊安全预测模型。"""

import tensorflow as tf
from tensorflow.keras.layers import (
    Attention,
    BatchNormalization,
    Bidirectional,
    Concatenate,
    Conv1D,
    Dense,
    Dropout,
    Input,
    LSTM,
    Masking,
    MaxPooling1D,
)
from tensorflow.keras.models import Model

# 类别索引（由 LabelEncoder 对 危险/安全/预警 排序得出）：危险=0、安全=1、预警=2
danger_idx = 0
warn_idx = 2


def set_class_indices(danger, warn):
    """根据训练时的标签编码结果同步全局类别索引，供损失函数与模型加载使用。"""
    global danger_idx, warn_idx
    danger_idx, warn_idx = danger, warn


def weighted_focal_loss(y_true, y_pred):
    """加权 Focal Loss：危险类权重最高、预警次之、安全最低。"""
    alpha = tf.where(tf.equal(y_true, danger_idx), 0.5,
                     tf.where(tf.equal(y_true, warn_idx), 0.4, 0.1))
    gamma = 2.0
    ce_loss = tf.keras.losses.sparse_categorical_crossentropy(y_true, y_pred)
    pt = tf.exp(-ce_loss)
    return tf.reduce_mean(alpha * tf.pow(1 - pt, gamma) * ce_loss)


def build_model(input_shape, num_classes):
    """构建模型：局部特征提取 -> 时序依赖建模 -> 注意力增强 -> 分类输出。"""
    inputs = Input(shape=input_shape)

    # 局部特征提取层：多尺度卷积捕捉瞬时异常与短期趋势
    x = Masking(mask_value=-100.0)(inputs)
    x = Conv1D(64, kernel_size=3, activation="relu", padding="same")(x)
    x = BatchNormalization()(x)
    x = Conv1D(128, kernel_size=5, activation="relu", padding="same")(x)
    x = MaxPooling1D(pool_size=2)(x)

    # 时序依赖建模层：双向LSTM捕捉长期依赖
    lstm_out = Bidirectional(LSTM(192, return_sequences=True))(x)

    # 注意力机制：突出关键危险时刻
    query = Dense(96)(lstm_out)
    value = Dense(96)(lstm_out)
    attention = Attention()([query, value])
    x = Concatenate(axis=-1)([lstm_out, attention])

    # 高级特征整合与分类输出
    x = Bidirectional(LSTM(96, dropout=0.2, recurrent_dropout=0.2))(x)
    x = BatchNormalization()(x)
    x = Dense(128, activation="relu", kernel_regularizer="l2")(x)
    x = Dropout(0.4)(x)
    outputs = Dense(num_classes, activation="softmax")(x)

    model = Model(inputs=inputs, outputs=outputs)
    model.compile(
        optimizer=tf.keras.optimizers.Nadam(learning_rate=0.0005),
        loss=weighted_focal_loss,
        metrics=["accuracy"],
    )
    return model