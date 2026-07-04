// slot.cs
using System;
using System.Collections.Generic;
using System.IO;
using System.Text.Json;
using System.Threading;

class SlotMachine
{
    static string Colorize(string text, string color)
    {
        string col = color switch
        {
            "red" => "\x1b[91m",
            "green" => "\x1b[92m",
            "yellow" => "\x1b[93m",
            "blue" => "\x1b[94m",
            "magenta" => "\x1b[95m",
            "cyan" => "\x1b[96m",
            "bold" => "\x1b[1m",
            _ => "\x1b[0m"
        };
        return col + text + "\x1b[0m";
    }

    static string[] SYMBOLS = {"🍒", "🍋", "🍊", "🍇", "🔔", "💎", "7"};
    static string[] SYMBOL_COLORS = {"red", "yellow", "magenta", "magenta", "cyan", "cyan", "bold"};
    static Dictionary<string, int> SYMBOL_VALUES = new Dictionary<string, int>
    {
        {"🍒", 5}, {"🍋", 5}, {"🍊", 5}, {"🍇", 8}, {"🔔", 10}, {"💎", 100}, {"7", 50}
    };

    class Stats
    {
        public int rolls { get; set; }
        public int total_won { get; set; }
        public int best_win { get; set; }
        public int balance { get; set; }
    }

    private int balance;
    private int bet;
    private List<string[]> history = new List<string[]>();
    private Stats stats;
    private string statsFile;

    public SlotMachine(int initialBalance = 100)
    {
        balance = initialBalance;
        statsFile = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".slot_stats.json");
        LoadStats();
    }

    void LoadStats()
    {
        if (File.Exists(statsFile))
        {
            try
            {
                string json = File.ReadAllText(statsFile);
                stats = JsonSerializer.Deserialize<Stats>(json);
                if (stats != null)
                {
                    balance = stats.balance > 0 ? stats.balance : 100;
                }
                else stats = new Stats { balance = 100 };
            }
            catch { stats = new Stats { balance = 100 }; }
        }
        else stats = new Stats { balance = 100 };
    }

    void SaveStats()
    {
        stats.balance = balance;
        string json = JsonSerializer.Serialize(stats);
        File.WriteAllText(statsFile, json);
    }

    string[] Spin(int betAmount)
    {
        if (betAmount > balance)
        {
            Console.WriteLine(Colorize("Недостаточно средств.", "red"));
            return null;
        }
        if (betAmount <= 0)
        {
            Console.WriteLine(Colorize("Ставка должна быть положительной.", "red"));
            return null;
        }
        bet = betAmount;
        balance -= bet;
        Console.WriteLine(Colorize("Вращение...", "bold"));
        // Анимация
        Random rnd = new Random();
        for (int i=0; i<3; i++)
        {
            string[] temp = new string[3];
            for (int j=0; j<3; j++) temp[j] = SYMBOLS[rnd.Next(SYMBOLS.Length)];
            Console.Write($"\r{temp[0]} {temp[1]} {temp[2]}");
            Thread.Sleep(200);
        }
        // Результат
        string[] result = new string[3];
        for (int i=0; i<3; i++) result[i] = SYMBOLS[rnd.Next(SYMBOLS.Length)];
        Console.Write($"\r{result[0]} {result[1]} {result[2]}\n");
        int win = CalculateWin(result);
        balance += win;
        stats.rolls++;
        stats.total_won += win;
        if (win > stats.best_win) stats.best_win = win;
        history.Add(result);
        if (win > 0)
            Console.WriteLine(Colorize($"Вы выиграли {win} монет!", "green"));
        else
            Console.WriteLine(Colorize("Ничего не выиграно.", "red"));
        SaveStats();
        return result;
    }

    int CalculateWin(string[] result)
    {
        if (result[0] == "💎" && result[1] == "💎" && result[2] == "💎")
            return bet * 100;
        if (result[0] == result[1] && result[1] == result[2])
            return bet * SYMBOL_VALUES[result[0]];
        // Два одинаковых + джокер
        foreach (string sym in SYMBOLS)
        {
            if (sym == "💎") continue;
            int count = 0;
            foreach (string s in result) if (s == sym) count++;
            if (count == 2 && Array.Exists(result, s => s == "💎"))
                return bet * 2 * SYMBOL_VALUES[sym];
        }
        if (Array.Exists(result, s => s == "💎"))
            return bet * 2;
        return 0;
    }

    void DisplayBalance()
    {
        Console.WriteLine(Colorize($"Баланс: {balance} монет", "yellow"));
    }

    void DisplayStats()
    {
        Console.WriteLine(Colorize("📊 Статистика:", "bold"));
        Console.WriteLine($"  Вращений: {stats.rolls}");
        Console.WriteLine($"  Всего выиграно: {stats.total_won}");
        Console.WriteLine($"  Лучший выигрыш: {stats.best_win}");
        Console.WriteLine($"  Текущий баланс: {balance}");
    }

    public void Run()
    {
        Console.WriteLine(Colorize("🎰 Добро пожаловать в Слот-машину!", "bold"));
        Console.WriteLine("Команды: spin <ставка>, balance, stats, deposit <сумма>, help, quit");
        DisplayBalance();
        while (true)
        {
            Console.Write("> ");
            string input = Console.ReadLine().Trim();
            if (input == "quit" || input == "q")
            {
                Console.WriteLine("Выход.");
                SaveStats();
                break;
            }
            else if (input == "balance" || input == "b")
            {
                DisplayBalance();
            }
            else if (input == "stats")
            {
                DisplayStats();
            }
            else if (input.StartsWith("spin "))
            {
                string[] parts = input.Split(' ');
                if (parts.Length != 2)
                {
                    Console.WriteLine("Используйте: spin <ставка>");
                    continue;
                }
                if (!int.TryParse(parts[1], out int betAmount))
                {
                    Console.WriteLine("Ставка должна быть числом.");
                    continue;
                }
                Spin(betAmount);
            }
            else if (input.StartsWith("deposit "))
            {
                string[] parts = input.Split(' ');
                if (parts.Length != 2)
                {
                    Console.WriteLine("Используйте: deposit <сумма>");
                    continue;
                }
                if (!int.TryParse(parts[1], out int amount) || amount <= 0)
                {
                    Console.WriteLine("Сумма должна быть положительным числом.");
                    continue;
                }
                balance += amount;
                Console.WriteLine(Colorize($"Баланс пополнен на {amount}. Текущий баланс: {balance}", "green"));
                SaveStats();
            }
            else if (input == "help" || input == "h")
            {
                Console.WriteLine("Команды:");
                Console.WriteLine("  spin <ставка>  - сделать ставку и крутить барабаны");
                Console.WriteLine("  balance        - показать баланс");
                Console.WriteLine("  stats          - показать статистику");
                Console.WriteLine("  deposit <сумма> - пополнить баланс");
                Console.WriteLine("  quit           - выход");
            }
            else
            {
                Console.WriteLine("Неизвестная команда. Введите 'help' для справки.");
            }
        }
    }

    static void Main()
    {
        SlotMachine game = new SlotMachine();
        game.Run();
    }
}
