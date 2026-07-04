#!/usr/bin/env ruby
# slot.rb
# encoding: UTF-8

require 'json'
require 'fileutils'

COLORS = {
  reset: "\e[0m",
  red: "\e[91m",
  green: "\e[92m",
  yellow: "\e[93m",
  blue: "\e[94m",
  magenta: "\e[95m",
  cyan: "\e[96m",
  bold: "\e[1m"
}

def colorize(text, color)
  "#{COLORS[color]}#{text}#{COLORS[:reset]}"
end

SYMBOLS = ['🍒', '🍋', '🍊', '🍇', '🔔', '💎', '7']
SYMBOL_COLORS = [:red, :yellow, :magenta, :magenta, :cyan, :cyan, :bold]
SYMBOL_VALUES = {
  '🍒' => 5, '🍋' => 5, '🍊' => 5, '🍇' => 8, '🔔' => 10, '💎' => 100, '7' => 50
}

class SlotMachine
  attr_reader :balance, :bet, :history, :stats_file

  def initialize
    @balance = 100
    @bet = 0
    @history = []
    @stats_file = File.join(Dir.home, '.slot_stats.json')
    load_stats
  end

  def load_stats
    if File.exist?(@stats_file)
      begin
        data = JSON.parse(File.read(@stats_file))
        @rolls = data['rolls'] || 0
        @total_won = data['total_won'] || 0
        @best_win = data['best_win'] || 0
        @balance = data['balance'] || 100
      rescue
        @rolls = 0
        @total_won = 0
        @best_win = 0
        @balance = 100
      end
    else
      @rolls = 0
      @total_won = 0
      @best_win = 0
    end
  end

  def save_stats
    data = {
      'rolls' => @rolls,
      'total_won' => @total_won,
      'best_win' => @best_win,
      'balance' => @balance
    }
    File.write(@stats_file, JSON.pretty_generate(data))
  end

  def spin(bet)
    if bet > @balance
      return { result: nil, msg: 'Недостаточно средств.' }
    end
    if bet <= 0
      return { result: nil, msg: 'Ставка должна быть положительной.' }
    end
    @bet = bet
    @balance -= bet
    puts colorize('Вращение...', :bold)
    3.times do
      temp = SYMBOLS.sample(3)
      print "\r#{temp.join(' ')}"
      sleep 0.2
    end
    result = SYMBOLS.sample(3)
    print "\r#{result.join(' ')}\n"
    win = calculate_win(result)
    @balance += win
    @rolls += 1
    @total_won += win
    @best_win = win if win > @best_win
    @history << result
    if win > 0
      puts colorize("Вы выиграли #{win} монет!", :green)
    else
      puts colorize('Ничего не выиграно.', :red)
    end
    save_stats
    { result: result, win: win }
  end

  def calculate_win(result)
    if result.all? { |s| s == '💎' }
      return @bet * 100
    end
    if result[0] == result[1] && result[1] == result[2]
      return @bet * SYMBOL_VALUES[result[0]]
    end
    # Два одинаковых + джокер
    SYMBOLS.each do |sym|
      next if sym == '💎'
      count = result.count { |s| s == sym }
      if count == 2 && result.include?('💎')
        return @bet * 2 * SYMBOL_VALUES[sym]
      end
    end
    if result.include?('💎')
      return @bet * 2
    end
    0
  end

  def display_balance
    puts colorize("Баланс: #{@balance} монет", :yellow)
  end

  def display_stats
    puts colorize('📊 Статистика:', :bold)
    puts "  Вращений: #{@rolls}"
    puts "  Всего выиграно: #{@total_won}"
    puts "  Лучший выигрыш: #{@best_win}"
    puts "  Текущий баланс: #{@balance}"
  end

  def run
    puts colorize('🎰 Добро пожаловать в Слот-машину!', :bold)
    puts 'Команды: spin <ставка>, balance, stats, deposit <сумма>, help, quit'
    display_balance
    loop do
      print '> '
      cmd = gets.chomp.strip
      parts = cmd.split
      if parts.empty?
        next
      end
      case parts[0].downcase
      when 'quit', 'q'
        puts 'Выход.'
        save_stats
        break
      when 'balance', 'b'
        display_balance
      when 'stats'
        display_stats
      when 'spin'
        if parts.size != 2
          puts 'Используйте: spin <ставка>'
          next
        end
        bet = parts[1].to_i
        if bet <= 0
          puts 'Ставка должна быть положительным числом.'
          next
        end
        spin(bet)
      when 'deposit'
        if parts.size != 2
          puts 'Используйте: deposit <сумма>'
          next
        end
        amount = parts[1].to_i
        if amount <= 0
          puts 'Сумма должна быть положительным числом.'
          next
        end
        @balance += amount
        puts colorize("Баланс пополнен на #{amount}. Текущий баланс: #{@balance}", :green)
        save_stats
      when 'help', 'h'
        puts 'Команды:'
        puts '  spin <ставка>  - сделать ставку и крутить барабаны'
        puts '  balance        - показать баланс'
        puts '  stats          - показать статистику'
        puts '  deposit <сумма> - пополнить баланс'
        puts '  quit           - выход'
      else
        puts 'Неизвестная команда. Введите \'help\' для справки.'
      end
    end
  end
end

if __FILE__ == $0
  game = SlotMachine.new
  game.run
end
