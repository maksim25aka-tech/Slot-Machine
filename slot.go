// slot.go
package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"math/rand"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"
)

const (
	reset  = "\033[0m"
	red    = "\033[91m"
	green  = "\033[92m"
	yellow = "\033[93m"
	blue   = "\033[94m"
	magenta= "\033[95m"
	cyan   = "\033[96m"
	bold   = "\033[1m"
)

func colorize(text, color string) string {
	return color + text + reset
}

var symbols = []string{"🍒", "🍋", "🍊", "🍇", "🔔", "💎", "7"}
var symbolColors = []string{red, yellow, magenta, magenta, cyan, cyan, bold}
var symbolValues = map[string]int{
	"🍒": 5, "🍋": 5, "🍊": 5, "🍇": 8, "🔔": 10, "💎": 100, "7": 50,
}

type Stats struct {
	Rolls    int `json:"rolls"`
	TotalWon int `json:"total_won"`
	BestWin  int `json:"best_win"`
	Balance  int `json:"balance"`
}

type SlotMachine struct {
	balance  int
	bet      int
	history  [][]string
	stats    Stats
	statsFile string
}

func NewSlotMachine() *SlotMachine {
	s := &SlotMachine{balance: 100}
	s.statsFile = filepath.Join(os.Getenv("HOME"), ".slot_stats.json")
	s.loadStats()
	return s
}

func (s *SlotMachine) loadStats() {
	data, err := os.ReadFile(s.statsFile)
	if err != nil {
		s.stats = Stats{Balance: 100}
		return
	}
	if err := json.Unmarshal(data, &s.stats); err != nil {
		s.stats = Stats{Balance: 100}
	}
	if s.stats.Balance == 0 && s.stats.Rolls == 0 {
		s.stats.Balance = 100
	}
	s.balance = s.stats.Balance
}

func (s *SlotMachine) saveStats() {
	s.stats.Balance = s.balance
	data, _ := json.MarshalIndent(s.stats, "", "  ")
	os.WriteFile(s.statsFile, data, 0644)
}

func (s *SlotMachine) spin(bet int) []string {
	if bet > s.balance {
		fmt.Println(colorize("Недостаточно средств.", red))
		return nil
	}
	if bet <= 0 {
		fmt.Println(colorize("Ставка должна быть положительной.", red))
		return nil
	}
	s.bet = bet
	s.balance -= bet
	fmt.Println(colorize("Вращение...", bold))
	// Анимация
	for i := 0; i < 3; i++ {
		fmt.Print("\r")
		for j := 0; j < 3; j++ {
			idx := rand.Intn(len(symbols))
			fmt.Print(colorize(symbols[idx], symbolColors[idx]) + " ")
		}
		time.Sleep(200 * time.Millisecond)
	}
	// Результат
	result := make([]string, 3)
	for i := 0; i < 3; i++ {
		idx := rand.Intn(len(symbols))
		result[i] = symbols[idx]
	}
	fmt.Printf("\r%s %s %s\n",
		colorize(result[0], symbolColors[indexOf(symbols, result[0])]),
		colorize(result[1], symbolColors[indexOf(symbols, result[1])]),
		colorize(result[2], symbolColors[indexOf(symbols, result[2])]),
	)
	win := s.calculateWin(result)
	s.balance += win
	s.stats.Rolls++
	s.stats.TotalWon += win
	if win > s.stats.BestWin {
		s.stats.BestWin = win
	}
	s.history = append(s.history, result)
	if win > 0 {
		fmt.Printf("%s\n", colorize(fmt.Sprintf("Вы выиграли %d монет!", win), green))
	} else {
		fmt.Println(colorize("Ничего не выиграно.", red))
	}
	s.saveStats()
	return result
}

func indexOf(slice []string, val string) int {
	for i, v := range slice {
		if v == val {
			return i
		}
	}
	return 0
}

func (s *SlotMachine) calculateWin(result []string) int {
	if result[0] == "💎" && result[1] == "💎" && result[2] == "💎" {
		return s.bet * 100
	}
	if result[0] == result[1] && result[1] == result[2] {
		return s.bet * symbolValues[result[0]]
	}
	// Два одинаковых + джокер
	for _, sym := range symbols {
		if sym == "💎" {
			continue
		}
		count := 0
		for _, r := range result {
			if r == sym {
				count++
			}
		}
		if count == 2 && contains(result, "💎") {
			return s.bet * 2 * symbolValues[sym]
		}
	}
	if contains(result, "💎") {
		return s.bet * 2
	}
	return 0
}

func contains(slice []string, val string) bool {
	for _, v := range slice {
		if v == val {
			return true
		}
	}
	return false
}

func (s *SlotMachine) displayBalance() {
	fmt.Printf("%s\n", colorize(fmt.Sprintf("Баланс: %d монет", s.balance), yellow))
}

func (s *SlotMachine) displayStats() {
	fmt.Println(colorize("📊 Статистика:", bold))
	fmt.Printf("  Вращений: %d\n", s.stats.Rolls)
	fmt.Printf("  Всего выиграно: %d\n", s.stats.TotalWon)
	fmt.Printf("  Лучший выигрыш: %d\n", s.stats.BestWin)
	fmt.Printf("  Текущий баланс: %d\n", s.balance)
}

func (s *SlotMachine) run() {
	scanner := bufio.NewScanner(os.Stdin)
	fmt.Println(colorize("🎰 Добро пожаловать в Слот-машину!", bold))
	fmt.Println("Команды: spin <ставка>, balance, stats, deposit <сумма>, help, quit")
	s.displayBalance()
	for {
		fmt.Print("> ")
		if !scanner.Scan() {
			break
		}
		cmd := strings.TrimSpace(scanner.Text())
		parts := strings.Fields(cmd)
		if len(parts) == 0 {
			continue
		}
		switch parts[0] {
		case "quit", "q":
			fmt.Println("Выход.")
			s.saveStats()
			return
		case "balance", "b":
			s.displayBalance()
		case "stats":
			s.displayStats()
		case "spin":
			if len(parts) != 2 {
				fmt.Println("Используйте: spin <ставка>")
				continue
			}
			bet, err := strconv.Atoi(parts[1])
			if err != nil {
				fmt.Println("Ставка должна быть числом.")
				continue
			}
			s.spin(bet)
		case "deposit":
			if len(parts) != 2 {
				fmt.Println("Используйте: deposit <сумма>")
				continue
			}
			amount, err := strconv.Atoi(parts[1])
			if err != nil || amount <= 0 {
				fmt.Println("Сумма должна быть положительным числом.")
				continue
			}
			s.balance += amount
			fmt.Printf("%s\n", colorize(fmt.Sprintf("Баланс пополнен на %d. Текущий баланс: %d", amount, s.balance), green))
			s.saveStats()
		case "help", "h":
			fmt.Println("Команды:")
			fmt.Println("  spin <ставка>  - сделать ставку и крутить барабаны")
			fmt.Println("  balance        - показать баланс")
			fmt.Println("  stats          - показать статистику")
			fmt.Println("  deposit <сумма> - пополнить баланс")
			fmt.Println("  quit           - выход")
		default:
			fmt.Println("Неизвестная команда. Введите 'help' для справки.")
		}
	}
}

func main() {
	rand.Seed(time.Now().UnixNano())
	game := NewSlotMachine()
	game.run()
}
