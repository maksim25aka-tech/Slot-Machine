// slot.java
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class slot {
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[91m";
    private static final String GREEN = "\u001B[92m";
    private static final String YELLOW = "\u001B[93m";
    private static final String BLUE = "\u001B[94m";
    private static final String MAGENTA = "\u001B[95m";
    private static final String CYAN = "\u001B[96m";
    private static final String BOLD = "\u001B[1m";

    private static String colorize(String text, String color) {
        return color + text + RESET;
    }

    private static final String[] SYMBOLS = {"🍒", "🍋", "🍊", "🍇", "🔔", "💎", "7"};
    private static final String[] SYMBOL_COLORS = {RED, YELLOW, MAGENTA, MAGENTA, CYAN, CYAN, BOLD};
    private static final Map<String, Integer> SYMBOL_VALUES = new HashMap<>();
    static {
        SYMBOL_VALUES.put("🍒", 5);
        SYMBOL_VALUES.put("🍋", 5);
        SYMBOL_VALUES.put("🍊", 5);
        SYMBOL_VALUES.put("🍇", 8);
        SYMBOL_VALUES.put("🔔", 10);
        SYMBOL_VALUES.put("💎", 100);
        SYMBOL_VALUES.put("7", 50);
    }

    private static class Stats {
        int rolls;
        int total_won;
        int best_win;
        int balance;
    }

    private int balance;
    private int bet;
    private List<String[]> history = new ArrayList<>();
    private Stats stats;
    private String statsFile;
    private Scanner scanner;

    public slot(int initialBalance) {
        balance = initialBalance;
        statsFile = System.getProperty("user.home") + "/.slot_stats.json";
        loadStats();
        scanner = new Scanner(System.in);
    }

    private void loadStats() {
        stats = new Stats();
        try {
            String json = new String(Files.readAllBytes(Paths.get(statsFile)));
            // упрощённый парсинг
            stats.rolls = extractInt(json, "rolls");
            stats.total_won = extractInt(json, "total_won");
            stats.best_win = extractInt(json, "best_win");
            stats.balance = extractInt(json, "balance");
            if (stats.balance == 0 && stats.rolls == 0) stats.balance = 100;
        } catch (Exception e) {
            stats.balance = 100;
        }
        balance = stats.balance > 0 ? stats.balance : 100;
    }

    private int extractInt(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return 0;
        int start = json.indexOf(":", idx) + 1;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        try { return Integer.parseInt(json.substring(start, end).trim()); } catch (Exception e) { return 0; }
    }

    private void saveStats() {
        stats.balance = balance;
        String json = "{\"rolls\":" + stats.rolls + ",\"total_won\":" + stats.total_won +
                      ",\"best_win\":" + stats.best_win + ",\"balance\":" + stats.balance + "}";
        try { Files.write(Paths.get(statsFile), json.getBytes()); } catch (IOException e) {}
    }

    private String[] spin(int betAmount) {
        if (betAmount > balance) {
            System.out.println(colorize("Недостаточно средств.", RED));
            return null;
        }
        if (betAmount <= 0) {
            System.out.println(colorize("Ставка должна быть положительной.", RED));
            return null;
        }
        bet = betAmount;
        balance -= bet;
        System.out.println(colorize("Вращение...", BOLD));
        // Анимация
        Random rnd = ThreadLocalRandom.current();
        for (int i=0; i<3; i++) {
            String[] temp = new String[3];
            for (int j=0; j<3; j++) temp[j] = SYMBOLS[rnd.nextInt(SYMBOLS.length)];
            System.out.printf("\r%s %s %s", temp[0], temp[1], temp[2]);
            try { Thread.sleep(200); } catch (InterruptedException e) {}
        }
        // Результат
        String[] result = new String[3];
        for (int i=0; i<3; i++) result[i] = SYMBOLS[rnd.nextInt(SYMBOLS.length)];
        System.out.printf("\r%s %s %s\n", result[0], result[1], result[2]);
        int win = calculateWin(result);
        balance += win;
        stats.rolls++;
        stats.total_won += win;
        if (win > stats.best_win) stats.best_win = win;
        history.add(result);
        if (win > 0) {
            System.out.println(colorize("Вы выиграли " + win + " монет!", GREEN));
        } else {
            System.out.println(colorize("Ничего не выиграно.", RED));
        }
        saveStats();
        return result;
    }

    private int calculateWin(String[] result) {
        if (result[0].equals("💎") && result[1].equals("💎") && result[2].equals("💎")) {
            return bet * 100;
        }
        if (result[0].equals(result[1]) && result[1].equals(result[2])) {
            return bet * SYMBOL_VALUES.get(result[0]);
        }
        // Два одинаковых + джокер
        for (String sym : SYMBOLS) {
            if (sym.equals("💎")) continue;
            int count = 0;
            for (String s : result) if (s.equals(sym)) count++;
            if (count == 2 && contains(result, "💎")) {
                return bet * 2 * SYMBOL_VALUES.get(sym);
            }
        }
        if (contains(result, "💎")) {
            return bet * 2;
        }
        return 0;
    }

    private boolean contains(String[] arr, String val) {
        for (String s : arr) if (s.equals(val)) return true;
        return false;
    }

    private void displayBalance() {
        System.out.println(colorize("Баланс: " + balance + " монет", YELLOW));
    }

    private void displayStats() {
        System.out.println(colorize("📊 Статистика:", BOLD));
        System.out.println("  Вращений: " + stats.rolls);
        System.out.println("  Всего выиграно: " + stats.total_won);
        System.out.println("  Лучший выигрыш: " + stats.best_win);
        System.out.println("  Текущий баланс: " + balance);
    }

    public void run() {
        System.out.println(colorize("🎰 Добро пожаловать в Слот-машину!", BOLD));
        System.out.println("Команды: spin <ставка>, balance, stats, deposit <сумма>, help, quit");
        displayBalance();
        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();
            if (cmd.equals("quit") || cmd.equals("q")) {
                System.out.println("Выход.");
                saveStats();
                break;
            } else if (cmd.equals("balance") || cmd.equals("b")) {
                displayBalance();
            } else if (cmd.equals("stats")) {
                displayStats();
            } else if (cmd.equals("spin")) {
                if (parts.length != 2) {
                    System.out.println("Используйте: spin <ставка>");
                    continue;
                }
                try {
                    int betAmount = Integer.parseInt(parts[1]);
                    spin(betAmount);
                } catch (NumberFormatException e) {
                    System.out.println("Ставка должна быть числом.");
                }
            } else if (cmd.equals("deposit")) {
                if (parts.length != 2) {
                    System.out.println("Используйте: deposit <сумма>");
                    continue;
                }
                try {
                    int amount = Integer.parseInt(parts[1]);
                    if (amount <= 0) {
                        System.out.println("Сумма должна быть положительной.");
                        continue;
                    }
                    balance += amount;
                    System.out.println(colorize("Баланс пополнен на " + amount + ". Текущий баланс: " + balance, GREEN));
                    saveStats();
                } catch (NumberFormatException e) {
                    System.out.println("Сумма должна быть числом.");
                }
            } else if (cmd.equals("help") || cmd.equals("h")) {
                System.out.println("Команды:");
                System.out.println("  spin <ставка>  - сделать ставку и крутить барабаны");
                System.out.println("  balance        - показать баланс");
                System.out.println("  stats          - показать статистику");
                System.out.println("  deposit <сумма> - пополнить баланс");
                System.out.println("  quit           - выход");
            } else {
                System.out.println("Неизвестная команда. Введите 'help' для справки.");
            }
        }
        scanner.close();
    }

    public static void main(String[] args) {
        slot game = new slot(100);
        game.run();
    }
}
