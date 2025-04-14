package org.example.data; // Убедитесь, что пакет правильный

import java.util.ArrayList;
import java.util.List;

public class QuestionPack {
    private String packName;
    // Инициализируем список сразу
    private List<Theme> themes = new ArrayList<>();

    // Конструкторы
    public QuestionPack(String packName) {
        this.packName = packName;
    }

    public QuestionPack() { // Пустой конструктор для Gson
    }

    // Геттеры и Сеттеры
    public String getPackName() {
        return packName;
    }

    public void setPackName(String packName) {
        this.packName = packName;
    }

    public List<Theme> getThemes() {
        return themes;
    }

    public void setThemes(List<Theme> themes) {
        this.themes = themes;
    }

    // (Опционально) Удобный метод для добавления темы
    public void addTheme(Theme theme) {
        if (this.themes == null) {
            this.themes = new ArrayList<>();
        }
        this.themes.add(theme);
    }

    // (Опционально) toString
    @Override
    public String toString() {
        return "QuestionPack{" +
                "name='" + packName + '\'' +
                ", numThemes=" + (themes != null ? themes.size() : 0) +
                '}';
    }
}