package org.example.data; // Убедитесь, что пакет правильный

public class Player {
    private String name;
    private int score; // Изначально счет 0

    // Конструктор
    public Player(String name) {
        this.name = name;
        this.score = 0; // Явно инициализируем нулем
    }

    public Player() { // Пустой конструктор
    }

    // Геттеры и Сеттеры
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getScore() {
        return score;
    }

    // Не просто сеттер, а методы для изменения счета
    public void setScore(int score) {
        this.score = score;
    }

    public void addScore(int points) {
        this.score += points;
    }

    public void subtractScore(int points) {
        this.score -= points;
    }


    // (Опционально) toString
    @Override
    public String toString() {
        return "Player{" +
                "name='" + name + '\'' +
                ", score=" + score +
                '}';
    }
}