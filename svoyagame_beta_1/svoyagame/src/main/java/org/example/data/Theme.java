// В файле: Theme.java
package org.example.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects; // Импорт для возможной проверки на null в будущем

/**
 * Представляет одну тему в пакете вопросов "Своя Игра".
 * Содержит название темы и список вопросов {@link Question}, относящихся к ней.
 */
public class Theme {

    private String themeName; // Название темы

    // Инициализируем список вопросов сразу при создании объекта,
    // чтобы избежать NullPointerException при доступе к нему.
    private List<Question> questions = new ArrayList<>();

    // --- Конструкторы ---

    /**
     * Создает новую тему с указанным названием.
     * Список вопросов инициализируется как пустой.
     * @param themeName Название темы.
     */
    public Theme(String themeName) {
        this.themeName = themeName;
        // questions уже инициализирован пустым списком при объявлении поля
    }

    /**
     * Пустой конструктор. Необходим для библиотек сериализации/десериализации, таких как Gson.
     * Имя темы будет null, список вопросов будет пустым.
     */
    public Theme() {
        // Имя темы остается null
        // questions уже инициализирован пустым списком при объявлении поля
    }

    // --- Геттеры и Сеттеры ---

    /**
     * Возвращает название темы.
     * @return Название темы.
     */
    public String getThemeName() {
        return themeName;
    }

    /**
     * Устанавливает название темы.
     * @param themeName Новое название темы.
     */
    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

    /**
     * Возвращает список вопросов в этой теме.
     * ВНИМАНИЕ: Возвращает ссылку на внутренний список. Изменения этого списка
     * извне повлияют на объект Theme. Для большей безопасности можно возвращать копию
     * (например, return new ArrayList<>(questions);) или неизменяемый список
     * (return Collections.unmodifiableList(questions);).
     * @return Список объектов {@link Question}.
     */
    public List<Question> getQuestions() {
        // Возвращаем прямую ссылку для простоты модификации (например, в редакторе)
        // но инициализируем, если вдруг оказался null (хотя не должен)
        if (this.questions == null) {
            this.questions = new ArrayList<>();
        }
        return questions;
    }

    /**
     * Устанавливает список вопросов для этой темы.
     * Предыдущий список вопросов будет заменен.
     * Если передан null, будет установлен пустой список.
     * @param questions Новый список вопросов.
     */
    public void setQuestions(List<Question> questions) {
        // Создаем копию переданного списка для внутренней безопасности
        this.questions = (questions != null) ? new ArrayList<>(questions) : new ArrayList<>();
    }

    // --- Удобные методы ---

    /**
     * Добавляет вопрос в список вопросов этой темы.
     * Если переданный вопрос равен null, он не будет добавлен.
     * @param question Объект вопроса для добавления.
     */
    public void addQuestion(Question question) {
        // Гарантируем, что список инициализирован
        if (this.questions == null) {
            this.questions = new ArrayList<>();
        }
        // Добавляем только не-null вопросы
        if (question != null) {
            this.questions.add(question);
        }
    }

    /**
     * Создает глубокую копию объекта Theme.
     * Копирует имя темы и создает новые объекты Question
     * для каждого вопроса в списке, используя копирующий конструктор Question.
     * Это необходимо, чтобы изменения в копии (например, назначение спецтипов)
     * не затрагивали оригинал.
     * @return Новый объект Theme, являющийся копией текущего.
     */
    public Theme cloneTheme() {
        // 1. Создаем новую тему с тем же именем
        Theme clonedTheme = new Theme(this.themeName);

        // 2. Создаем новый список для вопросов
        List<Question> clonedQuestions = new ArrayList<>();

        // 3. Копируем каждый вопрос, создавая новый объект Question
        // Проверяем, что текущий список вопросов не null
        if (this.questions != null) {
            for (Question originalQuestion : this.questions) {
                // Копируем только не-null вопросы
                if (originalQuestion != null) {
                    // Используем КОПИРУЮЩИЙ КОНСТРУКТОР Question(Question original)
                    // Убедитесь, что он существует и корректно реализован в классе Question!
                    clonedQuestions.add(new Question(originalQuestion));
                }
            }
        }

        // 4. Устанавливаем скопированный список вопросов в новую тему
        // Сеттер setQuestions создаст еще одну копию списка внутри clonedTheme
        clonedTheme.setQuestions(clonedQuestions);

        return clonedTheme;
    }

    // --- Стандартные методы ---

    @Override
    public String toString() {
        // Более информативный toString
        return "Theme{" +
                "themeName='" + (themeName != null ? themeName : "null") + '\'' +
                ", questionsCount=" + (questions != null ? questions.size() : 0) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Theme theme = (Theme) o;
        // Темы считаются равными, если у них одинаковое имя И одинаковый список вопросов
        // (сравнение списков по equals сравнивает и порядок, и сами элементы по equals)
        return Objects.equals(themeName, theme.themeName) &&
                Objects.equals(questions, theme.questions);
    }

    @Override
    public int hashCode() {
        // Хеш-код зависит от имени и списка вопросов
        return Objects.hash(themeName, questions);
    }
}