"""AI 危险源检测核心逻辑。

将已加载的检测模型输出统一解析为接口约定的响应格式：
[
    {
        "boxes": [{"confidence": 0.7, "x1": ..., "y1": ..., "x2": ..., "y2": ...}],
        "detection_name": "未戴安全帽",
        "total_objects": 1,
        "type": 1,
        "valid_count": 1
    }
]
"""
import logging

from model_loader import (
    ModelManager,
    TYPE_CIGARETTE,
    TYPE_HELMET,
    TYPE_SAFETY_CLOTHING,
    TYPE_SMOKEFIRE,
)

logger = logging.getLogger(__name__)

# 置信度阈值：低于该值的目标不作为有效危险源
CONFIDENCE_THRESHOLD = 0.4

# 危险源类型映射：
#   type -> {模型标识, 触发危险源的类别集合, 展示名称, DAMO-YOLO标签名->类别ID映射}
# 类别编号依据各模型 label_map.pkl：
#   安全帽   : {1:'safety hat', 2:'no safety hat'}   -> 未戴安全帽 = 2
#   安全衣   : 0=nor_cloths(普通/未穿) 1=ref_cloths(反光安全衣)
#   香烟     : {1:'cigarette'}                        -> 吸烟 = 1
#   明火烟火 : {1:'background', 2:'smoke', 3:'fire'}  -> 明火 = 3
DETECTION_TYPES = {
    1: {
        "model": TYPE_HELMET,
        "danger_labels": {2},
        "name": "未戴安全帽",
        "label_map": {"safety hat": 1, "no safety hat": 2},
    },
    2: {"model": TYPE_SAFETY_CLOTHING, "danger_labels": {0}, "name": "未穿安全衣"},
    3: {
        "model": TYPE_CIGARETTE,
        "danger_labels": {1},
        "name": "吸烟",
        "label_map": {"cigarette": 1},
    },
    4: {
        "model": TYPE_SMOKEFIRE,
        "danger_labels": {3},
        "name": "明火",
        "label_map": {"background": 1, "smoke": 2, "fire": 3},
    },
}


def _parse_damoyolo(result, danger_labels, threshold, label_map=None):
    """解析 modelscope DAMO-YOLO pipeline 输出。

    labels 为模型返回的类别名（如 'no safety hat'），需通过 label_map 转成类别 ID。

    返回 (total_objects, valid_boxes, valid_count)
    """
    label_map = label_map or {}
    labels = []
    for v in result.get("labels", []):
        if isinstance(v, (int, float)):
            labels.append(int(v))
        else:
            # 类别名 -> 类别ID；无法识别则视为背景，排除
            labels.append(label_map.get(str(v), -1))
    scores = [float(v) for v in result.get("scores", [])]
    boxes = result.get("boxes", [])

    total_objects = 0
    valid_boxes = []
    valid_count = 0
    for label, score, box in zip(labels, scores, boxes):
        if label not in danger_labels:
            continue
        total_objects += 1
        if score < threshold:
            continue
        valid_count += 1
        x1, y1, x2, y2 = (float(v) for v in box)
        valid_boxes.append(
            {
                "confidence": round(score, 4),
                "x1": round(x1, 2),
                "y1": round(y1, 2),
                "x2": round(x2, 2),
                "y2": round(y2, 2),
            }
        )
    return total_objects, valid_boxes, valid_count


def _parse_yolo(result, danger_labels, threshold):
    """解析 ultralytics YOLO 输出（用于安全衣模型）。"""
    total_objects = 0
    valid_boxes = []
    valid_count = 0
    for r in result:
        if r.boxes is None:
            continue
        confs = r.boxes.conf.cpu().tolist()
        cls_ids = r.boxes.cls.cpu().int().tolist()
        xyxy = r.boxes.xyxy.cpu().tolist()
        for conf, cls_id, (x1, y1, x2, y2) in zip(confs, cls_ids, xyxy):
            if cls_id not in danger_labels:
                continue
            total_objects += 1
            if conf < threshold:
                continue
            valid_count += 1
            valid_boxes.append(
                {
                    "confidence": round(float(conf), 4),
                    "x1": round(float(x1), 2),
                    "y1": round(float(y1), 2),
                    "x2": round(float(x2), 2),
                    "y2": round(float(y2), 2),
                }
            )
    return total_objects, valid_boxes, valid_count


def detect_image(model_manager: ModelManager, image_path: str, types):
    """对单张图片执行危险源检测。

    参数：
        model_manager: 已加载全部模型的 ModelManager 实例
        image_path: 待检测图片路径
        types: 需要检测的危险源类型列表（如 [1, 2, 3, 4]）

    返回：
        符合接口约定的检测结果列表。
    """
    results = []
    for type_id in types:
        type_id = int(type_id)
        if type_id not in DETECTION_TYPES:
            logger.warning("未知的危险源类型：%s，跳过", type_id)
            continue

        cfg = DETECTION_TYPES[type_id]
        model = model_manager.get(cfg["model"])

        if cfg["model"] == TYPE_SAFETY_CLOTHING:
            # ultralytics YOLO 模型
            raw = model.predict(source=image_path, verbose=False)
            total_objects, valid_boxes, valid_count = _parse_yolo(
                raw, cfg["danger_labels"], CONFIDENCE_THRESHOLD
            )
        else:
            # modelscope DAMO-YOLO pipeline
            raw = model(image_path)
            total_objects, valid_boxes, valid_count = _parse_damoyolo(
                raw, cfg["danger_labels"], CONFIDENCE_THRESHOLD, cfg.get("label_map")
            )

        results.append(
            {
                "boxes": valid_boxes,
                "detection_name": cfg["name"],
                "total_objects": total_objects,
                "type": type_id,
                "valid_count": valid_count,
            }
        )
    return results