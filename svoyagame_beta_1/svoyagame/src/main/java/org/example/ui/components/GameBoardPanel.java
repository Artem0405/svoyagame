// Файл: GameBoardPanel.java
package org.example.ui.components;

import org.example.data.Question;
import org.example.data.Theme;
import org.example.game.GameController;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*; // Для Map, Set, LinkedHashSet, Comparator, ArrayList, Point

/**
 * Панель, отображающая игровую доску ("табло") с темами и вопросами.
 * Используется как для ведущего (с активными кнопками для выбора вопроса),
 * так и для игрока (с неактивными кнопками, показывающими только очки).
 */
public class GameBoardPanel extends JPanel {

    private final GameController gameController;
    private final boolean isHostBoard; // True для доски ведущего, false для игроков
    // Хранит кнопки для доступа по координатам (Point(themeIndex, questionIndexInTheme))
    private final Map<Point, JButton> buttonMap;

    // --- КОНСТАНТЫ UI (Рекомендуется вынести в AppConstants.java) ---
    private static final Color BOARD_BACKGROUND = new Color(0, 0, 100); // Темно-синий
    private static final Color CELL_BORDER_COLOR = Color.LIGHT_GRAY;
    private static final Color HEADER_TEXT_COLOR = Color.WHITE;
    private static final Color BUTTON_TEXT_COLOR = Color.YELLOW; // Желтый для очков (активные/неотвеченные)
    private static final Color BUTTON_DISABLED_TEXT_COLOR = Color.GRAY; // Серый для отвеченных/неактивных
    // Шрифты
    private static final Font HEADER_POINTS_FONT = new Font("Arial", Font.BOLD, 16); // Шрифт для заголовков очков
    private static final Font THEME_NAME_FONT = new Font("Arial", Font.BOLD, 14);    // УМЕНЬШЕННЫЙ шрифт для заголовков тем
    private static final Font QUESTION_BUTTON_FONT = new Font("Arial", Font.BOLD, 20); // Шрифт для кнопок с очками
    // Максимальная длина отображаемого имени темы перед усечением
    private static final int MAX_THEME_NAME_DISPLAY_LENGTH = 22; // Подберите это значение!
    // --- КОНЕЦ КОНСТАНТ ---

    /**
     * Конструктор панели игровой доски.
     * @param controller GameController для взаимодействия.
     * @param isHost true, если это доска ведущего (кнопки могут быть активны), false - для игрока (кнопки всегда неактивны).
     * @throws IllegalArgumentException если controller равен null.
     */
    public GameBoardPanel(GameController controller, boolean isHost) {
        if (controller == null) {
            throw new IllegalArgumentException("GameController cannot be null for GameBoardPanel");
        }
        this.gameController = controller;
        this.isHostBoard = isHost;
        this.buttonMap = new HashMap<>();
        this.setBackground(BOARD_BACKGROUND);

        // Изначально панель пуста или показывает сообщение
        setLayout(new BorderLayout());
        displayInitialMessage("Ожидание начала раунда..."); // Показываем начальное сообщение
    }

    /**
     * Полностью перестраивает игровую доску на основе тем для текущего раунда.
     * Вызывается из GameController при старте нового раунда.
     * @param themesForRound Список тем для отображения в этом раунде (ожидается копия из GameController).
     */
    public void rebuildBoard(List<Theme> themesForRound) {
        System.out.println("GameBoardPanel.rebuildBoard called. isHost=" + isHostBoard);
        // Очищаем предыдущее состояние панели и карты кнопок
        this.removeAll();
        this.buttonMap.clear();

        // Проверка на пустой или некорректный список тем
        if (themesForRound == null || themesForRound.isEmpty()) {
            System.err.println("GameBoardPanel.rebuildBoard: Received null or empty themes list.");
            displayErrorMessage("Нет тем для отображения в этом раунде.");
            return;
        }

        // 1. Определяем уникальные значения очков и сортируем их
        // Используем LinkedHashSet для сохранения порядка добавления, если понадобится
        Set<Integer> pointValuesSet = new LinkedHashSet<>();
        boolean hasAnyQuestions = false;
        for (Theme theme : themesForRound) {
            // Пропускаем null темы
            if (theme != null && theme.getQuestions() != null) {
                for (Question q : theme.getQuestions()) {
                    // Учитываем только вопросы с положительными очками
                    if (q != null && q.getPoints() > 0) {
                        pointValuesSet.add(q.getPoints());
                        hasAnyQuestions = true;
                    }
                }
            }
        }

        // Если нет вопросов с очками, показываем сообщение
        if (!hasAnyQuestions || pointValuesSet.isEmpty()) {
            System.err.println("GameBoardPanel.rebuildBoard: No questions with positive points found.");
            displayErrorMessage("В этом раунде нет вопросов с очками.");
            return;
        }

        // Сортируем очки по возрастанию
        List<Integer> pointValues = new ArrayList<>(pointValuesSet);
        pointValues.sort(Comparator.naturalOrder());

        // 2. Настраиваем GridLayout
        int numThemes = themesForRound.size();
        int numPoints = pointValues.size();
        int rows = numThemes + 1; // +1 для заголовков очков
        int cols = numPoints + 1; // +1 для заголовков тем
        setLayout(new GridLayout(rows, cols, 3, 3)); // Промежутки между ячейками
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Внешний отступ панели

        System.out.println("  Rebuilding board with " + numThemes + " themes and " + numPoints + " point values.");

        // 3. Заполняем сетку компонентами
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // Создаем панель для каждой ячейки
                JPanel cellPanel = createCellPanel(); // Используем вспомогательный метод

                final int themeIndex = r - 1;       // Индекс темы (0-based)
                final int pointValueIndex = c - 1;  // Индекс значения очков (0-based)

                // Определяем тип ячейки и заполняем ее
                if (r == 0 && c == 0) {
                    // Верхний левый угол - пустой
                    // cellPanel остается пустым (или добавляем Box.createGlue())
                } else if (r == 0) {
                    // Строка заголовков очков
                    addPointsHeader(cellPanel, pointValues, pointValueIndex);
                } else if (c == 0) {
                    // Столбец заголовков тем
                    addThemeHeader(cellPanel, themesForRound, themeIndex);
                } else {
                    // Ячейки с кнопками вопросов
                    addQuestionButton(cellPanel, themesForRound, pointValues, themeIndex, pointValueIndex);
                }
                // Добавляем готовую ячейку в сетку
                this.add(cellPanel);
            } // конец цикла по столбцам
        } // конец цикла по строкам

        // Перерисовываем панель после добавления всех компонентов
        this.revalidate();
        this.repaint();
        System.out.println("  Board rebuild finished.");
    }

    /** Создает стандартную панель для ячейки сетки. */
    private JPanel createCellPanel() {
        JPanel cellPanel = new JPanel(new BorderLayout());
        cellPanel.setBackground(BOARD_BACKGROUND);
        cellPanel.setBorder(BorderFactory.createLineBorder(CELL_BORDER_COLOR, 1));
        return cellPanel;
    }

    /** Добавляет заголовок очков в ячейку. */
    private void addPointsHeader(JPanel cellPanel, List<Integer> pointValues, int pointValueIndex) {
        if (pointValueIndex >= 0 && pointValueIndex < pointValues.size()) {
            JLabel pointsLabel = new JLabel(String.valueOf(pointValues.get(pointValueIndex)), SwingConstants.CENTER);
            pointsLabel.setFont(HEADER_POINTS_FONT);
            pointsLabel.setForeground(HEADER_TEXT_COLOR);
            cellPanel.add(pointsLabel, BorderLayout.CENTER);
        }
    }

    /** Добавляет заголовок темы в ячейку с усечением и подсказкой. */
    private void addThemeHeader(JPanel cellPanel, List<Theme> themes, int themeIndex) {
        if (themeIndex >= 0 && themeIndex < themes.size()) {
            Theme currentTheme = themes.get(themeIndex);
            String themeName = "[Без имени]"; // Значение по умолчанию
            if (currentTheme != null && currentTheme.getThemeName() != null && !currentTheme.getThemeName().trim().isEmpty()) {
                themeName = currentTheme.getThemeName().trim();
            }

            JLabel themeLabel = new JLabel();
            themeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            themeLabel.setFont(THEME_NAME_FONT); // Уменьшенный шрифт
            themeLabel.setForeground(HEADER_TEXT_COLOR);

            // Усечение текста и ToolTip
            String displayText = themeName;
            if (themeName.length() > MAX_THEME_NAME_DISPLAY_LENGTH) {
                displayText = themeName.substring(0, MAX_THEME_NAME_DISPLAY_LENGTH - 3) + "...";
            }
            // Используем HTML для центрирования и возможного переноса (хотя усечение важнее)
            themeLabel.setText("<html><div style='text-align: center; padding: 2px;'>" + displayText + "</div></html>");
            themeLabel.setToolTipText(themeName); // Полный текст в подсказке

            cellPanel.add(themeLabel, BorderLayout.CENTER);
        }
    }

    /** Добавляет кнопку вопроса (или пустое место) в ячейку. */
    private void addQuestionButton(JPanel cellPanel, List<Theme> themes, List<Integer> pointValues, int themeIndex, int pointValueIndex) {
        if (themeIndex < 0 || themeIndex >= themes.size() ||
                pointValueIndex < 0 || pointValueIndex >= pointValues.size()) {
            return; // Выход, если индексы некорректны
        }

        int targetPoints = pointValues.get(pointValueIndex);
        Theme currentTheme = themes.get(themeIndex);
        Question targetQuestion = findQuestionByPoints(currentTheme, targetPoints);

        if (targetQuestion != null) {
            // Нашли вопрос
            JButton button = new JButton(String.valueOf(targetPoints));
            button.setFont(QUESTION_BUTTON_FONT);
            button.setForeground(BUTTON_TEXT_COLOR); // Изначально желтый
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusable(false);
            button.setEnabled(false); // Кнопки изначально неактивны

            // Находим реальный индекс вопроса в списке темы
            int questionIndexInTheme = findQuestionIndex(currentTheme, targetQuestion);
            if (questionIndexInTheme != -1) {
                Point buttonKey = new Point(themeIndex, questionIndexInTheme);
                buttonMap.put(buttonKey, button); // Сохраняем кнопку для управления

                // Добавляем слушателя только для хоста
                if (isHostBoard) {
                    final int finalThemeIndex = themeIndex; // Нужны final для лямбды
                    final int finalQuestionIndex = questionIndexInTheme;
                    button.addActionListener(e -> {
                        if (button.isEnabled()){ // Доп. проверка, что кнопка активна
                            System.out.println("Host board button clicked: Theme=" + finalThemeIndex + ", QIndex=" + finalQuestionIndex);
                            gameController.hostSelectedQuestion(finalThemeIndex, finalQuestionIndex);
                        }
                    });
                }
                cellPanel.add(button, BorderLayout.CENTER);
            } else {
                // Ошибка поиска индекса
                System.err.println("Internal error: Could not find index for question with points " + targetPoints + " in theme '" + currentTheme.getThemeName() + "'");
            }
        }
        // Если вопрос не найден (targetQuestion == null), cellPanel остается пустым
    }

    /** Отображает начальное сообщение или сообщение об ошибке по центру панели. */
    private void displayInitialMessage(String message) {
        this.removeAll(); // Очищаем все предыдущее
        this.setLayout(new BorderLayout());
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setFont(HEADER_POINTS_FONT);
        add(label, BorderLayout.CENTER);
        this.revalidate();
        this.repaint();
    }
    private void displayErrorMessage(String message) {
        this.removeAll();
        this.setLayout(new BorderLayout());
        JLabel errorLabel = new JLabel(message, SwingConstants.CENTER);
        errorLabel.setForeground(Color.RED); // Красный цвет для ошибки
        errorLabel.setFont(HEADER_POINTS_FONT);
        add(errorLabel, BorderLayout.CENTER);
        this.revalidate();
        this.repaint();
    }

    /**
     * Устанавливает состояние активности кнопок на доске в зависимости от паузы и отвеченных вопросов.
     * Обновляет текст и цвет кнопок.
     * @param enabled true, если игра не на паузе.
     * @param answeredQuestionsInRound Массив `boolean` отвеченных вопросов текущего раунда.
     */
    public void setBoardEnabled(boolean enabled, boolean[][] answeredQuestionsInRound) {
        // System.out.println("GameBoardPanel.setBoardEnabled called. enabled=" + enabled + ", isHost=" + isHostBoard);
        if (buttonMap == null || buttonMap.isEmpty()) {
            return; // Нечего обновлять
        }
        if (answeredQuestionsInRound == null) {
            System.err.println("  Warning: answeredQuestionsInRound is null in setBoardEnabled. Cannot update answered state.");
            // При ошибке данных, отключаем все кнопки
            for (JButton button : buttonMap.values()) {
                if (button != null) {
                    SwingUtilities.invokeLater(() -> button.setEnabled(false));
                }
            }
            return;
        }

        for (Map.Entry<Point, JButton> entry : buttonMap.entrySet()) {
            Point key = entry.getKey(); // key.x = themeIndex, key.y = questionIndexInTheme
            JButton button = entry.getValue();
            if (button == null) continue;

            boolean isAnswered = false;
            String originalPointsText = ""; // Пытаемся сохранить очки для неотвеченных

            try {
                // Проверка границ и получение статуса ответа
                if (key.x >= 0 && key.x < answeredQuestionsInRound.length &&
                        answeredQuestionsInRound[key.x] != null &&
                        key.y >= 0 && key.y < answeredQuestionsInRound[key.x].length)
                {
                    isAnswered = answeredQuestionsInRound[key.x][key.y];
                    // Сохраняем текст кнопки (очки), если она еще не очищена
                    if (!button.getText().isEmpty()) {
                        originalPointsText = button.getText();
                    }
                } else {
                    System.err.println("  Warning: Invalid coordinates or structure for key " + key + " in answeredQuestionsInRound.");
                    isAnswered = true; // Считаем отвеченным при ошибке структуры
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("  Error accessing answeredQuestionsInRound for key " + key + ": " + e.getMessage());
                isAnswered = true; // Считаем отвеченным при ошибке
            }

            // Определяем финальные значения для UI
            final boolean finalIsAnswered = isAnswered;
            final String textToShow = !finalIsAnswered ? (originalPointsText.isEmpty() ? "?" : originalPointsText) : ""; // Очки или пусто
            final Color textColor = !finalIsAnswered ? BUTTON_TEXT_COLOR : BUTTON_DISABLED_TEXT_COLOR; // Желтый или серый
            // Активна только у хоста, если игра не на паузе и вопрос не отвечен
            final boolean buttonShouldBeEnabled = isHostBoard && enabled && !finalIsAnswered;

            // Обновляем UI в EDT
            SwingUtilities.invokeLater(() -> {
                button.setText(textToShow);
                button.setForeground(textColor);
                // Устанавливаем состояние enabled только если оно изменилось,
                // чтобы избежать лишних событий и мерцания
                if (button.isEnabled() != buttonShouldBeEnabled) {
                    button.setEnabled(buttonShouldBeEnabled);
                }
            });
        }
        // System.out.println("  Board enable state updated for all buttons.");
    }

    /**
     * Отключает ОДНУ конкретную кнопку на доске после ответа на вопрос.
     * Устанавливает серый цвет и убирает текст (очки).
     * @param themeIndexInRound Индекс темы в текущем раунде.
     * @param questionIndexInTheme Индекс вопроса внутри списка вопросов темы.
     */
    public void disableButton(int themeIndexInRound, int questionIndexInTheme) {
        Point buttonKey = new Point(themeIndexInRound, questionIndexInTheme);
        JButton button = buttonMap.get(buttonKey);
        if (button != null) {
            // Обновляем в EDT
            SwingUtilities.invokeLater(() -> {
                button.setEnabled(false); // Всегда отключаем
                button.setText("");       // Убираем очки
                button.setForeground(BUTTON_DISABLED_TEXT_COLOR); // Ставим серый цвет
                System.out.println("  Button [" + themeIndexInRound + "," + questionIndexInTheme + "] disabled and cleared.");
            });
        } else {
            System.err.println("  disableButton: Button not found for key [" + themeIndexInRound + "," + questionIndexInTheme + "]");
        }
    }

    /** Обновляет состояние доски на основе данных из GameController (для синхронизации). */
    public void refreshBoard() {
        System.out.println("GameBoardPanel.refreshBoard called (isHost=" + isHostBoard + ")");
        if (gameController == null) {
            System.err.println("GameBoardPanel.refreshBoard: GameController is null!");
            return;
        }
        // Получаем актуальное состояние паузы и отвеченных вопросов
        boolean isPaused = gameController.isPaused();
        boolean[][] answered = gameController.getAnsweredQuestionsForCurrentRound();
        // Применяем это состояние ко всем кнопкам
        setBoardEnabled(!isPaused, answered); // enabled = !isPaused
        System.out.println("  Board refreshed based on GameController state (isPaused=" + isPaused + ")");
    }

    // --- Вспомогательные методы ---

    /** Находит первый вопрос в теме с указанным количеством очков. */
    private Question findQuestionByPoints(Theme theme, int points) {
        if (theme == null || theme.getQuestions() == null) {
            return null;
        }
        // Используем Stream API для поиска (более компактно)
        return theme.getQuestions().stream()
                .filter(q -> q != null && q.getPoints() == points)
                .findFirst() // Находим первый подходящий
                .orElse(null); // Возвращаем null, если не найден
    }

    /** Находит индекс заданного вопроса внутри списка вопросов темы. */
    private int findQuestionIndex(Theme theme, Question questionToFind) {
        if (theme == null || theme.getQuestions() == null || questionToFind == null) {
            return -1;
        }
        // Используем indexOf, предполагая, что equals у Question реализован корректно
        return theme.getQuestions().indexOf(questionToFind);
    }

} // Конец класса GameBoardPanel