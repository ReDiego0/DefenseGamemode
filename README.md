<h1 align="center">
  <br>Defense Gamemode
</h1>
<p align="center">
    <img src="https://img.shields.io/badge/kotlin-2.3.10-blue.svg?style=flat-square"/>
    <img src="https://img.shields.io/badge/paper--1.21.11-gray.svg?style=flat-square"/>
</p>
<p align="center">
  Modalidad de defensa de objetivo inspirada en Warframe y Destiny.
</p>

### Requerimientos
- Servidor Paper 1.21.11
- WorldGuard 7.0.9+

## Guía de Administración

### Instalación y Configuración de Mapas
1. Depositar los mundos base (plantillas) en el directorio `plugins/DefenseGamemode/templates/`.
2. Ingresar al servidor con permisos de administrador (`defense.setup`).
3. Ejecutar el comando `/defense setup start <mapa>` para inicializar la configuración de la arena.
4. Configurar el punto de aparición de jugadores, la ubicación del objetivo a defender y los vectores de aparición de entidades hostiles.

### Estructura de Archivos y Configuración
- `mobs.yml`: Definición de grupos de entidades (pools) y sus pesos matemáticos de aparición.
- `armors.yml` / `consumables.yml`: Configuración de equipamiento base, estadísticas y suministros tácticos.
- `class_weapons.yml` / `exotic_weapons.yml`: Configuración de armas propias de clases y armas exóticas.
- `missions/`: Directorio que contiene configuraciones individuales por misión (multiplicadores de dificultad, tablas de recompensas, rotaciones de oleadas y sistema de vidas).

## Guía de Usuario

### Comandos Principales
- `/defense join <misión>`: Ingresar a una instancia de defensa o forzar el ingreso de un jugador objetivo.
- `/defense class`: Abrir la interfaz de selección y especialización de clases.
- `/defense loadout`: Gestionar el inventario de expedición (Armas, Armaduras y Consumibles).
- `/defense exotics`: Consultar la vitrina de equipamiento mítico y sus requisitos de desbloqueo.

### Sistema de Escuadrones (Party)
- `/defense party invite <jugador>`: Emitir una invitación de escuadrón.
- `/defense party accept`: Aceptar una invitación pendiente.
- `/defense party leave`: Abandonar el escuadrón actual.
- `/defense party kick <jugador>`: Expulsar a un miembro del escuadrón (requiere privilegios de líder).

### Mecánicas Principales
- **Instancias Aisladas:** Cada partida genera un mundo temporal independiente mediante clonación asíncrona, el cual se destruye y purga de la memoria al finalizar.
- **Escalado Polinómico:** La dificultad geométrica de las entidades enemigas aumenta progresivamente, aplicando multiplicadores de salud y daño en función de la oleada actual y el nivel de la misión.
- **Extracción:** Al completar un ciclo predefinido de oleadas, se habilita un periodo de votación. Los jugadores pueden extraer con sus recompensas actuales o continuar defendiendo frente a un escalado de dificultad mayor.
- **Suministros Tácticos:** Los consumibles equipados en el Loadout no se pierden permanentemente; se recargan de forma automática al inicio de cada nueva instancia.
