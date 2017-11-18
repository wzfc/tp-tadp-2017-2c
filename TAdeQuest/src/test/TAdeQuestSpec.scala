package test

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfter
import com.TAdeQuest._

class TAdeQuestSpec extends FlatSpec with Matchers with BeforeAndAfter {

  /*TRABAJOS*/
  val guerrero = Trabajo(Some(ConjuntoStats(10, 15, 0, -10) + _), _.fuerza)
  val mago = Trabajo(Some(ConjuntoStats(0, -20, 0, 20) + _), _.inteligencia)
  val ladron = Trabajo(Some(ConjuntoStats(-5, 0, 10, 0) + _), _.velocidad)
//  val guerrero = ???
//  val mago = ???
//  val ladron = ???
  
  /*ITEMS*/
//  +10 hp, sólo lo pueden usar héroes con fuerza base > 30. Va en la cabeza.
  val cascoVikingo​ = ???
//  +20 inteligencia, sólo lo pueden usar magos (o ladrones con más de 30 ​de inteligencia ​​base). ​Una​ ​mano.
  val palitoMagico = ???
//  +30 velocidad, -30 ​hp. Armadura.
  val armaduraEleganteSport = ???
//  ​+2 fuerza.​ Ocupa ​las ​dos manos.
  val arcoViejo = ???
//  +20 hp. No pueden equiparlo los ladrones ni nadie con menos de 20 ​de​ fuerza base. Una mano.
  val escudoAntiRobo = ???
//  ​Todos los stats se incrementan 10% del valor del stat principal​ ​ del​ ​ trabajo.
  val talismanDeDedicacion = ???
//  +50 hp. -10 ​hp por ​cada ​otro ​item ​equipado.
  val talismanDelMinimalismo = ???
//  Si el héroe tiene más fuerza que inteligencia, +30 a la inteligencia; de lo contrario +10 a todos los stats menos la inteligencia. Sólo lo pueden ​equipar​ los​ héroes​ sin​ trabajo. ​Sombrero.
  val vinchaDelBufaloDeAgua = ???
//  Todos los ​stats ​son 1.
  val talismanMaldito = ???
//  ​Hace ​que la fuerza del héroe sea igual a ​su hp.
  val espadaDe​La​Vida = ???
  
  /*HEROES*/
  val superman = ???
  val batman = ???
  val robinHood = ???
  val ironman = ???
  val spiderman = ???
  val drStrange = ???
  
  /*TAREAS*/
//		  reduce la vida de cualquier héroe con fuerza < 20; facilidad de 10 para cualquier héroe o 20 si el líder del equipo es un guerrero;
  val pelearContraMonstruo = ???
//		  no le hace nada a los magos ni a los ladrones, pero sube la fuerza de todos los demás en 1 y baja en 5 su hp;facilidad igual a la inteligencia del héroe + 10 por cada ladrón en su equipo;
  val forzarPuerta = ???
//		  le agrega un talismán ​al heroe; facilidad igual a la velocidad ​del ​heroe, pero ​no​ puede ser hecho ​por equipos ​cuyo ​líder no​ sea un ​ladrón.
  val robarTalisman = ???
  
  /*RECOMPENSAS*/
  val ganarOroParaElPozoComun = ???
  val encontrarUnItem = ???
  val incrementarLosStatsDeLosMiembrosDelEquipoQueCumplanUnaCondicion = ???
  val encontrarUnNuevoHeroeQue​Se​Sume​Al​Equipo = ???
  
  /*MISIONES*/
  val misionTranqui = ???
  val misionPeligrosa = ???
  val misionImposible = ???
  
  /*EQUIPOS*/
  val equipoRocket = ???
  val vengadores = ???
  val ligaJusticia = ???

  /**
   * 1 - FORJANDO UN HEROE
   */
  //
  //  "obtener y alterear stats de un heroe" should
  //    "su energía debería ser igual a la máxima" in {
  //      val otroCharizard = unCharizard.doDescansar().pokemon
  //      assert(otroCharizard.energia === 200)
  //    }
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