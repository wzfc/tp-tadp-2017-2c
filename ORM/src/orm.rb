require 'tadb'

module ORM
  module DSL
    class AttributeMap
      attr_accessor :type, :default_value, :validations

      def initialize(type, opts)
        self.type = type
        self.default_value = opts.delete(:default)

        @complex = opts.delete(:complex) | false

        # Crea un array de procs de validacion.
        self.validations =
          opts.map do |constraint, args|
            ORM::Validation.send(constraint, args)
          end
      end

      def primitive?
        [String, Numeric, Boolean].include?(self.type)
      end

      def complex?
        @complex
      end
    end

    private

    # Devuelve una coleccion de tuplas que contienen:
    #   {clave=nombre_atributo, valor=attribute_map}
    def persistable_attributes
      if @persistable_attributes.nil? then
        @persistable_attributes = Hash.new

        # Incorporar todos los atributos persistibles de los ancestors.
        #   drop(1) porque el primer ancestor es la misma clase.
        self.ancestors.drop(1).reverse_each do |cls|
          @persistable_attributes.merge!(
            Hash(cls.instance_variable_get(:@persistable_attributes))
          )
        end
      end

      @persistable_attributes
    end

    def has_one(type, constraints)
      unless [String, Numeric, Boolean].include?(type) ||
          type.is_a?(ORM::PersistableClass)
        raise "Error: un atributo debe ser de una clase persistible"
      end

      # Extraer la clave :named porque va a ser la clave del hash de atributos.
      attr_name = constraints.delete(:named)

      unless self.is_a?(ORM::PersistableClass) then
        # Incluir el mixin que brinda las operaciones save, refresh y forget.
        include ORM::PersistableObject

        # Extender la clase agregando el metodo all_instances().
        extend ORM::PersistableClass

        # Definir id, id= y find_by_id con una llamada recursiva.
        has_one String, named: :id
      end

      attribute_map = AttributeMap.new(type, constraints)

      # Getter para el atributo que devuelve un valor
      #   por default si este no esta definido.
      self.send(:define_method, attr_name) do
        if instance_variables.include?("@#{attr_name}".to_sym)
          self.instance_eval "@#{attr_name}"
        elsif attribute_map.complex?
          self.instance_eval "@#{attr_name} = Array.new"
        else
          attribute_map.default_value
        end
      end

      # Setter para el atributo.
      attr_accessor attr_name

      # Define un metodo find_by_<atributo> que recibe un valor
      self.define_singleton_method("find_by_#{attr_name}") do |value|
        all_instances.find_all do |instance|
          instance.send(attr_name) == value
        end
      end

      # Guarda attribute_map en la coleccion. La clase Hash a su vez
      #   asegura que cada clave (nombre de atributo) sea unica.
      persistable_attributes[attr_name] = attribute_map
    end

    def has_many(type, constraints)

      # Marcar como atributo complejo.
      constraints[:complex] = true

      self.send(:has_one, type, constraints)
    end
  end

  module PersistableObject
    def save!
      entry = Hash.new                           # {id_atributo : valor}

      self.validate!

      self.class.send(:persistable_attributes)
        .select { |k, v| not(v.complex?) }
        .each_pair do |name, attr_map|

        value = if self.send(name).nil?
                  attr_map.default_value         # Valor por default.
                else
                  self.send(name)
                end

        entry[name] =
          if attr_map.primitive? then
            # Tipo basico.
            value
          else
            # ID.
            value.save!
          end
      end

      TADB::DB.table(self.class.to_s).delete(self.id)
      self.id = TADB::DB.table(self.class.to_s).insert(entry)

      # Una vez obtenido el ID, generar la tabla associativa.
      self.class.send(:persistable_attributes)
        .select { |k, v| v.complex? }.each_key do |name|

        table = TADB::DB.table("#{self.class.to_s}_#{name}_#{self.id}")

        self.send(name).each do |elem|
          elem.save!
          table.insert({id: elem.id})
        end
      end

      self.id
    end

    def refresh!
      if self.id.nil?
        raise "Error: Esta instancia no tiene id!"
      end

      # Obtener la instancia desde la BD.
      saved_instance = self.class.find_by_id(self.id).first

      # Setear todos los atributos con los valores de saved_instance.
      self.class.send(:persistable_attributes).each_key do |name|
        self.send("#{name}=", saved_instance.send(name))
      end

      self
    end

    def forget!
      if self.id.nil?
        raise "Error: Esta instancia no tiene id!"
      end

      TADB::DB.table(self.class.to_s).delete(self.id)

      # Elimina la tabla para los atributos complejos.
      self.class.send(:persistable_attributes)
        .select { |k, v| v.complex? }
        .each_pair do |name, attr_map|

        TADB::DB.table("#{self.class.to_s}_#{name}_#{self.id}").clear
      end

      self.id = nil
    end

    def validate!
      self.class.send(:persistable_attributes).each_pair do |name, attr_map|
        value = if self.send(name).nil?
                  attr_map.default_value         # Valor por default.
                else
                  self.send(name)
                end

        attr_map.validations.each do |validation|
          if attr_map.complex?
            value.each do |elem|
              self.instance_exec(name, elem, &validation)
            end
          else
            self.instance_exec(name, value, &validation)
          end
        end
      end
    end
  end

  module PersistableClass
    attr_accessor :all_instances

    def all_instances

      # Mapear cada Hash de atributos y valores a una
      #   instancia con esos atributos y valores.
      TADB::DB.table(self.to_s).entries.map do |entry|
        instance = self.new

        persistable_attributes.each_pair do |name, attr_map|
          if attr_map.primitive?
            instance.method("#{name}=").call(entry[name])
          elsif attr_map.complex?
            # Genera array de instancias.
            objs = TADB::DB.table("#{self.to_s}_#{name}_#{entry[:id]}")
              .entries.map do |entry|
              attr_map.type.find_by_id(entry[:id]).first
            end

            instance.method("#{name}=").call(objs)
          else
            instance.method("#{name}=").call(
              # Instancia de la clase referenciada con el id indicado.
              attr_map.type.find_by_id(entry[name]).first
            )
          end
        end

        instance
      end
    end
  end

  module Validation
    def self.type(arg)
      proc do |name, value|
        unless value.nil? then
          # Validacion de tipo.
          unless value.is_a?(arg) then
            raise "Error: \"#{name}\" debe ser un \"#{arg.to_s}\"."
          end

          value.validate! if arg.is_a?(ORM::PersistableClass)
        end
      end
    end

    def self.no_blank(arg)
      proc do |name, value|
        if arg && (value == nil || value == "")
          raise "Error: #{name.to_s} no puede tener un valor nulo."
        end
      end
    end

    def self.from(arg)
      proc do |name, value|
        if arg > value
          raise "Error: #{name.to_s} no puede ser menor que #{arg}."
        end
      end
    end

    def self.to(arg)
      proc do |name, value|
        if arg < value
          raise "Error: #{name.to_s} no puede ser mayor que #{arg}."
        end
      end
    end

    def self.validate(arg)
      proc do |name, value|
        unless value.instance_eval(&arg)
          raise "Error: Fallo la validacion (#{name.to_s})."
        end
      end
    end
  end
end

# Todas las clases y mixins conocen has_one().
Module.include ORM::DSL

Boolean = Module.new
TrueClass.include Boolean
FalseClass.include Boolean
