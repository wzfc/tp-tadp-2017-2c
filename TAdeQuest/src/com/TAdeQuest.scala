package com

import scala.util.{Try, Success, Failure}

package object TAdeQuest extends App {
  case class Heroe(
      statsBase: ConjuntoStats,
      trabajo: Option[Trabajo] = None,
      inventario: Inventario = Inventario()) {

    def cambiarTrabajo(_trabajo: Trabajo) = copy(trabajo = Some(_trabajo))

    def equiparItem(item: Item) = item(this)

    def statsFinales: ConjuntoStats = {
      // Los stats finales no pueden ser menor a 1.
      val limitar: Stat => Stat = _ max 1
      
      // Por cada item del inventario, aplicar el efecto
      //   al heroe y obtener los stats base del heroe resultante. 
      val statsParciales =
        inventario.items.foldLeft(this) { (heroeParcial, item) =>
          // Foldear el efecto porque es un Option.
          item.efecto.fold(heroeParcial)(efecto => efecto(heroeParcial))
        }.statsBase
        
      // Aplicar los efectos del trabajo.
      trabajo.fold(statsParciales)(_(statsParciales)) match {
        case ConjuntoStats(hp, fuerza, velocidad, inteligencia) =>
          ConjuntoStats(limitar(hp), limitar(fuerza),
            limitar(velocidad), limitar(inteligencia))
      }
    }
    
    def incrementoStatPrincipal(item: Item): Int = {
      StatPrincipal(equiparItem(item)) - StatPrincipal(this)
    }
    
    def realizarTarea(tarea: Tarea): Heroe = tarea(this)
    
    def aumentarHP(cantidad: Int): Heroe = copy(statsBase.copy(hp = statsBase.hp + cantidad))
    
    def aumentarFuerza(cantidad: Int): Heroe = copy(statsBase.copy(fuerza = statsBase.fuerza + cantidad))
    
    def aumentarVelocidad(cantidad: Int): Heroe = copy(statsBase.copy(velocidad = statsBase.velocidad + cantidad))
    
    def aumentarInteligencia(cantidad: Int): Heroe = copy(statsBase.copy(inteligencia = statsBase.inteligencia + cantidad))
  }

  type Stat = Int

  case class ConjuntoStats(
      hp: Stat,
      fuerza: Stat,
      velocidad: Stat,
      inteligencia: Stat) {

    def +(stats: ConjuntoStats): ConjuntoStats = {
      copy(
        hp = hp + HP(stats),
        fuerza = fuerza + Fuerza(stats),
        velocidad = velocidad + Velocidad(stats),
        inteligencia = inteligencia + Inteligencia(stats))
    }
  }
  
  trait ValorStat {
    val apply : ConjuntoStats => Stat
    def apply(heroe: Heroe): Stat = apply(heroe.statsFinales)
  }
  
  object HP extends ValorStat {
	  val apply = _.hp
  }

  object Fuerza extends ValorStat {
	  val apply = _.fuerza
  }

  object Velocidad extends ValorStat {
	  val apply = _.velocidad
  }

  object Inteligencia extends ValorStat {
	  val apply = _.inteligencia
  }
  
  object StatPrincipal {
    def apply(heroe: Heroe) = heroe.trabajo.get.statPrincipal(heroe.statsFinales)
  }

  type EfectoTrabajo = ConjuntoStats => ConjuntoStats
  case class Trabajo(
      efecto: Option[EfectoTrabajo],
      statPrincipal: ConjuntoStats => Stat) {
    def apply(stats: ConjuntoStats) = {
      efecto.fold(stats)(trabajo => trabajo(stats))
    }
  }

  type EfectoItem = Heroe => Heroe

  trait Item {
    val efecto: Option[EfectoItem]
    val restricciones: List[Heroe => Boolean]
    val valor: Int

    val apply: Inventario => Inventario
    def apply(heroe: Heroe): Heroe = {
      heroe.copy(inventario = apply(heroe.inventario))
    }
  }

  trait ItemCabeza extends Item {
    val apply = _.copy(itemCabeza = Some(this))
  }

  trait ItemTorso extends Item {
    val apply = _.copy(itemTorso = Some(this))
  }

  trait Talisman extends Item {
    val apply = inventario => inventario.copy(talismanes = this :: inventario.talismanes)
  }

  trait ItemUnaMano extends Item {
    val apply = {inventario: Inventario =>
      val nuevoItemManos: ItemManos =
        inventario.itemManos match {
          case UnaMano(Some(x), _) => // Si la mano izquierda esta ocupada
            UnaMano(Some(x), Option(this))
          case UnaMano(None, x) => // Si la mano izquierda esta desocupada
            UnaMano(Some(this), x)
          case DosManos(_) =>
            UnaMano(None, Some(this))
        }
      
      inventario.copy(itemManos = nuevoItemManos)
    }
    
    override def apply(heroe: Heroe): Heroe = {
      heroe.copy(inventario = this(heroe.inventario))
    }
  }

  trait ItemDosManos extends Item {
    val apply = _.copy(itemManos = DosManos(Option(this)))
    override def apply(heroe: Heroe): Heroe = {
      heroe.copy(inventario = this(heroe.inventario))
    }
  }
  
  trait ItemManos {
    def items: List[Item]
  }

  case class UnaMano(
      manoIzquierda: Option[ItemUnaMano],
      manoDerecha: Option[ItemUnaMano])
    extends ItemManos {
    def items = List(manoIzquierda, manoDerecha).flatten
  }

  case class DosManos(item: Option[ItemDosManos]) extends ItemManos {
    def items = item.toList
  }
  
  case class Inventario(
      itemCabeza: Option[ItemCabeza] = None,
      itemTorso: Option[ItemTorso] = None,
      itemManos: ItemManos = UnaMano(None, None),
      talismanes: List[Talisman] = List.empty) {
    def items: List[Item] = {
      List(itemCabeza, itemTorso).flatten ++    // flatten elimina todos los None
      itemManos.items ++
      talismanes
    }
  }

  case class Equipo(
      nombre: String,
      heroes: List[Heroe],
      pozoComun: Int) {
    def mejorEquipoSegun(cuantificador: Heroe => Int) = {
      Try(heroes.maxBy(cuantificador(_))).toOption
    }
    
    def ganarOro(cantidad: Int): Equipo = copy(pozoComun = pozoComun + cantidad)
    
    def obtenerItem(item: Item): Equipo = {
      heroes.sortBy(_.incrementoStatPrincipal(item))
            .filter(_.incrementoStatPrincipal(item) > 0) match {
        case List()             => copy(pozoComun = pozoComun + item.valor)
        case heroe :: masHeroes => reemplazarMiembro(heroe)(item(heroe))
      }
    }

    def obtenerMiembro(miembro: Heroe): Equipo = {
      copy(heroes = miembro :: heroes)
    }
    
    def reemplazarMiembro(miembroAReemplazar: Heroe)(miembroNuevo: Heroe): Equipo = {
      copy(heroes = heroes.map {
        case `miembroAReemplazar` => miembroNuevo
        case otro => otro
      })
    }

    def lider(): Option[Heroe] = {
      heroes.sortBy(StatPrincipal(_)) match {
        // Si hay 2 maximos, no hay lider.
        case a :: b :: masHeroes
        if (StatPrincipal(a) == StatPrincipal(b)) => None

        case x => x.headOption
      }
    }
    
    def realizarMision(mision: Mision): Try[Equipo] = mision(this)
  }

  type EfectoTarea = Heroe => Heroe
  type Facilidad = Heroe => Try[Int]

  case class Tarea(
      efecto: Option[EfectoTarea],
      facilidad: Facilidad) {
    def apply(heroe: Heroe): Heroe = {
      efecto.fold(heroe)(efecto => efecto(heroe))
    }
  }
  
  type Recompensa = Equipo => Equipo
  type ResultadoMision = Try[Equipo]

  case class Mision(
      tareas: List[Tarea],
      recompensa: Recompensa) {
    def apply(equipo: Equipo): ResultadoMision = {
      tareas.foldLeft(Try(equipo)) { (resultadoParcial, tarea) =>
        resultadoParcial.flatMap{equipoParcial =>
            // Solo tomar los heroes que sean capaces de realizar la tarea.
            equipoParcial.heroes.filter(tarea.facilidad(_).isSuccess) match {
              case List() =>
                // Estado del equipo: equipo o equipoParcial?
                Failure(MisionFallidaException(equipo, tarea))
              case candidatos =>
                // Elegir al que le resulta mas facil.
                val elegido = candidatos.maxBy(tarea.facilidad(_).get)
                Success(equipo.reemplazarMiembro(elegido)(tarea(elegido)))
            }
        }
      }.map(recompensa(_))
    }
  }
  
  case class MisionFallidaException(
      equipo: Equipo,
      tareaFallida: Tarea) extends Exception

  case class Taberna(misiones: List[Mision]) {
    type Criterio = (Equipo, Equipo) => Boolean

	  def elegirMision(criterio: Criterio)(equipo: Equipo): Option[Mision] = {
      // Filtrar misiones realizables, luego ordenar por condicion.
	    misiones.filter(_(equipo).isSuccess).sortWith {
	      (m1: Mision, m2: Mision) => criterio(m1(equipo).get, m2(equipo).get)
	    }.headOption
	  }
    
    def entrenar(criterio: Criterio)(equipo: Equipo): Equipo = {
      elegirMision(criterio)(equipo).fold(equipo){ mision => entrenar(criterio)(mision(equipo).get)}
    }
  }
}