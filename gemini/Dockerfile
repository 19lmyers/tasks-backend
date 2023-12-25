FROM rocm/rocm-terminal:6.0

RUN sudo apt update && sudo apt install -y --no-install-recommends software-properties-common
RUN sudo add-apt-repository -y ppa:deadsnakes/ppa
RUN sudo apt install -y curl python3.11 python3.11-distutils
RUN curl -sS https://bootstrap.pypa.io/get-pip.py | python3.11
RUN sudo apt install -y rocm-libs rccl
RUN sudo ldconfig

EXPOSE 8124:8124

WORKDIR /usr/src/app

COPY requirements.txt ./
RUN python3.11 -m pip install --no-cache-dir -r requirements.txt

USER root
RUN mkdir -p /classifiers && chown -R rocm-user /classifiers
USER rocm-user

COPY . .

ENTRYPOINT [ "python3.11", "-m", "gemini_server" ]