// slot.cpp
#include <iostream>
#include <vector>
#include <string>
#include <map>
#include <random>
#include <chrono>
#include <thread>
#include <fstream>
#include <algorithm>
#include <cctype>
#include <filesystem>

using namespace std;
namespace fs = std::filesystem;

const string RESET = "\033[0m";
const string RED = "\033[91m";
const string GREEN = "\033[92m";
const string YELLOW = "\033[93m";
const string BLUE = "\033[94m";
const string MAGENTA = "\033[95m";
const string CYAN = "\033[96m";
const string BOLD = "\033[1m";

string colorize(const string& text, const string& color) {
    return color + text + RESET;
}

vector<string> SYMBOLS = {"🍒", "🍋", "🍊", "🍇", "🔔", "💎", "7"};
vector<string> SYMBOL_COLORS = {RED, YELLOW, MAGENTA, MAGENTA, CYAN, CYAN, BOLD};
map<string, int> SYMBOL_VALUES = {
    {"🍒", 5}, {"🍋", 5}, {"🍊", 5}, {"🍇", 8}, {"🔔", 10}, {"💎", 100}, {"7", 50}
};

string getHomeDir() {
    const char* home = getenv("HOME");
    if (!home) home = getenv("USERPROFILE");
    return string(home);
}

class SlotMachine {
public:
    int balance;
    int bet;
    vector<vector<string>> history;
    string statsFile;
    int rolls;
    int totalWon;
    int bestWin;

    SlotMachine(int initialBalance = 100) : balance(initialBalance), bet(0), rolls(0), totalWon(0), bestWin(0) {
        statsFile = getHomeDir() + "/.slot_stats.json";
        loadStats();
    }

    void loadStats() {
        ifstream f(statsFile);
        if (!f) return;
        string content((istreambuf_iterator<char>(f)), istreambuf_iterator<char>());
        // Простой парсинг JSON (без библиотеки)
        auto extract = [&](const string& key) -> int {
            size_t pos = content.find("\"" + key + "\"");
            if (pos == string::npos) return 0;
            pos = content.find(":", pos) + 1;
            size_t end = content.find(",", pos);
            if (end == string::npos) end = content.find("}", pos);
            try { return stoi(content.substr(pos, end-pos)); } catch (...) { return 0; }
        };
        rolls = extract("rolls");
        totalWon = extract("total_won");
        bestWin = extract("best_win");
        balance = extract("balance");
        if (balance == 0 && rolls == 0) balance = 100;
    }

    void saveStats() {
        ofstream f(statsFile);
        if (f) {
            f << "{\"rolls\":" << rolls << ",\"total_won\":" << totalWon
              << ",\"best_win\":" << bestWin << ",\"balance\":" << balance << "}";
        }
    }

    vector<string> spin(int betAmount) {
        if (betAmount > balance) {
            cout << colorize("Недостаточно средств.", RED) << endl;
            return {};
        }
        if (betAmount <= 0) {
            cout << colorize("Ставка должна быть положительной.", RED) << endl;
            return {};
        }
        bet = betAmount;
        balance -= bet;
        // Анимация
        cout << colorize("Вращение...", BOLD) << endl;
        random_device rd;
        mt19937 gen(rd());
        uniform_int_distribution<> dis(0, SYMBOLS.size()-1);
        vector<string> result(3);
        for (int i=0; i<3; ++i) {
            // Показываем промежуточные символы
            cout << "\r";
            for (int j=0; j<3; ++j) {
                int idx = dis(gen);
                cout << colorize(SYMBOLS[idx], SYMBOL_COLORS[idx]) << " ";
            }
            cout.flush();
            this_thread::sleep_for(chrono::milliseconds(200));
        }
        // Финальный результат
        for (int i=0; i<3; ++i) {
            int idx = dis(gen);
            result[i] = SYMBOLS[idx];
        }
        cout << "\r" << colorize(result[0], SYMBOL_COLORS[find(SYMBOLS.begin(), SYMBOLS.end(), result[0])-SYMBOLS.begin()]) << " "
             << colorize(result[1], SYMBOL_COLORS[find(SYMBOLS.begin(), SYMBOLS.end(), result[1])-SYMBOLS.begin()]) << " "
             << colorize(result[2], SYMBOL_COLORS[find(SYMBOLS.begin(), SYMBOLS.end(), result[2])-SYMBOLS.begin()]) << endl;
        // Выигрыш
        int win = calculateWin(result);
        balance += win;
        rolls++;
        totalWon += win;
        if (win > bestWin) bestWin = win;
        history.push_back(result);
        if (win > 0) {
            cout << colorize("Вы выиграли " + to_string(win) + " монет!", GREEN) << endl;
        } else {
            cout << colorize("Ничего не выиграно.", RED) << endl;
        }
        saveStats();
        return result;
    }

    int calculateWin(const vector<string>& result) {
        if (result[0] == "💎" && result[1] == "💎" && result[2] == "💎") {
            return bet * 100;
        }
        if (result[0] == result[1] && result[1] == result[2]) {
            return bet * SYMBOL_VALUES[result[0]];
        }
        // Два одинаковых + джокер
        for (const string& sym : SYMBOLS) {
            if (sym == "💎") continue;
            int count = 0;
            for (const string& s : result) if (s == sym) count++;
            if (count == 2 && find(result.begin(), result.end(), "💎") != result.end()) {
                return bet * 2 * SYMBOL_VALUES[sym];
            }
        }
        // Один джокер
        if (find(result.begin(), result.end(), "💎") != result.end()) {
            return bet * 2;
        }
        return 0;
    }

    void displayBalance() {
        cout << colorize("Баланс: " + to_string(balance) + " монет", YELLOW) << endl;
    }

    void displayStats() {
        cout << colorize("📊 Статистика:", BOLD) << endl;
        cout << "  Вращений: " << rolls << endl;
        cout << "  Всего выиграно: " << totalWon << endl;
        cout << "  Лучший выигрыш: " << bestWin << endl;
        cout << "  Текущий баланс: " << balance << endl;
    }

    void run() {
        cout << colorize("🎰 Добро пожаловать в Слот-машину!", BOLD) << endl;
        cout << "Команды: spin <ставка>, balance, stats, deposit <сумма>, help, quit" << endl;
        displayBalance();
        string cmd;
        while (true) {
            cout << "> ";
            getline(cin, cmd);
            if (cmd == "quit" || cmd == "q") {
                cout << "Выход." << endl;
                saveStats();
                break;
            } else if (cmd == "balance" || cmd == "b") {
                displayBalance();
            } else if (cmd == "stats") {
                displayStats();
            } else if (cmd.substr(0,5) == "spin ") {
                string betStr = cmd.substr(5);
                int betAmount;
                try { betAmount = stoi(betStr); } catch (...) { cout << "Ставка должна быть числом." << endl; continue; }
                spin(betAmount);
            } else if (cmd.substr(0,8) == "deposit ") {
                string amountStr = cmd.substr(8);
                int amount;
                try { amount = stoi(amountStr); } catch (...) { cout << "Сумма должна быть числом." << endl; continue; }
                if (amount <= 0) { cout << "Сумма должна быть положительной." << endl; continue; }
                balance += amount;
                cout << colorize("Баланс пополнен на " + to_string(amount) + ". Текущий баланс: " + to_string(balance), GREEN) << endl;
                saveStats();
            } else if (cmd == "help" || cmd == "h") {
                cout << "Команды:\n  spin <ставка>  - сделать ставку и крутить барабаны\n  balance        - показать баланс\n  stats          - показать статистику\n  deposit <сумма> - пополнить баланс\n  quit           - выход" << endl;
            } else {
                cout << "Неизвестная команда. Введите 'help' для справки." << endl;
            }
        }
    }
};

int main() {
    SlotMachine game;
    game.run();
    return 0;
}
