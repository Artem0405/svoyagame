// Файл: HostFrame.java
package org.example.ui;

import org.example.data.Player;
import org.example.data.Question;
import org.example.data.Theme;
import org.example.game.GameController;
import org.example.ui.components.GameBoardPanel;
import org.example.ui.components.PlayerScorePanel;

// Импорты для звука
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

// Импорты для UI и событий
import javax.swing.*;
import javax.swing.border.Border; // Уточняем импорт Border
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
//import java.net.MalformedURLException; // Не используется для File -> AudioInputStream
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference; // Для передачи потока в SwingWorker

// Статический импорт для констант (опционально)
import static org.example.constants.AppConstants.*; // Если вы используете AppConstants

/**
 * Окно ведущего игры "Своя Игра".
 * Отображает игровую доску, счет игроков, текущий вопрос/ответ,
 * медиа-контент (аудио) и элементы управления игрой и ответами.
 */
public class HostFrame extends JFrame {

    // --- Основные компоненты ---
    private final GameController gameController;
    private PlayerScorePanel playerScorePanel;
    private GameBoardPanel gameBoardPanel;

    // --- Компоненты отображения вопроса/ответа ---
    private JPanel questionDisplayPanel; // Общая панель внизу
    private JTextArea questionArea;
    private JTextArea answerArea;

    // --- Компоненты управления ответом ---
    private JPanel controlPanel;         // Панель для кнопок "Верно"/"Неверно"/"Никто"

    // --- Медиа компоненты ---
    private JPanel mediaPanel;           // Панель для аудио/видео/картинок (справа от текста)
    private JLabel imageLabel;           // Метка для изображения (если будет)
    private JScrollPane imageScrollPane;   // Скролл для изображения (если будет)
    private JButton playSoundButton;      // Кнопка воспроизведения аудио
    private transient Clip currentClip;        // Текущий аудио клип
    private transient AudioInputStream currentAudioStream; // Текущий аудио поток (для закрытия)

    // --- Компоненты управления игрой (вверху) ---
    private JPanel topControlPanel;      // Панель над счетом
    private JButton pauseToggleButton;
    private JButton exitToMenuButton;
    private JButton nextRoundButton;
    private JLabel roundInfoLabel;
    private boolean isGamePaused = false; // Локальное состояние паузы для UI

    // --- Константы для UI (можно вынести в AppConstants) ---
    private static final Font QUESTION_FONT = new Font("Arial", Font.PLAIN, 18);
    private static final Font ANSWER_FONT = new Font("Arial", Font.BOLD, 18);
    private static final Font BUTTON_FONT_AUDIO = new Font("Arial", Font.BOLD, 14); // Уменьшенный шрифт для кнопки аудио
    private static final Dimension MEDIA_PANEL_SIZE = new Dimension(250, 100);
    private static final Dimension MIN_MEDIA_PANEL_SIZE = new Dimension(150, 50);
    private static final Color DEFAULT_TEXT_COLOR = UIManager.getColor("Label.foreground");

    /**
     * Конструктор окна ведущего.
     * @param controller Ссылка на игровой контроллер.
     * @throws IllegalArgumentException если controller равен null.
     */
    public HostFrame(GameController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("GameController cannot be null for HostFrame");
        }
        this.gameController = controller;

        setTitle("Своя Игра - Окно Ведущего");
        setPreferredSize(new Dimension(1200, 900)); // Рекомендуемый размер
        setMinimumSize(new Dimension(1000, 700)); // Минимальный размер
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Управляем закрытием через handleWindowClose
        setLocationRelativeTo(null); // Центрируем окно

        // Перехват попытки закрытия окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClose(); // Показать диалог подтверждения выхода
            }
            // Можно добавить windowOpened для инициализации, если нужно
        });

        createMenuBar();    // Создать стандартное меню "Игра"
        initComponents();   // Инициализировать все Swing компоненты
        layoutComponents(); // Разместить компоненты на форме

        pack();             // Рассчитать размеры перед вызовом initializeUI из контроллера
        System.out.println("HostFrame создан и инициализирован.");
    }

    /** Создает строку меню "Игра". */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Игра");

        JMenuItem backToMenu = new JMenuItem("Вернуться в главное меню");
        backToMenu.addActionListener(e -> handleGoToMenu());

        JMenuItem exitGame = new JMenuItem("Выход из приложения");
        exitGame.addActionListener(e -> handleExitApplication());

        gameMenu.add(backToMenu);
        gameMenu.addSeparator();
        gameMenu.add(exitGame);
        menuBar.add(gameMenu);
        setJMenuBar(menuBar);
    }

    /** Инициализирует все компоненты Swing. */
    private void initComponents() {
        // Панели счета и доски
        playerScorePanel = new PlayerScorePanel();
        gameBoardPanel = new GameBoardPanel(gameController, true); // true - доска ведущего

        // --- Панель отображения вопроса/ответа/медиа ---
        questionDisplayPanel = new JPanel(new BorderLayout(10, 5)); // Отступы между текстом и медиа
        questionDisplayPanel.setBorder(BorderFactory.createTitledBorder("Текущий вопрос / Ответ"));
        // Убрана установка PreferredSize, пусть определяется содержимым и layout

        // Текстовые области вопроса и ответа
        questionArea = createTextArea(false); // Не редактируемое
        answerArea = createTextArea(false); // Не редактируемое
        questionArea.setFont(QUESTION_FONT);
        answerArea.setFont(ANSWER_FONT); // Ответ жирным
        questionArea.setText("Ожидание начала раунда...");
        answerArea.setText("...");

        // Панель для текста (вопрос над ответом)
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5)); // 2 строки, 1 колонка, верт. отступ 5
        textPanel.add(new JScrollPane(questionArea)); // Оборачиваем в скролл
        textPanel.add(new JScrollPane(answerArea));   // Оборачиваем в скролл
        questionDisplayPanel.add(textPanel, BorderLayout.CENTER); // Текст в центре

        // --- Медиа панель (справа) ---
        mediaPanel = new JPanel(new BorderLayout(5, 5)); // Отступы внутри медиа панели
        mediaPanel.setPreferredSize(MEDIA_PANEL_SIZE); // Задаем предпочтительный размер
        mediaPanel.setMinimumSize(MIN_MEDIA_PANEL_SIZE);   // Задаем минимальный размер
        mediaPanel.setBorder(BorderFactory.createEtchedBorder());
        mediaPanel.setVisible(false); // Изначально скрыта

        // Кнопка аудио
        playSoundButton = new JButton("► Воспроизвести Аудио");
        playSoundButton.setFont(BUTTON_FONT_AUDIO);
        playSoundButton.setVisible(false);
        playSoundButton.setEnabled(false);
        playSoundButton.addActionListener(e -> playCurrentAudio());
        mediaPanel.add(playSoundButton, BorderLayout.SOUTH); // Кнопка внизу медиа панели

        // TODO: Добавить компоненты для изображения, если нужно (imageLabel, imageScrollPane)
        // imageLabel = new JLabel(); ...
        // imageScrollPane = new JScrollPane(imageLabel); ...
        // mediaPanel.add(imageScrollPane, BorderLayout.CENTER);

        // Добавляем медиа панель справа от текста вопроса/ответа
        questionDisplayPanel.add(mediaPanel, BorderLayout.EAST);

        // --- Панель кнопок ответа ("Верно"/"Неверно") ---
        controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5)); // Центрирование кнопок
        controlPanel.setBorder(BorderFactory.createTitledBorder("Управление ответом"));
        controlPanel.setVisible(false); // Изначально скрыта

        // --- Верхняя панель управления игрой ---
        topControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5)); // Отступы между кнопками
        // Использование константы для шрифта (пример)
        Font infoFont = HEADER_FONT != null ? HEADER_FONT : new Font("Arial", Font.BOLD, 16);
        roundInfoLabel = new JLabel("Раунд: -");
        roundInfoLabel.setFont(infoFont);

        pauseToggleButton = new JButton("Пауза");
        pauseToggleButton.addActionListener(e -> gameController.togglePause());

        nextRoundButton = new JButton("Следующий раунд");
        nextRoundButton.setEnabled(false); // Активируется при завершении раунда
        nextRoundButton.addActionListener(e -> gameController.startNextRound());

        exitToMenuButton = new JButton("В меню (Завершить)");
        exitToMenuButton.setToolTipText("Завершить текущую игру и вернуться в главное меню");
        exitToMenuButton.addActionListener(e -> handleGoToMenu());

        topControlPanel.add(roundInfoLabel);
        topControlPanel.add(Box.createHorizontalStrut(20)); // Больше промежуток
        topControlPanel.add(pauseToggleButton);
        topControlPanel.add(nextRoundButton);
        topControlPanel.add(exitToMenuButton);
    }

    /** Вспомогательный метод для создания и настройки JTextArea. */
    private JTextArea createTextArea(boolean editable) {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(editable);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setMargin(new Insets(5, 8, 5, 8)); // Отступы внутри поля
        return textArea;
    }

    /** Размещает инициализированные компоненты на фрейме. */
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10)); // Отступы между основными панелями

        // Верхняя часть: Панель управления игрой над панелью счета
        JPanel topAreaPanel = new JPanel(new BorderLayout(0, 5)); // Вертикальный отступ
        topAreaPanel.add(topControlPanel, BorderLayout.NORTH);
        topAreaPanel.add(playerScorePanel, BorderLayout.CENTER);
        add(topAreaPanel, BorderLayout.NORTH);

        // Центр: Игровая доска
        // Добавляем небольшой отступ вокруг доски
        JPanel boardPanelWrapper = new JPanel(new BorderLayout());
        boardPanelWrapper.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5)); // Отступы слева/справа
        boardPanelWrapper.add(gameBoardPanel, BorderLayout.CENTER);
        add(boardPanelWrapper, BorderLayout.CENTER);

        // Низ: Панель вопроса/ответа/медиа над панелью кнопок ответа
        JPanel bottomAreaPanel = new JPanel(new BorderLayout(0, 5)); // Вертикальный отступ
        bottomAreaPanel.add(questionDisplayPanel, BorderLayout.CENTER);
        bottomAreaPanel.add(controlPanel, BorderLayout.SOUTH);
        add(bottomAreaPanel, BorderLayout.SOUTH);

        // Добавить общие внешние отступы для всего фрейма
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    // --- Методы обновления UI, вызываемые из GameController ---

    /** Инициализирует UI начальными данными игроков. */
    public void initializeUI(List<Player> players) {
        if (playerScorePanel != null) {
            // Выполняем в EDT
            SwingUtilities.invokeLater(() -> {
                playerScorePanel.setPlayers(players);
                playerScorePanel.updateScores();
                System.out.println("HostFrame UI Initialized with players.");
            });
        } else {
            System.err.println("HostFrame.initializeUI: playerScorePanel is null!");
        }
    }

    /** Обновляет информацию о текущем раунде. */
    public void updateRoundInfo(int roundNumber, String roundName) {
        String info = "Раунд: " + roundNumber;
        // Добавляем имя раунда, если оно не стандартное
        if (roundName != null && !roundName.isBlank() &&
                !roundName.equalsIgnoreCase("Раунд " + roundNumber)) {
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
            // Само обновление происходит внутри PlayerScorePanel, просто даем команду
            SwingUtilities.invokeLater(() -> playerScorePanel.updateScores());
        }
    }

    /** Отображает данные текущего вопроса (текст, ответ, медиа). */
    public void displayQuestion(Question question) {
        System.out.println("HostFrame.displayQuestion called.");

        // Выполняем все изменения UI в EDT
        SwingUtilities.invokeLater(() -> {
            // 1. Очистка предыдущего состояния
            stopAndCloseCurrentAudio();
            mediaPanel.setVisible(false);
            playSoundButton.setVisible(false);
            playSoundButton.setEnabled(false);
            playSoundButton.setText("► Воспроизвести Аудио");
            // Очистка изображения, если есть
            // if (imageLabel != null) imageLabel.setIcon(null);
            // if (imageScrollPane != null) imageScrollPane.setVisible(false);

            // 2. Обработка null вопроса
            if (question == null) {
                System.err.println("HostFrame.displayQuestion: Received null question.");
                questionArea.setText("Ошибка: Вопрос не получен.");
                answerArea.setText("...");
                controlPanel.setVisible(false); // Скрыть кнопки ответа
                controlPanel.removeAll();
                questionDisplayPanel.revalidate(); // Обновить UI
                questionDisplayPanel.repaint();
                controlPanel.revalidate();
                controlPanel.repaint();
                return;
            }

            // 3. Отображение текста и ответа
            String themeName = findThemeName(question);
            questionArea.setText("Тема: " + themeName + " (" +
                    question.getPoints() + ")\n\n" +
                    (question.getQuestionText() != null ? question.getQuestionText() : "[Нет текста вопроса]"));
            answerArea.setText("Ответ: " +
                    (question.getAnswerText() != null ? question.getAnswerText() : "[Нет текста ответа]"));
            questionArea.setCaretPosition(0); // Прокрутка вверх
            answerArea.setCaretPosition(0);   // Прокрутка вверх

            // 4. Получение типа и пути
            Question.QuestionType type = question.getType();
            String filePath = question.getFilePath();
            System.out.println("  Question Type: " + type + ", FilePath: '" + filePath + "'");

            boolean mediaPanelNeedsShowing = false;

            // 5. Обработка АУДИО
            if (type == Question.QuestionType.AUDIO) {
                if (filePath != null && !filePath.trim().isEmpty()) {
                    System.out.println("  Processing AUDIO question...");
                    mediaPanelNeedsShowing = true;
                    playSoundButton.setVisible(true);
                    playSoundButton.setEnabled(false); // Деактивировать на время загрузки
                    playSoundButton.setText("Загрузка аудио...");
                    preloadAudio(new File(filePath)); // Загружаем аудио (асинхронно)
                } else {
                    System.err.println("  AUDIO question type but filePath is missing or empty!");
                    questionArea.append("\n\n[ОШИБКА: Не указан путь к аудиофайлу]");
                }
            }
            // 6. Обработка ИЗОБРАЖЕНИЯ (заглушка)
            else if (type == Question.QuestionType.IMAGE) {
                if (filePath != null && !filePath.trim().isEmpty()) {
                    System.out.println("  Processing IMAGE question...");
                    // mediaPanelNeedsShowing = true;
                    // loadImage(new File(filePath)); // Загрузить картинку
                } else {
                    System.err.println("  IMAGE question type but filePath is missing or empty!");
                    questionArea.append("\n\n[ОШИБКА: Не указан путь к файлу изображения]");
                }
            }

            // 7. Обновление видимости медиа панели
            mediaPanel.setVisible(mediaPanelNeedsShowing);

            // 8. Панель кнопок ответа будет показана методами setup...Buttons

            // 9. Перерисовка панелей
            questionDisplayPanel.revalidate();
            questionDisplayPanel.repaint();
            // controlPanel пока не трогаем, он обновится в setupButtonsInternal

            System.out.println("HostFrame.displayQuestion UI update scheduled.");
        }); // Конец invokeLater для displayQuestion
    }


    /** Очищает область отображения вопроса/ответа и останавливает медиа. */
    public void clearQuestionArea() {
        System.out.println("HostFrame.clearQuestionArea called.");
        SwingUtilities.invokeLater(() -> { // Все изменения UI в EDT
            stopAndCloseCurrentAudio();
            questionArea.setText("Ожидание выбора вопроса...");
            answerArea.setText("...");
            questionArea.setCaretPosition(0);
            answerArea.setCaretPosition(0);

            mediaPanel.setVisible(false);
            playSoundButton.setVisible(false);
            // Скрыть изображение, если есть

            controlPanel.removeAll();
            controlPanel.setVisible(false);

            questionDisplayPanel.revalidate();
            questionDisplayPanel.repaint();
            controlPanel.revalidate();
            controlPanel.repaint();
            System.out.println("  Question area cleared.");
        });
    }

    /** Настраивает кнопки ответа для всех игроков (обычный вопрос). */
    public void setupAnswerControlButtons(List<Player> players) {
        System.out.println("HostFrame: Setting up general answer buttons.");
        SwingUtilities.invokeLater(() -> setupButtonsInternal(players, null)); // null - нет победителя аукциона
    }

    /** Настраивает кнопки ответа для одного конкретного игрока. */
    public void setupSpecificAnswerButtons(Player targetPlayer) {
        if (targetPlayer == null) {
            System.err.println("HostFrame.setupSpecificAnswerButtons: targetPlayer is null!");
            // Можно очистить кнопки или показать сообщение
            SwingUtilities.invokeLater(() -> {
                controlPanel.removeAll();
                controlPanel.add(new JLabel("Ошибка: Игрок не указан."));
                controlPanel.setVisible(true);
                controlPanel.revalidate();
                controlPanel.repaint();
            });
            return;
        }
        System.out.println("HostFrame: Setting up specific answer buttons for " + targetPlayer.getName());
        SwingUtilities.invokeLater(() -> setupButtonsInternal(List.of(targetPlayer), targetPlayer));
    }

    /** Внутренний метод для создания и настройки кнопок ответа. Вызывается в EDT. */
    private void setupButtonsInternal(List<Player> playersToShow, Player auctionWinner) {
        controlPanel.removeAll(); // Очищаем предыдущие кнопки
        controlPanel.setVisible(true); // Делаем панель видимой

        Question currentQuestion = gameController.getCurrentQuestion(); // Получаем текущий вопрос
        boolean isSpecialQuestion = currentQuestion != null && currentQuestion.isSpecial();
        boolean isAuction = currentQuestion != null && currentQuestion.getSpecialType() == Question.SpecialType.AUCTION;

        if (playersToShow == null || playersToShow.isEmpty()) {
            controlPanel.add(new JLabel("Нет игроков для ответа."));
        } else {
            // Создаем кнопки для каждого указанного игрока
            for (Player player : playersToShow) {
                JPanel playerButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
                JButton correctButton = new JButton(player.getName() + " - Верно");
                JButton incorrectButton = new JButton(player.getName() + " - Неверно");

                correctButton.setEnabled(!isGamePaused);
                incorrectButton.setEnabled(!isGamePaused);

                // Используем лямбды для слушателей
                correctButton.addActionListener(e -> {
                    if (!isGamePaused) gameController.processAnswer(player, true);
                });
                incorrectButton.addActionListener(e -> {
                    if (!isGamePaused) gameController.processAnswer(player, false);
                });

                playerButtonPanel.add(correctButton);
                playerButtonPanel.add(incorrectButton);

                // Добавляем кнопку "Пас" для аукциона и только для победителя
                if (isAuction && player.equals(auctionWinner)) {
                    JButton passButton = new JButton("Пас");
                    passButton.setEnabled(!isGamePaused);
                    passButton.addActionListener(e -> {
                        if (!isGamePaused) gameController.processAnswer(player, false); // Пас = неверно
                    });
                    playerButtonPanel.add(passButton);
                }
                controlPanel.add(playerButtonPanel); // Добавляем панель игрока на общую панель
            }
        }

        // Добавляем кнопку "Никто / Снять вопрос" только для ОБЫЧНЫХ вопросов
        // и если показаны кнопки для НЕСКОЛЬКИХ игроков.
        if (!isSpecialQuestion && playersToShow != null && playersToShow.size() > 1) {
            JButton noAnswerButton = new JButton("Никто / Снять вопрос");
            noAnswerButton.setEnabled(!isGamePaused);
            noAnswerButton.addActionListener(e -> {
                if (!isGamePaused) gameController.processAnswer(null, false); // null игрок означает "Никто"
            });
            // Добавляем отступ для этой кнопки
            JPanel nobodyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            nobodyPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
            nobodyPanel.add(noAnswerButton);
            controlPanel.add(nobodyPanel);
        }

        // Обновляем панель кнопок
        controlPanel.revalidate();
        controlPanel.repaint();
        System.out.println("  Answer buttons setup complete.");
    }

    /** Обновляет состояние UI в ответ на изменение состояния паузы. */
    public void updatePauseState(boolean isPaused) {
        this.isGamePaused = isPaused;
        SwingUtilities.invokeLater(() -> { // Обновление UI в EDT
            pauseToggleButton.setText(isPaused ? "Продолжить" : "Пауза");
            // Обновляем состояние кнопок ответа
            if (controlPanel.isVisible()) {
                for (Component comp : controlPanel.getComponents()) {
                    setEnabledRecursively(comp, !isPaused); // Рекурсивно включаем/выключаем
                }
                controlPanel.revalidate();
                controlPanel.repaint();
            }
            System.out.println("HostFrame UI updated for pause state: " + isPaused);
        });
    }

    /** Рекурсивно устанавливает состояние enabled для компонента и его дочерних элементов. */
    private void setEnabledRecursively(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                setEnabledRecursively(child, enabled);
            }
        }
    }

    /** Устанавливает состояние активности кнопки "Следующий раунд". */
    public void setNextRoundButtonEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            if (nextRoundButton != null) {
                nextRoundButton.setEnabled(enabled);
            }
        });
    }

    // --- Методы для работы со звуком ---

    /** Асинхронно загружает аудиофайл. */
    private void preloadAudio(File audioFile) {
        stopAndCloseCurrentAudio(); // Закрыть предыдущий клип/поток перед загрузкой нового

        if (audioFile == null || !audioFile.exists() || !audioFile.isFile()) {
            String path = (audioFile != null) ? audioFile.getAbsolutePath() : "null";
            System.err.println("Audio file is null, not found, or not a file: " + path);
            SwingUtilities.invokeLater(()-> {
                playSoundButton.setText("Файл не найден!");
                playSoundButton.setEnabled(false);
                playSoundButton.setVisible(true); // Показать кнопку с ошибкой
                mediaPanel.setVisible(true);    // Показать панель
                mediaPanel.revalidate();
                mediaPanel.repaint();
                questionArea.append("\n\n[ОШИБКА: Аудиофайл не найден: " + path + "]");
            });
            return;
        }

        System.out.println("Preloading audio: " + audioFile.getAbsolutePath());

        // Используем AtomicReference для передачи потока из фонового в EDT
        AtomicReference<AudioInputStream> audioStreamRef = new AtomicReference<>(null);

        SwingWorker<Clip, Void> worker = new SwingWorker<>() {
            @Override
            protected Clip doInBackground() throws Exception {
                // Этот код выполняется в фоновом потоке
                AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile);
                audioStreamRef.set(stream); // Сохраняем ссылку на поток
                Clip clip = AudioSystem.getClip();
                clip.open(stream); // Открываем клип с потоком
                return clip;       // Возвращаем готовый клип
            }

            @Override
            protected void done() {
                // Этот код выполняется в EDT после завершения doInBackground
                AudioInputStream audioStream = audioStreamRef.get(); // Получаем поток
                try {
                    // Проверяем, не был ли клип закрыт, пока шла загрузка
                    if (currentClip != null) {
                        System.out.println("Preload finished, but another clip action occurred. Ignoring result.");
                        if(audioStream != null) try { audioStream.close(); } catch (IOException e) {/* ignore */}
                        return;
                    }

                    currentClip = get(); // Получаем клип (может бросить исключение, если doInBackground упал)
                    currentAudioStream = audioStream; // Сохраняем поток для будущего закрытия
                    System.out.println("Audio preloaded successfully in background: " + audioFile.getName());

                    // Добавляем слушателя событий линии
                    currentClip.addLineListener(event -> handleAudioLineEvent(event));

                    // Обновляем UI кнопки
                    playSoundButton.setEnabled(true);
                    playSoundButton.setText("► Воспроизвести Аудио");
                    // Перерисовка не нужна здесь, т.к. панель уже видима и кнопка обновилась

                } catch (Exception e) {
                    // Обработка ошибок, возникших в doInBackground
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    System.err.println("Error preloading audio in background: " + audioFile.getPath() + " - " + cause.getMessage());
                    cause.printStackTrace();

                    String errorText = "Ошибка загрузки аудио";
                    if (cause instanceof UnsupportedAudioFileException) {
                        errorText = "Неподдерживаемый формат";
                    } else if (cause instanceof IOException) {
                        errorText = "Ошибка чтения файла";
                    } else if (cause instanceof LineUnavailableException) {
                        errorText = "Ошибка линии звука";
                    }
                    final String finalErrorText = errorText;
                    SwingUtilities.invokeLater(() -> {
                        playSoundButton.setText(finalErrorText);
                        playSoundButton.setEnabled(false); // Кнопка неактивна при ошибке
                    });

                    // Закрываем поток, если он был открыт, но произошла ошибка
                    if (audioStream != null) {
                        try { audioStream.close(); } catch (IOException ioex) { /* ignore */ }
                    }
                    // Сбрасываем ссылки
                    currentAudioStream = null;
                    currentClip = null;
                }
            }
        };
        worker.execute(); // Запускаем фоновую задачу
    }

    /** Обрабатывает события звуковой линии (START, STOP). Вызывается из LineListener. */
    private void handleAudioLineEvent(LineEvent event) {
        final LineEvent.Type type = event.getType(); // Сохраняем тип в final переменную
        SwingUtilities.invokeLater(() -> { // Обновляем UI в EDT
            // Проверяем, актуален ли клип, с которым связано событие
            if (event.getLine() != currentClip || currentClip == null || !currentClip.isOpen()) {
                // Событие от старого или закрытого клипа - игнорируем
                return;
            }

            if (type == LineEvent.Type.STOP) {
                boolean reachedEnd = currentClip.getMicrosecondPosition() >= currentClip.getMicrosecondLength();
                if (reachedEnd) {
                    currentClip.setMicrosecondPosition(0); // Перемотать на начало
                    System.out.println("Audio playback finished naturally, rewound.");
                } else {
                    System.out.println("Audio playback stopped manually or other.");
                }
                playSoundButton.setText("► Воспроизвести Аудио");
                playSoundButton.setEnabled(true); // Всегда активна после остановки
            } else if (type == LineEvent.Type.START) {
                playSoundButton.setText("❚❚ Остановить Аудио");
                playSoundButton.setEnabled(true);
                System.out.println("Audio playback started via LineEvent.");
            }
        });
    }

    /** Воспроизводит или останавливает текущий загруженный аудио клип. */
    private void playCurrentAudio() {
        if (currentClip != null && currentClip.isOpen()) {
            if (currentClip.isRunning()) {
                System.out.println("User stopping currently playing audio.");
                currentClip.stop(); // Останавливаем; LineListener обработает
            } else {
                System.out.println("User starting audio playback from beginning.");
                currentClip.setMicrosecondPosition(0); // Начинаем с начала
                currentClip.start(); // Запускаем; LineListener обработает
            }
        } else {
            System.err.println("Play button clicked, but clip is null or not open.");
            // Можно показать сообщение пользователю
            JOptionPane.showMessageDialog(this,
                    "Аудиофайл не загружен или произошла ошибка.\nПопробуйте выбрать вопрос заново.",
                    "Ошибка воспроизведения", JOptionPane.WARNING_MESSAGE);
            // Сбросить кнопку на всякий случай
            playSoundButton.setText("Ошибка аудио");
            playSoundButton.setEnabled(false);
        }
    }

    /** Останавливает воспроизведение, закрывает клип и аудио поток. */
    private void stopAndCloseCurrentAudio() {
        System.out.println("stopAndCloseCurrentAudio called.");
        Clip clipToClose = currentClip; // Копируем ссылку, чтобы избежать гонки потоков
        AudioInputStream streamToClose = currentAudioStream;

        currentClip = null; // Немедленно сбрасываем ссылки
        currentAudioStream = null;

        if (clipToClose != null) {
            try {
                if (clipToClose.isRunning()) {
                    clipToClose.stop();
                }
                if (clipToClose.isOpen()) {
                    clipToClose.close(); // Освобождаем линию
                    System.out.println("Audio clip closed.");
                }
            } catch (Exception e) {
                System.err.println("Error stopping/closing audio clip: " + e.getMessage());
            }
        }
        if (streamToClose != null) {
            try {
                streamToClose.close(); // Закрываем поток
                System.out.println("Audio stream closed.");
            } catch (IOException e) {
                System.err.println("IOException while closing audio stream: " + e.getMessage());
            }
        }

        // Обновляем UI кнопки в EDT (на случай, если вызвано не из clearQuestionArea)
        SwingUtilities.invokeLater(() -> {
            if (playSoundButton != null) {
                // Не скрываем здесь, это делает clearQuestionArea или displayQuestion
                playSoundButton.setEnabled(false);
                playSoundButton.setText("► Воспроизвести Аудио");
            }
        });
    }

    // --- Методы обработки событий окна и меню ---

    /** Обрабатывает закрытие окна (нажатие на крестик). */
    private void handleWindowClose() {
        handleExitApplication(); // Показываем тот же диалог, что и при выходе из меню
    }

    /** Обрабатывает нажатие "Вернуться в главное меню". */
    private void handleGoToMenu() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Вы уверены, что хотите завершить текущую игру и вернуться в главное меню?",
                "Завершение игры", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            gameController.finishGameAndGoToMenu();
        }
    }

    /** Обрабатывает нажатие "Выход из приложения". */
    private void handleExitApplication() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Вы уверены, что хотите выйти из приложения?",
                "Выход из приложения", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            gameController.exitApplication();
        }
    }

    // --- Вспомогательные методы ---

    /** Находит имя темы для заданного вопроса. */
    private String findThemeName(Question question) {
        if (question == null || gameController == null) return "[Ошибка]";
        try {
            List<Theme> currentThemes = gameController.getThemesForCurrentRound();
            if (currentThemes == null || currentThemes.isEmpty()) return "[Нет тем]";

            for (Theme theme : currentThemes) {
                // Добавим проверку на null для theme и списка вопросов
                if (theme != null && theme.getQuestions() != null && theme.getQuestions().contains(question)) {
                    // Возвращаем имя темы или плейсхолдер, если имя null/пустое
                    String themeName = theme.getThemeName();
                    return (themeName != null && !themeName.trim().isEmpty()) ? themeName : "[Тема без имени]";
                }
            }
        } catch (Exception e) {
            System.err.println("Error in findThemeName: " + e.getMessage());
            return "[Ошибка темы]"; // Возвращаем ошибку, если что-то пошло не так
        }
        return "[Тема?]"; // Если вопрос не найден ни в одной теме
    }

    /** Возвращает панель игровой доски (для GameController). */
    public GameBoardPanel getGameBoardPanel() {
        return gameBoardPanel;
    }

    /** Переопределенный метод для освобождения ресурсов при закрытии окна. */
    @Override
    public void dispose() {
        System.out.println("Disposing HostFrame...");
        stopAndCloseCurrentAudio(); // Важно остановить звук и освободить ресурсы!
        // Здесь можно добавить освобождение других ресурсов, если они есть
        super.dispose(); // Вызвать dispose родительского класса JFrame
        System.out.println("HostFrame disposed.");
    }
}