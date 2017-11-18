package main

import scala.util.{Try, Success, Failure}

package object TAdeQuest {
  case class Heroe(
      statsBase: ConjuntoStats,
      trabajo: Option[Trabajo],
      inventario: Inventario) {

    def cambiarTrabajo(_trabajo: Trabajo) = copy(trabajo = Some(_trabajo))

    def realizarTrabajo() = {
      copy(statsBase = trabajo.fold(statsBase)(_(statsBase)))
    }

    def equiparItem(item: Item) = item(this)

    def statsFinales: ConjuntoStats = {
      // Los stats finales no pueden ser menor a 1.
      val limitar: Stat => Stat = _ max 1
      
      // Por cada item del inventario, aplicar el efecto
      //   al heroe y obtener los stats base del heroe resultante. 
      inventario.items.foldLeft(this) { (heroeParcial, item) =>
        // Foldear el efecto porque es un Option.
        item.efecto.fold(heroeParcial)(efecto => efecto(heroeParcial))
      }.statsBase match {
        case ConjuntoStats(hp, fuerza, velocidad, inteligencia, statPrincipal) =>
          ConjuntoStats(limitar(hp), limitar(fuerza),
            limitar(velocidad), limitar(inteligencia), statPrincipal)
      }
    }
  }

  type Stat = Int

  case class ConjuntoStats(
      hp: Stat,
      fuerza: Stat,
      velocidad: Stat,
      inteligencia: Stat,
      statPrincipal: ConjuntoStats => Stat) {

    def +(stats: ConjuntoStats): ConjuntoStats = {
      copy(
        hp = hp + HP(stats),
        fuerza = fuerza + Fuerza(stats),
        velocidad = velocidad + Velocidad(stats),
        inteligencia = inteligencia + Inteligencia(stats))
    }

    def valorStatPrincipal: Stat = statPrincipal(this)

    def incrementoRespectoA(statsAnteriores: ConjuntoStats) = {
      this.valorStatPrincipal - statsAnteriores.valorStatPrincipal
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
  
  object StatPrincipal extends ValorStat {
    val apply = _.valorStatPrincipal
  }

  type EfectoTrabajo = ConjuntoStats => ConjuntoStats
  case class Trabajo(
      efecto: Option[EfectoTrabajo],
      statPrincipal: Stat) {
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
    override def apply(heroe: Heroe): Heroe = {
      val nuevoItemManos: ItemManos =
        heroe.inventario.itemManos match {
          case UnaMano(Some(x), _) =>       // Si la mano izquierda esta ocupada
            UnaMano(Some(x), Option(this))
          case UnaMano(None, x) =>          // Si la mano izquierda esta desocupada
            UnaMano(Option(this), x)
          case DosManos(_) =>
            UnaMano(None, Option(this))
        }

      heroe.copy(inventario = heroe.inventario.copy(itemManos = nuevoItemManos))
    }
  }

  trait ItemDosManos extends Item {
    override def apply(heroe: Heroe): Heroe = {
      heroe.copy(inventario = heroe.inventario.copy(itemManos = DosManos(Option(this))))
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
    
    def obtenerItem(item: Item): Equipo = {
      val candidatos =
        for {
          heroe <- heroes
          heroeEquipado = item(heroe)
          if heroeEquipado.statsFinales.incrementoRespectoA(heroe.statsFinales) > 0
        } yield heroeEquipado

      Try(candidatos.maxBy(_.statsFinales.valorStatPrincipal)) match {
        case Success(heroe) =>
          reemplazarMiembro(heroe, item(heroe))    // Se lo reemplaza por el heroe equipado. 
        case Failure(_) =>
          copy(pozoComun = pozoComun + item.valor)
      }
    }

    def obtenerMiembro(miembro: Heroe): Equipo = {
      copy(heroes = miembro :: heroes)
    }
    
    def reemplazarMiembro(miembroReemplazado: Heroe, miembroNuevo: Heroe): Equipo = {
      copy(heroes = heroes.map {
        case `miembroReemplazado` => miembroNuevo
        case otro => otro
      })
    }

    def lider = ???
  }

  type EfectoTarea = Heroe => Heroe
  type Facilidad = Heroe => Int

  case class Tarea(
      efecto: Option[EfectoTarea],
      facilidad: Facilidad) {
    def apply(heroe: Heroe): Heroe = {
      efecto.fold(heroe)(efecto => efecto(heroe))
    }
  }
}
