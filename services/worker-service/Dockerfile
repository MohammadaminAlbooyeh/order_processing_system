FROM python:3.11-slim

WORKDIR /app

COPY shared/ ./shared/
COPY services/worker-service/ .

RUN pip install --no-cache-dir -r requirements.txt

CMD ["celery", "-A", "tasks", "worker", "--loglevel=info"]
