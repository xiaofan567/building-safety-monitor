"""塔吊安全预测模型命令行入口。

用法（在 AIRecognize 目录下执行）：
    python -m crane train        # 训练并保存模型
    python -m crane evaluate     # 评估验证集
    python -m crane predict      # 预测数据最后一条记录
"""

import argparse


def main():
    parser = argparse.ArgumentParser(description="塔吊安全预测模型工具")
    sub = parser.add_subparsers(dest="cmd", required=True)
    sub.add_parser("train", help="训练并保存模型")
    sub.add_parser("evaluate", help="评估验证集")
    sub.add_parser("predict", help="预测数据最后一条记录")
    args = parser.parse_args()

    if args.cmd == "train":
        from .train import main as train_main

        train_main()
    elif args.cmd == "evaluate":
        from .predict import evaluate

        evaluate()
    elif args.cmd == "predict":
        from .predict import predict_last

        predict_last()


if __name__ == "__main__":
    main()