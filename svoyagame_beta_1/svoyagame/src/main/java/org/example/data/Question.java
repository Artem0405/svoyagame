// В файле: Question.java
package org.example.data;

import java.util.Objects; // Для equals/hashCode

/**
 * Представляет один вопрос в игре "Своя Игра".
 * Содержит текст вопроса, текст ответа, количество баллов (очков),
 * тип контента (текст, изображение, аудио), путь к файлу для медиа-контента,
 * и специальный тип вопроса (обычный, кот в мешке, аукцион).
 */
public class Question {

    /**
     * Тип контента вопроса.
     */
    public enum QuestionType {
        TEXT,  // Обычный текстовый вопрос
        IMAGE, // Вопрос с изображением
        AUDIO  // Вопрос с аудио
    }

    /**
     * Специальный тип вопроса.
     */
    public enum SpecialType {
        NONE,         // Обычный вопрос
        CAT_IN_A_BAG, // Кот в мешке
        AUCTION       // Аукцион
    }

    // --- Поля класса ---
    private String questionText; // Текст вопроса
    private String answerText;   // Текст ответа
    private int points;          // Номинальная стоимость вопроса
    private QuestionType type;   // Тип контента вопроса (TEXT, IMAGE, AUDIO)
    private String filePath;     // Путь к файлу для IMAGE или AUDIO, null для TEXT
    private SpecialType specialType; // Специальный тип вопроса (NONE, CAT_IN_A_BAG, AUCTION)

    /**
     * Пустой конструктор. Необходим для библиотек вроде Gson.
     * Инициализирует поля значениями по умолчанию.
     */
    public Question() {
        // Устанавливаем безопасные значения по умолчанию
        this.questionText = "";
        this.answerText = "";
        this.points = 100; // Или 0, если предпочтительнее
        this.type = QuestionType.TEXT;
        this.filePath = null;
        this.specialType = SpecialType.NONE;
    }

    /**
     * Основной конструктор для создания вопроса.
     *
     * @param questionText Текст вопроса (не должен быть пустым).
     * @param answerText   Текст ответа (не должен быть пустым).
     * @param points       Номинальная стоимость вопроса (должна быть > 0).
     * @param type         Тип контента (TEXT, IMAGE, AUDIO).
     * @param filePath     Путь к файлу для IMAGE/AUDIO (будет проигнорирован и установлен в null для TEXT).
     * @param specialType  Специальный тип вопроса (NONE, CAT_IN_A_BAG, AUCTION).
     */
    public Question(String questionText, String answerText, int points,
                    QuestionType type, String filePath, SpecialType specialType) {
        // Устанавливаем базовые поля через сеттеры для возможной валидации
        setQuestionText(questionText);
        setAnswerText(answerText);
        setPoints(points);

        // Сначала устанавливаем тип, используя сеттер для обработки null
        // Если передан null, по умолчанию ставится TEXT
        setType(type != null ? type : QuestionType.TEXT);

        // *** ИСПРАВЛЕНИЕ: Явно вызываем setFilePath ПОСЛЕ установки типа ***
        // Передаем путь, который был передан в конструктор.
        // Сеттер setFilePath сам проверит текущий тип (установленный вызовом setType выше)
        // и установит путь или null соответственно.
        setFilePath(filePath);
        // *** КОНЕЦ ИСПРАВЛЕНИЯ ***

        // Устанавливаем специальный тип, используя сеттер для обработки null
        // Если передан null, по умолчанию ставится NONE
        setSpecialType(specialType != null ? specialType : SpecialType.NONE);
    }

    /**
     * Копирующий конструктор. Создает новый объект Question
     * как копию существующего. Используется для создания копий тем.
     * @param original Оригинальный объект Question для копирования. Не должен быть null.
     * @throws NullPointerException если original равен null
     */
    public Question(Question original) {
        // Убедимся, что оригинал не null, чтобы избежать неожиданного поведения
        Objects.requireNonNull(original, "Cannot copy a null Question");

        this.questionText = original.questionText;
        this.answerText = original.answerText;
        this.points = original.points;
        this.type = original.type;
        // Копируем путь к файлу. Так как String неизменяемый, простая ссылка безопасна.
        this.filePath = original.filePath;
        this.specialType = original.specialType;
    }


    // --- Геттеры ---

    public String getQuestionText() {
        return questionText;
    }

    public String getAnswerText() {
        return answerText;
    }

    public int getPoints() {
        return points;
    }

    public QuestionType getType() {
        return type;
    }

    /**
     * Возвращает путь к файлу, ТОЛЬКО если тип вопроса IMAGE или AUDIO.
     * Возвращает null, если тип вопроса TEXT или путь не был установлен.
     *
     * @return Путь к файлу или null.
     */
    public String getFilePath() {
        // Возвращаем путь только для соответствующих типов
        if (this.type == QuestionType.IMAGE || this.type == QuestionType.AUDIO) {
            return filePath;
        }
        return null; // Для TEXT или если filePath не установлен для медиа-типа
    }

    public SpecialType getSpecialType() {
        return specialType;
    }

    // --- Сеттеры ---

    /**
     * Устанавливает текст вопроса.
     * @param questionText Текст вопроса. Если null, устанавливается пустая строка.
     */
    public void setQuestionText(String questionText) {
        this.questionText = (questionText != null) ? questionText : "";
    }

    /**
     * Устанавливает текст ответа.
     * @param answerText Текст ответа. Если null, устанавливается пустая строка.
     */
    public void setAnswerText(String answerText) {
        this.answerText = (answerText != null) ? answerText : "";
    }

    /**
     * Устанавливает количество баллов (очков) за вопрос.
     * Если передано отрицательное значение, устанавливается 0 и выводится предупреждение.
     * @param points Количество баллов (должно быть >= 0).
     */
    public void setPoints(int points) {
        if (points < 0) {
            System.err.println("Warning: Question points set to negative value: "
                    + points + ". Setting to 0.");
            this.points = 0;
        } else {
            this.points = points;
        }
    }

    /**
     * Устанавливает тип контента вопроса (TEXT, IMAGE, AUDIO).
     * Если тип меняется на TEXT, путь к файлу (`filePath`) автоматически сбрасывается в null.
     * Если передан null, устанавливается тип TEXT по умолчанию.
     * @param type Новый тип контента.
     */
    public void setType(QuestionType type) {
        // Устанавливаем тип, с TEXT по умолчанию при null
        this.type = (type != null) ? type : QuestionType.TEXT;

        // Если новый тип - TEXT, обнуляем путь к файлу
        if (this.type == QuestionType.TEXT) {
            this.filePath = null;
        }
        // ВАЖНО: Этот метод НЕ устанавливает filePath для IMAGE/AUDIO,
        // это должен делать вызов setFilePath() отдельно!
    }

    /**
     * Устанавливает путь к файлу для вопросов типа IMAGE или AUDIO.
     * Если текущий тип вопроса TEXT, путь будет проигнорирован (останется null).
     * @param filePath Путь к файлу. Может быть null.
     */
    public void setFilePath(String filePath) {
        // Устанавливаем путь, только если тип вопроса это поддерживает (IMAGE или AUDIO)
        if (this.type == QuestionType.IMAGE || this.type == QuestionType.AUDIO) {
            // Здесь можно добавить валидацию самого пути, если необходимо
            // (например, проверка на пустоту, хотя null допускается)
            this.filePath = filePath;
        } else {
            // Если тип вопроса TEXT, убеждаемся, что путь равен null
            this.filePath = null;
        }
    }

    /**
     * Устанавливает специальный тип вопроса (NONE, CAT_IN_A_BAG, AUCTION).
     * Если передан null, устанавливается тип NONE по умолчанию.
     * @param specialType Новый специальный тип.
     */
    public void setSpecialType(SpecialType specialType) {
        this.specialType = (specialType != null) ? specialType : SpecialType.NONE;
    }

    // --- Вспомогательные методы ---

    /**
     * Проверяет, является ли вопрос специальным (т.е. не типа NONE).
     * @return true, если specialType не NONE, иначе false.
     */
    public boolean isSpecial() {
        return this.specialType != SpecialType.NONE;
    }

    // --- Стандартные методы ---

    @Override
    public String toString() {
        // Улучшенный toString для лучшей читаемости и отладки
        StringBuilder sb = new StringBuilder("Question{");
        sb.append("points=").append(points);
        sb.append(", type=").append(type);
        // Включаем filePath только если он не null (и имеет смысл для типа)
        if (filePath != null && (type == QuestionType.IMAGE || type == QuestionType.AUDIO)) {
            sb.append(", filePath='").append(filePath).append('\'');
        }
        sb.append(", specialType=").append(specialType);
        // Ограничиваем длину текста для краткости вывода
        String qTextShort = (questionText != null && questionText.length() > 30) ? questionText.substring(0, 27) + "..." : questionText;
        String aTextShort = (answerText != null && answerText.length() > 30) ? answerText.substring(0, 27) + "..." : answerText;
        sb.append(", questionText='").append(qTextShort).append('\'');
        sb.append(", answerText='").append(aTextShort).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Question question = (Question) o;
        // Сравниваем по основным полям, определяющим уникальность вопроса
        // (можно скорректировать набор полей при необходимости)
        return points == question.points &&
                type == question.type &&
                specialType == question.specialType &&
                Objects.equals(questionText, question.questionText) &&
                Objects.equals(answerText, question.answerText) &&
                Objects.equals(filePath, question.filePath); // Важно сравнивать и filePath
    }

    @Override
    public int hashCode() {
        // Генерируем хеш-код на основе тех же полей, что и в equals
        return Objects.hash(questionText, answerText, points, type, filePath, specialType);
    }
}