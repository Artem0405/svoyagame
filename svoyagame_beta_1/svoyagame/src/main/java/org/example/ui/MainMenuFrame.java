package org.example.ui; // Убедитесь, что пакет правильный

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainMenuFrame extends JFrame {

    public MainMenuFrame() {
        setTitle("Своя Игра - Главное Меню");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Закрытие окна завершает программу
        setSize(450, 200); // Немного шире для трех кнопок
        setLocationRelativeTo(null); // Окно по центру экрана
        setLayout(new FlowLayout(FlowLayout.CENTER, 20, 40)); // Простое расположение кнопок

        // --- Кнопка Создать Пак ---
        JButton createPackButton = new JButton("Создать пак вопросов");
        createPackButton.setFont(new Font("Arial", Font.PLAIN, 16));
        createPackButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Передаем ссылку на текущее окно (this) в конструктор PackCreatorFrame
                PackCreatorFrame packCreator = new PackCreatorFrame(MainMenuFrame.this); // Передаем this
                packCreator.setVisible(true);
                MainMenuFrame.this.setVisible(false); // Скрываем главное меню
            }
        });

        // --- Кнопка Начать Игру ---
        // --- Кнопка Начать Игру ---
        JButton startGameButton = new JButton("Начать игру");
        startGameButton.setFont(new Font("Arial", Font.PLAIN, 16));
// Вот этот ActionListener должен быть таким:
        startGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Создаем и показываем окно настройки игры, передавая ссылку на главное меню
                GameSetupFrame setupFrame = new GameSetupFrame(MainMenuFrame.this); // <-- Создаем GameSetupFrame
                setupFrame.setVisible(true);                                      // <-- Показываем его
                MainMenuFrame.this.setVisible(false);                             // <-- Скрываем главное меню
            }
        });

        // --- Кнопка Выход --- (Добавлена)
        JButton exitButton = new JButton("Выход");
        exitButton.setFont(new Font("Arial", Font.PLAIN, 16));
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Подтверждение выхода
                int choice = JOptionPane.showConfirmDialog(MainMenuFrame.this,
                        "Вы уверены, что хотите выйти?",
                        "Подтверждение выхода",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    System.exit(0); // Завершить приложение
                }
            }
        });

        // Добавляем кнопки на панель
        add(createPackButton);
        add(startGameButton);
        add(exitButton); // Добавлена кнопка Выход
    }

    // Метод для запуска главного меню (вызывается из Main.java)
    public static void run() {
        // Запуск создания GUI в Event Dispatch Thread (EDT) - важно для Swing!
        SwingUtilities.invokeLater(() -> {
            MainMenuFrame frame = new MainMenuFrame();
            frame.setVisible(true);
        });
    }
}