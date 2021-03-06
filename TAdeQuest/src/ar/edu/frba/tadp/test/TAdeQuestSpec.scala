package ar.edu.frba.tadp.test

import scala.util.Failure
import scala.util.Success

import org.scalatest.BeforeAndAfter
import org.scalatest.FlatSpec
import org.scalatest.Matchers

import ar.edu.frba.tadp.TAdeQuest._

class TAdeQuestSpec extends FlatSpec with Matchers with BeforeAndAfter {

  /*TRABAJOS*/
  object guerrero extends Trabajo(Some(ConjuntoStats(10, 15, 0, -10) + _), _.fuerza)
  object mago extends Trabajo(Some(ConjuntoStats(0, -20, 0, 20) + _), _.inteligencia)
  object ladron extends Trabajo(Some(ConjuntoStats(-5, 0, 10, 0) + _), _.velocidad)

  /*ITEMS*/

  def + = (a: Stat, b: Stat) => a + b
  def - = (a: Stat, b: Stat) => a - b
  
  //  +10 hp, sólo lo pueden usar héroes con fuerza base > 30. Va en la cabeza.
  object cascoVikingo extends ItemCabeza {
    override val efecto = _.hp(+)(10)
    override val restricciones = List(_.statsBase.fuerza > 30)
  }

  //  +20 inteligencia, sólo lo pueden usar magos (o ladrones con más de 30 de inteligencia base). Una mano.
  object palitoMagico extends ItemUnaMano {
    override val efecto = _.inteligencia(+)(20)
    override val restricciones = List(_.statsBase.inteligencia > 30)
  }

  //  +30 velocidad, -30 hp. Armadura.
  object armaduraEleganteSport extends ItemTorso {
    override val efecto = _.velocidad(+)(30).hp(-)(30)
  }

  //  +2 fuerza. Ocupa las dos manos.
  object arcoViejo extends ItemDosManos {
    override val efecto = _.fuerza(+)(2)
  }

  //  +20 hp. No pueden equiparlo los ladrones ni nadie con menos de 20 de fuerza base. Una mano.
  object escudoAntiRobo extends ItemUnaMano {
    override val efecto = _.hp(+)(20)
    override val restricciones = List(_.trabajo != ladron, _.statsBase.fuerza > 20)
  }

  implicit class MultiplicarConjuntoStat(stats: ConjuntoStats) {
    def *(factor: Double) = {
      stats.copy(
          hp = (stats.hp * factor).toInt,
          fuerza = (stats.fuerza * factor).toInt,
          velocidad = (stats.velocidad * factor).toInt,
          inteligencia = (stats.inteligencia * factor).toInt)
    } 
  }
  
  //  Todos los stats se incrementan 10% del valor del stat principal  del  trabajo.
  object talismanDeDedicacion extends Talisman {
    override val efecto = { heroe =>
      val incremento = (StatPrincipal(heroe).getOrElse(0) * 1.1).toInt
      heroe.copy(statsBase = heroe.statsBase +
          ConjuntoStats(incremento, incremento, incremento, incremento))
    }
  }

  //  +50 hp. -10 hp por cada otro item equipado.
  object talismanDelMinimalismo extends Talisman {
    override val efecto = heroe => heroe.hp(+)(50).hp(-)(heroe.inventario.items.size * 10)
  }

  //  Si el héroe tiene más fuerza que inteligencia, +30 a la inteligencia;
  //    de lo contrario +10 a todos los stats menos la inteligencia.
  //  Sólo lo pueden equipar los héroes sin trabajo. Sombrero.
  object vinchaDelBufaloDeAgua extends ItemCabeza {
    override val efecto = { heroe =>
      heroe match {
        case Heroe(ConjuntoStats(_, fuerza, _, inteligencia), _, _)
          if fuerza > inteligencia => heroe.inteligencia(+)(30)
        case _ => heroe.copy(statsBase = heroe.statsBase + ConjuntoStats(10, 10, 0, 10))
      }
    }
    override val restricciones = List(_.trabajo.isEmpty)
  }

  //  Todos los stats son 1.
  object talismanMaldito extends Talisman {
    override val efecto = _.copy(statsBase = ConjuntoStats(1, 1, 1, 1))
    override val valor = 50
  }
  
  def asignar(stat: Stat, valor: Stat) = valor
  
  //  Hace que la fuerza del héroe sea igual a su hp.
  object espadaDeLaVida extends ItemUnaMano {
    override val efecto = heroe => heroe.fuerza(asignar)(HP(heroe))
  }

  /*HEROES*/
  object superman extends Heroe(
      ConjuntoStats(100, 120, 605, 80),
      Some(guerrero),
      Inventario(
          itemCabeza = None,
          itemTorso  = Some(armaduraEleganteSport),
          itemManos  = UnaMano(None, None),
          talismanes = List(talismanDelMinimalismo)))
  
  object batman extends Heroe(
      ConjuntoStats(90, 50, 90, 80),
      Some(mago),
      Inventario(
          itemCabeza = Some(cascoVikingo),
          itemTorso  = Some(armaduraEleganteSport)))
  
  object robinHood extends Heroe(
      ConjuntoStats(40, 30, 70, 80),
      Some(ladron),
      Inventario(
          itemManos  = DosManos(Some(arcoViejo)),
          talismanes = List()))

  object ironman extends Heroe(
    ConjuntoStats(40, 100, 80, 80),
    Some(guerrero),
    Inventario(
        itemCabeza = Some(vinchaDelBufaloDeAgua),
        itemTorso  = Some(armaduraEleganteSport)))

  object spiderman extends Heroe(
    ConjuntoStats(100, 40, 70, 80),
    Some(guerrero),
    Inventario(
        itemManos  = UnaMano(Some(escudoAntiRobo), Some(palitoMagico)),
        itemCabeza = Some(vinchaDelBufaloDeAgua)))

  object drStrange extends Heroe(
    ConjuntoStats(40, 30, 70, 80),
    Some(mago),
    Inventario(
        itemManos = UnaMano(Some(palitoMagico), Some(espadaDeLaVida))))

  object drFrio extends Heroe(
    ConjuntoStats(50, 200, 100, 80),
    Some(ladron),
    Inventario(
        itemTorso  = Some(armaduraEleganteSport),
        itemCabeza = Some(vinchaDelBufaloDeAgua)))

  /*TAREAS*/
  //		  reduce la vida de cualquier héroe con fuerza < 20;
  //      facilidad de 10 para cualquier héroe o 20 si el líder del equipo es un guerrero;
  object pelearContraMonstruo extends Tarea(
      efecto = _ match {
        case x if Fuerza(x) < 20 => x.hp(-)(1)
        case y => y
      },
      facilidad = _ match {
        case x if _.lider == guerrero => Success(20)
        case _ => Success(10)
      })

  //  no le hace nada a los magos ni a los ladrones,
  //    pero sube la fuerza de todos los demás en 1 y baja en 5 su hp;
  //  facilidad igual a la inteligencia del héroe + 10 por cada ladrón en su equipo;
  object forzarPuerta extends Tarea(
      efecto = _ match {
        case x if x == mago || x == ladron => x
        case y => y.fuerza(+)(1).hp(-)(5)
      },
      facilidad = { (heroe, equipo) =>
        Success(equipo.heroes.filter(_.trabajo == ladron).size * 10 + Inteligencia(heroe))
      })
  
  // le agrega un talismán al heroe;
  // facilidad igual a la velocidad del heroe,
  //   pero no puede ser hecho por equipos cuyo líder no sea un ladrón.
  object robarTalisman extends Tarea(
      efecto = talismanDeDedicacion(_).get,
      facilidad = { (heroe, equipo) =>
        if (equipo.lider == ladron) Failure(new Exception("¡El lider es un ladron!"))
        else Success(Velocidad(heroe))
      })

  /*RECOMPENSAS*/
  val cantidadOro = 20
  val ganarOroParaElPozoComun: Recompensa = _.ganarOro(cantidadOro)

  val itemEncontrado = arcoViejo
  val encontrarUnItem: Recompensa = _.obtenerItem(itemEncontrado)

  // Incrementar los stats de los heroes que tienen HP < 10.
  val condicion: Heroe => Boolean = HP(_) < 10
  val incrementoStats = ConjuntoStats(10, 50, 9, 7)
  val incrementarLosStatsDeLosMiembrosDelEquipoQueCumplanUnaCondicion: Recompensa = 
    equipo => equipo.copy(heroes = equipo.heroes.map { heroe =>
      if (condicion(heroe)) heroe.copy(statsBase = heroe.statsBase + incrementoStats)
      else heroe
    })

  val heroeNuevo = batman
  val encontrarUnNuevoHeroeQueSeSumeAlEquipo: Recompensa = _.obtenerMiembro(heroeNuevo)

  /*MISIONES*/
  
  object misionTranqui extends Mision(List(forzarPuerta), ganarOroParaElPozoComun)

  object misionPeligrosa extends Mision(
      List(pelearContraMonstruo),
      encontrarUnNuevoHeroeQueSeSumeAlEquipo)

  object tareaImposible extends Tarea(
      efecto = x => x,
      facilidad = (h, e) => Failure(new Exception("Tarea Imposible")))

  object misionImposible extends Mision(
      List(pelearContraMonstruo, forzarPuerta, robarTalisman, tareaImposible),
      incrementarLosStatsDeLosMiembrosDelEquipoQueCumplanUnaCondicion)

  /*EQUIPOS*/
  val equipoRocket = Equipo("rocket", List(robinHood, drFrio), 10000)
  val vengadores = Equipo("avengers", List(spiderman, ironman, drStrange), 20000)
  val ligaJusticia = Equipo("justiceLeague", List(batman, superman), 15000)
  
  /*TABERNA*/
  
  val taberna = Taberna(List(misionTranqui, misionPeligrosa, misionImposible))
  
  val criterio = (e1: Equipo, e2: Equipo) => e1.pozoComun > e2.pozoComun

  /**
   * 1 - FORJANDO UN HEROE
   */
  
  "obtener y alterear stats de un heroe" should
    "su fuerza base debe ser 110" in {
      val resultado = superman.hp(asignar)(110)
      assert(HP(resultado.statsBase) === 110)
  }


  "un heroe se equipa con un item" should
    "aumenta la cantidad de items en el inventario" in {

      val cantidad = superman.inventario.items.size
      val resultado = superman.equiparItem(espadaDeLaVida)
      assert(resultado.get.inventario.items.size === cantidad + 1)
    }

  "un heroe cambia de trabajo" should
    "cambiar su stat principal" in {

      val resultado = robinHood.cambiarTrabajo(mago)
      assert(resultado !== robinHood)
    }

  /**
   * 2 - HAY EQUIPO
   */

  "obtener mejor heroe segun de un equipo" should
    "retorna un heroe que cumple el criterio" in {

      val heroe = ligaJusticia.mejorEquipoSegun(Fuerza(_))
      assert(heroe === Some(superman))
    }

  "obtener un item de un equipo" should
    "uno de los miembros obtiene el item" in {

      val resultado = vengadores.obtenerItem(arcoViejo)
      assert(resultado.heroes.flatMap(_.inventario.items).contains(arcoViejo))
    }

  "obtener un item de un equipo 2" should
    "incrementa el pozo comun del equipo" in {

      val pozoComun = vengadores.pozoComun
      val resultado = vengadores.obtenerItem(talismanMaldito)
      assert(resultado.pozoComun === pozoComun + talismanMaldito.valor)
    }

  "obtener un miembro de un equipo" should
    "se suma un miembro al equipo" in {
    
      val resultado = vengadores.obtenerMiembro(batman)
      assert(resultado.heroes.contains(batman))
    }

  "reemplazar el miembro de un equipo" should
    "obtener un miembro nuevo " in {

      val resultado = vengadores.reemplazarMiembro(ironman)(batman)
      assert(!resultado.heroes.contains(ironman))
    }

  "obtener el lider de un equipo" should
    "darme el lider de ese equipo" in {

    val resultado = equipoRocket.lider()
    assert(resultado === Some(drFrio))
  }
  /**
   * 3 - MISIONES
   */

  "cuando un equipo puede realizar una mision" should
    "cobra toda la recompensa" in {

      val pozoInicial = vengadores.pozoComun
      val resultado = vengadores.realizarMision(misionPeligrosa)
      assert(pozoInicial === resultado.get.pozoComun)
    }
  
  "cuando un equipo no puede realizar una mision" should
    "avisar que no puede realizar la mision" in {
    
      val resultado = ligaJusticia.realizarMision(misionImposible)
      assert(resultado.isFailure)
    }

  /**
   * 4 - LA TABERNA
   */

  "elegir mision para un equipo" should
    "retornar la mision adecuada para un equipo" in {

      val resultado = taberna.elegirMision(criterio)(vengadores)
      assert(resultado.get === misionTranqui)
    }

  "entrenar un equipo" should
    "finaliza las misiones y devuelve el equipo entrenado" in {
      // Disminuye HP y requiere HP >= 10
      val tarea = Tarea(
          efecto    = _.hp(-)(10),
          facilidad = (h, e) => if (HP(h.statsBase) >= 10) Success(10)
                                else Failure(new Exception()))
      val mision = Mision(
          tareas     = List(tarea),
          recompensa = x => x)
          
      val taberna = Taberna(List(mision))

      // Entrenar hasta que todos tengan HP < 10.
      val resultado = taberna.entrenar(criterio)(ligaJusticia)
      assert(resultado.heroes.forall(_.statsBase.hp < 10))
    }

}
