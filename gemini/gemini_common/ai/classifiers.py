import os
from typing import Optional

from gemini_common.ai import shopping
from gemini_common.constants import classifier_store_path, ClassifierType

import google.generativeai as genai
import tensorflow.keras as keras

from google.api_core.exceptions import InvalidArgument
from google.auth.exceptions import DefaultCredentialsError
from result import as_result


@as_result(InvalidArgument, DefaultCredentialsError)
def init(api_key: Optional[str]):
    if api_key is not None:
        genai.configure(api_key=api_key)
    else:
        genai.configure()


def init_model(classifier_type: ClassifierType) -> keras.Model:
    match classifier_type:
        case ClassifierType.SHOPPING:
            return shopping.create()


@as_result(InvalidArgument, DefaultCredentialsError, FileExistsError)
def create(classifier_id: str, classifier_type: ClassifierType):
    classifier_path = '{0}/{1}.keras'.format(classifier_store_path, classifier_id)

    if os.path.isfile(classifier_path):
        raise FileExistsError("{0} already exists".format(classifier_path))

    os.makedirs(os.path.dirname(classifier_path), exist_ok=True)

    classifier = init_model(classifier_type)

    classifier.save(classifier_path)

    return classifier_id


@as_result(InvalidArgument, DefaultCredentialsError, ValueError)
def predict(classifier_id: str, input_item: str):
    classifier_path = '{0}/{1}.keras'.format(classifier_store_path, classifier_id)

    classifier = keras.models.load_model(classifier_path)

    return shopping.predict(classifier, input_item)


@as_result(OSError)
def delete(classifier_id: str):
    classifier_path = '{0}/{1}.keras'.format(classifier_store_path, classifier_id)
    os.remove(classifier_path)
    return classifier_id
