package org.example.ui.components;

import org.example.data.Player;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerScorePanel extends JPanel {

    private List<Player> players;
    private Map<String, JLabel> scoreLabels;

    // Сохраняем ссылки на nameLabels для возможного обновления имен (хотя обычно не нужно)
    private Map<String, JLabel> nameLabels;

    public PlayerScorePanel() { // Конструктор без аргументов, данные придут позже
        this.scoreLabels = new HashMap<>();
        this.nameLabels = new HashMap<>();

        // Используем BoxLayout для вертикального расположения строк игроков
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("Счет игроков"));
        // Начальное состояние - пустое или с надписью "Ожидание игроков"
        add(new JLabel("Ожидание инициализации...", SwingConstants.CENTER));
    }

    // Метод для первичной установки игроков и отрисовки
    public void setPlayers(List<Player> initialPlayers) {
        this.players = initialPlayers;
        initializeUI();
    }

    private void initializeUI() {
        this.removeAll(); // Очищаем панель
        scoreLabels.clear();
        nameLabels.clear();

        if (players != null && !players.isEmpty()) {
            for (Player player : players) {
                // Создаем панель для одной строки (имя + счет) с FlowLayout
                JPanel playerRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2)); // Отступы hgap=10, vgap=2
                playerRowPanel.setAlignmentX(Component.LEFT_ALIGNMENT); // Выравнивание для BoxLayout
                // Убираем фон у вложенной панели, чтобы она не выделялась
                playerRowPanel.setOpaque(false);


                JLabel nameLabel = new JLabel(player.getName() + ":");
                nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
                nameLabels.put(player.getName(), nameLabel);

                JLabel scoreLabel = new JLabel(String.valueOf(player.getScore()));
                scoreLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                scoreLabel.setPreferredSize(new Dimension(60, scoreLabel.getPreferredSize().height)); // Задаем ширину для выравнивания
                scoreLabel.setHorizontalAlignment(SwingConstants.RIGHT); // Выравнивание счета по правому краю
                scoreLabels.put(player.getName(), scoreLabel);

                playerRowPanel.add(nameLabel);
                playerRowPanel.add(scoreLabel);

                this.add(playerRowPanel); // Добавляем строку игрока в основную панель
            }
        } else {
            // Если список игроков пуст после инициализации
            add(new JLabel("Нет игроков для отображения"));
        }
        // Перерисовываем панель, чтобы изменения вступили в силу
        this.revalidate();
        this.repaint();
    }

    public void updateScores() {
        if (players == null) return;

        SwingUtilities.invokeLater(() -> {
            boolean structureChanged = false;
            if (scoreLabels.size() != players.size()) {
                structureChanged = true;
            } else {
                for (Player player : players) {
                    JLabel label = scoreLabels.get(player.getName());
                    if (label != null) {
                        label.setText(String.valueOf(player.getScore()));
                        // Можно добавить изменение цвета при отрицательном счете
                        if(player.getScore() < 0) {
                            label.setForeground(Color.RED);
                        } else {
                            label.setForeground(UIManager.getColor("Label.foreground")); // Стандартный цвет
                        }
                    } else {
                        // Игрок есть, а лейбла нет - структура изменилась
                        structureChanged = true;
                        break;
                    }
                }
            }

            if (structureChanged) {
                System.out.println("Структура игроков изменилась, переинициализация PlayerScorePanel...");
                initializeUI(); // Полная перерисовка
            } else {
                // Просто перерисовываем для обновления текста/цвета
                this.revalidate();
                this.repaint();
            }
        });
    }
}