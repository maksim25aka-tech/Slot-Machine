// slot.js
#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');
const os = require('os');
const readline = require('readline');

const COLORS = {
    reset: '\x1b[0m',
    red: '\x1b[91m',
    green: '\x1b[92m',
    yellow: '\x1b[93m',
    blue: '\x1b[94m',
    magenta: '\x1b[95m',
    cyan: '\x1b[96m',
    bold: '\x1b[1m'
};

function colorize(text, color) {
    return COLORS[color] + text + COLORS.reset;
}

const SYMBOLS = ['🍒', '🍋', '🍊', '🍇', '🔔', '💎', '7'];
const SYMBOL_COLORS = ['red', 'yellow', 'magenta', 'magenta', 'cyan', 'cyan', 'bold'];
const SYMBOL_VALUES = {
    '🍒': 5, '🍋': 5, '🍊': 5, '🍇': 8, '🔔': 10, '💎': 100, '7': 50
};

class SlotMachine {
    constructor() {
        this.balance = 100;
        this.bet = 0;
        this.history = [];
        this.statsFile = path.join(os.homedir(), '.slot_stats.json');
        this.loadStats();
    }

    loadStats() {
        try {
            const data = JSON.parse(fs.readFileSync(this.statsFile, 'utf8'));
            this.rolls = data.rolls || 0;
            this.totalWon = data.total_won || 0;
            this.bestWin = data.best_win || 0;
            this.balance = data.balance || 100;
        } catch {
            this.rolls = 0;
            this.totalWon = 0;
            this.bestWin = 0;
            this.balance = 100;
        }
    }

    saveStats() {
        const data = {
            rolls: this.rolls,
            total_won: this.totalWon,
            best_win: this.bestWin,
            balance: this.balance
        };
        fs.writeFileSync(this.statsFile, JSON.stringify(data, null, 2));
    }

    spin(bet) {
        if (bet > this.balance) {
            return { result: null, msg: 'Недостаточно средств.' };
        }
        if (bet <= 0) {
            return { result: null, msg: 'Ставка должна быть положительной.' };
        }
        this.bet = bet;
        this.balance -= bet;
        // Анимация
        console.log(colorize('Вращение...', 'bold'));
        for (let i = 0; i < 3; i++) {
            let temp = SYMBOLS.map(() => SYMBOLS[Math.floor(Math.random() * SYMBOLS.length)]);
            process.stdout.write(`\r${temp[0]} ${temp[1]} ${temp[2]}`);
            // Задержка
            const start = Date.now();
            while (Date.now() - start < 200) {}
        }
        // Результат
        const result = SYMBOLS.map(() => SYMBOLS[Math.floor(Math.random() * SYMBOLS.length)]);
        process.stdout.write(`\r${result[0]} ${result[1]} ${result[2]}\n`);
        const win = this.calculateWin(result);
        this.balance += win;
        this.rolls++;
        this.totalWon += win;
        if (win > this.bestWin) this.bestWin = win;
        this.history.push(result);
        if (win > 0) {
            console.log(colorize(`Вы выиграли ${win} монет!`, 'green'));
        } else {
            console.log(colorize('Ничего не выиграно.', 'red'));
        }
        this.saveStats();
        return { result, win };
    }

    calculateWin(result) {
        if (result[0] === '💎' && result[1] === '💎' && result[2] === '💎') {
            return this.bet * 100;
        }
        if (result[0] === result[1] && result[1] === result[2]) {
            return this.bet * (SYMBOL_VALUES[result[0]] || 1);
        }
        // Два одинаковых + джокер
        for (const sym of SYMBOLS) {
            if (sym === '💎') continue;
            const count = result.filter(s => s === sym).length;
            if (count === 2 && result.includes('💎')) {
                return this.bet * 2 * SYMBOL_VALUES[sym];
            }
        }
        if (result.includes('💎')) {
            return this.bet * 2;
        }
        return 0;
    }

    displayBalance() {
        console.log(colorize(`Баланс: ${this.balance} монет`, 'yellow'));
    }

    displayStats() {
        console.log(colorize('📊 Статистика:', 'bold'));
        console.log(`  Вращений: ${this.rolls}`);
        console.log(`  Всего выиграно: ${this.totalWon}`);
        console.log(`  Лучший выигрыш: ${this.bestWin}`);
        console.log(`  Текущий баланс: ${this.balance}`);
    }

    async run() {
        const rl = readline.createInterface({
            input: process.stdin,
            output: process.stdout
        });
        const question = (q) => new Promise(resolve => rl.question(q, resolve));

        console.log(colorize('🎰 Добро пожаловать в Слот-машину!', 'bold'));
        console.log('Команды: spin <ставка>, balance, stats, deposit <сумма>, help, quit');
        this.displayBalance();

        while (true) {
            const input = await question('> ');
            const parts = input.trim().split(/\s+/);
            const cmd = parts[0]?.toLowerCase();
            if (!cmd) continue;
            if (cmd === 'quit' || cmd === 'q') {
                console.log('Выход.');
                this.saveStats();
                rl.close();
                break;
            } else if (cmd === 'balance' || cmd === 'b') {
                this.displayBalance();
            } else if (cmd === 'stats') {
                this.displayStats();
            } else if (cmd === 'spin') {
                if (parts.length !== 2) {
                    console.log('Используйте: spin <ставка>');
                    continue;
                }
                const bet = parseInt(parts[1]);
                if (isNaN(bet)) {
                    console.log('Ставка должна быть числом.');
                    continue;
                }
                const result = this.spin(bet);
                if (result.result) {
                    // уже выведено
                } else {
                    console.log(colorize(result.msg, 'red'));
                }
            } else if (cmd === 'deposit') {
                if (parts.length !== 2) {
                    console.log('Используйте: deposit <сумма>');
                    continue;
                }
                const amount = parseInt(parts[1]);
                if (isNaN(amount) || amount <= 0) {
                    console.log('Сумма должна быть положительным числом.');
                    continue;
                }
                this.balance += amount;
                console.log(colorize(`Баланс пополнен на ${amount}. Текущий баланс: ${this.balance}`, 'green'));
                this.saveStats();
            } else if (cmd === 'help' || cmd === 'h') {
                console.log('Команды:');
                console.log('  spin <ставка>  - сделать ставку и крутить барабаны');
                console.log('  balance        - показать баланс');
                console.log('  stats          - показать статистику');
                console.log('  deposit <сумма> - пополнить баланс');
                console.log('  quit           - выход');
            } else {
                console.log('Неизвестная команда. Введите \'help\' для справки.');
            }
        }
    }
}

async function main() {
    const game = new SlotMachine();
    await game.run();
}

main().catch(console.error);
