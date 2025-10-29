##Task Tracker Microservice Project - Makefile
##Упрощенное управление Docker Compose окружением

##Переменные
COMPOSE_FILE = docker-compose.yml
ENV_FILE = .env

##Цвета для вывода
GREEN = \033[0;32m
YELLOW = \033[1;33m
RED = \033[0;31m
NC = \033[0m # No Color

##Метки, которые не являются файлами
.PHONY: help up down logs ps clean-kafka

##По умолчанию показываем справку
.DEFAULT_GOAL := help

up: ## Запуск всех контейнеров
	@echo "$(GREEN)Запуск Task Tracker...$(NC)"
	docker compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE) up -d
	@echo "$(GREEN)✓ Все контейнеры запущены$(NC)"

down: ## Остановка и удаление всех контейнеров
	@echo "$(GREEN)Остановка Task Tracker...$(NC)"
	docker compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE) down
	@echo "$(GREEN)✓ Контейнеры остановлены$(NC)"

logs: ## Просмотр логов контейнеров (с флагом -f для слежения)
	@echo "$(GREEN)Просмотр логов Task Tracker...$(NC)"
	docker compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE) logs -f
	ps: ## Показать статус контейнеров
	@echo "$(GREEN)Статус контейнеров Task Tracker...$(NC)"
	docker compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE) ps

clean-kafka: ## Очистка Kafka (удаление контейнеров и volumes)
	@echo "$(GREEN)Очистка Kafka...$(NC)"
	docker compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE) rm -f zookeeper kafka
	docker compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE) down -v zookeeper kafka
	@echo "$(GREEN)✓ Kafka очищен$(NC)"