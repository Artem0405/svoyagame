package org.example.ui;

import org.example.data.*;
import org.example.data.PackStorage;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PackCreatorFrame extends JFrame {

    private final JFrame parentFrame; // Не будет ошибки после исправления конструкторов
    private QuestionPack currentPack;
    private Path editingFilePath = null;

    private DefaultListModel<String> themeListModel;
    private DefaultListModel<String> questionListModel;

    private JTextField packNameField;
    private JList<String> themeList;
    private JList<String> questionList;
    private JTextField newThemeField;
    private JTextArea questionTextArea;
    private JTextField answerField;
    private JSpinner pointsSpinner;
    private JComboBox<Question.QuestionType> typeComboBox;
    private JComboBox<Question.SpecialType> specialTypeComboBox;
    private JButton fileChooserButton;
    private JLabel filePathLabel;
    private JButton addThemeButton;
    private JButton removeThemeButton;
    private JButton newQuestionButton;
    private JButton saveQuestionButton;
    private JButton removeQuestionButton;
    private JButton savePackButton;
    private JButton backButton;

    private JPanel questionEditorPanel;

    private int selectedThemeIndex = -1;
    private int selectedQuestionIndex = -1;

    /**
     * Конструктор для СОЗДАНИЯ нового пакета.
     * @param parent Родительское окно (MainMenuFrame или GameSetupFrame).
     */
    public PackCreatorFrame(JFrame parent) { // Строка ~29 (бывшая 27)
        this.parentFrame = parent; // Сохраняем ссылку

        // Инициализируем модели данных до компонентов UI
        this.themeListModel = new DefaultListModel<>();
        this.questionListModel = new DefaultListModel<>();

        // ---- Базовая настройка окна ----
        setTitle("Создание Нового Пака Вопросов");
        setSize(950, 750);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowCloseRequest();
            }
        });

        // ---- Инициализация данных и UI ----
        // 1. Создать пустой пак
        loadOrCreatePack(null); // null означает создание нового

        // 2. Инициализировать все компоненты Swing
        initComponents();

        // 3. Разместить компоненты на форме
        layoutComponents();

        // 4. Добавить слушателей к компонентам
        addListeners();

        // 5. Начальное состояние UI
        updateThemeList(); // Заполнит список тем (будет пустым)
        clearQuestionEditor(); // Очистит редактор вопроса
        updateButtonStates(); // Установит начальное состояние кнопок

        System.out.println("PackCreatorFrame (New) инициализирован.");
    } // Конец первого конструктора

    /**
     * Конструктор для РЕДАКТИРОВАНИЯ существующего пакета.
     * @param parent Родительское окно (GameSetupFrame).
     * @param packPathToLoad Путь к файлу пакета (*.json) для загрузки и редактирования.
     */
    // ИСПРАВЛЕНО: Добавлен второй параметр Path packPathToLoad
    public PackCreatorFrame(JFrame parent, Path packPathToLoad) {
        // ИСПРАВЛЕНО: Вызываем первый конструктор для базовой инициализации
        this(parent);

        this.editingFilePath = packPathToLoad; // Сохраняем путь

        // Обновляем заголовок
        String title = "Редактирование Пака: ";
        try { // Безопасное получение имени файла
            title += (packPathToLoad != null ? packPathToLoad.getFileName().toString() : "Ошибка");
        } catch (Exception e) {
            title += "[Не удалось получить имя файла]";
        }
        setTitle(title);

        // ---- Инициализация данных и UI ----
        // 1. Загрузить пак из файла (перезапишет пустой пак, созданный в this(parent))
        loadOrCreatePack(packPathToLoad); // Передаем путь для загрузки

        // 2. Заполнить UI данными из загруженного пакета
        populateUIFromCurrentPack();

        // 3. Установить начальное состояние кнопок после загрузки
        updateButtonStates();

        System.out.println("PackCreatorFrame (Edit) инициализирован для файла: " + editingFilePath);

        // Компоненты, layout, слушатели уже инициализированы вызовом this(parent)
    }

    // Метод для загрузки или создания нового пака
    private void loadOrCreatePack(Path packPathToLoad) { // packPathToLoad теперь используется
        if (packPathToLoad != null) {
            System.out.println("Загрузка пакета для редактирования: " + packPathToLoad);
            try {
                currentPack = PackStorage.loadPack(packPathToLoad);
                if (currentPack == null) {
                    throw new IOException("PackStorage.loadPack вернул null");
                }
                // Валидация после загрузки (опционально, но рекомендуется)
                try {
                    validatePackContent(currentPack); // Вызываем метод валидации
                } catch (IllegalArgumentException e) {
                    System.err.println("Предупреждение: Загруженный пакет содержит ошибки: " + e.getMessage());
                    // ИСПРАВЛЕНО: Добавлен третий аргумент - тип сообщения
                    showErrorDialog("Загруженный пакет содержит ошибки:\n" + e.getMessage() +
                                    "\nВы можете исправить их и сохранить.",
                            "Ошибки в Пакете", JOptionPane.WARNING_MESSAGE);
                }
                // Если темы не инициализированы в пакете
                if (currentPack.getThemes() == null) {
                    currentPack.setThemes(new ArrayList<>());
                }
                this.editingFilePath = packPathToLoad; // Сохраняем путь
                System.out.println("Пакет '" + currentPack.getPackName() + "' загружен.");

            } catch (Exception e) {
                System.err.println("Ошибка при загрузке пакета для редактирования: " + e.getMessage());
                e.printStackTrace();
                showErrorDialog("Не удалось загрузить пакет '" + packPathToLoad.getFileName() + "' для редактирования:\n" + e.getMessage() +
                        "\nБудет создан новый пустой пакет.", "Ошибка Загрузки");
                createNewPackInternal(); // Создаем пустой пак при ошибке
                this.editingFilePath = null; // Сбрасываем путь
                setTitle("Создание Нового Пака (Ошибка загрузки)"); // Меняем заголовок
            }
        } else {
            // Создаем новый пустой пак
            System.out.println("Создание нового пустого пакета.");
            createNewPackInternal();
            this.editingFilePath = null;
        }
    }


    // Вспомогательный метод для создания пустого пака
    private void createNewPackInternal() {
        currentPack = new QuestionPack("Новый пак");
        currentPack.setThemes(new ArrayList<>());
    }

    // Валидация содержимого пакета (такой же, как в GameSetupFrame)
    private void validatePackContent(QuestionPack pack) throws IllegalArgumentException {
        if (pack == null) throw new IllegalArgumentException("Объект пака не может быть null.");
        if (pack.getPackName() == null || pack.getPackName().trim().isEmpty()) {
            System.out.println("Предупреждение: У пака нет названия."); // Не критично
        }
        if (pack.getThemes() == null || pack.getThemes().isEmpty()) {
            throw new IllegalArgumentException("Пак должен содержать хотя бы одну тему.");
        }
        boolean hasQuestionsOverall = false;
        for (int i = 0; i < pack.getThemes().size(); i++) {
            Theme theme = pack.getThemes().get(i);
            if (theme == null) throw new IllegalArgumentException("Обнаружена пустая тема (null) в пакете (индекс " + i + ").");

            String themeName = theme.getThemeName();
            if (themeName == null || themeName.trim().isEmpty()) {
                System.out.println("Предупреждение: Тема с индексом " + i + " не имеет названия.");
                themeName = "Тема " + (i + 1);
            }

            List<Question> questions = theme.getQuestions();
            if (questions == null || questions.isEmpty()) {
                System.out.println("Предупреждение: Тема '" + themeName + "' не содержит вопросов.");
                continue;
            }

            for (int j = 0; j < questions.size(); j++) {
                Question q = questions.get(j);
                if (q == null) throw new IllegalArgumentException("Обнаружен пустой вопрос (null) в теме '" + themeName + "' (индекс " + j + ").");
                if (q.getQuestionText() == null || q.getQuestionText().trim().isEmpty()) {
                    throw new IllegalArgumentException("Вопрос №" + (j + 1) + " в теме '" + themeName + "' имеет пустой текст.");
                }
                if (q.getAnswerText() == null || q.getAnswerText().trim().isEmpty()) {
                    throw new IllegalArgumentException("Вопрос №" + (j + 1) + " в теме '" + themeName + "' имеет пустой ответ.");
                }
                if (q.getPoints() <= 0) {
                    throw new IllegalArgumentException("Вопрос №" + (j + 1) + " в теме '" + themeName + "' имеет некорректное количество баллов (" + q.getPoints() + "). Баллы должны быть > 0.");
                }
                if (q.getType() == null) {
                    throw new IllegalArgumentException("Вопрос №" + (j + 1) + " в теме '" + themeName + "' не имеет типа (TEXT/IMAGE/AUDIO).");
                }
                if ((q.getType() == Question.QuestionType.IMAGE || q.getType() == Question.QuestionType.AUDIO)) {
                    String filePath = q.getFilePath();
                    if (filePath == null || filePath.trim().isEmpty()) {
                        throw new IllegalArgumentException("Вопрос №" + (j + 1) + " в теме '" + themeName + "' типа " + q.getType() + " не имеет пути к файлу.");
                    }
                }
                if (q.getSpecialType() == null) {
                    System.err.println("Внимание! У вопроса №"+(j+1)+" в теме '"+themeName+"' specialType=null. Установлен NONE.");
                    q.setSpecialType(Question.SpecialType.NONE);
                }
                hasQuestionsOverall = true;
            }
        }
        if (!hasQuestionsOverall) {
            throw new IllegalArgumentException("В пакете не найдено ни одного валидного вопроса.");
        }
    }

    // 1. Инициализация Компонентов
    private void initComponents() {
        // Название пака
        packNameField = new JTextField((currentPack != null ? currentPack.getPackName() : ""), 30);

        // Список тем
        themeListModel = new DefaultListModel<>();
        themeList = new JList<>(themeListModel);
        themeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        themeList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        themeList.setPrototypeCellValue("Пример длинного названия темы X"); // Для расчета ширины

        // Поле для новой темы и кнопки управления темами
        newThemeField = new JTextField(15);
        addThemeButton = new JButton("Добавить тему");
        removeThemeButton = new JButton("Удалить тему");

        // Список вопросов
        questionListModel = new DefaultListModel<>();
        questionList = new JList<>(questionListModel);
        questionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        questionList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        questionList.setPrototypeCellValue("Пример длинного вопроса (1000 очков)");

        // Компоненты редактора вопросов
        questionTextArea = new JTextArea(5, 30);
        questionTextArea.setLineWrap(true);
        questionTextArea.setWrapStyleWord(true);
        questionTextArea.setFont(UIManager.getFont("TextField.font")); // Стандартный шрифт

        answerField = new JTextField(30);
        pointsSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 10000, 10)); // Значение, мин, макс, шаг
        typeComboBox = new JComboBox<>(Question.QuestionType.values()); // Заполняем типами из enum

        // --- Инициализация ComboBox для SpecialType ---
        specialTypeComboBox = new JComboBox<>(Question.SpecialType.values());
        specialTypeComboBox.setSelectedItem(Question.SpecialType.NONE);
        // --- Конец инициализации ---

        fileChooserButton = new JButton("Выбрать файл...");
        filePathLabel = new JLabel("Файл не выбран");
        filePathLabel.setPreferredSize(new Dimension(200, 20)); // Ограничим размер
        filePathLabel.setMinimumSize(new Dimension(100, 20));
        filePathLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0)); // Отступ слева

        // Кнопки управления вопросами
        newQuestionButton = new JButton("Новый вопрос");
        saveQuestionButton = new JButton("Сохранить вопрос");
        removeQuestionButton = new JButton("Удалить вопрос");

        // Кнопки управления паком
        savePackButton = new JButton("Сохранить пак в файл");
        backButton = new JButton("Назад");
    }

    // 2. Расположение Компонентов (Layout)
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10)); // Основной менеджер компоновки

        // --- Верхняя панель: Название пака ---
        JPanel packNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        packNamePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        packNamePanel.add(new JLabel("Название пака:"));
        packNamePanel.add(packNameField);
        add(packNamePanel, BorderLayout.NORTH);

        // --- Левая панель: Темы ---
        JPanel themePanel = new JPanel(new BorderLayout(5, 5));
        themePanel.setBorder(BorderFactory.createTitledBorder("Темы"));
        themePanel.add(new JScrollPane(themeList), BorderLayout.CENTER);

        // Панель управления темами (ввод новой, кнопки)
        JPanel themeControlPanel = new JPanel(); // Используем Box Layout для вертикали
        themeControlPanel.setLayout(new BoxLayout(themeControlPanel, BoxLayout.Y_AXIS));
        themeControlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Подпанель для ввода новой темы
        JPanel newThemeInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        newThemeInputPanel.add(new JLabel("Новая:"));
        newThemeInputPanel.add(newThemeField);
        // Ограничиваем высоту, чтобы BoxLayout работал корректно
        newThemeInputPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, newThemeInputPanel.getPreferredSize().height));
        newThemeInputPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Подпанель для кнопок тем
        JPanel themeButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
        themeButtonPanel.add(addThemeButton);
        themeButtonPanel.add(removeThemeButton);
        themeButtonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, themeButtonPanel.getPreferredSize().height));
        themeButtonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        themeControlPanel.add(newThemeInputPanel);
        themeControlPanel.add(themeButtonPanel);
        themePanel.add(themeControlPanel, BorderLayout.SOUTH);
        themePanel.setPreferredSize(new Dimension(300, 0)); // Задаем предпочтительную ширину
        add(themePanel, BorderLayout.WEST);

        // --- Центральная и правая панель: Список вопросов и Редактор ---
        JPanel questionsAndEditorPanel = new JPanel(new BorderLayout(10, 10));
        questionsAndEditorPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        // Разделитель для списка вопросов и редактора
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.35); // Меньше места списку вопросов
        splitPane.setContinuousLayout(true);

        // Панель списка вопросов
        JPanel questionListPanel = new JPanel(new BorderLayout(5, 5));
        questionListPanel.setBorder(BorderFactory.createTitledBorder("Вопросы в выбранной теме"));
        questionListPanel.add(new JScrollPane(questionList), BorderLayout.CENTER);
        questionListPanel.setMinimumSize(new Dimension(200, 150)); // Мин. размер для списка
        splitPane.setTopComponent(questionListPanel);

        // Панель редактора вопроса (используем GridBagLayout для гибкости)
        questionEditorPanel = new JPanel(new GridBagLayout());
        questionEditorPanel.setBorder(BorderFactory.createTitledBorder("Редактор вопроса"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5); // Отступы ячеек
        gbc.anchor = GridBagConstraints.WEST; // Выравнивание по левому краю

        // Поля редактора
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.NORTHEAST; // Метка справа вверху
        questionEditorPanel.add(new JLabel("Вопрос:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 1.0; // Растягиваем текстовую область
        gbc.fill = GridBagConstraints.BOTH; gbc.anchor = GridBagConstraints.WEST;
        questionEditorPanel.add(new JScrollPane(questionTextArea), gbc); // TextArea в JScrollPane

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 1; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        questionEditorPanel.add(new JLabel("Ответ:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST;
        questionEditorPanel.add(answerField, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        questionEditorPanel.add(new JLabel("Баллы:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 0.0; // Не растягивать спиннер
        questionEditorPanel.add(pointsSpinner, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST;
        questionEditorPanel.add(new JLabel("Тип:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        questionEditorPanel.add(typeComboBox, gbc);

        // --- Добавляем ComboBox для SpecialType ---
        gbc.gridy++; gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST;
        questionEditorPanel.add(new JLabel("Спец. тип:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        questionEditorPanel.add(specialTypeComboBox, gbc);
        // --- Конец добавления ---


        gbc.gridy++; gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST;
        questionEditorPanel.add(new JLabel("Файл:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.NONE;
        questionEditorPanel.add(fileChooserButton, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.8; // Даем метке растягиваться
        questionEditorPanel.add(filePathLabel, gbc);

        // Панель кнопок вопроса
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER; gbc.weightx = 0.0;
        gbc.insets = new Insets(10, 5, 5, 5); // Больший отступ сверху
        JPanel questionButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        questionButtonPanel.add(newQuestionButton);
        questionButtonPanel.add(saveQuestionButton);
        questionButtonPanel.add(removeQuestionButton);
        questionEditorPanel.add(questionButtonPanel, gbc);

        questionEditorPanel.setMinimumSize(new Dimension(400, 300)); // Мин. размер для редактора
        splitPane.setBottomComponent(new JScrollPane(questionEditorPanel)); // Оборачиваем редактор в скролл на всякий случай

        questionsAndEditorPanel.add(splitPane, BorderLayout.CENTER);
        add(questionsAndEditorPanel, BorderLayout.CENTER); // Добавляем всю центральную часть

        // --- Нижняя панель: Кнопки Назад и Сохранить Пак ---
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bottomPanel.add(backButton, BorderLayout.WEST);
        bottomPanel.add(savePackButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // 3. Добавление Обработчиков Событий
    // В файле: PackCreatorFrame.java

    private void addListeners() {
        // --- Обработчики тем ---
        addThemeButton.addActionListener(e -> addTheme());
        removeThemeButton.addActionListener(e -> removeTheme());
        themeList.addListSelectionListener(e -> {
            // Обрабатываем только окончание выбора
            if (!e.getValueIsAdjusting()) {
                onThemeSelected();
            }
        });

        // --- Обработчики вопросов ---
        newQuestionButton.addActionListener(e -> onNewQuestion());
        saveQuestionButton.addActionListener(e -> saveQuestion());
        removeQuestionButton.addActionListener(e -> removeQuestion());
        questionList.addListSelectionListener(e -> {
            // Обрабатываем только окончание выбора
            if (!e.getValueIsAdjusting()) {
                onQuestionSelected();
            }
        });

        // --- Обработчики редактора ---

        // *** ГЛАВНОЕ ИСПРАВЛЕНИЕ: Добавляем слушателя для typeComboBox ***
        // Слушатель для основного ComboBox типа вопроса (TEXT/IMAGE/AUDIO)
        typeComboBox.addActionListener(e -> {
            // Когда тип меняется, НЕМЕДЛЕННО обновляем состояние кнопки выбора файла
            // и связанной с ней метки.
            updateFileChooserButtonState();
            // Метод updateFileChooserButtonState уже вызывает updateButtonStates() внутри,
            // так что состояние кнопки "Сохранить вопрос" тоже обновится.
            System.out.println("Тип вопроса изменен, вызван updateFileChooserButtonState(). Кнопка файла активна: " + fileChooserButton.isEnabled()); // Отладочный вывод
        });
        // *** КОНЕЦ ИСПРАВЛЕНИЯ ***

        // Слушатель для ComboBox спецтипа (можно оставить пустым или добавить логику)
        specialTypeComboBox.addActionListener(e -> {
        /* Пока ничего не делаем при смене спецтипа, но можно добавить,
           например, проверку на конфликт типов или доп. поля */
            // Важно: Не забыть вызвать updateButtonStates(), если логика здесь
            // может повлиять на возможность сохранения вопроса.
            updateButtonStates(); // Вызываем на случай, если спецтип влияет на валидность
        });

        // Слушатель для кнопки выбора файла
        fileChooserButton.addActionListener(e -> chooseFile());

        // Обновление имени пака при потере фокуса или нажатии Enter
        FocusAdapter packNameUpdater = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent evt) {
                // Вызываем updatePackName ТОЛЬКО если окно все еще активно,
                // чтобы избежать обновления при закрытии окна
                if (PackCreatorFrame.this.isShowing() && PackCreatorFrame.this.isActive()) {
                    updatePackName();
                }
            }
        };
        packNameField.addFocusListener(packNameUpdater);
        packNameField.addActionListener(e -> updatePackName()); // При нажатии Enter

        // Слушатели для активации кнопки "Сохранить вопрос" при изменении полей
        DocumentListener saveButtonEnabler = new DocumentListener() {
            // Вспомогательный метод для вызова обновления состояния кнопок
            private void update() {
                updateButtonStates();
            }
            @Override public void insertUpdate(DocumentEvent e) { update(); }
            @Override public void removeUpdate(DocumentEvent e) { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); } // Для JTextArea не так важно, но пусть будет
        };
        questionTextArea.getDocument().addDocumentListener(saveButtonEnabler);
        answerField.getDocument().addDocumentListener(saveButtonEnabler);

        // Добавляем ChangeListener к JSpinner для баллов
        pointsSpinner.addChangeListener(e -> updateButtonStates());

        // --- Обработчики общих кнопок ---
        savePackButton.addActionListener(e -> savePack());
        // Используем тот же обработчик для кнопки "Назад", что и для закрытия окна ("крестик")
        backButton.addActionListener(e -> handleWindowCloseRequest());
    }

    // 4. Вспомогательные методы и логика UI

    // Метод для обновления имени пакета (в PackCreatorFrame.java)
    private void updatePackName() {
        // Проверяем, что и пак, и поле существуют
        if (currentPack != null && packNameField != null) { // packNameField - поле из PackCreatorFrame
            String newName = packNameField.getText().trim();
            String oldName = currentPack.getPackName();
            if (oldName == null) oldName = "";

            if (!newName.isEmpty()) {
                if (!newName.equals(oldName)) {
                    System.out.println("PackCreatorFrame: Обновление имени пака с '" + oldName + "' на '" + newName + "'");
                    currentPack.setPackName(newName);
                    // После изменения имени пакета в редакторе,
                    // нужно обновить состояние кнопок редактора (напр., "Сохранить пак")
                    updateButtonStates(); // <--- Обновляем кнопки РЕДАКТОРА
                }
            } else {
                packNameField.setText(oldName);
                showErrorDialog("Название пака не может быть пустым.", "Ошибка ввода");
                // Перепроверить кнопки редактора
                updateButtonStates(); // <--- Обновляем кнопки РЕДАКТОРА
            }
        } else {
            System.err.println("Ошибка в updatePackName (PackCreatorFrame): currentPack или packNameField равен null.");
        }
    }

    private void addTheme() {
        String newThemeName = newThemeField.getText().trim();
        if (!newThemeName.isEmpty() && currentPack != null && currentPack.getThemes() != null) {
            // Проверка на дубликат (игнорируя регистр)
            boolean exists = false;
            for (Theme theme : currentPack.getThemes()) {
                if (theme.getThemeName().equalsIgnoreCase(newThemeName)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                Theme newTheme = new Theme(newThemeName);
                newTheme.setQuestions(new ArrayList<>()); // Инициализация списка вопросов
                currentPack.addTheme(newTheme);
                updateThemeList();
                newThemeField.setText("");
                // Выбираем добавленную тему
                themeList.setSelectedIndex(themeListModel.getSize() - 1);
            } else {
                JOptionPane.showMessageDialog(this, "Тема с таким именем уже существует!", "Ошибка", JOptionPane.WARNING_MESSAGE);
            }
        } else if (newThemeName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Название темы не может быть пустым.", "Ошибка", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void removeTheme() {
        if (selectedThemeIndex != -1 && currentPack != null && currentPack.getThemes() != null
                && selectedThemeIndex < currentPack.getThemes().size()) {

            String themeNameToDelete = currentPack.getThemes().get(selectedThemeIndex).getThemeName();
            int choice = JOptionPane.showConfirmDialog(this,
                    "Вы уверены, что хотите удалить тему '" + themeNameToDelete + "' и все ее вопросы?",
                    "Подтверждение удаления", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                currentPack.getThemes().remove(selectedThemeIndex);
                // Сбрасываем индексы и очищаем списки/редактор
                int indexToSelectAfterDelete = Math.max(0, selectedThemeIndex - 1); // Пытаемся выбрать предыдущий
                selectedThemeIndex = -1; // Сбрасываем перед обновлением
                selectedQuestionIndex = -1;
                updateThemeList();
                clearQuestionEditor();
                questionListModel.clear(); // Очищаем список вопросов

                // Восстанавливаем выделение, если список тем не пуст
                if (themeListModel.getSize() > 0) {
                    if (indexToSelectAfterDelete >= themeListModel.getSize()){
                        indexToSelectAfterDelete = themeListModel.getSize() - 1; // Последний
                    }
                    themeList.setSelectedIndex(indexToSelectAfterDelete);
                    themeList.ensureIndexIsVisible(indexToSelectAfterDelete);
                } else {
                    updateButtonStates(); // Обновить состояние кнопок, если тем не осталось
                }
            }
        } else {
            System.err.println("Попытка удалить тему с неверным индексом: " + selectedThemeIndex);
        }
    }

    private void onThemeSelected() {
        int previouslySelectedTheme = selectedThemeIndex;
        selectedThemeIndex = themeList.getSelectedIndex();

        // Обновляем UI только если выбор реально изменился
        if (selectedThemeIndex != previouslySelectedTheme) {
            selectedQuestionIndex = -1; // Сбрасываем выбор вопроса
            updateQuestionList(); // Обновляем список вопросов для новой темы
            clearQuestionEditor(); // Очищаем редактор
            updateButtonStates(); // Обновляем активность кнопок
        }
    }

    private void onQuestionSelected() {
        int previouslySelectedQuestion = selectedQuestionIndex;
        selectedQuestionIndex = questionList.getSelectedIndex();

        if (selectedQuestionIndex != previouslySelectedQuestion) {
            if (selectedQuestionIndex != -1 && selectedThemeIndex != -1
                    && selectedThemeIndex < currentPack.getThemes().size()) {
                try {
                    Theme currentTheme = currentPack.getThemes().get(selectedThemeIndex);
                    if (currentTheme != null && currentTheme.getQuestions() != null && selectedQuestionIndex < currentTheme.getQuestions().size()) {
                        Question selectedQ = currentTheme.getQuestions().get(selectedQuestionIndex);
                        loadQuestionData(selectedQ); // Загружаем данные в редактор
                    } else {
                        throw new IndexOutOfBoundsException("Неверный индекс вопроса или темы при выборе.");
                    }
                } catch (Exception e){
                    System.err.println("Ошибка при выборе вопроса: " + e.getMessage());
                    e.printStackTrace();
                    clearQuestionEditor(); // Очистить редактор при ошибке
                }
            } else {
                // Если выделение снято (индекс -1)
                clearQuestionEditor();
            }
            updateButtonStates();
        }
    }

    private void onNewQuestion() {
        if (selectedThemeIndex != -1) { // Убедимся, что тема выбрана
            questionList.clearSelection(); // Снимаем выделение в списке
            selectedQuestionIndex = -1;    // Сбрасываем индекс
            clearQuestionEditor();       // Очищаем поля редактора
            updateButtonStates();      // Обновляем кнопки
            SwingUtilities.invokeLater(questionTextArea::requestFocusInWindow); // Ставим фокус на поле ввода вопроса
        } else {
            JOptionPane.showMessageDialog(this, "Сначала выберите или создайте тему.", "Информация", JOptionPane.INFORMATION_MESSAGE);
        }
    }


    // Метод saveQuestion (ваш код, выглядит корректно)

    private void saveQuestion() {
        if (selectedThemeIndex == -1 || currentPack == null || currentPack.getThemes() == null
                || selectedThemeIndex >= currentPack.getThemes().size()) {
            JOptionPane.showMessageDialog(this, "Сначала выберите или создайте тему.",
                    "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Получаем данные из редактора
        String questionText = questionTextArea.getText().trim();
        String answerText = answerField.getText().trim();
        int points = (Integer) pointsSpinner.getValue();
        Question.QuestionType type = (Question.QuestionType) typeComboBox.getSelectedItem();
        Question.SpecialType specialType = (Question.SpecialType) specialTypeComboBox.getSelectedItem();

        // --- ИСПРАВЛЕНИЕ: Получаем ПОЛНЫЙ путь ТОЛЬКО из ToolTip ---
        String filePath = filePathLabel.getToolTipText();
        // String displayedFileName = filePathLabel.getText(); // Не используем для проверки пути

        // Валидация базовых полей
        if (questionText.isEmpty() || answerText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Поля 'Вопрос' и 'Ответ' должны быть заполнены.",
                    "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (points <= 0) {
            JOptionPane.showMessageDialog(this, "Количество баллов должно быть положительным.",
                    "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // --- ИСПРАВЛЕНИЕ: Улучшенная проверка файла ---
        // Проверяем файл ТОЛЬКО если тип IMAGE или AUDIO
        if (type == Question.QuestionType.IMAGE || type == Question.QuestionType.AUDIO) {
            // 1. Проверяем, что путь в ToolTip был установлен и не является маркером ошибки
            if (filePath == null || filePath.trim().isEmpty() || filePath.contains("(не найден)")) {
                JOptionPane.showMessageDialog(this,
                        "Для типов IMAGE и AUDIO необходимо выбрать существующий и доступный файл.",
                        "Ошибка", JOptionPane.WARNING_MESSAGE);
                return; // Прерываем сохранение
            }
            // 2. Дополнительная проверка существования файла прямо перед сохранением
            File checkFile = null;
            try {
                checkFile = new File(filePath);
                if (!checkFile.exists() || !checkFile.isFile()) {
                    JOptionPane.showMessageDialog(this,
                            "Выбранный файл ('" + (checkFile != null ? checkFile.getName() : "???") + "') больше не существует или не является файлом.\nПожалуйста, выберите файл заново.",
                            "Ошибка Файла", JOptionPane.WARNING_MESSAGE);
                    return; // Прерываем сохранение
                }
                // Опционально: Проверка прав на чтение, хотя chooseFile уже должен был это сделать
                if (!checkFile.canRead()) {
                    JOptionPane.showMessageDialog(this,
                            "Нет прав на чтение для выбранного файла ('" + checkFile.getName() + "').\nПожалуйста, проверьте права доступа или выберите другой файл.",
                            "Ошибка Доступа", JOptionPane.WARNING_MESSAGE);
                    return; // Прерываем сохранение
                }
            } catch (Exception e) {
                // Обработка ошибок создания File объекта или других проблем с путем
                JOptionPane.showMessageDialog(this,
                        "Произошла ошибка при проверке файла:\n" + e.getMessage(),
                        "Ошибка Файла", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            // Для типа TEXT принудительно обнуляем путь
            filePath = null;
        }
        // --- КОНЕЦ ИСПРАВЛЕНИЙ В ПРОВЕРКЕ ФАЙЛА ---

        // Отладочный вывод (можно закомментировать или удалить)
        System.out.println("Сохранение вопроса. Тип: " + type + ", Путь: '" + filePath + "'");

        // Создаем или обновляем объект Question
        Question question = new Question(questionText, answerText, points, type, filePath, specialType);

        Theme currentTheme = currentPack.getThemes().get(selectedThemeIndex);
        // Обеспечим, что список вопросов существует
        if (currentTheme.getQuestions() == null) {
            currentTheme.setQuestions(new ArrayList<>());
        }

        int indexToSelectAfterSave;

        if (selectedQuestionIndex == -1) {
            // Добавляем новый вопрос
            currentTheme.addQuestion(question);
            indexToSelectAfterSave = currentTheme.getQuestions().size() - 1; // Индекс нового вопроса
            System.out.println("Добавлен новый вопрос в тему '" + currentTheme.getThemeName() + "'");
        } else {
            // Обновляем существующий вопрос
            if (selectedQuestionIndex < currentTheme.getQuestions().size()) {
                currentTheme.getQuestions().set(selectedQuestionIndex, question);
                indexToSelectAfterSave = selectedQuestionIndex; // Оставляем выделение
                System.out.println("Обновлен вопрос [" + selectedThemeIndex + "][" + selectedQuestionIndex + "]");
            } else {
                // Ошибка индекса - добавляем в конец как новый
                System.err.println("Ошибка сохранения: Неверный индекс выбранного вопроса для обновления (" + selectedQuestionIndex + "). Добавление в конец.");
                currentTheme.addQuestion(question);
                indexToSelectAfterSave = currentTheme.getQuestions().size() - 1;
            }
        }

        // Обновляем список и восстанавливаем/устанавливаем выделение
        updateQuestionList(); // Обновит модель списка

        // Устанавливаем выделение на сохраненный/добавленный вопрос
        if (indexToSelectAfterSave >= 0 && indexToSelectAfterSave < questionListModel.getSize()) {
            questionList.setSelectedIndex(indexToSelectAfterSave);
            questionList.ensureIndexIsVisible(indexToSelectAfterSave);
            // Listener обработает выбор и вызовет loadQuestionData, если индекс изменился
            // Если индекс не изменился (было обновление), данные в редакторе уже актуальны
        } else {
            // Если что-то пошло не так с индексами
            System.err.println("Ошибка после сохранения: Не удалось выбрать вопрос с индексом " + indexToSelectAfterSave);
            questionList.clearSelection();
            clearQuestionEditor(); // Очищаем редактор на всякий случай
        }

        updateButtonStates(); // Обновить состояние кнопок
    }


    private void removeQuestion() {
        if (selectedThemeIndex != -1 && selectedQuestionIndex != -1
                && selectedThemeIndex < currentPack.getThemes().size()) {
            Theme currentTheme = currentPack.getThemes().get(selectedThemeIndex);
            if (currentTheme != null && currentTheme.getQuestions() != null && selectedQuestionIndex < currentTheme.getQuestions().size()) {

                String qTextPreview = currentTheme.getQuestions().get(selectedQuestionIndex).getQuestionText();
                if (qTextPreview.length() > 50) qTextPreview = qTextPreview.substring(0, 47) + "...";

                int choice = JOptionPane.showConfirmDialog(this,
                        "Вы уверены, что хотите удалить вопрос:\n\"" + qTextPreview + "\"?",
                        "Подтверждение удаления", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                if (choice == JOptionPane.YES_OPTION) {
                    currentTheme.getQuestions().remove(selectedQuestionIndex);

                    // Сбрасываем выбор и очищаем редактор
                    int indexToSelectAfterDelete = Math.max(0, selectedQuestionIndex - 1);
                    selectedQuestionIndex = -1; // Сброс индекса
                    updateQuestionList();
                    clearQuestionEditor();

                    // Восстанавливаем выделение, если вопросы остались
                    if (questionListModel.getSize() > 0) {
                        if (indexToSelectAfterDelete >= questionListModel.getSize()){
                            indexToSelectAfterDelete = questionListModel.getSize() - 1; // Последний
                        }
                        questionList.setSelectedIndex(indexToSelectAfterDelete);
                        questionList.ensureIndexIsVisible(indexToSelectAfterDelete);
                    } else {
                        updateButtonStates(); // Обновить кнопки, если вопросов не осталось
                    }
                }
            } else {
                System.err.println("Попытка удалить вопрос с неверным индексом вопроса или темы.");
                clearQuestionEditor();
                updateButtonStates();
            }
        }
    }

    private void chooseFile() {
        System.out.println("--- Метод chooseFile() вызван ---"); // Лог старта
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Выберите файл вопроса");

        // Устанавливаем фильтры в зависимости от типа вопроса
        Question.QuestionType selectedType = (Question.QuestionType) typeComboBox.getSelectedItem();

        // Определяем фильтры (как и раньше)
        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter("Images (*.jpg, *.jpeg, *.png, *.gif, *.bmp)", "jpg", "jpeg", "png", "gif", "bmp");
        FileNameExtensionFilter audioFilter = new FileNameExtensionFilter("Audio (*.wav)", "wav"); // Пока только WAV

        // Убираем "Все файлы" и добавляем нужные фильтры
        fileChooser.setAcceptAllFileFilterUsed(false); // Не показывать "Все файлы"
        if (selectedType == Question.QuestionType.IMAGE) {
            fileChooser.addChoosableFileFilter(imageFilter);
            fileChooser.setFileFilter(imageFilter); // Устанавливаем по умолчанию
            System.out.println("Установлен фильтр для IMAGE");
        } else if (selectedType == Question.QuestionType.AUDIO) {
            fileChooser.addChoosableFileFilter(audioFilter);
            fileChooser.setFileFilter(audioFilter); // Устанавливаем по умолчанию
            System.out.println("Установлен фильтр для AUDIO");
        } else {
            // Для ТЕХТ или других типов - не имеет смысла, кнопка должна быть неактивна
            // Но на всякий случай, если метод вызван ошибочно
            System.out.println("Тип вопроса не требует файла, диалог не должен был открыться.");
            // Можно добавить showErrorDialog, но лучше исправить логику вызова
            return; // Не показываем диалог для TEXT
        }

        // Пытаемся открыть в папке текущего файла, если он есть и валиден
        String currentPathTooltip = filePathLabel.getToolTipText();
        System.out.println("Текущий ToolTipText (путь) перед открытием диалога: '" + currentPathTooltip + "'");
        File initialDirectory = null;
        if (currentPathTooltip != null && !currentPathTooltip.trim().isEmpty() && !currentPathTooltip.contains("(не найден)")) {
            try {
                File currentFile = new File(currentPathTooltip);
                if (currentFile.exists()) {
                    initialDirectory = currentFile.isFile() ? currentFile.getParentFile() : currentFile;
                } else if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
                    // Если сам файл не найден, но папка существует
                    initialDirectory = currentFile.getParentFile();
                }
                if (initialDirectory != null && initialDirectory.isDirectory()) {
                    fileChooser.setCurrentDirectory(initialDirectory);
                    System.out.println("Диалог откроется в директории: " + initialDirectory.getAbsolutePath());
                } else {
                    System.out.println("Не удалось определить начальную директорию из текущего пути, используем директорию паков.");
                    initialDirectory = null; // Сброс, чтобы использовать packsDir ниже
                }
            } catch (Exception e) {
                System.err.println("Ошибка при установке начальной директории из '" + currentPathTooltip + "': " + e.getMessage());
                initialDirectory = null; // Сброс при ошибке
            }
        }

        // Если не удалось установить из ToolTip, используем директорию паков
        if (initialDirectory == null) {
            try {
                Path packsDir = PackStorage.getDefaultPacksDirectory();
                if (Files.exists(packsDir) && Files.isDirectory(packsDir)) {
                    fileChooser.setCurrentDirectory(packsDir.toFile());
                    System.out.println("Диалог откроется в директории паков по умолчанию: " + packsDir.toAbsolutePath());
                } else {
                    System.out.println("Директория паков по умолчанию не найдена.");
                }
            } catch (Exception ignore) {
                System.err.println("Ошибка доступа к директории паков по умолчанию.");
                // Оставит директорию по умолчанию для JFileChooser (обычно "Мои Документы")
            }
        }


        // Показываем диалог выбора файла
        int result = fileChooser.showOpenDialog(this);

        // Обрабатываем результат
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String absolutePath = selectedFile.getAbsolutePath(); // Получаем абсолютный путь
            String fileName = selectedFile.getName();

            System.out.println("Пользователь выбрал файл: " + absolutePath);

            if (selectedFile.exists() && selectedFile.isFile() && selectedFile.canRead()) {
                // Файл выбран, существует, это файл, и его можно читать
                System.out.println("Установка текста метки: '" + fileName + "'");
                filePathLabel.setText(fileName);

                // ---> САМОЕ ВАЖНОЕ <---
                System.out.println("Установка ToolTipText (полный путь): '" + absolutePath + "'");
                filePathLabel.setToolTipText(absolutePath); // Устанавливаем ПОЛНЫЙ АБСОЛЮТНЫЙ ПУТЬ в ToolTip

                // Проверка сразу после установки (для отладки)
                String checkTooltip = filePathLabel.getToolTipText();
                System.out.println("Проверка ToolTipText сразу после установки: '" + checkTooltip + "'");
                if (!Objects.equals(absolutePath, checkTooltip)) {
                    System.err.println("!!! ОШИБКА: ToolTipText не установился правильно! !!!");
                }
                // --- КОНЕЦ САМОГО ВАЖНОГО ---

                filePathLabel.setForeground(UIManager.getColor("Label.foreground")); // Стандартный цвет
                System.out.println("Метка файла обновлена успешно.");

            } else {
                // Файл не существует, или это папка, или нет прав на чтение
                String issue = !selectedFile.exists() ? "не существует" :
                        !selectedFile.isFile() ? "не является файлом" :
                                "недоступен для чтения";
                System.err.println("Выбранный файл имеет проблему: " + issue);

                filePathLabel.setText("Ошибка файла!");
                // ---> Важно: очищаем ToolTip или ставим маркер ошибки <---
                filePathLabel.setToolTipText(absolutePath + " (" + issue + ")"); // Сохраняем путь с проблемой
                // Или filePathLabel.setToolTipText(null); // Полностью очистить
                filePathLabel.setForeground(Color.RED); // Цвет ошибки

                JOptionPane.showMessageDialog(this,
                        "Выбранный файл " + issue + ".\nПожалуйста, выберите другой файл.",
                        "Ошибка Файла",
                        JOptionPane.ERROR_MESSAGE);
            }
            // Обновляем состояние кнопки "Сохранить вопрос" после выбора/ошибки файла
            updateButtonStates();
        } else {
            System.out.println("Выбор файла отменен пользователем.");
        }
        System.out.println("--- Метод chooseFile() завершен ---"); // Лог конца
    }

    // Сохраняет весь пак в файл JSON
    private void savePack() {
        System.out.println("Запрос на сохранение пакета...");

        if (currentPack == null) {
            System.err.println("Ошибка сохранения: currentPack is null");
            showErrorDialog("Внутренняя ошибка: текущий пакет не инициализирован.", "Ошибка сохранения");
            return;
        }

        // 1. Обновляем имя пака из поля ввода перед сохранением
        updatePackName(); // Убедимся, что имя в currentPack актуально

        // 2. Валидация содержимого пакета перед сохранением
        try {
            validatePackContentForSave(currentPack); // Используем метод валидации
        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка валидации перед сохранением: " + e.getMessage());
            showErrorDialog("Ошибка в структуре пакета:\n" + e.getMessage() +
                    "\n\nПожалуйста, исправьте ошибки перед сохранением.", "Ошибка сохранения");
            // TODO: Выделить проблемную тему/вопрос в UI, если возможно
            return; // Не сохраняем, если есть ошибки
        }

        // 3. Диалог сохранения файла
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить пак как...");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Пакеты Своей Игры (*.json)", "json"));

        // 4. Предлагаем имя файла и директорию
        File suggestedFile = null;
        if (editingFilePath != null) {
            // Если редактировали существующий файл, предлагаем его же
            suggestedFile = editingFilePath.toFile();
            // Установить начальную директорию, если возможно
            if (editingFilePath.getParent() != null && Files.isDirectory(editingFilePath.getParent())) {
                fileChooser.setCurrentDirectory(editingFilePath.getParent().toFile());
            } else { // Если родительская папка недоступна, используем папку паков по умолчанию
                try {
                    fileChooser.setCurrentDirectory(PackStorage.getDefaultPacksDirectory().toFile());
                } catch (Exception ignore) { /* Игнорируем ошибку доступа */}
            }
        } else {
            // Если новый пак, предлагаем имя из packNameField в директории паков по умолчанию
            String suggestedFileNameBase = currentPack.getPackName()
                    .replaceAll("[\\\\/:*?\"<>|]", "_") // Заменяем недопустимые символы
                    .replaceAll("[\\s.]+$", "") // Убираем точки и пробелы в конце
                    .trim();
            if (suggestedFileNameBase.isEmpty()) suggestedFileNameBase = "Новый Пак"; // Имя по умолчанию, если пусто

            try { // Получить директорию по умолчанию
                Path defaultDir = PackStorage.getDefaultPacksDirectory();
                // Убедимся, что директория существует или может быть создана
                if (!Files.exists(defaultDir)) {
                    try {
                        Files.createDirectories(defaultDir);
                    } catch (IOException | SecurityException createEx) {
                        System.err.println("Не удалось создать директорию паков по умолчанию: " + createEx.getMessage());
                        // Предложим сохранить в текущей директории пользователя
                        defaultDir = Paths.get(System.getProperty("user.dir"));
                    }
                }
                if (Files.isDirectory(defaultDir)) {
                    fileChooser.setCurrentDirectory(defaultDir.toFile());
                }
                suggestedFile = new File(defaultDir.toFile(), suggestedFileNameBase + ".json");
            } catch (Exception e) { // На случай других ошибок доступа
                System.err.println("Не удалось получить/использовать директорию паков по умолчанию: " + e.getMessage());
                suggestedFile = new File(suggestedFileNameBase + ".json"); // Предлагаем в текущей директории
            }
        }
        // Устанавливаем предложенное имя файла
        if (suggestedFile != null) {
            fileChooser.setSelectedFile(suggestedFile);
        }

        // 5. Показываем диалог сохранения
        int userSelection = fileChooser.showSaveDialog(this);

        // 6. Обработка выбора пользователя
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            Path filePath = fileChooser.getSelectedFile().toPath();

            // Убедимся, что расширение .json (добавляем, если нет)
            String fileNameStr = filePath.toString();
            if (!fileNameStr.toLowerCase().endsWith(".json")) {
                filePath = Paths.get(fileNameStr + ".json");
                System.out.println("Добавлено расширение .json. Итоговый путь: " + filePath);
            }

            // 7. Проверка перезаписи существующего файла
            if (Files.exists(filePath)) {
                int overwriteChoice = JOptionPane.showConfirmDialog(this,
                        "Файл '" + filePath.getFileName() + "' уже существует.\nПерезаписать?",
                        "Подтверждение перезаписи",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE); // Предупреждение о перезаписи

                if (overwriteChoice != JOptionPane.YES_OPTION) {
                    System.out.println("Сохранение отменено пользователем (отказ от перезаписи).");
                    return; // Отмена сохранения
                }
                System.out.println("Пользователь подтвердил перезапись файла: " + filePath);
            }

            // 8. Непосредственно сохранение с помощью PackStorage
            try {
                System.out.println("Попытка сохранения пакета в: " + filePath.toAbsolutePath());
                PackStorage.savePack(currentPack, filePath); // Вызов метода сохранения

                System.out.println("Пакет успешно сохранен!");
                JOptionPane.showMessageDialog(this,
                        "Пакет успешно сохранен!\n" + filePath.toAbsolutePath(),
                        "Сохранение завершено",
                        JOptionPane.INFORMATION_MESSAGE);

                // Если сохранили успешно, обновляем editingFilePath (важно для последующих сохранений)
                this.editingFilePath = filePath;
                setTitle("Редактирование Пака: " + filePath.getFileName()); // Обновить заголовок окна

                // Опционально: можно обновить состояние кнопок, если это необходимо
                updateButtonStates();

            } catch (IOException | SecurityException ioEx) {
                System.err.println("Ошибка ввода-вывода или безопасности при сохранении пакета: " + ioEx.getMessage());
                ioEx.printStackTrace();
                showErrorDialog("Ошибка при сохранении пака:\n" + ioEx.getMessage() +
                        "\n\nПроверьте права доступа к папке.", "Ошибка сохранения");
            } catch (Exception ex) { // Ловим другие возможные ошибки (напр., от Gson)
                System.err.println("Непредвиденная ошибка при сохранении пакета: " + ex.getMessage());
                ex.printStackTrace();
                showErrorDialog("Произошла непредвиденная ошибка при сохранении пака:\n" + ex.getMessage(),
                        "Ошибка сохранения");
            }
        } else {
            System.out.println("Сохранение пакета отменено пользователем (диалог закрыт).");
        }
    }

    // Валидация содержимого перед сохранением (проще, чем при старте игры)
    private void validatePackContentForSave(QuestionPack pack) throws IllegalArgumentException {
        if (pack.getThemes() == null || pack.getThemes().isEmpty()) {
            throw new IllegalArgumentException("Пак не содержит тем.");
        }
        boolean hasQuestionsOverall = false;
        for (int i = 0; i < pack.getThemes().size(); i++) {
            Theme theme = pack.getThemes().get(i);
            if (theme == null || theme.getThemeName() == null || theme.getThemeName().trim().isEmpty()) {
                throw new IllegalArgumentException("Обнаружена тема без названия (индекс " + i + ").");
            }
            if (theme.getQuestions() == null || theme.getQuestions().isEmpty()) {
                // Предупреждение, но не ошибка сохранения, если есть другие темы с вопросами
                System.out.println("Предупреждение: Тема '" + theme.getThemeName() + "' не содержит вопросов.");
                continue; // Проверяем дальше
            }
            hasQuestionsOverall = true; // Найден хотя бы один вопрос в пакете
            for (int j = 0; j < theme.getQuestions().size(); j++){
                Question q = theme.getQuestions().get(j);
                if (q == null || q.getQuestionText() == null || q.getQuestionText().trim().isEmpty()
                        || q.getAnswerText() == null || q.getAnswerText().trim().isEmpty()) {
                    throw new IllegalArgumentException("В теме '" + theme.getThemeName() + "' обнаружен вопрос №"+(j+1)+" без текста или ответа.");
                }
                if (q.getPoints() <= 0) {
                    throw new IllegalArgumentException("В теме '" + theme.getThemeName() + "' вопрос №"+(j+1)+" имеет некорректное количество баллов ("+q.getPoints()+").");
                }
                // Доп. проверки на файл? Пока не делаем при сохранении.
            }
        }
        if (!hasQuestionsOverall) {
            throw new IllegalArgumentException("Пак не содержит ни одного вопроса ни в одной теме.");
        }
    }


    // Обновляет список тем в JList
    private void updateThemeList() {
        themeListModel.clear();
        if (currentPack != null && currentPack.getThemes() != null) {
            for (Theme theme : currentPack.getThemes()) {
                themeListModel.addElement(theme.getThemeName());
            }
        }
        // Сброс выделения, если индекс стал невалидным
        if (selectedThemeIndex >= themeListModel.getSize()){
            themeList.clearSelection(); // Listener обработает и сбросит selectedThemeIndex
        }
        updateButtonStates(); // Обновляем кнопки тем
    }

    // Обновляет список вопросов в JList для выбранной темы
    private void updateQuestionList() {
        questionListModel.clear();
        if (selectedThemeIndex != -1 && currentPack != null && currentPack.getThemes() != null
                && selectedThemeIndex < currentPack.getThemes().size()) {
            Theme selectedTheme = currentPack.getThemes().get(selectedThemeIndex);
            if (selectedTheme != null && selectedTheme.getQuestions() != null) {
                for (Question q : selectedTheme.getQuestions()) {
                    if (q != null) { // Проверка на null
                        String qText = q.getQuestionText() != null ? q.getQuestionText() : "?";
                        if (qText.length() > 40) qText = qText.substring(0, 37) + "...";
                        questionListModel.addElement(q.getPoints() + ": " + qText);
                    }
                }
            }
        }
        // Сброс выделения, если индекс стал невалидным
        if (selectedQuestionIndex >= questionListModel.getSize()){
            questionList.clearSelection(); // Listener обработает и сбросит selectedQuestionIndex
        } else if (selectedQuestionIndex != -1){
            // Восстановить выделение, если оно было и осталось валидным
            questionList.setSelectedIndex(selectedQuestionIndex);
            questionList.ensureIndexIsVisible(selectedQuestionIndex);
        }
        updateButtonStates(); // Обновляем кнопки вопросов
    }

    // Очищает редактор вопроса до состояния по умолчанию
    private void clearQuestionEditor() {
        questionTextArea.setText("");
        answerField.setText("");
        pointsSpinner.setValue(100); // Устанавливаем значение по умолчанию
        typeComboBox.setSelectedItem(Question.QuestionType.TEXT); // Тип по умолчанию TEXT
        specialTypeComboBox.setSelectedItem(Question.SpecialType.NONE); // Спецтип по умолчанию NONE

        // Сброс метки файла
        filePathLabel.setText("Файл не выбран"); // Начальное состояние, когда файл может понадобиться
        filePathLabel.setToolTipText(null);      // !!! Важно: Обнуляем ToolTip
        filePathLabel.setForeground(UIManager.getColor("Label.foreground")); // Сброс цвета метки

        // Обновляем состояние кнопки выбора файла (станет неактивной, т.к. тип TEXT)
        updateFileChooserButtonState();
        // Обновляем состояние остальных кнопок редактора
        updateButtonStates();
    }

    // Загружает данные выбранного вопроса в редактор
    private void loadQuestionData(Question q) {
        if (q == null) {
            clearQuestionEditor(); // Просто очищаем, если вопроса нет
            return;
        }

        // Загружаем основные данные
        questionTextArea.setText(q.getQuestionText() != null ? q.getQuestionText() : "");
        answerField.setText(q.getAnswerText() != null ? q.getAnswerText() : "");
        pointsSpinner.setValue(q.getPoints() > 0 ? q.getPoints() : 100); // Устанавливаем значение (с проверкой)
        typeComboBox.setSelectedItem(q.getType() != null ? q.getType() : Question.QuestionType.TEXT);
        specialTypeComboBox.setSelectedItem(q.getSpecialType() != null ? q.getSpecialType() : Question.SpecialType.NONE); // Загрузка спецтипа

        // Обработка пути к файлу
        String path = q.getFilePath();
        Question.QuestionType type = q.getType(); // Используем тип из загруженного вопроса

        // Сначала сбрасываем состояние файла
        filePathLabel.setText("Файл не требуется");
        filePathLabel.setToolTipText(null); // !!! Важно: Обнуляем ToolTip по умолчанию
        filePathLabel.setForeground(UIManager.getColor("Label.foreground"));
        boolean fileNeeded = false; // Флаг, нужен ли файл для этого типа

        if ((type == Question.QuestionType.IMAGE || type == Question.QuestionType.AUDIO)) {
            fileNeeded = true; // Устанавливаем флаг, что файл нужен
            if (path != null && !path.trim().isEmpty()) {
                // Если путь указан для медиа-типа, проверяем его
                File f = null;
                boolean fileOk = false;
                try {
                    f = new File(path);
                    if (f.exists() && f.isFile() && f.canRead()) {
                        // Файл найден и валиден
                        filePathLabel.setText(f.getName());
                        filePathLabel.setToolTipText(f.getAbsolutePath()); // !!! Устанавливаем полный путь
                        fileOk = true;
                        System.out.println("Загружен путь для медиа: " + f.getAbsolutePath());
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка при проверке файла '" + path + "': " + e.getMessage());
                    // Оставляем fileOk = false
                }

                if (!fileOk) {
                    // Файл по указанному пути не найден или не валиден
                    filePathLabel.setText("Файл не найден!");
                    filePathLabel.setToolTipText(path + " (не найден)"); // Сохраняем путь с проблемой
                    filePathLabel.setForeground(Color.RED);
                    System.err.println("Файл для вопроса не найден или невалиден: " + path);
                }
            } else {
                // Путь не указан для медиа-типа
                filePathLabel.setText("Файл не выбран"); // Ставим "не выбран", т.к. тип требует файл
                filePathLabel.setToolTipText(null); // ToolTip пуст
                System.out.println("Путь к медиа файлу не указан в вопросе.");
            }
        } else {
            // Для типа TEXT файл не требуется
            System.out.println("Тип вопроса TEXT, файл не требуется.");
            // filePathLabel уже установлен в "Файл не требуется" и ToolTip в null
        }

        // Обновляем состояние кнопки выбора файла ПОСЛЕ определения типа
        updateFileChooserButtonState();
        // Обновляем состояние остальных кнопок редактора
        updateButtonStates();
        // Ставим фокус для удобства редактирования
        SwingUtilities.invokeLater(questionTextArea::requestFocusInWindow);
    }

    // Обновляет состояние активности кнопки выбора файла и сбрасывает файл, если тип TEXT
    private void updateFileChooserButtonState() {
        if(typeComboBox == null || fileChooserButton == null || filePathLabel == null) return; // Защита от NPE при инициализации

        Question.QuestionType selectedType = (Question.QuestionType) typeComboBox.getSelectedItem();
        boolean needsFile = (selectedType == Question.QuestionType.IMAGE || selectedType == Question.QuestionType.AUDIO);

        fileChooserButton.setEnabled(needsFile);

        // Если файл НЕ нужен (тип TEXT или другой)
        if (!needsFile) {
            // И если метка сейчас НЕ "Файл не требуется", то сбросить её
            if (!"Файл не требуется".equals(filePathLabel.getText())) {
                System.out.println("Тип изменен на немедийный, сброс метки файла.");
                filePathLabel.setText("Файл не требуется");
                filePathLabel.setToolTipText(null); // !!! Важно: Обнуляем ToolTip
                filePathLabel.setForeground(UIManager.getColor("Label.foreground"));
            }
        } else {
            // Если файл НУЖЕН, но метка сейчас "Файл не требуется" (например, переключились с TEXT)
            // то меняем её на "Файл не выбран", чтобы показать, что выбор ожидается.
            if ("Файл не требуется".equals(filePathLabel.getText())) {
                System.out.println("Тип изменен на медийный, установка метки 'Файл не выбран'.");
                filePathLabel.setText("Файл не выбран");
                filePathLabel.setToolTipText(null); // ToolTip пока пуст
            }
            // Цвет не меняем, он должен быть стандартным или красным (если была ошибка)
        }

        // Состояние кнопки "Сохранить" может измениться, так как требование файла влияет
        updateButtonStates();
    }

    // Обновляет состояние активности всех кнопок
    private void updateButtonStates() {
        boolean themeSelected = selectedThemeIndex != -1;
        boolean questionSelected = selectedQuestionIndex != -1;

        removeThemeButton.setEnabled(themeSelected);
        newQuestionButton.setEnabled(themeSelected); // Можно создать вопрос, если выбрана тема

        // Кнопка "Сохранить вопрос" активна, если:
        // 1. Выбрана тема
        // 2. Заполнены поля "Вопрос" и "Ответ"
        // 3. Если тип IMAGE/AUDIO, то выбран корректный файл
        boolean canSaveQuestion = themeSelected
                && !questionTextArea.getText().trim().isEmpty()
                && !answerField.getText().trim().isEmpty();
        if (canSaveQuestion) {
            Question.QuestionType type = (Question.QuestionType) typeComboBox.getSelectedItem();
            if ((type == Question.QuestionType.IMAGE || type == Question.QuestionType.AUDIO)) {
                String labelText = filePathLabel.getText();
                if (labelText.equals("Файл не выбран") || labelText.equals("Файл не найден!") || labelText.equals("Файл не требуется")) {
                    canSaveQuestion = false;
                }
            }
        }
        saveQuestionButton.setEnabled(canSaveQuestion);

        removeQuestionButton.setEnabled(themeSelected && questionSelected); // Можно удалить, только если выбран конкретный вопрос

        // Кнопка "Сохранить пак" активна, если есть имя пака и хотя бы одна тема
        boolean canSavePack = currentPack != null
                && currentPack.getPackName() != null && !currentPack.getPackName().trim().isEmpty()
                && currentPack.getThemes() != null && !currentPack.getThemes().isEmpty();
        savePackButton.setEnabled(canSavePack);
    }


    // Обработчик закрытия окна (крестик и кнопка "Назад")
    private void handleWindowCloseRequest() {
        // TODO: Добавить проверку на несохраненные изменения
        int choice = JOptionPane.showConfirmDialog(this,
                "Вы уверены, что хотите закрыть редактор?\nНесохраненные изменения в пакете будут потеряны.",
                "Подтверждение выхода",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            dispose(); // Закрыть окно редактора
            if (parentFrame != null) {
                parentFrame.setVisible(true); // Показать родительское окно (главное меню)
            }
        }
    }
    private void showErrorDialog(String message, String title, int messageType) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, title, messageType);
        });
    }

    // Существующий метод для ошибок (можно оставить или удалить, если все вызовы заменены)
    private void showErrorDialog(String message, String title) {
        showErrorDialog(message, title, JOptionPane.ERROR_MESSAGE); // Вызываем перегруженный с типом ERROR
    }

    /**
     * Заполняет компоненты UI данными из текущего пакета (currentPack).
     * Вызывается после загрузки пакета для редактирования.
     */
    private void populateUIFromCurrentPack() {
        if (currentPack == null) return;

        System.out.println("Заполнение UI данными из загруженного пакета...");

        // 1. Имя пакета
        packNameField.setText(currentPack.getPackName() != null ? currentPack.getPackName() : "");

        // 2. Список тем
        updateThemeList(); // Этот метод уже должен использовать currentPack

        // 3. Очистка редактора и списка вопросов (на случай, если там что-то было)
        clearQuestionEditor();
        if (questionListModel != null) {
            questionListModel.clear();
        }
        selectedThemeIndex = -1;
        selectedQuestionIndex = -1;

        // Опционально: выбрать первую тему, если она есть
        if (themeListModel != null && !themeListModel.isEmpty()) {
            themeList.setSelectedIndex(0);
            // onThemeSelected() будет вызван автоматически и заполнит список вопросов
        }
        System.out.println("Заполнение UI завершено.");
    }
}
