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
          [String, Numeric, TrueClass, FalseClass].include?(v.class)
        end

        if unless invalid_keys.empty?
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

  class Person
    has_one String, named: :first_name
    has_one String, named: :last_name
    has_one Numeric, named: :age
    has_one Boolean, named: :admin
    attr_accessor :some_other_non_persistible_attribute
  end

  class Grade
    has_one String, named: :value
    has_one Numeric, named: :value
  end

  class Point
    has_one Numeric, named: :x
    has_one Numeric, named: :y
    def add(other)
      x = self.x + other.x
      y = self.y + other.x
    end
  end

  class Student
    has_one String, named: :full_name
    has_one Numeric, named: :grade
    def promoted
      self.grade > 8
    end
    def has_last_name(last_name)
      self.full_name.split(' ').[1] === last_name
    end
  end

    class StudentA
      has_one String, named: :full_name
      has_one GradeA, named: :grade
    end
    class GradeA
      has_one Numeric, named: :value
    end

    module PersonB
      has_one String, named: :full_name
    end

    class StudentB
      include PersonB
      has_one Grade, named: :grade
    end

    class Assistance_Professor < StudentB
      has_one String, named: :type
    end

    class StudentZ
      has_one String, named: :full_name, no_blank: true
      has_one Numeric, named: :age, from: 18, to: 100
      has_many Grade, named: :grades, validate: proc {value > 2}
    end

    class StudentX
      has_one String, namde: :full_name, default: "natalia natalia"
      has_one Grade, named: :grade, default: Grade.new, no_blank: true
    end

it 'puedo usar has_one' do
  p​ = ​Person​.new
  p​.first_name​ = "raul"​
  p​.last_name​ = 8
  expect(p.first_name).to eq(8)
end

it 'puedo usar save' do
  p​ = ​Person​.new
  p​.first_name​ = "raul"
  p​.last_name​ ​= ​"porcheto"
  p​.save!
  expect(p.id).to eq("0fa00-f1230-0660-0021")
end

it 'puedo usar refresh' do
  p​ ​= ​Person​.new
  p​.first_name​ = "jose"
  p​.save!
  p​.first_name​ = ​"pepe"
  p​.first_name​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​
  p​.refresh!
  p​.first_name​ ​
  Person.new.refresh​! ​
  expect { Person.new.refresh​! ​}.to raise_error(Error, 'El objeto no tiene id')
end

it 'puedo usar forget' do
  p​​ =​ Person.new
  p​.first_name​ =​ "arturo"
  p​.last_name​ ​= ​"puig"
  p​.save
  p​.id​
  p​.​forget
  expect(p.id).to eq(nil)
end

# Recuperación​ y ​Búsqueda

it 'puedo usar all_instances en Point' do
  p1​ ​= Point.new
  p1​.x​ = 2
  p1​.y​ = ​5
  p1​.save!

  p2​ =​ Point.new
  p2​.x​ = 1
  p2​.​y =​ 3
  p2​.​save!

  p3 = Point.new
  p3.x = ​9
  p3.y = ​7

  Point​.all_instances​ ​ ​ ​ ​ ​ ​ ​
  p4​ ​=​ Point​.​all_instances​.first
  p4​.add(p2)
  p4​.save!
  Point​.all_instances​ ​ ​ ​ ​ ​ ​ ​
  p2​.forget!
  Point.all_instances​ ​ ​ ​ ​ ​ ​
  expect(p.id).to eq(nil)
end

it 'puedo usar all_instances en Student' do
  Student.new
  Student​.find_by_id​("5")
  Student​.​find_by_full_name("tito​ ​ puente")
  Student​.find_by_grade​(2)
  Student​.find_by_promoted(false)
  Student​.find_by_has_last_name("puente")
  expect(p.id).to eq(nil)
end

it 'puedo usar composicion con unico objeto' do
  s​ ​= ​StudentA.new
  s​.full_name​ =​ "leo sbaraglia"
  s​.grade​ = GradeA.new
  s​.grade​.value​ =​ 8
  s​.save​! ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​
  g​​ =​ s.grade​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​
  g​.value​ =​ 5
  g​.​save! ​
  expect(s​.refresh​!​.grade​).to eq(5)
end

it 'puedo usar composicion con multiples objetos' do
  s​ =​ Student​A.new
  s​.full_name​​ =​ "leo​ ​sbaraglia"
  s​.grades​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​
  s​.grades.push​(GradeA.new)
  s​.grades​.last.value​ ​=​ 8
  s​.grades​.push​(Grade.Anew)
  s​.grades.last​.value​ ​=​ 5
  s​.save​! ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​
  s​.refresh!.grades​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​
  g​​ = s.grades.last
  g​.value​ =​ 6
  g​.save!
  expect(s​.refresh​!​.grades​).to eq(8)
end

it 'puedo usar herencia entre tipos' do
  PersonB​.all_instances​ ​ ​ ​ ​ ​ ​
  StudentB​.search_by_id("5") ​
  Student​B.search_by_type​("a") ​
  expect { Student​B.search_by_type​("a") ​​}.to raise_error(Error, 'El objeto no entiende serrch_by_type')
end

it 'puedo usar validaciones de tipos' do
  s​=​ Student.new
  s​.full_name​ ​=​ 5
  s​.save​! ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​
  s​.full_name​ = ​"pepe​ botella"
  s​.save​! ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​
  s​.​grade ​= ​Grade.new
  s​.save​! ​ ​ ​ ​
  expect { s​.save​! ​}.to raise_error(Error, 'Grade.vsalue no es un Number')
end

it 'puedo usar validaciones de contenido' do
  s​ ​=​ StudentZ​.new
  s​.full_name​ =​ ""
  s​.save​! ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​
  s​.full_name​ ​=​ "emanuel​ ortega"
  s​.age​ ​=​ 15
  s​.save! ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​
  s​.age​ = ​22
  s​.grades​.push(Grade​.new)
  s​.save! ​ ​ ​ ​
  expect { s​.save! ​ ​​​}.to raise_error(Error, 'grade.value​ ​ no​ ​ es​ ​ > ​ ​ 2!')
end

it 'puedo usar validaciones por defecto' do
  s​ = Student​X.new
  s​.full_name​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​
  s​.name​ =​ nil
  s​.save!
  s​.refresh!
  s​.full_name​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​ ​
  expect(s​.full_name​ ).to eq('natalia natalia')
end

end
