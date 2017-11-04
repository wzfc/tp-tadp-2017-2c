package test

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import main.TAdeQuest._

class TAdeQuestSpec extends FlatSpec with Matchers {

  //  def exampleEspecie(tipo: TipoPokemon = Fuego,
  //                     tipoSecundario: Option[TipoPokemon] = None,
  //                     resistenciaEvolutiva: Int = 350,
  //                     caracteristicasBase: Caracteristicas = Caracteristicas(200, 100, 30),
  //                     incrementosCaracteristicas: Caracteristicas = Caracteristicas(10, 5, 2)) =
  //    Especie(tipo, tipoSecundario, resistenciaEvolutiva, caracteristicasBase, incrementosCaracteristicas)
  //
  //  def examplePokemon(
  //    nivel: Int = 1,
  //    experiencia: Int = 10,
  //    energia: Int = 100, energiaMaxima: Int = 200,
  //    fuerza: Int = 100, velocidad: Int = 30,
  //    especie: Especie = exampleEspecie()) = {
  //    Pokemon(experiencia, energia, especie)
  //  }
  //  
  //  import Actividades._
  //  
  //  implicit def tipoAEspecie(unTipo: TipoPokemon) = exampleEspecie(tipo = unTipo)
  //  implicit def tipoAEspecie(tipos: (TipoPokemon, TipoPokemon)) =
  //    exampleEspecie(tipo = tipos._1, tipoSecundario = Some(tipos._2))
  //
  //  val unCharizard = examplePokemon(especie = (Fuego, Volador))
  //  val unMachop = examplePokemon(especie = Pelea)
  //  val unGengar = examplePokemon(especie = Fantasma)
  //  val unMagikarp = examplePokemon(especie = Agua)
  
  /**
   * 1 - FORJANDO UN HEROE
   * */
  //
  //  "obtener y alterear stats de un heroe" should
  //    "su energía debería ser igual a la máxima" in {
  //      //Hacer descansar al charizard
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
  //      val resultado = Normal(unMachop)
  //      .realizarActividad(Actividades.levantar(10))
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
  //      val resultado = 
  //        Normal(unMachop).realizarActividad(levantar(3000))
  //      assert(resultado === Paralizado(unMachop.copy(energia = 90)))
  //    }
  //
  //  "obtener un miembro de un equipo" should
  //    "pierde un punto de energia y gana 200 de experiencia" in {
  //      val otroMachop = unMachop.doNadar(1).pokemon
  //
  //      assert(otroMachop.experiencia === 210)
  //      assert(otroMachop.energia === 99)
  //    }
  //
  //  "reemplazar el miembro de un equipo" should
  //    """pierde 60 puntos de energia y
  //       gana 12000 de experiencia y gana un punto de velocidad""" in {
  //
  //      val otroMagikarp = unMagikarp.doNadar(60).pokemon
  //
  //      assert(otroMagikarp.experiencia === 12010)
  //      assert(otroMagikarp.energia === 40)
  //      assert(otroMagikarp.velocidad === 97)
  //      assert(otroMagikarp.nivel === 34)
  // 
  //    }
  //  
  //  "obtener el lider de un equipo" should 
  //    "queda en KO y no gana experiencia" in {
  //     
  //    
  //    val resultado = Normal(unCharizard).realizarActividad(Actividades.nadar(1))
  //
  //    assert(resultado == KO(unCharizard))
  //  }
  /**
   * 3 - MISIONES
   * */
  //  
  //  "cuando un pokemon paralizado levanta pesas" should
  //    "no gana experiencia y queda KO" in {
  //
  //      val resultado = Paralizado(unMachop)
  //      .realizarActividad(Actividades.levantar(10))
  //      assert(resultado === KO(unMachop))
  //    }
  
  /**
   * 4 - LA TABERNA
   * */
  //  
  //  "elegir mision para un equipo" should
  //    "recupera su energía y queda Dormido" in {
  //
  //      val resultado = Normal(unMachop.copy(energia = 1))
  //        .realizarActividad(descansar)
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