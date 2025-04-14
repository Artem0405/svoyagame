package org.example.data; // Убедитесь, что пакет правильный

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PackStorage {

    // Создаем экземпляр Gson один раз.
    // setPrettyPrinting() делает JSON-файл читаемым для человека (с отступами).
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting() // Для читаемости
            .serializeNulls()    // <-- ДОБАВЛЕНО: Включать null поля в JSON
            .create();

    // Директория для хранения паков по умолчанию (относительно корня проекта)
    // Вы можете изменить это на абсолютный путь или другое место
    private static final Path DEFAULT_PACKS_DIRECTORY = Paths.get("packs");

    // --- Метод для сохранения пака ---
    public static void savePack(QuestionPack pack, Path filePath) throws IOException {
        // Создаем директории, если они не существуют
        Files.createDirectories(filePath.getParent());

        // Используем try-with-resources для автоматического закрытия BufferedWriter
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            gson.toJson(pack, writer); // Сериализуем объект pack в JSON и пишем в файл
            System.out.println("Пак успешно сохранен в: " + filePath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении пака в файл " + filePath + ": " + e.getMessage());
            e.printStackTrace(); // Выводим стек ошибки для отладки
            throw e; // Пробрасываем исключение дальше, чтобы вызывающий код мог его обработать
        }
    }

    // --- Метод для загрузки пака ---
    public static QuestionPack loadPack(Path filePath) throws IOException, JsonSyntaxException {
        // Используем try-with-resources для автоматического закрытия BufferedReader
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            QuestionPack pack = gson.fromJson(reader, QuestionPack.class); // Десериализуем JSON из файла в объект QuestionPack
            System.out.println("Пак успешно загружен из: " + filePath.toAbsolutePath());
            return pack;
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла пака " + filePath + ": " + e.getMessage());
            e.printStackTrace();
            throw e; // Пробрасываем исключение
        } catch (JsonSyntaxException e) {
            System.err.println("Ошибка синтаксиса JSON в файле " + filePath + ": " + e.getMessage());
            e.printStackTrace();
            throw e; // Пробрасываем исключение
        }
    }

    // --- Метод для получения списка доступных паков ---
    // Возвращает список путей к файлам .json в указанной директории
    public static List<Path> listAvailablePacks(Path directoryPath) {
        // Убедимся, что директория существует, если нет - создадим
        if (!Files.exists(directoryPath)) {
            try {
                System.out.println("Директория для паков не найдена, создаю: " + directoryPath.toAbsolutePath());
                Files.createDirectories(directoryPath);
            } catch (IOException e) {
                System.err.println("Не удалось создать директорию для паков: " + e.getMessage());
                e.printStackTrace();
                return Collections.emptyList(); // Возвращаем пустой список в случае ошибки
            }
        }

        // Проверяем, что это действительно директория
        if (!Files.isDirectory(directoryPath)) {
            System.err.println("Указанный путь не является директорией: " + directoryPath);
            return Collections.emptyList();
        }


        // Используем try-with-resources для Stream<Path>, чтобы он был закрыт
        try (Stream<Path> stream = Files.list(directoryPath)) {
            return stream
                    .filter(Files::isRegularFile) // Выбираем только файлы (не директории)
                    .filter(path -> path.toString().toLowerCase().endsWith(".json")) // Выбираем только файлы с расширением .json
                    .collect(Collectors.toList()); // Собираем результат в список
        } catch (IOException e) {
            System.err.println("Ошибка при чтении директории паков " + directoryPath + ": " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList(); // Возвращаем пустой список в случае ошибки
        }
    }

    // --- Вспомогательный метод для получения пути к директории паков по умолчанию ---
    public static Path getDefaultPacksDirectory() {
        return DEFAULT_PACKS_DIRECTORY;
    }

    // --- (Опционально) Вспомогательный метод для получения полного пути к файлу пака по имени ---
    public static Path getPackPathByName(String packName) {
        // Добавляем .json, если имя файла его не содержит
        String filename = packName.toLowerCase().endsWith(".json") ? packName : packName + ".json";
        return DEFAULT_PACKS_DIRECTORY.resolve(filename);
    }

}