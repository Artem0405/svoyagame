// Файл: GameController.java
package org.example.game;

import org.example.data.*; // Импортируем все классы данных (Player, Question, Theme, QuestionPack)
import org.example.ui.HostFrame;
import org.example.ui.MainMenuFrame;
import org.example.ui.PlayerFrame;
// Возможно, понадобится GameBoardPanel, если будете напрямую с ним взаимодействовать
// import org.example.ui.components.GameBoardPanel;

import javax.swing.*; // Для JOptionPane
import java.awt.Point; // Для координат вопросов и возврата из assignSpecialQuestions
import java.util.*; // Для List, ArrayList, Collections, Random, Map, HashMap
import java.util.stream.Collectors; // Для Stream API в startNextRound и findThemeName
import java.awt.Component; // <<< ДОБАВИТЬ ЭТУ СТРОКУ

/**
 * Управляет логикой игры "Своя Игра".
 * Хранит состояние игры, игроков, пакет вопросов, раунды,
 * обрабатывает выбор вопросов, ответы, начисление очков,
 * смену раундов, паузу и взаимодействие с UI (HostFrame, PlayerFrame).
 */
public class GameController {

    // --- Внутренний класс для хранения данных раунда ---
    private static class RoundInfo {
        final String name; // Название раунда (может быть null)
        final List<Theme> themes; // Глубокая копия тем для ЭТОГО раунда
        boolean[][] answeredQuestionsInRound; // Состояние ответов для этого раунда

        RoundInfo(String name, List<Theme> themesCopy) {
            this.name = (name != null && !name.isBlank()) ? name : null;
            // Принимаем уже скопированный список
            this.themes = themesCopy != null ? themesCopy : new ArrayList<>();
            initializeAnsweredQuestions(); // Инициализируем массив ответов
        }

        // Инициализация или переинициализация массива answeredQuestions
        // Вызывается при создании RoundInfo и на всякий случай в startNextRound
        void initializeAnsweredQuestions() {
            if (!this.themes.isEmpty()) {
                int numThemes = this.themes.size();
                int maxQuestions = 0;
                for (Theme t : this.themes) {
                    if (t != null && t.getQuestions() != null) {
                        maxQuestions = Math.max(maxQuestions, t.getQuestions().size());
                    } else {
                        // Логируем, если тема null или без списка вопросов
                        System.err.println("Warning: Found null theme or theme without questions list during RoundInfo initialization for round: " + this.name);
                    }
                }
                // Создаем массив с максимальным размером
                this.answeredQuestionsInRound = new boolean[numThemes][maxQuestions];
                // По умолчанию все значения false (Java инициализирует так)
            } else {
                // Если тем нет, создаем пустой массив
                System.err.println("Warning: Attempting to initialize RoundInfo with no themes for round: " + this.name);
                this.answeredQuestionsInRound = new boolean[0][0];
            }
        }
    }
    // --- Конец внутреннего класса RoundInfo ---

    // --- Поля класса GameController ---
    private final QuestionPack originalQuestionPack; // Исходный пак (не изменяется)
    private final List<Player> players;             // Список игроков (ссылки на объекты Player)
    private HostFrame hostFrame;                   // Ссылка на окно ведущего
    private PlayerFrame playerFrame;                 // Ссылка на окно игрока

    private Question currentQuestion;              // Текущий разыгрываемый вопрос
    private Player currentCatTargetPlayer = null;   // Игрок, отвечающий на "Кота в мешке"
    private Player currentAuctionWinner = null;    // Игрок, выигравший аукцион (отвечает)
    private int currentAuctionBet = 0;             // Ставка в текущем аукционе

    // --- Поля для раундов ---
    private final List<RoundInfo> rounds;          // Список всех раундов игры
    private int currentRoundIndex = -1;           // Индекс текущего раунда (-1 = игра не начата)
    private RoundInfo currentRoundInfo = null;     // Кэш текущего раунда для быстрого доступа

    // --- Поля для распределения спецвопросов ---
    private final int totalCatsToAssign;
    private final int totalAuctionsToAssign;
    // Счетчик реально назначенных вопросов (для отладки)
    private int totalCatsAssigned = 0;
    private int totalAuctionsAssigned = 0;


    // --- Состояние игры ---
    private boolean isPaused = false;              // Флаг паузы
    private final Random random = new Random();    // Генератор случайных чисел для выбора вопросов

    // --- КОНСТРУКТОР ---
    /**
     * Создает новый экземпляр GameController.
     * @param originalPack Пакет вопросов.
     * @param players Список игроков.
     * @param roundThemes Список списков тем для каждого раунда (будет глубоко скопирован).
     * @param roundNames Список названий раундов (может быть null).
     * @param totalCats Общее количество "Котов в мешке" для распределения.
     * @param totalAuctions Общее количество "Аукционов" для распределения.
     * @throws IllegalArgumentException если входные данные некорректны.
     */
    public GameController(QuestionPack originalPack, List<Player> players,
                          List<List<Theme>> roundThemes, List<String> roundNames,
                          int totalCats, int totalAuctions) {

        // Проверка входных данных
        if (originalPack == null) throw new IllegalArgumentException("originalPack не может быть null");
        if (players == null || players.isEmpty()) throw new IllegalArgumentException("players не может быть null или пустым");
        if (roundThemes == null || roundThemes.isEmpty()) throw new IllegalArgumentException("roundThemes не может быть null или пустым");

        this.originalQuestionPack = originalPack; // Сохраняем ссылку на оригинал
        this.players = new ArrayList<>(players); // Создаем копию списка игроков
        this.rounds = new ArrayList<>();
        this.totalCatsToAssign = Math.max(0, totalCats); // Гарантируем неотрицательное значение
        this.totalAuctionsToAssign = Math.max(0, totalAuctions); // Гарантируем неотрицательное значение

        int numRounds = roundThemes.size();
        // Проверка соответствия имен раундов
        if (roundNames != null && roundNames.size() != numRounds) {
            System.err.println("Предупреждение: Количество названий раундов (" +
                    roundNames.size() +
                    ") не совпадает с количеством раундов (" + numRounds + "). Названия могут быть некорректны.");
        }

        // Отладочные счетчики назначенных спецвопросов
        int catsAssignedSoFar = 0;
        int auctionsAssignedSoFar = 0;

        System.out.println("Initializing rounds...");

        // Создаем информацию о каждом раунде
        for (int i = 0; i < numRounds; i++) {
            List<Theme> originalThemesForThisRound = roundThemes.get(i);
            String nameForRound = (roundNames != null && i < roundNames.size() && roundNames.get(i) != null && !roundNames.get(i).isBlank())
                    ? roundNames.get(i) : "Раунд " + (i + 1);

            // *** ВАЖНО: Создаем ГЛУБОКУЮ копию тем для этого раунда ***
            List<Theme> themesForRoundCopy = deepCopyThemes(originalThemesForThisRound);

            if (themesForRoundCopy.isEmpty() || themesForRoundCopy.stream().allMatch(t -> t == null || t.getQuestions() == null || t.getQuestions().isEmpty())) {
                System.err.println("Предупреждение: В раунде '" + nameForRound + "' нет тем или вопросов. Раунд будет пропущен или пуст.");
                // Добавляем пустой раунд, чтобы сохранить индексацию
                this.rounds.add(new RoundInfo(nameForRound, themesForRoundCopy)); // Передаем (возможно пустую) копию
                continue; // Переходим к следующему раунду
            }

            // --- РАСПРЕДЕЛЕНИЕ СПЕЦВОПРОСОВ (работает с КОПИЕЙ) ---
            int remainingRounds = numRounds - i;
            int remainingCats = Math.max(0, this.totalCatsToAssign - catsAssignedSoFar);
            int remainingAuctions = Math.max(0, this.totalAuctionsToAssign - auctionsAssignedSoFar);

            // Рассчитываем квоту для текущего раунда
            int catsForThisRound = (remainingRounds > 0) ? (remainingCats / remainingRounds) : 0;
            // Оставшихся котов добавляем по одному, начиная с текущего раунда
            if (i < remainingCats % remainingRounds) {
                catsForThisRound++;
            }

            int auctionsForThisRound = (remainingRounds > 0) ? (remainingAuctions / remainingRounds) : 0;
            if (i < remainingAuctions % remainingRounds) {
                auctionsForThisRound++;
            }


            // Назначаем спецвопросы ВНУТРИ КОПИИ тем этого раунда
            System.out.println("Assigning special questions for round " + (i + 1) + " ('" + nameForRound + "'): " +
                    "Requesting Cats=" + catsForThisRound + ", Auctions=" + auctionsForThisRound);
            Point assignedCounts = assignSpecialQuestionsToThemes(themesForRoundCopy, catsForThisRound, auctionsForThisRound);

            catsAssignedSoFar += assignedCounts.x;
            auctionsAssignedSoFar += assignedCounts.y;
            System.out.println("  Assigned in this round: Cats=" + assignedCounts.x + ", Auctions=" + assignedCounts.y);
            // --- КОНЕЦ РАСПРЕДЕЛЕНИЯ ---

            // Добавляем информацию о раунде, используя СКОПИРОВАННЫЕ и МОДИФИЦИРОВАННЫЕ темы
            this.rounds.add(new RoundInfo(nameForRound, themesForRoundCopy));
            System.out.println("Round " + (i + 1) + " initialized.");
        } // Конец цикла for по раундам

        // Сохраняем общее количество реально назначенных (для отладки)
        this.totalCatsAssigned = catsAssignedSoFar;
        this.totalAuctionsAssigned = auctionsAssignedSoFar;

        System.out.println("GameController created. Rounds: " + this.rounds.size() +
                ", Players: " + this.players.size() +
                ", Total Cats Assigned: " + this.totalCatsAssigned + "/" + this.totalCatsToAssign +
                ", Total Auctions Assigned: " + this.totalAuctionsAssigned + "/" + this.totalAuctionsToAssign);

        // Начало игры отложено до регистрации окон методом registerFrames
    }

    // --- ВСПОМОГАТЕЛЬНЫЙ МЕТОД ГЛУБОКОГО КОПИРОВАНИЯ ТЕМ ---
    /**
     * Создает глубокую копию списка тем, включая копии всех вопросов внутри тем.
     * @param originalThemes Список тем для копирования.
     * @return Новый список с полными копиями тем и вопросов, или пустой список, если originalThemes null.
     */
    private List<Theme> deepCopyThemes(List<Theme> originalThemes) {
        if (originalThemes == null) {
            return new ArrayList<>(); // Возвращаем пустой список, а не null
        }

        List<Theme> copiedThemes = new ArrayList<>(originalThemes.size());
        for (Theme originalTheme : originalThemes) {
            if (originalTheme == null) {
                copiedThemes.add(null); // Сохраняем null, если он был в оригинале
                continue;
            }

            // 1. Копируем саму тему (используя конструктор Theme)
            Theme copiedTheme = new Theme(originalTheme.getThemeName());

            // 2. Копируем список вопросов
            List<Question> originalQuestions = originalTheme.getQuestions();
            if (originalQuestions != null) {
                List<Question> copiedQuestions = new ArrayList<>(originalQuestions.size());
                for (Question originalQuestion : originalQuestions) {
                    if (originalQuestion == null) {
                        copiedQuestions.add(null); // Сохраняем null
                        continue;
                    }
                    // 3. Копируем каждый вопрос, создавая НОВЫЙ объект Question
                    Question copiedQuestion = new Question(
                            originalQuestion.getQuestionText(),
                            originalQuestion.getAnswerText(),
                            originalQuestion.getPoints(),
                            originalQuestion.getType(),
                            originalQuestion.getFilePath(), // Путь копируется как строка (неизменяемый)
                            originalQuestion.getSpecialType() // Enum тоже копируется по значению
                    );
                    copiedQuestions.add(copiedQuestion);
                }
                // Устанавливаем скопированный список вопросов в скопированную тему
                copiedTheme.setQuestions(copiedQuestions);
            } else {
                // Если в оригинале список вопросов был null, создаем пустой список
                copiedTheme.setQuestions(new ArrayList<>());
            }
            copiedThemes.add(copiedTheme); // Добавляем скопированную тему в результат
        }
        return copiedThemes;
    }


    // --- МЕТОДЫ УПРАВЛЕНИЯ ИГРОЙ ---

    /** Регистрирует окна ведущего и игрока. Вызывается перед началом игры. */
    public void registerFrames(HostFrame host, PlayerFrame player) {
        this.hostFrame = host;
        this.playerFrame = player;
        System.out.println("Игровые окна зарегистрированы в контроллере.");

        // Инициализируем UI начальными данными игроков
        if (hostFrame != null) hostFrame.initializeUI(this.players);
        if (playerFrame != null) playerFrame.initializeUI(this.players);

        // Теперь можно начать первый раунд
        startNextRound();
    }

    /** Начинает следующий раунд или завершает игру, если раунды закончились. */
    public void startNextRound() {
        currentRoundIndex++; // Переходим к следующему раунду

        if (currentRoundIndex < rounds.size()) {
            currentRoundInfo = rounds.get(currentRoundIndex);

            // Проверка и инициализация массива ответов на всякий случай
            if (currentRoundInfo.answeredQuestionsInRound == null) {
                System.err.println("Warning: answeredQuestionsInRound was null for round " + (currentRoundIndex + 1) + ". Reinitializing.");
                currentRoundInfo.initializeAnsweredQuestions();
            }

            // Сбрасываем состояние перед началом раунда
            currentQuestion = null;
            currentCatTargetPlayer = null;
            currentAuctionWinner = null;
            currentAuctionBet = 0;
            isPaused = false; // Снимаем паузу принудительно

            String roundName = getCurrentRoundName();
            System.out.println("\n--- НАЧАЛО РАУНДА " + getCurrentRoundNumber() + (roundName != null ? " (\"" + roundName + "\")" : "") + " ---");

            // Обновляем UI для нового раунда
            SwingUtilities.invokeLater(() -> { // Обновления UI в EDT
                if (hostFrame != null) {
                    hostFrame.updateRoundInfo(getCurrentRoundNumber(), roundName);
                    if(hostFrame.getGameBoardPanel() != null) {
                        // Передаем ТОЛЬКО темы текущего раунда (уже копия)
                        hostFrame.getGameBoardPanel().rebuildBoard(currentRoundInfo.themes);
                        // Обновляем состояние доски (включить/выключить кнопки)
                        hostFrame.getGameBoardPanel().setBoardEnabled(!isPaused, currentRoundInfo.answeredQuestionsInRound);
                    }
                    hostFrame.clearQuestionArea(); // Очищаем область вопроса/ответа
                    hostFrame.updatePauseState(isPaused); // Обновляем кнопку паузы
                    hostFrame.setNextRoundButtonEnabled(false); // Отключаем кнопку "След. раунд"
                }
                if (playerFrame != null) {
                    playerFrame.updateRoundInfo(getCurrentRoundNumber(), roundName);
                    if(playerFrame.getGameBoardPanel() != null) {
                        playerFrame.getGameBoardPanel().rebuildBoard(currentRoundInfo.themes);
                        playerFrame.getGameBoardPanel().setBoardEnabled(!isPaused, currentRoundInfo.answeredQuestionsInRound);
                    }
                    playerFrame.clearQuestionArea();
                    playerFrame.updatePauseState(isPaused);
                }
            });
        } else {
            // Раунды закончились
            System.out.println("--- ИГРА ЗАВЕРШЕНА (все раунды сыграны) ---");
            showFinalResults();
        }
    }

    /** Обрабатывает выбор вопроса ведущим. */
    public void hostSelectedQuestion(int themeIndexInRound, int questionIndexInTheme) {
        if (currentRoundInfo == null) {
            System.err.println("Ошибка: Попытка выбрать вопрос до начала раунда.");
            return;
        }
        if (isPaused) {
            System.out.println("Нельзя выбрать вопрос: игра на паузе.");
            showInfoDialog(hostFrame, "Игра на паузе!", "Пауза");
            return;
        }
        if (currentQuestion != null) {
            System.out.println("Нельзя выбрать новый вопрос, пока текущий не разыгран.");
            showInfoDialog(hostFrame, "Текущий вопрос еще не разыгран.", "Внимание");
            return;
        }


        // Проверка валидности индексов и был ли вопрос уже отвечен
        if (!isValidQuestionIndexInCurrentRound(themeIndexInRound, questionIndexInTheme)) {
            System.err.println("Ошибка выбора вопроса: Невалидные индексы [" +
                    themeIndexInRound + "][" + questionIndexInTheme + "]");
            return; // Не показываем диалог, просто ошибка в логе
        }
        if (currentRoundInfo.answeredQuestionsInRound[themeIndexInRound][questionIndexInTheme]) {
            System.out.println("Вопрос раунда [" + themeIndexInRound + "][" + questionIndexInTheme + "] уже был выбран.");
            // Не показываем диалог, просто игнорируем клик
            return;
        }

        // Получаем выбранный вопрос (из копии тем текущего раунда)
        Question selectedQuestion = null;
        try {
            selectedQuestion = currentRoundInfo.themes.get(themeIndexInRound)
                    .getQuestions().get(questionIndexInTheme);
            if (selectedQuestion == null) throw new NullPointerException("Объект вопроса null");
        } catch (Exception e) {
            System.err.println("Критическая ошибка получения вопроса из RoundInfo: T=" +
                    themeIndexInRound + ", Q=" + questionIndexInTheme + " - " + e.getMessage());
            e.printStackTrace();
            showErrorDialog(hostFrame, "Внутренняя ошибка при выборе вопроса.", "Критическая ошибка");
            currentQuestion = null; // Сбрасываем на всякий случай
            return;
        }

        System.out.println("Ведущий выбрал вопрос (Раунд " + getCurrentRoundNumber() +
                "): Тема=" + themeIndexInRound + ", Вопрос=" + questionIndexInTheme +
                " (Тип: " + selectedQuestion.getType() + ", Спец: " + selectedQuestion.getSpecialType() + ")");

        // Устанавливаем текущий вопрос
        currentQuestion = selectedQuestion;
        currentCatTargetPlayer = null; // Сброс перед обработкой
        currentAuctionWinner = null;
        currentAuctionBet = 0;


        // --- ОБРАБОТКА СПЕЦТИПА ---
        boolean markAsAnsweredNow = true; // Отмечать как отвеченный сразу для спецвопросов
        switch (currentQuestion.getSpecialType()) {
            case CAT_IN_A_BAG:
                System.out.println("!!! СПЕЦВОПРОС: КОТ В МЕШКЕ !!!");
                // Вопрос отмечает как сыгранный сразу, но кнопки на доске отключатся после завершения
                markQuestionAsAnsweredInCurrentRound(themeIndexInRound, questionIndexInTheme); // Отмечаем в модели
                startCatInABag(currentQuestion); // Запускаем логику "Кота" (выбор игрока)
                markAsAnsweredNow = false; // Логика кота сама решит, когда разыгран
                break;
            case AUCTION:
                System.out.println("!!! СПЕЦВОПРОС: АУКЦИОН !!!");
                // Вопрос отмечает как сыгранный сразу
                markQuestionAsAnsweredInCurrentRound(themeIndexInRound, questionIndexInTheme); // Отмечаем в модели
                startAuction(currentQuestion); // Запускаем логику аукциона
                markAsAnsweredNow = false; // Логика аукциона сама решит
                break;
            case NONE:
            default:
                System.out.println("Обычный вопрос выбран.");
                displayCurrentQuestionUI(); // Отображаем вопрос
                // Настраиваем кнопки ответа для ВСЕХ игроков
                if (hostFrame != null) {
                    hostFrame.setupAnswerControlButtons(this.players);
                }
                markAsAnsweredNow = false; // Обычный вопрос отмечается после ответа
                break;
        }

        // Отключаем кнопку на доске сразу для спец. вопросов
        // Для обычных - кнопка остается активной до ответа
        if (markAsAnsweredNow) {
            // Отключаем кнопку немедленно, т.к. вопрос "разыгрывается" по-особому
            disableButtonOnBoardsInCurrentRound(themeIndexInRound, questionIndexInTheme);
        }
    }


    /** Обрабатывает ответ игрока или решение ведущего ("Никто"). */
    // В файле: GameController.java

    // В файле: GameController.java

    /**
     * Обрабатывает ответ игрока или решение ведущего ("Никто").
     * Начисляет/списывает очки, определяет завершенность вопроса,
     * обновляет UI (счет, состояние доски, область вопроса) и проверяет завершенность раунда.
     *
     * @param respondingPlayer Игрок, нажавший кнопку "Верно"/"Неверно" у ведущего (или null, если ведущий нажал "Никто").
     * @param correct          true, если ответ правильный, false - если неправильный или "Никто".
     */
    public void processAnswer(Player respondingPlayer, boolean correct) {
        // 1. Проверки начального состояния
        if (currentQuestion == null) {
            System.err.println("processAnswer вызван, когда нет текущего вопроса! Игнорируется.");
            return; // Нечего обрабатывать
        }
        if (isPaused) {
            System.out.println("Ответ не принят: игра на паузе.");
            // Сообщение ведущему (если окно есть)
            if (hostFrame != null) {
                showInfoDialog(hostFrame, "Игра на паузе! Ответ не принят.", "Пауза");
            }
            return; // Не обрабатываем ответы во время паузы
        }

        // 2. Сохраняем ссылки на обрабатываемый вопрос и его свойства (в final переменные)
        final Question questionBeingProcessed = currentQuestion;
        final Question.SpecialType specialType = questionBeingProcessed.getSpecialType();
        final Question.QuestionType questionType = questionBeingProcessed.getType(); // Сохраняем тип для закрытия картинки

        // 3. Определяем, чей счет будет меняться и кто инициировал действие
        Player playerWhoseScoreChanges = null;
        String actionSourceDescription; // Для логов

        if (respondingPlayer != null) { // Если кнопку нажал игрок (или ведущий за игрока)
            actionSourceDescription = "Игрок " + respondingPlayer.getName();
            switch (specialType) {
                case CAT_IN_A_BAG:
                    playerWhoseScoreChanges = currentCatTargetPlayer; // Счет меняется у цели "Кота"
                    if (playerWhoseScoreChanges == null) {
                        System.err.println("КРИТИЧЕСКАЯ ОШИБКА: Ответ на 'Кота', но цель (currentCatTargetPlayer) не установлена!");
                        playerWhoseScoreChanges = respondingPlayer; // Аварийный вариант - меняем счет отвечающего
                    }
                    actionSourceDescription += " (для " + (playerWhoseScoreChanges != null ? playerWhoseScoreChanges.getName() : "???") + ")";
                    break;
                case AUCTION:
                    playerWhoseScoreChanges = currentAuctionWinner; // Счет меняется у победителя аукциона
                    if (playerWhoseScoreChanges == null) {
                        System.err.println("КРИТИЧЕСКАЯ ОШИБКА: Ответ на 'Аукцион', но победитель (currentAuctionWinner) не установлен!");
                        // В этом случае, вероятно, лучше не менять ничей счет
                        playerWhoseScoreChanges = null;
                        // Можно завершить вопрос сразу же, так как произошла ошибка
                        // handleSpecialQuestionError("Ошибка аукциона: победитель не определен.", true);
                        // return;
                    }
                    break;
                case NONE:
                default:
                    playerWhoseScoreChanges = respondingPlayer; // Счет меняется у отвечающего
                    break;
            }
        } else { // Ведущий нажал "Никто / Снять вопрос"
            actionSourceDescription = "Ведущий (Никто/Снять)";
            playerWhoseScoreChanges = null; // Ничей счет не меняется
        }

        System.out.println("Обработка ответа: Инициатор=" + actionSourceDescription +
                ", Фактический игрок=" + (playerWhoseScoreChanges != null ? playerWhoseScoreChanges.getName() : "N/A") +
                ", Правильно=" + correct);

        // 4. Определяем очки и завершенность вопроса
        int points = (specialType == Question.SpecialType.AUCTION && playerWhoseScoreChanges != null)
                ? currentAuctionBet // Используем ставку для победителя аукциона
                : questionBeingProcessed.getPoints(); // Используем номинал в остальных случаях
        boolean questionFinished = false; // Завершился ли розыгрыш этого вопроса?

        if (playerWhoseScoreChanges != null) { // Если был игрок, чей счет нужно изменить
            if (correct) {
                playerWhoseScoreChanges.addScore(points);
                System.out.println("  Игроку " + playerWhoseScoreChanges.getName() + " начислено " + points + " очков. Текущий счет: " + playerWhoseScoreChanges.getScore());
                questionFinished = true; // Правильный ответ всегда завершает вопрос
            } else {
                playerWhoseScoreChanges.subtractScore(points);
                System.out.println("  У игрока " + playerWhoseScoreChanges.getName() + " вычтено " + points + " очков. Текущий счет: " + playerWhoseScoreChanges.getScore());
                // Завершаем спецвопросы после неправильного ответа, но не обычные
                questionFinished = (specialType != Question.SpecialType.NONE);
            }
            // Обновляем счет в UI *немедленно* после изменения
            updatePlayersScoresUI();

        } else { // Если ведущий нажал "Никто"
            System.out.println("  Вопрос завершен без изменения счета.");
            questionFinished = true; // Вопрос считается завершенным
        }

        // 5. Логика после обработки ответа
        if (questionFinished) {
            System.out.println("  Вопрос (" + questionBeingProcessed.getQuestionText().substring(0,Math.min(20, questionBeingProcessed.getQuestionText().length())) + "...) считается завершенным.");

            // Отмечаем вопрос как отвеченный в модели данных текущего раунда
            // и отключаем кнопку на досках
            markQuestionAsAnsweredInCurrentRoundByQuestion(questionBeingProcessed);

            // Сбрасываем текущий вопрос и связанные переменные состояния
            currentQuestion = null;
            currentCatTargetPlayer = null;
            currentAuctionWinner = null;
            currentAuctionBet = 0;

            // Очищаем UI вопроса/ответа у ведущего и игрока в потоке EDT
            SwingUtilities.invokeLater(() -> {
                System.out.println("  Scheduling UI clear for Host and Player frames after question finished.");
                if (hostFrame != null) {
                    hostFrame.clearQuestionArea(); // Очищает текст, аудио, кнопки ответа у ведущего
                }
                if (playerFrame != null) {
                    playerFrame.clearQuestionArea(); // Очищает ТОЛЬКО текст вопроса у игрока

                    // Закрываем диалог картинки у игрока, ЕСЛИ ответ был ПРАВИЛЬНЫЙ
                    if (correct && questionType == Question.QuestionType.IMAGE) {
                        System.out.println("  Scheduling force close of image dialog for player (correct answer).");
                        playerFrame.forceCloseImageDialog();
                    } else if (!correct && questionType == Question.QuestionType.IMAGE) {
                        System.out.println("  Incorrect answer for image question, image dialog remains open.");
                    }
                }
                // Проверяем, завершен ли раунд ПОСЛЕ всех действий
                checkRoundCompletion();
            });
        } else {
            // Вопрос не завершен (был неправильный ответ на ОБЫЧНЫЙ вопрос)
            System.out.println("  Вопрос не завершен, ожидается ответ другого игрока или решение ведущего.");
            // Диалог картинки у игрока (если был) остается открытым.
            // Опционально: деактивировать кнопки для только что ответившего игрока у ведущего
            if (hostFrame != null && respondingPlayer != null) {
                // TODO: Реализовать метод disableAnswerButtonsForPlayer в HostFrame, если нужно
                // hostFrame.disableAnswerButtonsForPlayer(respondingPlayer);
                System.out.println("  (TODO: Disable answer buttons for player " + respondingPlayer.getName() + " in HostFrame)");
            }
        }
        System.out.println("processAnswer finished.");
    } // Конец метода processAnswer

    /** Переключает состояние паузы. */
    public void togglePause() {
        isPaused = !isPaused;
        System.out.println("Игра " + (isPaused ? "поставлена на паузу." : "снята с паузы."));

        // Обновляем UI в EDT
        SwingUtilities.invokeLater(() -> {
            if (hostFrame != null) {
                hostFrame.updatePauseState(isPaused);
                if (hostFrame.getGameBoardPanel() != null) {
                    hostFrame.getGameBoardPanel().setBoardEnabled(!isPaused, currentRoundInfo.answeredQuestionsInRound);
                }
            }
            if (playerFrame != null) {
                playerFrame.updatePauseState(isPaused);
                if (playerFrame.getGameBoardPanel() != null) {
                    playerFrame.getGameBoardPanel().setBoardEnabled(!isPaused, currentRoundInfo.answeredQuestionsInRound);
                }
            }
        });
    }

    /** Завершает игру и возвращает в главное меню. */
    public void finishGameAndGoToMenu() {
        System.out.println("Завершение игры и возврат в главное меню...");
        // Сбросить состояние игры
        isPaused = false;
        currentQuestion = null;
        currentRoundIndex = -1;
        currentRoundInfo = null;
        // Закрыть игровые окна
        closeGameWindows();
        // Запустить главное меню снова
        MainMenuFrame.run();
    }

    /** Завершает приложение. */
    public void exitApplication() {
        System.out.println("Выход из приложения...");
        // Можно добавить сохранение состояния перед выходом
        closeGameWindows(); // Закрыть окна перед выходом
        System.exit(0);
    }


    // --- ЛОГИКА СПЕЦВОПРОСОВ ---

    /** Запускает логику "Кота в мешке". */
    private void startCatInABag(Question question) {
        // Выполняем взаимодействие с UI в EDT
        SwingUtilities.invokeLater(() -> {
            if (hostFrame == null) {
                handleSpecialQuestionError("Ошибка: Окно ведущего не найдено для 'Кота в мешке'.", true);
                return;
            }

            List<Player> availablePlayers = getPlayers(); // Получаем копию списка
            if (availablePlayers.isEmpty()) {
                handleSpecialQuestionError("Нет игроков для выбора в 'Коте в мешке'!", true);
                return;
            }

            String[] playerNames = availablePlayers.stream()
                    .map(Player::getName).filter(Objects::nonNull).toArray(String[]::new);

            if (playerNames.length == 0) {
                handleSpecialQuestionError("Не удалось получить имена игроков для выбора.", true);
                return;
            }

            // Диалог выбора игрока
            String chosenPlayerName = (String) JOptionPane.showInputDialog(
                    hostFrame,
                    "КОТ В МЕШКЕ!\nВопрос (Номинал: " + question.getPoints() + ")\nВыберите игрока, которому отдаете вопрос:",
                    "Кот в мешке",
                    JOptionPane.QUESTION_MESSAGE, null, playerNames, playerNames[0]);

            if (chosenPlayerName != null) {
                Player targetPlayer = findPlayerByName(chosenPlayerName);
                if (targetPlayer != null) {
                    System.out.println("'Кот в мешке' отдан игроку: " + targetPlayer.getName());
                    currentCatTargetPlayer = targetPlayer; // Запоминаем, кому отдали
                    displayCurrentQuestionUI(); // Отображаем вопрос ПОСЛЕ выбора игрока
                    hostFrame.setupSpecificAnswerButtons(targetPlayer); // Кнопки только для него
                } else {
                    handleSpecialQuestionError("Выбранный игрок '" + chosenPlayerName + "' не найден.", true);
                }
            } else {
                handleSpecialQuestionError("'Кот в мешке' отменен ведущим.", true); // Отмена = вопрос сгорает
            }
        });
    }


    /** Запускает логику "Аукциона". */
    private void startAuction(Question question) {
        currentAuctionWinner = null; // Сброс предыдущего победителя
        currentAuctionBet = 0;    // Сброс ставки
        final int nominalPoints = question.getPoints(); // Номинал вопроса

        SwingUtilities.invokeLater(() -> { // UI операции в EDT
            if (hostFrame == null) {
                handleSpecialQuestionError("Ошибка: Окно ведущего не найдено для 'Аукциона'.", true);
                return;
            }

            List<Player> eligiblePlayers = getPlayers().stream()
                    .filter(p -> p.getScore() > 0) // Только игроки с положительным счетом могут ставить
                    .collect(Collectors.toList());

            if (eligiblePlayers.isEmpty()) {
                handleSpecialQuestionError("Нет игроков с положительным счетом для участия в 'Аукционе'. Вопрос сгорает.", true);
                return;
            }

            // --- Логика сбора ставок (Упрощенная версия с JOptionPane) ---
            Map<Player, Integer> bids = new HashMap<>();
            int maxBid = 0;
            Player highestBidder = null;
            boolean leaderPassed = false; // Флаг, спасовал ли текущий лидер

            JOptionPane.showMessageDialog(hostFrame,
                    "!!! АУКЦИОН !!!\nНоминал вопроса: " + nominalPoints +
                            "\nСейчас будут приниматься ставки (минимальная ставка: " + nominalPoints + ").",
                    "Аукцион", JOptionPane.INFORMATION_MESSAGE);

            // Последовательный опрос игроков (можно заменить на более интерактивный UI)
            for (Player player : eligiblePlayers) {
                // Пропускаем, если игрок не может перебить текущую макс. ставку или поставить номинал
                if ((highestBidder != null && player.getScore() <= maxBid) ||
                        (highestBidder == null && player.getScore() < nominalPoints)) {
                    System.out.println("  Аукцион: Игрок " + player.getName() + " не может сделать ставку (Счет: " + player.getScore() + ", Макс. ставка: " + maxBid + ")");
                    continue;
                }

                // Проверяем, не спасовал ли лидер (если он есть) в свой ход
                if (player == highestBidder) {
                    leaderPassed = false; // Лидер дошел до своего хода, он может ставить
                }

                // Формируем сообщение для диалога
                String currentMaxBidInfo = "Текущая макс. ставка: " + maxBid +
                        (highestBidder != null ? " (" + highestBidder.getName() + ")" : " (нет)");
                int minAllowedBid = Math.max(nominalPoints, maxBid + 1); // Мин. ставка: номинал или макс+1
                int maxPossibleBid = player.getScore(); // Макс. ставка игрока
                String promptMessage = "Игрок: " + player.getName() + " (Счет: " + player.getScore() + ")\n" +
                        currentMaxBidInfo + "\n" +
                        "Ваша ставка (мин: " + minAllowedBid + ", макс: " + maxPossibleBid + ")\n" +
                        "Введите сумму или 'Пас':";

                String bidInput = JOptionPane.showInputDialog(hostFrame, promptMessage, "Аукцион - Ставка", JOptionPane.QUESTION_MESSAGE);

                // Обработка ввода
                if (bidInput == null || bidInput.trim().equalsIgnoreCase("пас")) {
                    System.out.println("  Аукцион: Игрок " + player.getName() + " пасует.");
                    if (player == highestBidder) {
                        leaderPassed = true; // Лидер спасовал
                        System.out.println("  Аукцион: Текущий лидер спасовал!");
                        // Можно сразу завершить аукцион, если больше нет претендентов, но для простоты идем дальше
                    }
                    continue; // Следующий игрок
                }

                try {
                    int bid = Integer.parseInt(bidInput.trim());
                    // Проверка корректности ставки
                    if (bid < minAllowedBid) {
                        showWarningDialog(hostFrame,"Ставка (" + bid + ") должна быть не меньше " + minAllowedBid, "Неверная ставка");
                        continue; // Даем шанс следующему (или можно повторить ввод)
                    }
                    if (bid > maxPossibleBid) {
                        showWarningDialog(hostFrame,"Ставка (" + bid + ") не может превышать ваш счет (" + maxPossibleBid + ")", "Неверная ставка");
                        continue;
                    }

                    // Ставка принята
                    System.out.println("  Аукцион: Игрок " + player.getName() + " ставит " + bid);
                    bids.put(player, bid); // Сохраняем ставку (хотя в этой логике не используется)
                    if (bid > maxBid) {
                        maxBid = bid;
                        highestBidder = player;
                        leaderPassed = false; // Новый лидер еще не пасовал
                        System.out.println("  Аукцион: Новый лидер - " + player.getName() + " со ставкой " + maxBid);
                    }
                } catch (NumberFormatException nfe) {
                    showWarningDialog(hostFrame, "Неверный ввод. Введите число (ставку) или слово 'Пас'.", "Ошибка ввода");
                    continue; // Даем шанс следующему
                }
            } // Конец цикла опроса игроков

            // --- Определение победителя аукциона ---
            if (highestBidder != null && !leaderPassed) {
                final Player winner = highestBidder;
                final int finalBid = maxBid;
                currentAuctionWinner = winner; // Запоминаем победителя
                currentAuctionBet = finalBid; // Запоминаем ставку

                System.out.println("Аукцион выиграл: " + winner.getName() + " со ставкой " + finalBid);
                // НЕ МЕНЯЕМ СТОИМОСТЬ ВОПРОСА ЗДЕСЬ, используем currentAuctionBet в processAnswer

                // Отображаем вопрос и кнопки только для победителя
                displayCurrentQuestionUI();
                hostFrame.setupSpecificAnswerButtons(winner);

            } else {
                // Никто не сделал ставку или лидер спасовал
                handleSpecialQuestionError("Аукцион не состоялся (нет ставок или лидер спасовал). Вопрос сгорает.", true);
            }
        }); // Конец invokeLater
    }


    /** Обрабатывает ошибку спецвопроса (сообщение + сброс). */
    private void handleSpecialQuestionError(String message, boolean resetQuestionState) {
        System.err.println("Ошибка спецвопроса: " + message);
        showErrorDialog(hostFrame, message, "Специальный вопрос");

        if (resetQuestionState) {
            // Найти координаты текущего вопроса и отметить как отвеченный (сгорел)
            if (currentQuestion != null) {
                markQuestionAsAnsweredInCurrentRoundByQuestion(currentQuestion);
            }

            // Сбросить текущее состояние
            currentQuestion = null;
            currentCatTargetPlayer = null;
            currentAuctionWinner = null;
            currentAuctionBet = 0;

            // Очистить UI вопроса в EDT
            SwingUtilities.invokeLater(() -> {
                if (hostFrame != null) hostFrame.clearQuestionArea();
                if (playerFrame != null) playerFrame.clearQuestionArea();
                // Проверить завершение раунда
                checkRoundCompletion();
            });
        }
    }

    // --- МЕТОДЫ РАБОТЫ С СОСТОЯНИЕМ РАУНДА ---

    /** Проверяет, все ли вопросы в текущем раунде отвечены. */
    private void checkRoundCompletion() {
        if (currentRoundInfo == null || currentRoundInfo.answeredQuestionsInRound == null) {
            System.err.println("Ошибка проверки завершения раунда: нет данных о раунде.");
            return;
        }

        boolean allAnswered = true;
        for (int i = 0; i < currentRoundInfo.themes.size(); i++) {
            Theme theme = currentRoundInfo.themes.get(i);
            if (theme == null || theme.getQuestions() == null) continue; // Пропускаем пустые темы

            // Проверяем размер массива ответов для этой темы
            if (i >= currentRoundInfo.answeredQuestionsInRound.length ||
                    currentRoundInfo.answeredQuestionsInRound[i] == null) {
                System.err.println("Ошибка: Некорректный массив ответов для темы " + i + " в раунде " + (currentRoundIndex + 1));
                allAnswered = false; // Считаем не завершенным при ошибке структуры
                break;
            }

            for (int j = 0; j < theme.getQuestions().size(); j++) {
                // Проверяем размер подмассива и сам флаг ответа
                if (j >= currentRoundInfo.answeredQuestionsInRound[i].length) {
                    System.err.println("Ошибка: Некорректный размер массива ответов для темы " + i + ", вопрос " + j);
                    allAnswered = false;
                    break;
                }
                if (!currentRoundInfo.answeredQuestionsInRound[i][j]) {
                    allAnswered = false; // Найден неотвеченный вопрос
                    break;
                }
            }
            if (!allAnswered) break; // Выходим из внешнего цикла, если нашли неотвеченный
        }

        if (allAnswered) {
            System.out.println("--- РАУНД " + getCurrentRoundNumber() + " ЗАВЕРШЕН ---");
            // Активируем кнопку "Следующий раунд" у ведущего
            if (hostFrame != null) {
                hostFrame.setNextRoundButtonEnabled(true);
            }
        }
    }

    /** Отмечает вопрос как отвеченный в текущем раунде по индексам. */
    private void markQuestionAsAnsweredInCurrentRound(int themeIndex, int questionIndex) {
        if (isValidQuestionIndexInCurrentRound(themeIndex, questionIndex)) {
            if (!currentRoundInfo.answeredQuestionsInRound[themeIndex][questionIndex]) {
                currentRoundInfo.answeredQuestionsInRound[themeIndex][questionIndex] = true;
                System.out.println("  Вопрос [" + themeIndex + "][" + questionIndex + "] отмечен как отвеченный.");
                // Отключаем кнопку на досках
                disableButtonOnBoardsInCurrentRound(themeIndex, questionIndex);
            } else {
                // Уже был отмечен, ничего не делаем
                System.out.println("  Вопрос [" + themeIndex + "][" + questionIndex + "] уже был отмечен.");
            }
        } else {
            System.err.println("Попытка отметить вопрос с неверными индексами: T=" + themeIndex + ", Q=" + questionIndex);
        }
    }

    /** Находит вопрос в текущем раунде и отмечает его как отвеченный. */
    private void markQuestionAsAnsweredInCurrentRoundByQuestion(Question q) {
        if (q == null || currentRoundInfo == null || currentRoundInfo.themes == null) return;

        for (int t = 0; t < currentRoundInfo.themes.size(); t++) {
            Theme theme = currentRoundInfo.themes.get(t);
            if (theme != null && theme.getQuestions() != null) {
                int qIdx = theme.getQuestions().indexOf(q); // Ищем вопрос в списке темы
                if (qIdx != -1) {
                    // Нашли вопрос, отмечаем его
                    markQuestionAsAnsweredInCurrentRound(t, qIdx);
                    return; // Выходим, как только нашли и отметили
                }
            }
        }
        // Если дошли сюда, вопрос не найден в текущем раунде
        System.err.println("Не удалось найти вопрос для отметки в текущем раунде: " + q);
    }

    /** Проверяет валидность индексов темы и вопроса в текущем раунде. */
    private boolean isValidQuestionIndexInCurrentRound(int themeIndex, int questionIndex) {
        if (currentRoundInfo == null || currentRoundInfo.themes == null || currentRoundInfo.answeredQuestionsInRound == null) {
            return false; // Нет данных о раунде
        }
        // Проверка индекса темы
        if (themeIndex < 0 || themeIndex >= currentRoundInfo.themes.size() ||
                themeIndex >= currentRoundInfo.answeredQuestionsInRound.length ||
                currentRoundInfo.answeredQuestionsInRound[themeIndex] == null) {
            return false;
        }
        Theme theme = currentRoundInfo.themes.get(themeIndex);
        if (theme == null || theme.getQuestions() == null) {
            return false; // Темы нет или у нее нет списка вопросов
        }
        // Проверка индекса вопроса
        return questionIndex >= 0 && questionIndex < theme.getQuestions().size() &&
                questionIndex < currentRoundInfo.answeredQuestionsInRound[themeIndex].length;
    }

    /** Отключает кнопку на досках ведущего и игрока. */
    private void disableButtonOnBoardsInCurrentRound(int themeIndexInRound, int questionIndexInTheme) {
        SwingUtilities.invokeLater(() -> { // Обновление UI в EDT
            if (hostFrame != null && hostFrame.getGameBoardPanel() != null) {
                hostFrame.getGameBoardPanel().disableButton(themeIndexInRound, questionIndexInTheme);
            }
            if (playerFrame != null && playerFrame.getGameBoardPanel() != null) {
                playerFrame.getGameBoardPanel().disableButton(themeIndexInRound, questionIndexInTheme);
            }
            System.out.println("  Disabled button on boards for [" + themeIndexInRound + "][" + questionIndexInTheme + "]");
        });
    }


    // --- МЕТОДЫ ДЛЯ UI И ОТОБРАЖЕНИЯ ---

    /** Отображает текущий вопрос в UI ведущего и игрока. */
    private void displayCurrentQuestionUI() {
        if (currentQuestion != null) {
            SwingUtilities.invokeLater(() -> { // Обновление UI в EDT
                if (hostFrame != null) hostFrame.displayQuestion(currentQuestion);
                if (playerFrame != null) playerFrame.displayQuestion(currentQuestion);
            });
        } else {
            System.err.println("displayCurrentQuestionUI вызван, когда currentQuestion is null!");
        }
    }

    /** Обновляет счет игроков в UI. */
    private void updatePlayersScoresUI() {
        SwingUtilities.invokeLater(() -> { // Обновление UI в EDT
            if (hostFrame != null) hostFrame.updatePlayerScores(this.players);
            if (playerFrame != null) playerFrame.updatePlayerScores(this.players);
        });
    }

    /** Показывает финальные результаты игры. */
    private void showFinalResults() {
        SwingUtilities.invokeLater(() -> { // Показ диалога в EDT
            if (hostFrame != null) { // Результаты показываем только у ведущего
                // Сортируем игроков по очкам (по убыванию)
                List<Player> sortedPlayers = new ArrayList<>(this.players);
                sortedPlayers.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));

                // Формируем HTML строку для отображения
                StringBuilder resultsHtml = new StringBuilder("<html><head><style>" +
                        "table { border-collapse: collapse; margin: 10px; } " +
                        "th, td { border: 1px solid #ccc; padding: 6px 10px; text-align: left; } " +
                        "th { background-color: #f2f2f2; } " +
                        ".score { text-align: right; } " +
                        "</style></head><body>" +
                        "<h2>Игра Окончена! Результаты:</h2><table>" +
                        "<tr><th>Место</th><th>Игрок</th><th>Очки</th></tr>");

                for (int i = 0; i < sortedPlayers.size(); i++) {
                    Player p = sortedPlayers.get(i);
                    resultsHtml.append("<tr>")
                            .append("<td>").append(i + 1).append(".</td>")
                            .append("<td>").append(p.getName()).append("</td>")
                            .append("<td class='score'>").append(p.getScore()).append("</td>")
                            .append("</tr>");
                }
                resultsHtml.append("</table></body></html>");

                // Показываем диалог с результатами и опциями
                Object[] options = {"В главное меню", "Выход из приложения"};
                int choice = JOptionPane.showOptionDialog(hostFrame,
                        resultsHtml.toString(),
                        "Результаты Игры",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null, options, options[0]);

                if (choice == 0) {
                    finishGameAndGoToMenu();
                } else if (choice == 1) {
                    exitApplication();
                } else {
                    // Если диалог просто закрыли, можно вернуться в меню по умолчанию
                    finishGameAndGoToMenu();
                }
            } else {
                // Если окна ведущего нет, просто завершаем игру
                System.out.println("Игра завершена, но окно ведущего не найдено для показа результатов.");
                exitApplication(); // Или finishGameAndGoToMenu()
            }
        });
    }

    /** Безопасно закрывает игровые окна. */
    private void closeGameWindows() {
        System.out.println("Закрытие игровых окон...");
        SwingUtilities.invokeLater(() -> {
            if (hostFrame != null) {
                hostFrame.dispose(); // Вызовет stopAndCloseCurrentAudio внутри
                hostFrame = null;
            }
            if (playerFrame != null) {
                playerFrame.dispose();
                playerFrame = null;
            }
        });
        // Даем время на закрытие окон перед следующим действием
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // --- Вспомогательные методы для диалогов ---
    private void showInfoDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
    private void showWarningDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
    }
    private void showErrorDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    // --- ГЕТТЕРЫ для доступа из UI (используются ограниченно) ---

    /** Возвращает КОПИЮ списка игроков. */
    public List<Player> getPlayers() {
        return new ArrayList<>(players); // Возвращаем копию для защиты оригинала
    }

    public Question getCurrentQuestion() {
        return currentQuestion;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public int getCurrentRoundNumber() {
        return currentRoundIndex + 1; // Номера раундов для пользователя начинаются с 1
    }

    public String getCurrentRoundName() {
        return (currentRoundInfo != null) ? currentRoundInfo.name : null;
    }

    /** Возвращает НЕИЗМЕНЯЕМЫЙ список тем ТЕКУЩЕГО раунда. */
    public List<Theme> getThemesForCurrentRound() {
        return (currentRoundInfo != null && currentRoundInfo.themes != null)
                ? Collections.unmodifiableList(currentRoundInfo.themes)
                : Collections.emptyList();
    }

    /** Возвращает КОПИЮ массива отвеченных вопросов ТЕКУЩЕГО раунда. */
    public boolean[][] getAnsweredQuestionsForCurrentRound() {
        if (currentRoundInfo == null || currentRoundInfo.answeredQuestionsInRound == null) {
            return new boolean[0][0]; // Возвращаем пустой массив, если раунд неактивен
        }
        // Создаем глубокую копию двумерного массива
        boolean[][] answered = currentRoundInfo.answeredQuestionsInRound;
        boolean[][] copy = new boolean[answered.length][];
        for (int i = 0; i < answered.length; i++) {
            if (answered[i] != null) {
                copy[i] = Arrays.copyOf(answered[i], answered[i].length);
            } else {
                copy[i] = null; // Или new boolean[0]; в зависимости от логики
            }
        }
        return copy;
    }

    // --- ВНУТРЕННИЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ---

    /**
     * Назначает случайным образом указанное количество "Котов" и "Аукционов"
     * на НЕ СПЕЦИАЛЬНЫЕ вопросы в заданном списке тем.
     * Модифицирует переданный список тем.
     * @param themesInRound Список тем для модификации (должен быть КОПИЕЙ).
     * @param numCatsToAssign Желаемое количество "Котов".
     * @param numAuctionsToAssign Желаемое количество "Аукционов".
     * @return Point, где x - реально назначенное кол-во "Котов", y - "Аукционов".
     */
    private Point assignSpecialQuestionsToThemes(List<Theme> themesInRound,
                                                 int numCatsToAssign, int numAuctionsToAssign) {
        if (themesInRound == null || themesInRound.isEmpty()) {
            System.out.println("  assignSpecialQuestions: No themes provided.");
            return new Point(0, 0);
        }

        // 1. Собрать координаты всех ОБЫЧНЫХ (NONE) вопросов в раунде
        List<Point> availableCoords = new ArrayList<>();
        int totalNormalQuestions = 0;
        for (int t = 0; t < themesInRound.size(); t++) {
            Theme theme = themesInRound.get(t);
            if (theme != null && theme.getQuestions() != null) {
                for (int q = 0; q < theme.getQuestions().size(); q++) {
                    Question question = theme.getQuestions().get(q);
                    // Добавляем только если вопрос существует и имеет тип NONE
                    if (question != null && question.getSpecialType() == Question.SpecialType.NONE) {
                        availableCoords.add(new Point(t, q)); // Сохраняем индексы темы и вопроса
                        totalNormalQuestions++;
                    }
                }
            }
        }

        // 2. Проверка, есть ли куда назначать и что назначать
        if (availableCoords.isEmpty() || (numCatsToAssign <= 0 && numAuctionsToAssign <= 0)) {
            System.out.println("  assignSpecialQuestions: No available normal questions or nothing to assign.");
            return new Point(0, 0);
        }

        // 3. Корректировка количества назначаемых вопросов (не больше доступных)
        int totalToAssign = numCatsToAssign + numAuctionsToAssign;
        if (totalToAssign > totalNormalQuestions) {
            System.out.println("  Warning: Requested more special questions (" + totalToAssign +
                    ") than available normal questions (" + totalNormalQuestions + "). Adjusting counts.");
            // Пропорционально уменьшаем (или можно другую логику)
            double ratio = (double) totalNormalQuestions / totalToAssign;
            numCatsToAssign = (int) Math.round(numCatsToAssign * ratio);
            numAuctionsToAssign = totalNormalQuestions - numCatsToAssign; // Остальное - аукционы
        }
        numCatsToAssign = Math.max(0, numCatsToAssign); // Гарантируем >= 0
        numAuctionsToAssign = Math.max(0, numAuctionsToAssign);

        // 4. Перемешать доступные координаты
        Collections.shuffle(availableCoords, random);

        // 5. Назначить Котов
        int assignedCats = 0;
        int currentIndex = 0;
        while (assignedCats < numCatsToAssign && currentIndex < availableCoords.size()) {
            Point coord = availableCoords.get(currentIndex++);
            try {
                Question qToModify = themesInRound.get(coord.x).getQuestions().get(coord.y);
                qToModify.setSpecialType(Question.SpecialType.CAT_IN_A_BAG);
                assignedCats++;
            } catch (Exception e) { // На случай ошибок индексации
                System.err.println("Error assigning Cat to coord " + coord + ": " + e.getMessage());
            }
        }

        // 6. Назначить Аукционы (начиная с оставшихся координат)
        int assignedAuctions = 0;
        while (assignedAuctions < numAuctionsToAssign && currentIndex < availableCoords.size()) {
            Point coord = availableCoords.get(currentIndex++);
            try {
                Question qToModify = themesInRound.get(coord.x).getQuestions().get(coord.y);
                // Убедимся, что не назначаем поверх Кота (хотя не должны были)
                if (qToModify.getSpecialType() == Question.SpecialType.NONE) {
                    qToModify.setSpecialType(Question.SpecialType.AUCTION);
                    assignedAuctions++;
                }
            } catch (Exception e) {
                System.err.println("Error assigning Auction to coord " + coord + ": " + e.getMessage());
            }
        }

        System.out.println("  assignSpecialQuestions Result: Assigned Cats=" + assignedCats + ", Auctions=" + assignedAuctions);
        return new Point(assignedCats, assignedAuctions); // Возвращаем реально назначенное количество
    }


    /** Находит игрока по имени (без учета регистра). */
    private Player findPlayerByName(String name) {
        if (name == null || name.isBlank()) return null;
        for (Player player : this.players) {
            if (name.equalsIgnoreCase(player.getName())) {
                return player;
            }
        }
        return null; // Не найден
    }

} // Конец класса GameController