# slot.py
#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import os
import random
import time
import json
from pathlib import Path

# ANSI-цвета
COLORS = {
    'reset': '\033[0m',
    'red': '\033[91m',
    'green': '\033[92m',
    'yellow': '\033[93m',
    'blue': '\033[94m',
    'magenta': '\033[95m',
    'cyan': '\033[96m',
    'bold': '\033[1m'
}

def colorize(text, color):
    return f"{COLORS.get(color, '')}{text}{COLORS['reset']}"

# Символы и их цвета
SYMBOLS = ['🍒', '🍋', '🍊', '🍇', '🔔', '💎', '7']
SYMBOL_COLORS = ['red', 'yellow', 'orange', 'magenta', 'cyan', 'cyan', 'bold']
SYMBOL_VALUES = {
    '🍒': 5, '🍋': 5, '🍊': 5, '🍇': 8, '🔔': 10, '💎': 100, '7': 50
}

class SlotMachine:
    def __init__(self, balance=100):
        self.balance = balance
        self.bet = 0
        self.history = []
        self.stats_file = Path.home() / '.slot_stats.json'
        self.load_stats()
        self.rolls = 0
        self.total_won = 0
        self.best_win = 0

    def load_stats(self):
        if self.stats_file.exists():
            try:
                with open(self.stats_file, 'r') as f:
                    data = json.load(f)
                    self.rolls = data.get('rolls', 0)
                    self.total_won = data.get('total_won', 0)
                    self.best_win = data.get('best_win', 0)
                    self.balance = data.get('balance', 100)
            except:
                pass

    def save_stats(self):
        data = {
            'rolls': self.rolls,
            'total_won': self.total_won,
            'best_win': self.best_win,
            'balance': self.balance
        }
        with open(self.stats_file, 'w') as f:
            json.dump(data, f)

    def spin(self, bet):
        if bet > self.balance:
            return None, "Недостаточно средств."
        if bet <= 0:
            return None, "Ставка должна быть положительной."
        self.bet = bet
        self.balance -= bet
        # Анимация вращения
        print(colorize("Вращение...", 'bold'))
        for _ in range(3):
            # Показываем случайные символы для эффекта
            temp = [random.choice(SYMBOLS) for _ in range(3)]
            print(f"\r{temp[0]} {temp[1]} {temp[2]}", end='')
            time.sleep(0.2)
        # Финальный результат
        result = [random.choice(SYMBOLS) for _ in range(3)]
        print(f"\r{result[0]} {result[1]} {result[2]}")
        # Подсчёт выигрыша
        win = self.calculate_win(result)
        self.balance += win
        self.rolls += 1
        self.total_won += win
        if win > self.best_win:
            self.best_win = win
        self.history.append((result, win))
        if win > 0:
            print(colorize(f"Вы выиграли {win} монет!", 'green'))
        else:
            print(colorize("Ничего не выиграно.", 'red'))
        self.save_stats()
        return result, win

    def calculate_win(self, result):
        # Проверка на джекпот (три 💎)
        if result[0] == '💎' and result[1] == '💎' and result[2] == '💎':
            return self.bet * 100
        # Проверка на три одинаковых
        if result[0] == result[1] == result[2]:
            return self.bet * SYMBOL_VALUES.get(result[0], 1)
        # Проверка на два одинаковых с джокером (💎)
        # Джокер может заменить любой символ
        # Проверим, есть ли среди них два одинаковых и один джокер
        for sym in set(result):
            if sym == '💎':
                continue
            if result.count(sym) == 2 and '💎' in result:
                return self.bet * 2 * SYMBOL_VALUES.get(sym, 1)
        # Проверка на наличие одного джокера (просто удвоение)
        if '💎' in result:
            # Если есть джокер, но нет пары, даём маленький бонус
            return self.bet * 2
        return 0

    def display_balance(self):
        print(colorize(f"Баланс: {self.balance} монет", 'yellow'))

    def display_stats(self):
        print(colorize("📊 Статистика:", 'bold'))
        print(f"  Вращений: {self.rolls}")
        print(f"  Всего выиграно: {self.total_won}")
        print(f"  Лучший выигрыш: {self.best_win}")
        print(f"  Текущий баланс: {self.balance}")

    def run(self):
        print(colorize("🎰 Добро пожаловать в Слот-машину!", 'bold'))
        print("Команды: spin <ставка>, balance, stats, deposit <сумма>, help, quit")
        self.display_balance()
        while True:
            cmd = input("> ").strip().lower()
            if cmd == 'quit' or cmd == 'q':
                print("Выход.")
                self.save_stats()
                break
            elif cmd == 'balance' or cmd == 'b':
                self.display_balance()
            elif cmd == 'stats':
                self.display_stats()
            elif cmd.startswith('spin '):
                parts = cmd.split()
                if len(parts) != 2:
                    print("Используйте: spin <ставка>")
                    continue
                try:
                    bet = int(parts[1])
                except:
                    print("Ставка должна быть числом.")
                    continue
                result, msg = self.spin(bet)
                if result is None:
                    print(colorize(msg, 'red'))
                else:
                    # Показываем выигрышную комбинацию с цветами
                    colored = []
                    for i, sym in enumerate(result):
                        col = SYMBOL_COLORS[SYMBOLS.index(sym)]
                        colored.append(colorize(sym, col))
                    print(f"Результат: {' '.join(colored)}")
                    if msg:
                        print(msg)
            elif cmd.startswith('deposit '):
                parts = cmd.split()
                if len(parts) != 2:
                    print("Используйте: deposit <сумма>")
                    continue
                try:
                    amount = int(parts[1])
                except:
                    print("Сумма должна быть числом.")
                    continue
                if amount <= 0:
                    print("Сумма должна быть положительной.")
                    continue
                self.balance += amount
                print(colorize(f"Баланс пополнен на {amount}. Текущий баланс: {self.balance}", 'green'))
                self.save_stats()
            elif cmd == 'help' or cmd == 'h':
                print("Команды:")
                print("  spin <ставка>  - сделать ставку и крутить барабаны")
                print("  balance        - показать баланс")
                print("  stats          - показать статистику")
                print("  deposit <сумма> - пополнить баланс")
                print("  quit           - выход")
            else:
                print("Неизвестная команда. Введите 'help' для справки.")

def main():
    game = SlotMachine()
    game.run()

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print(colorize("\nВыход.", 'yellow'))
        sys.exit(0)
