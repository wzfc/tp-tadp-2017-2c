package main

import scala.util.{Try, Success, Failure}

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

  type Stat = Int

  case class ConjuntoStats(
      hp: Stat,
      fuerza: Stat,
      velocidad: Stat,
      inteligencia: Stat,
      statPrincipal: ConjuntoStats => Stat) {
    def valorStatPrincipal: Stat = statPrincipal(this)
    def incrementoRespectoA(statsAnteriores: ConjuntoStats) = {
      this.valorStatPrincipal - statsAnteriores.valorStatPrincipal
    }
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
    def apply(heroe: Heroe) : Heroe = ???
  }

  trait ItemCabeza extends Item
  trait ItemTorso extends Item
  trait ItemMano extends Item
  trait Talisman extends Item
  
  trait ItemManos

  case class UnaMano(
    manoIzquierda: Option[ItemMano],
    manoDerecha: Option[ItemMano]) extends ItemManos

  case class DosManos(item: Option[ItemMano]) extends ItemManos

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
    def apply(heroe: Heroe) = {
      efecto.fold(heroe)(efecto => efecto(heroe))
    }
  }
}
