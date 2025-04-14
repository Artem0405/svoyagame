// Файл: PlayerFrame.java
package org.example.ui;

import org.example.data.Player;
import org.example.data.Question;
import org.example.data.Theme;
import org.example.game.GameController;
import org.example.ui.components.GameBoardPanel;
import org.example.ui.components.ImageDisplayDialog; // Диалог для картинок
import org.example.ui.components.PlayerScorePanel;

// Аудио импорты НЕ НУЖНЫ
// import javax.sound.sampled.*;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections; // Для Collections.emptyList()
import java.util.List;
import java.util.Objects;

// Статический импорт для констант (опционально)
import static org.example.constants.AppConstants.*; // Если вы используете AppConstants

/**
 * Окно игрока в игре "Своя Игра".
 * Отображает игровую доску (неактивную), счет игроков,
 * текст текущего вопроса и изображение (в отдельном окне, если применимо).
 * Не отображает ответ и не воспроизводит звук. Управляется ведущим/контроллером.
 */
public class PlayerFrame extends JFrame {

    // --- Основные компоненты ---
    private final GameController gameController;
    private PlayerScorePanel playerScorePanel;
    private GameBoardPanel gameBoardPanel;

    // --- Компоненты отображения вопроса ---
    private JPanel questionDisplayPanel; // Общая панель внизу
    private JTextArea questionArea;      // Только текст вопроса

    // --- Компоненты управления игрой (минимум) ---
    private JLabel roundInfoLabel;        // Информация о раунде

    // --- Компоненты для паузы и изображений ---
    private JComponent glassPane;         // Панель для отображения паузы
    private ImageDisplayDialog currentImageDialog = null; // Ссылка на активный диалог картинки

    // --- Константы UI ---
    private static final Font PLAYER_QUESTION_FONT = new Font("Arial", Font.PLAIN, 20); // Крупнее шрифт для игрока
    private static final Color PAUSE_OVERLAY_COLOR = new Color(0, 0, 0, 120); // Полупрозрачный черный (чуть темнее)
    private static final Font PAUSE_TEXT_FONT = new Font("Arial", Font.BOLD, 52);
    private static final Color PAUSE_TEXT_COLOR = Color.WHITE;

    /**
     * Конструктор окна игрока.
     * @param controller Ссылка на игровой контроллер.
     * @throws IllegalArgumentException если controller равен null.
     */
    public PlayerFrame(GameController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("GameController cannot be null for PlayerFrame");
        }
        this.gameController = controller;

        setTitle("Своя Игра - Игрок");
        setPreferredSize(new Dimension(1366, 768)); // Размер для Full HD (или другой стандартный)
        // Можно сделать нередактируемым размер: setResizable(false);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); // Игрок не должен закрывать окно
        setLocationRelativeTo(null); // Центрировать (или разместить иначе)

        // Предотвращаем закрытие окна игроком
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("PlayerFrame: Attempted to close window directly (ignored).");
                JOptionPane.showMessageDialog(PlayerFrame.this,
                        "Окно закроется автоматически по завершении игры.",
                        "Закрытие окна", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        initComponents();   // Инициализировать компоненты
        layoutComponents(); // Разместить компоненты
        setupGlassPane();   // Настроить панель паузы

        pack();             // Рассчитать размеры
        System.out.println("PlayerFrame создан и инициализирован.");
    }

    /** Инициализирует компоненты Swing. */
    private void initComponents() {
        playerScorePanel = new PlayerScorePanel();
        // false - доска игрока, кнопки неактивны
        gameBoardPanel = new GameBoardPanel(gameController, false);

        // Панель отображения вопроса (только текст)
        questionDisplayPanel = new JPanel(new BorderLayout(5, 5));
        questionDisplayPanel.setBorder(BorderFactory.createTitledBorder("Вопрос"));

        questionArea = createTextArea(false); // Не редактируемое
        questionArea.setFont(PLAYER_QUESTION_FONT);
        questionArea.setText("Ожидание начала игры...");
        JScrollPane questionScrollPane = new JScrollPane(questionArea);
        questionScrollPane.setBorder(BorderFactory.createEmptyBorder()); // Убираем рамку скролла
        questionDisplayPanel.add(questionScrollPane, BorderLayout.CENTER);

        // Метка информации о раунде
        Font infoFont = HEADER_FONT != null ? HEADER_FONT : new Font("Arial", Font.BOLD, 16); // Пример использования константы
        roundInfoLabel = new JLabel("Раунд: -");
        roundInfoLabel.setFont(infoFont);
        roundInfoLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    }

    /** Вспомогательный метод для создания и настройки JTextArea. */
    private JTextArea createTextArea(boolean editable) {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(editable);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        // Увеличенные отступы для лучшей читаемости у игрока
        textArea.setMargin(new Insets(10, 10, 10, 10));
        return textArea;
    }

    /** Размещает компоненты на фрейме. */
    private void layoutComponents() {
        // Используем JSplitPane для разделения доски и вопроса
        mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setContinuousLayout(true); // Плавное изменение размера
        mainSplitPane.setBorder(null); // Убираем рамку сплиттера

        // Верхняя часть сплиттера: Доска и над ней Счет+Инфо
        JPanel topArea = new JPanel(new BorderLayout(0, 5)); // Панель для счета/инфо и доски
        JPanel scoreAndInfoPanel = new JPanel(new BorderLayout(10, 0)); // Панель для счета и инфо о раунде
        scoreAndInfoPanel.add(playerScorePanel, BorderLayout.CENTER);
        scoreAndInfoPanel.add(roundInfoLabel, BorderLayout.EAST); // Инфо справа
        // Отступы вокруг счета/инфо
        scoreAndInfoPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        topArea.add(scoreAndInfoPanel, BorderLayout.NORTH); // Счет и инфо над доской
        topArea.add(gameBoardPanel, BorderLayout.CENTER);  // Доска под ними

        mainSplitPane.setTopComponent(topArea); // Верхняя часть сплиттера

        // Нижняя часть сплиттера: Панель с текстом вопроса
        questionDisplayPanel.setMinimumSize(new Dimension(0, 150)); // Минимальная высота панели вопроса
        mainSplitPane.setBottomComponent(questionDisplayPanel);

        // Устанавливаем начальное положение разделителя (80% под доску)
        // Вызываем после того, как окно станет видимым или после pack()
        SwingUtilities.invokeLater(() -> {
            if (mainSplitPane.isShowing() && mainSplitPane.getHeight() > 200) {
                mainSplitPane.setDividerLocation(0.8);
            } else {
                // Если окно еще не отрисовано, устанавливаем вес
                mainSplitPane.setResizeWeight(0.8);
            }
        });

        // Добавляем сплиттер в центр основного BorderLayout фрейма
        add(mainSplitPane, BorderLayout.CENTER);
    }

    /** Настраивает Glass Pane для отображения паузы. */
    private void setupGlassPane() {
        glassPane = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); // Работаем с копией Graphics
                // Рисуем полупрозрачный фон
                g2.setColor(PAUSE_OVERLAY_COLOR);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Рисуем текст "ПАУЗА" по центру
                g2.setColor(PAUSE_TEXT_COLOR);
                g2.setFont(PAUSE_TEXT_FONT);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); // Сглаживание
                FontMetrics fm = g2.getFontMetrics();
                String text = "ПАУЗА";
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getAscent();
                int x = (getWidth() - textWidth) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + textHeight;
                g2.drawString(text, x, y);
                g2.dispose(); // Освобождаем ресурсы копии Graphics
            }
        };
        glassPane.setOpaque(false);
        glassPane.setVisible(false);
        // Перехватываем события мыши, чтобы нельзя было кликнуть "сквозь" паузу
        glassPane.addMouseListener(new MouseAdapter() {});
        setGlassPane(glassPane);
    }

    // --- Методы обновления UI, вызываемые из GameController ---

    /** Инициализирует UI начальными данными игроков. */
    public void initializeUI(List<Player> players) {
        if (playerScorePanel != null) {
            SwingUtilities.invokeLater(() -> {
                playerScorePanel.setPlayers(players);
                playerScorePanel.updateScores();
                System.out.println("PlayerFrame UI Initialized with players.");
            });
        } else {
            System.err.println("PlayerFrame.initializeUI: playerScorePanel is null!");
        }
    }

    /** Обновляет информацию о текущем раунде. */
    public void updateRoundInfo(int roundNumber, String roundName) {
        String info = "Раунд: " + roundNumber;
        if (roundName != null && !roundName.isBlank() && !roundName.equalsIgnoreCase("Раунд " + roundNumber)) {
            info += " (\"" + roundName + "\")";
        }
        final String finalInfo = info;
        SwingUtilities.invokeLater(() -> {
            if (roundInfoLabel != null) {
                roundInfoLabel.setText(finalInfo);
            }
        });
    }

    /** Запрашивает обновление отображения счета игроков. */
    public void updatePlayerScores(List<Player> players) {
        if (playerScorePanel != null) {
            SwingUtilities.invokeLater(playerScorePanel::updateScores);
        }
    }

    /**
     * Отображает текст вопроса и показывает диалог с изображением, если тип вопроса IMAGE.
     * Закрывает предыдущий диалог изображения перед показом нового.
     * @param question Объект вопроса для отображения.
     */
    public void displayQuestion(Question question) {
        System.out.println("PlayerFrame.displayQuestion called.");

        SwingUtilities.invokeLater(() -> { // Все изменения UI в EDT
            // *** ВАЖНО: Закрыть ПРЕДЫДУЩИЙ диалог ПЕРЕД отображением нового вопроса ***
            closeCurrentImageDialog(); // Закрыть диалог от предыдущего вопроса, если он был

            // Очищаем только текстовую область
            questionArea.setText("...");
            questionArea.setCaretPosition(0);

            if (question == null) {
                System.err.println("PlayerFrame.displayQuestion: Received null question.");
                // Текст уже "..."
                return;
            }

            // Отображаем текст вопроса
            String themeName = findThemeName(question); // Находим имя темы
            String textToShow = "Тема: " + themeName + " (" +
                    question.getPoints() + ")\n\n" +
                    (question.getQuestionText() != null ? question.getQuestionText() : "[Нет текста вопроса]");
            System.out.println("PlayerFrame: Setting question text: " + textToShow.substring(0, Math.min(50, textToShow.length()))+"..."); // Лог текста
            questionArea.setText(textToShow);
            questionArea.setCaretPosition(0); // Прокрутка вверх ПОСЛЕ установки текста
            System.out.println("PlayerFrame: Text set.");


            Question.QuestionType type = question.getType();
            String filePath = question.getFilePath(); // Путь из объекта Question

            // --- Обработка ИЗОБРАЖЕНИЯ ---
            if (type == Question.QuestionType.IMAGE && filePath != null && !filePath.trim().isEmpty()) {
                System.out.println("  PlayerFrame: Processing IMAGE question, path: '" + filePath + "'");
                try {
                    File imageFile = new File(filePath);
                    if (imageFile.exists() && imageFile.isFile() && imageFile.canRead()) { // Добавили canRead()
                        URL imageUrl = imageFile.toURI().toURL(); // Преобразуем File в URL
                        // Создаем и показываем НОВЫЙ немодальный диалог
                        currentImageDialog = new ImageDisplayDialog(this, // this - родитель PlayerFrame
                                "Вопрос: " + themeName, imageUrl);
                        // Добавляем слушателя для сброса ссылки при закрытии диалога
                        currentImageDialog.addWindowListener(new WindowAdapter() {
                            @Override public void windowClosed(WindowEvent e) {
                                currentImageDialog = null; // Сбросить ссылку
                                System.out.println("PlayerFrame: Image dialog ("+ this.hashCode() +") closed BY USER OR CODE.");
                            }
                        });
                        currentImageDialog.setVisible(true); // Показать диалог
                        System.out.println("  PlayerFrame: Image dialog ("+ currentImageDialog.hashCode() +") shown.");
                    } else {
                        String issue = !imageFile.exists() ? "не найден" : !imageFile.isFile() ? "не файл" : "не читается";
                        System.err.println("  PlayerFrame: Image file " + issue + ": " + filePath);
                        questionArea.append("\n\n[Изображение недоступно ("+ issue +")]");
                    }
                } catch (MalformedURLException mue) {
                    System.err.println("  PlayerFrame: MalformedURLException for image: " + mue.getMessage());
                    questionArea.append("\n\n[Ошибка URL изображения]");
                } catch (Exception ex) { // Ловим любые другие ошибки при работе с файлом/URL/диалогом
                    System.err.println("  PlayerFrame: Error displaying image '" + filePath + "': " + ex.getMessage());
                    ex.printStackTrace();
                    questionArea.append("\n\n[Ошибка загрузки изображения]");
                }
            }
            // --- АУДИО НЕ ОБРАБАТЫВАЕТСЯ У ИГРОКА ---
            else if (type == Question.QuestionType.AUDIO) {
                System.out.println("  PlayerFrame: AUDIO question - displaying text only.");
            }

            // Обновляем панель вопроса
            questionDisplayPanel.revalidate();
            questionDisplayPanel.repaint();
        }); // Конец invokeLater для displayQuestion
    }

    /**
     * Очищает ТОЛЬКО текстовую область вопроса. Диалог изображения НЕ закрывается здесь.
     */
    public void clearQuestionArea() {
        System.out.println("PlayerFrame.clearQuestionArea called.");
        SwingUtilities.invokeLater(() -> { // В EDT
            // НЕ ЗАКРЫВАЕМ ДИАЛОГ КАРТИНКИ ЗДЕСЬ
            // closeCurrentImageDialog(); // <<<--- УБРАНО
            questionArea.setText("..."); // Ожидание следующего вопроса
            questionArea.setCaretPosition(0);
            System.out.println("  PlayerFrame: Text area cleared.");
            // Перерисовка обычно не нужна только для setText
        });
    }

    /**
     * Принудительно закрывает текущий диалог изображения (если он открыт).
     * Вызывается контроллером, например, при паузе или завершении вопроса с картинкой.
     */
    public void forceCloseImageDialog() {
        System.out.println("PlayerFrame.forceCloseImageDialog called.");
        // Закрытие диалога должно происходить в EDT
        SwingUtilities.invokeLater(this::closeCurrentImageDialog);
    }

    /** Внутренний метод для безопасного закрытия диалога изображения. */
    private void closeCurrentImageDialog() {
        if (currentImageDialog != null) {
            System.out.println("  Closing current image dialog ("+ currentImageDialog.hashCode() +").");
            currentImageDialog.dispose(); // Закрыть и освободить ресурсы
            currentImageDialog = null; // Сбросить ссылку
        }
    }

    /** Обновляет состояние паузы (показывает/скрывает glass pane). */
    public void updatePauseState(boolean isPaused) {
        SwingUtilities.invokeLater(() -> { // В EDT
            if (glassPane != null) {
                glassPane.setVisible(isPaused); // Показать или скрыть glass pane
                System.out.println("PlayerFrame pause state updated: " + isPaused);
                // Если ставим на паузу, закрываем диалог картинки
                if (isPaused) {
                    closeCurrentImageDialog();
                }
            }
        });
    }

    // --- Вспомогательные методы ---

    /** Находит имя темы для заданного вопроса. */
    private String findThemeName(Question question) {
        if (question == null || gameController == null) return "[Ошибка]";
        try {
            // Получаем темы текущего раунда из контроллера
            List<Theme> currentThemes = gameController.getThemesForCurrentRound();
            // Используем неизменяемый список, который вернул контроллер
            if (currentThemes.isEmpty()) return "[Нет тем]";

            for (Theme theme : currentThemes) {
                if (theme != null && theme.getQuestions() != null && theme.getQuestions().contains(question)) {
                    String themeName = theme.getThemeName();
                    return (themeName != null && !themeName.trim().isEmpty()) ? themeName : "[Тема без имени]";
                }
            }
        } catch (Exception e) {
            // Ловим возможные ошибки доступа к данным
            System.err.println("Error in PlayerFrame.findThemeName: " + e.getMessage());
            e.printStackTrace(); // Печать стека для диагностики
            return "[Ошибка темы]";
        }
        // Если вопрос не найден ни в одной из тем текущего раунда
        System.err.println("Warning: Question not found in any theme of the current round: " + question);
        return "[Тема?]";
    }

    /** Возвращает панель игровой доски (для GameController). */
    public GameBoardPanel getGameBoardPanel() {
        return gameBoardPanel;
    }

    /** Переопределенный метод для освобождения ресурсов при закрытии окна. */
    @Override
    public void dispose() {
        System.out.println("Disposing PlayerFrame...");
        closeCurrentImageDialog(); // Закрыть диалог картинки, если он еще открыт
        // Звук не закрываем, т.к. его нет у игрока
        super.dispose(); // Вызвать dispose родительского класса JFrame
        System.out.println("PlayerFrame disposed.");
    }

    // --- Поле для Layout ---
    private JSplitPane mainSplitPane;

} // Конец класса PlayerFrame