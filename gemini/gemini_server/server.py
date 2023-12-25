import asyncio
import json

from websockets import WebSocketServerProtocol
from websockets.server import serve

from multiprocessing import Process, Queue
import logging

import gemini_common.ai.classifiers as classifiers

logger = logging.getLogger()
logger.setLevel(logging.DEBUG)

classifiers.init(None)

classifier_id = "shopping"  # Currently this is hardcoded for performance reasons


def process_prediction(input, queue):
    result = classifiers.predict(classifier_id, input)
    queue.put(result)


async def connection(websocket: WebSocketServerProtocol):
    try:
        label = await websocket.recv()

        queue = Queue()

        process = Process(target=process_prediction, args=(label, queue))

        process.start()

        while process.exitcode is None:
            await asyncio.sleep(1)

        return_code = process.exitcode

        payload = {'status': return_code}

        if return_code == 0:
            result = queue.get()

            if result.is_ok():
                payload['result'] = result.unwrap().rstrip()
            else:
                payload['result'] = str(result.unwrap_err())

        else:
            payload['result'] = "Unknown Error"

        await websocket.send(json.dumps(payload))

        await websocket.close()

    except Exception as e:
        logging.exception(e)


async def main():
    await serve(connection, port=8124)
    logging.info("Running server on port 8124")
    await asyncio.Future()
