require 'rspec'
require_relative '../src/orm'

class Person
  has_one String, named: :first_name
  has_one String, named: :last_name
  has_one Numeric, named: :age
  has_one Boolean, named: :admin
end

class Grade
  has_one String, named: :value
  has_one Numeric, named: :value
end

class Point
  has_one Numeric, named: :x
  has_one Numeric, named: :y

  def add(other)
    self.x = self.x + other.x
    self.y = self.y + other.y
  end
end

class Student
  has_one String, named: :full_name
  has_one Numeric, named: :grade

  def promoted
    self.grade > 8
  end

  def has_last_name(last_name)
    self.full_name.split(' ')[1] === last_name
  end
end

class GradeA
  has_one Numeric, named: :value
end
class StudentA
  has_one String, named: :full_name
  has_one GradeA, named: :grade
end

module PersonB
  has_one String, named: :full_name
end

class StudentB
  include PersonB
  has_many GradeA, named: :grades
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
  has_one String, named: :full_name, default: "natalia natalia"
  has_one Grade, named: :grade, default: Grade.new, no_blank: true
end



describe 'Orm' do

  before do
    @persona = Person.new
    @persona.first_name = "Raul"
    @persona.last_name = "Perez"
    @persona.age= 20
    @persona.admin = false

    @estudiante= StudentA.new
    @estudiante.full_name = "Pable Perez"

    @estudiante2= StudentB.new
    @estudiante2.full_name = "Rocio Oliva"

    @grado= GradeA.new
    @grado.value = 5

    @grado2 = GradeA.new
    @grado2.value = 7

    @punto= Point.new
    @punto.x = 2
    @punto.y = 5

    @punto2 = Point.new
    @punto2.x = 1
    @punto2.y = 3

    @listaGrados= Array.new

    @listaGrados.push(@grado)
    @listaGrados.push(@grado2)

    @estudiante2.grades = @listaGrados

    @asistente = Assistance_Professor.new

    @asistente.full_name = 'Roberto Saraza'
    @asistente.grades = @listaGrados
    @asistente.type = 'Avanzado'

  end


  it 'puede crear los atributos con has_one' do
    expect(@persona.first_name).to eq("Raul")
  end

  it 'persiste un objeto con atributos primitivos y lo trae' do
    @persona.save!
    expect(Person.find_by_first_name("Raul")[0].first_name).to eq("Raul")
  end

  it 'puedo usar refresh' do
    @persona.save!
    @persona.first_name = "Pedro"
    @persona.first_name
    @persona.refresh!
    expect(@persona.first_name).to eq("Raul")
  end


  it 'falla si no tiene id' do
    expect{Person.new.refresh!}.to raise_error()
  end

  it 'persiste compuesto simple' do
    @estudiante.grade = @grado
    @estudiante.save!
    expect(StudentA.find_by_id("64faf926-07b5-4238-bc0e-29f7836b012f")[0].full_name).to eq("Pable Perez")
  end

  it 'puedo usar all_instances en Point' do
  @punto.save!
  @punto2.save!
  @punto3 = Point.all_instances.first
  @punto3.add(@punto2)
  @punto3.save!
  @punto2.forget!

  expect(Point.all_instances.size).to eq(1)
  expect(Point.all_instances.first.x).to eq(3)
  expect(Point.all_instances.first.y).to eq(8)
  end

  it 'persiste con has_many' do
    @estudiante2.save!
    expect(GradeA.all_instances.size).to eq(2)
  end

  it 'persiste la herencia' do
    @asistente.save!
  end



end
