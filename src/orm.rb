require 'tadb'

# Estaria bueno poner los submodulos de ORM en distintos archivos
module ORM
  module DSL
    private

    # Devuelve una coleccion de tuplas que contienen:
    #   {clave=nombre_atributo, valor=hash_de_constraints}
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

      # Incluir el mixin que brinda las operaciones save, refresh y forget.
      include ORM::PersistableObject

      # Extender la clase agregando el metodo all_instances().
      extend ORM::PersistableClass

      # Getter para el atributo que devuelve un valor
      #   por default si este no esta definido.
      self.send(:define_method, attr_name) do
        if instance_variables.include?("@#{attr_name}".to_sym)
          self.instance_eval "@#{attr_name}"
        else
          constraints[:default]
        end
      end

      # Setter para el atributo.
      attr_writer attr_name

      # Define un metodo find_by_<atributo> que recibe un valor
      self.define_singleton_method("find_by_#{attr_name}") do |value|
        all_instances.find_all do |instance|
          instance.send(attr_name) == value
        end
      end

      # Definir id, id= y find_by_id con una llamada recursiva.
      has_one(String, named: :id) unless attr_name == :id

      # Agregar una clave :type que indica la clase del atributo.
      constraints[:type] = type

      # Guarda los constraints coleccion. La clase Hash a su vez
      #   asegura que cada clave (nombre de atributo) sea unica.
      persistable_attributes[attr_name] = constraints
    end
  end

  module PersistableObject
    def save!
      entry = Hash.new                           # {id_atributo : valor}

      self.class.send(:persistable_attributes).each_pair do |name, constraints|
        if [String, Numeric, Boolean].include?(constraints[:type]) then
          # Tipo basico.

          value = if self.send(name).nil?
                    constraints[:default]        # Valor por default.
                  else
                    self.send(name)
                  end

          # No blank.
          if constraints[:no_blank] && (value == nil || value == "")
            raise "Error: #{name.to_s} no puede tener un valor nulo."
          end

          # From (valor minimo).
          if constraints.key?(:from) && constraints[:from] > value
            raise "Error: #{name.to_s} no puede ser "\
              "menor que #{constraints[:from]}."
          end

          # To (valor maximo).
          if constraints.key?(:to) && constraints[:to] < value
            raise "Error: #{name.to_s} no puede ser "\
              "mayor que #{constraints[:to]}."
          end

          entry[name] = value
        else
          # Clave foranea.

          instance = if self.send(name).nil?
                       constraints[:default]     # Instancia por default.
                     else
                       self.send(name)
                     end

          # Validate (bloque de validacion).
          # TODO: En caso de ser array, se valida para
          #   cada uno de sus elementos.
          if constraints.key?(:validate) &&
              instance.instance_eval(&constraints[:validate])
            raise "Error: Fallo la validacion."
          end

          # Guarda la clave foranea.
          entry[name] = instance.save!
        end
      end

      TADB::DB.table(self.class.to_s).delete(self.id)
      self.id = TADB::DB.table(self.class.to_s).insert(entry)
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

      self.id = nil
    end

    def validate!
      self.class.send(:persistable_attributes).each_pair do |name, constraints|
        # Lanzar una excepcion a menos que sea
        #   nil o de la clase correspondiente.
        unless self.send(name).nil? &&
            self.send(name).is_a?(constraints[:type])
          raise "Error: \"#{name}\" debe ser un "\
            "\"#{constraints[:type].to_s}\"."
        end

        # TODO: Chequear tipos complejos.
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

        entry.each_pair do |attr, value|
          # Clase del atributo.
          type = persistable_attributes[attr][:type]

          if [String, Numeric, Boolean].include?(type)
            instance.method("#{attr}=").call(value)
          else
            instance.method("#{attr}=").call(
              # Instancia de la clase referenciada con el id indicado.
              type.find_by_id(value).first
            )
          end
        end

        instance
      end
    end
  end
end

# Todas las clases y mixins conocen has_one().
Module.include ORM::DSL

Boolean = Module.new
TrueClass.include Boolean
FalseClass.include Boolean
