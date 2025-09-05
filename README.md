
## Escuela Colombiana de Ingeniería
### Arquitecturas de Software – ARSW


#### Ejercicio – programación concurrente, condiciones de carrera y sincronización de hilos. EJERCICIO INDIVIDUAL O EN PAREJAS.

##### Parte I – Antes de terminar la clase.

Control de hilos con wait/notify. Productor/consumidor.

1. Revise el funcionamiento del programa y ejecútelo. Mientras esto ocurren, ejecute jVisualVM y revise el consumo de CPU del proceso correspondiente. A qué se debe este consumo?, cual es la clase responsable?
![img.png](img.png)
2. Haga los ajustes necesarios para que la solución use más eficientemente la CPU, teniendo en cuenta que -por ahora- la producción es lenta y el consumo es rápido. Verifique con JVisualVM que el consumo de CPU se reduzca.
![image.jpg](img%2Fimage.jpg)
3. Haga que ahora el productor produzca muy rápido, y el consumidor consuma lento. Teniendo en cuenta que el productor conoce un límite de Stock (cuantos elementos debería tener, a lo sumo en la cola), haga que dicho límite se respete. Revise el API de la colección usada como cola para ver cómo garantizar que dicho límite no se supere. Verifique que, al poner un límite pequeño para el 'stock', no haya consumo alto de CPU ni errores.


##### Parte II. – Antes de terminar la clase.

Teniendo en cuenta los conceptos vistos de condición de carrera y sincronización, haga una nueva versión -más eficiente- del ejercicio anterior (el buscador de listas negras). En la versión actual, cada hilo se encarga de revisar el host en la totalidad del subconjunto de servidores que le corresponde, de manera que en conjunto se están explorando la totalidad de servidores. Teniendo esto en cuenta, haga que:

- La búsqueda distribuida se detenga (deje de buscar en las listas negras restantes) y retorne la respuesta apenas, en su conjunto, los hilos hayan detectado el número de ocurrencias requerido que determina si un host es confiable o no (_BLACK_LIST_ALARM_COUNT_).
- Lo anterior, garantizando que no se den condiciones de carrera.

Solución:
Podemos solucionar esto agregando un contador global compartido en HostBlackListValidator para que todos los hilos sepan cuántas apariciones ha sido encontradas hasta el momento. Para manejo de concurrencia, esta variable o contador en este caso debe ser AtomicInteger.

```java
public static AtomicInteger globalOccurrences = new AtomicInteger(0);
```

Luego, cambiamos la condición de parada del bucle for en la clase hilo, más exactamente en el metodo run() para que también depende de ese contador global, y no solo del propio del hilo

```java
@Override
    public void run() {
        for (int i = start; i < end && HostBlackListsValidator.globalOccurrences.get() < HostBlackListsValidator.BLACK_LIST_ALARM_COUNT; i++) {
            reviewedListCounter++;
            if (skds.isInBlackListServer(i, ipHostAddress)) {
                blacklistAppearances.add(i);
                appearanceCounter++;
                HostBlackListsValidator.globalOccurrences.incrementAndGet();
            }
        }
    }
```

Verificación:
Como podemos observar, ya no se checkean todas las listas, solo hasta el momento en que se encuentra la 5ta aparición de la IP en las listas.
![Captura de pantalla 2025-09-04 231711.png](img/Captura%20de%20pantalla%202025-09-04%20231711.png)

##### Parte III. – Avance para el martes, antes de clase.

Sincronización y Dead-Locks.

![](http://files.explosm.net/comics/Matt/Bummed-forever.png)

1. Revise el programa “highlander-simulator”, dispuesto en el paquete edu.eci.arsw.highlandersim. Este es un juego en el que:

	* Se tienen N jugadores inmortales.
	* Cada jugador conoce a los N-1 jugador restantes.
	* Cada jugador, permanentemente, ataca a algún otro inmortal. El que primero ataca le resta M puntos de vida a su contrincante, y aumenta en esta misma cantidad sus propios puntos de vida.
	* El juego podría nunca tener un único ganador. Lo más probable es que al final sólo queden dos, peleando indefinidamente quitando y sumando puntos de vida.

2. Revise el código e identifique cómo se implemento la funcionalidad antes indicada. Dada la intención del juego, un invariante debería ser que la sumatoria de los puntos de vida de todos los jugadores siempre sea el mismo(claro está, en un instante de tiempo en el que no esté en proceso una operación de incremento/reducción de tiempo). Para este caso, para N jugadores, cual debería ser este valor?.


	- Debería ser `N * DEFAULT_IMMORTAL_HEALTH`.


3. Ejecute la aplicación y verifique cómo funcionan las opción ‘pause and check’. Se cumple el invariante?.

![Captura de pantalla 2025-09-04 184942.png](img/Captura%20de%20pantalla%202025-09-04%20184942.png)


No se cumple el invariante del sistema: la suma de los puntos de vida (HP) de todos los jugadores no coincide con el valor esperado. Con N = 3 jugadores y DEFAULT_IMMORTAL_HEALTH = 100, el total debería ser 300; sin embargo, el valor observado es 540 (según la evidencia mostrada en la imagen).

    Detalle:

	Esperado:
	3×100=300
	
	Observado: 540


4. Una primera hipótesis para que se presente la condición de carrera para dicha función (pause and check), es que el programa consulta la lista cuyos valores va a imprimir, a la vez que otros hilos modifican sus valores. Para corregir esto, haga lo que sea necesario para que efectivamente, antes de imprimir los resultados actuales, se pausen todos los demás hilos. Adicionalmente, implemente la opción ‘resume’.

![Captura de pantalla 2025-09-01 161926.png](img/Captura%20de%20pantalla%202025-09-01%20161926.png)
![Captura de pantalla 2025-09-04 185220.png](img/Captura%20de%20pantalla%202025-09-04%20185220.png)
	
	ya se cumplen las funciones de los botones resume y pause and check

5. Verifique nuevamente el funcionamiento (haga clic muchas veces en el botón). Se cumple o no el invariante?.

Ahora el invariante se cumple. Antes, cuando la pausa no existía, la suma podía salir diferente porque algunos hilos seguían peleando mientras tú calculabas la suma → condición de carrera. Con la corrección (pausar y luego sumar), la suma siempre es N * DEFAULT_IMMORTAL_HEALTH

6. Identifique posibles regiones críticas en lo que respecta a la pelea de los inmortales. Implemente una estrategia de bloqueo que evite las condiciones de carrera. Recuerde que si usted requiere usar dos o más ‘locks’ simultáneamente, puede usar bloques sincronizados anidados:

Utilizamos una estrategia de bloqueo implementando sincronización anidada:
```java
synchronized(locka){
	synchronized(lockb){
		//code
	}
}
```
De esta manera garantizamos exclusión mutua, es decir que cuando un objeto Immortal pelea con otro, nadie más puede modificar al tiempo los estados de this o i2, además ayuda a evitar condiciones de carrera sobre la variable health. Esta sincronización se hace en el método fight de Immortal.

```java
public void fight(Immortal i2) {
	synchronized (this) {
		synchronized (i2) {
			if (i2.getHealth() > 0) {
				i2.changeHealth(i2.getHealth() - defaultDamageValue);
				this.health += defaultDamageValue;
				updateCallback.processReport("Fight: " + this + " vs " + i2 + "\n");
			} else {
				updateCallback.processReport(this + " says:" + i2 + " is already dead!\n");
			}
		}
	}
}
```

7. Tras implementar su estrategia, ponga a correr su programa, y ponga atención a si éste se llega a detener. Si es así, use los programas jps y jstack para identificar por qué el programa se detuvo.

Primero, ejecutamos el comando .\jps para ver los procesos que se estaban ejecutando
![Captura de pantalla 2025-09-04 194526.png](img/Captura%20de%20pantalla%202025-09-04%20194526.png)

Luego ejecutamos .\jstack <ID del proceso>, en esta caso: 19776
![Captura de pantalla 2025-09-04 195051.png](img/Captura%20de%20pantalla%202025-09-04%20195051.png)

Revisando el proceso, podemos observar que ocurrió un deadlock:
![Captura de pantalla 2025-09-04 195818.png](img/Captura%20de%20pantalla%202025-09-04%20195818.png)

8. Plantee una estrategia para corregir el problema antes identificado (puede revisar de nuevo las páginas 206 y 207 de _Java Concurrency in Practice_).

Estrategia:
El problema del deadlock anterior era que cada hilo intentaba bloquear en distinto orden, lo cual generaba un ciclo osea cada uno tomaba un lock y esperaba por el otro.
Por tanto, implementamos una estrategia basada en lock ordering, la cual consiste en establecer un orden global y consistente para la adquisición de locks. En este caso, antes de sincronizar dos Immortal, con la implementación del método getLockOrderImmortal, se determina cuál de los dos tiene menor System.identityHashCode y se bloquea primero ese objeto, luego el otro. De esta forma se elimina el deadlock, ya que los hilos ahora respetan la misma regla al adquirir lock.  

Implementación:
Método getLockOrderImmortal:
```java
public static Immortal getLockOrderImmortal(Immortal a, Immortal b) {
	return System.identityHashCode(a) < System.identityHashCode(b) ? a : b;
}
```
Uso en el método fight():

```java
public void fight(Immortal i2) {
	Immortal firstLock = getLockOrderImmortal(this,i2);
	Immortal secondLock = firstLock == this ? i2 : this;
	synchronized (firstLock) {
		synchronized (secondLock) {
			if (i2.getHealth() > 0) {
				i2.changeHealth(i2.getHealth() - defaultDamageValue);
				this.health += defaultDamageValue;
				updateCallback.processReport("Fight: " + this + " vs " + i2 + "\n");
			} else {
				updateCallback.processReport(this + " says:" + i2 + " is already dead!\n");
			}
		}
	}
}
```
9. Una vez corregido el problema, rectifique que el programa siga funcionando de manera consistente cuando se ejecutan 100, 1000 o 10000 inmortales. Si en estos casos grandes se empieza a incumplir de nuevo el invariante, debe analizar lo realizado en el paso 4.

Ejecución con 100 hilos:

![Captura de pantalla 2025-09-04 211914.png](img/Captura%20de%20pantalla%202025-09-04%20211914.png)

Ejecución con 1000 hilos:

![Captura de pantalla 2025-09-04 212119.png](img/Captura%20de%20pantalla%202025-09-04%20212119.png)

Ejecución con 10000 hilos:

![Captura de pantalla 2025-09-04 212313.png](img/Captura%20de%20pantalla%202025-09-04%20212313.png)

Como podemos ver en las fotos, el invariante se cumple incluso para números grandes como 10000

10. Un elemento molesto para la simulación es que en cierto punto de la misma hay pocos 'inmortales' vivos realizando peleas fallidas con 'inmortales' ya muertos. Es necesario ir suprimiendo los inmortales muertos de la simulación a medida que van muriendo. Para esto:
	* Analizando el esquema de funcionamiento de la simulación, esto podría crear una condición de carrera? Implemente la funcionalidad, ejecute la simulación y observe qué problema se presenta cuando hay muchos 'inmortales' en la misma. Escriba sus conclusiones al respecto en el archivo RESPUESTAS.txt.

Podemos hacer que los 'inmortales' muertos sean removidos de la lista de población de inmortales, pero, esto genera un problema (ver RESPUESTAS.txt)

```java
if (im.getHealth() <= 0) {
	immortalsPopulation.remove(im);
}
```

	* Corrija el problema anterior __SIN hacer uso de sincronización__, pues volver secuencial el acceso a la lista compartida de inmortales haría extremadamente lenta la simulación.

Lo que podemos hacer ahora es, en la clase ControlFrame, en la creación de la lista, cambiar el tipo de lista de immortalPopulation que es LinkedList a CopyOnWriteArrayList, porque permite iterar y eliminar elementos sin generar la excepción ConcurrentModificationException, y también sin que todos los hilos tengan que esperar un bloqueo


11. Para finalizar, implemente la opción STOP.

```java
btnStop.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
		immortals.clear();
		btnStart.setEnabled(true);
	}
});
```
<!--
### Criterios de evaluación

1. Parte I.
	* Funcional: La simulación de producción/consumidor se ejecuta eficientemente (sin esperas activas).

2. Parte II. (Retomando el laboratorio 1)
	* Se modificó el ejercicio anterior para que los hilos llevaran conjuntamente (compartido) el número de ocurrencias encontradas, y se finalizaran y retornaran el valor en cuanto dicho número de ocurrencias fuera el esperado.
	* Se garantiza que no se den condiciones de carrera modificando el acceso concurrente al valor compartido (número de ocurrencias).


2. Parte III.
	* Diseño:
		- Coordinación de hilos:
			* Para pausar la pelea, se debe lograr que el hilo principal induzca a los otros a que se suspendan a sí mismos. Se debe también tener en cuenta que sólo se debe mostrar la sumatoria de los puntos de vida cuando se asegure que todos los hilos han sido suspendidos.
			* Si para lo anterior se recorre a todo el conjunto de hilos para ver su estado, se evalúa como R, por ser muy ineficiente.
			* Si para lo anterior los hilos manipulan un contador concurrentemente, pero lo hacen sin tener en cuenta que el incremento de un contador no es una operación atómica -es decir, que puede causar una condición de carrera- , se evalúa como R. En este caso se debería sincronizar el acceso, o usar tipos atómicos como AtomicInteger).

		- Consistencia ante la concurrencia
			* Para garantizar la consistencia en la pelea entre dos inmortales, se debe sincronizar el acceso a cualquier otra pelea que involucre a uno, al otro, o a los dos simultáneamente:
			* En los bloques anidados de sincronización requeridos para lo anterior, se debe garantizar que si los mismos locks son usados en dos peleas simultánemante, éstos será usados en el mismo orden para evitar deadlocks.
			* En caso de sincronizar el acceso a la pelea con un LOCK común, se evaluará como M, pues esto hace secuencial todas las peleas.
			* La lista de inmortales debe reducirse en la medida que éstos mueran, pero esta operación debe realizarse SIN sincronización, sino haciendo uso de una colección concurrente (no bloqueante).

	

	* Funcionalidad:
		* Se cumple con el invariante al usar la aplicación con 10, 100 o 1000 hilos.
		* La aplicación puede reanudar y finalizar(stop) su ejecución.
		
		-->

<a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by-nc/4.0/88x31.png" /></a><br />Este contenido hace parte del curso Arquitecturas de Software del programa de Ingeniería de Sistemas de la Escuela Colombiana de Ingeniería, y está licenciado como <a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/">Creative Commons Attribution-NonCommercial 4.0 International License</a>.
