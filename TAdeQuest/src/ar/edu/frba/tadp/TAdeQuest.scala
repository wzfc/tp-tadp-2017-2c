package ar.edu.frba.tadp

import scala.util.{Try, Success, Failure}

package object TAdeQuest {
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
          item.efecto(heroeParcial)
        }.statsBase
        
      // Aplicar los efectos del trabajo.
      trabajo.fold(statsParciales)(_(statsParciales)) match {
        case ConjuntoStats(hp, fuerza, velocidad, inteligencia) =>
          ConjuntoStats(limitar(hp), limitar(fuerza),
            limitar(velocidad), limitar(inteligencia))
      }
    }
    
    def incrementoStatPrincipal(item: Item): Try[Int] = {
      (equiparItem(item).map(StatPrincipal(_)), StatPrincipal(this)) match {
        case (Success(Some(a)), Some(b)) => Success(a + b)
        case (Failure(_), _)             => Failure(new Exception("¡No puede equipar este item!"))
        case _                           => Failure(new Exception("¡No posee stat principal!"))
      }
    }
    
    def realizarTarea(tarea: Tarea): Heroe = tarea(this)
    
    def hp[A](operador: (Stat, A) => Stat)(arg: A) = {
      copy(statsBase.copy(hp = operador(statsBase.hp, arg)))
    }
    
    def fuerza[A](operador: (Stat, A) => Stat)(arg: A) = {
      copy(statsBase.copy(fuerza = operador(statsBase.fuerza, arg)))
    }
    
    def velocidad[A](operador: (Stat, A) => Stat)(arg: A) = {
      copy(statsBase.copy(velocidad = operador(statsBase.velocidad, arg)))
    }
    
    def inteligencia[A](operador: (Stat, A) => Stat)(arg: A) = {
      copy(statsBase.copy(inteligencia = operador(statsBase.inteligencia, arg)))
    }
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
    def apply(heroe: Heroe): Option[Stat] = {
      heroe.trabajo.map(_.statPrincipal(heroe.statsFinales))
    }
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
    val efecto: EfectoItem = x => x
    val restricciones: List[Heroe => Boolean] = List.empty
    val valor: Int = 0

    val apply: Inventario => Inventario

    def apply(heroe: Heroe): Try[Heroe] = {
      if (validarRestricciones(heroe))
        Success(heroe.copy(inventario = apply(heroe.inventario)))
      else
        Failure(RestriccionItemNoCumplidaException(heroe))
    }
    
    def validarRestricciones(heroe: Heroe) = restricciones.forall(_(heroe))
  }
  
  case class RestriccionItemNoCumplidaException(heroe: Heroe) extends Exception

  trait ItemCabeza extends Item {
    val apply = _.copy(itemCabeza = Some(this))
  }

  trait ItemTorso extends Item {
    val apply = _.copy(itemTorso = Some(this))
  }

  trait Talisman extends Item {
    val apply = inventario => inventario.copy(talismanes = this :: inventario.talismanes)
  }

  trait ItemMano extends Item {
    override def apply(heroe: Heroe): Try[Heroe] = {
      if (validarRestricciones(heroe))
        Success(heroe.copy(inventario = this(heroe.inventario)))
      else
        Failure(RestriccionItemNoCumplidaException(heroe))
    }
  }
  
  trait ItemUnaMano extends ItemMano {
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
  }

  trait ItemDosManos extends ItemMano {
    val apply = _.copy(itemManos = DosManos(Option(this)))
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
      val candidatos =
        for {
          heroe <- heroes
          incremento = heroe.incrementoStatPrincipal(item).getOrElse(-1)
          if incremento > 0
        } yield heroe
      
      // get() no falla porque el for descarta los Failure.
      candidatos.sortBy(_.incrementoStatPrincipal(item).get) match {
        case List()     => copy(pozoComun = pozoComun + item.valor)
        case heroe :: _ => reemplazarMiembro(heroe)(item(heroe).get)
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
      heroes.filter(StatPrincipal(_).isDefined)
            .sortBy(StatPrincipal(_).get) match {
        // Si hay 2 maximos, no hay lider.
        case a :: b :: masHeroes
        if (StatPrincipal(a) == StatPrincipal(b)) => None

        case x => x.headOption
      }
    }
    
    def realizarMision(mision: Mision): Try[Equipo] = mision(this)
  }

  type EfectoTarea = Heroe => Heroe
  type Facilidad = (Heroe, Equipo) => Try[Int]

  case class Tarea(
      efecto: EfectoTarea,
      facilidad: Facilidad) {
    def apply(heroe: Heroe): Heroe = efecto(heroe)
  }
  
  type Recompensa = Equipo => Equipo
  type ResultadoMision = Try[Equipo]

  case class Mision(
      tareas: List[Tarea],
      recompensa: Recompensa) {
    def apply(equipo: Equipo): ResultadoMision = {
      tareas.foldLeft(Try(equipo)) { (resultadoParcial, tarea) =>
        resultadoParcial.flatMap { equipoParcial =>
          // Solo tomar los heroes que sean capaces de realizar la tarea.
          equipoParcial.heroes.filter(tarea.facilidad(_, equipo).isSuccess) match {
            case List() =>
              // Estado del equipo: equipo o equipoParcial?
              Failure(MisionFallidaException(equipo, tarea))
            case candidatos =>
              // Elegir al que le resulta mas facil.
              val elegido = candidatos.maxBy(tarea.facilidad(_, equipo).get)
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
      elegirMision(criterio)(equipo).fold(equipo){ mision =>
        entrenar(criterio)(mision(equipo).get)
      }
    }
  }
}