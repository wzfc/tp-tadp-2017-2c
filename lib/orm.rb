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
        self.find_by(desc[:named], value)
      end

      # Agregarlo a la coleccion. La clase Hash a su vez asegura que cada
      #   clave (nombre de atributo) sea unica.
      persistable_attributes[desc[:named]] = type
    end
  end

  module DataManipulation
    attr_accessor :id

    def save!
      entry = Hash.new

      puts "Table #{self.class}:"

      self.class.send(:persistable_attributes).each_pair do
        |name, type|
        value = self.instance_variable_get("@#{name}")

        puts "\tSave #{name} = \"#{value}\" (#{type})"

        # {id_atributo : valor}
        entry[name] = value
      end

      # FIXME: all_instances no guarda las instancias sino la genera
      #   a partir de la BD.
      self.class.all_instances << self
      self.id = TADB::DB.table(self.class.to_s).insert(entry)
    end

    def refresh!
      puts "Refresh..."

      # FIXME: Esto esta mal porque debe mantener la misma id.
      # Eliminar la entrada por id y volver a guardar.
      self.forget!
      self.save!
    end

    def forget!
      if self.id.nil?
        raise "Error: #{self.to_s} no tiene id!"
      end

      puts "Forget #{self.to_s}"
      TADB::DB.table(self.class.to_s).delete(self.id)

      self.class.all_instances.delete(self)
      self.id = nil
    end
  end

  module DataHome
    attr_accessor :all_instances

    def all_instances
      # TODO: Obtener las instancias con los datos de la BD.
      @all_instances ||= Array.new
    end

    def find_by(attribute_name, value)
      all_instances.find_all do |instance|
        instance.send(attribute_name) == value
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
