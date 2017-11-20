package ar.edu.frba.tadp.test

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfter
import ar.edu.frba.tadp.TAdeQuest._
import scala.util.{Success, Failure}
import org.scalactic.source.Position.apply

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
          None,
          Some(armaduraEleganteSport),
          UnaMano(None, None),
          List(talismanDeDedicacion)))
  superman.equiparItem(espadaDeLaVida)
  
  object batman extends Heroe(
      ConjuntoStats(90, 50, 90, 80),
      Some(mago),
      Inventario())
  batman.equiparItem(armaduraEleganteSport)
  batman.equiparItem(cascoVikingo)

  //object robinHood
  //object ironman
  //object spiderman
  //object drStrange

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
  
  // Tranqui porque se gana sin hacer nada.
  object misionTranqui extends Mision(List.empty, ganarOroParaElPozoComun)

  object misionPeligrosa extends Mision(
      List(pelearContraMonstruo),
      encontrarUnNuevoHeroeQueSeSumeAlEquipo)

  object misionImposible extends Mision(
      List(pelearContraMonstruo, forzarPuerta, robarTalisman),
      incrementarLosStatsDeLosMiembrosDelEquipoQueCumplanUnaCondicion)

  /*EQUIPOS*/
  val equipoRocket = Equipo("rocket", List(), 10000)
  val vengadores = Equipo("avengers", List(), 20000)
  val ligaJusticia = Equipo("justiceLeague", List(), 15000)

  /**
   * 1 - FORJANDO UN HEROE
   */
  
  "obtener y alterear stats de un heroe" should
    "su fuerza base debe ser 10" in {
    	object superman extends Heroe(
    			ConjuntoStats(100, 120, 605, 80),
    			Some(guerrero),
    			Inventario(
    					None,
    					Some(armaduraEleganteSport),
    					UnaMano(None, None),
    					List(talismanDeDedicacion)))
    	superman.equiparItem(espadaDeLaVida)

      superman.hp(asignar)(10)
      HP(superman) === 10
  }

  //
  //  "un heroe se equipa con un item" should
  //    "gana un punto de experiencia por cada kg levantado" in {
  //
  //      val otroCharizard = unCharizard.doLevantar(10).pokemon
  //      assert(otroCharizard.experiencia === 20)
  //    }
  //
  //  "un heroe cambia de trabajo" should
  //    "gana dos puntos de experiencia por cada kg levantado" in {
  //
  //      val resultado = Normal(unMachop).realizarActividad(Actividades.levantar(10))
  //      assert(resultado === Normal(unMachop.copy(experiencia = 30)))
  //    }

  /**
   * 2 - HAY EQUIPO
   */
  //
  //  "obtener mejor heroe segun de un equipo" should
  //    "no poder levantar pesas" in {
  //
  //      val resultado = unGengar.doLevantar(10)
  //      assert(resultado == NoPuedeRealizar(unGengar))
  //    }
  //
  //  "obtener un item de un equipo" should
  //    "pierde 10 de energia y no gana experiencia" in {
  //
  //      val resultado = Normal(unMachop).realizarActividad(levantar(3000))
  //      assert(resultado === Paralizado(unMachop.copy(energia = 90)))
  //    }
  //
  //  "obtener un miembro de un equipo" should
  //    "pierde un punto de energia y gana 200 de experiencia" in {
  //      val otroMachop = unMachop.doNadar(1).pokemon
  //      assert(otroMachop.energia === 99)
  //    }
  //
  //  "reemplazar el miembro de un equipo" should
  //    "pierde 60 puntos de energia y
  //       gana 12000 de experiencia y gana un punto de velocidad" in {
  //
  //      val otroMagikarp = unMagikarp.doNadar(60).pokemon
  //      assert(otroMagikarp.experiencia === 12010)
  //    }
  //
  //  "obtener el lider de un equipo" should
  //    "queda en KO y no gana experiencia" in {
  //
  //    val resultado = Normal(unCharizard).realizarActividad(Actividades.nadar(1))
  //    assert(resultado == KO(unCharizard))
  //  }
  /**
   * 3 - MISIONES
   */
  //
  //  "cuando un pokemon paralizado levanta pesas" should
  //    "no gana experiencia y queda KO" in {
  //
  //      val resultado = Paralizado(unMachop).realizarActividad(Actividades.levantar(10))
  //      assert(resultado === KO(unMachop))
  //    }

  /**
   * 4 - LA TABERNA
   */
  //
  //  "elegir mision para un equipo" should
  //    "recupera su energía y queda Dormido" in {
  //
  //      val resultado = Normal(unMachop.copy(energia = 1)).realizarActividad(descansar)
  //      assert(resultado === Dormido(unMachop.copy(energia = unMachop.energiaMaxima)))
  //    }
  //
  //  "entrenar un equipo" should
  //    "se despierta después de tres actividades" in {
  //
  //      val resultado = Dormido(unMachop).realizarActividad(levantar(10))
  //      assert(resultado === Dormido(unMachop, 2))
  //
  //      val resultado2 = resultado.realizarActividad(levantar(10))
  //      assert(resultado2 === Dormido(unMachop, 1))
  //
  //      assert(resultado2.realizarActividad(levantar(10)) === Normal(unMachop))
  //    }

}
