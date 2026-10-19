# Configurador Back Campoaras

Servidor del configurador de productos de baños, desarrollado por AAG Insight Strategy Concept SL, usando la tecnología Spring Boot con Java 21, dentro del servidor se reflejan cinco componentes principales, siendo:

- Seguridad
- Autenticacion
- Configuracion
- Administracion
- Base de datos

Todo el configurador gira alrededor de la base de datos, siendo un componente esencial debido al manejo de usuarios y  productos para que el configurador funcione, debido a esa criticidad, la seguridad del mismo debe de ser fuerte.

# Secciones del servidor

El servidor se divide en varias secciones, todas se conectan entre sí, es muy importante entenderlas ya que cualquier error en los datos, o lógica de servicios, puede afectar al funcionamiento general del configurador.

## Configuración del servidor

El servidor utiliza distintos mecanismos de seguridad y funcionamiento, que se reflejan en esta sección, además, se hablará del fichero `application.yaml` el cual posee configuración adicional que el servidor recoge antes de arrancar, primero se hablará de la configuración codificada y posteriormente del fichero.

- Mecanismo CORS: Este mecanismo permite definir que URL pueden consultar los recursos del servidor, esto se hace para que otras webs no pueda extraer información de la base de datos sin el permiso de acceso que otorga este mecanismo, se define en el fichero `CORSConfig.java`, en el da permiso a los métodos GET,POST,PUT,PATCH, DELETE y OPTIONS, de las URL permitidas, además de permitir que se pasen cookies para autenticación.

- Documentación Open API: Para el desarrollo y pruebas de endpoint se ha usado swagger de Open APi, para ello se usa el archivo OpenApiConfig, que activa la ruta /swagger-ui/index.html, dónde están todos los endpoints con sus configuraciones para probarlos en desarrollo por petición en vez de por web, en producción esto se deshabilita.

- Configuración JWT: LA autenticación y gestión de sesiones funciona por tokens JWT los cuales codifican el id del usuario y les pone un tiempo de sesión, acabados ese tiempo, se debe de refrescar para extender la sesión, en caso contrario se fuerza al usuario a iniciar sesión de nuevo, esta configuración se encuentra en los ficheros `JwtUtil.java` y `JwtAuthenticationFilter.java`.

- Filtros y configuraciones de seguridad: Aquí se definen todos los filtros y configuraciones que autorizan y protegen a un usuario, además de proteger sus datos, esta configuración se encuentra en el fichero `SecurityConfig.java`

- Configuración de ficheros de entorno: Esta configuración carga los valores sensibles de un fichero de entorno antes de arrancar la app, para ello se utilizan dos archivos, el primero es `DotEnvInitializer.java` el cual es el encargado de cargar las variables, el segundo es `spring.factories` ubicado en `resources/META-INF` el cual se encarga de autorizar al fichero anterior a ejecutarse antes de Spring Boot.

- Fichero `application.yaml`: Este fichero es el fichero de configuración general, cuando el servidor arranca usa este archivo para que de manera interna pueda realizar varias acciones que permiten el funcionamiento del configurador, estas acciones son:
    - Configuración y conexión a la base de datos, url de acceso, credenciales y carga de datos.
    - Configuración de cuenta y servidor SMTP para el envío automático de correos electrónicos,
    - Configuración http del servidor, permite establecer una dirección privada y puerto específico, útil si hay más de un ordenador participando en el desarrollo
    - Configuración para subida de archivos y objetos complejos.
    -  Configuración de OpenAPI para documentación de endpoints.
    - Configuración de logging para el registro de eventos en fichero.
    - Configuración JWT para establecer una contraseña de encriptación y tiempos de acceso y refresco de sesiones.
    - URL/s permitidas para el mecanismo CORS
    - Contraseñas para encriptar los datos sensibles

## Base de datos

La base de datos del servidor es un MySQL, mediante un conector se usa JPA (Java Persistence API),ofrece muchos beneficios, entre ellos acceso a datos mediante métodos y protección frente a inyecciones, para ello hay que seguir una configuración específica:

- Manejo de entidades: Las tablas que corresponden con MySQL en JPA son entidades, son objetos convencionales que utilizan anotaciones para crear las columnas de una tabla de base de datos, ejemplo con la entidad Usuario.java:

    ```java
    @Entity
    @Table(name="usuario")
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public class Usuario 
    {
        @Id
        @Column(nullable = false,unique = true)
        private String uuid;
        
        @Column(nullable = false,unique = true)
        private String email;
        
        @Column(nullable = false)
        private String username;
        
        @Column(nullable = false)
        private String password;
        
        @Column(nullable = false)
        private float descuento;
        
        @Column(nullable = false)
        private float segundoDescuento = 0;
        
        // Este campo hace referencia al email del comercial
        @Column
        private String comercial;
        
        @Column
        private LocalDateTime acceso;
        
        @Column(nullable = false)
        private boolean verificado;
        
        @Column(nullable = false)
        private String USRToken;
        
        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn(name = "role_id")
        private Rol rol;
    }
    ```

    Las entidades más importantes son Usuario, Producto, Serie, Acabado, Color, Frente, Configuracion, ProductoConfigurado, BulkProductosUsuario y Pedido.

    Estas entidades contienen datos de usuarios, productos y pedidos que hacen que el configurador funcione, el resto de entidades son invisibles ante la web pero funcionales en el servidor para manejar autorizaciones, copias de seguridad e información adicional.

- Repositorios: Los repositorios en JPA son interfaces que contienen operaciones CRUD de una entidad, permitiendo así añadir, consultar, actualizar y borrar una entidad o tabla de la base de datos, cada entidad debe de tener su repositorio, su sintaxis es la siguiente:

    ```java
    public interface IUsuarioRepository extends JpaRepository<Usuario, String>
    {
        Usuario findByUsername(String username);
        
        Usuario findByEmail(String email);
        
        List<Usuario> findByComercial(String comercial);
    }
    ```

- DTOS: Un dto es un modelo de datos que sirve para obtener o enviar una entidad de la base de datos por http, se usa para evitar tene que usar la propia entidad que posee configuración sensible como objeto intercambiable, la sintaxis de un DTO es muy básica limitandose a ser un JSON pero en Java:

    ```java
    @Getter
    @AllArgsConstructor
    public class UserGetDTO 
    {
        private String uuid;
        
        private String email;
        
        private String username;
        
        private String password;
        
        private float descuento;
        
        private float segundoDescuento;
        
        private String comercial;
        
        private LocalDateTime acceso;
        
        private String rol;
        
        private boolean verificado;
    }
    ```

## Endpoints

Los endpoints del servidor permiten que la web se comunique pasando o solicitando datos de determinadas rutas que el servidor habilita, los endpoints se situan en la carpeta rest, la sintaxis de un endpoint es la siguiente (ejemplo con login de usuario):

```java
@RequestMapping(method = RequestMethod.POST, value = "/login")
public ResponseEntity<?> login(@RequestBody(required = true) final LoginDTO body,
        HttpServletRequest request,
        HttpServletResponse response)
```

Estos endpoints pueden estar protegidos, siendo que si lo están, somenten al cliente o a la web a mandar una cookie con el token JWT que se le concede, si el token es validado y pasa el filtro, el endpoint ejecuta un filtro para saber si el usuario autenticado existe y si posee los permisos necesarios para entrar dentro del endpoint:

```java
String ip = this.security.getClientIPAddress(request);
String seguridad = this.security.getIpInfo(ip, request);

Usuario usuario = this.security.isAuth(userRepo, "/user", seguridad);

this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.CLIENTE_ROLE, seguridad, "/user", usuario.getUSRToken());
```

Dependiendo de la finalidad del endpoint, se debe de ajustar el rol del método hierarchy para hacer que el control sea más o menos estricto.

Los endpoints además funcionan por **servicios**, estos servicios contienen la lógica que debe de realizar el endpoint, por ejemplo, han solicitado productos, el endpoint tiene un servicio que consulta la base de datos, recopila los productos y los pone en un DTO para devolver una salida en el lenguaje que entienda la web.

## Seguridad

La seguridad del servidor se divide en varias capas para proteger al servidor de ataques no deseados, estas capas se pondrán por orden de exigencia empezando por las más superficiales (menos estrictas) hasta las internas (más estrictas)

- Seguridad en cabeceras HTTP: Spring Boot por defecto, contiene cabeceras que bloquean su manipulación, además de que no muestran información relevante que un usuario o atacante pueda comprometer.

- Mecanismo CORS: Se ha mencionado anteriormente, este mecanismo bloquea que webs no permitidas consulten información del servidor, para ello se definen las url permitidas en el fichero `application.yaml`, el fichero `CORSConfig.java` contiene un filtro que habilita que las url permitidas puedan mandar peticiones GET,POST,PUT,PATCH y OPTIONS al servidor, mandar cookies como credenciales y utilizar solo la cabecera Set-Cookie.

- Autenticación JWT: La autenticación se realiza mediante tokens JWT, este token se forma mediante el id del usuario autenticado, el cual se encripta junto con la fecha de expiración mediante una clave llamada CPWT_KEY definida en un archivo .env, al iniciar sesión se crean dos tokens, access_token y refresh_token, teniendo una duración de 10 minutos y 3 horas, cuando el access_token caduca, se refresca mediante el refresh_token para extender la sesión, si el refresh_token caduca la sesión termina, la validación de este token hace que se active un objeto llamado UserDetailsService, el cual contiene información del usuario para manejar la autenticación.

- Autorización: La autorización de un usuario se lleva mediante tres componentes:
    - Gestión de roles: Existen 5 roles siendo VERIFICATION,CLIENTE,COMERCIAL,ADMIN,SUPERADMIN, los roles se asignan a los usuarios nada más registrarse siendo VERIFICATION el más bajo, el resto de roles los debe de asignar un administrador, el rol SUPERADMIN solo se obtiene al arrancar la app por usuarios ya definidos.

    - Capacidades: Cada rol posee unas capacidades que se obtienen cada vez que el usuario se autentica, esas capacidades le dicen a la web qué debe de mostrar y qué no, dentro del servidor se valida internamente si un usuario con x rol tiene permiso para realizar una determinada acción.

    - Permiso por endpoint: Cada endpoint que requiera autenticación pasa por un método llamado hierarchy, el cual se le define un rol, ese rol es el mínimo permitido para que un usuario pueda llamar al endpoint, por ejemplo, obtener productos posee un rol mínimo de CLIENTE, eso quiere decir que todos los roles excepto VERIFICATION tienen acceso, cuanto más alto sea el rol, menos usuarios pueden acceder.

- Logging y gestión de eventos: Existe un archivo llamado `GeneralSecurity.java` el cual extrae metadatos para poder llevar los eventos del servidor, estos eventos se gestionan mediante Log4j, usando un archivo de configuración, llamado `log4j2-spring.xml` los logs o eventos se guardan en un fichero llamado campoaras.log, cada log posee la siguiente estructura, se usará el siguiente ejemplo

    ```log
    [ADMIN] -- /get-users -- USR-926113d7 Ha solicitado obtener una lista de usuarios con el rol SUPERADMIN -- [SEGURIDAD] IP:46.25.218.223 | Accept-Language:en-US,en;q=0.9,es;q=0.8 | REMOTE_ADDR:127.0.0.1 | HOST:127.0.0.1 | PORT:54414 | Referer:https://configurador.creacionescampoaras.com/ | Origin:https://configurador.creacionescampoaras.com | X-Forwarded-For:46.25.218.223 | X-Real-IP:null | Content-Length:null | Accept-Encoding:gzip, br | User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36 | timestamp:2026-10-14T09:46:21.189470256 | 
    ```

    - Estado: Se define con [ ESTADO ]
    - Ruta: Ruta del endpoint afectado
    - Mensaje: Accion realizada o aviso dado
    - Seguridad: Metadatos extraidos que ayudan al análisis de logs

- Hash de contraseñas: Las contraseñas de los usuarios se hashean mediante BCrypt para garantizar que por fuerza bruta no rompan la contraseña de un usuario privilegiado.

- Encriptación de datos: Todos los datos de tipo String se encriptan mediante dos claves:
    - Contraseña de encriptación: Contraseña almacenada en PASSWORD, sirve para encriptar los datos por esa clave.
    - Salt: Cadena de datos aleatorios que se añade al dato a encriptar, haciendo así que si hay datos repetidos, generan un valor de encriptación diferente, haciendo así que los datos no se puedan romper en caso de fuerza bruta

    Esto permite que cada valor string se guarde como una clave aleatoria en vez de en texto plano, haciendo que no se puedan robar los datos, es muy importante que cuando se vaya a tratar con este tipo de datos se llame al servicio `EncryptorService` el cual con los métodos decrypt y encrypt se juegue con este paso para asegurar la entrada y salida de datos.

    **IMPORTANTE** La clave PASSWORD bajo ningún concepto **DEBE DE SER CAMBIADA** en producción, debido a que es necesaria para recuperar el valor original de los datos, al alerarla los datos no se pueden desencriptar, dando errores a la hora de obtener productos, datos de usuario, etc.

# Funciones del servidor

Las funciones del servidor se dividen en 5 ramas, esas ramas poseen todos los endpoints necesarios para funcionar y comunicarse con la web:

## Rama de autenticación

Esta rama se encarga de llevar el registro, autenticación y autorización del usuario, en total posee 9 endpoints, se detallará su función y el método http que sirven:

- /register - POST: Este endpoint registra a un usuario, se recoge su email, username y contraseña, el servicio de autenticación se encarga de validar que el usuario no exista y que el correo y contraseña sean válidos, una vez pasados los filtros los datos se registran en una entidad llamada `Verification.java`, en la cual se registra un código que se envía por correo electrónico al usuario para verificar que es un ser humano.

- /verify/{verificationCode} - POST: Este endpoint valida que el código de verificación sea correcto, en caso de que lo sea, el usuario que está en `Verification.java` pasa a registrarse como un usuario final, dándole el rol de VERIFICATION, este rol solo puede ser editado por un administrador, por lo que el usuario quedará a la espera de ser verificado.

- /login - POST: Este endpoint se encarga de autenticar a un usuario, el servicio recoge las credenciales, valida que los datos introducidos sean correctos, en caso de que lo sean, se genera un access_token y un refresh_token con el id del usuario y se genera una cookie que devuelve el servidor, esta cookie es httponly y secure (en producción), se manda a través de Set-Cookie para que la web no la pueda leer, solo la pueda mandar para que el servidor pueda realizar bien el proceso de autenticación.

- /refresh - POST: Este endpoint refresca los tokens access_token y refresh_token una vez haya expirado el access_token del usuario que trata de autenticarse, el servicio valida que ambas cookies existen y que poseen el token válido

- /logout - POST: Este endpoint recoge la cookie de autenticación y la modifica para que sea autoexpirable, se encarga de validar primero que la cookie recibida sea válida y corresponda con el usuario que la ha mandado.

- /me - GET: Este endpoint devuelve datos básicos de usuario para que la web sepa quién se está autenticando, se valida que la cookie sea correcta antes de saber que usuario está tratando de autenticarse.

- /capabilities - GET: Este endpoint devuelve las capacidades que posee el usuario que trata de autenticarse para que la web sepa que debe de mostrar y ocultar.

- /forget-password - POST: Este enpoint recupera la contraseña de un usuario, para ello el usuario debe de pasar su correo, el servicio valida que exista y crea un código que lo guarda en la entidad `VerificationPass.java` junto con el correo del usuario, ese código se envía por correo.

- /reset-password/{verificationCode} - POST: Este endpoint recibe un código de verificación de reseteo de contraseña y la contraseña nueva, el servicio se encarga de que la contraseña sea válida y que no se repita con la anterior, al estar todo correcto, se busca al usuario por el código solicitado de la entidad `VerificationPass.java` y actualiza la contraseña del usuario.

## Rama de administración

Esta rama se encarga de llevar las operaciones con usuarios, pudiendo darlos de alta, consultarlos, editarlos y eliminarlos, también se encarga de llevar a cabo la exportación e importación de datos en JSON y EXCEL, los únicos roles que entran en juego en esta rama son ADMIN y SUPERADMIN, en total poseen 10 endpoints, se detallará su función y el método http que sirven:

- /create-user - POST: Este endpoint se encarga de crear un usuario, recibe por body un DTO de usuario con los datos **email,username,contraseña,descuento,segundo descuento,comercial,rol** todos estos datos son obligatorios, el servicio destinado a esta rama llamado `AdminService.java` se encarga de validar de que el usuario no esté repetido, campos de texto válidos, valor de comercial existente y rol existente, este método de creación, a diferencia del registro manual, no valida si el correo electrónico del usuario existe, por lo que depende del administrador verificar que el correo introducido existe.

- /get-users - GET: Este endpoint recoge todos los usuarios, los introduce en un DTO y los devuelve en lista, dependiendo del rol que solicite estos usuarios se mostrarán unos u otros, por ejemplo un SUPERADMIN puede verlos todos, pero un ADMIN no puede ver a otros SUPERADMIN, este endpoint es el único que puede ser llamado por un usuario con rol de COMERCIAL, siendo que solo puede ver los usuarios que tenga destinados.

- /verificate/{userId} - PATCH: Este endpoint verifica una cuenta en proceso, cambiandole el rol de VERIFICATION a CLIENTE al usuario afectado, requiere de un uuid de usuario siendo que se coloca en el path, el servicio valida el uuid y comprueba que el usuario exista.

- /del-user/{userId} - DELETE: Este endpoint borra a un usuario por completo junto con los pedidos que tiene asociados, requiere del uuid del usuario por path, un SUPERADMIN no puede borrar a otro SUPERADMIN, lo mismo aplica para ADMIN hacia SUPERADMIN,  el servicio valida el uuid y comprueba que el usuario exista.

- /upt-user/{userId} - PATCH: Este endpoint actualiza los datos de un usuario, se le debe de pasar un uuid por path, y los datos a actualizar en un objeto que se recoge como un DTO, el servidor valida que los datos a actualizar sean correctos y que no coincida con otro usuario, este endpoint actualiza también el rol de usuario siendo que se puede quitar la verificación y dejar la cuenta en proceso, como el anterior método los ADMIN y SUPERADMIN no pueden editar a otro SUPERADMIN.

- /verificate-action - POST: Este endpoint se encarga de permitir que un SUPERADMIN realice una acción, en concreto exportar o importar datos en EXCEL y JSON, para ello se valida que el usuario sea SUPERADMIN y que su contraseña, obtenida por body, sea correcta, una vez con todo validado se  en la entidad `AdminVerification.java` un código de vericación y se manda por email, ese códig autoriza al administrador a realizar una de las acciones mencionadas.

- /export-data - POST: Este endpoint exporta, todos los datos de los productos a JSON, siendo que se exportan configuraciones y datos básicos, requiere de un código de verificación que se obtiene por body, se comprueba que sea válido y que coincida con un usuario SUPERADMIN, una vez que se cumpla esas validaciones se recogen los datos por orden siendo que primero se recogen los acabados y colores, posteriormente los productos, ya que las demás entidades dependen de las primeras recogidas, las siguientes en recogerse son, variantes, frentes y por último configuraciones, se crea un archivo JSON estructurado y se devuelve a la web, ese archivo JSON sirve únicamente para copias de seguridad.

- /import-data - POST: Este endpoint recibe por un multipart/form-data el código de verificación y el archivo JSON a importar, como en el antiguo endpoint, se importan los productos y configuraciones en un orden, siendo que primero se importan los acabados y colores, seguidamente los productos, y por último las variables dependientes siendo las variantes, frentes y configuraciones, este endpoint sirve tanto para importar desde 0 como para actualizar productos y configuraciones.

- /export-excel - POST: Este endpoint exporta las configuraciones de los productos, ordenadas por catálogo, a un fichero Excel, siendo que exporta la tarifa entera al formato que necesita la aplicación, este archivo sirve para aplicar actualizaciones masivas de precios o medidas, ya que se trabaja a nivel de excel en vez de aplicación.

- /import-excel - POST: Este endpoint recibe por un multipart/form-data el código de verificación y el archivo EXCEL a importar, los archivos EXCEL en Java son muy sensibles para importar datos, por lo que el servicio si detecta que una configuración contiene datos erróneos, para no arrastrar el error hacia otras configuraciones, salta a la siguiente configuración anulando la importación de la afectada, este endpoint solo actualiza, si hay un producto que está sin configuraciones, y en el Excel contiene datos de ese producto, no las importará.

## Rama de gestión de productos

Esta rama se encarga de gestionar los productos y configuraciones del configurador, cuenta con 29 endpoints, siendo que se detallará la entidad junto con sus 4 métodos, ya que cada entidad cuenta con métodos GET, POST, PATCH y DELETE, que el servicio maneja por un switch que recoge los datos necesarios para poder gestionar esta sección.

- Productos: Cuenta con la entidad `Producto.java`, es una **entidad padre** siendo que no depende de otra directamente lo que juega un papel crucial, ya que su edición o eliminación, puede afectar al resto de entidades que dependen directa o indirectamente de esta entidad, cuenta con los siguientes datos:
    - nombre: Nombre del producto, debe de ser único.
    - tipo: Tipo de producto, mueble, lavabo, espejo, etc. Separa los productos para que en la web el usuario final se oriente a escoger la configuración que mejor le convenga.
    - cajon: En caso de que el producto sea un mueble, se le coloca la marca que ha diseñado el cajón.
    - orden: Campo para ordenar los productos por catálogo.
    - frentesProductos: Clave foránea que se relaciona con otra tabla que junta frente con productos, para asociar varios frentes a varios productos.

    Esta entidad 

- Series: Cuenta con la entidad `Serie.java`, en la web toma el nombre como **variante**, corresponde a distintas formas u opciones de un producto, por lo que depende directamente de la entidad `Producto.java`, cuenta con los siguientes datos:
    - variante: Nombre de la variante debe de ser único.
    - modulo: Tipo de variante, se indica si la variante cuenta con patas, está suspendida, cuenta con medidas o selecciones especiales, etc.
    - extra: Campo que otorga funciones especiales a la variante, para filtrarse o aparecer en situaciones determinadas.
    - orden: Campo para ordenar las variantes por catálogo.
    - producto: Producto asociado a la variante, cualquier cambio en el producto supone un cambio inmediato en la variante afectada.

- Acabados: Cuenta con la entidad `Acabado.java`, es una **entidad padre**, como ocurre en productos, su edición o eliminación afectan a otras entidades que dependen de esta, cuenta con los siguientes datos:
    - nombre: Nombre del acabado, debe de ser único.
    - tipos: Características adicionales del acabado que hacen que se filtre o actúe de diferentes maneras al resto cuando se selecciona en una configuración.
    - orden: Campo para ordenar los acabados por catálogo.
    - colores: Clave foránea que se relaciona con otra tabla que junta colores con acabados, para asociar varios colores a varios acabados.
    - frentes: Clave foránea que se relaciona con otra tabla que junta frente con acabados, para asociar varios frentes a varios acabados.
    - frentesExtension: Clave foránea que se relaciona con otra tabla que junta regletas o tiradores con acabados, se llama frentesExtension, ya que las regletas o tiradores se encuentran dentro de un Frente, por lo que requiere de otros acabados.

- Colores: Cuenta con la entidad `Color.java`, corresponde a los colores que posee un acabado en concreto, por lo que depende directamente de la entidad `Acabado.java`, cuenta con los siguientes datos:
    - nombre: Nombre del color, debe de ser único.
    - orden: Campo para ordenar los colores por catálogo.
    - acabados: Clave foránea que se relaciona directamente con acabados, añade los acabados a los que se asocia este color.

- Frentes: Cuenta con la entidad `Frente.java`, corresponde a los frentes que posee un producto en concreto, esta entidad es muy dependiente, depende directamente de `Producto.java` y de `Acabado.java`, ya que un frente necesita de un producto para aparecer, y necesita de un acabado para compararlo con el precio de un armazón, además de darle funcionalidad a regletas o tiradores, por lo que si una de las entidades padres se modifica, afecta directamente a esta entidad, cuenta con los siguientes datos:
    - nombre: Nombre del frente, debe de ser único.
    - referencia: Primeras dos iniciales del frente, debe de ser único.
    - regleta: Booleano que indica si el frente posee regleta o no.
    - tirador: Booleano que indica si el frente posee tirador o no.
    - orden: Campo para ordenar los frentes por catálogo.
    - acabados: Clave foránea que se relaciona directamente con los acabados, añade los acabados a los que se asocia el frente.
    - acabadosExtension: Clave foránea que se relaciona directamente con los acabados de las regletas o tiradores.
    - productoFrente: lave foránea que se relaciona con los productos a los que pertenece un frente.

- Configuraciones: Cuenta con la entidad `Configuracion.java`, contiene los datos de las configuraciones de un producto, como pasa con la entidad frente, es la más dependiente, ya que depende del resto de entidades para funcionar, siendo que es la entidad final que da precios y medidas de un producto, en base a otras entidades definidas, cuenta con los siguientes datos:
    - referencia: Código de la configuración, es un String y debe de ser único.
    - fondo: Valor flotante que indica el fondo base del mueble.
    - ancho: Valor flotante que indica el ancho del mueble.
    - alto: Valor flotante que indica el alto del mueble.
    - altoMax: Valor flotante que indica el alto máximo del mueble.
    - fondoMin: Valor flotante que indica el fondo mínimo del mueble, requerido solo para medidas especiales.
    - fondoMax: Valor flotante que indica el fondo máximo del mueble, requerido solo para medidas especiales.
    - precioMedidaFondoEsp: Valor flotante que indica el precio a sumar por seleccionar un fondo especial.
    - precioMedidaAnchoEsp: Valor flotante que indica el precio a sumar por seleccionar un ancho especial.
    - precioMedidaAltoEsp: Valor flotante que indica el precio a sumar por seleccionar un alto especial.
    - armazon: Objeto json que contiene los acabados disponibles junto con el precio correspondiente, su esquema es [ {"nombre":"acabado","precio":valor-precio} ]
    - extras: Objeto json que contiene los extras de la configuración junto con su precio, su esquema es  [ {"nombre":"acabado","precio":valor-precio} ]
    - serie: Clave foránea que identifica la serie para relacinar configuraciones.

Todas estas entidades salvo los métodos GET, solo pueden ser activadas por usuarios SUPERADMIN, la obtención de entidades pueden ser activadas por CLIENTES, ya que se complementa con la configuración de productos.

## Rama de acciones generales

Esta rama contiene los endpoints que pueden ejecutar los usuarios, con rol CLIENTE, corresponde con la configuración de productos y realización de pedidos, contiene 11 endpoints, se detallará su función y método.

- /user - GET: Este endpoint devuelve los datos de contacto del usuario, siendo su nombre de usuario y correo, el resto de datos solo los puede visualizar un comercial o un administrador.

- /configure - POST: Este endpoint se encarga de crear una entidad `ProductoConfigurado.java` a través de una configuración, esta entidad contiene los datos que el usuario ha escogido para configurar un producto, siendo que depende directamente de la entidad `Configuracion.java`, los datos que se pasan son los siguientes:
    - configuracion: Objeto `Configuracion.java` que el usuario ha escogido a través de una referencia.
    - usuario: Usuario que ha configurado el producto.
    - precioArmazaon: Valor flotante que corresponde con el precio del armazón escogido, se usa la variable configuracion como refrencia directa.
    - acabado: Acabado escogido, se evalúa que coincida con el objeto configuración para darle valor a precioArmazon.
    - colorArmazon: Color del armazon escogido.
    - codigoColorArmazon: Si el acabado escogido contiene el tipo codigo, en vez de seleccionar un color se rellena este campo haciendo referencia a colores especiales como RAL o NCS.
    - precioFrente: Valor flotante que corresponde con el precio del acabado del frente escogido, se usa la variable configuración y acabadoFrente como referencia directa.
    - frente: Frente escogido en la configuración.
    - acabadoFrente: Acabado del frente escogido, se evalúa con el objeto configuracion y frente para darle valor a precioFrente.
    - colorFrente: Color del frente escogido.
    - codigoColorFrente: Si el acabado del frente escogido contiene el tipo codigo, en vez de seleccionar un color se rellena este campo haciendo referencia a colores especiales como RAL o NCS.
    - precioTirador (deshabilitado): Precio del tirador escogido, está en desuso.
    - acabadoTirador: Acabado del tirador escogido, se evalúa con el objeto frente mediante acabadosExtension.
    - colorTirador: Color del tirador escogido:
    - codigoColorTirador: Si el acabado del tirador escogido contiene el tipo codigo, en vez de seleccionar un color se rellena este campo haciendo referencia a colores especiales como RAL o NCS.
    - precioRegleta (deshabilitado): Precio de la regleta escogida, está en desuso.
    - acabadoRegleta: Acabado de la regleta escogida, se evalúa con el objeto frente mediante acabadosExtension.
    - colorRegleta: Color de la regleta escogida.
    - codigoColorRegleta: Si el acabado de la regleta escogida contiene el tipo codigo, en vez de seleccionar un color se rellena este campo haciendo referencia a colores especiales como RAL o NCS.
    - fondo: Valor del fondo si se ha escogido un fondo especial, en caso contrario se pondrá a 0 y se usará el del objeto configuracion.
    - ancho: Valor del ancho si se ha escogido un ancho especial, en caso contrario se pondrá a 0 y se usará el del objeto configuracion.
    - alto: Valor del alto si se ha escogido un alto especial, en caso contrario se pondrá a 0 y se usará el del objeto configuracion.
    - extras: Extras de la configuración seleccionados.
    - precioFinal: Precio final de la configuración que suma, suplementos de medidas especiales, precio más alto entre los precios de un acabado o frente.
    - cantidad: Cantidad de la configuración.
    - observaciones: Observaciones de la configuración realizada.
    - fecha: Fecha de realización de la configuración.

    Esta configuración se almacena en una cesta para la entidad `BulkProductosUsuario.java` la cual almacena una referencia, esto sirve para cuando un usuario quiere añadir más productos a su cesta para luego crear un presupuesto.

    **IMPORTANTE** Este endpoint contiene un servicio que es muy sensible, ya que se valida todo lo que el usuario pasa, por lo que si se va a actualizar, se debe de depurar linea a linea para que ninguna validación falle, y sobre todo la configuracion junto con la cesta se guarden correctamente en la base de datos con las variables String encriptadas.

- /configure - GET: Endpoint que devuelve la cesta con una o varias configuraciones pendientes del usuario, se le pueden pasar dos variables como query params, una es isEnd que es booleano, y otra uuid String, correspondiente a la cesta actual.

- /configure/{uuid} - PATCH: Endpoint que finaliza una configuración, cerrando la cesta para poder mandarla a transformar a un pedido NO CURSADO, utiliza el uuid de la cesta por path, se valida en el mismo endpoint que la cesta exista.

- /configure - PATCH: Endpoint que actualiza una observación de un producto configurado en una cesta, se le pasa un objeto json, el cual debe de contener el uuid del producto configurado y la observación nueva, se valida que el producto configurado exista y que se encuentre dentro de una cesta abierta.

- /configure/{uuid} - DELETE: Endpoint que elimina un producto configurado de una cesta abierta, se le pasa el uuid del producto configurado por path, se valida en el servicio que el producto exista, y que se encuentre dentro de una cesta abierta.

- /order-proposal - POST: Endpoint que crea una entidad `Pedido.java` a partir de una cesta, esta entidad posee los siguientes datos:
    - referencia: String que identifica el pedido como un concepto, debe de ser único por usuario.
    - productos: Lista de uuid productos configurados.
    - fecha: Fecha de realización del pedido.
    - estado: Enum que explica el estado de un pedido, siendo que puede tener dos valores CURSADO o NO_CURSADO.
    - usuarioPedido: Objeto usuario, es el autor del pedido.

    Este endpoint además crea una copia de seguridad del pedido, siendo la entidad `PedidoBackup.java` que contiene los mismos datos del pedido pero con la diferencia de que productos, en vez de ser una lista de uuid, es un json con el valor en bruto de los productos, esto sirve para que en caso de que se elimine un producto, y el pedido esta en CURSADO, no se elimine deirectamente de la base de datos hasta que pasen 45 días, o un administrador lo borre.

- /order-proposal - GET: Endpoint que devuelve una lista de pedidos CURSADOS y NO CURSADOS.

- /order-proposal-bak - GET: Endpoint que devuelve una lista de copias de seguridad de pedidos CURSADOS, en caso de que esos pedidos existan el endpoint omite devolver sus copias.

- /order-proposal/send - POST: Endpoint que envía un pdf del pedido por correo electrónico a clientes@creacionescampoaras.com y al comercial designado del usuario que ha tramitado el pedido, se valida que el archivo corresponda con un pdf.

- /scrapping/color - GET: Endpoint que se conecta a la web  [coloresral.com](https://www.coloresral.com.es/) o  [coloresncs.es](https://www.coloresncs.es/) para extraer el color RAL o NCS, se realiza un scrapping web, de manera que se valida que el código introducido sea correcto.

## Rama de pedidos

Rama especial dedidaca a la gestión de pedidos, contiene tres endpoints, de los que se detallará función y método.

- /producto-configurado - GET: Endpoint que devuelve la cesta o pedido de un usuario que esté en NO CURSADO, este endpoint solo pueden ejecutarlo comerciales o administradores para consultar la cesta de un usuario en concreto.

- /producto-configurado/{uuid} - DELETE: Endpoint que permite borrar pedidos NO CURSADOS, este endpoint se puede activar desde un cliente para que borre sus propios pedidos, o a través de un rol superior para borrar pedidos de un cliente, se le debe de pasar el uuid por path.

- /orders-bak/{uuid} - DELETE: Endpoint que permite borrar la copia de seguridad de un pedido, este endpoint solo pueden ejecutarlo administradores, ya que son los encargados de gestionar y borrar las copiad de seguridad de un pedido, se le pasa su uuid por path.

# Funciones especiales

El configurador posee un archivo llamado `CampoarasScheduled.java` el cual es un archivo programado que a las dos de la mañana, revisa todos los pedidos y configuraciones, si han pasado 45 días desde su creación, el archivo borra esos pedidos obsoletos incluyendo las copias de seguridad.

Internamente en los endpoints de gestión de productos, cada vez que se borra una entidad padre, o importante, las entidades que son dependientes y los pedidos realizados con esa entidad se borran para evitar errores por datos inexistentes.

El configurador posee un servicio de correo, el cual se conecta a la cuenta no-reply@creacionescampoaras.com con el que se envían correos de verificación para registrar, recuperar contraseñas o realizar acciones de administración, y para enviar pedidos a clientes@creacionescampoaras.com y a los comerciales de los usuarios, los correos se envvían en formato HTML y solo los de pedidos aceptan archivos PDF, estos correos se gestionan desde `MailService.java`.

En el configurador existe un directorio dedicado a las imágenes de los productos, siendo que se encuentra en /src/main/resources/imgs, las imágenes toman el nombre del uuid del producto junto con la extensión recibida, siendo .png, .jpg o .jpeg, es muy importante no renombrar las imágenes ya que perderían la referencia del producto y no se mostrarían en la web.