import pandas as pd
import numpy as np
import tensorflow.keras as keras
from google.api_core import retry

import google.generativeai as genai

from tensorflow.keras import layers
from tensorflow.math import argmax

# Load labels into memory for later
grocery_labels = {}
with open("data/shopping_labels.txt") as file:
    for line in file:
        label, category = line.partition("=")[::2]
        grocery_labels[int(label)] = category.strip()

# Load training set into memory
grocery_training_set = pd.read_csv(
    'data/shopping_training_set.csv',
    names=['Item', 'Category'])

# Load validation set into memory
grocery_validation_set = pd.read_csv(
    'data/shopping_validation_set.csv',
    names=['Item', 'Category'])

model = 'models/embedding-001'


def make_embed_text_fn(model):
    @retry.Retry(timeout=300.0)
    def embed_fn(text: str) -> list[float]:
        # Set the task_type to CLASSIFICATION.
        embedding = genai.embed_content(model=model,
                                        content=text,
                                        task_type="classification")
        return embedding['embedding']

    return embed_fn


def create_embeddings(model, df):
    df['Embedding'] = df['Item'].apply(make_embed_text_fn(model))
    return df


def build_classification_model(input_size: int, num_classes: int) -> keras.Model:
    inputs = x = keras.Input([input_size])
    x = layers.Dense(input_size, activation='relu')(x)
    x = layers.Dense(num_classes, activation='softmax')(x)
    return keras.Model(inputs=[inputs], outputs=x)


def create() -> keras.Model:
    # Create embeddings
    training_set = create_embeddings(model, grocery_training_set)
    validation_set = create_embeddings(model, grocery_validation_set)

    # Split the x and y components of the train and validation subsets
    y_train = training_set['Category']
    x_train = np.stack(training_set['Embedding'])
    y_val = validation_set['Category']
    x_val = np.stack(validation_set['Embedding'])

    # Derive the embedding size from the first training element
    embedding_size = len(training_set['Embedding'].iloc[0])

    # Create classifier for categories
    classifier = build_classification_model(embedding_size, len(training_set['Category'].unique()))

    classifier.compile(loss=keras.losses.SparseCategoricalCrossentropy(),
                       optimizer=keras.optimizers.Adam(learning_rate=0.001),
                       metrics=['accuracy'])

    # Define constants for training
    num_epochs = 30

    # Train the classifier for the desired number of epochs
    callback = keras.callbacks.EarlyStopping(monitor='accuracy', patience=3)
    # Train classifier
    history = classifier.fit(x=x_train,
                             y=y_train,
                             validation_data=(x_val, y_val),
                             callbacks=[callback],
                             epochs=num_epochs,
                             verbose=0)

    return classifier


def predict(classifier: keras.Model, input_item: str) -> str:
    input_df = pd.DataFrame({'Item': [input_item]})
    embeddings = np.stack(create_embeddings(model, input_df)['Embedding'])

    probabilities = classifier.predict(embeddings, verbose=0)
    return grocery_labels[argmax(probabilities, axis=1).numpy().item()]