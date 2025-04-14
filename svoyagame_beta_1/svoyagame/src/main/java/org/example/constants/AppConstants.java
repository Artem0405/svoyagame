package org.example.constants;

import java.awt.Font;
import java.awt.Color; // Если переносите и цвета

public class AppConstants {
    // Шрифты
    public static final Font HEADER_FONT = new Font("Arial", Font.BOLD, 10);
    public static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 20);
    public static final Font PLAYER_NAME_FONT = new Font("Arial", Font.BOLD, 14);
    public static final Font PLAYER_SCORE_FONT = new Font("Arial", Font.PLAIN, 14);
    public static final Font QUESTION_AREA_FONT = new Font("Arial", Font.PLAIN, 18);
    public static final Font ANSWER_AREA_FONT = new Font("Arial", Font.BOLD, 18);
    public static final Font AUDIO_BUTTON_FONT = new Font("Arial", Font.BOLD, 16);
    public static final Font ROUND_INFO_FONT = HEADER_FONT; // Можно использовать другую константу
    public static final Font THEME_NAME_FONT = new Font("Arial", Font.BOLD, 14);

    // Цвета (Пример)
    public static final Color BOARD_BACKGROUND = new Color(0, 0, 100);
    public static final Color HEADER_TEXT_COLOR = Color.WHITE;
    public static final Color BUTTON_TEXT_COLOR = Color.YELLOW;
    // ... другие цвета ...
}