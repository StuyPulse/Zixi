FROM eclipse-temurin:25-jdk

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    git \
    curl \
    ca-certificates \
    python3 && \
    curl -fsSL https://deb.nodesource.com/setup_22.x | bash - && \
    apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY worker/server.py ./server.py
COPY assets/ ./assets/
COPY scripts/ ./scripts/

ENV PYTHONUNBUFFERED=1

EXPOSE 10000

CMD ["python3", "server.py"]