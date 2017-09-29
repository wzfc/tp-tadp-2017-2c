require 'tadb'

# Estaria bueno poner los submodulos de ORM en distintos archivos
module ORM
  module DSL
    private

    # Devuelve una coleccion de tuplas que contienen:
    #   {clave=nombre_atributo, valor=tipo_de_dato}
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

    def has_one(type, desc)
      # TODO: Extender la cantidad de tipos admitidos.
      unless [String, Numeric, Boolean].include?(type) then
        raise "Error: un atributo debe ser String, Numeric o Boolean"
      end

      # Incluir el mixin que brinda las operaciones save, refresh y forget.
      include ORM::DataManipulation

      # Extender la clase agregando el metodo all_instances()
      extend ORM::DataHome

      # Getter y setter para el atributo.
      attr_accessor desc[:named]

      # Define un metodo find_by_<atributo> que recibe un valor
      self.define_singleton_method("find_by_#{desc[:named]}") do |value|
        all_instances.find_all do |instance|
          instance.send(desc[:named]) == value
        end
      end

      # Definir id, id= y find_by_id con una llamada recursiva.
      has_one(String, named: :id) unless desc[:named] == :id

      # Agregarlo a la coleccion. La clase Hash a su vez asegura que cada
      #   clave (nombre de atributo) sea unica.
      persistable_attributes[desc[:named]] = type
    end
  end

  module DataManipulation
    def save!
      entry = Hash.new

      self.class.send(:persistable_attributes).each_key do |name|
        # {id_atributo : valor}
        entry[name] = self.instance_variable_get("@#{name}")
      end

      TADB::DB.table(self.class.to_s).delete(self.id)
      self.id = TADB::DB.table(self.class.to_s).insert(entry)
    end

    def refresh!
      if self.id.nil?
        raise "Error: Esta instancia no tiene id!"
      end

      TADB::DB.table(self.class.to_s).entries.each do |entry|
        entry.each_pair do |attr, value|
          self.method("#{attr}=").call(value)
        end
      end

      nil
    end

    def forget!
      if self.id.nil?
        raise "Error: Esta instancia no tiene id!"
      end

      TADB::DB.table(self.class.to_s).delete(self.id)

      self.id = nil
    end
  end

  module DataHome
    attr_accessor :all_instances

    def all_instances

      # Mapear cada Hash de atributos y valores a una
      #   instancia con esos atributos y valores.
      TADB::DB.table(self.to_s).entries.map do |entry|
        instance = self.new

        entry.each_pair do |attr, value|
          instance.method("#{attr}=").call(value)
        end

        instance
      end
    end
  end
end

# Todas las clases y mixins conocen has_one().
Module.include ORM::DSL

# REVIEW: No es la mejor solucion
Boolean = Module.new
TrueClass.include Boolean
FalseClass.include Boolean
