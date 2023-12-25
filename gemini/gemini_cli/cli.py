import os
import sys
from typing import Annotated, Optional

import typer
from result import as_result

import gemini_common.ai.classifiers as classifiers
import gemini_common.constants as constants

app = typer.Typer()


@app.command(help="Create a new classifier.")
def create(
        classifier_id: Annotated[str, typer.Argument()],
        classifier_type: Annotated[classifiers.ClassifierType, typer.Argument()],
        api_key: Annotated[Optional[str], typer.Argument()] = None,
):
    def create_classifier(_):
        return classifiers.create(classifier_id, classifier_type)

    result = classifiers.init(api_key).and_then(create_classifier)

    if result.is_ok():
        print(result.unwrap())
    else:
        ex = result.unwrap_err()
        print(ex, file=sys.stderr)
        raise typer.Exit(code=-1)


@app.command(name="list", help="List the existing classifiers.")
def list_existing():
    result = list_func()

    if result.is_ok():
        classifier_list = result.unwrap()

        print(*classifier_list, sep='\n')
    else:
        ex = result.unwrap_err()
        print(ex, file=sys.stderr)
        raise typer.Exit(code=-1)


@as_result(OSError)
def list_func() -> list[str]:
    def remove_ext(classifier_name):
        return os.path.splitext(classifier_name)[0]

    return list(map(remove_ext, os.listdir(constants.classifier_store_path)))


@app.command(help="Predict a category for an item with a given classifier.")
def predict(
        classifier_id: Annotated[str, typer.Argument()],
        input_item: Annotated[str, typer.Argument()],
        api_key: Annotated[Optional[str], typer.Argument()] = None,
):
    def predict_category(_):
        return classifiers.predict(classifier_id, input_item)

    result = classifiers.init(api_key).and_then(predict_category)

    if result.is_ok():
        print(result.unwrap())
    else:
        ex = result.unwrap_err()
        print(ex, file=sys.stderr)
        raise typer.Exit(code=-1)


@app.command(help="Delete the given classifier.")
def delete(classifier_id: Annotated[str, typer.Argument()], ):
    result = classifiers.delete(classifier_id)
    if result.is_ok():
        print(result.unwrap())
    else:
        ex = result.unwrap_err()
        print(ex, file=sys.stderr)
        raise typer.Exit(code=-1)
