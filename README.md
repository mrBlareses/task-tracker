# TASK TRACKER Microservice Platform

![Java](https://img.shields.io/badge/Java-24-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![Kafka](https://img.shields.io/badge/Kafka-Apache-black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)
![Build](https://img.shields.io/badge/build-gradle-success)

## Содержание

- [Архитектура](#архитектура)
- [Диаграммы C4](#диаграммы-c4)
- [Состав репозитория](#состав-репозитория)
- [Стек технологий](#стек-технологий)
- [Предварительные требования](#предварительные-требования)
- [Быстрый старт](#быстрый-старт)
- [Ручной запуск](#ручной-запуск-без-makefile)
- [Docker команды](#docker-команды)
- [Gradle команды](#gradle-команды)
- [Порты и сервисы](#порты-и-сервисы)
- [Debugging / Диагностика](#debugging--диагностика)
- [Типовые ошибки](#типовые-ошибки)
- [Конфигурация](#конфигурация)
- [API документация](#api-документация)
- [Postman коллекция](#postman-коллекция)
- [Структура каталогов](#структура-каталогов)

---

## Архитектура

Платформа представляет собой многопользовательский планировщик задач, вдохновленный Trello. 
Пользователи могут использовать его как TODO-лист. Архитектура основана на микросервисах с использованием Spring Boot.

Основные компоненты:
- Backend Service: REST API для регистрации, авторизации (JWT), работы с задачами (создание, редактирование, удаление, пометка как выполненной).
- Scheduler Service: Генерация ежедневных отчетов по задачам (Spring Scheduler запускает задачи по cron в полночь).
- Email Sender Service: Отправка email (приветственные письма при регистрации, ежедневные отчеты) через Mailjet с использованием Spring Mail.
- Postgres: Хранение пользователей и задач (отношение один-ко-многим).
- Kafka + Zookeeper: Брокер сообщений для асинхронной отправки задач на email.
- Mailjet: Внешний SMTP-сервис для реальной отправки писем.
---

## Диаграммы C4
Описание контейнеров (сервисов) и их взаимодействия.
Технологический стек каждого компонента.

### [Контекстная диаграмма](diagrams/context.puml) 
- Описание взаимодействия системы с внешними пользователями и системами
- Высокоуровневый обзор

> 💡 **Примечание**: Для просмотра диаграмм используйте [PlantUML](https://plantuml.com/) или установите плагин для отображения .puml файлов в вашей IDE.
---

## Состав репозитория

- `backend-service/` — основной backend сервис (Spring Boot REST API).
- `diagrams/` — C4-диаграммы архитектуры (PlantUML).
- `docker-compose.yml` — инфраструктура проекта (PostgreSQL, Kafka, Zookeeper, backend-service).
- `.env.example` — пример переменных окружения.
- `.env` — локальные переменные окружения (в `.gitignore`).
- `Taskfile.yml` — команды для управления окружением и сборкой проекта.
- `README.md` — документация проекта.

# Стек технологий

- **Java:** 24
- **Framework:** Spring Boot
- **Database:** PostgreSQL
- **Messaging:** Apache Kafka + Zookeeper
- **Containerization:** Docker, Docker Compose
- **Build:** Gradle
- **Migrations:** Liquibase
- **Mapping:** MapStruct
- **API documentation:** OpenAPI (SpringDoc + Swagger UI)
- **Testing:** JUnit

---

# Предварительные требования

Перед запуском проекта убедитесь, что установлены:

- Docker
- Docker Compose
- JDK 24
- Git
---

## Быстрый старт

```bash
# Быстрый запуск полного окружения разработки (Postgres, Kafka, приложение)
task up

# Полный сброс окружения — удаление volumes, пересборка образа и запуск
task reset

# Просмотр логов всех контейнеров Docker Compose в реальном времени
task docker-compose-logs

# Показать список запущенных Docker контейнеров
task status

```

Требуется: Docker, Docker Compose, JDK 24, Task (go-task).

---

## Ручной запуск (без Makefile)

```bash
# Запуск всех сервисов напрямую
docker compose -f docker-compose.yml --env-file .env up -d

# Остановка
docker compose -f docker-compose.yml --env-file .env down
```

## Docker команды

```bash
# Сборка Docker образа backend-service
task backend-service-build

# Запуск контейнера backend-service
task backend-service-run

# Остановка контейнера backend-service
task backend-service-stop

# Удаление контейнера backend-service
task backend-service-remove

# Полное удаление контейнера и образа backend-service
task backend-service-clean
```

## Gradle команды
```bash
# Запуск Spring Boot приложения локально
task run

# Сборка проекта и создание jar файла
task build

# Запуск всех тестов
task test

# Очистка папки build
task clean
```

## Порты и сервисы

| Сервис            | Порт (host) | Описание                                     |
|-------------------|-------------|----------------------------------------------|
| Postgres          | 5432        | База данных для пользователей и задач        |
| Zookeeper         | 2181        | Координация для Kafka                        |   
| Kafka             | 9092        | Брокер Kafka                                 | 
| backend-service   | 8081        | REST API Spring Boot приложения              |


---
# Debugging / Диагностика
Проверить контейнеры:
```bash
docker compose ps
```
Просмотреть логи:
```bash
task docker-compose-logs
```
Список контейнеров:
```bash
docker ps
```
Проверить занятые порты (Windows):
```bash
netstat -ano | findstr :8081
netstat -ano | findstr :5432
netstat -ano | findstr :9092
```
---
# Типовые ошибки
**Порты заняты**
Убедитесь что `8081`, `5432`, `2181`, `9092` не используются.
---
**Backend-service не стартует**
Проверьте логи:
```bash
task docker-compose-logs
```
---

**Нет подключения к Postgres**
Проверьте `.env` и состояние контейнера postgres.
---
**Kafka не запускается**
Очистите volumes:
```bash
task docker-compose-clean
```
---

# Конфигурация
Все настройки выполняются через переменные окружения.
Скопируйте файл:
```bash
cp .env.example .env
```
### Основные переменные
**Server**
```
SERVER_PORT
```
**PostgreSQL**
```
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
```
**Spring Datasource**
```
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```
**Kafka**
```
SPRING_KAFKA_BOOTSTRAP_SERVERS
```
**JWT**
```
JWT_SECRET
```
---

# API документация
Swagger UI доступен по адресу:
```
http://localhost:8081/swagger-ui.html
```
OpenAPI спецификация:
```
http://localhost:8081/v3/api-docs
```
---

# Postman коллекция

Для удобного тестирования API доступна Postman коллекция.

<a href="https://mash77-4007.postman.co/workspace/task-tracker~a5009b8f-ef6f-4bee-9a64-ee045b45e135/collection/41844941-05476f88-28e2-4c97-8619-acaeee2ea940?action=share&source=copy-link&creator=41844941">
  <img src="./button.svg" alt="Run in Postman" width="150"/>
</a>

После запуска проекта:
```bash
task docker-compose-up
```

API будет доступен:
```
http://localhost:8081
```

## Импорт API в Postman через OpenAPI
1. Запустите проект
```bash
task docker-compose-up
```
2. Откройте Postman
3. Нажмите **Import → Link**
4. Вставьте:
```
http://localhost:8081/v3/api-docs
```
Postman автоматически создаст коллекцию со всеми endpoints.
---

## Структура каталогов
```
backend-service/
├── openapi/                      # OpenAPI спецификации
├── src/
│   ├── main/
│   │   ├── java/org/mushroom/
│   │   │   ├── config/           # Конфигурации Spring
│   │   │   ├── controller/       # REST контроллеры
│   │   │   ├── entity/           # JPA сущности
│   │   │   ├── enums/            # enums
│   │   │   ├── exception/        # Кастомные исключения
│   │   │   ├── kafka/            # Kafka продюсеры/консьюмеры
│   │   │   ├── mapper/           # MapStruct мапперы
│   │   │   ├── reports/          # Отчёты / аналитика
│   │   │   ├── repository/       # Spring Data репозитории
│   │   │   ├── service/          # Бизнес-логика
│   │   │   └── util/             # Утилиты
│   │   └── resources/
│   │       └── db.changelog/     # Liquibase миграции
│   │           └── scripts/
│   └── test/
│       ├── java/org/mushroom/
│       │   ├── config/
│       │   ├── controller/
│       │   ├── repository/
│       │   └── service/
│       └── resources/
├── build.gradle
└── settings.gradle
```
