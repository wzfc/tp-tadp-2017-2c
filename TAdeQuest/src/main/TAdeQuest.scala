package ar.edu.tadp

package object TAdeQuest {
  case class Heroe(
      stats: ConjuntoStats,
      trabajo: Trabajo) {
    def cambiarTrabajo(_trabajo: Trabajo) = copy(trabajo = _trabajo)
    def realizarTrabajo() = copy(stats = trabajo(stats))
    def equiparItem(item: Item) = item(this)
  }

  case class ConjuntoStats(
    hp: Stat,
    fuerza: Stat,
    velocidad: Stat,
    inteligencia: Stat)

  case class Stat(valor: Int, valorBase: Int) {
    require(valorBase > 1, "El valor base debe ser positivo.")
    require(valor > valorBase, "El valor debe ser mayor al valor base.")
  }

  type EfectoTrabajo = ConjuntoStats => ConjuntoStats
  case class Trabajo(
      efectos: List[EfectoTrabajo],
      statPrincipal: Stat) {
    def apply(stats: ConjuntoStats) = {
      efectos.foldLeft(stats)((statsParcial, trabajo) => trabajo(statsParcial))
    }
  }

  type EfectoItem = Heroe => Heroe

  trait Item {
    val efectos: List[EfectoItem]
    def apply(heroe: Heroe) : Heroe
  }

  trait ItemCabeza extends Item
  trait ItemTorso extends Item
  trait ItemMano extends Item
  trait Talisman extends Item

  case class Inventario(
    itemCabeza: Option[ItemCabeza] = None,
    itemTorso: Option[ItemTorso] = None,
    itemManoIzq: Option[ItemMano] = None,
    itemManoDer: Option[ItemMano] = None,
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
