# Содержание
1. [Введение](#1-введение)
2. [Требования пользователя](#2-требования-пользователя)  
   2.1 [Программные интерфейсы](#21-программные-интерфейсы)  
   2.2 [Интерфейс пользователя](#22-интерфейс-пользователя)  
   2.3 [Характеристики пользователей](#23-характеристики-пользователей)  
   2.4 [Предположения и зависимости](#24-предположения-и-зависимости)  
3. [Системные требования](#3-системные-требования)  
   3.1 [Функциональные требования](#31-функциональные-требования)  
   3.2 [Нефункциональные требования](#32-нефункциональные-требования)  
      3.2.1 [Атрибуты качества](#321-атрибуты-качества)

## 1. Введение
Aviagerman - это мобильное приложение для бронирования авиабилетов. Продукт предназначен для помощи пользователям в поиске, бронировании и управлении авиабилетами, а также для администрирования рейсов.

Основные функции:
- Поиск доступных рейсов по дате или просмотр всех рейсов
- Бронирование билетов на выбранные рейсы
- Управление существующими бронированиями
- Создание и управление профилем пользователя
- Административные функции для добавления новых рейсов

Продукт не включает в себя функции онлайн-оплаты или интеграции с системами авиакомпаний. Он предназначен для демонстрации возможностей разработки мобильных приложений с использованием современных технологий Android.

## 2. Требования пользователя

### 2.1 Программные интерфейсы

- Android SDK 21+
- Jetpack Compose для создания пользовательского интерфейса
- Room для локального хранения данных
- Kotlin Coroutines для асинхронных операций
- JNI для интеграции нативного кода C++

### 2.2 Интерфейс пользователя

Интерфейс будет реализован в виде мобильного приложения для Android с использованием Material Design 3. Основные экраны:

1. Поиск билетов

   ![Alt-текст](/docs/mockups/)

2. Мои брони

   ![Alt-текст](/docs/mockups/)

3. Авторизация пользователя

   ![Alt-текст](/docs/mockups/)

4. Добавление рейса (для администраторов)

   ![Alt-текст](/docs/mockups/)

5. Подтверждение бронирования

   ![Alt-текст](/docs/mockups/)

### 2.3 Характеристики пользователей

Целевая аудитория - люди, ищущие и бронирующие авиабилеты. Предполагается базовый уровень владения смартфоном и приложениями. Также предусмотрены пользователи с ролью администратора для управления рейсами.

### 2.4 Предположения и зависимости

- Приложение будет работать на устройствах с Android 5.0 (API level 21) и выше
- Для хранения данных будет использоваться локальная база данных Room
- Часть функциональности может зависеть от нативного кода, реализованного на C++

## 3. Системные требования

### 3.1 Функциональные требования

### 3.2 Нефункциональные требования

#### 3.2.1 Атрибуты качества