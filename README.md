   # k8s-sentiment-ai
   
   Итоговый проект по дисциплине «Оркестрация и контейнеризация».
   
   Java REST API для анализа тональности текста, развёрнутое в Kubernetes с мониторингом Prometheus+Grafana.
   
   ## Структура
   - `SentimentApp.java` — Java SE REST API с метриками Prometheus
   - `Dockerfile` — multi-stage сборка с jlink (образ 44.9 МБ)
   - `k8s-deployment.yaml` — Deployment (3 реплики), Service (LoadBalancer), Ingress, HPA
   
   ## Сборка и запуск
   ```bash
   docker build -t sentiment-app:1.0.0 .
   docker run -p 8080:8080 sentiment-app:1.0.0
