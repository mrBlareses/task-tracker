# TASK TRACKER Microservice Platform

![Java](https://img.shields.io/badge/Java-24-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![Kafka](https://img.shields.io/badge/Kafka-Apache-black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)
![Build](https://img.shields.io/badge/build-gradle-success)

## Содержание

- [О проекте](#о-проекте)
- [Архитектура](#архитектура)
- [Диаграммы C4](#диаграммы-c4)
- [Состав репозитория](#состав-репозитория)
- [Стек технологий](#стек-технологий)
- [Предварительные требования](#предварительные-требования)
- [Быстрый старт](#быстрый-старт)
- [Taskfile команды](#taskfile-команды)
- [Ручной запуск](#ручной-запуск)
- [Docker команды](#docker-команды)
- [Gradle команды](#gradle-команды)
- [Порты и сервисы](#порты-и-сервисы)
- [Debugging / Диагностика](#debugging--диагностика)
- [Конфигурация](#конфигурация)
- [API документация](#api-документация)
- [Postman коллекция](#postman-коллекция)
- [Проверка email flow](#проверка-email-flow)
- [JaCoCo / покрытие тестами](#jacoco--покрытие-тестами)
- [Структура каталогов](#структура-каталогов)

---

## О проекте

**Task Tracker** — это микросервисная платформа для управления задачами.

Система поддерживает:

- регистрацию и аутентификацию пользователей через JWT
- создание, просмотр, обновление и удаление задач
- смену статуса задач
- построение ежедневной аналитики по задачам пользователей
- отправку приветственных email после регистрации
- отправку ежедневных email-отчётов

Проект разделён на несколько сервисов, которые взаимодействуют:
- синхронно через HTTP
- асинхронно через Kafka

---

## Архитектура

Платформа представляет собой многопользовательский планировщик задач, вдохновленный Trello. 
Пользователи могут использовать его как TODO-лист. Архитектура основана на микросервисах с использованием Spring Boot.

Основные компоненты:
- Backend Service: REST API для регистрации, авторизации (JWT), работы с задачами (создание, редактирование, удаление, пометка как выполненной).
- Scheduler Service: Генерация ежедневных отчетов по задачам (Spring Scheduler запускает задачи по cron в полночь).
- Email Sender Service: Отправка email (приветственные письма при регистрации, ежедневные отчеты) через MailHog с использованием Spring Mail.
- Postgres: хранение пользователей и задач.
- Kafka + Zookeeper: брокер сообщений для асинхронной коммуникации между сервисами (публикация/обработка событий и команд).
- MailHog (dev): локальный SMTP сервер + web UI для просмотра писем во время разработки.
---

## Диаграммы C4

> Исходники диаграмм — в `diagrams/*.puml`. Для удобства в README используются рендеры в PNG.

### Компонентная диаграмма

![C4 Component](diagrams/BackendServiceComponentDiagramWithOpenAPI.png)

### Контекстная диаграмма Backend Service

![Backend Service Context](diagrams/TaskTrackerArchitecture.png)

### [Контекстная диаграмма](diagrams/context.puml) 
- Описание взаимодействия системы с внешними пользователями и системами
- Высокоуровневый обзор

> 💡 **Примечание**: Для просмотра диаграмм используйте [PlantUML](https://plantuml.com/) или установите плагин для отображения .puml файлов в вашей IDE.
---

## Состав репозитория

- `backend-service/` — основной REST API сервис
- `cron-service/` — scheduler / orchestration service
- `email-sender-service/` — email ingestion + outbox + SMTP sending
- `diagrams/` — архитектурные диаграммы
- `docker-compose.yml` — инфраструктура проекта
- `.env.example` — пример переменных окружения
- `.gitignore` — правила игнорирования служебных файлов
- `Taskfile.yml` — команды для управления окружением
- `README.md` — документация проекта

---

## Стек технологий

- **Java:** 21
- **Spring Boot:** 3.3.x
- **Spring Security:** JWT authentication
- **Spring Data JPA**
- **Liquibase**
- **PostgreSQL 15**
- **Apache Kafka**
- **Zookeeper**
- **Spring Mail**
- **MailHog**
- **Gradle**
- **Docker / Docker Compose**
- **OpenAPI / Swagger UI**
- **JUnit 5 / Mockito**
- **JaCoCo**
- **MapStruct**
- **Lombok**

---

# Предварительные требования

Перед запуском проекта убедитесь, что установлены:

- Docker
- Docker Compose
- JDK 24
- Git
- Postman
- Task (`go-task`) — для использования `Taskfile.yml`
---

## Быстрый старт

```bash
# Быстрый запуск полного окружения разработки
task up

# Полный сброс окружения — удаление volumes, пересборка образов и запуск
task reset

# Просмотр логов всех контейнеров Docker Compose в реальном времени
task docker-compose-logs

# Показать список контейнеров проекта
task status

```
---

## Taskfile команды

### Быстрые команды

```bash
task up
task up-fast
task down
task reset
task rebuild
```
Описание:

task up — поднять всё окружение с пересборкой
task up-fast — поднять всё окружение без пересборки
task down — остановить окружение
task reset — полный сброс: down -v + up --build
task rebuild — пересобрать все docker-образы

 - Сборка и тесты

```bash
   task build
   task test
   task clean
```
 - Backend service

```bash
task backend-service-run
task backend-service-test
task backend-service-build
task backend-service-clean
```
 - Cron service

```bash
task cron-service-run
task cron-service-test
task cron-service-build
task cron-service-clean
```
 - Email sender service

```bash
task email-service-run
task email-service-test
task email-service-build
task email-service-clean
task email-jacoco
```
 - Сборка Docker-образов по сервисам

```bash
task compose-build
task compose-build-backend
task compose-build-cron
task compose-build-email
```
 - Выборочный запуск окружения

```bash
task up-backend
task up-cron
task up-email
```
 - Логи и диагностика

```bash
task docker-compose-logs
task logs-backend
task logs-cron
task logs-email
task logs-mailhog
task db-shell
task kafka-topics
task mailhog
task ps
task status
```
Требуется: Docker, Docker Compose, JDK 21, Task (go-task).

---

## Ручной запуск

```bash
# Запуск всех сервисов напрямую
docker compose --env-file .env up -d

# Остановка
docker compose --env-file .env down
```

## Docker команды

```bash
# Сборка всех Docker образов проекта
task compose-build

# Сборка Docker образа backend-service
task compose-build-backend

# Сборка Docker образа cron-service
task compose-build-cron

# Сборка Docker образа email-sender-service
task compose-build-email

# Запуск всего окружения через Docker Compose с пересборкой
task docker-compose-up

# Запуск всего окружения без пересборки
task docker-compose-up-no-build

# Остановка и удаление контейнеров Docker Compose
task docker-compose-down

# Полная очистка Docker Compose включая volumes
task docker-compose-clean

# Поднять только backend-service и его зависимости
task up-backend

# Поднять только cron-service и его зависимости
task up-cron

# Поднять только email-sender-service и его зависимости
task up-email

# Просмотр логов всех контейнеров
task docker-compose-logs

# Просмотр логов backend-service
task logs-backend

# Просмотр логов cron-service
task logs-cron

# Просмотр логов email-sender-service
task logs-email

# Просмотр логов mailhog
task logs-mailhog

# Показать контейнеры проекта
task status
```
## Gradle команды

```bash
# Полная сборка всего multi-module проекта
task build

# Запуск всех тестов проекта
task test

# Очистка build директорий
task clean

# Запуск backend-service локально через Gradle
task backend-service-run

# Запуск тестов backend-service
task backend-service-test

# Сборка backend-service jar
task backend-service-build

# Очистка backend-service
task backend-service-clean

# Запуск cron-service локально через Gradle
task cron-service-run

# Запуск тестов cron-service
task cron-service-test

# Сборка cron-service jar
task cron-service-build

# Очистка cron-service
task cron-service-clean

# Запуск email-sender-service локально через Gradle
task email-service-run

# Запуск тестов email-sender-service
task email-service-test

# Сборка email-sender-service jar
task email-service-build

# Очистка email-sender-service
task email-service-clean

# Генерация JaCoCo отчета для email-sender-service
task email-jacoco
```

## Порты и сервисы

| Сервис                | Порт (host) | Описание                              |
|-----------------------|-------------|---------------------------------------|
| Postgres              | 5432        | База данных для пользователей и задач |
| Zookeeper             | 2181        | Координация для Kafka                 |   
| Kafka                 | 9092        | Брокер Kafka                          | 
| backend-service       | 8081        | REST API Spring Boot приложения       |
| cron-service	         | 8082	       | Scheduler / orchestration service     |
| email-sender-service	 | 8083	       | Email ingestion / outbox / SMTP       |
| MailHog SMTP	         | 1025	       | SMTP сервер для dev                   |
| MailHog UI	         | 8025	       | Web UI для просмотра писем            |


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

# Конфигурация
Все настройки выполняются через переменные окружения.
Скопируйте файл:
```bash
cp .env.example .env
```
### Основные переменные
- Представлены в .env.example
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
## Проверка email flow

После регистрации пользователя или запуска daily analytics можно проверить email flow следующим образом.

1. Открыть MailHog UI
   http://localhost:8025
2. Проверить письма

Ожидаемые письма:
Welcome to Task Tracker
Your daily task report

3. Проверить outbox в Postgres
   task db-shell

SQL:
```bash
SELECT id, type, status, dedup_key, attempts, payload
FROM outbox_emails
ORDER BY id DESC;
```
 - Ожидаемые типы:
WELCOME
REPORT

 - Ожидаемые статусы:
NEW
PROCESSING
SENT
FAILED
Защита от дублей

Для email-sender-service реализована дедупликация через уникальный индекс:
(dedup_key, type)
Вставка новых записей выполняется через:
INSERT ... ON CONFLICT DO NOTHING
Это позволяет безопасно игнорировать повторно полученные Kafka-события без выброса SQL ошибок.

## JaCoCo / покрытие тестами

Для email-sender-service используется JaCoCo.

Генерация отчёта
task email-jacoco

или напрямую:
```bash
./gradlew :email-sender-service:test :email-sender-service:jacocoTestReport
```
HTML отчёт
```
email-sender-service/build/reports/jacoco/test/html/index.html
```

Через отчёт удобно смотреть:

общий процент покрытия
покрытие по пакетам
непокрытые ветки
непокрытые методы

## Структура каталогов
```
.
├── backend-service/
│   ├── build.gradle
│   ├── openapi/
│   │   ├── service-api.yml
│   │   ├── task-api.yml
│   │   └── user-api.yml
│   └── src/
├── cron-service/
│   ├── build.gradle
│   └── src/
├── email-sender-service/
│   ├── build.gradle
│   └── src/
├── diagrams/
│   ├── BackendServiceComponentDiagramWithOpenAPI.png
│   ├── component.puml
│   ├── context.puml
│   └── TaskTrackerArchitecture.png
├── gradle/
│   └── wrapper/
├── .dockerignore
├── .env.example
├── .gitignore
├── button.svg
├── docker-compose.yml
├── gradlew
├── gradlew.bat
├── settings.gradle
├── Taskfile.yml
└── README.md
```
