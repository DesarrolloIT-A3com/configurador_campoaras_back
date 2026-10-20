# Preparación e instalación

En esta guía se orientará al desarrollador a instalar y actualizar el configurador, de manera que se pueda ejecutar correctamente en un entorno de desarrollo.

# Requisitos

El desarrollador debe de saber las siguientes tecnologías:
- Conocimientos básicos y funcionamiento del protocolo HTTP 
- Java 21 con Spring Boot
- Lombok para tratamiento de objetos y entidades
- JPA
- MySQL
- Git y manejo con GitHub

Se valora que tenga base en ciberseguridad y que sepa interpretar y explotar logs de aplicación.

# Preparación del proyecto

El proyecto se encuentra en dos repositorios, el primero es en GitHub, en la siguiente url [https://github.com/DesarrolloIT-A3com/configurador_campoaras_back](https://github.com/DesarrolloIT-A3com/configurador_campoaras_back).

El segundo es en el servidor de ficheros de A3 en la ruta `\\A3-di-0001\a3com\06_PROYECTOS\00_CLIENTES\1974_CAMPOARAS\04_WEB\configurador_campoaras_back.zip`

Ambas formas tienen enlace directo a git, por lo que es muy importante tener Git instalado con la cuenta de `DesarrolloIT-A3com` configurada, una vez esté el proyecto en el entorno, se ejecuta git pull sobre la raíz del proyecto para traer los últimos cambios.

Hay dos ramas de trabajo:

- main: Rama principal, se usa exclusivamente para cuando el proyecto esté en producción, todos los errores que haya de producción se corrigen en esa rama.

- dev: Rama de desarrollo, exclusiva para cambios, funcionalidades nuevas, mantenimiento, todo debe de estar validado aquí, una vez que todo esté listo se realiza un `git merge dev` en la rama main.

Los ficheros que cambian de dev a main son `CPConstants.java` y `AuthRestController.java`, ya que contienen rutas y configuración que se alterna en desarrollo y en producción, antes de subir el proyecto al servidor se deben de validar estos dos archivos en main.

Dentro del proyecto en el servidor de A3, existen tres archivos que no se encuentran en el repositorio de git, siendo:

- dev.env
- prod.env
- .env

Estos archivos son ficheros de entornos, necesarios para el funcionamiento de la app, ya que contienen claves, credenciales y configuración.

Dependiendo del perfil a utilizar se debe de copiar dev.env o prod.env a .env que es el fichero principal

# Instalación

El proyecto se ha realizado con Java Maven 21, mediante el IDE Eclipse 2025/09, se recomienda tener el mismo IDE o uno superior funcional.

El proyecto compila con JDK 21, si el desarrollador no posee esta librería se puede descargar a través de [Oracle](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html), se debe de colocar en eclipse como alternate JRE o installed JRE para que recoja las librerías de Java 21.

Posteriormente hay que instalar lombok, este se encuentra en la carpeta .m2 en la carpeta de usuario, la ruta es `%USERPROFILE%\.m2\repository\org\projectlombok\lombok` se debe de escoger la versión más reciente y ejecutar el .jar de `lombok-<version-escogida>.jar` con el IDE **cerrado**, una vez se instale, se cierra el instalador, hay que abrir el IDE y en la raíz del proyecto `click derecho --> Run As --> Maven Build`, se abre un Pop-Up, en el campo `Goals` se introduce `clean install`, se activa el skip de test, y se pulsa sobre run.

Con esto Eclipse descarga todas las dependencias y actualiza el proyecto junto con lombok instalado, en caso de que haya errores porque Eclipse no encuentre la dependencia de lombok, sobre la raíz del proyecto se hace `click derecho --> Maven --> Update Project`, con esto maven actualiza el proyecto y refresca el IDE con las dependencias instaladas.

# Configuración de la base de datos

La base de datos se configura mediante 3 frentes:

- Motor de MySQL: Hay que tener la base de datos `configurador_campoaras` creada antes de arrancar el proyecto.

- application.yaml: En este fichero se configura el conector, url de la base de datos, usuario y contraseña, dentro de la variable datasource, por ello hay que asegurar que esté dentro de spring, y que la variable url apunte a `jdbc:mysql://localhost:3306/configurador_campoaras`, la configuración final debe de quedar de la siguiente manera:

    ```yaml
    jpa:
    hibernate:
      ddl-auto: validate
    datasource:
    driverClassName: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/configurador_campoaras
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      connection-timeout: 60000
      maximum-pool-size: 5
    ```
    
    Es importante que si es la primera vez que se arranca el proyecto, `ddl-auto` debe de estar en `create`, una vez arrancado el proyecto **si no hay errores**, se cambia a `validate`.

- Archivo de entorno .env: Debe de tener las propiedades DB_USER y DB_PASSWORD, correspondientes a las credenciales de acceso a la base de datos, estos valores se cargan mediante el fichero `DotEnvInitializer.java`.

De esta forma, en el primer arranque Spring cargará toda la estructura de la base de datos definida por las entidades en la carpeta **entities**, en el segundo arranque con validate, se comprobará que la estructura es correcta, ya que create borra todo y vuelve a crear la estructura de la base de datos sin contenido.

# Configuración CORS

CORS es un mecanismo para que webs ajenas al configurador, no puedan acceder a recursos del servidor, para ello se designa dentro del archivo `application.yaml` una propiedad llamada url-cors, dentro de un archivo de entorno .env se usa la propiedad URL_CORS, hay que colocar la url que sirve la web ya sea localhost o una privada con el puerto `5173` de manera que quede de la siguiente manera - `http://localhost:5173`.

# Configuracion de red

El proyecto puede usarse en localhost, o exponerlo en una red privada, si se usa la segunda opción, primero se debe conocer la dirección IP privada que sirve el dispositivo.

Se abre una terminal o cmd, y se ejecuta `ipconfig`, hay que buscar el adaptador de Ethernet o inalámbrico, y fijarse en la Dirección Ipv4 (ejemplo con este dispisitivo)

```txt
Adaptador de Ethernet Ethernet 2:

   Sufijo DNS específico para la conexión. . :
   Vínculo: dirección IPv6 local. . . : fe80::c466:31b6:3561:c0e2%3
   Dirección IPv4. . . . . . . . . . . . . . : 192.168.1.133
   Máscara de subred . . . . . . . . . . . . : 255.255.255.0
   Puerta de enlace predeterminada . . . . . : 192.168.1.254
```

Posteriormente en `application.yaml` se debe de destinar una propiedad `server` que esté dentro de `spring`, que contenga una variable `address` que tenga la dirección IP privada, ejemplo con la dirección anterior:

```yaml
server:
    address: 192.168.1.169
```

El puerto por defecto es 8080, si se desea cambiar se debe de añadir debajo de address, la variable `port` junto con el puerto a cubrir.

# Configuración de OpenAPI (Opcional)

OpenAPI es una paltaforma que permite tener tus endpoints dentro del servidor y probarlos sin necesidad de una web, esto se usa en fases de desarrollo temprano para comprobar que los endpoints no presentan errores antes de conectarlos a una web, para ello se deben de definir ficheros .yaml con configuración especifica de OpenAPI.

Esos ficheros ya están definidos, se encuenrtran en `src/main/resources/static/openapi/`, pero hay que cambiarles la url de servicio para que estos endpoints funcionen, para ello al abriruno de los archivos en las primeras líneas se observará lo siguiente:

```yaml
servers:
  - url: http://192.168.1.133:8080/
    description: Documentación de endpoints de desarrollo
```

Hay que cambiar la propiedad url por la que se ejecuta el proyecto, recompilarlo mediante maven build y volver a ejecutar el proyecto.

# Primer arranque y pruebas de funcionamiento

Una vez esté lo básico configurado, se arranca el proyecto en la rama dev, para probar que el flujo http funciona, se puede hacer de dos maneras:

1. Probarlo desde la web: Ejecutar la web en desarrollo, asegurando que la url de la web coincide con URL_CORS visto anteriormente, dirigirse a la pestaña de login y probar las credenciales de un usuario por defecto, si funciona, probar algunos endpoints para comprobar que la autenticación, autorización y control de sesiones funcionan.

2. Probarlo desde OpenAPI: Con el proyecto arrancado hay que entrar en la url del servidor y colocar la ruta `/swagger-ui/index.html`, dirigirse en las definiciones (arriba a la derecha) hacia `Auth`, y buscar el endpoint de login, se abre el desplegable, se pulsa sobre **Try it out** y en el json que ofrece, colocar las credenciales de un usuario por defecto, si la respuesta es un 200, el servidor ha mandado las cookies de acceso al almacén del navegador, para ello se abre Dev Tools con F12 y sobre la opción `Application` hay una pestaña que es `Cookies` se seleccionan las cookies del servidor y se debe de copiar el valor de la cookie `access_token`, una vez esté en el portapapeles, volviendo a la pantalla principal, hay un botón llamado **Authorize**, se pulsa sobre el y se pega el valor de la cookie, se vuelve a pulsar sobre Authorize y se prueba el endpoint /me para comprobar que la autenticación funciona.

Para extraer las credenciales del usuario por defecto se hace desde el fichero **.env** en las propiedades ADMIN_NAME y ADMIN_PASS.

Se recomiendan observar los logs para comprobar que no hay errores y que el funcionamiento es correcto.

# Puesta en producción

Una vez que los cambios en dev estén listos, trasladamos el proyecto a la rama `main`, mediante `git checkout main`, posteriormente se realiza un `git merge dev`, antes de realizar el push, hay que comprobar los archivos `CPConstants.java` y `AuthRestController.java`:

- CPConstans: En este archivo hay que comprobar las propiedades IMG_PATH y la configuración de .env, en producción se usa un método llamado getEnv, que obtiene las propiedades de entorno del sistema, en desarrollo se utiliza un ternario para buscar el archivo .env de la raíz y de resources.

- AuthRestController: En este archivo hay que situarse en los endpoints /login y /logout, ya que en la configuración de cookies se usa `secure(true)` y `samesite("None")` para que las cookies funcionen por https y no den error entre dominios.

Lo normal es que se cambien solos, pero es recomendable comprobarlo ya que se pueden cambiar los valores de un merge a otro, si se alteran los ficheros o se realizan cambios en dev.

Una vez esté correcto el merge, y los archivos CPConstants y AuthRestController estén comprobados, se debe de compilar el proyecto, para ello sobre la raíz del proyecto `Click derecho --> Run As --> Maven build` en Goals se coloca `clean install` con la opción skip test marcada, se pulsa sobre run para comenzar la compilación.

Una vez termine y si no hay errores, el .jar se encontrará en la carpeta target, teniendo como nombre `configurador.campoaras-<version>.jar`.

En la rama main habrán cuatro archivos, `start.sh`, `campoaras.service`, `campoaras-cloudflare.service` y `application-prod.yaml`.

Esos archivos deben de estar incluidos dentro del servidor de producción ya que son los encargados de arrancar el .jar y establecer una VPN hacia cloudflare para establecer una capa de seguridad.

En start.sh se declara una variable que hace mención al .jar, hay que asegurar que coincida con el nombre del nuevo .jar actualizado.

Lo mismo ocurre con campoaras.service, se hace mención al .jar, por lo que hay que asegurar que tengan el mismo nombre para que el sistema encuentre el .jar y ejecute así el servidor.

`application-prod.yaml`, es una versión de producción del fichero `application.yaml` en los que cambian propiedades para que el archivo .jar recoja un perfil de producción más seguro que el perfil de desarrollo, este fichero deshabilita OpenAPI para no exponer endpoints de swagger y especifica la ruta del fichero de logs del sistema, por lo que debe de estar junto al .jar de producción.

El servidor se encuentra en un VPS de Raiola situado en la dirección IP 185.237.234.123, tiene un usuario dedicado llamado campoaras, la conexión al servidor debe de ser por ssh por lo que para conectarse se usa:

```bash
ssh campoaras@185.237.234.123
```

La contraseña se encuentra en NOLIMITSCONSULTING en la línea 102.

Una vez dentro del servidor lo primero que se debe de hacer es detener campoaras.service mediante `systemctl`:

```bash
sudo systemctl stop campoaras.service
```

Una vez detenido, hay que preparar un .zip en el dispositivo dónde está el proyecto con lo siguiente:

- .jar del proyecto
- application-prod.yaml
- start.sh
- campoaras.service
- campoaras-cloudflare.service
- .env de producción

Se transfiere mediante scp:

```bash
# en la terminal hay que estar situado en el nivel del .zip
scp .\archivo.zip campoaras@185.237.234.123:/home/campoaras
```

Una vez transferido, se descomprime el .zip mediante `unzip`, es recomendable descomprimirlo en la carpeta home del usuario y ya mediante permisos moverlo a la ruta del proyecto para que cuando se ejecute el proyecto tenga permisos de lectura, escritura y ejecución:

```bash
cd ~
unzip arhivo.zip
```

Antes de traspasar un archivo se recomienda ejecutar `sed` sobre start.sh y campoaras.service, si se han abierto en un **windows**, esto elimina caracteres especiales que coloca Windows que entran en conflicto con Linux:

```bash
sed -i 's/\r$//' ~/start.sh
sed -i 's/\r$//' ~/campoaras.service
sed -i 's/\r$//' ~/campoaras-cloudflare.service
sed -i 's/\r$//' ~/application-prod.yaml
```

Una vez con todo preparado y limpio, se deben de mover los archivos hacia la ruta `/opt/campoaras`, excepto campoaras.service y campoaras-cloudflare.service, esos archivos van a `/etc/systemd/system/`, hay que moverlos con `sudo` ya que son directorios protegidos, no se recomienda hacerlo como root.

Antes de mover el .jar, se recomienda renombrar el .jar antiguo, añadiendole la extensión .bak, por si el nuevo .jar presenta errores, volver al antiguo para no dejar la aplicación colgada.

Los ficheros que deben de estar junto al .jar son `start.sh`, `application-prod.yaml` y `.env`, por lo que se deben de mover a `/opt/campoaras`, una vez preparados se recomienda abrir otra terminal en la que se monitoreen los logs del sistema para comprobar que el arranque y funcionamiento de la app van bien, para ello se debe de ejecutar:

```bash
tail -f /opt/campoaras/s/campoaras.log
```

En la terminal original se arranca primero el .jar de campoaras con:

```bash
sudo systemctl start campoaras.service
```

Se observan los logs, si ha arrancado correctamente, se debe de reiniciar el servicio de cloudflare con:

```bash
sudo systemctl restart campoaras-cloudflare.service
```

Una vez con todo listo, accediendo al [configurador](https://configurador.creacionescampoaras.com) e iniciando sesión se comprueba si el servidor responde correctamente.

En caso contrario hay que revisar los logs y los errores que ofrece la web en DevTools para poder arreglar el fallo repitiendo todo el proceso.