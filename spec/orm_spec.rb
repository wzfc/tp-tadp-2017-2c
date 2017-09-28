require 'rspec'
require 'fileutils'
require 'json'
require 'securerandom'
require_relative '../src/orm'

describe 'Orm' do

  class DB
    def self.table(table_name)
      Table.new(table_name)
    end

    def self.clear_all
      FileUtils.remove_dir("./db")
    end

    class Table
      def initialize(name)
        @name = name
      end

      def entries
        file("r") do |f|
          f.readlines.map do |line|
            JSON.parse(line).map {|k, v| [k.to_sym, v]}.to_h
          end
        end
      end

      def insert(_entry)
        entry = _entry.clone
        entry[:id] ||= SecureRandom.uuid
        invalid_keys = entry.keys.select do |k|
          v = entry[k]
          !v.is_a? (String) && !v.is_a? (Numeric) && v != true && != false
        end

        if !invalid_keys.empty?
          throw(TypeError.new("Can't persist field(s) #{invalid_keys} because they contain non-primitive values #{invalid_keys.map {|k| entry[k]}}"))
        end

        file("a") do |f|
          f.puts(JSON.generate(entry))
        end

        entry[:id]
      end

      def delete(id)
        remaining = entries.reject {|entry| entry[:id] === id}
        file("w") do |f|
          remaining.each do |entry|
            f.puts(JSON.generate(entry))
          end
        end
      end
        
      def clear
        File.open("./db/#{@name}", "w") {}
      end

      private

      def file(mode, &block)
        Dir.mkdir("./db") unless File.exist?("./db")
        clear unless File.exist?("./db/#{@name}")
        File.open("./db/#{@name}", mode) {|file| block.call(file)}
      end

    end
  end

end


