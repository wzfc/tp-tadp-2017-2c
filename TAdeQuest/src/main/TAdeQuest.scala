package main

package object TAdeQuest {
  case class Heroe(
      statsBase: ConjuntoStats,
      trabajo: Trabajo,
      inventario: Inventario) {
    def cambiarTrabajo(_trabajo: Trabajo) = copy(trabajo = _trabajo)
    def realizarTrabajo() = copy(statsBase = trabajo(statsBase))
    def equiparItem(item: Item) = item(this)
    def statsFinales: ConjuntoStats = ??? //Tomar los items y aplicarlos a los stat base
  }

  trait Stat {
    def apply(c: ConjuntoStats) : Int
  }
//  object Fuerza extends Stat {
//    def apply(c: ConjuntoStats) : Int = c.fuerza
//  }
  
  
  case class ConjuntoStats(
    hp: Stat,
    fuerza: Stat,
    velocidad: Stat,
    inteligencia: Stat)

//  case class Stat(valor: Int, valorBase: Int) {
//    require(valorBase > 1, "El valor base debe ser positivo.")
//    require(valor > valorBase, "El valor debe ser mayor al valor base.")
//  }

  type EfectoTrabajo = ConjuntoStats => ConjuntoStats
  case class Trabajo(
      efecto: Option[EfectoTrabajo],
      statPrincipal: Stat) {
    def apply(stats: ConjuntoStats) = {
      efecto.foldLeft(stats)((statsParcial, trabajo) => trabajo(statsParcial))
    }
  }

  type EfectoItem = Heroe => Heroe

  trait Item {
    val efecto: Option[EfectoItem]
    val restricciones: List[Heroe => Boolean]
    def apply(heroe: Heroe) : Heroe = ???
  }

  trait ItemCabeza extends Item
  trait ItemTorso extends Item
  trait ItemMano extends Item
  trait Talisman extends Item
  
  trait ItemManos extends Item
//  case class UnaMano(manoIzquierda: Option[ItemMano], manoDerecha: Option[ItemMano]) extends ItemManos
//  case class DosManos(item: Option[ItemMano]) extends ItemManos

  case class Inventario(
    itemCabeza: Option[ItemCabeza] = None,
    itemTorso: Option[ItemTorso] = None,
    itemManos: ItemManos,
    talismanes: List[Talisman] = List.empty)

  case class Equipo(
      nombre: String,
      heroes: List[Heroe],
      pozoComun: Int) {
    def mejorEquipoSegun(cuantificador: Heroe => Int) = {
      heroes.maxBy(cuantificador(_))
    }
  }

  type Tarea = Heroe => Heroe
}
