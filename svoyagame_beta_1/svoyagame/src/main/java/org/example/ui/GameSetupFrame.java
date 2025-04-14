package org.example.ui;

import org.example.data.*; // Импорт моделей данных
import org.example.game.GameController;
import org.example.data.PackStorage; // Явный импорт

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GameSetupFrame extends JFrame {

    private final JFrame parentFrame; // Ссылка на главное меню для возврата

    // Константы
    private static final String PLACEHOLDER_PACK = "--- Выберите пак ---";

    // --- Компоненты UI: Настройка Пака и Игроков ---
    private JComboBox<String> packComboBox;
    private JButton editPackButton;   // <-- Новая кнопка
    private JButton deletePackButton; // <-- Новая кнопка
    private JSpinner playerCountSpinner;
    private JPanel playerNamesPanel; // Панель для динамического добавления полей имен

    // --- Компоненты UI: Настройка Раундов (Только количество) ---
    private JSpinner roundsCountSpinner;
    // private JRadioButton randomDistributionRadio; // УДАЛЕНО
    private JPanel roundSetupPanel; // Панель с CardLayout для отображения инфо/ошибки
    private JLabel themesPerRoundLabel; // Метка для информации о количестве тем на раунд
    private JPanel currentRandomPanel = null; // Панель для отображения информации о случайном распределении

    // --- Компоненты UI: Настройка Спецвопросов ---
    private JSpinner catsCountSpinner;
    private JSpinner auctionsCountSpinner;
    private JLabel maxSpecialsLabel; // Метка, показывающая макс. возможное кол-во спецвопросов

    // --- Компоненты UI: Основные Кнопки ---
    private JButton startGameButton;
    private JButton backButton;

    // --- Данные и Состояние ---
    private List<Path> availablePackPaths; // Список путей к найденным файлам пакетов *.json
    private QuestionPack selectedPack = null; // Пакет, выбранный пользователем
    private List<JTextField> playerNameFields; // Список JTextField для ввода имен игроков

    public GameSetupFrame(JFrame parent) {
        this.parentFrame = parent;
        this.playerNameFields = new ArrayList<>();
        this.availablePackPaths = new ArrayList<>();

        // Базовая настройка окна
        setTitle("Настройка Игры");
        setMinimumSize(new Dimension(600, 500)); // Немного меньше, т.к. ручной настройки нет
        setSize(700, 550);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Обрабатываем закрытие сами

        // Перехват закрытия окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                goBackToParent(); // Возврат в главное меню
            }
            // Опционально: вернуть видимость главного меню, если оно было скрыто
            @Override
            public void windowClosed(WindowEvent e) {
                if (parentFrame != null && !parentFrame.isVisible()) {
                    // parentFrame.setVisible(true); // Решено делать это в goBackToParent
                }
            }
        });

        // Инициализация и размещение
        initComponents();
        layoutComponents();
        addListeners();

        // Начальная загрузка и состояние
        loadAvailablePacks();
        updatePlayerNameFields(); // Создаем начальные поля имен
        checkStartButtonState(); // Проверяем кнопку старт (будет неактивна)

        System.out.println("GameSetupFrame инициализирован.");
    }

    // --- 1. ИНИЦИАЛИЗАЦИЯ КОМПОНЕНТОВ ---
    private void initComponents() {
        System.out.println("Инициализация компонентов GameSetupFrame...");

        // 1. Пакеты
        packComboBox = new JComboBox<>();
        packComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        packComboBox.addItem(PLACEHOLDER_PACK); // Добавляем плейсхолдер

        editPackButton = new JButton("Редакт.");
        editPackButton.setToolTipText("Редактировать выбранный пакет");
        editPackButton.setFont(new Font("Arial", Font.PLAIN, 10)); // Маленький шрифт
        editPackButton.setMargin(new Insets(1, 2, 1, 2));      // Меньше отступы
        editPackButton.setEnabled(false); // Изначально неактивна

        deletePackButton = new JButton("Удалить");
        deletePackButton.setToolTipText("Удалить выбранный пакет");
        deletePackButton.setFont(new Font("Arial", Font.PLAIN, 10));
        deletePackButton.setMargin(new Insets(1, 2, 1, 2));
        deletePackButton.setEnabled(false); // Изначально неактивна

        // 2. Игроки
        playerCountSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 8, 1)); // Нач=2, Мин=1, Макс=8, Шаг=1
        playerCountSpinner.setFont(new Font("Arial", Font.PLAIN, 14));
        Dimension spinnerSize = new Dimension(60, playerCountSpinner.getPreferredSize().height);
        playerCountSpinner.setPreferredSize(spinnerSize);
        playerCountSpinner.setMaximumSize(spinnerSize);

        playerNamesPanel = new JPanel();
        playerNamesPanel.setLayout(new BoxLayout(playerNamesPanel, BoxLayout.Y_AXIS));
        // Рамка будет добавлена к JScrollPane в layoutComponents

        // 3. Раунды (только количество и информация)
        // Модель: Нач=1, Мин=1, Макс=1(пока), Шаг=1. Максимум обновится после выбора пака.
        roundsCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
        roundsCountSpinner.setFont(new Font("Arial", Font.PLAIN, 14));
        roundsCountSpinner.setPreferredSize(spinnerSize);
        roundsCountSpinner.setMaximumSize(spinnerSize);
        roundsCountSpinner.setEnabled(false); // Активируется после выбора пака

        // Метка с информацией о темах на раунд
        themesPerRoundLabel = new JLabel("Тем в раунде: N/A");
        themesPerRoundLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        themesPerRoundLabel.setForeground(Color.GRAY);

        // Панель для отображения информации о распределении (используем CardLayout)
        roundSetupPanel = new JPanel(new CardLayout(5, 5));

        // Карта "EMPTY" (когда пак не выбран или тем мало)
        JLabel emptyRoundLabel = new JLabel("Выберите пакет для настройки раундов.", SwingConstants.CENTER);
        emptyRoundLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        emptyRoundLabel.setForeground(Color.DARK_GRAY);
        roundSetupPanel.add(emptyRoundLabel, "EMPTY");

        // Карта "RANDOM_INFO" (когда все хорошо для случайного распределения)
        currentRandomPanel = new JPanel(new BorderLayout());
        JLabel randomInfoLabel = new JLabel("Темы будут распределены случайно по раундам.", SwingConstants.CENTER);
        randomInfoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        currentRandomPanel.add(randomInfoLabel, BorderLayout.CENTER);
        roundSetupPanel.add(currentRandomPanel, "RANDOM_INFO");

        // 4. Коты/Аукционы
        catsCountSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1)); // Нач=0, Мин=0, Макс=0(пока), Шаг=1
        auctionsCountSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
        catsCountSpinner.setFont(new Font("Arial", Font.PLAIN, 14));
        auctionsCountSpinner.setFont(new Font("Arial", Font.PLAIN, 14));
        catsCountSpinner.setPreferredSize(spinnerSize);
        auctionsCountSpinner.setPreferredSize(spinnerSize);
        catsCountSpinner.setMaximumSize(spinnerSize);
        auctionsCountSpinner.setMaximumSize(spinnerSize);
        catsCountSpinner.setEnabled(false); // Активируются после выбора пака
        auctionsCountSpinner.setEnabled(false);

        maxSpecialsLabel = new JLabel("Макс. спец.: 0");
        maxSpecialsLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        maxSpecialsLabel.setForeground(Color.GRAY);

        // 5. Основные Кнопки
        startGameButton = new JButton("Начать игру!");
        startGameButton.setFont(new Font("Arial", Font.BOLD, 14));
        startGameButton.setEnabled(false); // Изначально неактивна

        backButton = new JButton("Назад");
        backButton.setFont(new Font("Arial", Font.PLAIN, 14));

        System.out.println("Инициализация компонентов GameSetupFrame завершена.");
    }

    // --- 2. РАЗМЕЩЕНИЕ КОМПОНЕНТОВ ---
    private void layoutComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10); // Отступы
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Ряд 0: Пакет вопросов
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST;
        add(new JLabel("Пакет вопросов:"), gbc);

        // Используем панель для ComboBox и кнопок
        JPanel packSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); // Уменьшим отступы
        packSelectionPanel.add(packComboBox);
        packSelectionPanel.add(editPackButton);
        packSelectionPanel.add(deletePackButton);

        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        add(packSelectionPanel, gbc);
        gbc.gridwidth = 1; // Сброс
        // Ряд 1: Количество игроков
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; add(new JLabel("Количество игроков:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.NONE; add(playerCountSpinner, gbc);
        // Остаток ряда пуст

        // Ряд 2: Имена игроков
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4; gbc.weighty = 0.3; // Даем вес для растягивания
        gbc.fill = GridBagConstraints.BOTH;
        JScrollPane scrollPane = new JScrollPane(playerNamesPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Имена игроков"));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(300, 100)); // Предпочтительный размер
        add(scrollPane, gbc);
        gbc.weighty = 0.0; // Сброс веса

        // Ряд 3: Количество раундов
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        add(new JLabel("Количество раундов:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; add(roundsCountSpinner, gbc);
        // Радио-кнопки удалены

        // Ряд 4: Спецвопросы
        gbc.gridx = 0; gbc.gridy = 4; gbc.anchor = GridBagConstraints.EAST; add(new JLabel("Кол-во 'Котов':"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; add(catsCountSpinner, gbc);
        gbc.gridx = 2; gbc.anchor = GridBagConstraints.EAST; add(new JLabel("Кол-во 'Аукционов':"), gbc);
        gbc.gridx = 3; gbc.anchor = GridBagConstraints.WEST;
        // Панель для спиннера аукционов и метки максимума
        JPanel specialPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        specialPanel.add(auctionsCountSpinner);
        specialPanel.add(Box.createHorizontalStrut(10));
        specialPanel.add(maxSpecialsLabel);
        add(specialPanel, gbc);


        // Ряд 5: Панель настройки раундов (теперь только информация)
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 4; gbc.weightx = 1.0; gbc.weighty = 0.7; // Даем больше веса
        gbc.fill = GridBagConstraints.BOTH;
        JPanel roundDetailPanel = new JPanel(new BorderLayout(5,5));
        roundDetailPanel.setBorder(BorderFactory.createTitledBorder("Распределение тем по раундам"));
        roundDetailPanel.add(themesPerRoundLabel, BorderLayout.NORTH); // Метка сверху
        roundDetailPanel.add(roundSetupPanel, BorderLayout.CENTER); // Панель с CardLayout
        add(roundDetailPanel, gbc);


        // Ряд 6: Кнопки Назад и Старт
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 1; gbc.weightx = 0.0; gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.SOUTHWEST;
        add(backButton, gbc);

        gbc.gridx = 3; gbc.anchor = GridBagConstraints.SOUTHEAST;
        add(startGameButton, gbc);

        System.out.println("Размещение компонентов GameSetupFrame завершено.");
    }

    // --- 3. ДОБАВЛЕНИЕ СЛУШАТЕЛЕЙ ---
    private void addListeners() {
        // Кнопка "Назад"
        backButton.addActionListener(e -> goBackToParent());
        editPackButton.addActionListener(e -> editSelectedPack());
        deletePackButton.addActionListener(e -> deleteSelectedPack());

        // Изменение количества игроков
        playerCountSpinner.addChangeListener(e -> {
            updatePlayerNameFields();
            // checkStartButtonState() будет вызван из updatePlayerNameFields
        });

        // Выбор пакета вопросов
        packComboBox.addActionListener(e -> {
            handlePackSelection();
            // checkStartButtonState() и др. обновления будут вызваны из handlePackSelection
        });

        // Общий слушатель для числовых спиннеров настроек (Раунды, Коты, Аукционы)
        ChangeListener numericSettingListener = e -> {
            // Обновляем зависимые компоненты
            if (e.getSource() == catsCountSpinner || e.getSource() == auctionsCountSpinner) {
                updateSpecialSpinnersMaximum(); // Обновить максимум другого спецвопроса
            } else if (e.getSource() == roundsCountSpinner) {
                updateRoundSetupUI(); // Обновить инфо-панель раундов
            }
            // Проверяем состояние кнопки "Старт" после любого изменения
            checkStartButtonState();
        };
        catsCountSpinner.addChangeListener(numericSettingListener);
        auctionsCountSpinner.addChangeListener(numericSettingListener);
        roundsCountSpinner.addChangeListener(numericSettingListener);

        // Кнопка "Начать игру!"
        startGameButton.addActionListener(e -> startGame());

        // Слушатели для полей имен игроков добавляются динамически в addPlayerNameField()

        // Слушатель для поля названия пакета

        System.out.println("Слушатели для GameSetupFrame добавлены.");
    }

    // --- 4. ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ И ЛОГИКА ---

    // --- Загрузка и обработка пакетов ---

    private void loadAvailablePacks() {
        Path packsDir = null; // Путь к директории паков
        boolean directoryError = false; // Флаг ошибки доступа/создания директории

        System.out.println("Загрузка доступных пакетов...");

        // Очистка предыдущих данных перед загрузкой
        availablePackPaths.clear();
        packComboBox.removeAllItems(); // Очищаем ComboBox перед заполнением
        packComboBox.addItem(PLACEHOLDER_PACK); // Добавляем плейсхолдер по умолчанию

        try {
            // 1. Получение пути к директории паков
            packsDir = PackStorage.getDefaultPacksDirectory();
            System.out.println("Используемая директория паков: " + packsDir.toAbsolutePath());

            // 2. Проверка и создание директории, если необходимо
            if (!Files.exists(packsDir)) {
                System.out.println("Директория паков не найдена, попытка создания...");
                try {
                    Files.createDirectories(packsDir);
                    System.out.println("Успешно создана директория: " + packsDir.toAbsolutePath());
                } catch (IOException | SecurityException createEx) {
                    directoryError = true; // Устанавливаем флаг ошибки
                    handleDirectoryError("Не удалось создать директорию для паков", packsDir, createEx);
                    // Не прерываем выполнение, чтобы finally блок отработал
                }
            } else if (!Files.isDirectory(packsDir)) {
                directoryError = true;
                handleDirectoryError("Указанный путь для паков не является директорией", packsDir, null);
            } else if (!Files.isReadable(packsDir)) {
                directoryError = true;
                handleDirectoryError("Нет прав на чтение директории паков", packsDir, null);
            }

            // 3. Чтение списка паков (только если не было ошибок с директорией)
            if (!directoryError) {
                System.out.println("Чтение содержимого директории паков...");
                // PackStorage.listAvailablePacks обрабатывает IOException внутри
                // и возвращает пустой список в случае ошибки чтения
                List<Path> packFiles = PackStorage.listAvailablePacks(packsDir);
                availablePackPaths.addAll(packFiles); // Сохраняем найденные пути
                System.out.println("Найдено пакетов (*.json): " + availablePackPaths.size());

                // 4. Заполнение ComboBox
                if (!availablePackPaths.isEmpty()) {
                    availablePackPaths.forEach(packPath -> {
                        try {
                            String fileName = packPath.getFileName().toString();
                            // Убираем .json из отображаемого имени
                            String displayName = fileName.toLowerCase().endsWith(".json")
                                    ? fileName.substring(0, fileName.length() - 5)
                                    : fileName;
                            // Добавляем только непустые имена
                            if (displayName != null && !displayName.trim().isEmpty()) {
                                packComboBox.addItem(displayName);
                            } else {
                                System.err.println("Пропущено добавление пакета с некорректным именем: " + packPath);
                            }
                        } catch (Exception e) {
                            System.err.println("Ошибка при обработке имени файла пакета: " + packPath + " -> " + e.getMessage());
                        }
                    });
                    System.out.println("ComboBox заполнен пакетами.");
                } else {
                    System.out.println("В директории не найдено файлов *.json.");
                    // Показываем предупреждение пользователю, только если директория существует и читаема
                    if (Files.exists(packsDir) && Files.isReadable(packsDir)) {
                        showErrorDialog("Не найдено ни одного пакета вопросов (*.json) в папке:\n" + packsDir.toAbsolutePath() +
                                        "\n\nСоздайте пак с помощью редактора или поместите файлы *.json в эту папку.",
                                "Пакеты не найдены", JOptionPane.WARNING_MESSAGE);
                    }
                }
            } else {
                System.out.println("Чтение директории пропущено из-за предыдущей ошибки.");
            }

        } catch (Exception ex) { // Ловим любые другие неожиданные ошибки (например, при получении default dir)
            directoryError = true; // Считаем это тоже ошибкой директории
            handleDirectoryError("Непредвиденная ошибка при доступе к директории паков", packsDir, ex);
            // ex.printStackTrace(); // Для отладки
        } finally {
            // 5. Обновляем состояние UI в блоке finally, чтобы это произошло всегда
            System.out.println("Завершение loadAvailablePacks (ошибка директории: " + directoryError + ")");

            // Управляем активностью ComboBox: активен, если нет ошибок И есть пакеты (кроме плейсхолдера)
            boolean enableComboBox = !directoryError && !availablePackPaths.isEmpty();
            packComboBox.setEnabled(enableComboBox);

            // Если ComboBox должен быть неактивен, убедимся, что выбран плейсхолдер
            // и все зависимые настройки сброшены
            if (!enableComboBox && packComboBox.getSelectedIndex() != 0) {
                packComboBox.setSelectedIndex(0); // Это вызовет handlePackSelection
            } else if (!enableComboBox) {
                // Если плейсхолдер уже выбран, но паков нет/была ошибка,
                // нужно вручную сбросить зависимые UI (т.к. handlePackSelection не вызовется)
                handlePackSelection(); // Вызов со сбросом
            }
            // Проверка кнопки "Старт" нужна в любом случае
            checkStartButtonState();
            updatePackActionButtonsState();
        }
    }

    private void handlePackSelection() {
        int selectedIndex = packComboBox.getSelectedIndex();
        Path selectedPackPath = null;
        QuestionPack loadedPack = null;
        boolean success = false;

        // Сбрасываем предыдущий выбранный пак и зависимое состояние UI
        selectedPack = null;
        roundsCountSpinner.setEnabled(false);
        catsCountSpinner.setEnabled(false);
        auctionsCountSpinner.setEnabled(false);
        // Модели сбрасываются в update...Model методах

        System.out.println("Выбран элемент ComboBox с индексом: " + selectedIndex);

        if (selectedIndex > 0 && selectedIndex <= availablePackPaths.size()) { // Выбран конкретный пак
            try {
                selectedPackPath = availablePackPaths.get(selectedIndex - 1);
                System.out.println("Попытка загрузки пакета: " + selectedPackPath.toAbsolutePath());

                if (!Files.exists(selectedPackPath)) {
                    throw new IOException("Файл пакета больше не существует: " + selectedPackPath.getFileName());
                }

                loadedPack = PackStorage.loadPack(selectedPackPath); // Загрузка

                if (loadedPack == null) {
                    throw new IOException("Не удалось загрузить пак (результат null). Возможно, файл поврежден или имеет неверный формат JSON.");
                }

                System.out.println("Валидация содержимого пакета...");
                validatePackContent(loadedPack); // Валидация (бросает исключение при ошибке)

                // Если все успешно - сохраняем пак и активируем UI
                selectedPack = loadedPack;
                success = true;
                System.out.println("Пакет '" + selectedPack.getPackName() + "' успешно загружен и валидирован.");

                roundsCountSpinner.setEnabled(true);
                catsCountSpinner.setEnabled(true);
                auctionsCountSpinner.setEnabled(true);

            } catch (IOException | IllegalArgumentException | IndexOutOfBoundsException | com.google.gson.JsonSyntaxException e) {
                // Обработка ошибок загрузки/валидации
                success = false;
                System.err.println("Ошибка при выборе или загрузке пакета '"
                        + (selectedPackPath != null ? selectedPackPath.getFileName() : "N/A") + "': " + e.getMessage());
                // e.printStackTrace(); // Для детальной диагностики
                showErrorDialog("Ошибка при загрузке или проверке пакета:\n"
                                + (selectedPackPath != null ? selectedPackPath.getFileName() : "N/A") + "\n\n" + e.getMessage(),
                        "Ошибка Пакета", JOptionPane.ERROR_MESSAGE);
                // Сбросить выбор в ComboBox обратно на плейсхолдер
                if (packComboBox.getSelectedIndex() != 0) {
                    SwingUtilities.invokeLater(() -> packComboBox.setSelectedIndex(0));
                    // selectedIndex будет обработан снова при смене на 0
                    return; // Прерываем текущую обработку, т.к. будет повторный вызов
                }
            } catch (Exception e) {
                success = false;
                System.err.println("Непредвиденная ошибка при обработке выбора пакета: " + e.getMessage());
                e.printStackTrace();
                showErrorDialog("Произошла непредвиденная ошибка:\n" + e.getMessage(), "Критическая Ошибка", JOptionPane.ERROR_MESSAGE);
                if (packComboBox.getSelectedIndex() != 0) {
                    SwingUtilities.invokeLater(() -> packComboBox.setSelectedIndex(0));
                    return;
                }
            }
        } else {
            System.out.println("Выбран плейсхолдер, сброс настроек раундов/спец.");
            // Плейсхолдер выбран, success остается false, UI уже сброшен
        }

        // Обновляем модели спиннеров и UI настройки раундов ВНЕ зависимости от успеха/неудачи,
        // т.к. они должны отражать текущее состояние (либо параметры пакета, либо минимум/ноль)
        updateRoundSpinnerModel();
        updateSpecialSpinnersModel();
        updateRoundSetupUI(); // Покажет EMPTY или RANDOM_INFO

        checkStartButtonState(); // Проверяем кнопку старт в конце
        updatePackActionButtonsState();
        System.out.println("Обработка выбора пакета завершена. Успех: " + success);
    }

    // Валидация содержимого пакета (как раньше)
    private void validatePackContent(QuestionPack pack) throws IllegalArgumentException {
        // ... (код метода validatePackContent остается БЕЗ ИЗМЕНЕНИЙ) ...
        // Проверяет pack != null, наличие тем, наличие вопросов в темах,
        // корректность текста/ответа/баллов/типа/файла/спецтипа вопросов
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
                themeName = "Тема " + (i + 1); // Используем временное имя для сообщений об ошибках
            }

            List<Question> questions = theme.getQuestions();
            if (questions == null || questions.isEmpty()) {
                // Не ошибка, если есть другие темы с вопросами
                System.out.println("Предупреждение: Тема '" + themeName + "' не содержит вопросов.");
                continue; // Проверяем следующую тему
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
                // Проверка наличия файла для медиа-вопросов
                if ((q.getType() == Question.QuestionType.IMAGE || q.getType() == Question.QuestionType.AUDIO)) {
                    String filePath = q.getFilePath();
                    if (filePath == null || filePath.trim().isEmpty()) {
                        throw new IllegalArgumentException("Вопрос №" + (j + 1) + " в теме '" + themeName + "' типа " + q.getType() + " не имеет пути к файлу.");
                    }
                    // Опциональная проверка существования файла здесь может замедлить загрузку,
                    // лучше оставить ее на момент отображения вопроса.
                }
                // Проверка типа спецвопроса (не null)
                if (q.getSpecialType() == null) {
                    System.err.println("Внимание! У вопроса №"+(j+1)+" в теме '"+themeName+"' specialType=null. Установлен NONE.");
                    q.setSpecialType(Question.SpecialType.NONE); // Автоисправление
                }
                hasQuestionsOverall = true; // Найден хотя бы один валидный вопрос
            }
        }
        if (!hasQuestionsOverall) {
            throw new IllegalArgumentException("В пакете не найдено ни одного валидного вопроса.");
        }
        // System.out.println("Пакет '" + pack.getPackName() + "' прошел валидацию."); // Лог успеха
    }

    // Обработка ошибок доступа к директории
    private void handleDirectoryError(String messagePrefix, Path dir, Exception ex) {
        String dirPathStr = (dir != null) ? dir.toAbsolutePath().toString() : "N/A";
        String errorMessage = messagePrefix + ":\n" + dirPathStr;
        if (ex != null) {
            errorMessage += "\n" + ex.getMessage();
            System.err.println(messagePrefix + " (" + dirPathStr + "): " + ex.getMessage());
            // ex.printStackTrace(); // Для детальной отладки
        } else {
            System.err.println(messagePrefix + " (" + dirPathStr + ")");
        }
        // Показываем диалог в потоке EDT
        final String finalErrorMessage = errorMessage; // Нужна final переменная для лямбды
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                finalErrorMessage + "\n\nПроверьте права доступа или конфигурацию.",
                "Ошибка Директории Паков",
                JOptionPane.ERROR_MESSAGE));
    }

    // --- Обновление UI ---

    private void updatePlayerNameFields() {
        if (playerCountSpinner == null || playerNameFields == null || playerNamesPanel == null) {
            System.err.println("Ошибка: Компоненты игрока не инициализированы в updatePlayerNameFields");
            return;
        }
        int playerCount = (Integer) playerCountSpinner.getValue();

        // Добавляем поля, если нужно
        while (playerNameFields.size() < playerCount) {
            addPlayerNameField();
        }
        // Удаляем лишние поля
        while (playerNameFields.size() > playerCount) {
            removePlayerNameField();
        }

        // Обновляем UI панели имен
        playerNamesPanel.revalidate();
        playerNamesPanel.repaint();

        // Проверяем кнопку старт ПОСЛЕ обновления полей
        checkStartButtonState();
    }

    private void addPlayerNameField() {
        int playerNum = playerNameFields.size() + 1;
        JTextField nameField = new JTextField("Игрок " + playerNum, 15);
        nameField.setFont(new Font("Arial", Font.PLAIN, 14));
        JLabel nameLabel = new JLabel("Игрок " + playerNum + ":");
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        // Слушатель для проверки кнопки "Старт" при изменении текста
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            private void check() { checkStartButtonState(); }
            @Override public void insertUpdate(DocumentEvent e) { check(); }
            @Override public void removeUpdate(DocumentEvent e) { check(); }
            @Override public void changedUpdate(DocumentEvent e) { check(); }
        });

        playerNameFields.add(nameField);

        JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fieldPanel.add(nameLabel);
        fieldPanel.add(nameField);
        // Устанавливаем максимальный размер, чтобы BoxLayout не растягивал сильно
        fieldPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, nameField.getPreferredSize().height + 5));
        playerNamesPanel.add(fieldPanel);
    }

    private void removePlayerNameField() {
        if (!playerNameFields.isEmpty() && playerNamesPanel.getComponentCount() > 0) {
            playerNameFields.remove(playerNameFields.size() - 1);
            playerNamesPanel.remove(playerNamesPanel.getComponentCount() - 1);
        }
    }

    // Обновление модели спиннера раундов
    private void updateRoundSpinnerModel() {
        int totalThemes = (selectedPack != null && selectedPack.getThemes() != null)
                ? (int) selectedPack.getThemes().stream().filter(Objects::nonNull).count()
                : 0;
        int maxRounds = Math.max(1, totalThemes); // Максимум - кол-во тем, но не меньше 1
        int minRounds = 1;
        int currentRoundValue = 1; // Значение по умолчанию или текущее, если оно валидно

        if (roundsCountSpinner.isEnabled()) { // Получаем текущее, только если активно
            currentRoundValue = Math.min((Integer) roundsCountSpinner.getValue(), maxRounds);
            currentRoundValue = Math.max(minRounds, currentRoundValue);
        } else { // Если неактивно, сбрасываем на минимум
            currentRoundValue = minRounds;
        }


        // System.out.println("DEBUG: updateRoundSpinnerModel - TotalThemes: " + totalThemes +
        //                   ", MinRounds: " + minRounds + ", MaxRounds: " + maxRounds +
        //                   ", NewValue: " + currentRoundValue);

        roundsCountSpinner.setModel(new SpinnerNumberModel(currentRoundValue, minRounds, maxRounds, 1));
        roundsCountSpinner.setEnabled(totalThemes > 0); // Активен, только если есть темы
    }

    // Обновление моделей спиннеров спецвопросов
    private void updateSpecialSpinnersModel() {
        int totalQuestions = getTotalQuestionsInPack();
        maxSpecialsLabel.setText("Макс. спец.: " + totalQuestions);
        int minSpecials = 0;

        int currentCats = 0;
        int currentAuctions = 0;

        // Получаем текущие значения, только если спиннеры активны
        if (catsCountSpinner.isEnabled()) {
            try { currentCats = (Integer) catsCountSpinner.getValue(); } catch (Exception ignored) {}
        }
        if (auctionsCountSpinner.isEnabled()) {
            try { currentAuctions = (Integer) auctionsCountSpinner.getValue(); } catch (Exception ignored) {}
        }

        // Модель для Котов
        int maxCats = Math.max(minSpecials, totalQuestions - currentAuctions);
        int catsValue = Math.min(currentCats, maxCats);
        catsValue = Math.max(minSpecials, catsValue);
        catsCountSpinner.setModel(new SpinnerNumberModel(catsValue, minSpecials, maxCats, 1));

        // Модель для Аукционов
        int maxAuctions = Math.max(minSpecials, totalQuestions - catsValue); // Используем обновленное catsValue
        int auctionsValue = Math.min(currentAuctions, maxAuctions);
        auctionsValue = Math.max(minSpecials, auctionsValue);
        auctionsCountSpinner.setModel(new SpinnerNumberModel(auctionsValue, minSpecials, maxAuctions, 1));

        boolean enabled = selectedPack != null && totalQuestions > 0;
        catsCountSpinner.setEnabled(enabled);
        auctionsCountSpinner.setEnabled(enabled);

        // System.out.println("DEBUG: updateSpecialSpinnersModel - TotalQ: " + totalQuestions +
        //                   ", Cats(Max="+maxCats+", Val="+catsValue+"), Auctions(Max="+maxAuctions+", Val="+auctionsValue+")");
    }

    // Обновление зависимых максимумов спецвопросов при изменении одного из них
    private void updateSpecialSpinnersMaximum() {
        int totalQuestions = getTotalQuestionsInPack();
        int minSpecials = 0;
        int catsValue = 0;
        int auctionsValue = 0;
        try { catsValue = (Integer) catsCountSpinner.getValue(); } catch (Exception ignored) {}
        try { auctionsValue = (Integer) auctionsCountSpinner.getValue(); } catch (Exception ignored) {}


        // Обновляем максимум для АУКЦИОНОВ
        SpinnerNumberModel auctionModel = (SpinnerNumberModel) auctionsCountSpinner.getModel();
        int newMaxAuctions = Math.max(minSpecials, totalQuestions - catsValue);
        if (!Objects.equals(auctionModel.getMaximum(), newMaxAuctions)) {
            auctionModel.setMaximum(newMaxAuctions);
            // Если текущее значение стало больше нового максимума, сбросить его
            if (auctionsValue > newMaxAuctions) {
                SwingUtilities.invokeLater(() -> auctionsCountSpinner.setValue(newMaxAuctions));
            }
        }

        // Обновляем максимум для КОТОВ
        SpinnerNumberModel catModel = (SpinnerNumberModel) catsCountSpinner.getModel();
        // Пересчитываем auctionsValue на случай, если оно изменилось выше
        try { auctionsValue = (Integer) auctionsCountSpinner.getValue(); } catch (Exception ignored) {}
        int newMaxCats = Math.max(minSpecials, totalQuestions - auctionsValue);
        if (!Objects.equals(catModel.getMaximum(), newMaxCats)) {
            catModel.setMaximum(newMaxCats);
            if (catsValue > newMaxCats) {
                SwingUtilities.invokeLater(() -> catsCountSpinner.setValue(newMaxCats));
            }
        }
        // System.out.println("DEBUG: updateSpecialSpinnersMaximum - NewMaxA: " + newMaxAuctions + ", NewMaxC: " + newMaxCats);
    }


    // Обновление панели настройки раундов (показ инфо или ошибки)
    private void updateRoundSetupUI() {
        CardLayout cl = (CardLayout) (roundSetupPanel.getLayout());
        String cardToShow = "EMPTY"; // По умолчанию

        if (selectedPack != null && selectedPack.getThemes() != null && !selectedPack.getThemes().isEmpty()) {
            int numRounds = 1; // Значение по умолчанию или текущее
            if (roundsCountSpinner.isEnabled()) { // Получаем, только если активно
                try { numRounds = (Integer) roundsCountSpinner.getValue(); } catch (Exception ignored) {}
            }
            int totalThemes = (int) selectedPack.getThemes().stream().filter(Objects::nonNull).count();

            if (totalThemes >= numRounds && totalThemes > 0) {
                cardToShow = "RANDOM_INFO";
                int themesPerRound = totalThemes / numRounds;
                int remainder = totalThemes % numRounds;
                themesPerRoundLabel.setText("Тем в раунде: ~" + themesPerRound +
                        (remainder > 0 ? " (+ остаток " + remainder + ")" : ""));
                themesPerRoundLabel.setForeground(Color.GRAY);
            } else {
                themesPerRoundLabel.setText("Ошибка: Тем (" + totalThemes + ") меньше, чем раундов (" + numRounds + ")!");
                themesPerRoundLabel.setForeground(Color.RED);
                cardToShow = "EMPTY";
            }
        } else {
            themesPerRoundLabel.setText("Тем в раунде: N/A");
            themesPerRoundLabel.setForeground(Color.GRAY);
            cardToShow = "EMPTY";
        }

        cl.show(roundSetupPanel, cardToShow);
        roundSetupPanel.revalidate();
        roundSetupPanel.repaint();
        // System.out.println("DEBUG: updateRoundSetupUI - Показана карта: " + cardToShow);
    }


    // Проверка и установка состояния кнопки "Начать игру"
    private void checkStartButtonState() {
        // Выполняем проверку и обновление кнопки в потоке EDT
        SwingUtilities.invokeLater(() -> {
            boolean packSelected = packComboBox.getSelectedIndex() > 0 && selectedPack != null;

            // Проверка имен (как раньше)
            boolean namesValid = false;
            if (playerNameFields != null && !playerNameFields.isEmpty() &&
                    playerNameFields.size() == (Integer) playerCountSpinner.getValue()) {
                namesValid = true;
                List<String> lowerCaseNames = new ArrayList<>();
                for (JTextField nameField : playerNameFields) {
                    if (nameField == null || nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                        namesValid = false; break;
                    }
                    String name = nameField.getText().trim().toLowerCase();
                    if (lowerCaseNames.contains(name)) {
                        namesValid = false; break;
                    }
                    lowerCaseNames.add(name);
                }
            }

            // Проверка настройки раундов (случайное распределение)
            boolean roundSetupValid = false;
            int totalThemes = 0;
            if (packSelected && selectedPack.getThemes() != null) {
                totalThemes = (int) selectedPack.getThemes().stream().filter(Objects::nonNull).count();
            }
            if (packSelected && roundsCountSpinner.isEnabled()) {
                int numRounds = (Integer) roundsCountSpinner.getValue();
                if (totalThemes >= numRounds && totalThemes > 0) { // Достаточно тем
                    roundSetupValid = true;
                }
            }

            // Проверка спецвопросов (как раньше)
            boolean specialSetupValid = false;
            int totalQuestions = 0;
            if (packSelected) totalQuestions = getTotalQuestionsInPack();
            if (packSelected) {
                if (totalQuestions == 0) {
                    specialSetupValid = true;
                } else if (catsCountSpinner.isEnabled() && auctionsCountSpinner.isEnabled()) {
                    try {
                        int cats = (Integer) catsCountSpinner.getValue();
                        int auctions = (Integer) auctionsCountSpinner.getValue();
                        if (cats >= 0 && auctions >= 0 && cats + auctions <= totalQuestions) {
                            specialSetupValid = true;
                        }
                    } catch (Exception ignored) {}
                }
            }


            // Итоговое решение
            boolean shouldBeEnabled = packSelected && namesValid && roundSetupValid && specialSetupValid;

            // Установка состояния кнопки
            startGameButton.setEnabled(shouldBeEnabled);

            // Детальный лог состояния (можно закомментировать)
             /*
             System.out.println("--- checkStartButtonState ---");
             System.out.println("Pack Selected: " + packSelected);
             System.out.println("Names Valid: " + namesValid + " (Fields: " + (playerNameFields != null ? playerNameFields.size() : "null") + ", Spinner: " + playerCountSpinner.getValue() + ")");
             System.out.println("Round Setup Valid: " + roundSetupValid + " (Themes: "+totalThemes+", Rounds: "+roundsCountSpinner.getValue()+")");
             System.out.println("Special Setup Valid: " + specialSetupValid + " (TotalQ: "+totalQuestions+", Cats: "+catsCountSpinner.getValue()+", Auctions: "+auctionsCountSpinner.getValue()+")");
             System.out.println("==> Setting Start Button Enabled: " + shouldBeEnabled);
             System.out.println("-----------------------------");
             */
        });
    }

    // Вспомогательный метод для подсчета общего количества вопросов в пакете
    private int getTotalQuestionsInPack() {
        if (selectedPack == null || selectedPack.getThemes() == null) {
            return 0;
        }
        int totalQuestions = 0;
        for (Theme theme : selectedPack.getThemes()) {
            if (theme != null && theme.getQuestions() != null) {
                totalQuestions += (int) theme.getQuestions().stream().filter(Objects::nonNull).count();
            }
        }
        return totalQuestions;
    }


    // --- Старт Игры ---
    private void startGame() {
        System.out.println("Нажата кнопка 'Начать игру'. Проверка и запуск...");

        // 1. Финальная проверка состояния кнопки и данных
        if (!startGameButton.isEnabled()) {
            showErrorDialog("Пожалуйста, убедитесь, что все настройки корректны.", "Запуск Невозможен");
            return;
        }
        if (selectedPack == null) {
            showErrorDialog("Ошибка: Пакет вопросов не выбран или не загружен.", "Ошибка Запуска");
            return;
        }
        if (playerNameFields == null || playerNameFields.isEmpty() ||
                playerNameFields.size() != (Integer) playerCountSpinner.getValue()) {
            showErrorDialog("Ошибка: Некорректное количество или состояние полей имен игроков.", "Ошибка Запуска");
            updatePlayerNameFields(); // Попробуем исправить
            return;
        }

        // 2. Сбор имен игроков (с финальной проверкой)
        List<Player> players = new ArrayList<>();
        List<String> lowerCaseNames = new ArrayList<>();
        for (JTextField nameField : playerNameFields) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                showErrorDialog("Имя игрока не может быть пустым.", "Ошибка Имен Игроков");
                nameField.requestFocusInWindow(); return;
            }
            String lowerName = name.toLowerCase();
            if (lowerCaseNames.contains(lowerName)) {
                showErrorDialog("Имена игроков должны быть уникальными (без учета регистра).\nОбнаружен дубликат: " + name, "Ошибка Имен Игроков");
                nameField.requestFocusInWindow(); return;
            }
            lowerCaseNames.add(lowerName);
            players.add(new Player(name));
        }
        System.out.println("Собраны игроки: " + players.size());


        // 3. Сбор данных о раундах (ТОЛЬКО случайное распределение)
        List<List<Theme>> roundThemes = new ArrayList<>();
        List<String> roundNames = new ArrayList<>();
        int numRounds = (Integer) roundsCountSpinner.getValue();

        System.out.println("Выполняется случайное распределение тем...");
        try {
            List<Theme> allThemes = selectedPack.getThemes().stream()
                    .filter(Objects::nonNull)
                    .filter(t -> t.getQuestions() != null && !t.getQuestions().isEmpty()) // Берем только темы с вопросами
                    .collect(Collectors.toList());

            if (allThemes.size() < numRounds) { // Проверка, что тем (с вопросами) достаточно
                throw new IllegalStateException("Недостаточно тем с вопросами (" + allThemes.size() + ") для " + numRounds + " раундов.");
            }

            Collections.shuffle(allThemes);

            int themesPerRoundBase = allThemes.size() / numRounds;
            int remainder = allThemes.size() % numRounds;
            int currentThemeIndex = 0;

            for (int i = 0; i < numRounds; i++) {
                int themesInThisRound = themesPerRoundBase + (i < remainder ? 1 : 0);
                List<Theme> themesForThisRound = new ArrayList<>();
                for (int j = 0; j < themesInThisRound && currentThemeIndex < allThemes.size(); j++) {
                    themesForThisRound.add(allThemes.get(currentThemeIndex++));
                }
                // Добавляем раунд, только если удалось добавить в него темы
                if (!themesForThisRound.isEmpty()) {
                    roundThemes.add(themesForThisRound);
                    roundNames.add("Раунд " + (roundThemes.size()));
                }
            }
            System.out.println("Темы распределены случайно по " + roundThemes.size() + " раундам.");
            if (roundThemes.isEmpty() && numRounds > 0) {
                throw new IllegalStateException("Не удалось сформировать ни одного игрового раунда (возможно, в пакете нет тем с вопросами).");
            }

        } catch (Exception e) {
            System.err.println("Критическая ошибка при распределении тем: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Произошла ошибка при распределении тем:\n" + e.getMessage(), "Ошибка Запуска");
            return;
        }


        // 4. Получаем количество Котов и Аукционов
        int numCats = (Integer) catsCountSpinner.getValue();
        int numAuctions = (Integer) auctionsCountSpinner.getValue();

        // 5. Создаем контроллер игры
        System.out.println("Создание GameController...");
        GameController gameController;
        try {
            gameController = new GameController(selectedPack, players, roundThemes, roundNames, numCats, numAuctions);
        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка при создании GameController: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Произошла ошибка при инициализации игры:\n" + e.getMessage(), "Ошибка Запуска");
            return;
        }

        // 6. Создаем и показываем игровые окна
        System.out.println("Создание игровых окон...");
        HostFrame hostFrame = new HostFrame(gameController);
        PlayerFrame playerFrame = new PlayerFrame(gameController);

        // 7. Регистрируем окна в контроллере (ВАЖНО: до показа окон)
        gameController.registerFrames(hostFrame, playerFrame); // Контроллер начнет первый раунд

        // 8. Настраиваем расположение окон (опционально)
        System.out.println("Настройка расположения окон...");
        setupWindowLocations(hostFrame, playerFrame);

        // 9. Показываем игровые окна
        System.out.println("Отображение игровых окон...");
        hostFrame.setVisible(true);
        playerFrame.setVisible(true);

        // 10. Закрываем окно настройки и скрываем главное меню
        System.out.println("Закрытие окна настроек...");
        this.dispose(); // Закрыть текущее окно (GameSetupFrame)
        if (parentFrame != null) {
            System.out.println("Скрытие главного меню...");
            parentFrame.setVisible(false); // Скрываем главное меню
        }

        System.out.println("Запуск игры завершен.");

    } // Конец startGame()


    // --- Навигация и вспомогательные UI методы ---

    // Возврат в главное меню
    private void goBackToParent() {
        System.out.println("Возврат в главное меню...");
        this.dispose(); // Закрыть окно настроек
        if (parentFrame != null) {
            parentFrame.setVisible(true); // Показать главное меню
        }
    }

    // Размещение окон (как раньше)
    private void setupWindowLocations(JFrame hostFrame, JFrame playerFrame) {
        // ... (код метода setupWindowLocations остается БЕЗ ИЗМЕНЕНИЙ) ...
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();
        GraphicsDevice mainScreen = ge.getDefaultScreenDevice();
        GraphicsDevice secondaryScreen = null;
        for (GraphicsDevice screen : screens) {
            if (screen != mainScreen) {
                secondaryScreen = screen;
                break;
            }
        }
        // Центрируем ведущего
        hostFrame.pack();
        hostFrame.setLocationRelativeTo(null);

        // Размещаем игрока
        playerFrame.pack();
        if (secondaryScreen != null) {
            System.out.println("Найден второй монитор. Размещаем окно игроков на втором экране.");
            Rectangle bounds = secondaryScreen.getDefaultConfiguration().getBounds();
            playerFrame.setLocation(bounds.x + (bounds.width - playerFrame.getWidth()) / 2,
                    bounds.y + (bounds.height - playerFrame.getHeight()) / 2);
        } else {
            System.out.println("Второй монитор не найден. Окно игроков будет размещено справа от окна ведущего.");
            Point hostLoc = hostFrame.getLocation();
            int hostWidth = hostFrame.getWidth();
            Rectangle mainScreenBounds = mainScreen.getDefaultConfiguration().getBounds();
            int playerX = hostLoc.x + hostWidth + 10;
            int playerY = hostLoc.y;
            // Проверка выхода за правый край
            if (playerX + playerFrame.getWidth() > mainScreenBounds.x + mainScreenBounds.width) {
                playerX = hostLoc.x; // Ставим под ведущим
                playerY = hostLoc.y + hostFrame.getHeight() + 10;
                // Проверка выхода за нижний край
                if(playerY + playerFrame.getHeight() > mainScreenBounds.y + mainScreenBounds.height){
                    playerX = hostLoc.x + hostWidth + 10; // Возвращаем справа
                    playerY = hostLoc.y;
                    // Если не влезает, то не влезает... можно уменьшить окно игрока?
                }
            }
            playerFrame.setLocation(playerX, playerY);
        }
    }

    // Вспомогательный метод для показа ошибок (как раньше)
    private void showErrorDialog(String message, String title) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE));
    }

    // Перегрузка для показа предупреждений
    private void showErrorDialog(String message, String title, int messageType) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message, title, messageType));
    }

    /**
     * Обновляет состояние (enabled/disabled) кнопок "Редактировать" и "Удалить"
     * в зависимости от того, выбран ли реальный пакет в ComboBox.
     */
    private void updatePackActionButtonsState() {
        boolean packIsSelected = packComboBox.getSelectedIndex() > 0 && selectedPack != null;
        editPackButton.setEnabled(packIsSelected);
        deletePackButton.setEnabled(packIsSelected);
    }

    /**
     * Вызывается при нажатии кнопки "Редактировать".
     * Открывает выбранный пак в PackCreatorFrame.
     */
    private void editSelectedPack() {
        int selectedIndex = packComboBox.getSelectedIndex();
        if (selectedIndex <= 0 || selectedIndex > availablePackPaths.size()) {
            showErrorDialog("Пакет не выбран или индекс некорректен.", "Ошибка");
            return;
        }
        Path packPathToEdit = availablePackPaths.get(selectedIndex - 1);
        System.out.println("Запрос на редактирование пакета: " + packPathToEdit);

        // Проверяем существование файла перед открытием редактора
        if (!Files.exists(packPathToEdit)) {
            showErrorDialog("Файл пакета '" + packPathToEdit.getFileName() + "' больше не существует.\nОбновите список пакетов.", "Файл не найден");
            loadAvailablePacks(); // Обновить список
            return;
        }

        // Создаем и показываем редактор, передавая путь
        PackCreatorFrame packCreator = new PackCreatorFrame(this, packPathToEdit); // Новый конструктор!
        packCreator.setVisible(true);
        this.setVisible(false); // Скрываем окно настроек
    }

    /**
     * Вызывается при нажатии кнопки "Удалить".
     * Удаляет выбранный файл пакета после подтверждения.
     */
    private void deleteSelectedPack() {
        int selectedIndex = packComboBox.getSelectedIndex();
        if (selectedIndex <= 0 || selectedIndex > availablePackPaths.size()) {
            showErrorDialog("Пакет не выбран или индекс некорректен.", "Ошибка");
            return;
        }
        Path packPathToDelete = availablePackPaths.get(selectedIndex - 1);
        String packName = packPathToDelete.getFileName().toString();

        System.out.println("Запрос на удаление пакета: " + packPathToDelete);

        int choice = JOptionPane.showConfirmDialog(this,
                "Вы уверены, что хотите удалить пакет '" + packName + "'?\nЭто действие необратимо!",
                "Подтверждение удаления",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            try {
                boolean deleted = Files.deleteIfExists(packPathToDelete);
                if (deleted) {
                    System.out.println("Пакет успешно удален: " + packName);
                    JOptionPane.showMessageDialog(this, "Пакет '" + packName + "' успешно удален.",
                            "Удаление завершено", JOptionPane.INFORMATION_MESSAGE);
                    // Обязательно перезагружаем список пакетов
                    loadAvailablePacks();
                } else {
                    System.err.println("Не удалось удалить пакет (возможно, он уже был удален): " + packName);
                    showErrorDialog("Не удалось удалить пакет '" + packName + "'.\nВозможно, он уже был удален.", "Ошибка удаления");
                    loadAvailablePacks(); // Все равно обновить список
                }
            } catch (IOException | SecurityException ex) {
                System.err.println("Ошибка при удалении пакета '" + packName + "': " + ex.getMessage());
                ex.printStackTrace();
                showErrorDialog("Произошла ошибка при удалении пакета '" + packName + "':\n" + ex.getMessage(),
                        "Ошибка удаления", JOptionPane.ERROR_MESSAGE);
                loadAvailablePacks(); // Обновить список на случай изменения прав
            }
        } else {
            System.out.println("Удаление пакета отменено пользователем.");
        }
    }
} // Конец класса GameSetupFrame